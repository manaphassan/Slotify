package my.slotify.app.feature.logistics

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.slotify.app.core.profile.FamilyProfileData
import java.util.Locale
import kotlin.math.abs

/**
 * Address resolver and reverse-geocoding utility for Slotify Logistics.
 * Combines Android Geocoder with the family profile's known landmarks for
 * instant, offline-safe address resolution.
 */
object LocationAddressHelper {

    /** Offline fallback landmarks supplied by the active family profile. */
    private val localLandmarks: List<Triple<Double, Double, String>>
        get() = FamilyProfileData.addressLandmarks.map { Triple(it.lat, it.lng, it.address) }

    /**
     * Resolves human-readable street or area address from geographic coordinates.
     */
    suspend fun resolveAddress(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        // 1. Try Android Geocoder if available
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.forLanguageTag("ms-MY"))
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resultList: List<Address>? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(lat, lng, 1) { list ->
                        resultList = list
                        latch.countDown()
                    }
                    latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    resultList
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                }

                val primary = addresses?.firstOrNull()
                if (primary != null) {
                    val line = primary.getAddressLine(0)
                    if (!line.isNullOrBlank()) {
                        return@withContext line
                    }
                    val feature = primary.thoroughfare ?: primary.subLocality ?: primary.locality
                    if (!feature.isNullOrBlank()) {
                        val state = primary.adminArea ?: "Selangor"
                        return@withContext "$feature, $state"
                    }
                }
            }
        } catch (_: Exception) {
            // Geocoder service may throw IOException if offline or network unavailable
        }

        // 2. Fallback to nearest local landmark
        val nearest = localLandmarks.minByOrNull {
            calculateDistanceKm(lat, lng, it.first, it.second)
        }

        if (nearest != null) {
            val dist = calculateDistanceKm(lat, lng, nearest.first, nearest.second)
            if (dist <= 0.25) {
                return@withContext nearest.third
            } else if (dist <= 2.5) {
                val dir = getCardinalDirection(nearest.first, nearest.second, lat, lng)
                return@withContext "${nearest.third} (${formatKm(dist)} $dir)"
            }
        }

        // 3. Fallback coordinate label
        String.format(Locale.US, "Lokasi Peta (%.5f° U, %.5f° T)", lat, lng)
    }

    private fun getCardinalDirection(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): String {
        val dLat = toLat - fromLat
        val dLng = toLng - fromLng
        return when {
            dLat > 0.001 && abs(dLng) <= 0.001 -> "Utara"
            dLat < -0.001 && abs(dLng) <= 0.001 -> "Selatan"
            dLng > 0.001 && abs(dLat) <= 0.001 -> "Timur"
            dLng < -0.001 && abs(dLat) <= 0.001 -> "Barat"
            dLat > 0 && dLng > 0 -> "Timur Laut"
            dLat > 0 && dLng < 0 -> "Barat Laut"
            dLat < 0 && dLng > 0 -> "Tenggara"
            else -> "Barat Daya"
        }
    }
}
