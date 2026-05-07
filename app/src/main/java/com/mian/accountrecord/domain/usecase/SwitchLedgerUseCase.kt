package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.repository.LedgerRepository
import javax.inject.Inject

class SwitchLedgerUseCase @Inject constructor(
    private val repository: LedgerRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.switchTo(id)
    }
}
