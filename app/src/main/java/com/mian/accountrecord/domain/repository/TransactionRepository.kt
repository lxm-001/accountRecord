package com.mian.accountrecord.domain.repository

import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

interface TransactionRepository {

    fun getById(id: Long): Flow<Transaction?>

    fun getByLedgerAndMonth(ledgerId: Long, yearMonth: YearMonth): Flow<List<Transaction>>

    fun getByLedgerAndDateRange(ledgerId: Long, start: LocalDateTime, end: LocalDateTime): Flow<List<Transaction>>

    fun getMonthlySummary(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary>

    fun getExpenseByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>>

    fun getIncomeByCategory(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>>

    suspend fun insert(transaction: Transaction): Long

    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    suspend fun update(transaction: Transaction)

    suspend fun delete(transaction: Transaction)
}
