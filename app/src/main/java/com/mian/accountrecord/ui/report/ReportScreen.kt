package com.mian.accountrecord.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.domain.model.MonthlySummary
import com.mian.accountrecord.domain.model.TrendPoint
import com.mian.accountrecord.ui.components.EmptyStateView
import com.mian.accountrecord.ui.components.MonthPickerDialog
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.observeAsState(ReportViewModel.UiState())
    var showMonthPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        // Period switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.switchPeriod(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
            }
            Text(
                text = "${uiState.currentPeriod.year}年${uiState.currentPeriod.monthValue}月",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showMonthPicker = true }
            )
            IconButton(onClick = { viewModel.switchPeriod(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
            }
        }

        // Summary row
        SummaryRow(summary = uiState.summary)

        Spacer(modifier = Modifier.height(12.dp))

        // Chart type selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ChartType.entries.forEach { type ->
                val label = when (type) {
                    ChartType.PIE -> "饼图"
                    ChartType.BAR -> "柱状图"
                }
                FilterChip(
                    selected = uiState.chartType == type,
                    onClick = { viewModel.switchChartType(type) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart area or empty state
        if (uiState.isLoading) {
            // Show nothing while loading to avoid flash of empty state
            Spacer(modifier = Modifier.weight(1f))
        } else if (uiState.isEmpty) {
            EmptyStateView(
                message = "暂无数据，快去记一笔吧",
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onIconClick = onNavigateToHome
            )
        } else {
            when (uiState.chartType) {
                ChartType.PIE -> PieChart(
                    expenseData = uiState.expenseData,
                    incomeData = uiState.incomeData,
                    summary = uiState.summary,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                ChartType.BAR -> BarChart(
                    data = uiState.trendData,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentYearMonth = uiState.currentPeriod,
            onSelect = { selected ->
                showMonthPicker = false
                viewModel.setPeriod(selected)
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
private fun SummaryRow(summary: MonthlySummary) {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("收入", summary.income, Color(0xFF4CAF50), fmt)
            SummaryItem("支出", summary.expense, Color(0xFFF44336), fmt)
            SummaryItem("结余", summary.balance,
                if (summary.balance < BigDecimal.ZERO) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface, fmt, keepSign = true)
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: BigDecimal, color: Color, fmt: NumberFormat, keepSign: Boolean = false) {
    val display = if (keepSign && amount < BigDecimal.ZERO) {
        "¥-${fmt.format(amount.abs())}"
    } else {
        "¥${fmt.format(amount.abs())}"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(display, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private val pieColors = listOf(
    Color(0xFFF44336), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
    Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFE91E63), Color(0xFF795548),
    Color(0xFF607D8B), Color(0xFFFFC107), Color(0xFF3F51B5), Color(0xFF8BC34A)
)

@Composable
private fun PieChart(
    expenseData: List<ReportViewModel.CategoryAmount>,
    incomeData: List<ReportViewModel.CategoryAmount>,
    summary: MonthlySummary,
    modifier: Modifier = Modifier
) {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        // Expense donut
        item {
            Text("支出分类", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (expenseData.isEmpty()) {
            item { Text("暂无支出数据", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) }
        } else {
            item { DonutChart(data = expenseData, colors = pieColors, totalLabel = "总支出", total = summary.expense) }
            items(expenseData.size) { i ->
                val item = expenseData[i]
                val total = expenseData.fold(BigDecimal.ZERO) { a, b -> a + b.amount }
                val pct = if (total > BigDecimal.ZERO) item.amount.toDouble() / total.toDouble() * 100 else 0.0
                LegendRow(item.categoryName, pieColors[i % pieColors.size], "¥${nf.format(item.amount)}", "%.1f%%".format(pct))
            }
        }

        // Income donut
        item {
            Spacer(Modifier.height(16.dp))
            Text("收入分类", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (incomeData.isEmpty()) {
            item { Text("暂无收入数据", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) }
        } else {
            item { DonutChart(data = incomeData, colors = incomeColors, totalLabel = "总收入", total = summary.income) }
            items(incomeData.size) { i ->
                val item = incomeData[i]
                val total = incomeData.fold(BigDecimal.ZERO) { a, b -> a + b.amount }
                val pct = if (total > BigDecimal.ZERO) item.amount.toDouble() / total.toDouble() * 100 else 0.0
                LegendRow(item.categoryName, incomeColors[i % incomeColors.size], "¥${nf.format(item.amount)}", "%.1f%%".format(pct))
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private val incomeColors = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFF00BCD4),
    Color(0xFF8BC34A), Color(0xFF3F51B5), Color(0xFFFF9800), Color(0xFF009688)
)

@Composable
private fun DonutChart(
    data: List<ReportViewModel.CategoryAmount>,
    colors: List<Color>,
    totalLabel: String,
    total: BigDecimal
) {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }
    val dataTotal = data.fold(BigDecimal.ZERO) { a, b -> a + b.amount }

    Canvas(
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)
    ) {
        val cx = size.width / 2; val cy = size.height / 2
        val maxR = minOf(size.width, size.height) / 2 * 0.9f
        val strokeW = maxR * 0.28f
        val r = maxR - strokeW / 2
        val tl = Offset(cx - r, cy - r)
        val sz = Size(r * 2, r * 2)

        if (dataTotal > BigDecimal.ZERO) {
            var angle = -90f
            data.forEachIndexed { i, item ->
                val sweep = item.amount.toFloat() / dataTotal.toFloat() * 360f
                val gap = if (data.size > 1) 2f else 0f
                // Shadow
                drawArc(colors[i % colors.size].copy(alpha = 0.3f), angle + gap / 2, sweep - gap, false,
                    Offset(tl.x, tl.y + 2f), sz, style = Stroke(strokeW + 1f, cap = StrokeCap.Butt))
                // Main
                drawArc(colors[i % colors.size], angle + gap / 2, sweep - gap, false, tl, sz,
                    style = Stroke(strokeW, cap = StrokeCap.Butt))
                // Highlight
                drawArc(Color.White.copy(alpha = 0.15f), angle + gap / 2, sweep - gap, false,
                    Offset(tl.x, tl.y - 1f), sz, style = Stroke(strokeW * 0.2f, cap = StrokeCap.Butt))
                angle += sweep
            }
        }

        // Center
        drawCircle(Color.White, r - strokeW / 2 - 2f, Offset(cx, cy))
        drawContext.canvas.nativeCanvas.drawText(totalLabel, cx, cy - 14f,
            android.graphics.Paint().apply { textSize = 20f; color = android.graphics.Color.GRAY; textAlign = android.graphics.Paint.Align.CENTER })
        drawContext.canvas.nativeCanvas.drawText("¥${nf.format(total)}", cx, cy + 14f,
            android.graphics.Paint().apply { textSize = 30f; isFakeBoldText = true; color = android.graphics.Color.DKGRAY; textAlign = android.graphics.Paint.Align.CENTER })
    }
}

@Composable
private fun LegendRow(name: String, color: Color, amount: String, pct: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Canvas(Modifier.size(12.dp)) { drawCircle(color) }
        Spacer(Modifier.width(8.dp))
        Text(name, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(amount, fontSize = 13.sp, color = Color.DarkGray)
        Spacer(Modifier.width(8.dp))
        Text(pct, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
private fun BarChart(
    data: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val incomeColor = Color(0xFF66BB6A)
    val incomeColorDark = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFEF5350)
    val expenseColorDark = Color(0xFFC62828)
    val gridColor = Color(0xFFF0F0F0)
    val labelColor = android.graphics.Color.parseColor("#888888")

    // Animate bar heights on data change
    val animProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 600,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    // Track selected bar index for tooltip
    var selectedIndex by remember { mutableStateOf(-1) }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(10.dp)) {
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(incomeColor, incomeColorDark)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("收入", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.width(16.dp))
            Canvas(Modifier.size(10.dp)) {
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(expenseColor, expenseColorDark)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("支出", fontSize = 11.sp, color = Color.Gray)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 36.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val groupWidth = size.width.toFloat() / data.size
                        val tapped = (offset.x / groupWidth).toInt().coerceIn(0, data.size - 1)
                        selectedIndex = if (selectedIndex == tapped) -1 else tapped
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val maxVal = data.maxOf { maxOf(it.income, it.expense) }.toFloat().coerceAtLeast(1f)
            // Add 15% headroom so bars don't touch the top
            val chartMax = maxVal * 1.15f
            val groupWidth = w / data.size
            val barWidth = (groupWidth * 0.30f).coerceIn(6f, 40f)
            val barGap = (barWidth * 0.15f).coerceAtLeast(2f)
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2.5f)
            val nf = java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 }
            val anim = animProgress.value

            // Grid lines + Y-axis labels (5 lines including baseline)
            for (i in 0..4) {
                val y = h - (h * i / 4)
                val value = chartMax * i / 4
                // Dashed-style grid: lighter, thinner
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                drawContext.canvas.nativeCanvas.drawText(
                    nf.format(value),
                    -8f,
                    y + 8f,
                    android.graphics.Paint().apply {
                        textSize = 22f
                        color = labelColor
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                )
            }

            data.forEachIndexed { i, point ->
                val cx = groupWidth * i + groupWidth / 2
                val isSelected = i == selectedIndex
                val barAlpha = if (selectedIndex >= 0 && !isSelected) 0.45f else 1f

                // Income bar (left of center)
                val rawIncomeH = point.income.toFloat() / chartMax * h
                val incomeH = rawIncomeH * anim
                if (incomeH > 1f) {
                    val barLeft = cx - barWidth - barGap / 2
                    val barTop = h - incomeH

                    // Soft shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.08f * barAlpha),
                        topLeft = Offset(barLeft + 2f, barTop + 4f),
                        size = Size(barWidth, incomeH),
                        cornerRadius = cornerRadius
                    )
                    // Main bar with vertical gradient
                    drawRoundRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                incomeColor.copy(alpha = barAlpha),
                                incomeColorDark.copy(alpha = barAlpha)
                            ),
                            startY = barTop,
                            endY = h
                        ),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, incomeH),
                        cornerRadius = cornerRadius
                    )
                    // Subtle inner highlight (left edge)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f * barAlpha),
                        topLeft = Offset(barLeft + 1.5f, barTop + 2f),
                        size = Size(barWidth * 0.25f, (incomeH - 4f).coerceAtLeast(0f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )

                    // Value label on top when selected
                    if (isSelected) {
                        val label = "¥${nf.format(point.income)}"
                        val paint = android.graphics.Paint().apply {
                            textSize = 22f
                            color = android.graphics.Color.parseColor("#2E7D32")
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label, barLeft + barWidth / 2, barTop - 8f, paint
                        )
                    }
                }

                // Expense bar (right of center)
                val rawExpenseH = point.expense.toFloat() / chartMax * h
                val expenseH = rawExpenseH * anim
                if (expenseH > 1f) {
                    val barLeft = cx + barGap / 2
                    val barTop = h - expenseH

                    // Soft shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.08f * barAlpha),
                        topLeft = Offset(barLeft + 2f, barTop + 4f),
                        size = Size(barWidth, expenseH),
                        cornerRadius = cornerRadius
                    )
                    // Main bar with vertical gradient
                    drawRoundRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                expenseColor.copy(alpha = barAlpha),
                                expenseColorDark.copy(alpha = barAlpha)
                            ),
                            startY = barTop,
                            endY = h
                        ),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, expenseH),
                        cornerRadius = cornerRadius
                    )
                    // Subtle inner highlight
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f * barAlpha),
                        topLeft = Offset(barLeft + 1.5f, barTop + 2f),
                        size = Size(barWidth * 0.25f, (expenseH - 4f).coerceAtLeast(0f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )

                    // Value label on top when selected
                    if (isSelected) {
                        val label = "¥${nf.format(point.expense)}"
                        val paint = android.graphics.Paint().apply {
                            textSize = 22f
                            color = android.graphics.Color.parseColor("#C62828")
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label, barLeft + barWidth / 2, barTop - 8f, paint
                        )
                    }
                }

                // Selected indicator: subtle highlight band behind bars
                if (isSelected) {
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.06f),
                        topLeft = Offset(groupWidth * i, 0f),
                        size = Size(groupWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                    )
                }

                // Day label
                val dayLabelPaint = android.graphics.Paint().apply {
                    textSize = 22f
                    color = if (isSelected) android.graphics.Color.parseColor("#333333") else labelColor
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = isSelected
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${point.date.dayOfMonth}日", cx, h + 28f, dayLabelPaint
                )
            }
        }
    }
}
