package com.mian.accountrecord.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.domain.model.Transaction
import java.math.BigDecimal

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToReport: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToLedgerManage: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    externalQuickEntryVisible: Boolean = false,
    onQuickEntryVisibilityChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.observeAsState(HomeViewModel.HomeUiState())
    val quickEntryState by viewModel.quickEntryState.observeAsState(HomeViewModel.QuickEntryState())
    val categories by viewModel.categories.observeAsState(emptyList())

    // Sync external FAB trigger → ViewModel
    LaunchedEffect(externalQuickEntryVisible) {
        if (externalQuickEntryVisible && !quickEntryState.isVisible) {
            viewModel.toggleQuickEntry()
        }
    }

    // Sync ViewModel visibility → external state (so FAB hides/shows)
    LaunchedEffect(quickEntryState.isVisible) {
        onQuickEntryVisibilityChange(quickEntryState.isVisible)
    }

    HomeScreenContent(
        uiState = uiState,
        quickEntryState = quickEntryState,
        categories = categories,
        onSwitchMonth = viewModel::switchMonth,
        onSelectMonth = viewModel::setMonth,
        onSwitchLedger = viewModel::switchLedger,
        onManageLedgers = onNavigateToLedgerManage,
        onNavigateToReport = onNavigateToReport,
        onNavigateToBudget = onNavigateToBudget,
        onTransactionClick = { onNavigateToTransactionDetail(it.id) },
        onDeleteTransaction = viewModel::deleteTransaction,
        onRefresh = viewModel::refresh,
        onQuickEntryTypeChange = viewModel::updateQuickEntryType,
        onQuickEntryAmountInput = viewModel::updateQuickEntryAmount,
        onQuickEntryCategorySelect = viewModel::updateQuickEntryCategory,
        onQuickEntryDateChange = viewModel::updateQuickEntryDate,
        onQuickEntryNoteChange = viewModel::updateQuickEntryNote,
        onQuickEntrySave = viewModel::saveTransaction,
        onQuickEntryDismiss = viewModel::toggleQuickEntry,
        hasQuickEntryData = viewModel.hasQuickEntryData()
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeViewModel.HomeUiState,
    quickEntryState: HomeViewModel.QuickEntryState,
    categories: List<com.mian.accountrecord.domain.model.Category>,
    onSwitchMonth: (Int) -> Unit,
    onSelectMonth: (java.time.YearMonth) -> Unit,
    onSwitchLedger: (Long) -> Unit,
    onManageLedgers: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onRefresh: () -> Unit,
    onQuickEntryTypeChange: (com.mian.accountrecord.domain.model.TransactionType) -> Unit,
    onQuickEntryAmountInput: (String) -> Unit,
    onQuickEntryCategorySelect: (com.mian.accountrecord.domain.model.Category) -> Unit,
    onQuickEntryDateChange: (java.time.LocalDate) -> Unit,
    onQuickEntryNoteChange: (String) -> Unit,
    onQuickEntrySave: () -> Unit,
    onQuickEntryDismiss: () -> Unit,
    hasQuickEntryData: Boolean
) {
    RefreshableContent(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top bar: ledger switcher + month navigation
            HomeTopBar(
                currentLedger = uiState.currentLedger,
                ledgers = uiState.ledgers,
                currentYearMonth = uiState.currentYearMonth,
                onSwitchMonth = onSwitchMonth,
                onSelectMonth = onSelectMonth,
                onSwitchLedger = onSwitchLedger,
                onManageLedgers = onManageLedgers
            )

            // 2. Summary card (income / expense / balance)
            SummaryCard(
                summary = uiState.summary,
                onClick = onNavigateToReport
            )

            // 3. Budget progress bar (only shown when budget is set)
            val budgetProgress = uiState.budgetProgress
            if (budgetProgress != null && budgetProgress.budget.amount > BigDecimal.ZERO) {
                BudgetProgressBar(
                    budgetProgress = budgetProgress,
                    onNavigateToBudget = onNavigateToBudget
                )
            }

            // 4. Transaction list (takes remaining space)
            TransactionList(
                transactionsByDate = uiState.transactionsByDate,
                onItemClick = onTransactionClick,
                onDeleteTransaction = onDeleteTransaction,
                isInitialLoading = uiState.isInitialLoading,
                onEmptyIconClick = onQuickEntryDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Quick Entry Panel (BottomSheet overlay, rendered outside the Column)
    if (quickEntryState.isVisible) {
        QuickEntryPanel(
            state = quickEntryState,
            categories = categories,
            currentLedgerName = uiState.currentLedger?.name ?: "",
            onTypeChange = onQuickEntryTypeChange,
            onAmountInput = onQuickEntryAmountInput,
            onCategorySelect = onQuickEntryCategorySelect,
            onDateChange = onQuickEntryDateChange,
            onNoteChange = onQuickEntryNoteChange,
            onSave = onQuickEntrySave,
            onDismiss = onQuickEntryDismiss,
            hasData = hasQuickEntryData
        )
    }
}
