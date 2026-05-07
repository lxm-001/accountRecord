package com.mian.accountrecord.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mian.accountrecord.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE ledger_id = :ledgerId AND year_month = :yearMonth")
    fun getByMonth(ledgerId: Long, yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE ledger_id = :ledgerId AND year_month = :yearMonth AND category_id IS NULL LIMIT 1")
    fun getTotalBudget(ledgerId: Long, yearMonth: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}
