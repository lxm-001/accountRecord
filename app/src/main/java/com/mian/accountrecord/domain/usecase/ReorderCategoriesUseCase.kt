package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.repository.CategoryRepository
import javax.inject.Inject

class ReorderCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categories: List<Category>) {
        categories.forEachIndexed { index, category ->
            repository.updateSortOrder(category.id, index)
        }
    }
}
