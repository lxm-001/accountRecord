package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.CsvPrinter
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.time.YearMonth
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val csvPrinter: CsvPrinter
) {
    suspend operator fun invoke(
        ledgerId: Long,
        yearMonth: YearMonth,
        outputStream: OutputStream
    ) {
        val transactions = transactionRepository.getByLedgerAndMonth(ledgerId, yearMonth).first()
        csvPrinter.print(transactions, outputStream)
    }
}
