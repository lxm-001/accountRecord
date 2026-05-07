package com.mian.accountrecord.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    onBack: () -> Unit,
    viewModel: UserSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editField by remember { mutableStateOf<EditField?>(null) }
    var editValue by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Take persistable read permission so the URI survives app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* some providers don't support persistable */ }
            viewModel.updateAvatar(it.toString())
            Toast.makeText(context, "头像已更新", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Avatar section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.avatarUrl != null) {
                        AsyncImage(
                            model = uiState.avatarUrl,
                            contentDescription = "头像",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "头像",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击更换头像", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nickname
            SettingsRow(
                icon = Icons.Filled.Person,
                label = "昵称",
                value = uiState.nickname.ifEmpty { "未设置" },
                onClick = {
                    editField = EditField.NICKNAME
                    editValue = uiState.nickname
                }
            )

            // Phone
            SettingsRow(
                icon = Icons.Filled.Phone,
                label = "手机号",
                value = uiState.phone.ifEmpty { "未绑定" },
                valueColor = if (uiState.phone.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    editField = EditField.PHONE
                    editValue = uiState.phone
                }
            )

            // Email
            SettingsRow(
                icon = Icons.Filled.Email,
                label = "邮箱",
                value = uiState.email.ifEmpty { "未绑定" },
                valueColor = if (uiState.email.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    editField = EditField.EMAIL
                    editValue = uiState.email
                }
            )

            // Login provider (read-only)
            SettingsRow(
                icon = Icons.Filled.Person,
                label = "登录方式",
                value = uiState.providerLabel.ifEmpty { "游客" },
                showArrow = false,
                onClick = {}
            )
        }
    }

    // Edit dialog
    editField?.let { field ->
        val title = when (field) {
            EditField.NICKNAME -> "修改昵称"
            EditField.PHONE -> "绑定手机号"
            EditField.EMAIL -> "绑定邮箱"
        }
        val placeholder = when (field) {
            EditField.NICKNAME -> "请输入昵称"
            EditField.PHONE -> "请输入手机号"
            EditField.EMAIL -> "请输入邮箱地址"
        }

        AlertDialog(
            onDismissRequest = { editField = null },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (field) {
                        EditField.NICKNAME -> {
                            if (editValue.isNotBlank()) {
                                viewModel.updateNickname(editValue.trim())
                                Toast.makeText(context, "昵称已更新", Toast.LENGTH_SHORT).show()
                            }
                        }
                        EditField.PHONE -> {
                            if (editValue.matches(Regex("^1[3-9]\\d{9}$"))) {
                                viewModel.updatePhone(editValue.trim())
                                Toast.makeText(context, "手机号已绑定", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "请输入正确的手机号", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                        }
                        EditField.EMAIL -> {
                            if (editValue.contains("@") && editValue.contains(".")) {
                                viewModel.updateEmail(editValue.trim())
                                Toast.makeText(context, "邮箱已绑定", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "请输入正确的邮箱地址", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                        }
                    }
                    editField = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editField = null }) { Text("取消") }
            }
        )
    }
}

private enum class EditField { NICKNAME, PHONE, EMAIL }

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        enabled = showArrow,
        modifier = Modifier
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                color = valueColor,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            if (showArrow) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}
