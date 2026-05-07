package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Long> {
        val existing = repository.getByType(category.type).first()
        val isDuplicate = existing.any { it.name == category.name }
        if (isDuplicate) {
            return Result.failure(IllegalArgumentException("该分类名称已存在"))
        }
        val id = repository.insert(category)
        return Result.success(id)
    }
}
