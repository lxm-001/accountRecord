package com.mian.accountrecord.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.data.local.preferences.AppPreferences
import com.mian.accountrecord.domain.model.BudgetProgress
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionSource
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.usecase.AddTransactionUseCase
import com.mian.accountrecord.domain.usecase.DeleteTransactionUseCase
import com.mian.accountrecord.domain.usecase.GetBudgetProgressUseCase
import com.mian.accountrecord.domain.usecase.GetCategoriesUseCase
import com.mian.accountrecord.domain.usecase.GetLedgersUseCase
import com.mian.accountrecord.domain.usecase.GetMonthlySummaryUseCase
import com.mian.accountrecord.domain.usecase.GetTransactionsByMonthUseCase
import com.mian.accountrecord.domain.usecase.SwitchLedgerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactions: GetTransactionsByMonthUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val getLedgers: GetLedgersUseCase,
    private val switchLedger: SwitchLedgerUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    data class HomeUiState(
        val currentLedger: Ledger? = null,
        val ledgers: List<Ledger> = emptyList(),
        val currentYearMonth: YearMonth = YearMonth.now(),
        val summary: MonthlySummary = MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
        val budgetProgress: BudgetProgress? = null,
        val transactionsByDate: Map<LocalDate, List<Transaction>> = emptyMap(),
        val isRefreshing: Boolean = false,
        val isInitialLoading: Boolean = true
    )

    data class QuickEntryState(
        val type: TransactionType = TransactionType.EXPENSE,
        val amount: String = "",
        val selectedCategory: Category? = null,
        val selectedDate: LocalDate = LocalDate.now(),
        val note: String = "",
        val isVisible: Boolean = false,
        val lastUsedCategoryId: Long? = null
    )

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private val _quickEntryState = MutableLiveData(QuickEntryState())
    val quickEntryState: LiveData<QuickEntryState> = _quickEntryState

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private var dataCollectionJob: Job? = null
    private var categoriesJob: Job? = null

    init {
        val savedCategoryId = appPreferences.lastSelectedCategoryId
        if (savedCategoryId != -1L) {
            _quickEntryState.value = _quickEntryState.value?.copy(lastUsedCategoryId = savedCategoryId)
        }
        loadCategories(TransactionType.EXPENSE)
        observeLedgers()
    }

    private fun loadCategories(type: TransactionType) {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            getCategories(type).collectLatest { cats ->
                _categories.value = cats
            }
        }
    }

    private fun observeLedgers() {
        viewModelScope.launch {
            getLedgers().collectLatest { ledgers ->
                val current = _uiState.value?.currentLedger
                val activeLedger = current
                    ?: ledgers.find { it.isActive }
                    ?: ledgers.firstOrNull()

                _uiState.value = _uiState.value?.copy(
                    ledgers = ledgers,
                    currentLedger = activeLedger
                )
                activeLedger?.let { reloadData(it.id, _uiState.value!!.currentYearMonth) }
            }
        }
    }

    private fun reloadData(ledgerId: Long, yearMonth: YearMonth) {
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            launch {
                getTransactions(ledgerId, yearMonth).collectLatest { txByDate ->
                    _uiState.value = _uiState.value?.copy(
                        transactionsByDate = txByDate,
                        isInitialLoading = false
                    )
                }
            }
            launch {
                getMonthlySummary(ledgerId, yearMonth).collectLatest { summary ->
                    _uiState.value = _uiState.value?.copy(summary = summary)
                }
            }
            launch {
                getBudgetProgress(ledgerId, yearMonth).collectLatest { progressList ->
                    val totalBudget = progressList.find { it.budget.categoryId == null }
                    _uiState.value = _uiState.value?.copy(budgetProgress = totalBudget)
                }
            }
        }
    }

    fun switchMonth(delta: Int) {
        val state = _uiState.value ?: return
        val newMonth = state.currentYearMonth.plusMonths(delta.toLong())
        _uiState.value = state.copy(currentYearMonth = newMonth)
        state.currentLedger?.let { reloadData(it.id, newMonth) }
    }

    fun setMonth(yearMonth: YearMonth) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(currentYearMonth = yearMonth)
        state.currentLedger?.let { reloadData(it.id, yearMonth) }
    }

    fun switchLedger(ledgerId: Long) {
        viewModelScope.launch {
            switchLedger.invoke(ledgerId)
            val state = _uiState.value ?: return@launch
            val newLedger = state.ledgers.find { it.id == ledgerId }
            _uiState.value = state.copy(currentLedger = newLedger)
            newLedger?.let { reloadData(it.id, state.currentYearMonth) }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransaction.invoke(transaction)
        }
    }

    fun refresh() {
        val state = _uiState.value ?: return
        val ledger = state.currentLedger ?: return
        _uiState.value = state.copy(isRefreshing = true)
        reloadData(ledger.id, state.currentYearMonth)
        // Data flows emit new values asynchronously; add a small delay so the
        // refresh indicator is visible and then dismiss it.
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _uiState.value = _uiState.value?.copy(isRefreshing = false)
        }
    }

    // --- Quick Entry Panel ---

    fun toggleQuickEntry() {
        val current = _quickEntryState.value ?: return
        if (current.isVisible) {
            _quickEntryState.value = QuickEntryState(
                lastUsedCategoryId = current.lastUsedCategoryId
            )
        } else {
            _quickEntryState.value = current.copy(
                isVisible = true,
                selectedDate = LocalDate.now()
            )
        }
    }

    fun updateQuickEntryType(type: TransactionType) {
        _quickEntryState.value = _quickEntryState.value?.copy(type = type)
        loadCategories(type)
    }

    fun updateQuickEntryAmount(digit: String) {
        val current = _quickEntryState.value ?: return
        val newAmount = when (digit) {
            "⌫" -> if (current.amount.isNotEmpty()) current.amount.dropLast(1) else ""
            "." -> if (current.amount.contains(".")) current.amount else current.amount + "."
            else -> {
                val parts = current.amount.split(".")
                if (parts.size == 2 && parts[1].length >= 2) {
                    current.amount // already 2 decimal places
                } else {
                    current.amount + digit
                }
            }
        }
        _quickEntryState.value = current.copy(amount = newAmount)
    }

    fun updateQuickEntryCategory(category: Category) {
        _quickEntryState.value = _quickEntryState.value?.copy(selectedCategory = category)
        appPreferences.lastSelectedCategoryId = category.id
    }

    fun updateQuickEntryDate(date: LocalDate) {
        _quickEntryState.value = _quickEntryState.value?.copy(selectedDate = date)
    }

    fun updateQuickEntryNote(note: String) {
        _quickEntryState.value = _quickEntryState.value?.copy(note = note)
    }

    fun saveTransaction() {
        val entry = _quickEntryState.value ?: return
        val state = _uiState.value ?: return
        val ledger = state.currentLedger ?: return
        val category = entry.selectedCategory ?: return
        val amount = entry.amount.toBigDecimalOrNull() ?: return

        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                type = entry.type,
                category = category,
                ledgerId = ledger.id,
                date = entry.selectedDate.atStartOfDay(),
                note = entry.note.ifBlank { null },
                source = TransactionSource.MANUAL
            )
            val result = addTransaction(transaction)
            if (result.isSuccess) {
                _quickEntryState.value = QuickEntryState(
                    lastUsedCategoryId = category.id
                )
            }
        }
    }

    fun hasQuickEntryData(): Boolean {
        val entry = _quickEntryState.value ?: return false
        return entry.amount.isNotEmpty() || entry.note.isNotEmpty()
    }
}
