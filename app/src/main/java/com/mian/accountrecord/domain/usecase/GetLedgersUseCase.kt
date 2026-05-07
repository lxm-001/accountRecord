package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLedgersUseCase @Inject constructor(
    private val repository: LedgerRepository
) {
    operator fun invoke(): Flow<List<Ledger>> {
        return repository.getAll()
    }
}
