package com.mian.accountrecord.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mian.accountrecord.domain.model.MonthlySummary
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val IncomeColor = Color(0xFF4CAF50)
private val ExpenseColor = Color(0xFFF44336)
private val LabelColor = Color(0xFF999999)

@Composable
fun SummaryCard(
    summary: MonthlySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryColumn(
                label = "收入",
                amount = summary.income,
                color = IncomeColor,
                modifier = Modifier.weight(1f)
            )
            SummaryColumn(
                label = "支出",
                amount = summary.expense,
                color = ExpenseColor,
                modifier = Modifier.weight(1f)
            )
            SummaryColumn(
                label = "结余",
                amount = summary.balance,
                color = if (summary.balance < BigDecimal.ZERO) ExpenseColor
                        else MaterialTheme.colorScheme.onSurface,
                keepSign = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: BigDecimal,
    color: Color,
    keepSign: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = LabelColor
        )
        val amountText = "¥${formatAmount(amount, keepSign)}"
        val fontSize = when {
            amountText.length > 12 -> 12.sp
            amountText.length > 10 -> 13.sp
            amountText.length > 8 -> 14.sp
            amountText.length > 6 -> 16.sp
            else -> 18.sp
        }
        Text(
            text = amountText,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatAmount(amount: BigDecimal, keepSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return if (keepSign) {
        val prefix = if (amount < BigDecimal.ZERO) "-" else ""
        "$prefix${formatter.format(amount.abs())}"
    } else {
        formatter.format(amount.abs())
    }
}
