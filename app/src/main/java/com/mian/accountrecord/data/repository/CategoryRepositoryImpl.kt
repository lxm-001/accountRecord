package com.mian.accountrecord.data.repository

import com.mian.accountrecord.data.local.db.CategoryDao
import com.mian.accountrecord.data.mapper.toDomain
import com.mian.accountrecord.data.mapper.toEntity
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getByType(type: TransactionType): Flow<List<Category>> {
        val typeInt = if (type == TransactionType.EXPENSE) 0 else 1
        return categoryDao.getByType(typeInt).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAll(): Flow<List<Category>> {
        return categoryDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insert(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun update(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun delete(category: Category) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun updateSortOrder(id: Long, order: Int) {
        categoryDao.updateSortOrder(id, order)
    }

    override suspend fun migrateTransactions(sourceId: Long, targetId: Long) {
        categoryDao.migrateTransactions(sourceId, targetId)
    }
}
