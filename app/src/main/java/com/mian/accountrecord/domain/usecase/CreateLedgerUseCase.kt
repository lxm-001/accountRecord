package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.repository.CategoryRepository
import com.mian.accountrecord.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateLedgerUseCase @Inject constructor(
    private val repository: LedgerRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(ledger: Ledger): Result<Long> {
        val existing = repository.getAll().first()
        val isDuplicate = existing.any { it.name == ledger.name }
        if (isDuplicate) {
            return Result.failure(IllegalArgumentException("该账本名称已存在"))
        }
        val id = repository.insert(ledger)
        return Result.success(id)
    }
}
