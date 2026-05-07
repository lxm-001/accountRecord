package com.mian.accountrecord.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mian.accountrecord.domain.model.TransactionType
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val ExpenseColor = Color(0xFFF44336)
private val IncomeColor = Color(0xFF4CAF50)

@Composable
fun AmountText(
    amount: BigDecimal,
    type: TransactionType,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val formatted = formatter.format(amount)
    val prefix = when (type) {
        TransactionType.EXPENSE -> "-¥"
        TransactionType.INCOME -> "+¥"
    }
    val color = when (type) {
        TransactionType.EXPENSE -> ExpenseColor
        TransactionType.INCOME -> IncomeColor
    }

    Text(
        text = "$prefix$formatted",
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}
