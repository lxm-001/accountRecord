package com.mian.accountrecord.domain.model

import java.math.BigDecimal

data class BudgetProgress(
    val budget: Budget,
    val spent: BigDecimal,
    val ratio: Float
)
