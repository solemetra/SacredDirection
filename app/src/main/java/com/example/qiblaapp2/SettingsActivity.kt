package com.example.qiblaapp2

import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity(), ReminderPermissionHost {

    override var notificationRowTapped: Boolean = false

    private val postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        PrayerReminderPermissions.onNotificationPermissionResult(this, granted)
        refreshReminderPermissionsUi()
        PrayerReminderPermissionsSheet.refreshIfShowing(this)
    }

    override fun requestPostNotificationsPermission() {
        postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun clearOldIntPrefs() {
        val prefs = getSharedPreferences("prayer_settings", MODE_PRIVATE)
        prefs.edit {
            if (prefs.all["calc_method"] is Int) remove("calc_method")
            if (prefs.all["asr_method"] is Int) remove("asr_method")
            if (prefs.all["high_latitude_method"] is Int) remove("high_latitude_method")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearOldIntPrefs()
        setContentView(R.layout.activity_settings)
        TabUiHelper.applyBottomNavInsets(this)
        highlightActiveTab()
        setupNavigation()
        setupAboutCard()
        setupHijri()
        setupReminderPermissionsCard()

        val prefs = getSharedPreferences("prayer_settings", MODE_PRIVATE)

        val radioGroupAsr = findViewById<RadioGroup>(R.id.radioGroupAsr)
        val asrMethod = prefs.getString("asr_method", "shafii")
        radioGroupAsr.check(if (asrMethod == "hanafi") R.id.radioAsrHanafi else R.id.radioAsrShafii)
        radioGroupAsr.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit { putString("asr_method", if (checkedId == R.id.radioAsrHanafi) "hanafi" else "shafii") }
            sendPrayerTimesUpdateBroadcast()
        }

        val switchFajrFixed = findViewById<SwitchCompat>(R.id.switchFajrFixed)
        switchFajrFixed.isChecked = prefs.getBoolean("fajr_fixed", false)
        switchFajrFixed.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("fajr_fixed", isChecked) }
            sendPrayerTimesUpdateBroadcast()
        }

        val switchIshaFixed = findViewById<SwitchCompat>(R.id.switchIshaFixed)
        switchIshaFixed.isChecked = prefs.getBoolean("isha_fixed", false)
        switchIshaFixed.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("isha_fixed", isChecked) }
            sendPrayerTimesUpdateBroadcast()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshReminderPermissionsUi()
        PrayerReminderPermissions.requestPostNotificationsIfNeeded(this)
        PrayerReminderPermissionsSheet.refreshIfShowing(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PrayerReminderPermissions.REQUEST_NOTIFICATION) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            PrayerReminderPermissions.onNotificationPermissionResult(this, granted)
            refreshReminderPermissionsUi()
        }
    }

    private fun setupReminderPermissionsCard() {
        findViewById<View>(R.id.rowReminderPermissions).setOnClickListener {
            PrayerReminderPermissions.showPermissionsSheet(this)
        }
        refreshReminderPermissionsUi()
    }

    private fun refreshReminderPermissionsUi() {
        val granted = PrayerReminderPermissions.grantedPermissionCount(this)
        val total = PrayerReminderPermissions.PERMISSION_ITEM_COUNT
        findViewById<TextView>(R.id.textReminderPermSummary).apply {
            text = if (granted >= total) {
                getString(R.string.reminder_perm_summary_done)
            } else {
                getString(R.string.reminder_perm_summary_count, granted, total)
            }
            setTextColor(
                ContextCompat.getColor(
                    this@SettingsActivity,
                    if (granted >= total) R.color.green_primary else R.color.gray_text
                )
            )
        }
    }

    private fun setupHijri() {
        val toggle = findViewById<MaterialButtonToggleGroup>(R.id.toggleHijriOffset)
        var updatingSelection = false

        fun selectOffset(offset: Int) {
            updatingSelection = true
            val buttonId = when (offset.coerceIn(-1, 1)) {
                -1 -> R.id.btnHijriMinus
                1 -> R.id.btnHijriPlus
                else -> R.id.btnHijriZero
            }
            toggle.check(buttonId)
            updatingSelection = false
        }

        selectOffset(HijriPrefs.getDayOffset(this))

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (updatingSelection || !isChecked) return@addOnButtonCheckedListener
            val offset = when (checkedId) {
                R.id.btnHijriMinus -> -1
                R.id.btnHijriPlus -> 1
                else -> 0
            }
            HijriPrefs.setDayOffset(this, offset)
        }
    }

    private fun setupAboutCard() {
        findViewById<TextView>(R.id.textSettingsVersion).text =
            getString(R.string.version, BuildConfig.VERSION_NAME)

        findViewById<TextView>(R.id.btnPrivacyPolicy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
    }

    private fun highlightActiveTab() {
        val btnDirection = findViewById<LinearLayout>(R.id.btnDirection)
        val btnPrayerTimes = findViewById<LinearLayout>(R.id.btnPrayerTimes)
        val btnDua = findViewById<LinearLayout>(R.id.btnDua)
        val btnSettings = findViewById<LinearLayout>(R.id.btnSettings)

        btnSettings.findViewById<ImageView>(R.id.iconSettings)?.setColorFilter(
            ContextCompat.getColor(this, R.color.blue_primary)
        )
        btnDirection.findViewById<ImageView>(R.id.iconDirection)?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_text)
        )
        btnPrayerTimes.findViewById<ImageView>(R.id.iconPrayer)?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_text)
        )
        btnDua.findViewById<ImageView>(R.id.iconDua)?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_text)
        )
    }

    private fun setupNavigation() {
        val btnDirection = findViewById<LinearLayout>(R.id.btnDirection)
        val btnPrayerTimes = findViewById<LinearLayout>(R.id.btnPrayerTimes)
        val btnDua = findViewById<LinearLayout>(R.id.btnDua)

        btnDirection.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        btnPrayerTimes.setOnClickListener {
            startActivity(Intent(this, PrayerTimesActivity::class.java))
            finish()
        }
        btnDua.setOnClickListener {
            startActivity(Intent(this, DuaActivity::class.java))
            finish()
        }
    }

    private fun sendPrayerTimesUpdateBroadcast() {
        val intent = Intent("com.example.qiblaapp2.UPDATE_PRAYER_TIMES")
        sendBroadcast(intent)
        PrayerTimesWidgetProvider.updateAllWidgets(this)
    }
}
