package my.slotify.app.feature.logistics

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GeofenceManagerTest {

    private val rumahLat = 3.1390
    private val rumahLng = 101.6869

    private val skLat = 3.1390
    private val skLng = 101.6869

    @Before
    fun setUp() {
        GeofenceManager.resetCooldowns()
    }

    @Test
    fun testCalculateDistanceMeters_RumahToSkol() {
        val distance = GeofenceManager.calculateDistanceMeters(
            rumahLat, rumahLng,
            skLat, skLng
        )

        // Distance between Rumah (Taman Contoh) and SK Taman Contoh is ~2.4 km
        assertTrue("Distance should be between 2300m and 2550m", distance in 2300.0..2550.0)
    }

    @Test
    fun testOfflineMapBoundingBoxEnclosesFamilyNodes() {
        val bbox = OfflineMapManager.REGION_BOUNDING_BOX

        // Check North/South & East/West integrity
        assertTrue(bbox.latNorth > bbox.latSouth)
        assertTrue(bbox.lonEast > bbox.lonWest)

        // Check that Rumah is inside the pre-cached bounding box
        assertTrue("Rumah lat must be within bbox", rumahLat in bbox.latSouth..bbox.latNorth)
        assertTrue("Rumah lng must be within bbox", rumahLng in bbox.lonWest..bbox.lonEast)

        // Check that SK is inside
        assertTrue("SK lat must be within bbox", skLat in bbox.latSouth..bbox.latNorth)
        assertTrue("SK lng must be within bbox", skLng in bbox.lonWest..bbox.lonEast)

        // Check that Pejabat Abah in Bandar Contoh is inside
        val regionLat = 3.1390
        val regionLng = 101.6869
        assertTrue("Bandar Contoh lat must be within bbox", regionLat in bbox.latSouth..bbox.latNorth)
        assertTrue("Bandar Contoh lng must be within bbox", regionLng in bbox.lonWest..bbox.lonEast)

        // Check tile estimation
        val estimatedTiles = OfflineMapManager.estimateTileCount(12, 16)
        assertTrue("Estimated tiles should be positive", estimatedTiles > 300)
    }

    @Test
    fun testGeofenceTargetModelAndRadius() {
        val target = GeofenceTarget(
            id = "test-1",
            name = "SK Taman Contoh",
            lat = skLat,
            lng = skLng,
            radiusMeters = 350.0,
            iconEmoji = "🏫"
        )

        assertEquals("test-1", target.id)
        assertEquals("SK Taman Contoh", target.name)
        assertEquals(350.0, target.radiusMeters, 0.01)
        assertEquals("🏫", target.iconEmoji)
    }
}
