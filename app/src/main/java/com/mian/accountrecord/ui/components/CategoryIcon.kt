package com.mian.accountrecord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mian.accountrecord.domain.model.Category

private val iconMap: Map<String, ImageVector> = mapOf(
    "restaurant" to Icons.Filled.Restaurant,
    "commute" to Icons.Filled.Commute,
    "shopping_cart" to Icons.Filled.ShoppingCart,
    "home" to Icons.Filled.Home,
    "sports_esports" to Icons.Filled.SportsEsports,
    "local_hospital" to Icons.Filled.LocalHospital,
    "school" to Icons.Filled.School,
    "phone" to Icons.Filled.Phone,
    "checkroom" to Icons.Filled.Checkroom,
    "category" to Icons.Filled.Category,
    "people" to Icons.Filled.People,
    "pets" to Icons.Filled.Pets,
    "more_horiz" to Icons.Filled.MoreHoriz,
    "account_balance_wallet" to Icons.Filled.AccountBalanceWallet,
    "attach_money" to Icons.Filled.AttachMoney,
    "emoji_events" to Icons.Filled.EmojiEvents,
    "work" to Icons.Filled.Work,
    "card_giftcard" to Icons.Filled.CardGiftcard,
    "trending_up" to Icons.Filled.TrendingUp,
)

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color.Gray
    }
}

@Composable
fun CategoryIcon(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val color = parseHexColor(category.color)
    val icon = iconMap[category.icon] ?: Icons.Filled.Category

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.name,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}
