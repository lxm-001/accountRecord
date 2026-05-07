package com.mian.accountrecord.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mian.accountrecord.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sort_order ASC")
    fun getByType(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Transaction
    @Query("UPDATE transactions SET category_id = :targetId WHERE category_id = :sourceId")
    suspend fun migrateTransactions(sourceId: Long, targetId: Long)
}
