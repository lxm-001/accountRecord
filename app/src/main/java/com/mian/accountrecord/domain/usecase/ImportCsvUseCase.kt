package com.mian.accountrecord.domain.usecase

import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionSource
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.repository.CategoryRepository
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.CsvParser
import com.mian.accountrecord.util.CsvSource
import com.mian.accountrecord.util.ParsedTransaction
import kotlinx.coroutines.flow.first
import java.io.InputStream
import javax.inject.Inject

data class ImportResult(
    val importedCount: Int,
    val totalParsed: Int,
    val duplicateCount: Int,
    val errors: List<String>
)

class ImportCsvUseCase @Inject constructor(
    private val csvParser: CsvParser,
    private val detectDuplicates: DetectDuplicatesUseCase,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        inputStream: InputStream,
        source: CsvSource,
        ledgerId: Long
    ): ImportResult {
        // Step 1: Parse CSV
        val parseResult = csvParser.parse(inputStream, source)
        val errors = parseResult.errors.toMutableList()

        if (parseResult.transactions.isEmpty()) {
            return ImportResult(
                importedCount = 0,
                totalParsed = 0,
                duplicateCount = 0,
                errors = errors
            )
        }

        // Step 2: Resolve category names to IDs
        val allCategories = categoryRepository.getAll().first()
        val categoryByName = allCategories.associateBy { it.name }
        val fallbackExpense = allCategories.find { it.name == "其他" && it.type == TransactionType.EXPENSE }
        val fallbackIncome = allCategories.find { it.name == "其他" && it.type == TransactionType.INCOME }

        // Step 3: Detect duplicates
        val withDuplicates = detectDuplicates(parseResult.transactions, ledgerId)
        val duplicateCount = withDuplicates.count { it.isDuplicate }

        // Step 4: Filter selected (non-duplicate) transactions
        val selected = withDuplicates.filter { it.isSelected }

        // Step 5: Convert ParsedTransaction to Transaction domain objects
        val transactionSource = when (source) {
            CsvSource.ALIPAY -> TransactionSource.ALIPAY
            CsvSource.WECHAT -> TransactionSource.WECHAT
        }

        val transactions = selected.mapNotNull { parsed ->
            val category = resolveCategory(
                parsed, categoryByName, fallbackExpense, fallbackIncome
            )
            if (category == null) {
                errors.add("无法为交易找到分类: ${parsed.counterparty} ${parsed.amount}")
                return@mapNotNull null
            }
            Transaction(
                amount = parsed.amount,
                type = parsed.type,
                category = category,
                ledgerId = ledgerId,
                date = parsed.date,
                note = parsed.counterparty.ifBlank { null },
                source = transactionSource
            )
        }

        // Step 6: Batch insert
        if (transactions.isNotEmpty()) {
            transactionRepository.insertAll(transactions)
        }

        // Step 7: Return ImportResult
        return ImportResult(
            importedCount = transactions.size,
            totalParsed = parseResult.transactions.size,
            duplicateCount = duplicateCount,
            errors = errors
        )
    }

    private fun resolveCategory(
        parsed: ParsedTransaction,
        categoryByName: Map<String, Category>,
        fallbackExpense: Category?,
        fallbackIncome: Category?
    ): Category? {
        // Try mapped category name first
        if (parsed.mappedCategoryName != null) {
            val matched = categoryByName[parsed.mappedCategoryName]
            if (matched != null) return matched
        }

        // Fallback to "其他" category of the same type
        return when (parsed.type) {
            TransactionType.EXPENSE -> fallbackExpense
            TransactionType.INCOME -> fallbackIncome
        }
    }
}
