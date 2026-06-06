package com.example.qiblaapp2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import timezonemap.TimezoneMapper
import java.util.Calendar
import java.util.TimeZone

object PrayerAlarmScheduler {
    private const val TAG = "PrayerAlarmScheduler"
    private const val PREFS_NAME = "prayer_settings"
    private const val MINUTES_BEFORE = 10

    private val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun rescheduleAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmStates = loadAlarmStates(prefs)
        val calculator = buildCalculator(context, prefs) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(context, alarmManager)

        val today = SimpleDate.now()
        val todayTimes = calculator.calculate(today).toTimeList()
        val tomorrowTimes = calculator.calculate(today.plusDays(1)).toTimeList()
        val now = Calendar.getInstance()

        PRAYER_NAMES.indices.forEach { index ->
            if (!alarmStates[index]) return@forEach
            val triggerAt = nextTriggerMillis(
                todayTimes[index],
                tomorrowTimes[index],
                today,
                now
            ) ?: return@forEach
            scheduleAlarm(
                context,
                alarmManager,
                PRAYER_NAMES[index],
                index == 0,
                triggerAt
            )
        }
    }

    private fun nextTriggerMillis(
        todayTime: String,
        tomorrowTime: String,
        today: SimpleDate,
        now: Calendar
    ): Long? {
        val todayTrigger = triggerCalendar(today, todayTime)?.timeInMillis
        if (todayTrigger != null && todayTrigger > now.timeInMillis) {
            return todayTrigger
        }
        return triggerCalendar(today.plusDays(1), tomorrowTime)?.timeInMillis
    }

    private fun triggerCalendar(date: SimpleDate, timeString: String): Calendar? {
        val parts = timeString.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            set(date.year, date.month - 1, date.day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -MINUTES_BEFORE)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        prayerName: String,
        isFajr: Boolean,
        triggerAtMillis: Long
    ) {
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied for $prayerName, using inexact", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule $prayerName", e)
        }
    }

    private fun cancelAll(context: Context, alarmManager: AlarmManager) {
        PRAYER_NAMES.forEach { prayerName ->
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

    private fun loadAlarmStates(prefs: android.content.SharedPreferences): BooleanArray {
        val alarmStates = BooleanArray(5) { true }
        val stateString = prefs.getString("alarm_states", null)
        if (stateString != null && stateString.length == alarmStates.size) {
            for (i in alarmStates.indices) {
                alarmStates[i] = stateString[i] == '1'
            }
        }
        return alarmStates
    }

    private fun buildCalculator(
        context: Context,
        prefs: android.content.SharedPreferences
    ): PrayerTimesCalculator? {
        return try {
            val latitude = prefs.getFloat("latitude", 54.3120f).toDouble()
            val longitude = prefs.getFloat("longitude", 59.3847f).toDouble()
            val timezoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude)
            val tz = TimeZone.getTimeZone(timezoneId)
            val timezone = tz.getOffset(System.currentTimeMillis()) / 3600000.0

            val asrStr = prefs.getString("asr_method", "shafii")
            val asrMethod = if (asrStr == "hanafi") AsrMethod.HANAFI else AsrMethod.SHAFII
            val highLatIdx = prefs.getString("high_latitude_method", "0")?.toIntOrNull() ?: 0
            val elevation = prefs.getFloat("elevation", 0.0f).toDouble()
            val fajrFixed = prefs.getBoolean("fajr_fixed", false)
            val ishaFixed = prefs.getBoolean("isha_fixed", false)
            val highLatitudeMethod = HighLatitudeMethod.entries.getOrElse(highLatIdx) {
                HighLatitudeMethod.AUTO
            }

            PrayerTimesCalculator(
                latitude = latitude,
                longitude = longitude,
                timezone,
                asrMethodParam = asrMethod,
                highLatitudeMethodParam = highLatitudeMethod,
                fajrFixedParam = fajrFixed,
                ishaFixedParam = ishaFixed,
                elevation = elevation
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build prayer calculator", e)
            null
        }
    }

    private fun PrayerTimes.toTimeList(): List<String> =
        listOf(fajr, dhuhr, asr, maghrib, isha)
}
