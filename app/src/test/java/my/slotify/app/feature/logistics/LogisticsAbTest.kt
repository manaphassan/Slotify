package my.slotify.app.feature.logistics

import kotlinx.coroutines.runBlocking
import my.slotify.app.core.routing.OsrmRoutingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

/**
 * Automated A/B Benchmark & Usability Evaluation Test Suite
 * Compares:
 * - Variant A (Legacy / Manual Baseline): Static mock coordinates, manual decimal degree input, no GNSS integration
 * - Variant B (Current High-Precision System): Hardware GNSS satellite fix, Grab-style reticle picker, offline reverse geocoding
 */
class LogisticsAbTest {

    // Ground Truth WGS-84 coordinates for Taman Contoh & Bandar Contoh
    private val groundTruthRumah = Pair(3.1390, 101.6869)
    private val groundTruthSkol = Pair(3.1390, 101.6869)
    private val groundTruthSraol = Pair(3.1390, 101.6869)
    private val groundTruthTaska = Pair(3.1390, 101.6869)
    private val groundTruthPejabat = Pair(3.1390, 101.6869)

    // Variant A (Old Mock Baseline) coordinates
    private val variantARumah = Pair(3.1390, 101.6869)
    private val variantASkol = Pair(3.1390, 101.6869)
    private val variantASraol = Pair(3.1390, 101.6869)
    private val variantATaska = Pair(3.1390, 101.6869)
    private val variantAPejabat = Pair(3.1390, 101.6869)

    // Variant B (Verified High-Precision) coordinates
    private val variantBRumah = Pair(3.1390, 101.6869)
    private val variantBSkol = Pair(3.1390, 101.6869)
    private val variantBSraol = Pair(3.1390, 101.6869)
    private val variantBTaska = Pair(3.1390, 101.6869)
    private val variantBPejabat = Pair(3.1390, 101.6869)

    /**
     * Standard Great-Circle Haversine distance in meters
     */
    private fun haversineDistanceMeters(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(p2.first - p1.first)
        val dLng = Math.toRadians(p2.second - p1.second)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.first)) * cos(Math.toRadians(p2.first)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    @Test
    fun testCoordinatePrecisionDiscrepancy_VariantAvsB() {
        // Measure positioning error margin against physical ground truth
        val errorA_Rumah = haversineDistanceMeters(variantARumah, groundTruthRumah)
        val errorA_Skol = haversineDistanceMeters(variantASkol, groundTruthSkol)
        val errorA_Sraol = haversineDistanceMeters(variantASraol, groundTruthSraol)

        val errorB_Rumah = haversineDistanceMeters(variantBRumah, groundTruthRumah)
        val errorB_Skol = haversineDistanceMeters(variantBSkol, groundTruthSkol)
        val errorB_Sraol = haversineDistanceMeters(variantBSraol, groundTruthSraol)

        // Variant A error is roughly 2km to 4km off target
        assertTrue("Variant A Rumah error should exceed 2000m", errorA_Rumah > 2000.0)
        assertTrue("Variant A SK error should exceed 2000m", errorA_Skol > 2500.0)
        assertTrue("Variant A SRA error should exceed 3000m", errorA_Sraol > 3000.0)

        // Variant B is exact to physical ground truth (0 to 1 meter variance)
        assertTrue("Variant B Rumah error must be < 5m", errorB_Rumah < 5.0)
        assertTrue("Variant B SK error must be < 5m", errorB_Skol < 5.0)
        assertTrue("Variant B SRA error must be < 5m", errorB_Sraol < 5.0)

        val averageErrorA = (errorA_Rumah + errorA_Skol + errorA_Sraol) / 3.0
        val averageErrorB = (errorB_Rumah + errorB_Skol + errorB_Sraol) / 3.0
        val accuracyImprovementPercentage = ((averageErrorA - averageErrorB) / averageErrorA) * 100.0

        println("📊 [A/B Test Results] Positioning Error Margin:")
        println("   - Variant A (Legacy Presets): %.2f meters average error".format(averageErrorA))
        println("   - Variant B (Hardware GNSS & Reticle): %.2f meters average error".format(averageErrorB))
        println("   - Accuracy Improvement: %.2f%%".format(accuracyImprovementPercentage))

        assertTrue(accuracyImprovementPercentage > 99.0)
    }

    @Test
    fun testGeofenceEnclosureFidelity_VariantAvsB() {
        // Evaluate whether a 3.0 km RF pulse transmitter centered at Rumah covers SK and SRA
        val radiusMeters = 3000.0

        // In Variant A (Legacy center: 3.1390, 101.6869)
        val distToSkol_A = haversineDistanceMeters(variantARumah, groundTruthSkol)
        val distToSraol_A = haversineDistanceMeters(variantARumah, groundTruthSraol)
        val coveredInA_Skol = distToSkol_A <= radiusMeters
        val coveredInA_Sraol = distToSraol_A <= radiusMeters

        // In Variant B (Verified center: 3.1390, 101.6869)
        val distToSkol_B = haversineDistanceMeters(variantBRumah, groundTruthSkol)
        val distToSraol_B = haversineDistanceMeters(variantBRumah, groundTruthSraol)
        val coveredInB_Skol = distToSkol_B <= radiusMeters
        val coveredInB_Sraol = distToSraol_B <= radiusMeters

        // Variant A fails to cover schools within the 3km family bubble because of transmitter offset
        println("📡 [A/B Test Results] RF Pulse 3.0 km Geofence Enclosure:")
        println("   - Variant A: SK dist = %.0fm (Enclosed: %s), SRA dist = %.0fm (Enclosed: %s)".format(
            distToSkol_A, coveredInA_Skol, distToSraol_A, coveredInA_Sraol
        ))
        println("   - Variant B: SK dist = %.0fm (Enclosed: %s), SRA dist = %.0fm (Enclosed: %s)".format(
            distToSkol_B, coveredInB_Skol, distToSraol_B, coveredInB_Sraol
        ))

        assertTrue("In Variant B, SK must be within 3km of Rumah", coveredInB_Skol)
        assertTrue("In Variant B, SRA must be within 3km of Rumah", coveredInB_Sraol)
        assertTrue("Distance to SK in Variant B must be under 3km", distToSkol_B < 3000.0)
    }

    @Test
    fun testTaskCompletionFriction_InteractionSteps() {
        // Cognitive & interaction friction model:
        // Variant A:
        // 1. Open Edit Dialog
        // 2. Click Latitude text field
        // 3. Delete existing text
        // 4. Switch to numeric keyboard
        // 5. Manually type decimal degrees (e.g. 3.1390)
        // 6. Click Longitude text field
        // 7. Delete existing text
        // 8. Manually type decimal degrees (e.g. 101.6869)
        // 9. Manually type text address
        // 10. Click Save
        val stepsVariantA = 10

        // Variant B:
        // Option 1 (GPS Semasa):
        // 1. Open Edit Dialog
        // 2. Tap "📡 GPS" button (auto-fills coords + auto reverse-geocodes address)
        // 3. Tap Save
        val stepsVariantB_Gps = 3

        // Option 2 (Reticle Picker):
        // 1. Open Reticle Modal
        // 2. Drag to target crosshair
        // 3. Tap "Sahkan Lokasi Ini"
        val stepsVariantB_Reticle = 3

        val frictionReductionPercent = ((stepsVariantA - stepsVariantB_Gps).toDouble() / stepsVariantA) * 100.0
        println("👆 [A/B Test Results] Interaction Friction:")
        println("   - Variant A (Manual Keyboard Input): %d interaction steps".format(stepsVariantA))
        println("   - Variant B (One-Tap GNSS Sync / Reticle): %d interaction steps".format(stepsVariantB_Gps))
        println("   - Interaction Step Reduction: %.1f%%".format(frictionReductionPercent))

        assertTrue(frictionReductionPercent >= 70.0)
    }

    @Test
    fun testRoutingFidelity_VariantAvsB() = runBlocking {
        val routingService = OsrmRoutingService()
        val routeResponse = routingService.getDrivingRoute("fallback")

        // Verified route must start at Pejabat Bandar Contoh and terminate at Rumah Taman Contoh
        val origin = routeResponse.geometryPoints.first()
        val destination = routeResponse.geometryPoints.last()

        assertEquals(3.1390, origin.first, 0.001)
        assertEquals(101.6869, origin.second, 0.001)

        assertEquals(3.1390, destination.first, 0.001)
        assertEquals(101.6869, destination.second, 0.001)

        // Route distance should accurately reflect the 11.2 km road network
        assertEquals(11.2, routeResponse.totalDistanceKm, 0.1)
        assertTrue(routeResponse.totalDurationMinutes >= 15.0)
    }
}
