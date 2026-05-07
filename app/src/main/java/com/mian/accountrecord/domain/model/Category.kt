package com.mian.accountrecord.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val type: TransactionType,
    val isPreset: Boolean = false,
    val sortOrder: Int = 0
)
