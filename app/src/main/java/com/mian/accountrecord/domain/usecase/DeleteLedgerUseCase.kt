package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.repository.LedgerRepository
import javax.inject.Inject

class DeleteLedgerUseCase @Inject constructor(
    private val repository: LedgerRepository
) {
    suspend operator fun invoke(ledger: Ledger): Result<Unit> {
        if (repository.count() <= 1) {
            return Result.failure(IllegalArgumentException("至少保留一个账本"))
        }
        repository.delete(ledger)
        return Result.success(Unit)
    }
}
