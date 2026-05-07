package com.mian.accountrecord.domain.model

import java.math.BigDecimal

data class MonthlySummary(
    val income: BigDecimal,
    val expense: BigDecimal,
    val balance: BigDecimal
)
