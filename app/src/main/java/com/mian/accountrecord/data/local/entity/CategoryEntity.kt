package com.mian.accountrecord.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,              // Material Icon 名称
    val color: String,             // Hex 颜色值 "#FF5722"
    val type: Int,                 // 0=支出, 1=收入
    @ColumnInfo(name = "is_preset") val isPreset: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)
