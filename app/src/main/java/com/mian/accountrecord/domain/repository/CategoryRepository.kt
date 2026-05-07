package com.mian.accountrecord.domain.repository

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getByType(type: TransactionType): Flow<List<Category>>

    fun getAll(): Flow<List<Category>>

    suspend fun insert(category: Category): Long

    suspend fun update(category: Category)

    suspend fun delete(category: Category)

    suspend fun updateSortOrder(id: Long, order: Int)

    suspend fun migrateTransactions(sourceId: Long, targetId: Long)
}
