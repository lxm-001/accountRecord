package com.mian.accountrecord.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mian.accountrecord.domain.model.AlertType
import com.mian.accountrecord.domain.model.BudgetAlert

object NotificationHelper {

    private const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "预算提醒"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "预算使用情况提醒"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showBudgetAlert(context: Context, alert: BudgetAlert) {
        val message = when (alert.type) {
            AlertType.WARNING -> "${alert.categoryName} 预算已使用 80%"
            AlertType.OVERSPENT -> "${alert.categoryName} 预算已超支"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("预算提醒")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        val notificationId = (alert.categoryId ?: 0L).toInt() + alert.type.ordinal * 10000
        manager.notify(notificationId, notification)
    }
}
