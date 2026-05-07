package com.mian.accountrecord.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Welcome screen shown after first-time login.
 * Displays user avatar (circular 80dp), nickname (20sp), and welcome message (16sp gray).
 * Auto-navigates to home after 2 seconds; "跳过" button for immediate skip.
 *
 * Validates: Requirements 8.1, 8.2
 */
@Composable
fun WelcomeScreen(
    nickname: String,
    avatarUrl: String?,
    onTimeout: () -> Unit,
    onSkip: () -> Unit
) {
    // Req 8.2: Auto-navigate after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Center content: avatar, nickname, welcome message
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Req 8.1: User avatar (circular, 80dp)
            // Using placeholder icon since no image loading library (Coil) is available
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Req 8.1: User nickname (20sp)
            Text(
                text = nickname,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Req 8.1: Welcome message (16sp gray)
            Text(
                text = "欢迎来到 AccountRecord",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        // Req 8.2: Bottom "跳过" button for immediate skip
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text("跳过")
            }
        }
    }
}
