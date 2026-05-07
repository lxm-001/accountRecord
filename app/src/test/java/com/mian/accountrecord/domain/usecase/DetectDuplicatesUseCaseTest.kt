package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class DetectDuplicatesUseCaseTest {

    private lateinit var fakeRepo: FakeTransactionRepository
    private lateinit var useCase: DetectDuplicatesUseCase

    private val defaultCategory = Category(
        id = 1, name = "餐饮", icon = "restaurant",
        color = "#FF5722", type = TransactionType.EXPENSE
    )

    @Before
    fun setup() {
        fakeRepo = FakeTransactionRepository()
        useCase = DetectDuplicatesUseCase(fakeRepo)
    }

    @Test
    fun `empty parsed list returns empty list`() = runTest {
        val result = useCase(emptyList(), 1L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `no duplicates when ledger has no existing transactions`() = runTest {
        fakeRepo.existingTransactions = emptyList()

        val parsed = listOf(
            createParsedTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val result = useCase(parsed, 1L)

        assertEquals(1, result.size)
        assertFalse(result[0].isDuplicate)
        assertTrue(result[0].isSelected)
    }

    @Test
    fun `detects duplicate when amount date and counterparty match`() = runTest {
        fakeRepo.existingTransactions = listOf(
            createExistingTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val parsed = listOf(
            createParsedTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val result = useCase(parsed, 1L)

        assertEquals(1, result.size)
        assertTrue(result[0].isDuplicate)
        assertFalse(result[0].isSelected)
    }

    @Test
    fun `no duplicate when amount differs`() = runTest {
        fakeRepo.existingTransactions = listOf(
            createExistingTransaction("50.00", "2024-03-15T14:30:00", "肯德基")
        )

        val parsed = listOf(
            createParsedTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val result = useCase(parsed, 1L)

        assertFalse(result[0].isDuplicate)
        assertTrue(result[0].isSelected)
    }

    @Test
    fun `no duplicate when date differs by more than a minute`() = runTest {
        fakeRepo.existingTransactions = listOf(
            createExistingTransaction("35.50", "2024-03-15T15:00:00", "肯德基")
        )

        val parsed = listOf(
            createParsedTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val result = useCase(parsed, 1L)

        assertFalse(result[0].isDuplicate)
    }

    @Test
    fun `duplicate detected when same minute but different second`() = runTest {
        fakeRepo.existingTransactions = listOf(
            createExistingTransaction("35.50", "2024-03-15T14:30:45", "肯德基")
        )

        val parsed = listOf(
            createParsedTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val result = useCase(parsed, 1L)

        assertTrue(result[0].isDuplicate)
    }

    @Test
    fun `mixed duplicates and non-duplicates`() = runTest {
        fakeRepo.existingTransactions = listOf(
            createExistingTransaction("35.50", "2024-03-15T14:30:00", "肯德基")
        )

        val parsed = listOf(
            createParsedTransaction("35.50", "2024-03-15T14:30:00", "肯德基"),
            createParsedTransaction("100.00", "2024-03-16T10:00:00", "超市")
        )

        val result = useCase(parsed, 1L)

        assertEquals(2, result.size)
        assertTrue(result[0].isDuplicate)
        assertFalse(result[0].isSelected)
        assertFalse(result[1].isDuplicate)
        assertTrue(result[1].isSelected)
    }

    private fun createParsedTransaction(
        amount: String,
        dateStr: String,
        counterparty: String
    ): ParsedTransaction {
        return ParsedTransaction(
            amount = BigDecimal(amount),
            type = TransactionType.EXPENSE,
            date = LocalDateTime.parse(dateStr),
            counterparty = counterparty,
            originalCategory = "餐饮美食",
            mappedCategoryName = "餐饮"
        )
    }

    private fun createExistingTransaction(
        amount: String,
        dateStr: String,
        note: String
    ): Transaction {
        return Transaction(
            id = 1,
            amount = BigDecimal(amount),
            type = TransactionType.EXPENSE,
            category = defaultCategory,
            ledgerId = 1,
            date = LocalDateTime.parse(dateStr),
            note = note
        )
    }

    private class FakeTransactionRepository : TransactionRepository {
        var existingTransactions: List<Transaction> = emptyList()

        override fun getByLedgerAndDateRange(
            ledgerId: Long,
            start: LocalDateTime,
            end: LocalDateTime
        ): Flow<List<Transaction>> = flowOf(existingTransactions)

        override fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>> =
            flowOf(emptyList())

        override fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> =
            flowOf(MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))

        override fun getExpenseByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>> =
            flowOf(emptyList())

        override suspend fun insert(transaction: Transaction): Long = 1L
        override suspend fun insertAll(transactions: List<Transaction>): List<Long> = emptyList()
        override suspend fun update(transaction: Transaction) {}
        override suspend fun delete(transaction: Transaction) {}
    }
}
