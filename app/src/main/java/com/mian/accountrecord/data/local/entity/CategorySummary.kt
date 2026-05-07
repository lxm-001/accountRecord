package com.mian.accountrecord.data.local.entity

import androidx.room.ColumnInfo

/**
 * 按分类汇总的查询结果类。
 * 用于 TransactionDao.getExpenseByCategoryAndDateRange 查询。
 */
data class CategorySummary(
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val total: Long
)
