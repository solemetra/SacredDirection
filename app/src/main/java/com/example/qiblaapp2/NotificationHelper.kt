package com.example.qiblaapp2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationHelper {
    /** v2: new id so devices drop the old silent channel from testing. */
    const val PRAYER_CHANNEL_ID = "prayer_reminders_v2"
    const val SERVICE_CHANNEL_ID = "service_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val prayerChannel = NotificationChannel(
                PRAYER_CHANNEL_ID,
                "Prayer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders 10 min before prayer time"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                setShowBadge(true)
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
