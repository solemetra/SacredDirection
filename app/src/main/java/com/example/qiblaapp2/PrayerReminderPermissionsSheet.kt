package com.example.qiblaapp2

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class PrayerReminderPermissionsSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.Theme_SacredDirection_BottomSheet

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setCanceledOnTouchOutside(false)
            setOnShowListener {
                val sheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                sheet?.let { bottomSheet ->
                    bottomSheet.setBackgroundColor(Color.TRANSPARENT)
                    BottomSheetBehavior.from(bottomSheet).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        skipCollapsed = true
                        isFitToContents = true
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_reminder_permissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = 20 + bottomInset)
            insets
        }

        view.findViewById<View>(R.id.rowSheetLocation).setOnClickListener {
            (activity as? AppCompatActivity)?.let { act ->
                if (!PrayerReminderPermissions.hasLocationPermission(act)) {
                    PrayerReminderPermissions.openLocation(act)
                }
            }
        }
        view.findViewById<View>(R.id.rowSheetNotifications).setOnClickListener {
            (activity as? AppCompatActivity)?.let { act ->
                PrayerReminderPermissions.openNotifications(act)
            }
        }
        view.findViewById<View>(R.id.rowSheetExactAlarms).setOnClickListener {
            (activity as? AppCompatActivity)?.let { PrayerReminderPermissions.openExactAlarms(it) }
        }
        view.findViewById<View>(R.id.rowSheetBattery).setOnClickListener {
            (activity as? AppCompatActivity)?.let { PrayerReminderPermissions.openBattery(it) }
        }
        view.findViewById<MaterialButton>(R.id.btnSheetAllSet).setOnClickListener { dismiss() }

        syncAfterPermissionChange()
    }

    override fun onResume() {
        super.onResume()
        syncAfterPermissionChange()
    }

    fun syncAfterPermissionChange() {
        val activity = activity as? AppCompatActivity ?: return
        PrayerReminderPermissions.requestPostNotificationsIfNeeded(activity)
        refreshUi()
    }

    fun refreshUi() {
        val view = view ?: return
        val context = requireContext()

        val hasLocation = PrayerReminderPermissions.hasLocationPermission(context)
        val notificationsDone = PrayerReminderPermissions.notificationsFullyReady(context)
        val hasExact = PrayerReminderPermissions.canScheduleExactAlarms(context)
        val hasBattery = PrayerReminderPermissions.isIgnoringBatteryOptimizations(context)
        val allReady = PrayerReminderPermissions.allReady(context)

        updateStatus(view.findViewById(R.id.statusSheetLocation), hasLocation)
        view.findViewById<View>(R.id.rowSheetLocation).apply {
            isClickable = !hasLocation
            isFocusable = !hasLocation
        }
        updateStatus(view.findViewById(R.id.statusSheetNotifications), notificationsDone)
        updateStatus(
            view.findViewById(R.id.statusSheetExactAlarms),
            PrayerReminderPermissions.isExactAlarmsGranted(context)
        )
        updateStatus(view.findViewById(R.id.statusSheetBattery), hasBattery)

        val nextStep = view.findViewById<TextView>(R.id.textSheetNextStep)
        if (allReady) {
            nextStep.visibility = View.GONE
        } else {
            nextStep.visibility = View.VISIBLE
            nextStep.text = when {
                !hasLocation -> getString(R.string.reminder_perm_next_location)
                !notificationsDone -> getString(R.string.reminder_perm_next_notifications)
                !hasExact -> getString(R.string.reminder_perm_next_exact)
                else -> getString(R.string.reminder_perm_next_battery)
            }
        }

        val primaryBtn = view.findViewById<MaterialButton>(R.id.btnSheetAllSet)
        primaryBtn.visibility = View.VISIBLE
        primaryBtn.text = getString(
            if (allReady) R.string.reminder_perm_all_ready else R.string.reminder_perm_later
        )
        primaryBtn.setTextColor(ContextCompat.getColor(context, R.color.white_text))
    }

    private fun updateStatus(statusView: TextView, granted: Boolean) {
        val context = requireContext()
        if (granted) {
            statusView.setText(R.string.reminder_perm_status_done)
            statusView.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
            statusView.setBackgroundResource(R.drawable.perm_status_done_bg)
        } else {
            statusView.setText(R.string.reminder_perm_status_allow)
            statusView.setTextColor(ContextCompat.getColor(context, R.color.blue_primary_variant))
            statusView.setBackgroundResource(R.drawable.perm_status_allow_bg)
        }
    }

    companion object {
        const val TAG = "PrayerReminderPermissionsSheet"

        fun show(activity: AppCompatActivity) {
            val existing = activity.supportFragmentManager.findFragmentByTag(TAG)
            if (existing != null) {
                (existing as? PrayerReminderPermissionsSheet)?.syncAfterPermissionChange()
                return
            }
            PrayerReminderPermissionsSheet().show(activity.supportFragmentManager, TAG)
        }

        fun refreshIfShowing(activity: AppCompatActivity) {
            (activity.supportFragmentManager.findFragmentByTag(TAG) as? PrayerReminderPermissionsSheet)
                ?.syncAfterPermissionChange()
        }
    }
}
