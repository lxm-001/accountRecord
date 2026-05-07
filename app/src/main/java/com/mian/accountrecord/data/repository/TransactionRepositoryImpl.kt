package com.mian.accountrecord.data.repository

import com.mian.accountrecord.data.local.db.CategoryDao
import com.mian.accountrecord.data.local.db.TransactionDao
import com.mian.accountrecord.data.mapper.toDomain
import com.mian.accountrecord.data.mapper.toEntity
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun getById(id: Long): Flow<Transaction?> {
        return transactionDao.getById(id)
            .combine(categoryDao.getAll()) { entity, categories ->
                if (entity == null) return@combine null
                val categoryMap = categories.associate { it.id to it.toDomain() }
                categoryMap[entity.categoryId]?.let { category ->
                    entity.toDomain(category)
                }
            }
    }

    override fun getByLedgerAndDateRange(
        ledgerId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Transaction>> {
        val zone = ZoneId.systemDefault()
        val startTime = start.atZone(zone).toInstant().toEpochMilli()
        val endTime = end.atZone(zone).toInstant().toEpochMilli()
        return transactionDao.getByLedgerAndDateRange(ledgerId, startTime, endTime)
            .combine(categoryDao.getAll()) { transactions, categories ->
                val categoryMap = categories.associate { it.id to it.toDomain() }
                transactions.mapNotNull { entity ->
                    categoryMap[entity.categoryId]?.let { category ->
                        entity.toDomain(category)
                    }
                }
            }
    }

    override fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>> {
        val (startTime, endTime) = yearMonthToRange(yearMonth)
        return transactionDao.getByLedgerAndDateRange(ledgerId, startTime, endTime)
            .combine(categoryDao.getAll()) { transactions, categories ->
                val categoryMap = categories.associate { it.id to it.toDomain() }
                transactions.mapNotNull { entity ->
                    categoryMap[entity.categoryId]?.let { category ->
                        entity.toDomain(category)
                    }
                }
            }
    }

    override fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> {
        val (startTime, endTime) = yearMonthToRange(yearMonth)
        return transactionDao.getSummaryByLedgerAndDateRange(ledgerId, startTime, endTime)
            .map { summaries ->
                var income = BigDecimal.ZERO
                var expense = BigDecimal.ZERO
                for (summary in summaries) {
                    val amount = BigDecimal(summary.total)
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                    when (summary.type) {
                        0 -> expense = amount
                        1 -> income = amount
                    }
                }
                MonthlySummary(
                    income = income,
                    expense = expense,
                    balance = income.subtract(expense)
                )
            }
    }

    override fun getExpenseByCategory(
        ledgerId: Long,
        yearMonth: YearMonth
    ): Flow<List<Pair<Long, BigDecimal>>> {
        val (startTime, endTime) = yearMonthToRange(yearMonth)
        return transactionDao.getExpenseByCategoryAndDateRange(ledgerId, startTime, endTime)
            .map { summaries ->
                summaries.map { summary ->
                    summary.categoryId to BigDecimal(summary.total)
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                }
            }
    }

    override fun getIncomeByCategory(
        ledgerId: Long,
        yearMonth: YearMonth
    ): Flow<List<Pair<Long, BigDecimal>>> {
        val (startTime, endTime) = yearMonthToRange(yearMonth)
        return transactionDao.getIncomeByCategoryAndDateRange(ledgerId, startTime, endTime)
            .map { summaries ->
                summaries.map { summary ->
                    summary.categoryId to BigDecimal(summary.total)
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                }
            }
    }

    override suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun insertAll(transactions: List<Transaction>): List<Long> {
        return transactionDao.insertAll(transactions.map { it.toEntity() })
    }

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    private fun yearMonthToRange(yearMonth: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startTime = yearMonth.atDay(1).atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        val endTime = yearMonth.plusMonths(1).atDay(1).atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        return startTime to endTime
    }
}
