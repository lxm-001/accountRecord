package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.model.LedgerTemplate
import com.mian.accountrecord.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteLedgerUseCaseTest {

    private val ledger = Ledger(
        id = 1, name = "日常账本", icon = "book",
        template = LedgerTemplate.DAILY, isActive = true
    )

    @Test
    fun `only one ledger remaining should return failure`() = runTest {
        val repo = FakeLedgerRepository(ledgerCount = 1)
        val useCase = DeleteLedgerUseCase(repo)
        val result = useCase(ledger)
        assertTrue(result.isFailure)
        assertEquals("至少保留一个账本", result.exceptionOrNull()?.message)
    }

    @Test
    fun `multiple ledgers should delete successfully`() = runTest {
        val repo = FakeLedgerRepository(ledgerCount = 3)
        val useCase = DeleteLedgerUseCase(repo)
        val result = useCase(ledger)
        assertTrue(result.isSuccess)
        assertTrue(repo.deletedLedgers.contains(ledger))
    }

    @Test
    fun `exactly two ledgers should allow deletion`() = runTest {
        val repo = FakeLedgerRepository(ledgerCount = 2)
        val useCase = DeleteLedgerUseCase(repo)
        val result = useCase(ledger)
        assertTrue(result.isSuccess)
    }

    private class FakeLedgerRepository(
        private val ledgerCount: Int
    ) : LedgerRepository {
        val deletedLedgers = mutableListOf<Ledger>()

        override fun getAll(): Flow<List<Ledger>> = flowOf(emptyList())
        override fun getActive(): Flow<Ledger?> = flowOf(null)
        override suspend fun insert(ledger: Ledger): Long = 0L
        override suspend fun update(ledger: Ledger) {}
        override suspend fun delete(ledger: Ledger) { deletedLedgers.add(ledger) }
        override suspend fun switchTo(id: Long) {}
        override suspend fun count(): Int = ledgerCount
    }
}
