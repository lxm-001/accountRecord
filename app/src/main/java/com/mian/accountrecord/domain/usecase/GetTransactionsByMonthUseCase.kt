package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GetTransactionsByMonthUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(ledgerId: Long, yearMonth: YearMonth): Flow<Map<LocalDate, List<Transaction>>> {
        return repository.getByLedgerAndMonth(ledgerId, yearMonth)
            .map { transactions ->
                transactions
                    .groupBy { it.date.toLocalDate() }
                    .toSortedMap(compareByDescending { it })
            }
    }
}
