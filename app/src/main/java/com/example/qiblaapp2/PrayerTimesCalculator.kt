package com.example.qiblaapp2

import java.util.Locale
import java.util.Calendar
import kotlin.math.*

// Время намазов
data class PrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

// Простая замена LocalDate для совместимости с API < 26
data class SimpleDate(
    val year: Int,
    val month: Int, // 1-12
    val day: Int
) {
    val dayOfYear: Int
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)
            return calendar.get(Calendar.DAY_OF_YEAR)
        }
    
    companion object {
        fun now(): SimpleDate {
            val calendar = Calendar.getInstance()
            return SimpleDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        
        @Suppress("unused")
        fun of(year: Int, month: Int, day: Int): SimpleDate {
            return SimpleDate(year, month, day)
        }
    }
    
    fun plusDays(days: Int): SimpleDate {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}

enum class AsrMethod {
    SHAFII, HANAFI
}

enum class HighLatitudeMethod {
    AUTO, ONE_SEVENTH, MEDIAN, ANGLE, MIDNIGHT
}

// Prayer times calculator based on PrayTimes.py
class PrayerTimesCalculator(
    private val latitude: Double,
    private val longitude: Double,
    private val timezone: Double,
    asrMethodParam: AsrMethod,
    highLatitudeMethodParam: HighLatitudeMethod,
    private val elevation: Double = 0.0,
    fajrFixedParam: Boolean = false,
    ishaFixedParam: Boolean = false
) {
    // Приватные свойства для параметров конструктора
    private val asrMethod: AsrMethod = asrMethodParam
    private val highLatitudeMethod: HighLatitudeMethod = highLatitudeMethodParam
    private val fajrFixed: Boolean = fajrFixedParam
    private val ishaFixed: Boolean = ishaFixedParam
    
    // Оставляю только параметры MWL
    private val methods = mapOf(
        "fajr" to 18.0, "isha" to 17.0
    )

    // Default settings
    private val settings = mutableMapOf<String, Any>(
        "imsak" to "10 min",
        "dhuhr" to "0 min",
        "asr" to "Standard",
        "highLats" to "NightMiddle",
        "maghrib" to "0 min",
        "midnight" to "Standard"
    )

    init {
        // Set method parameters
        settings.putAll(methods)
        // Set Asr method
        settings["asr"] = if (asrMethod == AsrMethod.HANAFI) "Hanafi" else "Standard"
        // Set high latitude method
        settings["highLats"] = when (highLatitudeMethod) {
            HighLatitudeMethod.ONE_SEVENTH -> "OneSeventh"
            HighLatitudeMethod.MEDIAN -> "NightMiddle"
            HighLatitudeMethod.ANGLE -> "AngleBased"
            HighLatitudeMethod.MIDNIGHT -> "NightMiddle"
            HighLatitudeMethod.AUTO -> if (abs(latitude) > 48.0) "OneSeventh" else "AngleBased"
        }
    }

    fun calculate(date: SimpleDate): PrayerTimes {
        val jDate = julian(date.year, date.month, date.day) - longitude / (15.0 * 24.0)

        // Initial times
        var times = mutableMapOf(
            "imsak" to 5.0, "fajr" to 5.0, "sunrise" to 6.0, "dhuhr" to 12.0,
            "asr" to 13.0, "sunset" to 18.0, "maghrib" to 18.0, "isha" to 18.0
        )

        // Convert to day portions
        for (key in times.keys) {
            times[key] = times[key]!! / 24.0
        }

        // Main iteration
        times = computePrayerTimes(times, jDate)
        times = adjustTimes(times)

        // Add midnight
        if (settings["midnight"] == "Jafari") {
            times["midnight"] = times["sunset"]!! + timeDiff(times["sunset"]!!, times["fajr"]!!) / 2.0
        } else {
            times["midnight"] = times["sunset"]!! + timeDiff(times["sunset"]!!, times["sunrise"]!!) / 2.0
        }

        // Sunrise from NOAA (shown in UI); fixed Fajr must use the same value
        times["sunrise"] = calculateSunriseNoaa(date, latitude, longitude, timezone)

        if (fajrFixed) {
            times["fajr"] = fixHour(times["sunrise"]!! - 1.5)
        }
        if (ishaFixed) {
            times["isha"] = fixHour(times["maghrib"]!! + 1.5)
        }

        return PrayerTimes(
            fajr = getFormattedTime(times["fajr"]!!),
            sunrise = getFormattedTime(times["sunrise"]!!),
            dhuhr = getFormattedTime(times["dhuhr"]!!),
            asr = getFormattedTime(times["asr"]!!),
            maghrib = getFormattedTime(times["maghrib"]!!),
            isha = getFormattedTime(times["isha"]!!)
        )
    }

    // Compute prayer times
    private fun computePrayerTimes(times: MutableMap<String, Double>, jDate: Double): MutableMap<String, Double> {
        val imsak = sunAngleTime(evalParam(settings["imsak"]!!), times["imsak"]!!, jDate, "ccw")
        val fajr = sunAngleTime(evalParam(settings["fajr"]!!), times["fajr"]!!, jDate, "ccw")
        val sunrise = sunAngleTime(riseSetAngle(), times["sunrise"]!!, jDate, "ccw")
        val dhuhr = midDay(times["dhuhr"]!!, jDate)
        val asr = asrTime(asrFactor(settings["asr"]!!), times["asr"]!!, jDate)
        val sunset = sunAngleTime(riseSetAngle(), times["sunset"]!!, jDate, "cw")
        val maghrib = sunAngleTime(evalParam(settings["maghrib"]!!), times["maghrib"]!!, jDate, "cw")
        val isha = sunAngleTime(evalParam(settings["isha"]!!), times["isha"]!!, jDate, "cw")

        return mutableMapOf(
            "imsak" to imsak, "fajr" to fajr, "sunrise" to sunrise, "dhuhr" to dhuhr,
            "asr" to asr, "sunset" to sunset, "maghrib" to maghrib, "isha" to isha
        )
    }

    // Compute mid-day time
    private fun midDay(time: Double, jDate: Double): Double {
        val eqt = sunPosition(jDate + time)[1]
        return fixHour(12.0 - eqt)
    }

    // Compute time for sun angle
    private fun sunAngleTime(angle: Double, time: Double, jDate: Double, direction: String = "cw"): Double {
        return try {
            val decl = sunPosition(jDate + time)[0]
            val noon = midDay(time, jDate)
            val t = (1.0 / 15.0) * Math.toDegrees(acos(
                (-sin(Math.toRadians(angle)) - sin(Math.toRadians(decl)) * sin(Math.toRadians(latitude))) /
                        (cos(Math.toRadians(decl)) * cos(Math.toRadians(latitude)))
            ))
            noon + (if (direction == "ccw") -t else t)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    // Compute Asr time
    private fun asrTime(factor: Double, time: Double, jDate: Double): Double {
        val decl = sunPosition(jDate + time)[0]
        val angle = -Math.toDegrees(atan(1.0 / (factor + tan(Math.toRadians(abs(latitude - decl))))))
        return sunAngleTime(angle, time, jDate, "cw")
    }

    // Sun position (declination and equation of time)
    private fun sunPosition(jd: Double): DoubleArray {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d

        val ra = Math.toDegrees(atan2(
            cos(Math.toRadians(e)) * sin(Math.toRadians(l)),
            cos(Math.toRadians(l))
        )) / 15.0

        val eqt = q / 15.0 - fixHour(ra)
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))

        return doubleArrayOf(decl, eqt)
    }

    // Julian date
    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    // Adjust times
    private fun adjustTimes(times: MutableMap<String, Double>): MutableMap<String, Double> {
        val tzAdjust = timezone - longitude / 15.0
        for (key in times.keys) {
            times[key] = times[key]!! + tzAdjust
        }

        // High latitude adjustment
        if (settings["highLats"] != "None") {
            adjustHighLatTimes(times)
        }

        // Special cases for Imsak, Maghrib, Isha
        if (isMin(settings["imsak"]!!)) {
            times["imsak"] = times["fajr"]!! - evalParam(settings["imsak"]!!) / 60.0
        }
        if (isMin(settings["maghrib"]!!)) {
            times["maghrib"] = times["sunset"]!! + evalParam(settings["maghrib"]!!) / 60.0
        }
        if (isMin(settings["isha"]!!)) {
            times["isha"] = times["maghrib"]!! + evalParam(settings["isha"]!!) / 60.0
        }

        times["dhuhr"] = times["dhuhr"]!! + evalParam(settings["dhuhr"]!!) / 60.0

        return times
    }

    // Adjust high latitude times
    private fun adjustHighLatTimes(times: MutableMap<String, Double>) {
        val nightTime = timeDiff(times["sunset"]!!, times["sunrise"]!!)

        times["imsak"] = adjustHLTime(times["imsak"]!!, times["sunrise"]!!, evalParam(settings["imsak"]!!), nightTime, "ccw")
        times["fajr"] = adjustHLTime(times["fajr"]!!, times["sunrise"]!!, evalParam(settings["fajr"]!!), nightTime, "ccw")
        times["isha"] = adjustHLTime(times["isha"]!!, times["sunset"]!!, evalParam(settings["isha"]!!), nightTime, "cw")
        times["maghrib"] = adjustHLTime(times["maghrib"]!!, times["sunset"]!!, evalParam(settings["maghrib"]!!), nightTime, "cw")
    }

    // Adjust single time for high latitude
    private fun adjustHLTime(time: Double, base: Double, angle: Double, night: Double, direction: String): Double {
        val portion = nightPortion(angle, night)
        val diff = if (direction == "ccw") timeDiff(time, base) else timeDiff(base, time)
        return if (time.isNaN() || diff > portion) {
            base + (if (direction == "ccw") -portion else portion)
        } else time
    }

    // Night portion calculation
    private fun nightPortion(angle: Double, night: Double): Double {
        val method = settings["highLats"]
        return when (method) {
            "AngleBased" -> angle / 60.0 * night
            "OneSeventh" -> night / 7.0
            else -> night / 2.0 // NightMiddle
        }
    }

    // Helper functions
    private fun riseSetAngle(): Double = 0.833 + 0.0347 * sqrt(elevation)

    private fun asrFactor(asrParam: Any): Double = when (asrParam) {
        "Standard" -> 1.0
        "Hanafi" -> 2.0
        else -> evalParam(asrParam)
    }

    private fun evalParam(param: Any): Double = when (param) {
        is String -> {
            val numStr = param.replace(Regex("[^0-9.+-]"), "")
            if (numStr.isNotEmpty()) numStr.toDouble() else 0.0
        }
        is Number -> param.toDouble()
        else -> 0.0
    }

    private fun isMin(param: Any): Boolean = param is String && param.contains("min")

    private fun timeDiff(time1: Double, time2: Double): Double = fixHour(time2 - time1)

    private fun fixHour(hour: Double): Double = fix(hour, 24.0)

    private fun fixAngle(angle: Double): Double = fix(angle, 360.0)

    private fun fix(a: Double, mode: Double): Double {
        if (a.isNaN()) return a
        val fixed = a - mode * floor(a / mode)
        return if (fixed < 0) fixed + mode else fixed
    }

    private fun getFormattedTime(time: Double): String {
        if (time.isNaN()) return "N/A"

        val fixedTime = fixHour(time + 0.5 / 60.0) // add 0.5 minutes to round
        val hours = floor(fixedTime).toInt()
        val minutes = floor((fixedTime - hours) * 60).toInt()

        // Всегда используем 24-часовой формат
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    // --- Новый расчёт восхода солнца по формуле NOAA (аналог daylight) ---
    private fun calculateSunriseNoaa(date: SimpleDate, latitude: Double, longitude: Double, timezone: Double): Double {
        val dayOfYear = date.dayOfYear
        val lngHour = longitude / 15.0
        // Восход
        val t = dayOfYear + ((6.0 - lngHour) / 24.0)
        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(Math.toRadians(m))) + (0.020 * sin(2 * Math.toRadians(m))) + 282.634
        l = (l + 360) % 360
        val ra = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(l))))
        val lQuadrant  = (floor(l/90.0)) * 90.0
        val raQuadrant = (floor(ra/90.0)) * 90.0
        var raFix = ra + (lQuadrant - raQuadrant)
        raFix /= 15.0
        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(Math.toRadians(90.833)) - (sinDec * sin(Math.toRadians(latitude)))) / (cosDec * cos(Math.toRadians(latitude)))
        if (cosH > 1) return Double.NaN // солнце не восходит
        val h = 360.0 - Math.toDegrees(acos(cosH))
        val hFix = h / 15.0
        val time = hFix + raFix - (0.06571 * t) - 6.622
        var ut = (time - lngHour) % 24.0
        if (ut < 0) ut += 24.0
        val localT = ut + timezone
        return localT % 24.0
    }
    // --- конец нового кода ---
}

// Пример использования
fun main() {
    val calculator = PrayerTimesCalculator(
        latitude = 54.3120,
        longitude = 59.3847,
        timezone = 5.0, // UTC+5
        asrMethodParam = AsrMethod.SHAFII,
        highLatitudeMethodParam = HighLatitudeMethod.AUTO,
        elevation = 0.0
    )

    val today = SimpleDate.now()
    val prayerTimes = calculator.calculate(today)

    println("Время намазов на $today (координаты: 54.31°N, 59.38°E):")
    println("Фаджр: ${prayerTimes.fajr}")
    println("Восход: ${prayerTimes.sunrise}")
    println("Зухр: ${prayerTimes.dhuhr}")
    println("Аср: ${prayerTimes.asr}")
    println("Магриб: ${prayerTimes.maghrib}")
    println("Иша: ${prayerTimes.isha}")

    // Тест разных методов
    listOf(AsrMethod.SHAFII, AsrMethod.HANAFI).forEach { asrMethod ->
        val testCalc = PrayerTimesCalculator(
            latitude = 54.3120,
            longitude = 59.3847,
            timezone = 5.0,
            asrMethodParam = asrMethod,
            highLatitudeMethodParam = HighLatitudeMethod.AUTO
        )
        val testTimes = testCalc.calculate(today)
        println("\n$asrMethod: Фаджр ${testTimes.fajr}, Аср ${testTimes.asr}")
    }
}