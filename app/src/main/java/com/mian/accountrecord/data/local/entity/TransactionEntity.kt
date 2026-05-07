package com.mian.accountrecord.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"]
        ),
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"]
        )
    ],
    indices = [Index("category_id"), Index("ledger_id"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,                          // 金额，单位：分
    val type: Int,                             // 0=支出, 1=收入
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "ledger_id") val ledgerId: Long,
    val date: Long,                            // 时间戳（毫秒）
    val note: String? = null,
    @ColumnInfo(name = "source") val source: String = "manual", // manual / alipay / wechat
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
