package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.TransactionType
import com.opencsv.CSVReader
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CsvParserImpl @Inject constructor() : CsvParser {

    companion object {
        val CATEGORY_MAPPING = mapOf(
            "餐饮美食" to "餐饮", "交通出行" to "交通", "充值缴费" to "通讯",
            "日用百货" to "日用", "服饰装扮" to "服饰", "医疗健康" to "医疗",
            "文化休闲" to "娱乐", "教育培训" to "教育", "住房缴费" to "住房",
            "商户消费" to "购物", "转账" to "社交", "红包" to "红包",
            "群收款" to "社交"
        )

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        )

        // Alipay header columns
        private const val ALIPAY_HEADER_MARKER = "交易时间"
        private const val ALIPAY_COL_DATE = "交易时间"
        private const val ALIPAY_COL_CATEGORY = "交易分类"
        private const val ALIPAY_COL_COUNTERPARTY = "交易对方"
        private const val ALIPAY_COL_AMOUNT = "金额（元）"
        private const val ALIPAY_COL_TYPE = "收/支"

        // WeChat header columns
        private const val WECHAT_HEADER_MARKER = "交易时间"
        private const val WECHAT_COL_DATE = "交易时间"
        private const val WECHAT_COL_CATEGORY = "交易类型"
        private const val WECHAT_COL_COUNTERPARTY = "交易对方"
        private const val WECHAT_COL_AMOUNT = "金额(元)"
        private const val WECHAT_COL_TYPE = "收/支"
    }

    override fun parse(inputStream: InputStream, source: CsvSource): CsvParseResult {
        val errors = mutableListOf<String>()
        val transactions = mutableListOf<ParsedTransaction>()

        try {
            val charset = when (source) {
                CsvSource.ALIPAY -> Charsets.UTF_8.let {
                    try {
                        charset("GBK")
                    } catch (_: Exception) {
                        it
                    }
                }
                CsvSource.WECHAT -> Charsets.UTF_8
            }

            val reader = CSVReader(InputStreamReader(inputStream, charset))
            val allRows = reader.readAll()
            reader.close()

            if (allRows.isEmpty()) {
                errors.add("文件为空")
                return CsvParseResult(emptyList(), errors)
            }

            // Find the header row
            val headerIndex = findHeaderRow(allRows, source)
            if (headerIndex < 0) {
                errors.add("无法识别文件格式：未找到表头行")
                return CsvParseResult(emptyList(), errors)
            }

            val headerRow = allRows[headerIndex].map { it.trim() }
            val columnMap = buildColumnMap(headerRow, source)

            if (columnMap == null) {
                errors.add("文件格式不正确：缺少必要的列")
                return CsvParseResult(emptyList(), errors)
            }

            // Parse data rows
            for (i in (headerIndex + 1) until allRows.size) {
                val row = allRows[i]
                if (row.isEmpty() || row.all { it.isBlank() }) continue

                try {
                    val parsed = parseRow(row, columnMap, source, i + 1)
                    if (parsed != null) {
                        transactions.add(parsed)
                    }
                } catch (e: Exception) {
                    errors.add("第${i + 1}行解析失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            errors.add("文件读取失败: ${e.message}")
        }

        return CsvParseResult(transactions, errors)
    }

    private fun findHeaderRow(rows: List<Array<String>>, source: CsvSource): Int {
        val marker = when (source) {
            CsvSource.ALIPAY -> ALIPAY_HEADER_MARKER
            CsvSource.WECHAT -> WECHAT_HEADER_MARKER
        }
        return rows.indexOfFirst { row ->
            row.any { it.trim().contains(marker) }
        }
    }

    private data class ColumnMap(
        val dateIndex: Int,
        val categoryIndex: Int,
        val counterpartyIndex: Int,
        val amountIndex: Int,
        val typeIndex: Int
    )

    private fun buildColumnMap(header: List<String>, source: CsvSource): ColumnMap? {
        val dateCol: String
        val categoryCol: String
        val counterpartyCol: String
        val amountCol: String
        val typeCol: String

        when (source) {
            CsvSource.ALIPAY -> {
                dateCol = ALIPAY_COL_DATE
                categoryCol = ALIPAY_COL_CATEGORY
                counterpartyCol = ALIPAY_COL_COUNTERPARTY
                amountCol = ALIPAY_COL_AMOUNT
                typeCol = ALIPAY_COL_TYPE
            }
            CsvSource.WECHAT -> {
                dateCol = WECHAT_COL_DATE
                categoryCol = WECHAT_COL_CATEGORY
                counterpartyCol = WECHAT_COL_COUNTERPARTY
                amountCol = WECHAT_COL_AMOUNT
                typeCol = WECHAT_COL_TYPE
            }
        }

        val dateIndex = header.indexOfFirst { it.contains(dateCol) }
        val categoryIndex = header.indexOfFirst { it.contains(categoryCol) }
        val counterpartyIndex = header.indexOfFirst { it.contains(counterpartyCol) }
        val amountIndex = header.indexOfFirst { it.contains(amountCol) }
        val typeIndex = header.indexOfFirst { it.contains(typeCol) }

        if (dateIndex < 0 || amountIndex < 0 || typeIndex < 0) return null

        return ColumnMap(
            dateIndex = dateIndex,
            categoryIndex = categoryIndex,
            counterpartyIndex = counterpartyIndex,
            amountIndex = amountIndex,
            typeIndex = typeIndex
        )
    }

    private fun parseRow(
        row: Array<String>,
        columnMap: ColumnMap,
        source: CsvSource,
        rowNumber: Int
    ): ParsedTransaction? {
        val typeStr = row.getOrNull(columnMap.typeIndex)?.trim() ?: return null

        // Filter out "不计收支" transactions
        if (typeStr == "不计收支" || typeStr.isEmpty()) return null

        val type = when {
            typeStr.contains("支出") -> TransactionType.EXPENSE
            typeStr.contains("收入") -> TransactionType.INCOME
            else -> return null // skip unknown types
        }

        val dateStr = row.getOrNull(columnMap.dateIndex)?.trim()
            ?: throw IllegalArgumentException("缺少交易时间")
        val date = parseDate(dateStr)
            ?: throw IllegalArgumentException("无法解析日期: $dateStr")

        val amountStr = row.getOrNull(columnMap.amountIndex)?.trim()
            ?: throw IllegalArgumentException("缺少金额")
        val amount = parseAmount(amountStr)
            ?: throw IllegalArgumentException("无法解析金额: $amountStr")

        val counterparty = row.getOrNull(columnMap.counterpartyIndex)?.trim() ?: ""
        val originalCategory = row.getOrNull(columnMap.categoryIndex)?.trim() ?: ""
        val mappedCategoryName = CATEGORY_MAPPING[originalCategory]

        return ParsedTransaction(
            amount = amount,
            type = type,
            date = date,
            counterparty = counterparty,
            originalCategory = originalCategory,
            mappedCategoryName = mappedCategoryName,
            mappedCategoryId = null,
            isDuplicate = false,
            isSelected = true
        )
    }

    private fun parseDate(dateStr: String): LocalDateTime? {
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr.trim(), formatter)
            } catch (_: Exception) {
                // try next formatter
            }
        }
        return null
    }

    private fun parseAmount(amountStr: String): BigDecimal? {
        return try {
            val cleaned = amountStr
                .replace("¥", "")
                .replace("￥", "")
                .replace(",", "")
                .replace(" ", "")
                .trim()
            if (cleaned.isEmpty()) return null
            BigDecimal(cleaned).abs()
        } catch (_: Exception) {
            null
        }
    }
}
