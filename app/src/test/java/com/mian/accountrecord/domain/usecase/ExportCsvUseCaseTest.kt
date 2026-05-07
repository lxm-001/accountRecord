package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.CsvPrinter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class ExportCsvUseCaseTest {

    private lateinit var fakeRepo: FakeTransactionRepository
    private lateinit var fakePrinter: FakeCsvPrinter
    private lateinit var useCase: ExportCsvUseCase

    private val defaultCategory = Category(
        id = 1, name = "餐饮", icon = "restaurant",
        color = "#FF5722", type = TransactionType.EXPENSE
    )

    @Before
    fun setup() {
        fakeRepo = FakeTransactionRepository()
        fakePrinter = FakeCsvPrinter()
        useCase = ExportCsvUseCase(fakeRepo, fakePrinter)
    }

    @Test
    fun `export passes transactions to printer`() = runTest {
        val transactions = listOf(
            Transaction(
                id = 1, amount = BigDecimal("35.50"), type = TransactionType.EXPENSE,
                category = defaultCategory, ledgerId = 1,
                date = LocalDateTime.of(2024, 3, 15, 14, 30, 0)
            )
        )
        fakeRepo.monthTransactions = transactions

        val outputStream = ByteArrayOutputStream()
        useCase(1L, YearMonth.of(2024, 3), outputStream)

        assertEquals(transactions, fakePrinter.printedTransactions)
        assertSame(outputStream, fakePrinter.printedOutputStream)
    }

    @Test
    fun `export with empty transactions calls printer with empty list`() = runTest {
        fakeRepo.monthTransactions = emptyList()

        val outputStream = ByteArrayOutputStream()
        useCase(1L, YearMonth.of(2024, 3), outputStream)

        assertTrue(fakePrinter.printedTransactions!!.isEmpty())
    }

    // --- Fakes ---

    private class FakeTransactionRepository : TransactionRepository {
        var monthTransactions: List<Transaction> = emptyList()

        override fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>> =
            flowOf(monthTransactions)

        override fun getByLedgerAndDateRange(
            ledgerId: Long, start: LocalDateTime, end: LocalDateTime
        ): Flow<List<Transaction>> = flowOf(emptyList())

        override fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> =
            flowOf(MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))

        override fun getExpenseByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>> =
            flowOf(emptyList())

        override suspend fun insert(transaction: Transaction): Long = 1L
        override suspend fun insertAll(transactions: List<Transaction>): List<Long> = emptyList()
        override suspend fun update(transaction: Transaction) {}
        override suspend fun delete(transaction: Transaction) {}
    }

    private class FakeCsvPrinter : CsvPrinter {
        var printedTransactions: List<Transaction>? = null
        var printedOutputStream: OutputStream? = null

        override fun print(transactions: List<Transaction>, outputStream: OutputStream) {
            printedTransactions = transactions
            printedOutputStream = outputStream
        }
    }
}
