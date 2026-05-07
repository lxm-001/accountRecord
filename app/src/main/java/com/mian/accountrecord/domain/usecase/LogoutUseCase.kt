package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.clearAuth()
    }
}
