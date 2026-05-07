package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.model.TrendPoint
import com.mian.accountrecord.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

class GetTrendDataUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(ledgerId: Long, yearMonth: YearMonth): Flow<List<TrendPoint>> {
        return repository.getByLedgerAndMonth(ledgerId, yearMonth)
            .map { transactions ->
                transactions
                    .groupBy { it.date.toLocalDate() }
                    .map { (date, dayTransactions) ->
                        TrendPoint(
                            date = date,
                            income = dayTransactions
                                .filter { it.type == TransactionType.INCOME }
                                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount },
                            expense = dayTransactions
                                .filter { it.type == TransactionType.EXPENSE }
                                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                        )
                    }
                    .sortedBy { it.date }
            }
    }
}
