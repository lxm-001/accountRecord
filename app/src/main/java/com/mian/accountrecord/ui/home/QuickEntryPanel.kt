package com.mian.accountrecord.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.ui.components.CategoryIcon
import com.mian.accountrecord.ui.components.ConfirmDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter


private val ExpenseColor = Color(0xFFF44336)
private val IncomeColor = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryPanel(
    state: HomeViewModel.QuickEntryState,
    categories: List<Category>,
    currentLedgerName: String,
    onTypeChange: (TransactionType) -> Unit,
    onAmountInput: (String) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    hasData: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDiscardDialog by remember { mutableStateOf(false) }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = "放弃本次记账？",
            message = "已输入的数据将不会保存",
            onConfirm = {
                showDiscardDialog = false
                onDismiss()
            },
            onDismiss = { showDiscardDialog = false },
            confirmText = "放弃",
            dismissText = "继续编辑"
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (hasData) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // 1. Top bar: type toggle + save button
            TopBar(
                type = state.type,
                onTypeChange = onTypeChange,
                onSave = onSave,
                canSave = state.amount.isNotEmpty()
                        && state.amount != "."
                        && state.amount != "0."
                        && state.selectedCategory != null
            )

            // 2. Amount display
            AmountDisplay(
                amount = state.amount,
                type = state.type
            )

            // 3. Category grid (single row on small screens)
            CategoryGrid(
                categories = categories,
                selectedCategory = state.selectedCategory,
                onCategorySelect = onCategorySelect
            )

            // 4. Info chips
            InfoChips(
                selectedDate = state.selectedDate,
                ledgerName = currentLedgerName,
                note = state.note,
                onDateChange = onDateChange,
                onNoteChange = onNoteChange
            )

            // 5. Number keypad — fixed height, no weight
            NumberKeypad(
                onInput = onAmountInput
            )
        }
    }
}


@Composable
private fun TopBar(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Segmented toggle for expense/income
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
        ) {
            val expenseSelected = type == TransactionType.EXPENSE
            Box(
                modifier = Modifier
                    .clickable { onTypeChange(TransactionType.EXPENSE) }
                    .background(if (expenseSelected) ExpenseColor.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "支出",
                    color = if (expenseSelected) ExpenseColor else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = if (expenseSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .clickable { onTypeChange(TransactionType.INCOME) }
                    .background(if (!expenseSelected) IncomeColor.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "收入",
                    color = if (!expenseSelected) IncomeColor else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = if (!expenseSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(text = "保存", fontSize = 14.sp)
        }
    }
}

@Composable
private fun AmountDisplay(amount: String, type: TransactionType) {
    val displayAmount = if (amount.isEmpty()) "0.00" else amount
    val textColor = if (amount.isEmpty()) Color.Gray else Color.Black
    val activeColor = when (type) {
        TransactionType.EXPENSE -> ExpenseColor
        TransactionType.INCOME -> IncomeColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "¥$displayAmount",
            color = if (amount.isEmpty()) textColor else activeColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = selectedCategory?.id == category.id
            CategoryGridItem(
                category = category,
                isSelected = isSelected,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

@Composable
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryIcon(category = category, size = 32.dp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        // Selected underline indicator
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(
                    if (isSelected) accentColor else Color.Transparent,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}


@Composable
private fun InfoChips(
    selectedDate: LocalDate,
    ledgerName: String,
    note: String,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteInput by remember(note) { mutableStateOf(note) }

    if (showNoteDialog) {
        NoteInputDialog(
            currentNote = noteInput,
            onConfirm = { newNote ->
                onNoteChange(newNote)
                showNoteDialog = false
            },
            onDismiss = { showNoteDialog = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date chip
        val dateText = when (selectedDate) {
            LocalDate.now() -> "今天"
            LocalDate.now().minusDays(1) -> "昨天"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("M月d日"))
        }
        FilterChip(
            selected = false,
            onClick = { /* DatePicker would be triggered here */ },
            label = { Text(dateText, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = "日期",
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Ledger chip (read-only)
        FilterChip(
            selected = false,
            onClick = { },
            label = { Text(ledgerName, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    Icons.Filled.MenuBook,
                    contentDescription = "账本",
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Note chip
        val noteDisplay = if (note.isBlank()) "添加备注" else note.take(10)
        FilterChip(
            selected = note.isNotBlank(),
            onClick = { showNoteDialog = true },
            label = { Text(noteDisplay, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "备注",
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

@Composable
private fun NoteInputDialog(
    currentNote: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentNote) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加备注") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("请输入备注") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


@Composable
private fun NumberKeypad(
    onInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onInput(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier.padding(2.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (key == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "退格",
                    modifier = Modifier.size(20.dp),
                    tint = Color.DarkGray
                )
            } else {
                Text(
                    text = key,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }
    }
}