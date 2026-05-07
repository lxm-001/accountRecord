package com.mian.accountrecord.data.mapper

import com.mian.accountrecord.data.local.entity.BudgetEntity
import com.mian.accountrecord.domain.model.Budget
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    ledgerId = ledgerId,
    categoryId = categoryId,
    amount = BigDecimal(amount).divide(BigDecimal(100), 2, RoundingMode.HALF_UP),
    yearMonth = YearMonth.parse(yearMonth)
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    ledgerId = ledgerId,
    categoryId = categoryId,
    amount = amount.multiply(BigDecimal(100)).toLong(),
    yearMonth = yearMonth.toString()
)
