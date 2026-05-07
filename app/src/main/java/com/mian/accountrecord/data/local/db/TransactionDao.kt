package com.mian.accountrecord.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mian.accountrecord.data.local.entity.CategorySummary
import com.mian.accountrecord.data.local.entity.TransactionEntity
import com.mian.accountrecord.data.local.entity.TypeSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT * FROM transactions 
        WHERE ledger_id = :ledgerId 
        AND date >= :startTime AND date < :endTime 
        ORDER BY date DESC
    """)
    fun getByLedgerAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT type, SUM(amount) as total 
        FROM transactions 
        WHERE ledger_id = :ledgerId 
        AND date >= :startTime AND date < :endTime 
        GROUP BY type
    """)
    fun getSummaryByLedgerAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<TypeSummary>>

    @Query("""
        SELECT category_id, SUM(amount) as total 
        FROM transactions 
        WHERE ledger_id = :ledgerId AND type = 0
        AND date >= :startTime AND date < :endTime 
        GROUP BY category_id ORDER BY total DESC
    """)
    fun getExpenseByCategoryAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<CategorySummary>>

    @Query("""
        SELECT category_id, SUM(amount) as total 
        FROM transactions 
        WHERE ledger_id = :ledgerId AND type = 1
        AND date >= :startTime AND date < :endTime 
        GROUP BY category_id ORDER BY total DESC
    """)
    fun getIncomeByCategoryAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<CategorySummary>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): Flow<TransactionEntity?>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
