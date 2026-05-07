package com.mian.accountrecord.domain.model

data class Ledger(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val template: LedgerTemplate = LedgerTemplate.CUSTOM,
    val isActive: Boolean = false
)
