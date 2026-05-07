package com.mian.accountrecord.domain.model

data class BudgetAlert(
    val categoryId: Long?,
    val categoryName: String,
    val type: AlertType,
    val ratio: Float
)

enum class AlertType { WARNING, OVERSPENT }
