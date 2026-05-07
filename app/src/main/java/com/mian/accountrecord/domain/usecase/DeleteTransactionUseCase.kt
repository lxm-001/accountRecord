package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.delete(transaction)
    }
}
