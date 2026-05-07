package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CsvParserImplTest {

    private lateinit var parser: CsvParserImpl

    @Before
    fun setup() {
        parser = CsvParserImpl()
    }

    // --- Alipay CSV Tests ---

    @Test
    fun `parse alipay csv with valid data`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024-03-15 14:30:00", "餐饮美食", "肯德基", "35.50", "支出"),
                arrayOf("2024-03-16 09:00:00", "工资", "公司", "10000.00", "收入")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals(0, result.errors.size)
        assertEquals(2, result.transactions.size)

        val expense = result.transactions[0]
        assertEquals(BigDecimal("35.50"), expense.amount)
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals("肯德基", expense.counterparty)
        assertEquals("餐饮美食", expense.originalCategory)
        assertEquals("餐饮", expense.mappedCategoryName)
        assertNull(expense.mappedCategoryId)
        assertEquals(LocalDateTime.of(2024, 3, 15, 14, 30, 0), expense.date)

        val income = result.transactions[1]
        assertEquals(BigDecimal("10000.00"), income.amount)
        assertEquals(TransactionType.INCOME, income.type)
    }

    @Test
    fun `parse alipay csv filters out 不计收支`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024-03-15 14:30:00", "餐饮美食", "肯德基", "35.50", "支出"),
                arrayOf("2024-03-15 15:00:00", "转账", "余额宝", "1000.00", "不计收支")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals(0, result.errors.size)
        assertEquals(1, result.transactions.size)
        assertEquals("肯德基", result.transactions[0].counterparty)
    }

    @Test
    fun `parse alipay csv with amount containing special chars`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024-03-15 14:30:00", "餐饮美食", "餐厅", "¥1,234.56", "支出")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals(0, result.errors.size)
        assertEquals(1, result.transactions.size)
        assertEquals(BigDecimal("1234.56"), result.transactions[0].amount)
    }

    // --- WeChat CSV Tests ---

    @Test
    fun `parse wechat csv with valid data`() {
        val csv = buildWechatCSV(
            listOf(
                arrayOf("2024-03-15 14:30:00", "商户消费", "美团外卖", "¥28.90", "支出"),
                arrayOf("2024-03-16 10:00:00", "转账", "张三", "¥500.00", "收入")
            )
        )
        val result = parser.parse(csv.byteInputStream(Charsets.UTF_8), CsvSource.WECHAT)

        assertEquals(0, result.errors.size)
        assertEquals(2, result.transactions.size)

        val expense = result.transactions[0]
        assertEquals(BigDecimal("28.90"), expense.amount)
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals("美团外卖", expense.counterparty)
        assertEquals("商户消费", expense.originalCategory)
        assertEquals("购物", expense.mappedCategoryName)

        val income = result.transactions[1]
        assertEquals(BigDecimal("500.00"), income.amount)
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals("社交", income.mappedCategoryName)
    }

    // --- Category Mapping Tests ---

    @Test
    fun `category mapping maps known categories`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024-03-15 10:00:00", "交通出行", "滴滴出行", "25.00", "支出"),
                arrayOf("2024-03-15 11:00:00", "医疗健康", "药店", "68.00", "支出"),
                arrayOf("2024-03-15 12:00:00", "文化休闲", "电影院", "45.00", "支出")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals("交通", result.transactions[0].mappedCategoryName)
        assertEquals("医疗", result.transactions[1].mappedCategoryName)
        assertEquals("娱乐", result.transactions[2].mappedCategoryName)
    }

    @Test
    fun `category mapping returns null for unknown categories`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024-03-15 10:00:00", "未知分类", "某商家", "100.00", "支出")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals(1, result.transactions.size)
        assertEquals("未知分类", result.transactions[0].originalCategory)
        assertNull(result.transactions[0].mappedCategoryName)
    }

    // --- Error Handling Tests ---

    @Test
    fun `parse empty file returns error`() {
        val result = parser.parse("".byteInputStream(), CsvSource.ALIPAY)

        assertTrue(result.transactions.isEmpty())
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `parse file without header returns error`() {
        val csv = "这是一些随机内容\n没有表头\n"
        val result = parser.parse(csv.byteInputStream(), CsvSource.ALIPAY)

        assertTrue(result.transactions.isEmpty())
        assertTrue(result.errors.any { it.contains("未找到表头行") })
    }

    @Test
    fun `parse with invalid date adds error and skips row`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("invalid-date", "餐饮美食", "餐厅", "35.50", "支出"),
                arrayOf("2024-03-15 14:30:00", "餐饮美食", "另一家", "20.00", "支出")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals(1, result.transactions.size)
        assertEquals("另一家", result.transactions[0].counterparty)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `parse date with slash format`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024/03/15 14:30:00", "餐饮美食", "餐厅", "35.50", "支出")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        assertEquals(1, result.transactions.size)
        assertEquals(LocalDateTime.of(2024, 3, 15, 14, 30, 0), result.transactions[0].date)
    }

    @Test
    fun `parsed transactions have default flags`() {
        val csv = buildAlipayCSV(
            listOf(
                arrayOf("2024-03-15 14:30:00", "餐饮美食", "餐厅", "35.50", "支出")
            )
        )
        val result = parser.parse(csv.byteInputStream(charset("GBK")), CsvSource.ALIPAY)

        val tx = result.transactions[0]
        assertFalse(tx.isDuplicate)
        assertTrue(tx.isSelected)
    }

    // --- Helper methods ---

    private fun buildAlipayCSV(dataRows: List<Array<String>>): String {
        val sb = StringBuilder()
        // Simulate Alipay metadata lines
        repeat(5) { sb.appendLine("支付宝交易记录明细查询") }
        // Header row
        sb.appendLine("交易时间,交易分类,交易对方,金额（元）,收/支")
        // Data rows
        for (row in dataRows) {
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    private fun buildWechatCSV(dataRows: List<Array<String>>): String {
        val sb = StringBuilder()
        // Simulate WeChat metadata lines
        repeat(5) { sb.appendLine("微信支付账单明细") }
        // Header row
        sb.appendLine("交易时间,交易类型,交易对方,金额(元),收/支")
        // Data rows
        for (row in dataRows) {
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }
}
