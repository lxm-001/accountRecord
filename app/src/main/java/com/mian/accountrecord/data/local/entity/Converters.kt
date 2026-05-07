package com.mian.accountrecord.data.local.entity

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room 类型转换器。
 * 处理 Room 不直接支持的类型与基本类型之间的转换。
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
