package my.slotify.app.core.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OsrmRoutingServiceTest {

    private val sampleResponse = """
        {"code":"Ok","routes":[{"distance":11200.0,"duration":1080.0,
          "legs":[{"distance":7500.0,"duration":720.0,"summary":"Jalan A"},
                  {"distance":3700.0,"duration":360.0,"summary":""}],
          "geometry":{"type":"LineString","coordinates":[[101.50,3.10],[101.55,3.12],[101.60,3.15]]}}]}
    """.trimIndent()

    @Test
    fun parse_readsTotalsLegsAndGeometry() {
        val route = OsrmRoutingService.parse(sampleResponse)
        assertNotNull(route)
        route!!
        assertEquals(11.2, route.totalDistanceKm, 1e-9)
        assertEquals(18.0, route.totalDurationMinutes, 1e-9)
        assertEquals(2, route.legs.size)
        assertEquals(7500.0, route.legs[0].distanceMeters, 1e-9)
        // GeoJSON is [lon, lat]; the model stores (lat, lon)
        assertEquals(3.10 to 101.50, route.geometryPoints.first())
        assertEquals(3.15 to 101.60, route.geometryPoints.last())
    }

    @Test
    fun parse_noRoutes_returnsNull() {
        assertNull(OsrmRoutingService.parse("""{"code":"NoRoute","routes":[]}"""))
    }

    @Test
    fun networkFailure_returnsNull_insteadOfFabricatedRoute() = runBlocking {
        // Port 1 on localhost refuses connections immediately; no external network is used.
        val service = OsrmRoutingService(baseUrl = "http://127.0.0.1:1")
        assertNull(service.getDrivingRoute("101.5,3.1;101.6,3.2"))
    }
}
