package my.slotify.app.feature.logistics

import android.content.Context
import androidx.work.*
import my.slotify.app.core.notification.SlotifyNotificationManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.*

enum class GeofenceEventType {
    ENTERED,
    APPROACHING,
    INSIDE,
    EXITED,
    NONE
}

data class GeofenceTarget(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double = 400.0,
    val iconEmoji: String = "📍"
)

data class GeofenceProximityStatus(
    val target: GeofenceTarget,
    val distanceMeters: Double,
    val eventType: GeofenceEventType
)

object GeofenceManager {

    private const val WORK_NAME = "slotify_geofence_periodic_monitor"
    private const val ALERT_COOLDOWN_MS = 10 * 60 * 1000L // 10 minutes between duplicate chimes

    // State memory for cooldown & previous location
    private val lastAlertTimestamps = ConcurrentHashMap<String, Long>()
    private val activeProximityStates = ConcurrentHashMap<String, GeofenceEventType>()

    /**
     * Default list of ground-truth Taman Contoh / Bandar Contoh geofence perimeters
     */
    fun getDefaultTargets(context: Context): List<GeofenceTarget> {
        val savedPois = PoiStorage.loadPois(context)
        return if (savedPois.isNotEmpty()) {
            savedPois.map { poi ->
                val radius = when (poi.category) {
                    PoiCategory.RUMAH -> 500.0
                    PoiCategory.SEKOLAH -> 350.0
                    PoiCategory.TRANSIT -> 300.0
                    PoiCategory.PEJABAT -> 400.0
                    PoiCategory.ALL -> 400.0
                }
                GeofenceTarget(
                    id = poi.id,
                    name = poi.name,
                    lat = poi.lat,
                    lng = poi.lng,
                    radiusMeters = radius,
                    iconEmoji = poi.iconEmoji
                )
            }
        } else {
            listOf(
                GeofenceTarget("geo-hom", "Rumah (Taman Contoh)", 3.1390, 101.6869, 500.0, "🏠"),
                GeofenceTarget("geo-sko", "SK Taman Contoh", 3.1390, 101.6869, 350.0, "🏫"),
                GeofenceTarget("geo-sra", "SRA Taman Contoh", 3.1390, 101.6869, 350.0, "🕌"),
                GeofenceTarget("geo-tas", "Taska Ceria", 3.1390, 101.6869, 300.0, "👶"),
                GeofenceTarget("geo-tad", "Tadika Ceria", 3.1390, 101.6869, 300.0, "🎒"),
                GeofenceTarget("geo-srai", "SRAI Bukit Contoh", 3.1390, 101.6869, 400.0, "🕌"),
                GeofenceTarget("geo-pej", "Pejabat Abah (Bandar Contoh)", 3.1390, 101.6869, 400.0, "🏢")
            )
        }
    }

    /**
     * WGS-84 Haversine distance in meters
     */
    fun calculateDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Evaluates current user coordinate against all active geofence targets.
     * Fires high-priority chime notifications when boundary transitions occur.
     */
    fun evaluateProximity(
        context: Context,
        currentLat: Double,
        currentLng: Double,
        targets: List<GeofenceTarget> = getDefaultTargets(context)
    ): List<GeofenceProximityStatus> {
        val results = mutableListOf<GeofenceProximityStatus>()
        val now = System.currentTimeMillis()

        targets.forEach { target ->
            val dist = calculateDistanceMeters(currentLat, currentLng, target.lat, target.lng)
            val previousState = activeProximityStates[target.id] ?: GeofenceEventType.NONE

            val currentState = when {
                dist <= target.radiusMeters -> {
                    if (previousState == GeofenceEventType.NONE || previousState == GeofenceEventType.APPROACHING) {
                        GeofenceEventType.ENTERED
                    } else {
                        GeofenceEventType.INSIDE
                    }
                }
                dist <= target.radiusMeters * 2.0 -> {
                    if (previousState == GeofenceEventType.NONE || previousState == GeofenceEventType.EXITED) {
                        GeofenceEventType.APPROACHING
                    } else {
                        GeofenceEventType.APPROACHING
                    }
                }
                else -> {
                    if (previousState == GeofenceEventType.ENTERED || previousState == GeofenceEventType.INSIDE) {
                        GeofenceEventType.EXITED
                    } else {
                        GeofenceEventType.NONE
                    }
                }
            }

            activeProximityStates[target.id] = currentState
            results.add(GeofenceProximityStatus(target, dist, currentState))

            // Check if notification is warranted
            val lastAlertTime = lastAlertTimestamps[target.id] ?: 0L
            val isCooldownOver = (now - lastAlertTime) >= ALERT_COOLDOWN_MS

            if (isCooldownOver && (currentState == GeofenceEventType.ENTERED || currentState == GeofenceEventType.APPROACHING)) {
                lastAlertTimestamps[target.id] = now
                val eventName = if (currentState == GeofenceEventType.ENTERED) "ENTERED" else "APPROACHING"
                SlotifyNotificationManager.showGeofenceProximityAlert(
                    context = context,
                    locationName = target.name,
                    distanceMeters = dist,
                    eventType = eventName
                )
            }
        }

        return results
    }

    /**
     * Enqueues Android WorkManager background periodic sync
     */
    fun scheduleBackgroundMonitoring(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<GeofenceProximityWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancels background periodic monitoring
     */
    fun cancelBackgroundMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Clears cooldown memory (useful for testing and reset)
     */
    fun resetCooldowns() {
        lastAlertTimestamps.clear()
        activeProximityStates.clear()
    }
}
