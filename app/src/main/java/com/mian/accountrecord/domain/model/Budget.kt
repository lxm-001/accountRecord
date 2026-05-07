package com.mian.accountrecord.domain.model

import java.math.BigDecimal
import java.time.YearMonth

data class Budget(
    val id: Long = 0,
    val ledgerId: Long,
    val categoryId: Long? = null,
    val amount: BigDecimal,
    val yearMonth: YearMonth
)
