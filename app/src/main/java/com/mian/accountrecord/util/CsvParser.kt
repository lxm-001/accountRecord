package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.TransactionType
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDateTime

interface CsvParser {
    fun parse(inputStream: InputStream, source: CsvSource): CsvParseResult
}

enum class CsvSource { ALIPAY, WECHAT }

data class CsvParseResult(
    val transactions: List<ParsedTransaction>,
    val errors: List<String>
)

data class ParsedTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val date: LocalDateTime,
    val counterparty: String,
    val originalCategory: String,
    val mappedCategoryName: String?,
    val mappedCategoryId: Long? = null,
    val isDuplicate: Boolean = false,
    val isSelected: Boolean = true
)
