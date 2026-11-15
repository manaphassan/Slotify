package my.slotify.app.feature.logistics

import android.content.Context
import androidx.work.*
import my.slotify.app.core.designsystem.theme.SystemStrings
import my.slotify.app.core.notification.SlotifyNotificationManager
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

/**
 * Pure geofence state machine (no Android dependencies, unit-tested).
 *
 * - "Near" is within 2 × radius. Approaching only fires when coming from outside that ring.
 * - Leaving requires going 10 % beyond the radius (hysteresis against GPS jitter).
 * - After leaving, the target stays EXITED while still near, so walking away never reports
 *   "approaching"; it resets to NONE once beyond the near ring.
 */
object GeofenceStateMachine {
    const val NEAR_FACTOR = 2.0
    const val EXIT_HYSTERESIS = 1.1

    fun next(previous: GeofenceEventType, distanceMeters: Double, radiusMeters: Double): GeofenceEventType {
        val inside = distanceMeters <= radiusMeters
        val near = distanceMeters <= radiusMeters * NEAR_FACTOR
        return when (previous) {
            GeofenceEventType.ENTERED, GeofenceEventType.INSIDE ->
                if (distanceMeters <= radiusMeters * EXIT_HYSTERESIS) GeofenceEventType.INSIDE else GeofenceEventType.EXITED

            GeofenceEventType.EXITED -> when {
                inside -> GeofenceEventType.ENTERED
                near -> GeofenceEventType.EXITED
                else -> GeofenceEventType.NONE
            }

            GeofenceEventType.NONE, GeofenceEventType.APPROACHING -> when {
                inside -> GeofenceEventType.ENTERED
                near -> GeofenceEventType.APPROACHING
                else -> GeofenceEventType.NONE
            }
        }
    }

    /** Event that should trigger an alert for this transition, or null. */
    fun alertFor(previous: GeofenceEventType, current: GeofenceEventType): GeofenceEventType? = when {
        current == GeofenceEventType.ENTERED && previous != GeofenceEventType.ENTERED -> GeofenceEventType.ENTERED
        current == GeofenceEventType.APPROACHING && previous == GeofenceEventType.NONE -> GeofenceEventType.APPROACHING
        else -> null
    }
}

object GeofenceManager {

    private const val WORK_NAME = "slotify_geofence_periodic_monitor"
    private const val STATE_PREFS = "slotify_geofence_state"
    private const val ALERT_COOLDOWN_MS = 10 * 60 * 1000L // per target *and* event type

    /** Geofence perimeters derived from the user's saved POIs (or the profile defaults). */
    fun getDefaultTargets(context: Context): List<GeofenceTarget> =
        PoiStorage.loadPois(context).map { poi ->
            val radius = when (poi.category) {
                PoiCategory.RUMAH -> 500.0
                PoiCategory.SEKOLAH -> 350.0
                PoiCategory.TRANSIT -> 300.0
                PoiCategory.PEJABAT -> 400.0
                PoiCategory.ALL -> 400.0
            }
            GeofenceTarget(poi.id, poi.name, poi.lat, poi.lng, radius, poi.iconEmoji)
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
     * Evaluates the current coordinate against all targets, persists each target's state and
     * posts a notification on ENTERED / APPROACHING transitions (rate-limited per event type).
     */
    fun evaluateProximity(
        context: Context,
        currentLat: Double,
        currentLng: Double,
        alertStrings: SystemStrings,
        targets: List<GeofenceTarget> = getDefaultTargets(context),
        now: Long = System.currentTimeMillis()
    ): List<GeofenceProximityStatus> {
        val prefs = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val results = targets.map { target ->
            val dist = calculateDistanceMeters(currentLat, currentLng, target.lat, target.lng)
            val previous = prefs.getString("state_${target.id}", null)
                ?.let { runCatching { GeofenceEventType.valueOf(it) }.getOrNull() }
                ?: GeofenceEventType.NONE
            val current = GeofenceStateMachine.next(previous, dist, target.radiusMeters)
            editor.putString("state_${target.id}", current.name)

            GeofenceStateMachine.alertFor(previous, current)?.let { event ->
                val key = "alert_${target.id}_${event.name}"
                if (now - prefs.getLong(key, 0L) >= ALERT_COOLDOWN_MS) {
                    editor.putLong(key, now)
                    SlotifyNotificationManager.showGeofenceProximityAlert(
                        context = context,
                        s = alertStrings,
                        locationName = target.name,
                        distanceMeters = dist,
                        eventType = event.name
                    )
                }
            }
            GeofenceProximityStatus(target, dist, current)
        }
        editor.apply()
        return results
    }

    /**
     * Enqueues the periodic background check. The worker itself exits early unless
     * background location has been granted.
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

    fun cancelBackgroundMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Clears persisted geofence state and cooldowns. */
    fun resetState(context: Context) {
        context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
