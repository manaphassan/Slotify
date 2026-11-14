package my.slotify.app.core.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OsrmLeg(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val summary: String
)

data class OsrmRouteResponse(
    val totalDistanceKm: Double,
    val totalDurationMinutes: Double,
    val legs: List<OsrmLeg>,
    val geometryPoints: List<Pair<Double, Double>> = emptyList() // List of (latitude, longitude)
)

/**
 * Thin client for the OSRM driving API.
 *
 * Returns null on any failure — callers must show an explicit offline/estimate state rather
 * than presenting fabricated data as a live route. The public demo server is rate-limited
 * and intended for light use only; point [baseUrl] at a self-hosted instance for distribution.
 */
class OsrmRoutingService(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = sharedClient
) {

    suspend fun getDrivingRoute(coordinatesLonLatSemicolonSeparated: String): OsrmRouteResponse? =
        withContext(Dispatchers.IO) {
            val url =
                "$baseUrl/route/v1/driving/$coordinatesLonLatSemicolonSeparated?overview=full&geometries=geojson&steps=false"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful || body.isEmpty()) null else parse(body)
                }
            } catch (e: Exception) {
                null
            }
        }

    companion object {
        const val DEFAULT_BASE_URL = "https://router.project-osrm.org"
        const val USER_AGENT = "SlotifyApp/1.1 (Android; +https://github.com/manaphassan/Slotify)"

        private val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
        }

        /** Parses an OSRM `route` response. Returns null if it contains no route. */
        fun parse(body: String): OsrmRouteResponse? {
            val json = JSONObject(body)
            val routes = json.optJSONArray("routes") ?: return null
            if (routes.length() == 0) return null

            val route = routes.getJSONObject(0)
            val legsArray = route.optJSONArray("legs")
            val legs = (0 until (legsArray?.length() ?: 0)).map { i ->
                val leg = legsArray!!.getJSONObject(i)
                OsrmLeg(
                    distanceMeters = leg.getDouble("distance"),
                    durationSeconds = leg.getDouble("duration"),
                    summary = leg.optString("summary", "")
                )
            }

            // GeoJSON [lon, lat] -> (lat, lon)
            val coords = route.optJSONObject("geometry")?.optJSONArray("coordinates")
            val points = (0 until (coords?.length() ?: 0)).map { i ->
                val c = coords!!.getJSONArray(i)
                Pair(c.getDouble(1), c.getDouble(0))
            }

            return OsrmRouteResponse(
                totalDistanceKm = route.getDouble("distance") / 1000.0,
                totalDurationMinutes = route.getDouble("duration") / 60.0,
                legs = legs,
                geometryPoints = points
            )
        }
    }
}
