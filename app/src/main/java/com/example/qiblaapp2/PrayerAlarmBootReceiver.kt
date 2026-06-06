package com.example.qiblaapp2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        PrayerAlarmScheduler.rescheduleAll(context.applicationContext)
        PrayerTimesWidgetProvider.updateAllWidgets(context.applicationContext)
    }
}
