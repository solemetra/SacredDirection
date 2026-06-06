package com.example.qiblaapp2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SoundAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val prayerName = intent.getStringExtra("prayerName") ?: "Prayer"
        val isFajr = intent.getBooleanExtra("isFajr", false)
        
        showNotification(context, prayerName)
        playShortSystemSound(context)
        tryPlayCustomAzan(context, isFajr)
        PrayerAlarmScheduler.rescheduleAll(context.applicationContext)
    }
    
    private fun showNotification(context: Context, prayerName: String) {
        NotificationHelper.createNotificationChannels(context)

        val notification = NotificationCompat.Builder(context, NotificationHelper.PRAYER_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.reminder_notification_title, prayerName))
            .setContentText(context.getString(R.string.reminder_notification_body, prayerName))
            .setSmallIcon(R.drawable.ic_compass)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        NotificationManagerCompat.from(context).notify(prayerName.hashCode(), notification)
    }
    
    private fun playShortSystemSound(context: Context) {
        try {
            // Получаем короткий системный звук уведомления
            val soundUri = getShortNotificationSound(context)
            val ringtone = RingtoneManager.getRingtone(context, soundUri)
            
            // Воспроизводим короткий звук 3 раза с паузами для привлечения внимания
            repeat(3) { i ->
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        ringtone?.play()
                    } catch (e: Exception) {
                        // Игнорируем ошибки - это запасной звук
                    }
                }, i * 800L) // Каждые 800мс
            }
        } catch (e: Exception) {
            // Если системный звук не сработал - не критично
        }
    }
    
    private fun getShortNotificationSound(context: Context): android.net.Uri {
        return try {
            // Пытаемся найти короткий звук среди системных
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
            
            val cursor = ringtoneManager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                
                // Ищем короткие звуки (не мелодии)
                val shortSoundKeywords = listOf("beep", "ding", "chime", "ping", "pop", "click", "tone")
                
                if (shortSoundKeywords.any { title.lowercase().contains(it) }) {
                    return ringtoneManager.getRingtoneUri(cursor.position)
                }
            }
            
            // Если не найден подходящий - используем дефолтный
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } catch (e: Exception) {
            // В крайнем случае - дефолтный звук уведомления
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
    
    private fun tryPlayCustomAzan(context: Context, isFajr: Boolean) {
        try {
            // Пытаемся воспроизвести кастомный азан
            val audioRes = if (isFajr) R.raw.adhan_fajr else R.raw.adhan_prayer
            val player = MediaPlayer.create(context, audioRes)
            
            if (player != null) {
                player.start()
                player.setOnCompletionListener { it.release() }
            }
        } catch (e: Exception) {
            // Если кастомный звук не сработал - не критично, 
            // системный звук уже проиграл
        }
    }
}