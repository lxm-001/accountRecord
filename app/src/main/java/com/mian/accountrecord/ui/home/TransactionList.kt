package com.mian.accountrecord.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.ui.components.EmptyStateView
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HeaderBackground = Color(0xFFF5F5F5)

private val chineseLocale = Locale.CHINESE

private val dateWithWeekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 EEEE", chineseLocale)

private val dateOnlyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", chineseLocale)

private fun formatDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return when (date) {
        today -> "今天 ${date.format(dateOnlyFormatter)}"
        yesterday -> "昨天 ${date.format(dateOnlyFormatter)}"
        else -> date.format(dateWithWeekdayFormatter)
    }
}

private fun formatDailySubtotal(transactions: List<Transaction>): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val expense = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
    val income = transactions
        .filter { it.type == TransactionType.INCOME }
        .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
    return "支出 ¥${formatter.format(expense)} | 收入 ¥${formatter.format(income)}"
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    transactionsByDate: Map<LocalDate, List<Transaction>>,
    onItemClick: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    isInitialLoading: Boolean = false,
    onEmptyIconClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (isInitialLoading) {
        // Show nothing while initial data is loading to avoid flash of empty state
        Box(modifier = modifier.fillMaxSize())
        return
    }
    if (transactionsByDate.isEmpty()) {
        EmptyStateView(
            message = "还没有记录，快快记账吧",
            modifier = modifier.fillMaxSize(),
            onIconClick = onEmptyIconClick
        )
        return
    }

    // Sort dates descending (most recent first)
    val sortedDates = transactionsByDate.keys.sortedDescending()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        sortedDates.forEach { date ->
            val transactions = transactionsByDate[date] ?: return@forEach

            stickyHeader(key = date.toString()) {
                DateHeader(
                    date = date,
                    transactions = transactions
                )
            }

            items(
                items = transactions,
                key = { it.id }
            ) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { onItemClick(transaction) },
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    transactions: List<Transaction>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(HeaderBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateHeader(date),
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDailySubtotal(transactions),
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
