package com.example.qiblaapp2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
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
 
import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent.*

class PrayerTimesActivity : AppCompatActivity() {
    private val prefsName = "prayer_settings"
    
    // prayerTimesUpdateReceiver теперь статический класс в отдельном файле

    private val alarmStates = BooleanArray(5) { true } // Fajr, Dhuhr, Asr, Maghrib, Isha
    private var showSunrise = true
    private val alarmManager by lazy { getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prayer_times)
        // PrayerTimesUpdateReceiver теперь зарегистрирован статически в AndroidManifest.xml
        clearOldIntPrefs()
        loadAlarmStates() // Восстановить состояния будильников
        updatePrayerTimesDisplay()
        highlightActiveTab()
        setupNavigation()
        setupAlarmButtons()
        updateCityName()
        setupSunriseSunsetToggle()
    }

    override fun onDestroy() {
        super.onDestroy()
        // PrayerTimesUpdateReceiver теперь статический, не нужно unregister
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
        // Определяем временную зону по координатам через TimezoneMapper
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

        val timesList = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)
         scheduleSoundAlarms(timesList)

        // Установить григорианскую дату
        val dateTextView = findViewById<TextView>(R.id.textGregorianDate)
        val calendar = java.util.Calendar.getInstance()
        calendar.set(today.year, today.month - 1, today.day)
        val format = java.text.SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        dateTextView?.text = format.format(calendar.time)

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
        // Обновляю sunrise/sunset
        findViewById<TextView>(R.id.textSunriseTime)?.text = times.sunrise
        findViewById<TextView>(R.id.textSunsetTime)?.text = times.maghrib
        // Обновляем виджет с новыми временами
        PrayerTimesWidgetProvider.updateAllWidgets(this)
    }

    private fun highlightActiveTab() {
        val btnDirection = findViewById<LinearLayout>(R.id.btnDirection)
        val btnPrayerTimes = findViewById<LinearLayout>(R.id.btnPrayerTimes)
        val btnDua = findViewById<LinearLayout>(R.id.btnDua)
        val btnSettings = findViewById<LinearLayout>(R.id.btnSettings)

        btnPrayerTimes.findViewById<ImageView>(R.id.iconPrayer)?.setColorFilter(
            ContextCompat.getColor(this, R.color.blue_primary)
        )
        btnDirection.findViewById<ImageView>(R.id.iconDirection)?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_text)
        )
        btnDua.findViewById<ImageView>(R.id.iconDua)?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_text)
        )
        btnSettings.findViewById<ImageView>(R.id.iconSettings)?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_text)
        )
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.btnDirection)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.btnPrayerTimes)?.setOnClickListener {
            // Уже на этом экране
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
        setupAlarmButton(
            0,
            findViewById(R.id.alarmButtonFajr),
            findViewById(R.id.alarmTextFajr)
        )
        setupAlarmButton(
            1,
            findViewById(R.id.alarmButtonDhuhr),
            findViewById(R.id.alarmTextDhuhr)
        )
        setupAlarmButton(
            2,
            findViewById(R.id.alarmButtonAsr),
            findViewById(R.id.alarmTextAsr)
        )
        setupAlarmButton(
            3,
            findViewById(R.id.alarmButtonMaghrib),
            findViewById(R.id.alarmTextMaghrib)
        )
        setupAlarmButton(
            4,
            findViewById(R.id.alarmButtonIsha),
            findViewById(R.id.alarmTextIsha)
        )
    }

    private fun setupAlarmButton(index: Int, buttonView: View, textView: TextView) {
        updateAlarmButtonUI(index, buttonView, textView)
        buttonView.setOnClickListener {
            alarmStates[index] = !alarmStates[index]
            updateAlarmButtonUI(index, buttonView, textView)
            saveAlarmStates() // Сохранять при изменении
        }
        textView.setOnClickListener {
            alarmStates[index] = !alarmStates[index]
            updateAlarmButtonUI(index, buttonView, textView)
            saveAlarmStates() // Сохранять при изменении
        }
    }

    private fun updateAlarmButtonUI(index: Int, buttonView: View, textView: TextView) {
        if (alarmStates[index]) {
            buttonView.setBackgroundResource(R.drawable.alarm_button_background)
            textView.text = getString(R.string.alarm_on)
            textView.setTextColor(ContextCompat.getColor(this, R.color.dark_text))
        } else {
            buttonView.setBackgroundResource(R.drawable.alarm_button_off_background)
            textView.text = getString(R.string.alarm_off)
            textView.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
        }
    }

    // --- Сохранение и восстановление состояний будильников ---
    private fun saveAlarmStates() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val stateString = alarmStates.joinToString(separator = "") { if (it) "1" else "0" }
        prefs.edit {
            putString("alarm_states", stateString)
        }
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

    private fun setupSunriseSunsetToggle() {
        val sunriseBlock = findViewById<LinearLayout>(R.id.sunriseBlock)
        val sunsetBlock = findViewById<LinearLayout>(R.id.sunsetBlock)

        fun showSunriseView() {
            sunriseBlock.visibility = View.VISIBLE
            sunsetBlock.visibility = View.GONE
            showSunrise = true
        }

        fun showSunsetView() {
            sunriseBlock.visibility = View.GONE
            sunsetBlock.visibility = View.VISIBLE
            showSunrise = false
        }

        // Начальное состояние — показываем sunrise
        showSunriseView()

        // Переключение по нажатию на весь блок (включая заголовок и иконку)
        // Нажатие на видимый блок показывает противоположный
        sunriseBlock.setOnClickListener { showSunsetView() }
        sunsetBlock.setOnClickListener { showSunriseView() }
    }

    private fun scheduleSoundAlarms(times: List<String>) {
        cancelAllSoundAlarms()
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        times.forEachIndexed { index, timeString ->
            if (alarmStates[index]) {
                val isFajr = index == 0
                scheduleSoundAlarm(prayerNames[index], timeString, isFajr)
            }
        }
    }

    private fun scheduleSoundAlarm(prayerName: String, timeString: String, isFajr: Boolean) {
        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull()
            val minute = parts[1].toIntOrNull()
            if (hour == null || minute == null) return
            
            val now = java.util.Calendar.getInstance()
            
            // Сначала создаем календарь для времени намаза (без вычитания 10 минут)
            val prayerTime = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            // Создаем календарь для будильника (за 10 минут до намаза)
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = prayerTime.timeInMillis
                add(java.util.Calendar.MINUTE, -10)
                
                // Если время намаза еще не прошло, но будильник уже прошел,
                // устанавливаем на завтра
                if (prayerTime.after(now) && before(now)) {
                    // Намаз сегодня, но будильник уже прошел - ставим на завтра
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                } else if (prayerTime.before(now)) {
                    // Намаз уже прошел - ставим на завтра
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            

            val intent = Intent(this, SoundAlarmReceiver::class.java).apply {
                putExtra("isFajr", isFajr)
                putExtra("prayerName", prayerName)
            }
            val pendingIntent = getBroadcast(
                this,
                prayerName.hashCode(),
                intent,
                FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
            )
            // Используем точный будильник для Android 6.0+
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ - проверяем разрешение на точные будильники
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        // Если нет разрешения, используем обычный будильник
                        alarmManager.setAndAllowWhileIdle(
                            RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Если нет разрешения, используем обычный будильник
                alarmManager.set(
                    RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Ошибка при создании будильника
        }
    }

    private fun cancelAllSoundAlarms() {
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        prayerNames.forEach { prayerName ->
            val intent = Intent(this, SoundAlarmReceiver::class.java)
            val pendingIntent = getBroadcast(
                this,
                prayerName.hashCode(),
                intent,
                FLAG_NO_CREATE or FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}