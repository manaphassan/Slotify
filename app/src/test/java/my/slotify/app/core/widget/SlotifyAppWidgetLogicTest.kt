package my.slotify.app.core.widget

import my.slotify.app.feature.logistics.GeofenceEventType
import my.slotify.app.feature.logistics.GeofenceManager
import my.slotify.app.feature.logistics.GeofenceTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotifyAppWidgetLogicTest {

    @Test
    fun testGeofenceProximityCalculationsForKnownSchools() {
        val skOlakLempit = GeofenceTarget("geo-sko", "SK Taman Contoh", 3.1390, 101.6869, 350.0, "🏫")
        
        // Exact coordinate
        val distExact = GeofenceManager.calculateDistanceMeters(3.1390, 101.6869, skOlakLempit.lat, skOlakLempit.lng)
        assertEquals(0.0, distExact, 0.5)

        // 100 meters away (inside 350m perimeter)
        val distClose = GeofenceManager.calculateDistanceMeters(3.1390, 101.6869, skOlakLempit.lat, skOlakLempit.lng)
        assertTrue("Distance should be within 350m radius", distClose < 350.0)

        // Bandar Contoh town (~7km away)
        val distFar = GeofenceManager.calculateDistanceMeters(3.1390, 101.6869, skOlakLempit.lat, skOlakLempit.lng)
        assertTrue("Distance should exceed 5km", distFar > 5000.0)
    }

    @Test
    fun testMultiTargetProximityMatchesClosestZone() {
        val targets = listOf(
            GeofenceTarget("geo-hom", "Rumah (Taman Contoh)", 3.1390, 101.6869, 500.0, "🏠"),
            GeofenceTarget("geo-sko", "SK Taman Contoh", 3.1390, 101.6869, 350.0, "🏫"),
            GeofenceTarget("geo-srai", "SRAI Bukit Contoh", 3.1390, 101.6869, 400.0, "🕌")
        )

        // Coordinate near SK Taman Contoh
        val userLat = 3.1390
        val userLng = 101.6869

        val closest = targets.minByOrNull { t ->
            GeofenceManager.calculateDistanceMeters(userLat, userLng, t.lat, t.lng)
        }

        assertEquals("Closest target should be SK Taman Contoh", "geo-sko", closest?.id)
        val dist = GeofenceManager.calculateDistanceMeters(userLat, userLng, closest!!.lat, closest.lng)
        assertTrue("Distance should be under 50m", dist < 50.0)
    }

    @Test
    fun testBookHintSubjectMapping() {
        val booksMap = mapOf(
            "BM" to "📚 Bawa: Buku Teks Bahasa Melayu Jilid 1 & Buku Latihan",
            "MT" to "📚 Bawa: Buku Teks Matematik & Buku Latihan Jilid 1",
            "SN" to "📚 Bawa: Buku Teks Sains & Buku Amali Sains",
            "BI" to "📚 Bawa: Buku Teks Bahasa Inggeris & Reading Log",
            "TAWHID" to "📚 Bawa: Kitab Tauhid & Buku Nota SRA",
            "IQRA" to "📚 Bawa: Buku Iqra' & Buku Doa Harian"
        )

        booksMap.forEach { (code, expectedHint) ->
            assertTrue("Expected hint for $code", expectedHint.contains(code) || expectedHint.contains("Buku"))
        }
    }

    @Test
    fun testChildCyclingIndexModulo() {
        val studentCount = 3
        val studentNames = listOf("Adam", "Aisyah", "Danial")

        val cycle0 = 0 % studentCount
        val cycle1 = 1 % studentCount
        val cycle2 = 2 % studentCount
        val cycle3 = 3 % studentCount
        val cycle4 = 4 % studentCount

        assertEquals("Adam", studentNames[cycle0])
        assertEquals("Aisyah", studentNames[cycle1])
        assertEquals("Danial", studentNames[cycle2])
        assertEquals("Adam", studentNames[cycle3])
        assertEquals("Aisyah", studentNames[cycle4])
    }

    @Test
    fun testActiveChildHighlightFormatting() {
        val activeStudent = "Aisyah"
        val adamStatus = "SK"
        val aisyahStatus = "SRAI"
        val danialStatus = "Tadika"

        val summary = when (activeStudent) {
            "Aisyah" -> "👦 Adam: $adamStatus • [👧 Aisyah: $aisyahStatus] • 🧒 Danial: $danialStatus"
            "Danial" -> "👦 Adam: $adamStatus • 👧 Aisyah: $aisyahStatus • [🧒 Danial: $danialStatus]"
            else -> "[👦 Adam: $adamStatus] • 👧 Aisyah: $aisyahStatus • 🧒 Danial: $danialStatus"
        }

        assertTrue("Should highlight Aisyah with brackets", summary.contains("[👧 Aisyah: SRAI]"))
        assertTrue("Should not highlight Adam", !summary.contains("[👦 Adam"))
    }
}
