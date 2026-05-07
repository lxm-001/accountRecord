package com.mian.accountrecord.ui.billimport

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.util.CsvSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillImportScreen(
    viewModel: ImportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(ImportViewModel.UiState())
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                viewModel.importFile(stream)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账单导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Source selector
            Text("选择账单来源", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = uiState.source == CsvSource.ALIPAY,
                    onClick = { viewModel.setSource(CsvSource.ALIPAY) },
                    label = { Text("支付宝") }
                )
                FilterChip(
                    selected = uiState.source == CsvSource.WECHAT,
                    onClick = { viewModel.setSource(CsvSource.WECHAT) },
                    label = { Text("微信") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // File picker button
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("text/*"))
                },
                enabled = !uiState.isLoading
            ) {
                Text("选择 CSV 文件")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("正在解析...", color = Color.Gray)
            }

            // Import result
            uiState.importResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("导入完成", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("成功导入 ${result.importedCount} 条记录")
                        if (result.duplicateCount > 0) {
                            Text("跳过 ${result.duplicateCount} 条重复记录", color = Color.Gray)
                        }
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(error, color = Color(0xFFF44336), fontSize = 13.sp)
            }
        }
    }
}
