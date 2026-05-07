package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.ParsedTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

class DetectDuplicatesUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    /**
     * Checks parsed transactions against existing ones in the ledger.
     * A duplicate is detected when amount, date (within same minute), and counterparty match.
     * Duplicates are returned with isDuplicate = true and isSelected = false.
     */
    suspend operator fun invoke(
        parsed: List<ParsedTransaction>,
        ledgerId: Long
    ): List<ParsedTransaction> {
        if (parsed.isEmpty()) return parsed

        // Determine date range from parsed transactions (with some buffer)
        val minDate = parsed.minOf { it.date }.minusDays(1)
        val maxDate = parsed.maxOf { it.date }.plusDays(1)

        val existing = repository.getByLedgerAndDateRange(
            ledgerId,
            minDate,
            maxDate
        ).first()

        return parsed.map { parsedTx ->
            val isDuplicate = existing.any { existingTx ->
                existingTx.amount.compareTo(parsedTx.amount) == 0 &&
                    isSameMinute(existingTx.date, parsedTx.date) &&
                    isSimilarCounterparty(existingTx.note, parsedTx.counterparty)
            }
            if (isDuplicate) {
                parsedTx.copy(isDuplicate = true, isSelected = false)
            } else {
                parsedTx
            }
        }
    }

    private fun isSameMinute(a: LocalDateTime, b: LocalDateTime): Boolean {
        return a.year == b.year &&
            a.monthValue == b.monthValue &&
            a.dayOfMonth == b.dayOfMonth &&
            a.hour == b.hour &&
            a.minute == b.minute
    }

    private fun isSimilarCounterparty(note: String?, counterparty: String): Boolean {
        if (note.isNullOrBlank() && counterparty.isBlank()) return true
        if (note.isNullOrBlank() || counterparty.isBlank()) return false
        return note.trim().contains(counterparty.trim(), ignoreCase = true) ||
            counterparty.trim().contains(note.trim(), ignoreCase = true)
    }
}
