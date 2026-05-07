package com.mian.accountrecord.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.model.AuthState
import com.mian.accountrecord.domain.model.LoginResult
import com.mian.accountrecord.domain.model.OAuthProvider
import com.mian.accountrecord.domain.model.OAuthResult
import com.mian.accountrecord.domain.usecase.CheckAuthStateUseCase
import com.mian.accountrecord.domain.usecase.LoginWithAlipayUseCase
import com.mian.accountrecord.domain.usecase.LoginWithWeChatUseCase
import com.mian.accountrecord.util.ClickGuard
import com.mian.accountrecord.util.LoginRateLimiter
import com.mian.accountrecord.util.NetworkChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithWeChat: LoginWithWeChatUseCase,
    private val loginWithAlipay: LoginWithAlipayUseCase,
    private val checkAuthState: CheckAuthStateUseCase,
    private val networkChecker: NetworkChecker,
    private val loginRateLimiter: LoginRateLimiter
) : ViewModel() {

    data class LoginUiState(
        val authState: AuthState = AuthState.UNAUTHENTICATED,
        val isAgreementChecked: Boolean = false,
        val isLoading: Boolean = false,
        val loadingMessage: String = "",
        val errorMessage: String? = null,
        val loginResult: LoginResult? = null,
        val cooldownSeconds: Int = 0,
        val showCancelDialog: Boolean = false
    )

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    private var currentOAuthState: String = ""
    private val clickGuard = ClickGuard()
    private var loginJob: Job? = null
    private var cooldownJob: Job? = null

    /**
     * 启动时检查登录态，若已登录则更新 authState 为 AUTHENTICATED。
     */
    fun checkInitialAuthState() {
        val state = checkAuthState()
        _uiState.value = _uiState.value?.copy(authState = state)
    }

    /**
     * 用户协议勾选状态变更。
     */
    fun onAgreementCheckedChange(checked: Boolean) {
        _uiState.value = _uiState.value?.copy(isAgreementChecked = checked)
    }

    /**
     * 微信登录按钮点击。
     * 流程：ClickGuard → 网络检测 → 频率检测 → 生成 CSRF state → 设置加载态 → 15 秒超时等待回调。
     */
    fun onWeChatLoginClick() {
        performLogin(OAuthProvider.WECHAT)
    }

    /**
     * 支付宝登录按钮点击。
     */
    fun onAlipayLoginClick() {
        performLogin(OAuthProvider.ALIPAY)
    }

    private fun performLogin(provider: OAuthProvider) {
        // ClickGuard 防重复点击（Req 7.1）
        if (!clickGuard.isClickAllowed()) return

        val current = _uiState.value ?: return

        // 网络检测（Req 7.5）
        if (!networkChecker.isNetworkAvailable()) {
            _uiState.value = current.copy(
                errorMessage = "当前无网络连接，请检查网络设置"
            )
            return
        }

        // 频率限制检测（Req 6.1）
        if (!loginRateLimiter.canLogin()) {
            startCooldownTimer()
            return
        }

        // 记录登录尝试
        loginRateLimiter.recordLoginAttempt()

        // 再次检查是否触发冷却
        if (!loginRateLimiter.canLogin()) {
            startCooldownTimer()
            return
        }

        // 生成 CSRF state（Req 6.2）
        currentOAuthState = UUID.randomUUID().toString()

        // 设置加载态
        _uiState.value = current.copy(
            authState = AuthState.AUTHENTICATING,
            isLoading = true,
            loadingMessage = "正在登录...",
            errorMessage = null
        )

        // 15 秒超时处理（Req 7.4）
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Wait for 15 seconds; if onOAuthCallback cancels this job, timeout won't fire
            kotlinx.coroutines.delay(15_000L)
            if (_uiState.value?.isLoading == true) {
                // 超时：恢复 UI 状态
                _uiState.value = _uiState.value?.copy(
                    authState = AuthState.UNAUTHENTICATED,
                    isLoading = false,
                    loadingMessage = "",
                    errorMessage = "登录超时，请检查网络后重试"
                )
                clickGuard.reset()
            }
        }
    }

    /**
     * OAuth 回调处理。
     * 验证 state 一致性（Req 6.2, 6.3）→ 调用登录用例 → 更新状态。
     */
    fun onOAuthCallback(oauthResult: OAuthResult) {
        // 取消超时计时
        loginJob?.cancel()

        viewModelScope.launch {
            // CSRF state 验证（Req 6.3）
            if (oauthResult.state != currentOAuthState) {
                _uiState.value = _uiState.value?.copy(
                    authState = AuthState.UNAUTHENTICATED,
                    isLoading = false,
                    loadingMessage = "",
                    errorMessage = "登录异常，请重试"
                )
                return@launch
            }

            val useCase: suspend (OAuthResult) -> Result<LoginResult> = when (oauthResult.provider) {
                OAuthProvider.WECHAT -> loginWithWeChat::invoke
                OAuthProvider.ALIPAY -> loginWithAlipay::invoke
            }

            useCase(oauthResult)
                .onSuccess { result ->
                    _uiState.value = _uiState.value?.copy(
                        authState = AuthState.AUTHENTICATED,
                        isLoading = false,
                        loadingMessage = "",
                        loginResult = result
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value?.copy(
                        authState = AuthState.UNAUTHENTICATED,
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = "登录失败，请稍后重试"
                    )
                }
        }
    }

    /**
     * 显示取消登录确认对话框（Req 7.7）。
     */
    fun onShowCancelDialog() {
        _uiState.value = _uiState.value?.copy(showCancelDialog = true)
    }

    /**
     * 确认取消登录：取消当前登录流程，恢复初始状态。
     */
    fun onCancelLogin() {
        loginJob?.cancel()
        clickGuard.reset()
        _uiState.value = _uiState.value?.copy(
            authState = AuthState.UNAUTHENTICATED,
            isLoading = false,
            loadingMessage = "",
            showCancelDialog = false,
            errorMessage = null
        )
    }

    /**
     * 关闭取消登录对话框（用户选择继续等待）。
     */
    fun onDismissCancelDialog() {
        _uiState.value = _uiState.value?.copy(showCancelDialog = false)
    }

    /**
     * 跳过登录，以游客身份直接进入主页。
     */
    fun skipLogin() {
        _uiState.value = _uiState.value?.copy(
            authState = AuthState.AUTHENTICATED,
            loginResult = LoginResult(
                user = com.mian.accountrecord.domain.model.User(
                    openId = "guest",
                    nickname = "游客",
                    avatarUrl = null,
                    oauthProvider = OAuthProvider.WECHAT
                ),
                isFirstLogin = false
            )
        )
    }

    /**
     * 清除错误消息（Req 7.8）。
     */
    fun onErrorDismissed() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    /**
     * 获取当前 CSRF state，供 UI 层传递给 SDK。
     */
    fun getCurrentOAuthState(): String = currentOAuthState

    /**
     * 启动冷却倒计时（Req 6.1）。
     * 每秒更新 cooldownSeconds，倒计时结束后清零。
     */
    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        val remaining = loginRateLimiter.getCooldownRemainingSeconds()
        if (remaining <= 0) return

        _uiState.value = _uiState.value?.copy(
            cooldownSeconds = remaining,
            errorMessage = null
        )

        cooldownJob = viewModelScope.launch {
            var seconds = remaining
            while (seconds > 0) {
                _uiState.value = _uiState.value?.copy(cooldownSeconds = seconds)
                delay(1000L)
                seconds = loginRateLimiter.getCooldownRemainingSeconds()
            }
            _uiState.value = _uiState.value?.copy(cooldownSeconds = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        loginJob?.cancel()
        cooldownJob?.cancel()
    }
}
