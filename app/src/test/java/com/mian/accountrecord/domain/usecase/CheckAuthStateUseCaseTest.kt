package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckAuthStateUseCaseTest {

    /**
     * A testable subclass that overrides the MMKV-dependent behavior
     * by controlling what isLoginInfoValid() returns.
     */
    private class TestableAuthPreferences(
        private val loginInfoValid: Boolean
    ) : com.mian.accountrecord.data.local.preferences.AuthPreferences() {
        override fun isLoginInfoValid(): Boolean = loginInfoValid
    }

    @Test
    fun `returns AUTHENTICATED when login info is valid`() {
        val prefs = TestableAuthPreferences(loginInfoValid = true)
        val useCase = CheckAuthStateUseCase(prefs)

        val result = useCase()

        assertEquals(AuthState.AUTHENTICATED, result)
    }

    @Test
    fun `returns UNAUTHENTICATED when login info is invalid`() {
        val prefs = TestableAuthPreferences(loginInfoValid = false)
        val useCase = CheckAuthStateUseCase(prefs)

        val result = useCase()

        assertEquals(AuthState.UNAUTHENTICATED, result)
    }
}
