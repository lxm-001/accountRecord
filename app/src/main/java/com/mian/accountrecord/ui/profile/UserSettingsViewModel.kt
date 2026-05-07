package com.mian.accountrecord.ui.profile

import androidx.lifecycle.ViewModel
import com.mian.accountrecord.data.local.preferences.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class UserSettingsUiState(
    val nickname: String = "",
    val avatarUrl: String? = null,
    val phone: String = "",
    val email: String = "",
    val providerLabel: String = ""
)

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSettingsUiState())
    val uiState: StateFlow<UserSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        _uiState.value = UserSettingsUiState(
            nickname = authPreferences.currentNickname ?: "",
            avatarUrl = authPreferences.currentAvatarUrl,
            phone = authPreferences.currentPhone ?: "",
            email = authPreferences.currentEmail ?: "",
            providerLabel = when (authPreferences.currentProvider?.lowercase()) {
                "wechat" -> "微信登录"
                "alipay" -> "支付宝登录"
                else -> "游客"
            }
        )
    }

    fun updateNickname(nickname: String) {
        authPreferences.currentNickname = nickname
        _uiState.value = _uiState.value.copy(nickname = nickname)
    }

    fun updateAvatar(avatarUrl: String) {
        authPreferences.currentAvatarUrl = avatarUrl
        _uiState.value = _uiState.value.copy(avatarUrl = avatarUrl)
    }

    fun updatePhone(phone: String) {
        authPreferences.currentPhone = phone
        _uiState.value = _uiState.value.copy(phone = phone)
    }

    fun updateEmail(email: String) {
        authPreferences.currentEmail = email
        _uiState.value = _uiState.value.copy(email = email)
    }
}
