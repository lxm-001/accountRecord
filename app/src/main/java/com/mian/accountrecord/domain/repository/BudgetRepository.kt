package com.mian.accountrecord.domain.repository

import com.mian.accountrecord.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface BudgetRepository {

    fun getByMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Budget>>

    fun getTotalBudget(ledgerId: Long, yearMonth: YearMonth): Flow<Budget?>

    suspend fun upsert(budget: Budget)

    suspend fun delete(budget: Budget)
}
