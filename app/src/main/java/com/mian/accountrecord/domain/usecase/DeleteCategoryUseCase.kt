package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        if (category.isPreset) {
            return Result.failure(IllegalArgumentException("不能删除预设分类"))
        }
        val allCategories = repository.getByType(category.type).first()
        val otherCategory = allCategories.first { it.name == "其他" && it.type == category.type }
        repository.migrateTransactions(category.id, otherCategory.id)
        repository.delete(category)
        return Result.success(Unit)
    }
}
