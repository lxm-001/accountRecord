package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.User
import com.mian.accountrecord.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckFirstLoginUseCaseTest {

    private class FakeAuthRepository(
        private val hasHistory: Boolean
    ) : AuthRepository {
        override suspend fun saveLoginInfo(user: User): Result<Unit> = Result.success(Unit)
        override suspend fun clearAuth(): Result<Unit> = Result.success(Unit)
        override suspend fun getCurrentUser(): User? = null
        override suspend fun hasLoginHistory(openId: String): Boolean = hasHistory
        override suspend fun recordLogin(user: User) {}
    }

    @Test
    fun `returns true when user has no login history`() = runTest {
        val repository = FakeAuthRepository(hasHistory = false)
        val useCase = CheckFirstLoginUseCase(repository)

        val result = useCase("test_open_id")

        assertTrue(result)
    }

    @Test
    fun `returns false when user has login history`() = runTest {
        val repository = FakeAuthRepository(hasHistory = true)
        val useCase = CheckFirstLoginUseCase(repository)

        val result = useCase("test_open_id")

        assertFalse(result)
    }
}
