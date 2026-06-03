package com.example.qiblaapp2

import android.content.Context
import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import android.os.Build
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object HijriPrefs {
    private const val PREFS_NAME = "prayer_settings"
    private const val KEY_DAY_OFFSET = "hijri_day_offset"
    private const val KEY_SHOW_HIJRI = "show_hijri_date"

    private val hijriMonthsEnglish = arrayOf(
        "Muharram",
        "Safar",
        "Rabi' al-awwal",
        "Rabi' al-thani",
        "Jumada al-awwal",
        "Jumada al-thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    fun getDayOffset(context: Context): Int {
        val offset = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DAY_OFFSET, 0)
        return offset.coerceIn(-1, 1)
    }

    fun setDayOffset(context: Context, offset: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_DAY_OFFSET, offset.coerceIn(-1, 1))
        }
    }

    fun isShowingHijri(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_HIJRI, false)
    }

    fun setShowingHijri(context: Context, showHijri: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SHOW_HIJRI, showHijri)
        }
    }

    fun toggleShowingHijri(context: Context): Boolean {
        val next = !isShowingHijri(context)
        setShowingHijri(context, next)
        return next
    }

    fun formatGregorian(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 12, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(calendar.time)
    }

    fun formatHijriUmmAlQura(context: Context, year: Int, month: Int, day: Int): String {
        val gregorian = Calendar.getInstance()
        gregorian.set(year, month - 1, day, 12, 0, 0)
        gregorian.set(Calendar.MILLISECOND, 0)

        val hijri = createUmmAlQuraCalendar()
        hijri.timeInMillis = gregorian.timeInMillis
        val offset = getDayOffset(context)
        if (offset != 0) {
            hijri.add(IslamicCalendar.DATE, offset)
        }

        val hijriDay = hijri.get(IslamicCalendar.DAY_OF_MONTH)
        val hijriMonth = hijri.get(IslamicCalendar.MONTH).coerceIn(0, hijriMonthsEnglish.lastIndex)
        return "$hijriDay ${hijriMonthsEnglish[hijriMonth]}"
    }

    fun formatDateForPrayerScreen(context: Context, year: Int, month: Int, day: Int): String {
        return if (isShowingHijri(context)) {
            formatHijriUmmAlQura(context, year, month, day)
        } else {
            formatGregorian(year, month, day)
        }
    }

    fun offsetLabel(context: Context): String {
        return when (getDayOffset(context)) {
            -1 -> "-1"
            1 -> "+1"
            else -> "0"
        }
    }

    private fun createUmmAlQuraCalendar(): IslamicCalendar {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IslamicCalendar(ULocale("@calendar=islamic-umalqura"))
        } else {
            IslamicCalendar()
        }
    }
}
