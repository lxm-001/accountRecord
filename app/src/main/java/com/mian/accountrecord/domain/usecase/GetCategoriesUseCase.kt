package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> {
        return repository.getByType(type)
    }
}
