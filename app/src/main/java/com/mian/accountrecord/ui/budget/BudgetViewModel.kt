package com.mian.accountrecord.ui.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.model.Budget
import com.mian.accountrecord.domain.model.BudgetProgress
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.usecase.GetBudgetProgressUseCase
import com.mian.accountrecord.domain.usecase.GetCategoriesUseCase
import com.mian.accountrecord.domain.usecase.GetLedgersUseCase
import com.mian.accountrecord.domain.usecase.SetBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val setBudget: SetBudgetUseCase,
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getLedgers: GetLedgersUseCase
) : ViewModel() {

    data class UiState(
        val totalBudget: BudgetProgress? = null,
        val categoryBudgets: List<BudgetProgress> = emptyList(),
        val categories: List<Category> = emptyList(),
        val isLoading: Boolean = true,
        val hasAnyBudget: Boolean = false,
        val currentYearMonth: YearMonth = YearMonth.now()
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    private var dataJob: Job? = null
    private var activeLedgerId: Long? = null

    init {
        viewModelScope.launch {
            getLedgers().collectLatest { ledgers ->
                val newActiveId = ledgers.find { it.isActive }?.id ?: ledgers.firstOrNull()?.id
                activeLedgerId = newActiveId
                getCategories(TransactionType.EXPENSE).first().let { cats ->
                    _uiState.value = _uiState.value?.copy(categories = cats)
                }
                if (newActiveId != null) {
                    loadBudgets(newActiveId, _uiState.value?.currentYearMonth ?: YearMonth.now())
                } else {
                    _uiState.value = _uiState.value?.copy(isLoading = false, hasAnyBudget = false)
                }
            }
        }
    }

    private fun loadBudgets(ledgerId: Long, yearMonth: YearMonth) {
        dataJob?.cancel()
        _uiState.value = _uiState.value?.copy(isLoading = true)
        dataJob = viewModelScope.launch {
            getBudgetProgress(ledgerId, yearMonth).collectLatest { progressList ->
                val total = progressList.find { it.budget.categoryId == null }
                val categoryBudgets = progressList.filter { it.budget.categoryId != null }
                _uiState.value = _uiState.value?.copy(
                    totalBudget = total,
                    categoryBudgets = categoryBudgets,
                    hasAnyBudget = progressList.isNotEmpty(),
                    isLoading = false
                )
            }
        }
    }

    fun saveBudget(categoryId: Long?, amount: BigDecimal) {
        val yearMonth = _uiState.value?.currentYearMonth ?: YearMonth.now()
        val ledgerId = activeLedgerId ?: return
        // Find existing budget id to update instead of creating duplicate
        val existingId = if (categoryId == null) {
            _uiState.value?.totalBudget?.budget?.id ?: 0
        } else {
            _uiState.value?.categoryBudgets?.find { it.budget.categoryId == categoryId }?.budget?.id ?: 0
        }
        viewModelScope.launch {
            setBudget(Budget(id = existingId, ledgerId = ledgerId, categoryId = categoryId, amount = amount, yearMonth = yearMonth))
            loadBudgets(ledgerId, yearMonth)
        }
    }

    fun switchMonth(delta: Int) {
        val state = _uiState.value ?: return
        val newMonth = state.currentYearMonth.plusMonths(delta.toLong())
        _uiState.value = state.copy(currentYearMonth = newMonth)
        activeLedgerId?.let { loadBudgets(it, newMonth) }
    }

    fun setMonth(yearMonth: YearMonth) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(currentYearMonth = yearMonth)
        activeLedgerId?.let { loadBudgets(it, yearMonth) }
    }
}
