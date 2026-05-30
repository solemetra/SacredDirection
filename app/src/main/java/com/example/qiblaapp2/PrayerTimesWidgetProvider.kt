package com.example.qiblaapp2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.TimeZone
import com.example.qiblaapp2.AsrMethod.*
import com.example.qiblaapp2.HighLatitudeMethod.*
import timezonemap.TimezoneMapper

class PrayerTimesWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (isBootAction(intent.action)) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PrayerTimesWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
                context.startService(Intent(context, WidgetUpdateService::class.java))
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Первый виджет создан - запускаем сервис обновлений
        super.onEnabled(context)
        val intent = Intent(context, WidgetUpdateService::class.java)
        context.startService(intent)
    }

    override fun onDisabled(context: Context) {
        // Последний виджет удален - останавливаем сервис
        super.onDisabled(context)
        val intent = Intent(context, WidgetUpdateService::class.java)
        context.stopService(intent)
    }

    companion object {
        private const val PREFS_NAME = "prayer_settings"
        
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            
            try {
                // Получаем настройки
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val latitude = prefs.getFloat("latitude", 54.3120f).toDouble()
                val longitude = prefs.getFloat("longitude", 59.3847f).toDouble()
                
                // Часовой пояс по координатам — как на экране намазов
                val timezoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude)
                val tz = TimeZone.getTimeZone(timezoneId)
                val now = System.currentTimeMillis()
                val timezone = tz.getOffset(now) / 3600000.0

                // Получаем настройки методов
                val asrStr = prefs.getString("asr_method", "shafii")
                val asrMethod = if (asrStr == "hanafi") HANAFI else SHAFII
                val highLatIdx = prefs.getString("high_latitude_method", "0")?.toIntOrNull() ?: 0
                val elevation = prefs.getFloat("elevation", 0.0f).toDouble()
                val fajrFixed = prefs.getBoolean("fajr_fixed", false)
                val ishaFixed = prefs.getBoolean("isha_fixed", false)

                val highLatitudeMethod = if (highLatIdx in HighLatitudeMethod.entries.indices) {
                    HighLatitudeMethod.entries[highLatIdx]
                } else {
                    AUTO
                }

                // Создаем калькулятор
                val calculator = PrayerTimesCalculator(
                    latitude = latitude,
                    longitude = longitude,
                    timezone,
                    asrMethodParam = asrMethod,
                    highLatitudeMethodParam = highLatitudeMethod,
                    fajrFixedParam = fajrFixed,
                    ishaFixedParam = ishaFixed,
                    elevation = elevation
                )
                
                val today = SimpleDate.now()
                val times = calculator.calculate(today)

                // Обновляем времена в виджете
                views.setTextViewText(R.id.widgetFajrTime, times.fajr)
                views.setTextViewText(R.id.widgetDhuhrTime, times.dhuhr)
                views.setTextViewText(R.id.widgetAsrTime, times.asr)
                views.setTextViewText(R.id.widgetMaghribTime, times.maghrib)
                views.setTextViewText(R.id.widgetIshaTime, times.isha)

                // Добавляем клик для открытия приложения
                val intent = Intent(context, PrayerTimesActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

            } catch (e: Exception) {
                // В случае ошибки показываем дефолтные значения
                views.setTextViewText(R.id.widgetFajrTime, "--:--")
                views.setTextViewText(R.id.widgetDhuhrTime, "--:--")
                views.setTextViewText(R.id.widgetAsrTime, "--:--")
                views.setTextViewText(R.id.widgetMaghribTime, "--:--")
                views.setTextViewText(R.id.widgetIshaTime, "--:--")
            }

            // Обновляем виджет
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PrayerTimesWidgetProvider::class.java)
            )
            if (appWidgetIds.isEmpty()) return
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun isBootAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED ||
                action == "android.intent.action.QUICKBOOT_POWERON" ||
                action == "com.htc.intent.action.QUICKBOOT_POWERON"
        }
    }
} 