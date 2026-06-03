package com.example.qiblaapp2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import android.net.Uri
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Build
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var myLocationButton: ImageButton
    private lateinit var locationManager: LocationManager
    private var userMarker: Marker? = null
    private var currentUserLocation: GeoPoint? = null

    // Kaaba coordinates in Mecca
    private val kaabaLatitude = 21.422487
    private val kaabaLongitude = 39.826206

    // Status tracking flags
    private var isLocationPermissionGranted = false
    private var isGpsEnabled = false
    private var hasInternetConnection = false
    private var notificationDialogShown = false

    // Добавить переменную для хранения линии направления
    private var qiblaLine: Polyline? = null

    private val gpsEnableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        // Можно обработать результат, если нужно
    }

    private var lastLocationUpdateTime: Long = 0L
    private var lastLocationAccuracy: Float = Float.MAX_VALUE

    private lateinit var textQiblaAngle: TextView
    private lateinit var textQiblaDirection: TextView
    private lateinit var textDistanceValue: TextView
    private lateinit var textDistanceUnit: TextView
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val GPS_ENABLE_REQUEST_CODE = 1002
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003

        private const val TILE_CACHE_MAX_BYTES = 1536L * 1024 * 1024   // 1.5 GB
        private const val TILE_CACHE_TRIM_BYTES = 1200L * 1024 * 1024 // trim to ~1.2 GB
        private const val TILE_CACHE_EXPIRY_MS = 60L * 24 * 60 * 60 * 1000 // 60 days
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initOsmdroid()
            setContentView(R.layout.activity_main)
            
            initViews()
            setupMap()
            setupLocationButton()
            setupNavigation()

            // Check all requirements
            checkAllRequirements()
            
            // Check notification permission (Android 13+)
            checkNotificationPermission()

            // Show privacy consent on first launch
            maybeShowPrivacyConsent()

        } catch (e: Exception) {
            showError("App Initialization Error", e.message ?: "Unknown error")
        }
    }

    private fun initOsmdroid() {
        val osmPrefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        val config = Configuration.getInstance()
        config.userAgentValue = packageName
        config.load(this, osmPrefs)
        config.userAgentValue = packageName
        config.osmdroidBasePath = File(cacheDir, "osmdroid")
        config.osmdroidTileCache = File(config.osmdroidBasePath, "tiles")
        config.tileFileSystemCacheMaxBytes = TILE_CACHE_MAX_BYTES
        config.tileFileSystemCacheTrimBytes = TILE_CACHE_TRIM_BYTES
        config.expirationOverrideDuration = TILE_CACHE_EXPIRY_MS
        config.tileDownloadThreads = 4
        config.save(this, osmPrefs)
    }

    private fun setupMap() {
        try {
            applyMapStyle()
            mapView.setUseDataConnection(true)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(16.0)
            mapView.isFlingEnabled = true

            val prefs = getSharedPreferences("prayer_settings", MODE_PRIVATE)
            val lat = prefs.getFloat("latitude", 54.3120f).toDouble()
            val lon = prefs.getFloat("longitude", 59.3847f).toDouble()
            mapView.controller.setCenter(GeoPoint(lat, lon))
            mapView.invalidate()
        } catch (e: Exception) {
            showError("Map Error", "Failed to initialize map: ${e.message}")
        }
    }

    private fun applyMapStyle() {
        if (MapStylePrefs.applyTo(mapView, this)) {
            showToast(getString(R.string.map_style_maptiler_no_key))
        }
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        myLocationButton = findViewById(R.id.myLocationButton)
        textQiblaAngle = findViewById(R.id.textQiblaAngle)
        textQiblaDirection = findViewById(R.id.textQiblaDirection)
        textDistanceValue = findViewById(R.id.textDistanceValue)
        textDistanceUnit = findViewById(R.id.textDistanceUnit)
        mapView.setMultiTouchControls(true)
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)
    }

    private fun setupLocationButton() {
        myLocationButton.setOnClickListener {
            if (currentUserLocation != null) {
                mapView.controller.animateTo(currentUserLocation)
                mapView.controller.setZoom(18.0)
            }
            startLocationUpdates()
        }
    }

    private fun checkAllRequirements() {
        // 1. Check internet connection
        checkInternetConnection()

        // 2. Check location permissions
        checkLocationPermission()

        // 3. Check GPS
        checkGpsStatus()

        updateLocationInfo()
    }

    private fun checkInternetConnection() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

            hasInternetConnection = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (!hasInternetConnection) {
                showToast("No internet connection. Map may not work correctly.")
            }
        } catch (e: Exception) {
            hasInternetConnection = false
            showToast("Error checking internet connection")
        }
    }

    private fun checkLocationPermission() {
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!isLocationPermissionGranted) {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show explanation to user
            AlertDialog.Builder(this)
                .setTitle("Location Permission")
                .setMessage("Location permission is required to determine Qibla direction.")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton("Cancel") { _, _ ->
                    showToast("App cannot work without location permission")
                }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkGpsStatus() {
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (!isGpsEnabled) {
                showGpsDisabledDialog()
            } else if (isLocationPermissionGranted) {
                startLocationUpdates()
            }
        } catch (e: Exception) {
            showError("GPS Error", "Failed to check GPS status: ${e.message}")
        }
    }

    private fun showGpsDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("GPS Disabled")
            .setMessage("GPS needs to be enabled to determine location.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                try {
                    gpsEnableLauncher.launch(intent)
                } catch (e: Exception) {
                    showToast("Failed to open GPS settings")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showToast("GPS is required for app to work")
            }
            .show()
    }

    private fun startLocationUpdates() {
        if (!isLocationPermissionGranted || !isGpsEnabled) {
            updateLocationInfo()
            return
        }
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                // Запрашиваем обновления от обоих провайдеров
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10f, this)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10f, this)

                // Получаем last known location от обоих провайдеров
                val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLocation = getBestLocation(gpsLocation, netLocation)
                bestLocation?.let { onLocationChanged(it) }
            }
        } catch (e: SecurityException) {
            showError("Security Error", "No location permission")
        } catch (e: Exception) {
            showError("GPS Error", "Failed to start location search: ${e.message}")
        }
    }

    private fun getBestLocation(loc1: Location?, loc2: Location?): Location? {
        return when {
            loc1 == null -> loc2
            loc2 == null -> loc1
            loc1.time > loc2.time -> loc1
            loc2.time > loc1.time -> loc2
            // Если время одинаковое, выбираем по точности
            else -> if (loc1.accuracy <= loc2.accuracy) loc1 else loc2
        }
    }

    private fun updateLocationInfo() {
        // Status information is available but not currently displayed
    }

    // LocationListener methods
    override fun onLocationChanged(location: Location) {
        if (location.time > lastLocationUpdateTime || location.accuracy < lastLocationAccuracy) {
            lastLocationUpdateTime = location.time
            lastLocationAccuracy = location.accuracy
            try {
                val userLocation = GeoPoint(location.latitude, location.longitude)
                currentUserLocation = userLocation
                // Сохраняем координаты в SharedPreferences
                val prefs = getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)
                prefs.edit {
                    putFloat("latitude", location.latitude.toFloat())
                    putFloat("longitude", location.longitude.toFloat())
                }
                // Отправляем broadcast для обновления времени намазов
                val intent = Intent("com.example.qiblaapp2.UPDATE_PRAYER_TIMES")
                sendBroadcast(intent)
                
                // Обновляем виджет при изменении местоположения
                PrayerTimesWidgetProvider.updateAllWidgets(this)

                updateLocationInfo()
                if (userMarker == null) {
                    mapView.controller.setCenter(userLocation)
                }
                addUserMarker(userLocation)
                addKaabaMarker()
                drawQiblaLine(userLocation)
                updateQiblaInfo(location)
            } catch (e: Exception) {
                showError("Location Update Error", e.message ?: "Unknown error")
            }
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            isGpsEnabled = false
            showToast("GPS disabled")
            updateLocationInfo()
        }
    }

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            isGpsEnabled = true
            showToast("GPS enabled")
            if (isLocationPermissionGranted) {
                startLocationUpdates()
            }
        }
    }

    private fun addUserMarker(location: GeoPoint) {
        try {
            userMarker?.let { mapView.overlays.remove(it) }

            userMarker = Marker(mapView).apply {
                position = location
                title = "Your Location"
                snippet = "Pray towards Qibla"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_mylocation)?.apply {
                    setTint(ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark))
                }
            }
            mapView.overlays.add(userMarker)
        } catch (e: Exception) {
            showError("Marker Error", "Failed to add user marker")
        }
    }

    private fun addKaabaMarker() {
        try {
            val kaabaLocation = GeoPoint(kaabaLatitude, kaabaLongitude)
            val kaabaMarker = Marker(mapView).apply {
                position = kaabaLocation
                title = "🕋 Kaaba"
                snippet = "Holy Kaaba in Mecca, Saudi Arabia"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_dialog_map)?.apply {
                    setTint(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                }
            }
            mapView.overlays.add(kaabaMarker)
        } catch (e: Exception) {
            showError("Kaaba Marker Error", "Failed to add Kaaba marker")
        }
    }

    @Suppress("DEPRECATION")
    private fun drawQiblaLine(userLocation: GeoPoint) {
        try {
            // Удаляем старую линию, если есть
            qiblaLine?.let { mapView.overlays.remove(it) }

            val kaabaPoint = GeoPoint(kaabaLatitude, kaabaLongitude)
            val line = Polyline().apply {
                addPoint(userLocation)
                addPoint(kaabaPoint)
                color = 0xFFFFD700.toInt()
                width = 8f
            }
            mapView.overlays.add(line)
            qiblaLine = line // Сохраняем ссылку на текущую линию
            mapView.invalidate()
        } catch (e: Exception) {
            showError("Line Error", "Failed to draw direction line")
        }
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2) * sin(dLat/2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2) * sin(dLon/2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return earthRadius * c
    }

    private fun updateQiblaInfo(location: Location) {
        val angle = calculateBearing(
            location.latitude, location.longitude,
            kaabaLatitude, kaabaLongitude
        )
        textQiblaAngle.text = String.format(Locale.getDefault(), "%.1f°", angle)
        textQiblaDirection.text = getDirectionNameEn(angle)
        val distance = calculateDistance(
            location.latitude, location.longitude,
            kaabaLatitude, kaabaLongitude
        )
        textDistanceValue.text = String.format(Locale.getDefault(), "%.1f", distance)
        textDistanceUnit.text = getString(R.string.distance_unit_km)
    }

    private fun getDirectionNameEn(angle: Double): String {
        return when {
            angle >= 337.5 || angle < 22.5 -> "NORTH"
            angle >= 22.5 && angle < 67.5 -> "NORTH-EAST"
            angle >= 67.5 && angle < 112.5 -> "EAST"
            angle >= 112.5 && angle < 157.5 -> "SOUTH-EAST"
            angle >= 157.5 && angle < 202.5 -> "SOUTH"
            angle >= 202.5 && angle < 247.5 -> "SOUTH-WEST"
            angle >= 247.5 && angle < 292.5 -> "WEST"
            angle >= 292.5 && angle < 337.5 -> "NORTH-WEST"
            else -> ""
        }
    }

    private fun setupNavigation() {
        val btnDirection = findViewById<LinearLayout>(R.id.btnDirection)
        val btnPrayerTimes = findViewById<LinearLayout>(R.id.btnPrayerTimes)
        val btnDua = findViewById<LinearLayout>(R.id.btnDua)
        val btnSettings = findViewById<LinearLayout>(R.id.btnSettings)

        btnDirection.setOnClickListener {
            // Already on Direction screen
        }

        btnPrayerTimes.setOnClickListener {
            startActivity(Intent(this, PrayerTimesActivity::class.java))
            finish()
        }

        btnDua.setOnClickListener {
            startActivity(Intent(this, DuaActivity::class.java))
            finish()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
        
        // Долгий тап для сброса флага диалога уведомлений (для отладки)
        btnSettings.setOnLongClickListener {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notification_dialog_shown", false).apply()
            notificationDialogShown = false
            showToast("Флаг диалога уведомлений сброшен")
            true
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true
                showToast("Permission granted")
                checkGpsStatus()
            } else {
                isLocationPermissionGranted = false
                showToast("App cannot work without location permission")
                updateLocationInfo()
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Разрешение на уведомления получено")
            } else {
                showToast("Уведомления не будут работать без разрешения")
                // Предлагаем открыть настройки
                AlertDialog.Builder(this)
                    .setTitle("Настройки уведомлений")
                    .setMessage("Вы можете включить уведомления в настройках приложения")
                    .setPositiveButton("Открыть настройки") { _, _ ->
                        try {
                            val intent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = android.net.Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            showToast("Не удалось открыть настройки")
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GPS_ENABLE_REQUEST_CODE) {
            // Check GPS after returning from settings
            checkGpsStatus()
        }
    }

    // Helper methods for showing errors
    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun checkNotificationPermission() {
        // Сначала проверяем SharedPreferences - показывали ли диалог ранее
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val dialogShownBefore = prefs.getBoolean("notification_dialog_shown", false)
        
        if (dialogShownBefore) {
            return // Не показываем диалог повторно
        }
        
        // Проверяем, показывали ли уже диалог в этой сессии
        if (notificationDialogShown) {
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                
                // Помечаем, что диалог был показан
                notificationDialogShown = true
                prefs.edit().putBoolean("notification_dialog_shown", true).apply()
                
                // Для Huawei устройств показываем более подробное объяснение
                val isHuawei = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) || 
                               Build.MANUFACTURER.equals("HONOR", ignoreCase = true)
                
                val message = if (isHuawei) {
                    "На устройствах Huawei/Honor необходимо разрешить уведомления для звуковых сигналов намаза. " +
                    "После разрешения также проверьте настройки автозапуска в диспетчере приложений."
                } else {
                    "Разрешение на уведомления необходимо для звуковых сигналов времени намаза."
                }
                
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    // Показываем объяснение пользователю
                    AlertDialog.Builder(this)
                        .setTitle("Разрешение на уведомления")
                        .setMessage(message)
                        .setPositiveButton("Разрешить") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_REQUEST_CODE
                            )
                        }
                        .setNegativeButton("Отмена") { _, _ ->
                            showToast("Без разрешения уведомления не будут работать")
                        }
                        .show()
                } else {
                    // Запрашиваем разрешение напрямую
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            // Для устройств до Android 13 проверяем, включены ли уведомления
            checkNotificationChannelEnabled()
        }
    }
    
    private fun checkNotificationChannelEnabled() {
        // Проверяем, показывали ли уже диалог
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val dialogShownBefore = prefs.getBoolean("notification_dialog_shown", false)
        
        if (dialogShownBefore || notificationDialogShown) {
            return
        }
        
        val isHuawei = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) || 
                       Build.MANUFACTURER.equals("HONOR", ignoreCase = true)
        
        if (isHuawei) {
            // Помечаем, что диалог был показан
            notificationDialogShown = true
            prefs.edit().putBoolean("notification_dialog_shown", true).apply()
            // Для Huawei показываем информацию о настройках
            AlertDialog.Builder(this)
                .setTitle("Настройка уведомлений")
                .setMessage("На устройствах Huawei/Honor для корректной работы уведомлений рекомендуется:\n\n" +
                          "1. Разрешить автозапуск приложения\n" +
                          "2. Добавить приложение в исключения энергосбережения\n" +
                          "3. Включить уведомления в настройках")
                .setPositiveButton("Открыть настройки") { _, _ ->
                    try {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = android.net.Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        showToast("Не удалось открыть настройки")
                    }
                }
                .setNegativeButton("Позже", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            mapView.onResume()
            mapView.setUseDataConnection(true)
            applyMapStyle()
            checkAllRequirements()
        } catch (e: Exception) {
            showError("Resume Error", e.message ?: "Unknown error")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            mapView.onPause()
            if (::locationManager.isInitialized) {
                locationManager.removeUpdates(this)
            }
        } catch (e: Exception) {
            // Ignore errors on pause
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::locationManager.isInitialized) {
                locationManager.removeUpdates(this)
            }
        } catch (e: Exception) {
            // Ignore errors on destroy
        }
    }

    private fun maybeShowPrivacyConsent() {
        val prefs = getSharedPreferences("app_consent", Context.MODE_PRIVATE)
        val accepted = prefs.getBoolean("privacy_accepted", false)
        if (accepted) return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.privacy_title))
            .setMessage(getString(R.string.privacy_message))
            .setPositiveButton(getString(R.string.privacy_agree)) { _, _ ->
                prefs.edit { putBoolean("privacy_accepted", true) }
            }
            .setNegativeButton(getString(R.string.privacy_disagree)) { _, _ ->
                finish()
            }
            .setNeutralButton(getString(R.string.privacy_open)) { _, _ ->
                try {
                    startActivity(Intent(this, PrivacyPolicyActivity::class.java))
                } catch (_: Exception) {}
            }
            .setCancelable(false)
            .show()
    }
}