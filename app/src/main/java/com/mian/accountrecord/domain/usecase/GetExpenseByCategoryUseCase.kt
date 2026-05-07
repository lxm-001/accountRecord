package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

class GetExpenseByCategoryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(ledgerId: Long, yearMonth: YearMonth): Flow<List<Pair<Long, BigDecimal>>> {
        return repository.getExpenseByCategory(ledgerId, yearMonth)
    }
}
