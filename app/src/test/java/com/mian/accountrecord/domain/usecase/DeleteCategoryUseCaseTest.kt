package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeleteCategoryUseCaseTest {

    private lateinit var useCase: DeleteCategoryUseCase
    private lateinit var fakeRepo: FakeCategoryRepository

    private val otherCategory = Category(
        id = 99, name = "其他", icon = "more",
        color = "#999999", type = TransactionType.EXPENSE, isPreset = true
    )

    private val customCategory = Category(
        id = 10, name = "宠物", icon = "pets",
        color = "#FF9800", type = TransactionType.EXPENSE, isPreset = false
    )

    @Before
    fun setup() {
        fakeRepo = FakeCategoryRepository(
            categories = listOf(customCategory, otherCategory)
        )
        useCase = DeleteCategoryUseCase(fakeRepo)
    }

    @Test
    fun `preset category should return failure`() = runTest {
        val presetCategory = Category(
            id = 1, name = "餐饮", icon = "restaurant",
            color = "#FF5722", type = TransactionType.EXPENSE, isPreset = true
        )
        val result = useCase(presetCategory)
        assertTrue(result.isFailure)
        assertEquals("不能删除预设分类", result.exceptionOrNull()?.message)
    }

    @Test
    fun `custom category should migrate transactions then delete`() = runTest {
        val result = useCase(customCategory)
        assertTrue(result.isSuccess)
        assertEquals(customCategory.id, fakeRepo.lastMigrateSourceId)
        assertEquals(otherCategory.id, fakeRepo.lastMigrateTargetId)
        assertTrue(fakeRepo.deletedCategories.contains(customCategory))
    }

    @Test
    fun `migrateTransactions should be called with correct source and target ids`() = runTest {
        useCase(customCategory)
        assertEquals(10L, fakeRepo.lastMigrateSourceId)
        assertEquals(99L, fakeRepo.lastMigrateTargetId)
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>
    ) : CategoryRepository {
        var lastMigrateSourceId: Long? = null
        var lastMigrateTargetId: Long? = null
        val deletedCategories = mutableListOf<Category>()

        override fun getByType(type: TransactionType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })

        override fun getAll(): Flow<List<Category>> = flowOf(categories)
        override suspend fun insert(category: Category): Long = 0L
        override suspend fun update(category: Category) {}
        override suspend fun delete(category: Category) { deletedCategories.add(category) }
        override suspend fun updateSortOrder(id: Long, order: Int) {}
        override suspend fun migrateTransactions(sourceId: Long, targetId: Long) {
            lastMigrateSourceId = sourceId
            lastMigrateTargetId = targetId
        }
    }
}
