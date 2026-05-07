package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        if (transaction.amount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("金额必须大于0"))
        }
        if (transaction.amount > BigDecimal("99999999.99")) {
            return Result.failure(IllegalArgumentException("金额超出上限"))
        }
        val id = repository.insert(transaction)
        return Result.success(id)
    }
}
