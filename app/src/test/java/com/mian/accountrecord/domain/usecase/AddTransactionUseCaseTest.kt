package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class AddTransactionUseCaseTest {

    private lateinit var useCase: AddTransactionUseCase
    private lateinit var fakeRepo: FakeTransactionRepository

    @Before
    fun setup() {
        fakeRepo = FakeTransactionRepository()
        useCase = AddTransactionUseCase(fakeRepo)
    }

    private fun createTransaction(amount: BigDecimal) = Transaction(
        amount = amount,
        type = TransactionType.EXPENSE,
        category = Category(
            id = 1, name = "餐饮", icon = "restaurant",
            color = "#FF5722", type = TransactionType.EXPENSE
        ),
        ledgerId = 1,
        date = LocalDateTime.now()
    )

    @Test
    fun `amount zero should return failure`() = runTest {
        val result = useCase(createTransaction(BigDecimal.ZERO))
        assertTrue(result.isFailure)
        assertEquals("金额必须大于0", result.exceptionOrNull()?.message)
    }

    @Test
    fun `negative amount should return failure`() = runTest {
        val result = useCase(createTransaction(BigDecimal("-10.00")))
        assertTrue(result.isFailure)
        assertEquals("金额必须大于0", result.exceptionOrNull()?.message)
    }

    @Test
    fun `amount exceeding upper limit should return failure`() = runTest {
        val result = useCase(createTransaction(BigDecimal("100000000.00")))
        assertTrue(result.isFailure)
        assertEquals("金额超出上限", result.exceptionOrNull()?.message)
    }

    @Test
    fun `valid amount should return success with id`() = runTest {
        fakeRepo.nextInsertId = 42L
        val result = useCase(createTransaction(BigDecimal("100.50")))
        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
    }

    @Test
    fun `amount at upper boundary should return success`() = runTest {
        fakeRepo.nextInsertId = 1L
        val result = useCase(createTransaction(BigDecimal("99999999.99")))
        assertTrue(result.isSuccess)
    }

    private class FakeTransactionRepository : TransactionRepository {
        var nextInsertId = 1L

        override fun getByLedgerAndDateRange(
            ledgerId: Long,
            start: java.time.LocalDateTime,
            end: java.time.LocalDateTime
        ): Flow<List<Transaction>> = flowOf(emptyList())

        override fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>> =
            flowOf(emptyList())

        override fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> =
            flowOf(MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))

        override fun getExpenseByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>> =
            flowOf(emptyList())

        override suspend fun insert(transaction: Transaction): Long = nextInsertId
        override suspend fun insertAll(transactions: List<Transaction>): List<Long> = emptyList()
        override suspend fun update(transaction: Transaction) {}
        override suspend fun delete(transaction: Transaction) {}
    }
}
