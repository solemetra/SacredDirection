package com.example.qiblaapp2

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

interface NotificationPermissionRequester {
    fun requestPostNotificationsPermission()
}

/** Host screen for the reminder permission sheet (keeps state if sheet is recreated). */
interface ReminderPermissionHost : NotificationPermissionRequester {
    var notificationRowTapped: Boolean
}

object PrayerReminderPermissions {
    const val REQUEST_NOTIFICATION = 501
    const val PERMISSION_ITEM_COUNT = 4

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openLocation(activity: AppCompatActivity) {
        activity.startActivity(
            Intent(activity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        )
    }

    /** UI / badge / button — on Huawei either runtime perm or EMUI toggle is enough. */
    fun hasNotificationPermission(context: Context): Boolean {
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val postGranted = hasPostNotificationsGranted(context)
            return if (isHuaweiDevice()) {
                postGranted || notificationsEnabled
            } else {
                postGranted
            }
        }
        return notificationsEnabled
    }

    /** Strict check for scheduling and "all ready". */
    fun notificationsFullyReady(context: Context): Boolean {
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPostNotificationsGranted(context) && notificationsEnabled
        }
        return notificationsEnabled
    }

    private fun hasPostNotificationsGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isExactAlarmsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExactAlarms(context)
        } else {
            false
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun allReady(context: Context): Boolean {
        return notificationsFullyReady(context) &&
            canScheduleExactAlarms(context) &&
            isIgnoringBatteryOptimizations(context)
    }

    fun grantedPermissionCount(context: Context): Int {
        var count = 0
        if (hasLocationPermission(context)) count++
        if (notificationsFullyReady(context)) count++
        if (isExactAlarmsItemDone(context)) count++
        if (isIgnoringBatteryOptimizations(context)) count++
        return count
    }

    private fun isExactAlarmsItemDone(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExactAlarms(context)
        } else {
            true
        }
    }

    fun showPermissionsSheet(activity: AppCompatActivity) {
        PrayerReminderPermissionsSheet.show(activity)
    }

    /** Auto-prompt on Prayer tab until reminder permissions are ready. */
    fun showSetupIfNeeded(activity: AppCompatActivity) {
        if (allReady(activity) || activity.isFinishing) return
        PrayerReminderPermissionsSheet.show(activity)
    }

    fun openNotifications(activity: AppCompatActivity) {
        if (activity is ReminderPermissionHost) {
            activity.notificationRowTapped = true
        }
        if (isHuaweiDevice()) {
            openNotificationSettings(activity)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission(activity)) return
            if (activity is NotificationPermissionRequester) {
                activity.requestPostNotificationsPermission()
            } else {
                openNotificationSettings(activity)
            }
        } else {
            openNotificationSettings(activity)
        }
    }

    fun openExactAlarms(activity: AppCompatActivity) {
        openExactAlarmSettings(activity)
    }

    fun openBattery(activity: AppCompatActivity) {
        openBatterySettings(activity)
    }

    fun isHuaweiDevice(): Boolean {
        return Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
            Build.MANUFACTURER.equals("HONOR", ignoreCase = true)
    }

    fun onNotificationPermissionResult(activity: AppCompatActivity, granted: Boolean) {
        if (activity is ReminderPermissionHost) {
            activity.notificationRowTapped = granted || activity.notificationRowTapped
        }
        PrayerReminderPermissionsSheet.refreshIfShowing(activity)
    }

    fun requestPostNotificationsIfNeeded(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasPostNotificationsGranted(activity)) return
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) return
        if (activity is NotificationPermissionRequester) {
            activity.requestPostNotificationsPermission()
        }
    }

    private fun openExactAlarmSettings(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    private fun openBatterySettings(activity: AppCompatActivity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
            activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openNotificationSettings(activity: AppCompatActivity) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }
        activity.startActivity(intent)
    }
}
