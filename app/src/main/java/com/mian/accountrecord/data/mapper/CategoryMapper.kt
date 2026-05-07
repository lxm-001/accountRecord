package com.mian.accountrecord.data.mapper

import com.mian.accountrecord.data.local.entity.CategoryEntity
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = if (type == 0) TransactionType.EXPENSE else TransactionType.INCOME,
    isPreset = isPreset,
    sortOrder = sortOrder
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = if (type == TransactionType.EXPENSE) 0 else 1,
    isPreset = isPreset,
    sortOrder = sortOrder
)
