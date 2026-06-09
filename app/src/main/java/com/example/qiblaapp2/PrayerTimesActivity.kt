package com.example.qiblaapp2

import android.content.Context

import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle

import android.widget.TextView

import android.widget.LinearLayout

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat

import androidx.core.content.edit

import android.widget.ImageView

import android.view.View

import android.os.Build

import java.util.TimeZone

import android.location.Geocoder

import java.util.Locale

import timezonemap.TimezoneMapper

import com.example.qiblaapp2.AsrMethod.*

import com.example.qiblaapp2.HighLatitudeMethod.*



class PrayerTimesActivity : AppCompatActivity(), ReminderPermissionHost {

    private val prefsName = "prayer_settings"

    override var notificationRowTapped: Boolean = false

    private val postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        PrayerReminderPermissions.onNotificationPermissionResult(this, granted)
    }

    private val alarmStates = BooleanArray(5) { true } // Fajr, Dhuhr, Asr, Maghrib, Isha

    override fun requestPostNotificationsPermission() {
        postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prayer_times)

        TabUiHelper.applyBottomNavInsets(this)

        clearOldIntPrefs()

        loadAlarmStates()

        updatePrayerTimesDisplay()

        TabUiHelper.highlightBottomTab(this, TabUiHelper.BottomTab.PRAYER)

        setupNavigation()

        setupAlarmButtons()

        updateCityName()

        setupDateToggle()

    }



    override fun onResume() {

        super.onResume()

        updateDateDisplay(SimpleDate.now())

        if (!PrayerReminderPermissions.allReady(this)) {
            PrayerReminderPermissions.showSetupIfNeeded(this)
        }

        PrayerReminderPermissionsSheet.refreshIfShowing(this)

        PrayerAlarmScheduler.rescheduleAll(this)

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
        }

    }



    private fun clearOldIntPrefs() {

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        prefs.edit {

            if (prefs.all["calc_method"] is Int) remove("calc_method")

            if (prefs.all["asr_method"] is Int) remove("asr_method")

            if (prefs.all["high_latitude_method"] is Int) remove("high_latitude_method")

        }

    }



    private fun updatePrayerTimesDisplay() {

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val latitude = prefs.getFloat("latitude", 54.3120f).toDouble()

        val longitude = prefs.getFloat("longitude", 59.3847f).toDouble()

        val timezoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude)

        val tz = TimeZone.getTimeZone(timezoneId)

        val now = System.currentTimeMillis()

        val timezone = tz.getOffset(now) / 3600000.0



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



        PrayerAlarmScheduler.rescheduleAll(this)

        updateDateDisplay(today)



        findViewById<TextView>(R.id.fajrTime)?.apply {

            text = times.fajr

            setTextColor(android.graphics.Color.BLACK)

        }

        findViewById<TextView>(R.id.dhuhrTime)?.apply {

            text = times.dhuhr

            setTextColor(android.graphics.Color.BLACK)

        }

        findViewById<TextView>(R.id.asrTime)?.apply {

            text = times.asr

            setTextColor(android.graphics.Color.BLACK)

        }

        findViewById<TextView>(R.id.maghribTime)?.apply {

            text = times.maghrib

            setTextColor(android.graphics.Color.BLACK)

        }

        findViewById<TextView>(R.id.ishaTime)?.apply {

            text = times.isha

            setTextColor(android.graphics.Color.BLACK)

        }

        findViewById<TextView>(R.id.textSunriseTime)?.text = times.sunrise

        findViewById<TextView>(R.id.textSunsetTime)?.text = times.maghrib

        PrayerTimesWidgetProvider.updateAllWidgets(this)

    }



    private fun updateDateDisplay(today: SimpleDate) {

        findViewById<TextView>(R.id.textGregorianDate)?.text =

            HijriPrefs.formatDateForPrayerScreen(this, today.year, today.month, today.day)

    }



    private fun setupDateToggle() {

        findViewById<TextView>(R.id.textGregorianDate)?.setOnClickListener {

            HijriPrefs.toggleShowingHijri(this)

            updateDateDisplay(SimpleDate.now())

        }

    }



    private fun setupNavigation() {

        findViewById<LinearLayout>(R.id.btnDirection)?.setOnClickListener {

            startActivity(Intent(this, MainActivity::class.java))

            finish()

        }

        findViewById<LinearLayout>(R.id.btnPrayerTimes)?.setOnClickListener {

            // Already on this screen

        }

        findViewById<LinearLayout>(R.id.btnDua)?.setOnClickListener {

            startActivity(Intent(this, DuaActivity::class.java))

            finish()

        }

        findViewById<LinearLayout>(R.id.btnSettings)?.setOnClickListener {

            startActivity(Intent(this, SettingsActivity::class.java))

            finish()

        }

    }



    private fun setupAlarmButtons() {

        setupAlarmButton(0, findViewById(R.id.alarmButtonFajr), findViewById(R.id.alarmTextFajr))

        setupAlarmButton(1, findViewById(R.id.alarmButtonDhuhr), findViewById(R.id.alarmTextDhuhr))

        setupAlarmButton(2, findViewById(R.id.alarmButtonAsr), findViewById(R.id.alarmTextAsr))

        setupAlarmButton(3, findViewById(R.id.alarmButtonMaghrib), findViewById(R.id.alarmTextMaghrib))

        setupAlarmButton(4, findViewById(R.id.alarmButtonIsha), findViewById(R.id.alarmTextIsha))

    }



    private fun setupAlarmButton(index: Int, buttonView: View, textView: TextView) {

        updateAlarmButtonUI(index, buttonView, textView)

        val toggle = {

            alarmStates[index] = !alarmStates[index]

            updateAlarmButtonUI(index, buttonView, textView)

            saveAlarmStates()

            PrayerAlarmScheduler.rescheduleAll(this)

        }

        buttonView.setOnClickListener { toggle() }

        textView.setOnClickListener { toggle() }

    }



    private fun updateAlarmButtonUI(index: Int, buttonView: View, textView: TextView) {

        if (alarmStates[index]) {

            buttonView.setBackgroundResource(R.drawable.alarm_button_background)

            textView.text = getString(R.string.alarm_on)

            textView.setTextColor(ContextCompat.getColor(this, R.color.alarm_label_on))

        } else {

            buttonView.setBackgroundResource(R.drawable.alarm_button_off_background)

            textView.text = getString(R.string.alarm_off)

            textView.setTextColor(ContextCompat.getColor(this, R.color.alarm_label_off))

        }

    }



    private fun saveAlarmStates() {

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val stateString = alarmStates.joinToString(separator = "") { if (it) "1" else "0" }

        prefs.edit { putString("alarm_states", stateString) }

    }



    private fun loadAlarmStates() {

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val stateString = prefs.getString("alarm_states", null)

        if (stateString != null && stateString.length == alarmStates.size) {

            for (i in alarmStates.indices) {

                alarmStates[i] = stateString[i] == '1'

            }

        }

    }



    @Suppress("DEPRECATION")

    private fun updateCityName() {

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val latitude = prefs.getFloat("latitude", 54.3120f).toDouble()

        val longitude = prefs.getFloat("longitude", 59.3847f).toDouble()

        val textCity = findViewById<TextView>(R.id.textCityPrayerTimes)

        val geocoder = Geocoder(this, Locale.ENGLISH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->

                val city = if (addresses.isNotEmpty()) {

                    addresses[0].locality ?: addresses[0].subAdminArea ?: "-"

                } else {

                    "-"

                }

                runOnUiThread { textCity.text = city }

            }

        } else {

            Thread {

                try {

                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                    val city = if (addresses?.isNotEmpty() == true) {

                        addresses[0].locality ?: addresses[0].subAdminArea ?: "-"

                    } else {

                        "-"

                    }

                    runOnUiThread { textCity.text = city }

                } catch (e: Exception) {

                    runOnUiThread { textCity.text = getString(R.string.unknown_location) }

                }

            }.start()

        }

    }

}


