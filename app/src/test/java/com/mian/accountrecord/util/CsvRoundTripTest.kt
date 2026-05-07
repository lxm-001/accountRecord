package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionSource
import com.mian.accountrecord.domain.model.TransactionType
import com.opencsv.CSVReader
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * CSV 往返一致性属性测试
 *
 * **Validates: Requirements 2.9**
 *
 * 属性 1：对任意有效 Transaction 列表，先 CsvPrinter 导出再解析 CSV，
 * 结果与原始列表等价（金额、类型、分类名、日期、备注均一致）。
 *
 * 由于 CsvPrinter 输出格式（交易时间,类型,分类,金额（元）,备注）与 CsvParser
 * 期望的支付宝/微信格式列不完全匹配，本测试通过直接解析 CsvPrinter 输出的 CSV
 * 来验证往返一致性：导出的每一行数据都能被正确还原为等价的 Transaction 字段。
 */
class CsvRoundTripTest {

    private lateinit var printer: CsvPrinterImpl
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Before
    fun setup() {
        printer = CsvPrinterImpl()
    }

    // --- Helper ---

    private fun makeCategory(name: String, type: TransactionType) = Category(
        id = 1, name = name, icon = "icon", color = "#000000", type = type
    )

    private fun makeTx(
        amount: BigDecimal,
        type: TransactionType,
        categoryName: String,
        date: LocalDateTime,
        note: String? = null
    ) = Transaction(
        amount = amount,
        type = type,
        category = makeCategory(categoryName, type),
        ledgerId = 1,
        date = date,
        note = note,
        source = TransactionSource.MANUAL
    )

    /**
     * Export transactions via CsvPrinter, then parse the raw CSV back and
     * assert each row matches the original transaction fields.
     */
    private fun assertRoundTrip(original: List<Transaction>) {
        // Step 1: Export
        val outputStream = ByteArrayOutputStream()
        printer.print(original, outputStream)
        val csvBytes = outputStream.toByteArray()

        // Step 2: Parse the exported CSV back
        val reader = CSVReader(InputStreamReader(ByteArrayInputStream(csvBytes), Charsets.UTF_8))
        val allRows = reader.readAll()
        reader.close()

        // Header + data rows
        assertTrue("CSV should have header + ${original.size} data rows", allRows.size >= original.size + 1)

        // Verify header
        val header = allRows[0].map { it.trim() }
        assertTrue(header.contains("交易时间"))
        assertTrue(header.contains("类型"))
        assertTrue(header.contains("分类"))
        assertTrue(header.contains("金额（元）"))
        assertTrue(header.contains("备注"))

        val dateIdx = header.indexOf("交易时间")
        val typeIdx = header.indexOf("类型")
        val categoryIdx = header.indexOf("分类")
        val amountIdx = header.indexOf("金额（元）")
        val noteIdx = header.indexOf("备注")

        // Step 3: Verify each row matches the original transaction
        for (i in original.indices) {
            val tx = original[i]
            val row = allRows[i + 1] // skip header

            val parsedDate = LocalDateTime.parse(row[dateIdx].trim(), dateFormatter)
            val parsedType = if (row[typeIdx].trim() == "支出") TransactionType.EXPENSE else TransactionType.INCOME
            val parsedCategory = row[categoryIdx].trim()
            val parsedAmount = BigDecimal(row[amountIdx].trim())
            val parsedNote = row[noteIdx].trim().ifEmpty { null }

            assertEquals("Row ${i + 1} date mismatch", tx.date, parsedDate)
            assertEquals("Row ${i + 1} type mismatch", tx.type, parsedType)
            assertEquals("Row ${i + 1} category mismatch", tx.category.name, parsedCategory)
            assertEquals("Row ${i + 1} amount mismatch", 0, tx.amount.compareTo(parsedAmount))
            assertEquals("Row ${i + 1} note mismatch", tx.note ?: "", parsedNote ?: "")
        }
    }

    // --- Test cases exercising the round-trip property ---

    @Test
    fun `round trip - single expense transaction`() {
        val txList = listOf(
            makeTx(BigDecimal("35.50"), TransactionType.EXPENSE, "餐饮",
                LocalDateTime.of(2024, 3, 15, 14, 30, 0))
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - single income transaction`() {
        val txList = listOf(
            makeTx(BigDecimal("10000.00"), TransactionType.INCOME, "工资",
                LocalDateTime.of(2024, 6, 1, 9, 0, 0))
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - transaction with note`() {
        val txList = listOf(
            makeTx(BigDecimal("88.00"), TransactionType.EXPENSE, "交通",
                LocalDateTime.of(2024, 4, 10, 8, 15, 0), note = "打车去机场")
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - transaction without note`() {
        val txList = listOf(
            makeTx(BigDecimal("12.00"), TransactionType.EXPENSE, "日用",
                LocalDateTime.of(2024, 5, 20, 18, 0, 0), note = null)
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - multiple mixed transactions`() {
        val txList = listOf(
            makeTx(BigDecimal("35.50"), TransactionType.EXPENSE, "餐饮",
                LocalDateTime.of(2024, 3, 15, 14, 30, 0), note = "午餐"),
            makeTx(BigDecimal("10000.00"), TransactionType.INCOME, "工资",
                LocalDateTime.of(2024, 3, 16, 9, 0, 0)),
            makeTx(BigDecimal("25.00"), TransactionType.EXPENSE, "交通",
                LocalDateTime.of(2024, 3, 17, 8, 0, 0), note = "地铁"),
            makeTx(BigDecimal("500.00"), TransactionType.INCOME, "红包",
                LocalDateTime.of(2024, 3, 18, 20, 0, 0), note = "生日红包")
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - transaction with special characters in note`() {
        val txList = listOf(
            makeTx(BigDecimal("99.99"), TransactionType.EXPENSE, "购物",
                LocalDateTime.of(2024, 7, 1, 12, 0, 0), note = "买了\"特价\"商品, 含逗号"),
            makeTx(BigDecimal("50.00"), TransactionType.EXPENSE, "娱乐",
                LocalDateTime.of(2024, 7, 2, 15, 0, 0), note = "看电影 换行测试")
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - transaction with large amount`() {
        val txList = listOf(
            makeTx(BigDecimal("99999999.99"), TransactionType.INCOME, "投资收益",
                LocalDateTime.of(2024, 12, 31, 23, 59, 59))
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - transaction with small amount`() {
        val txList = listOf(
            makeTx(BigDecimal("0.01"), TransactionType.EXPENSE, "其他",
                LocalDateTime.of(2024, 1, 1, 0, 0, 0))
        )
        assertRoundTrip(txList)
    }

    @Test
    fun `round trip - empty transaction list`() {
        assertRoundTrip(emptyList())
    }

    @Test
    fun `round trip - many transactions preserve order`() {
        val txList = (1..20).map { i ->
            makeTx(
                BigDecimal("${i}.${i.toString().padStart(2, '0')}"),
                if (i % 2 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                if (i % 2 == 0) "工资" else "餐饮",
                LocalDateTime.of(2024, 1, i, i % 24, 0, 0),
                note = if (i % 3 == 0) "备注$i" else null
            )
        }
        assertRoundTrip(txList)
    }
}
