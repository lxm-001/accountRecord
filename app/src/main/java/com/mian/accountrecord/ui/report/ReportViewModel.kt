package com.mian.accountrecord.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.TrendPoint
import com.mian.accountrecord.domain.usecase.GetCategoriesUseCase
import com.mian.accountrecord.domain.usecase.GetExpenseByCategoryUseCase
import com.mian.accountrecord.domain.usecase.GetIncomeByCategoryUseCase
import com.mian.accountrecord.domain.usecase.GetLedgersUseCase
import com.mian.accountrecord.domain.usecase.GetMonthlySummaryUseCase
import com.mian.accountrecord.domain.usecase.GetTrendDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

enum class ChartType { PIE, BAR }

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getExpenseByCategory: GetExpenseByCategoryUseCase,
    private val getIncomeByCategory: GetIncomeByCategoryUseCase,
    private val getTrendData: GetTrendDataUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val getLedgers: GetLedgersUseCase,
    private val getCategories: GetCategoriesUseCase
) : ViewModel() {

    data class CategoryAmount(val categoryId: Long, val categoryName: String, val amount: BigDecimal)

    data class UiState(
        val chartType: ChartType = ChartType.PIE,
        val currentPeriod: YearMonth = YearMonth.now(),
        val summary: MonthlySummary = MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
        val expenseData: List<CategoryAmount> = emptyList(),
        val incomeData: List<CategoryAmount> = emptyList(),
        val categoryData: List<CategoryAmount> = emptyList(),
        val trendData: List<TrendPoint> = emptyList(),
        val isLoading: Boolean = true,
        val isEmpty: Boolean = true
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    private var dataJob: Job? = null
    private var activeLedgerId: Long? = null
    private var categoryMap: Map<Long, String> = emptyMap()

    init {
        viewModelScope.launch {
            val allCategories = mutableListOf<Category>()
            getCategories(com.mian.accountrecord.domain.model.TransactionType.EXPENSE).first().let { allCategories.addAll(it) }
            getCategories(com.mian.accountrecord.domain.model.TransactionType.INCOME).first().let { allCategories.addAll(it) }
            categoryMap = allCategories.associate { it.id to it.name }

            getLedgers().collectLatest { ledgers ->
                val newActiveId = ledgers.find { it.isActive }?.id ?: ledgers.firstOrNull()?.id
                activeLedgerId = newActiveId
                activeLedgerId?.let { loadData(it, _uiState.value?.currentPeriod ?: YearMonth.now()) }
            }
        }
    }

    private fun loadData(ledgerId: Long, period: YearMonth) {
        dataJob?.cancel()
        _uiState.value = _uiState.value?.copy(isLoading = true)
        dataJob = viewModelScope.launch {
            launch {
                getMonthlySummary(ledgerId, period).collectLatest { summary ->
                    _uiState.value = _uiState.value?.copy(summary = summary)
                }
            }
            launch {
                getExpenseByCategory(ledgerId, period).collectLatest { data ->
                    val mapped = data.map { CategoryAmount(it.first, categoryMap[it.first] ?: "未知", it.second) }
                    _uiState.value = _uiState.value?.copy(
                        expenseData = mapped,
                        categoryData = mapped,
                        isEmpty = mapped.isEmpty() && (_uiState.value?.incomeData?.isEmpty() != false),
                        isLoading = false
                    )
                }
            }
            launch {
                getIncomeByCategory(ledgerId, period).collectLatest { data ->
                    val mapped = data.map { CategoryAmount(it.first, categoryMap[it.first] ?: "未知", it.second) }
                    _uiState.value = _uiState.value?.copy(
                        incomeData = mapped,
                        isEmpty = mapped.isEmpty() && (_uiState.value?.expenseData?.isEmpty() != false)
                    )
                }
            }
            launch {
                getTrendData(ledgerId, period).collectLatest { trend ->
                    _uiState.value = _uiState.value?.copy(trendData = trend)
                }
            }
        }
    }

    fun switchChartType(type: ChartType) {
        _uiState.value = _uiState.value?.copy(chartType = type)
    }

    fun switchPeriod(delta: Int) {
        val state = _uiState.value ?: return
        val newPeriod = state.currentPeriod.plusMonths(delta.toLong())
        _uiState.value = state.copy(currentPeriod = newPeriod)
        activeLedgerId?.let { loadData(it, newPeriod) }
    }

    fun setPeriod(yearMonth: YearMonth) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(currentPeriod = yearMonth)
        activeLedgerId?.let { loadData(it, yearMonth) }
    }
}
