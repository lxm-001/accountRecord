package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.AlertType
import com.mian.accountrecord.domain.model.BudgetAlert
import com.mian.accountrecord.domain.repository.BudgetRepository
import com.mian.accountrecord.domain.repository.CategoryRepository
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import javax.inject.Inject

class CheckBudgetAlertUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(ledgerId: Long, yearMonth: YearMonth): List<BudgetAlert> {
        val budgets = budgetRepository.getByMonth(ledgerId, yearMonth).first()
        val expenses = transactionRepository.getExpenseByCategory(ledgerId, yearMonth).first()
        val summary = transactionRepository.getMonthlySummary(ledgerId, yearMonth).first()
        val categories = categoryRepository.getAll().first()
        val categoryMap = categories.associate { it.id to it.name }
        val expenseMap = expenses.toMap()

        val alerts = mutableListOf<BudgetAlert>()

        for (budget in budgets) {
            val spent = if (budget.categoryId == null) {
                summary.expense
            } else {
                expenseMap[budget.categoryId] ?: BigDecimal.ZERO
            }

            if (budget.amount.compareTo(BigDecimal.ZERO) == 0) continue

            val ratio = spent.divide(budget.amount, 4, RoundingMode.HALF_UP).toFloat()
            val categoryName = if (budget.categoryId == null) {
                "本月总预算"
            } else {
                categoryMap[budget.categoryId] ?: "未知分类"
            }

            when {
                ratio >= 1.0f -> alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        type = AlertType.OVERSPENT,
                        ratio = ratio
                    )
                )
                ratio >= 0.8f -> alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        type = AlertType.WARNING,
                        ratio = ratio
                    )
                )
            }
        }

        return alerts
    }
}
