package com.mian.accountrecord.data.local.entity

/**
 * 按类型（收入/支出）汇总的查询结果类。
 * 用于 TransactionDao.getSummaryByLedgerAndDateRange 查询。
 */
data class TypeSummary(
    val type: Int,
    val total: Long
)
