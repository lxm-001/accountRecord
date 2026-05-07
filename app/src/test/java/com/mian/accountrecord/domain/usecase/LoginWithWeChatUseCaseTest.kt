package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.model.LedgerTemplate
import com.mian.accountrecord.domain.model.OAuthProvider
import com.mian.accountrecord.domain.model.OAuthResult
import com.mian.accountrecord.domain.model.User
import com.mian.accountrecord.domain.repository.AuthRepository
import com.mian.accountrecord.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginWithWeChatUseCaseTest {

    private class FakeAuthRepository(
        private val hasHistory: Boolean = false
    ) : AuthRepository {
        var saveLoginInfoCalled = false
        var recordLoginCalled = false

        override suspend fun saveLoginInfo(user: User): Result<Unit> {
            saveLoginInfoCalled = true
            return Result.success(Unit)
        }
        override suspend fun clearAuth(): Result<Unit> = Result.success(Unit)
        override suspend fun getCurrentUser(): User? = null
        override suspend fun hasLoginHistory(openId: String): Boolean = hasHistory
        override suspend fun recordLogin(user: User) { recordLoginCalled = true }
    }

    private class FakeLedgerRepository(
        private var ledgerCount: Int = 0
    ) : LedgerRepository {
        var insertedLedger: Ledger? = null

        override fun getAll(): Flow<List<Ledger>> = flowOf(emptyList())
        override fun getActive(): Flow<Ledger?> = flowOf(null)
        override suspend fun insert(ledger: Ledger): Long {
            insertedLedger = ledger
            return 1L
        }
        override suspend fun update(ledger: Ledger) {}
        override suspend fun delete(ledger: Ledger) {}
        override suspend fun switchTo(id: Long) {}
        override suspend fun count(): Int = ledgerCount
    }

    private val testOAuthResult = OAuthResult(
        openId = "test_open_id",
        nickname = "TestUser",
        avatarUrl = "https://example.com/avatar.png",
        state = "test_state",
        provider = OAuthProvider.WECHAT
    )

    @Test
    fun `first login with no ledgers creates default ledger`() = runTest {
        val authRepo = FakeAuthRepository(hasHistory = false)
        val ledgerRepo = FakeLedgerRepository(ledgerCount = 0)
        val useCase = LoginWithWeChatUseCase(authRepo, ledgerRepo)

        val result = useCase(testOAuthResult)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isFirstLogin)
        val inserted = ledgerRepo.insertedLedger
        assertEquals("日常账本", inserted?.name)
        assertEquals("home", inserted?.icon)
        assertEquals(LedgerTemplate.DAILY, inserted?.template)
        assertTrue(inserted?.isActive == true)
    }

    @Test
    fun `first login with existing ledgers does not create default ledger`() = runTest {
        val authRepo = FakeAuthRepository(hasHistory = false)
        val ledgerRepo = FakeLedgerRepository(ledgerCount = 1)
        val useCase = LoginWithWeChatUseCase(authRepo, ledgerRepo)

        val result = useCase(testOAuthResult)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isFirstLogin)
        assertEquals(null, ledgerRepo.insertedLedger)
    }

    @Test
    fun `returning user does not create default ledger`() = runTest {
        val authRepo = FakeAuthRepository(hasHistory = true)
        val ledgerRepo = FakeLedgerRepository(ledgerCount = 0)
        val useCase = LoginWithWeChatUseCase(authRepo, ledgerRepo)

        val result = useCase(testOAuthResult)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow().isFirstLogin)
        assertEquals(null, ledgerRepo.insertedLedger)
    }
}
