package com.mian.accountrecord.ui.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.domain.model.BudgetProgress
import com.mian.accountrecord.ui.components.MonthPickerDialog
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

private fun Color.toArgbInt(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

private val BudgetGreen = Color(0xFF4CAF50)
private val BudgetOrange = Color(0xFFFF9800)
private val BudgetRed = Color(0xFFF44336)

private fun budgetColor(ratio: Float) = when {
    ratio > 1.0f -> BudgetRed
    ratio >= 0.8f -> BudgetOrange
    else -> BudgetGreen
}

private val fmt: NumberFormat
    get() = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.observeAsState(BudgetViewModel.UiState())
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var editingCategoryId by remember { mutableStateOf<Long?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            androidx.compose.material3.LargeFloatingActionButton(
                onClick = {
                    editingCategoryId = null
                    showSetBudgetDialog = true
                },
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, contentDescription = "设置预算", modifier = Modifier.size(32.dp)) }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Month switcher
                item(key = "month") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.switchMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一月")
                        }
                        Text(
                            text = "${uiState.currentYearMonth.year}年${uiState.currentYearMonth.monthValue}月",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showMonthPicker = true }
                        )
                        IconButton(onClick = { viewModel.switchMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一月")
                        }
                    }
                }

                if (!uiState.hasAnyBudget) {
                    // Empty guide
                    item(key = "empty") {
                        ElevatedCard(
                            onClick = {
                                editingCategoryId = null
                                showSetBudgetDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📊", fontSize = 40.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("开始管理你的预算", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                                Text("设置月度预算，帮你控制支出", fontSize = 14.sp, color = Color.Gray)
                                Text("点击右下角按钮设置第一个预算", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    // Overview card
                    uiState.totalBudget?.let { total ->
                        item(key = "overview") {
                            BudgetOverviewCard(
                                progress = total,
                                yearMonth = uiState.currentYearMonth,
                                onClick = {
                                    editingCategoryId = null
                                    showSetBudgetDialog = true
                                }
                            )
                        }
                    }

                    // Category budgets
                    items(uiState.categoryBudgets, key = { it.budget.id }) { progress ->
                        val catName = uiState.categories.find { it.id == progress.budget.categoryId }?.name ?: "未知"
                        CategoryBudgetCard(
                            label = catName,
                            progress = progress,
                            yearMonth = uiState.currentYearMonth,
                            onClick = {
                                editingCategoryId = progress.budget.categoryId
                                showSetBudgetDialog = true
                            }
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentYearMonth = uiState.currentYearMonth,
            onSelect = { showMonthPicker = false; viewModel.setMonth(it)},
            onDismiss = { showMonthPicker = false }
        )
    }

    if (showSetBudgetDialog) {
        val title = when {
            editingCategoryId != null -> "设置分类预算"
            uiState.totalBudget != null -> "修改月度总预算"
            else -> "设置月度总预算"
        }
        SetBudgetDialog(
            title = title,
            onConfirm = { viewModel.saveBudget(editingCategoryId, it); showSetBudgetDialog = false },
            onDismiss = { showSetBudgetDialog = false }
        )
    }
}

@Composable
private fun BudgetOverviewCard(
    progress: BudgetProgress,
    yearMonth: YearMonth,
    onClick: () -> Unit
) {
    val remaining = (progress.budget.amount - progress.spent).coerceAtLeast(BigDecimal.ZERO)
    val daysLeft = yearMonth.lengthOfMonth() - java.time.LocalDate.now().dayOfMonth + 1
    val dailyAvailable = if (daysLeft > 0) remaining.divide(BigDecimal(daysLeft), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
    val color = budgetColor(progress.ratio)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("本月预算总览", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))

            // Water level ball (Canvas fallback; replace with Lottie when water_ball.json is added to res/raw)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas3DWaterBall(progress = progress, color = color)
            }

            Spacer(Modifier.height(16.dp))

            // Data grid 2x2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DataCell("预算", "¥${fmt.format(progress.budget.amount)}", Modifier.weight(1f))
                DataCell("已花", "¥${fmt.format(progress.spent)}", Modifier.weight(1f), color)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DataCell("剩余", "¥${fmt.format(remaining)}", Modifier.weight(1f))
                DataCell("日均可用", "¥${fmt.format(dailyAvailable)}", Modifier.weight(1f))
            }

            // Over-budget warning
            if (progress.ratio >= 1.0f) {
                Spacer(Modifier.height(8.dp))
                val overAmount = progress.spent - progress.budget.amount
                Text("🚨 已超支 ¥${fmt.format(overAmount)}", fontSize = 13.sp, color = BudgetRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DataCell(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
    }
}

@Composable
private fun CategoryBudgetCard(
    label: String,
    progress: BudgetProgress,
    yearMonth: YearMonth,
    onClick: () -> Unit
) {
    val remaining = (progress.budget.amount - progress.spent).coerceAtLeast(BigDecimal.ZERO)
    val daysLeft = yearMonth.lengthOfMonth() - java.time.LocalDate.now().dayOfMonth + 1
    val dailyAvailable = if (daysLeft > 0) remaining.divide(BigDecimal(daysLeft), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
    val color = budgetColor(progress.ratio)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("¥${fmt.format(progress.spent)} / ¥${fmt.format(progress.budget.amount)}", fontSize = 13.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E0E0))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction = progress.ratio.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("剩余 ¥${fmt.format(remaining)}", fontSize = 12.sp, color = Color.Gray)
                Text("日均 ¥${fmt.format(dailyAvailable)}", fontSize = 12.sp, color = Color.Gray)
            }
            if (progress.ratio >= 1.0f) {
                val over = progress.spent - progress.budget.amount
                Text("超支 ¥${fmt.format(over)}", fontSize = 12.sp, color = BudgetRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Canvas3DWaterBall(progress: BudgetProgress, color: Color) {
    val argb = color.toArgbInt()
    val ratio = progress.ratio.coerceIn(0f, 1f)

    // Pre-allocate native objects outside DrawScope to avoid per-frame allocation
    val bgPaint = remember { android.graphics.Paint().apply { isAntiAlias = true } }
    val waterPaint = remember { android.graphics.Paint().apply { isAntiAlias = true } }
    val strokePaint = remember { android.graphics.Paint().apply {
        isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f
    } }
    val labelPaint = remember { android.graphics.Paint().apply {
        textSize = 22f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
    } }
    val pctPaint = remember { android.graphics.Paint().apply {
        textSize = 42f; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
    } }
    val clipPath = remember { android.graphics.Path() }
    val waterPath = remember { android.graphics.Path() }
    val circlePath = remember { android.graphics.Path() }

    Canvas(modifier = Modifier.size(150.dp)) {
        val r = size.minDimension / 2
        val cx = center.x
        val cy = center.y

        // Background circle with radial gradient
        bgPaint.shader = android.graphics.RadialGradient(
            cx - r * 0.25f, cy - r * 0.3f, r * 1.6f,
            intArrayOf(
                android.graphics.Color.argb(255, 245, 245, 250),
                android.graphics.Color.argb(255, 200, 200, 215)
            ),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        drawContext.canvas.nativeCanvas.drawCircle(cx, cy, r - 3f, bgPaint)

        // Water level
        if (ratio > 0f) {
            val wt = cy + r - (2 * r * ratio)
            // Ensure gradient start and end differ by at least 1px to avoid native crash
            val gradientEnd = (cy + r).coerceAtLeast(wt + 1f)

            circlePath.reset()
            circlePath.addCircle(cx, cy, r - 3f, android.graphics.Path.Direction.CW)

            waterPath.reset()
            waterPath.moveTo(cx - r, wt)
            var x = cx - r
            var up = true
            val waveWidth = r / 2.5f
            while (x < cx + r + waveWidth) {
                waterPath.quadTo(
                    x + waveWidth / 2, if (up) wt - 5f else wt + 5f,
                    x + waveWidth, wt
                )
                x += waveWidth
                up = !up
            }
            waterPath.lineTo(cx + r, cy + r)
            waterPath.lineTo(cx - r, cy + r)
            waterPath.close()

            clipPath.reset()
            clipPath.op(waterPath, circlePath, android.graphics.Path.Op.INTERSECT)

            waterPaint.shader = android.graphics.LinearGradient(
                cx, wt, cx, gradientEnd,
                android.graphics.Color.argb(80, android.graphics.Color.red(argb), android.graphics.Color.green(argb), android.graphics.Color.blue(argb)),
                android.graphics.Color.argb(180, android.graphics.Color.red(argb), android.graphics.Color.green(argb), android.graphics.Color.blue(argb)),
                android.graphics.Shader.TileMode.CLAMP
            )
            drawContext.canvas.nativeCanvas.drawPath(clipPath, waterPaint)
        }

        // Border ring
        strokePaint.color = argb
        drawContext.canvas.nativeCanvas.drawCircle(cx, cy, r - 2f, strokePaint)

        // Text labels
        val pct = (progress.ratio * 100).toInt().coerceAtMost(999)
        labelPaint.color = android.graphics.Color.GRAY
        drawContext.canvas.nativeCanvas.drawText("已使用", cx, cy - 24f, labelPaint)
        pctPaint.color = android.graphics.Color.DKGRAY
        drawContext.canvas.nativeCanvas.drawText("${pct}%", cx, cy + 16f, pctPaint)
    }
}

@Composable
private fun SetBudgetDialog(title: String, onConfirm: (BigDecimal) -> Unit, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val parsedAmount = amountText.toBigDecimalOrNull()
    val isValid = parsedAmount != null && parsedAmount > BigDecimal.ZERO

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💰", fontSize = 24.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("输入预算金额", color = Color.Gray) },
                    prefix = { Text("¥ ", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "设置合理的预算，帮你更好地控制支出",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsedAmount?.let { onConfirm(it) } },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("确认保存", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("取消", fontSize = 14.sp, color = Color.Gray)
            }
        }
    )
}
