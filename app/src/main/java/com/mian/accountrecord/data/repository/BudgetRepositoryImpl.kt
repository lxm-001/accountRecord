package com.mian.accountrecord.data.repository

import com.mian.accountrecord.data.local.db.BudgetDao
import com.mian.accountrecord.data.mapper.toDomain
import com.mian.accountrecord.data.mapper.toEntity
import com.mian.accountrecord.domain.model.Budget
import com.mian.accountrecord.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getByMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Budget>> {
        return budgetDao.getByMonth(ledgerId, yearMonth.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalBudget(ledgerId: Long, yearMonth: YearMonth): Flow<Budget?> {
        return budgetDao.getTotalBudget(ledgerId, yearMonth.toString()).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun upsert(budget: Budget) {
        budgetDao.upsert(budget.toEntity())
    }

    override suspend fun delete(budget: Budget) {
        budgetDao.delete(budget.toEntity())
    }
}
