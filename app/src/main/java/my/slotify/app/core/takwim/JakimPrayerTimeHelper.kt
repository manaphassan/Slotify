package my.slotify.app.core.takwim

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

/**
 * JAKIM-aligned Prayer Times Calculator calibrated for Zon SGR01:
 * Daerah Daerah Contoh (Bandar Contoh, Taman Contoh, Bukit Contoh), Sepang & Hulu Langat.
 * Uses official Malaysian solar calculation parameters (Fajr 20.0°, Isha 18.0°, Ihtiyat +2 min).
 */
object JakimPrayerTimeHelper {

    // Coordinates for Bandar Contoh, Daerah Contoh
    const val REGION_LATITUDE = 3.1390
    const val REGION_LONGITUDE = 101.6869
    const val TIMEZONE_OFFSET_HOURS = 8.0 // UTC+8

    data class PrayerSchedule(
        val subuh: String,
        val syuruk: String,
        val zohor: String,
        val asar: String,
        val maghrib: String,
        val isyak: String,
        val currentPrayer: String,
        val nextPrayerName: String,
        val countdownText: String,
        val transitNote: String = "Solat Zohor & Transit SRA: 01:15 PM – 02:00 PM"
    )

    data class RawPrayerTimes(
        val subuhMinutes: Int,
        val syurukMinutes: Int,
        val zohorMinutes: Int,
        val asarMinutes: Int,
        val maghribMinutes: Int,
        val isyakMinutes: Int
    )

    /**
     * Calculates prayer times for a given day of year and year using astronomical algorithm.
     */
    fun calculatePrayerTimes(calendar: Calendar = Calendar.getInstance()): RawPrayerTimes {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        
        // Solar declination & equation of time approximation (Spencer / Cooper formula)
        val b = 2.0 * Math.PI * (dayOfYear - 81) / 365.0
        val eotMinutes = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        val declinationDegrees = 23.45 * sin(2.0 * Math.PI * (284 + dayOfYear) / 365.0)
        val decRad = Math.toRadians(declinationDegrees)
        val latRad = Math.toRadians(REGION_LATITUDE)

        // Solar noon (Zohor) in minutes from midnight UTC+8
        // Local Mean Time to Standard Time adjustment: 4 minutes per degree longitude difference from 120°E (Malaysia standard meridian)
        val longDiffMinutes = (120.0 - REGION_LONGITUDE) * 4.0
        val solarNoonMinutes = (12.0 * 60.0) + longDiffMinutes - eotMinutes + 2.0 // +2 min ihtiyat

        // Hour angle calculation helper: cos(H) = (sin(alt) - sin(lat)*sin(dec)) / (cos(lat)*cos(dec))
        fun hourAngle(altitudeDegrees: Double): Double {
            val altRad = Math.toRadians(altitudeDegrees)
            val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            val clamped = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(clamped)) * 4.0 // 1 degree = 4 minutes
        }

        // Subuh: Sun is 20° below horizon (altitude = -20.0°)
        val fajrAngleMinutes = hourAngle(-20.0)
        val subuhMinutes = (solarNoonMinutes - fajrAngleMinutes + 2.0).roundToInt()

        // Syuruk: Sun upper limb on horizon (altitude = -0.833°)
        val sunriseMinutes = hourAngle(-0.833)
        val syurukMinutes = (solarNoonMinutes - sunriseMinutes).roundToInt()

        // Zohor: Midday
        val zohorMinutes = solarNoonMinutes.roundToInt()

        // Asar: Shafi'i jurisprudence (shadow = object length + noon shadow)
        val noonShadow = tan(abs(latRad - decRad))
        val asrAltitude = Math.toDegrees(atan(1.0 / (1.0 + noonShadow)))
        val asrAngleMinutes = hourAngle(asrAltitude)
        val asarMinutes = (solarNoonMinutes + asrAngleMinutes + 2.0).roundToInt()

        // Maghrib: Sunset (-0.833°) + 2 min ihtiyat
        val sunsetMinutes = hourAngle(-0.833)
        val maghribMinutes = (solarNoonMinutes + sunsetMinutes + 2.0).roundToInt()

        // Isyak: Sun is 18° below horizon (altitude = -18.0°) + 2 min ihtiyat
        val ishaAngleMinutes = hourAngle(-18.0)
        val isyakMinutes = (solarNoonMinutes + ishaAngleMinutes + 2.0).roundToInt()

        return RawPrayerTimes(
            subuhMinutes = subuhMinutes,
            syurukMinutes = syurukMinutes,
            zohorMinutes = zohorMinutes,
            asarMinutes = asarMinutes,
            maghribMinutes = maghribMinutes,
            isyakMinutes = isyakMinutes
        )
    }

    private fun formatMinutesToTime(minutes: Int): String {
        val normalized = (minutes % 1440 + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        val ampm = if (h >= 12) "PM" else "AM"
        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        return String.format(Locale.US, "%d:%02d %s", h12, m, ampm)
    }

    /**
     * Returns full user-facing prayer schedule with live countdown.
     */
    fun getSchedule(now: LocalTime = LocalTime.now(), calendar: Calendar = Calendar.getInstance()): PrayerSchedule {
        val raw = calculatePrayerTimes(calendar)
        val nowMinutes = now.hour * 60 + now.minute

        val timesList = listOf(
            "Subuh" to raw.subuhMinutes,
            "Syuruk" to raw.syurukMinutes,
            "Zohor" to raw.zohorMinutes,
            "Asar" to raw.asarMinutes,
            "Maghrib" to raw.maghribMinutes,
            "Isyak" to raw.isyakMinutes
        )

        // Find current and next
        var currentPrayer = "Isyak"
        var nextPrayerName = "Subuh"
        var diffMinutes = (raw.subuhMinutes + 1440 - nowMinutes) % 1440

        for (i in timesList.indices) {
            val (name, mins) = timesList[i]
            if (nowMinutes >= mins) {
                currentPrayer = name
                val nextIdx = (i + 1) % timesList.size
                val (nextName, nextMins) = timesList[nextIdx]
                nextPrayerName = nextName
                diffMinutes = if (nextMins >= nowMinutes) nextMins - nowMinutes else (nextMins + 1440 - nowMinutes)
            }
        }

        val countdownText = if (diffMinutes < 60) {
            "Lagi $diffMinutes min"
        } else {
            val hours = diffMinutes / 60
            val mins = diffMinutes % 60
            if (mins == 0) "Lagi $hours jam" else "Lagi ${hours}j ${mins}m"
        }

        return PrayerSchedule(
            subuh = formatMinutesToTime(raw.subuhMinutes),
            syuruk = formatMinutesToTime(raw.syurukMinutes),
            zohor = formatMinutesToTime(raw.zohorMinutes),
            asar = formatMinutesToTime(raw.asarMinutes),
            maghrib = formatMinutesToTime(raw.maghribMinutes),
            isyak = formatMinutesToTime(raw.isyakMinutes),
            currentPrayer = currentPrayer,
            nextPrayerName = nextPrayerName,
            countdownText = countdownText
        )
    }
}
