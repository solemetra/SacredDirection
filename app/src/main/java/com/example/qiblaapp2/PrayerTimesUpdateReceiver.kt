package com.example.qiblaapp2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerTimesUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        PrayerAlarmScheduler.rescheduleAll(context.applicationContext)
    }
}
