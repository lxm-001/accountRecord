package com.mian.accountrecord.data.mapper

import com.mian.accountrecord.data.local.entity.TransactionEntity
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionSource
import com.mian.accountrecord.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun TransactionEntity.toDomain(category: Category): Transaction = Transaction(
    id = id,
    amount = BigDecimal(amount).divide(BigDecimal(100), 2, RoundingMode.HALF_UP),
    type = if (type == 0) TransactionType.EXPENSE else TransactionType.INCOME,
    category = category,
    ledgerId = ledgerId,
    date = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()),
    note = note,
    source = TransactionSource.valueOf(source.uppercase())
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount.multiply(BigDecimal(100)).toLong(),
    type = if (type == TransactionType.EXPENSE) 0 else 1,
    categoryId = category.id,
    ledgerId = ledgerId,
    date = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    note = note,
    source = source.name.lowercase()
)
