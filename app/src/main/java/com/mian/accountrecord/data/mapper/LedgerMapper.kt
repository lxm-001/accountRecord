package com.mian.accountrecord.data.mapper

import com.mian.accountrecord.data.local.entity.LedgerEntity
import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.model.LedgerTemplate

fun LedgerEntity.toDomain(): Ledger = Ledger(
    id = id,
    name = name,
    icon = icon,
    template = LedgerTemplate.valueOf(template.uppercase()),
    isActive = isActive
)

fun Ledger.toEntity(): LedgerEntity = LedgerEntity(
    id = id,
    name = name,
    icon = icon,
    template = template.name.lowercase(),
    isActive = isActive
)
