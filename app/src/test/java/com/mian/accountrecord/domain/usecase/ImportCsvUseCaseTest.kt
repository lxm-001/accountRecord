package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionSource
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.CategoryRepository
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.CsvParseResult
import com.mian.accountrecord.util.CsvParser
import com.mian.accountrecord.util.CsvSource
import com.mian.accountrecord.util.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class ImportCsvUseCaseTest {

    private lateinit var fakeCsvParser: FakeCsvParser
    private lateinit var fakeTransactionRepo: FakeTransactionRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var detectDuplicates: DetectDuplicatesUseCase
    private lateinit var useCase: ImportCsvUseCase

    private val expenseCategory = Category(
        id = 1, name = "餐饮", icon = "restaurant",
        color = "#FF5722", type = TransactionType.EXPENSE
    )
    private val incomeCategory = Category(
        id = 2, name = "工资", icon = "work",
        color = "#4CAF50", type = TransactionType.INCOME
    )
    private val otherExpense = Category(
        id = 3, name = "其他", icon = "more",
        color = "#9E9E9E", type = TransactionType.EXPENSE
    )
    private val otherIncome = Category(
        id = 4, name = "其他", icon = "more",
        color = "#9E9E9E", type = TransactionType.INCOME
    )

    @Before
    fun setup() {
        fakeCsvParser = FakeCsvParser()
        fakeTransactionRepo = FakeTransactionRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeCategoryRepo.categories = listOf(expenseCategory, incomeCategory, otherExpense, otherIncome)
        detectDuplicates = DetectDuplicatesUseCase(fakeTransactionRepo)
        useCase = ImportCsvUseCase(fakeCsvParser, detectDuplicates, fakeTransactionRepo, fakeCategoryRepo)
    }

    @Test
    fun `empty CSV returns zero counts`() = runTest {
        fakeCsvParser.result = CsvParseResult(emptyList(), emptyList())

        val result = useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals(0, result.importedCount)
        assertEquals(0, result.totalParsed)
        assertEquals(0, result.duplicateCount)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `parse errors are propagated in result`() = runTest {
        fakeCsvParser.result = CsvParseResult(emptyList(), listOf("文件格式不正确"))

        val result = useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals(0, result.importedCount)
        assertEquals(1, result.errors.size)
        assertEquals("文件格式不正确", result.errors[0])
    }

    @Test
    fun `successful import with matched category`() = runTest {
        val parsed = listOf(
            createParsedTransaction("35.50", "餐饮", TransactionType.EXPENSE)
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        val result = useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.totalParsed)
        assertEquals(0, result.duplicateCount)
        assertEquals(1, fakeTransactionRepo.insertedTransactions.size)
        assertEquals(expenseCategory, fakeTransactionRepo.insertedTransactions[0].category)
        assertEquals(TransactionSource.ALIPAY, fakeTransactionRepo.insertedTransactions[0].source)
    }

    @Test
    fun `unmatched category falls back to other`() = runTest {
        val parsed = listOf(
            createParsedTransaction("100.00", null, TransactionType.EXPENSE)
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        val result = useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals(1, result.importedCount)
        assertEquals(otherExpense, fakeTransactionRepo.insertedTransactions[0].category)
    }

    @Test
    fun `income transaction falls back to income other category`() = runTest {
        val parsed = listOf(
            createParsedTransaction("5000.00", null, TransactionType.INCOME)
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        val result = useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals(1, result.importedCount)
        assertEquals(otherIncome, fakeTransactionRepo.insertedTransactions[0].category)
    }

    @Test
    fun `duplicates are counted and excluded`() = runTest {
        val existingTx = Transaction(
            id = 1, amount = BigDecimal("35.50"), type = TransactionType.EXPENSE,
            category = expenseCategory, ledgerId = 1,
            date = LocalDateTime.of(2024, 3, 15, 14, 30, 0), note = "肯德基"
        )
        fakeTransactionRepo.existingTransactions = listOf(existingTx)

        val parsed = listOf(
            createParsedTransaction("35.50", "餐饮", TransactionType.EXPENSE,
                date = "2024-03-15T14:30:00", counterparty = "肯德基"),
            createParsedTransaction("100.00", "餐饮", TransactionType.EXPENSE,
                date = "2024-03-16T10:00:00", counterparty = "超市")
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        val result = useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals(1, result.importedCount)
        assertEquals(2, result.totalParsed)
        assertEquals(1, result.duplicateCount)
    }

    @Test
    fun `wechat source maps to WECHAT TransactionSource`() = runTest {
        val parsed = listOf(
            createParsedTransaction("50.00", "餐饮", TransactionType.EXPENSE)
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        val result = useCase(emptyInputStream(), CsvSource.WECHAT, 1L)

        assertEquals(1, result.importedCount)
        assertEquals(TransactionSource.WECHAT, fakeTransactionRepo.insertedTransactions[0].source)
    }

    @Test
    fun `counterparty is stored as note`() = runTest {
        val parsed = listOf(
            createParsedTransaction("35.50", "餐饮", TransactionType.EXPENSE,
                counterparty = "肯德基门店")
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertEquals("肯德基门店", fakeTransactionRepo.insertedTransactions[0].note)
    }

    @Test
    fun `blank counterparty results in null note`() = runTest {
        val parsed = listOf(
            createParsedTransaction("35.50", "餐饮", TransactionType.EXPENSE,
                counterparty = "")
        )
        fakeCsvParser.result = CsvParseResult(parsed, emptyList())

        useCase(emptyInputStream(), CsvSource.ALIPAY, 1L)

        assertNull(fakeTransactionRepo.insertedTransactions[0].note)
    }

    private fun emptyInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))

    private fun createParsedTransaction(
        amount: String,
        mappedCategoryName: String?,
        type: TransactionType,
        date: String = "2024-03-15T14:30:00",
        counterparty: String = "测试商户"
    ): ParsedTransaction {
        return ParsedTransaction(
            amount = BigDecimal(amount),
            type = type,
            date = LocalDateTime.parse(date),
            counterparty = counterparty,
            originalCategory = "原始分类",
            mappedCategoryName = mappedCategoryName
        )
    }

    // --- Fakes ---

    private class FakeCsvParser : CsvParser {
        var result = CsvParseResult(emptyList(), emptyList())
        override fun parse(inputStream: InputStream, source: CsvSource): CsvParseResult = result
    }

    private class FakeTransactionRepository : TransactionRepository {
        var existingTransactions: List<Transaction> = emptyList()
        val insertedTransactions = mutableListOf<Transaction>()

        override fun getByLedgerAndDateRange(
            ledgerId: Long, start: LocalDateTime, end: LocalDateTime
        ): Flow<List<Transaction>> = flowOf(existingTransactions)

        override fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>> =
            flowOf(emptyList())

        override fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> =
            flowOf(MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))

        override fun getExpenseByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>> =
            flowOf(emptyList())

        override suspend fun insert(transaction: Transaction): Long = 1L
        override suspend fun insertAll(transactions: List<Transaction>): List<Long> {
            insertedTransactions.addAll(transactions)
            return transactions.mapIndexed { i, _ -> (i + 1).toLong() }
        }
        override suspend fun update(transaction: Transaction) {}
        override suspend fun delete(transaction: Transaction) {}
    }

    private class FakeCategoryRepository : CategoryRepository {
        var categories: List<Category> = emptyList()

        override fun getByType(type: TransactionType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })

        override fun getAll(): Flow<List<Category>> = flowOf(categories)
        override suspend fun insert(category: Category): Long = 1L
        override suspend fun update(category: Category) {}
        override suspend fun delete(category: Category) {}
        override suspend fun updateSortOrder(id: Long, order: Int) {}
        override suspend fun migrateTransactions(sourceId: Long, targetId: Long) {}
    }
}
