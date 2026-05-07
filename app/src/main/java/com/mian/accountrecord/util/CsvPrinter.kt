package com.mian.accountrecord.util

import com.mian.accountrecord.domain.model.Transaction
import java.io.OutputStream

interface CsvPrinter {
    fun print(transactions: List<Transaction>, outputStream: OutputStream)
}
