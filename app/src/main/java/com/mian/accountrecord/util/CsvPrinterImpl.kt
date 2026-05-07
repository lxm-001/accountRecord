package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionType
import com.opencsv.CSVWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CsvPrinterImpl @Inject constructor() : CsvPrinter {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val HEADER = arrayOf("交易时间", "类型", "分类", "金额（元）", "备注")
    }

    override fun print(transactions: List<Transaction>, outputStream: OutputStream) {
        val writer = CSVWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        writer.use { csv ->
            csv.writeNext(HEADER)
            for (tx in transactions) {
                csv.writeNext(
                    arrayOf(
                        tx.date.format(DATE_FORMATTER),
                        if (tx.type == TransactionType.EXPENSE) "支出" else "收入",
                        tx.category.name,
                        tx.amount.toPlainString(),
                        tx.note ?: ""
                    )
                )
            }
        }
    }
}
