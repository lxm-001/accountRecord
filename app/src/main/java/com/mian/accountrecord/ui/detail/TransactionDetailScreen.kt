package com.mian.accountrecord.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.ui.components.ConfirmDialog
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(TransactionDetailViewModel.UiState())
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除记录",
            message = "确定要删除这条记录吗？",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("交易详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFF44336))
                    }
                }
            )
        }
    ) { padding ->
        val tx = uiState.transaction
        if (tx == null) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("加载中...", color = Color.Gray)
            }
        } else {
            val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                minimumFractionDigits = 2; maximumFractionDigits = 2
            }
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
            val typeLabel = if (tx.type == TransactionType.EXPENSE) "支出" else "收入"
            val amountColor = if (tx.type == TransactionType.EXPENSE) Color(0xFFF44336) else Color(0xFF4CAF50)
            val prefix = if (tx.type == TransactionType.EXPENSE) "-" else "+"

            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount
                Text(
                    text = "$prefix¥${fmt.format(tx.amount)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                DetailRow("类型", typeLabel)
                DetailRow("分类", tx.category.name)
                DetailRow("日期", tx.date.format(dateFormatter))
                DetailRow("备注", tx.note ?: "无")

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除记录", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp)
    }
}
