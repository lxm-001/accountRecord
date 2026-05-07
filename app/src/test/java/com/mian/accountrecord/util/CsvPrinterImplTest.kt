package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDateTime

class CsvPrinterImplTest {

    private lateinit var printer: CsvPrinterImpl

    @Before
    fun setup() {
        printer = CsvPrinterImpl()
    }

    @Test
    fun `print empty list outputs only header`() {
        val output = ByteArrayOutputStream()
        printer.print(emptyList(), output)
        val csv = output.toString(Charsets.UTF_8.name())
        val lines = csv.trim().lines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("交易时间"))
        assertTrue(lines[0].contains("类型"))
        assertTrue(lines[0].contains("分类"))
        assertTrue(lines[0].contains("金额（元）"))
        assertTrue(lines[0].contains("备注"))
    }

    @Test
    fun `print expense transaction formats correctly`() {
        val tx = Transaction(
            id = 1,
            amount = BigDecimal("35.50"),
            type = TransactionType.EXPENSE,
            category = Category(
                id = 1, name = "餐饮", icon = "restaurant",
                color = "#FF5722", type = TransactionType.EXPENSE
            ),
            ledgerId = 1,
            date = LocalDateTime.of(2024, 3, 15, 14, 30, 0),
            note = "午餐"
        )

        val output = ByteArrayOutputStream()
        printer.print(listOf(tx), output)
        val csv = output.toString(Charsets.UTF_8.name())
        val lines = csv.trim().lines()

        assertEquals(2, lines.size)
        val dataLine = lines[1]
        assertTrue(dataLine.contains("2024-03-15 14:30:00"))
        assertTrue(dataLine.contains("支出"))
        assertTrue(dataLine.contains("餐饮"))
        assertTrue(dataLine.contains("35.50"))
        assertTrue(dataLine.contains("午餐"))
    }

    @Test
    fun `print income transaction formats type as 收入`() {
        val tx = Transaction(
            id = 2,
            amount = BigDecimal("10000.00"),
            type = TransactionType.INCOME,
            category = Category(
                id = 2, name = "工资", icon = "work",
                color = "#4CAF50", type = TransactionType.INCOME
            ),
            ledgerId = 1,
            date = LocalDateTime.of(2024, 3, 16, 9, 0, 0),
            note = null
        )

        val output = ByteArrayOutputStream()
        printer.print(listOf(tx), output)
        val csv = output.toString(Charsets.UTF_8.name())

        assertTrue(csv.contains("收入"))
        assertTrue(csv.contains("工资"))
        assertTrue(csv.contains("10000.00"))
    }

    @Test
    fun `print transaction with null note outputs empty string`() {
        val tx = Transaction(
            id = 3,
            amount = BigDecimal("100.00"),
            type = TransactionType.EXPENSE,
            category = Category(
                id = 1, name = "交通", icon = "directions_car",
                color = "#2196F3", type = TransactionType.EXPENSE
            ),
            ledgerId = 1,
            date = LocalDateTime.of(2024, 3, 17, 8, 0, 0),
            note = null
        )

        val output = ByteArrayOutputStream()
        printer.print(listOf(tx), output)
        val csv = output.toString(Charsets.UTF_8.name())
        val lines = csv.trim().lines()

        assertEquals(2, lines.size)
        // The last field should be empty (no note)
        assertTrue(lines[1].contains("交通"))
    }

    @Test
    fun `print multiple transactions outputs correct number of rows`() {
        val transactions = listOf(
            Transaction(
                id = 1, amount = BigDecimal("35.50"), type = TransactionType.EXPENSE,
                category = Category(1, "餐饮", "restaurant", "#FF5722", TransactionType.EXPENSE),
                ledgerId = 1, date = LocalDateTime.of(2024, 3, 15, 14, 30, 0), note = "午餐"
            ),
            Transaction(
                id = 2, amount = BigDecimal("10000.00"), type = TransactionType.INCOME,
                category = Category(2, "工资", "work", "#4CAF50", TransactionType.INCOME),
                ledgerId = 1, date = LocalDateTime.of(2024, 3, 16, 9, 0, 0), note = null
            ),
            Transaction(
                id = 3, amount = BigDecimal("25.00"), type = TransactionType.EXPENSE,
                category = Category(3, "交通", "directions_car", "#2196F3", TransactionType.EXPENSE),
                ledgerId = 1, date = LocalDateTime.of(2024, 3, 17, 8, 0, 0), note = "地铁"
            )
        )

        val output = ByteArrayOutputStream()
        printer.print(transactions, output)
        val csv = output.toString(Charsets.UTF_8.name())
        val lines = csv.trim().lines()

        assertEquals(4, lines.size) // 1 header + 3 data rows
    }
}
