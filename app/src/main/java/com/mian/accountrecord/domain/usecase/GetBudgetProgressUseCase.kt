package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.BudgetProgress
import com.mian.accountrecord.domain.repository.BudgetRepository
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import javax.inject.Inject

class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(ledgerId: Long, yearMonth: YearMonth): Flow<List<BudgetProgress>> {
        return combine(
            budgetRepository.getByMonth(ledgerId, yearMonth),
            transactionRepository.getExpenseByCategory(ledgerId, yearMonth),
            transactionRepository.getMonthlySummary(ledgerId, yearMonth)
        ) { budgets, expenses, summary ->
            val expenseMap = expenses.toMap()
            budgets.map { budget ->
                val spent = if (budget.categoryId == null) {
                    summary.expense
                } else {
                    expenseMap[budget.categoryId] ?: BigDecimal.ZERO
                }
                val ratio = if (budget.amount.compareTo(BigDecimal.ZERO) == 0) {
                    0f
                } else {
                    spent.divide(budget.amount, 4, RoundingMode.HALF_UP).toFloat()
                }
                BudgetProgress(budget = budget, spent = spent, ratio = ratio)
            }
        }
    }
}
