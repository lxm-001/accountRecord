package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.data.local.preferences.AuthPreferences
import com.mian.accountrecord.domain.model.AuthState
import javax.inject.Inject

class CheckAuthStateUseCase @Inject constructor(
    private val authPreferences: AuthPreferences
) {
    operator fun invoke(): AuthState {
        return if (authPreferences.isLoginInfoValid()) {
            AuthState.AUTHENTICATED
        } else {
            AuthState.UNAUTHENTICATED
        }
    }
}
