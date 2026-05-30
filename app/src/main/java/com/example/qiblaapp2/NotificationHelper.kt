package com.example.qiblaapp2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val PRAYER_CHANNEL_ID = "prayer_channel"
    const val SERVICE_CHANNEL_ID = "service_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val prayerChannel = NotificationChannel(
                PRAYER_CHANNEL_ID,
                "Prayer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming prayers"
            }

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps prayer alarms active in background"
                setShowBadge(false)
            }

            manager.createNotificationChannel(prayerChannel)
            manager.createNotificationChannel(serviceChannel)
        }
    }
} 