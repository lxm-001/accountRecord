package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.LoginResult
import com.mian.accountrecord.domain.model.OAuthResult
import com.mian.accountrecord.domain.model.User
import com.mian.accountrecord.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithAlipayUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(oauthResult: OAuthResult): Result<LoginResult> = runCatching {
        val user = User(
            openId = oauthResult.openId,
            nickname = oauthResult.nickname,
            avatarUrl = oauthResult.avatarUrl,
            oauthProvider = oauthResult.provider
        )
        val isFirstLogin = !authRepository.hasLoginHistory(user.openId)
        authRepository.saveLoginInfo(user).getOrThrow()
        authRepository.recordLogin(user)
        LoginResult(user = user, isFirstLogin = isFirstLogin)
    }
}
