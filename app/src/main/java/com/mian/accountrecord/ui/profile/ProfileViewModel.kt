package com.mian.accountrecord.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.data.local.preferences.AuthPreferences
import com.mian.accountrecord.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val nickname: String = "",
    val avatarUrl: String? = null,
    val providerLabel: String = "",
    val showLogoutDialog: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    fun refreshUserInfo() {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        val nickname = authPreferences.currentNickname ?: ""
        val avatarUrl = authPreferences.currentAvatarUrl
        val providerLabel = when (authPreferences.currentProvider?.lowercase()) {
            "wechat" -> "微信登录"
            "alipay" -> "支付宝登录"
            else -> ""
        }
        _uiState.value = ProfileUiState(
            nickname = nickname,
            avatarUrl = avatarUrl,
            providerLabel = providerLabel
        )
    }

    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun dismissLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    fun confirmLogout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.value = _uiState.value.copy(showLogoutDialog = false)
            onLogoutComplete()
        }
    }
}
