package com.mian.accountrecord.ui.category

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.ui.components.CategoryIcon
import com.mian.accountrecord.ui.components.ConfirmDialog

private val availableIcons = listOf(
    "restaurant", "directions_car", "shopping_cart", "home", "sports_esports",
    "local_hospital", "school", "phone", "checkroom", "inventory_2",
    "people", "pets", "more_horiz", "payments", "card_giftcard",
    "work", "trending_up", "redeem", "star", "favorite"
)

private val iconLabels = mapOf(
    "restaurant" to "餐饮",
    "directions_car" to "交通",
    "shopping_cart" to "购物",
    "home" to "住房",
    "sports_esports" to "娱乐",
    "local_hospital" to "医疗",
    "school" to "教育",
    "phone" to "通讯",
    "checkroom" to "服饰",
    "inventory_2" to "日用",
    "people" to "社交",
    "pets" to "宠物",
    "more_horiz" to "其他",
    "payments" to "账单",
    "card_giftcard" to "礼物",
    "work" to "工作",
    "trending_up" to "投资",
    "redeem" to "红包",
    "star" to "收藏",
    "favorite" to "喜欢"
)

private val availableColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#2196F3", "#03A9F4", "#009688", "#4CAF50", "#FF9800",
    "#FF5722", "#795548"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(CategoryViewModel.UiState())
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加分类")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab: 支出 / 收入
            val selectedTab = if (uiState.currentType == TransactionType.EXPENSE) 0 else 1
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { viewModel.switchType(TransactionType.EXPENSE) }) {
                    Text("支出", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { viewModel.switchType(TransactionType.INCOME) }) {
                    Text("收入", modifier = Modifier.padding(12.dp))
                }
            }

            CategoryList(
                categories = uiState.categories,
                onLongPress = { category ->
                    if (!category.isPreset) categoryToDelete = category
                }
            )
        }
    }

    // Add category dialog
    if (showAddDialog) {
        AddCategoryDialog(
            onConfirm = { name, icon, color ->
                viewModel.addCategory(name, icon, color)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Delete confirmation dialog
    categoryToDelete?.let { category ->
        ConfirmDialog(
            title = "删除分类",
            message = "删除「${category.name}」后，关联的记录将迁移到「其他」分类。确定删除吗？",
            onConfirm = {
                viewModel.deleteCategory(category)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryList(
    categories: List<Category>,
    onLongPress: (Category) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(categories, key = { it.id }) { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onLongPress(category) }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(category = category, size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (category.isPreset) {
                    Text(
                        text = "预设",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onConfirm: (name: String, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconIndex by remember { mutableIntStateOf(0) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Simple icon selector (show icon names)
                Text("选择图标", fontSize = 12.sp, color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableIcons.take(6).forEachIndexed { index, icon ->
                        TextButton(
                            onClick = { selectedIconIndex = index },
                            modifier = Modifier.background(
                                if (index == selectedIconIndex) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                        ) {
                            Text(iconLabels[icon] ?: icon, fontSize = 10.sp)
                        }
                    }
                }
                // Simple color selector
                Text("选择颜色", fontSize = 12.sp, color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableColors.take(6).forEachIndexed { index, color ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(color))
                        } catch (_: Exception) { Color.Gray }
                        TextButton(
                            onClick = { selectedColorIndex = index },
                            modifier = Modifier
                                .background(parsedColor.copy(alpha = if (index == selectedColorIndex) 1f else 0.3f))
                                .height(32.dp)
                                .width(32.dp)
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, availableIcons[selectedIconIndex], availableColors[selectedColorIndex])
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
