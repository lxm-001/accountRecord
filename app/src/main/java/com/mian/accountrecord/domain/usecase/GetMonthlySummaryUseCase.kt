package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlySummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(ledgerId: Long, yearMonth: YearMonth): Flow<MonthlySummary> {
        return repository.getMonthlySummary(ledgerId, yearMonth)
    }
}
