package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.AlertType
import com.mian.accountrecord.domain.model.Budget
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.BudgetRepository
import com.mian.accountrecord.domain.repository.CategoryRepository
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

class CheckBudgetAlertUseCaseTest {

    private val yearMonth = YearMonth.of(2026, 4)
    private val ledgerId = 1L

    private val category = Category(
        id = 1, name = "餐饮", icon = "restaurant",
        color = "#FF5722", type = TransactionType.EXPENSE
    )

    private fun createUseCase(
        budgetAmount: BigDecimal,
        spentAmount: BigDecimal,
        categoryId: Long? = 1L
    ): CheckBudgetAlertUseCase {
        val budgetRepo = FakeBudgetRepository(
            budgets = listOf(Budget(id = 1, ledgerId = ledgerId, categoryId = categoryId, amount = budgetAmount, yearMonth = yearMonth))
        )
        val transactionRepo = FakeTransactionRepository(
            expenseByCategory = if (categoryId != null) listOf(categoryId to spentAmount) else emptyList(),
            summary = MonthlySummary(BigDecimal.ZERO, spentAmount, BigDecimal.ZERO.subtract(spentAmount))
        )
        val categoryRepo = FakeCategoryRepository(categories = listOf(category))
        return CheckBudgetAlertUseCase(budgetRepo, transactionRepo, categoryRepo)
    }

    @Test
    fun `ratio 50 percent should produce no alert`() = runTest {
        val useCase = createUseCase(
            budgetAmount = BigDecimal("1000"),
            spentAmount = BigDecimal("500")
        )
        val alerts = useCase(ledgerId, yearMonth)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `ratio 80 percent should produce WARNING alert`() = runTest {
        val useCase = createUseCase(
            budgetAmount = BigDecimal("1000"),
            spentAmount = BigDecimal("800")
        )
        val alerts = useCase(ledgerId, yearMonth)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.WARNING, alerts[0].type)
        assertEquals(0.8f, alerts[0].ratio, 0.01f)
    }

    @Test
    fun `ratio 100 percent should produce OVERSPENT alert`() = runTest {
        val useCase = createUseCase(
            budgetAmount = BigDecimal("1000"),
            spentAmount = BigDecimal("1000")
        )
        val alerts = useCase(ledgerId, yearMonth)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.OVERSPENT, alerts[0].type)
        assertEquals(1.0f, alerts[0].ratio, 0.01f)
    }

    @Test
    fun `ratio 150 percent should produce OVERSPENT alert`() = runTest {
        val useCase = createUseCase(
            budgetAmount = BigDecimal("1000"),
            spentAmount = BigDecimal("1500")
        )
        val alerts = useCase(ledgerId, yearMonth)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.OVERSPENT, alerts[0].type)
        assertEquals(1.5f, alerts[0].ratio, 0.01f)
    }

    @Test
    fun `total budget with overspent should use summary expense`() = runTest {
        val useCase = createUseCase(
            budgetAmount = BigDecimal("2000"),
            spentAmount = BigDecimal("2000"),
            categoryId = null // total budget
        )
        val alerts = useCase(ledgerId, yearMonth)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.OVERSPENT, alerts[0].type)
        assertEquals("本月总预算", alerts[0].categoryName)
    }

    private class FakeBudgetRepository(
        private val budgets: List<Budget>
    ) : BudgetRepository {
        override fun getByMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Budget>> = flowOf(budgets)
        override fun getTotalBudget(ledgerId: Long, yearMonth: YearMonth): Flow<Budget?> =
            flowOf(budgets.firstOrNull { it.categoryId == null })
        override suspend fun upsert(budget: Budget) {}
        override suspend fun delete(budget: Budget) {}
    }

    private class FakeTransactionRepository(
        private val expenseByCategory: List<Pair<Long, BigDecimal>> = emptyList(),
        private val summary: MonthlySummary = MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    ) : TransactionRepository {
        override fun getByLedgerAndDateRange(
            ledgerId: Long,
            start: java.time.LocalDateTime,
            end: java.time.LocalDateTime
        ): Flow<List<Transaction>> = flowOf(emptyList())

        override fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>> =
            flowOf(emptyList())
        override fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> =
            flowOf(summary)
        override fun getExpenseByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>> =
            flowOf(expenseByCategory)
        override suspend fun insert(transaction: Transaction): Long = 0L
        override suspend fun insertAll(transactions: List<Transaction>): List<Long> = emptyList()
        override suspend fun update(transaction: Transaction) {}
        override suspend fun delete(transaction: Transaction) {}
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>
    ) : CategoryRepository {
        override fun getByType(type: TransactionType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })
        override fun getAll(): Flow<List<Category>> = flowOf(categories)
        override suspend fun insert(category: Category): Long = 0L
        override suspend fun update(category: Category) {}
        override suspend fun delete(category: Category) {}
        override suspend fun updateSortOrder(id: Long, order: Int) {}
        override suspend fun migrateTransactions(sourceId: Long, targetId: Long) {}
    }
}
