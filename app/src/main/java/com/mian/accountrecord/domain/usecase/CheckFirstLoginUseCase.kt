package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.repository.AuthRepository
import javax.inject.Inject

class CheckFirstLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(openId: String): Boolean {
        return !authRepository.hasLoginHistory(openId)
    }
}
