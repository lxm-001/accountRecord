package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Budget
import com.mian.accountrecord.domain.repository.BudgetRepository
import javax.inject.Inject

class SetBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) {
        repository.upsert(budget)
    }
}
