package com.mian.accountrecord.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.ui.components.MonthPickerDialog
import java.time.YearMonth

@Composable
fun HomeTopBar(
    currentLedger: Ledger?,
    ledgers: List<Ledger>,
    currentYearMonth: YearMonth,
    onSwitchMonth: (Int) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onSwitchLedger: (Long) -> Unit,
    onManageLedgers: () -> Unit
) {
    var ledgerMenuExpanded by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: ledger name + dropdown
        Row(
            modifier = Modifier.clickable { ledgerMenuExpanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentLedger?.name ?: "账本",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "切换账本",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DropdownMenu(
                expanded = ledgerMenuExpanded,
                onDismissRequest = { ledgerMenuExpanded = false },
                shape = RoundedCornerShape(16.dp)
            ) {
                ledgers.forEach { ledger ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = ledger.name,
                                fontWeight = if (ledger.id == currentLedger?.id) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            ledgerMenuExpanded = false
                            onSwitchLedger(ledger.id)
                        }
                    )
                }
                if (ledgers.isNotEmpty()) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text("管理账本") },
                    onClick = {
                        ledgerMenuExpanded = false
                        onManageLedgers()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right: month switcher (clickable date opens picker)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSwitchMonth(-1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一月"
                )
            }
            Text(
                text = "${currentYearMonth.year}年${currentYearMonth.monthValue}月",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showMonthPicker = true }
            )
            IconButton(onClick = { onSwitchMonth(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一月"
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentYearMonth = currentYearMonth,
            onSelect = { selected ->
                showMonthPicker = false
                onSelectMonth(selected)
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}
