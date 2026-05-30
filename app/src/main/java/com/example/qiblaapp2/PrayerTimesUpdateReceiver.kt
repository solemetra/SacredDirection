package com.example.qiblaapp2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import android.os.Build
import timezonemap.TimezoneMapper

class PrayerTimesUpdateReceiver : BroadcastReceiver() {
    private val prefsName = "prayer_settings"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        

        
        // Получаем настройки
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val latitude = prefs.getFloat("latitude", 54.3120f).toDouble()
        val longitude = prefs.getFloat("longitude", 59.3847f).toDouble()
        
        // Определяем временную зону
        val timezoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude)
        val tz = java.util.TimeZone.getTimeZone(timezoneId)
        val now = System.currentTimeMillis()
        val timezone = tz.getOffset(now).toDouble() / 3600000.0
        
        // Получаем настройки методов
        val asrStr = prefs.getString("asr_method", "shafii")
        val asrMethod = if (asrStr == "hanafi") AsrMethod.HANAFI else AsrMethod.SHAFII
        val highLatIdx = prefs.getString("high_latitude_method", "0")?.toIntOrNull() ?: 0
        val elevation = prefs.getFloat("elevation", 0.0f).toDouble()
        val fajrFixed = prefs.getBoolean("fajr_fixed", false)
        val ishaFixed = prefs.getBoolean("isha_fixed", false)
        val highLatitudeMethod = HighLatitudeMethod.entries.getOrElse(highLatIdx) { HighLatitudeMethod.AUTO }
        
        // Загружаем состояния будильников
        val alarmStates = loadAlarmStates(prefs)
        
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
        
        // Вычисляем времена на завтра (так как сегодняшние уже прошли)
        val tomorrow = SimpleDate.now().plusDays(1)
        val times = calculator.calculate(tomorrow)
        
        // Пересоздаем будильники
        val timesList = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)
        scheduleSoundAlarms(context, timesList, alarmStates)
    }
    
    private fun loadAlarmStates(prefs: android.content.SharedPreferences): BooleanArray {
        val alarmStates = BooleanArray(5) { true } // По умолчанию все включены
        val stateString = prefs.getString("alarm_states", null)
        if (stateString != null && stateString.length == alarmStates.size) {
            for (i in alarmStates.indices) {
                alarmStates[i] = stateString[i] == '1'
            }
        }
        return alarmStates
    }
    
    private fun scheduleSoundAlarms(context: Context, times: List<String>, alarmStates: BooleanArray) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        
        // Сначала отменяем все старые будильники
        cancelAllSoundAlarms(context, alarmManager)
        
        times.forEachIndexed { index, timeString ->
            if (alarmStates[index]) {
                val isFajr = index == 0
                scheduleSoundAlarm(context, alarmManager, prayerNames[index], timeString, isFajr)
            }
        }
    }
    
    private fun scheduleSoundAlarm(context: Context, alarmManager: AlarmManager, prayerName: String, timeString: String, isFajr: Boolean) {
        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull()
            val minute = parts[1].toIntOrNull()
            if (hour == null || minute == null) return
            
            val calendar = Calendar.getInstance().apply {
                // Ставим на завтра
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // За 10 минут до намаза
                add(Calendar.MINUTE, -10)
            }
            
            val intent = Intent(context, SoundAlarmReceiver::class.java).apply {
                putExtra("isFajr", isFajr)
                putExtra("prayerName", prayerName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Используем точный будильник для Android 6.0+
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ - проверяем разрешение на точные будильники
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        // Если нет разрешения, используем обычный будильник
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Если нет разрешения, используем обычный будильник
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
        } catch (e: Exception) {
            // Ошибка при создании будильника
        }
    }
    
    private fun cancelAllSoundAlarms(context: Context, alarmManager: AlarmManager) {
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        prayerNames.forEach { prayerName ->
            val intent = Intent(context, SoundAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
} 