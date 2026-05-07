package com.mian.accountrecord.ui.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mian.accountrecord.R
import kotlinx.coroutines.launch

// WeChat green and Alipay blue brand colors
private val WeChatGreen = Color(0xFF07C160)
private val AlipayBlue = Color(0xFF1677FF)
private val DisabledGray = Color(0xFFBDBDBD)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: (isFirstLogin: Boolean, nickname: String, avatarUrl: String?) -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(LoginViewModel.LoginUiState())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Shake animation offset for checkbox area (Req 1.6)
    val shakeOffset = remember { Animatable(0f) }

    // Navigate on login success
    LaunchedEffect(uiState.loginResult) {
        uiState.loginResult?.let { result ->
            onLoginSuccess(result.isFirstLogin, result.user.nickname, result.user.avatarUrl)
        }
    }

    // Show error messages as Toast
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onErrorDismissed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo (72dp) - Req 1.2
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name "AccountRecord" (20sp bold) - Req 1.2
            Text(
                text = "AccountRecord",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Welcome text (14sp gray) - Req 1.2
            Text(
                text = "极简记账，从这里开始",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Cooldown warning (Req 6.1)
            if (uiState.cooldownSeconds > 0) {
                Text(
                    text = "操作过于频繁，请 ${uiState.cooldownSeconds} 秒后重试",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // WeChat login button (Req 1.2, 1.4, 1.5, 7.2)
            LoginButton(
                text = "微信登录",
                icon = { Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White) },
                backgroundColor = WeChatGreen,
                enabled = uiState.isAgreementChecked && !uiState.isLoading && uiState.cooldownSeconds == 0,
                isLoading = uiState.isLoading,
                onClick = { viewModel.onWeChatLoginClick() },
                onDisabledClick = {
                    // Req 1.6: Toast + shake animation when agreement not checked
                    if (!uiState.isAgreementChecked) {
                        Toast.makeText(context, "请先阅读并同意用户协议和隐私政策", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            shakeCheckbox(shakeOffset)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Alipay login button (Req 1.2, 1.4, 1.5, 7.2)
            LoginButton(
                text = "支付宝登录",
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White) },
                backgroundColor = AlipayBlue,
                enabled = uiState.isAgreementChecked && !uiState.isLoading && uiState.cooldownSeconds == 0,
                isLoading = uiState.isLoading,
                isLoadingProvider = false, // Only show spinner on the active provider button
                onClick = { viewModel.onAlipayLoginClick() },
                onDisabledClick = {
                    if (!uiState.isAgreementChecked) {
                        Toast.makeText(context, "请先阅读并同意用户协议和隐私政策", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            shakeCheckbox(shakeOffset)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 跳过登录按钮
            TextButton(
                onClick = {
                    if (uiState.isAgreementChecked) {
                        viewModel.skipLogin()
                    } else {
                        Toast.makeText(context, "请先阅读并同意用户协议和隐私政策", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            shakeCheckbox(shakeOffset)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "跳过登录，直接使用",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Agreement checkbox area (Req 1.3) with shake animation (Req 1.6)
            AgreementSection(
                isChecked = uiState.isAgreementChecked,
                onCheckedChange = viewModel::onAgreementCheckedChange,
                onNavigateToAgreement = onNavigateToAgreement,
                onNavigateToPrivacy = onNavigateToPrivacy,
                shakeOffset = shakeOffset.value
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Loading overlay (Req 7.3)
        if (uiState.isLoading) {
            LoadingOverlay(message = uiState.loadingMessage)
        }
    }

    // Back key handler during loading (Req 7.7)
    BackHandler(enabled = uiState.isLoading) {
        viewModel.onShowCancelDialog()
    }

    // Cancel login confirmation dialog (Req 7.7)
    if (uiState.showCancelDialog) {
        CancelLoginDialog(
            onConfirm = viewModel::onCancelLogin,
            onDismiss = viewModel::onDismissCancelDialog
        )
    }
}

/**
 * Login button with loading state support.
 * When [enabled] is false but user taps, [onDisabledClick] fires (for Toast + shake).
 * When [isLoading] is true, shows a CircularProgressIndicator instead of text (Req 7.2).
 */
@Composable
private fun LoginButton(
    text: String,
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit,
    isLoadingProvider: Boolean = true
) {
    val buttonColor = if (enabled) backgroundColor else DisabledGray

    Button(
        onClick = {
            if (enabled) onClick() else onDisabledClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = true, // Always clickable to handle disabled click feedback
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        )
    ) {
        if (isLoading && isLoadingProvider) {
            // Req 7.2: Replace button content with CircularProgressIndicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 16.sp)
        }
    }
}

/**
 * Agreement checkbox + clickable links section (Req 1.3).
 * Supports shake animation via [shakeOffset] (Req 1.6).
 */
@Composable
private fun AgreementSection(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    shakeOffset: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.dp.roundToPx(), 0) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )

        val annotatedText = buildAnnotatedString {
            append("我已阅读并同意")

            pushStringAnnotation(tag = "agreement", annotation = "agreement")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("《用户协议》")
            }
            pop()

            append("和")

            pushStringAnnotation(tag = "privacy", annotation = "privacy")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("《隐私政策》")
            }
            pop()
        }

        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            onClick = { offset ->
                annotatedText.getStringAnnotations("agreement", offset, offset)
                    .firstOrNull()?.let {
                        onNavigateToAgreement()
                        return@ClickableText
                    }
                annotatedText.getStringAnnotations("privacy", offset, offset)
                    .firstOrNull()?.let {
                        onNavigateToPrivacy()
                        return@ClickableText
                    }
            }
        )
    }
}

/**
 * Semi-transparent loading overlay with message (Req 7.3).
 * Black 30% opacity background, centered white text + spinner.
 */
@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(enabled = false) { }, // Block interaction
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message.ifEmpty { "正在登录..." },
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Cancel login confirmation dialog (Req 7.7).
 */
@Composable
private fun CancelLoginDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("是否取消登录？") },
        text = { Text("当前正在登录中，确定要取消吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认取消")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("继续等待")
            }
        }
    )
}

/**
 * Shake animation for the checkbox area (Req 1.6).
 * Horizontal displacement ±4dp over 300ms.
 */
private suspend fun shakeCheckbox(offset: Animatable<Float, *>) {
    val shakeValues = listOf(4f, -4f, 3f, -3f, 2f, -2f, 0f)
    val durationPerStep = 300 / shakeValues.size

    for (value in shakeValues) {
        offset.animateTo(
            targetValue = value,
            animationSpec = tween(durationMillis = durationPerStep)
        )
    }
}
