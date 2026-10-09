package my.slotify.app.feature.logistics

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Result data class for a GPS fix with accuracy evaluation.
 */
data class GpsFixResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val isSatelliteGps: Boolean = provider.equals(LocationManager.GPS_PROVIDER, ignoreCase = true),
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val quality: GpsAccuracyQuality
        get() = when {
            accuracyMeters <= 6.0f -> GpsAccuracyQuality.EXCELLENT
            accuracyMeters <= 15.0f -> GpsAccuracyQuality.GOOD
            accuracyMeters <= 35.0f -> GpsAccuracyQuality.MODERATE
            else -> GpsAccuracyQuality.COARSE
        }
}

/**
 * Adaptive Power Profiles to balance GPS accuracy vs battery consumption
 */
enum class GpsPowerProfile(val label: String, val minIntervalMs: Long, val minDistanceM: Float) {
    ACTIVE_HIGH_PRECISION("Ketepatan Tinggi (1Hz)", 1000L, 2.0f),
    BALANCED_TRANSIT("Seimbang (3s)", 3000L, 5.0f),
    BATTERY_SAVER("Jimat Bateri (30s)", 30000L, 25.0f)
}

/**
 * Categorized GPS accuracy levels with user-friendly descriptions and indicator colors.
 */
enum class GpsAccuracyQuality(val label: String, val colorHex: Long, val badgeEmoji: String) {
    EXCELLENT("Tepat (Pintu/Pagar)", 0xFF22C55E, "🟢"),   // <= 6m (Door/Gate precision)
    GOOD("Baik (Jalan/Lorong)", 0xFF0284C7, "🔵"),        // <= 15m (Street level)
    MODERATE("Sederhana", 0xFFF59E0B, "🟡"),              // <= 35m (Neighborhood)
    COARSE("Kasar (Mencari Satelit)", 0xFFEF4444, "🔴")   // > 35m (Acquiring lock)
}

/**
 * High-accuracy GPS location helper for Slotify Logistics.
 * Connects directly to hardware GPS and Network location providers to ensure
 * meter-level accuracy for map positioning and POI adjustments.
 */
object GpsLocationHelper {

    /**
     * Checks if either FINE or COARSE location permission is granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Checks whether high-accuracy GPS or network location providers are enabled.
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
        val netEnabled = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }
        return gpsEnabled || netEnabled
    }

    /**
     * Opens Android Location Settings so user can enable high accuracy GPS.
     */
    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Converts an Android Location object into a strongly-typed GpsFixResult.
     */
    fun Location.toGpsFixResult(): GpsFixResult = GpsFixResult(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        provider = provider ?: "gps",
        speedMps = speed,
        bearingDegrees = bearing,
        timestamp = time
    )

    /**
     * Obtains the highest-accuracy fresh GPS fix available with a timeout.
     * Combines hardware GPS_PROVIDER with NETWORK_PROVIDER for fast indoor assist.
     * Never discards moderate-accuracy indoor fixes (>10m) on timeout, and strictly rejects stale cached fixes.
     */
    @SuppressLint("MissingPermission")
    suspend fun getAccurateLocation(context: Context, timeoutMs: Long = 6000L): GpsFixResult? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null

        // 1. Quick check: If there is a very fresh (< 2 minutes) high-precision last known fix, return immediately
        val freshCached = getBestLastKnownLocation(lm, maxAgeMs = 2 * 60 * 1000L)
        if (freshCached != null && freshCached.accuracyMeters <= 12.0f) {
            return@withContext freshCached
        }

        // 2. Try Android R (API 30+) getCurrentLocation for hardware GPS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val api30Fix = getCurrentLocationApi30(lm, LocationManager.GPS_PROVIDER)
            if (api30Fix != null && api30Fix.accuracy <= 20.0f) {
                return@withContext api30Fix.toGpsFixResult()
            }
        }

        // 3. Request single fresh fix with active listener on GPS + Network providers
        val freshFix = requestSingleFreshFix(lm, timeoutMs)
        if (freshFix != null) {
            return@withContext freshFix
        }

        // 4. Fallback: only accept last-known locations within the last 10 minutes (rejects hours-old Jalan Gate fixes)
        getBestLastKnownLocation(lm, maxAgeMs = 10 * 60 * 1000L)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationApi30(lm: LocationManager, provider: String): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            val executor = Executors.newSingleThreadExecutor()

            try {
                lm.getCurrentLocation(
                    provider,
                    cancellationSignal,
                    executor
                ) { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                    executor.shutdown()
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
                executor.shutdown()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFreshFix(lm: LocationManager, timeoutMs: Long): GpsFixResult? = coroutineScope {
        var bestLoc: Location? = null
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val current = bestLoc
                // Keep the fix with better accuracy
                if (current == null || loc.accuracy < current.accuracy) {
                    bestLoc = loc
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            // Register both GPS (satellites) and Network (Wi-Fi/Cell for indoors)
            val mainLooper = android.os.Looper.getMainLooper()
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, mainLooper) } catch (_: Exception) {}
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, mainLooper) } catch (_: Exception) {}
            }

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val current = bestLoc
                // If high precision (<= 15 meters) achieved, return immediately without waiting for timeout
                if (current != null && current.accuracy <= 15.0f) {
                    return@coroutineScope current.toGpsFixResult()
                }
                delay(200)
            }

            // Timeout elapsed: do NOT throw away the indoor fix! Return best available if accuracy is reasonable (<= 50m)
            bestLoc?.takeIf { it.accuracy <= 50.0f }?.toGpsFixResult()
        } finally {
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    /**
     * Retrieves the best cached last-known location across providers,
     * STRICTLY enforcing a maximum age to avoid returning stale coordinates from hours ago.
     */
    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(lm: LocationManager, maxAgeMs: Long = 10 * 60 * 1000L): GpsFixResult? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        val now = System.currentTimeMillis()

        for (p in providers) {
            try {
                if (lm.isProviderEnabled(p)) {
                    val loc = lm.getLastKnownLocation(p)
                    if (loc != null) {
                        val ageMs = now - loc.time
                        // Discard if older than maxAgeMs or has invalid future time
                        if (ageMs in 0..maxAgeMs) {
                            if (best == null || loc.accuracy < best.accuracy) {
                                best = loc
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return best?.toGpsFixResult()
    }

    /**
     * Flow of continuous high-precision GPS updates as the user moves.
     */
    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(context: Context, minTimeMs: Long = 2000L, minDistanceM: Float = 2.0f): Flow<GpsFixResult> = callbackFlow {
        if (!hasLocationPermission(context)) {
            close()
            return@callbackFlow
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                trySend(loc.toGpsFixResult())
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val mainLooper = android.os.Looper.getMainLooper()
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, listener, mainLooper) } catch (_: Exception) {}
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, minDistanceM, listener, mainLooper) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
        }
    }
}
