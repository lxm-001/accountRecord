package com.mian.accountrecord.ui.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.model.LedgerTemplate
import com.mian.accountrecord.ui.components.ConfirmDialog

private val templateOptions = listOf(
    LedgerTemplate.DAILY to "日常",
    LedgerTemplate.TRAVEL to "旅行",
    LedgerTemplate.FAMILY to "家庭",
    LedgerTemplate.PROJECT to "项目",
    LedgerTemplate.CUSTOM to "自定义"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerManageScreen(
    viewModel: LedgerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(LedgerViewModel.UiState())
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var ledgerToDelete by remember { mutableStateOf<Ledger?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账本管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新建账本")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(uiState.ledgers, key = { it.id }) { ledger ->
                LedgerItem(
                    ledger = ledger,
                    onSwitch = { viewModel.switchTo(ledger.id) },
                    onDelete = { ledgerToDelete = ledger }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateLedgerDialog(
            onConfirm = { name, template ->
                viewModel.create(name, "menu_book", template)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    ledgerToDelete?.let { ledger ->
        ConfirmDialog(
            title = "删除账本",
            message = "确定要删除「${ledger.name}」吗？该账本下的所有记录将被删除。",
            onConfirm = {
                viewModel.delete(ledger)
                ledgerToDelete = null
            },
            onDismiss = { ledgerToDelete = null }
        )
    }
}

@Composable
private fun LedgerItem(
    ledger: Ledger,
    onSwitch: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onSwitch)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ledger.name,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (ledger.isActive) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "当前账本",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.Gray)
        }
    }
}

@Composable
private fun CreateLedgerDialog(
    onConfirm: (name: String, template: LedgerTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(LedgerTemplate.DAILY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账本名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("选择模板", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    templateOptions.forEach { (template, label) ->
                        TextButton(onClick = { selectedTemplate = template }) {
                            Text(
                                text = label,
                                color = if (selectedTemplate == template)
                                    MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedTemplate) },
                enabled = name.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
