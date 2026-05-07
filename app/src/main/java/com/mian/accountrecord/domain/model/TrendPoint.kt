package com.mian.accountrecord.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class TrendPoint(
    val date: LocalDate,
    val income: BigDecimal,
    val expense: BigDecimal
)
