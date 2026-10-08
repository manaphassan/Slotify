package my.slotify.app.core.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmRoutingServiceTest {

    @Test
    fun testFallbackEstimationReturnsValidRoute() = runBlocking {
        val service = OsrmRoutingService()
        // Passing invalid coordinate structure or offline URL triggers fallback
        val response = service.getDrivingRoute("invalid;coords")
        
        assertNotNull(response)
        assertTrue(response.totalDistanceKm > 0)
        assertTrue(response.totalDurationMinutes > 0)
        assertEquals(3, response.legs.size)
        assertFalse(response.geometryPoints.isEmpty())
        assertEquals(4, response.geometryPoints.size)
        
        // Check initial and final coordinate pairs match Pejabat and Rumah
        val firstPoint = response.geometryPoints.first()
        val lastPoint = response.geometryPoints.last()
        assertEquals(3.1390, firstPoint.first, 0.001)
        assertEquals(3.1390, lastPoint.first, 0.001)
    }

    @Test
    fun testOsrmLegModel() {
        val leg = OsrmLeg(
            distanceMeters = 7200.0,
            durationSeconds = 720.0,
            summary = "Jalan Bandar Contoh"
        )
        assertEquals(7200.0, leg.distanceMeters, 0.01)
        assertEquals(720.0, leg.durationSeconds, 0.01)
        assertEquals("Jalan Bandar Contoh", leg.summary)
    }
}
