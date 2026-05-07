package com.mian.accountrecord.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"]
        ),
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("category_id"), Index("ledger_id")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "ledger_id") val ledgerId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null, // null = 月度总预算
    val amount: Long,              // 预算金额，单位：分
    @ColumnInfo(name = "year_month") val yearMonth: String // 格式 "2026-04"
)
