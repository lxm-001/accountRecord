package com.mian.accountrecord.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mian.accountrecord.domain.model.BudgetProgress
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val BudgetGreen = Color(0xFF4CAF50)
private val BudgetOrange = Color(0xFFFF9800)
private val BudgetRed = Color(0xFFF44336)
private val LabelColor = Color(0xFF999999)
private val TrackColor = Color(0xFFE0E0E0)

@Composable
fun BudgetProgressBar(
    budgetProgress: BudgetProgress,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = budgetProgress.ratio
    val progressColor = when {
        ratio > 1.0f -> BudgetRed
        ratio >= 0.8f -> BudgetOrange
        else -> BudgetGreen
    }
    val clampedRatio = ratio.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top row: "本月预算" left + "¥spent / ¥total" right + gear icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "本月预算",
                fontSize = 14.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "¥${formatBudgetAmount(budgetProgress.spent)} / ¥${formatBudgetAmount(budgetProgress.budget.amount)}",
                fontSize = 14.sp,
                color = LabelColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onNavigateToBudget,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "预算设置",
                    modifier = Modifier.size(16.dp),
                    tint = LabelColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar: 8dp height, 4dp corner radius
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = clampedRatio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun formatBudgetAmount(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(amount.abs())
}
