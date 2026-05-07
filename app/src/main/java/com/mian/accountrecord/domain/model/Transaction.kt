package com.mian.accountrecord.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val amount: BigDecimal,
    val type: TransactionType,
    val category: Category,
    val ledgerId: Long,
    val date: LocalDateTime,
    val note: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL
)
