package com.example.qiblaapp2

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class WidgetUpdateService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            // Обновляем все виджеты
            PrayerTimesWidgetProvider.updateAllWidgets(this@WidgetUpdateService)
            
            // Планируем следующее обновление через 15 минут
            handler.postDelayed(this, 15 * 60 * 1000L)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Запускаем периодические обновления
        handler.post(updateRunnable)
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Останавливаем обновления
        handler.removeCallbacks(updateRunnable)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
} 