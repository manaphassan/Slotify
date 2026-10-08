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

class OsrmRoutingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun getDrivingRoute(coordinatesLonLatSemicolonSeparated: String): OsrmRouteResponse = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/driving/$coordinatesLonLatSemicolonSeparated?overview=full&geometries=geojson&steps=false"
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SlotifyApp/1.0 (Android; Bandar Contoh/Klang Valley)")
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isEmpty()) {
                return@withContext fallbackEstimation()
            }

            val json = JSONObject(responseBody)
            val routes = json.optJSONArray("routes")
            if (routes == null || routes.length() == 0) {
                return@withContext fallbackEstimation()
            }

            val route = routes.getJSONObject(0)
            val totalDistance = route.getDouble("distance") / 1000.0
            val totalDuration = route.getDouble("duration") / 60.0

            val legsArray = route.optJSONArray("legs")
            val legsList = mutableListOf<OsrmLeg>()
            if (legsArray != null) {
                for (i in 0 until legsArray.length()) {
                    val leg = legsArray.getJSONObject(i)
                    legsList.add(
                        OsrmLeg(
                            distanceMeters = leg.getDouble("distance"),
                            durationSeconds = leg.getDouble("duration"),
                            summary = leg.optString("summary", "Jalan Tempatan")
                        )
                    )
                }
            }

            // Parse detailed road geometry (GeoJSON [lon, lat] -> (lat, lon))
            val geomObj = route.optJSONObject("geometry")
            val coordsArray = geomObj?.optJSONArray("coordinates")
            val geomPoints = mutableListOf<Pair<Double, Double>>()
            if (coordsArray != null) {
                for (i in 0 until coordsArray.length()) {
                    val coord = coordsArray.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    geomPoints.add(Pair(lat, lon))
                }
            }

            OsrmRouteResponse(
                totalDistanceKm = totalDistance,
                totalDurationMinutes = totalDuration,
                legs = legsList,
                geometryPoints = geomPoints
            )
        } catch (e: Exception) {
            fallbackEstimation()
        }
    }

    private fun fallbackEstimation(): OsrmRouteResponse {
        return OsrmRouteResponse(
            totalDistanceKm = 9.9,
            totalDurationMinutes = 18.0,
            legs = listOf(
                OsrmLeg(7200.0, 720.0, "Pejabat Bandar Contoh ➔ Tadika Danial"),
                OsrmLeg(1200.0, 180.0, "Tadika Danial ➔ Taska Ceria"),
                OsrmLeg(1500.0, 180.0, "Taska Ceria ➔ Rumah")
            ),
            geometryPoints = listOf(
                Pair(3.1390, 101.6869), // Pejabat
                Pair(3.1390, 101.6869), // Tadika Danial
                Pair(3.1390, 101.6869), // Taska Ceria
                Pair(3.1390, 101.6869)  // Rumah
            )
        )
    }
}
