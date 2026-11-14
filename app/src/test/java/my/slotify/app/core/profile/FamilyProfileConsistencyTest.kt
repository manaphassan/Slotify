package my.slotify.app.core.profile

import my.slotify.app.core.time.TimeOfDay
import my.slotify.app.feature.logistics.RouteSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Consistency checks for whichever FamilyProfileData is compiled in (demo on CI, private locally).
 * They catch broken references when the profile is edited by hand.
 */
class FamilyProfileConsistencyTest {

    private val profile: FamilyProfile = FamilyProfileData

    @Test
    fun sessionsAndSlots_referenceExistingParents() {
        val studentIds = profile.students.map { it.id }.toSet()
        val sessionIds = profile.sessions.map { it.id }.toSet()
        profile.sessions.forEach { assertTrue("session ${it.id} → unknown student", it.studentId in studentIds) }
        profile.slots.forEach { assertTrue("slot ${it.id} → unknown session", it.sessionId in sessionIds) }
        profile.icsImports.forEach { assertTrue("ics → unknown session", it.sessionId in sessionIds) }
        listOf(
            profile.locationNodes.map { it.studentId }, profile.homework.map { it.studentId },
            profile.exams.map { it.studentId }, profile.pbdAssessments.map { it.studentId })
            .flatten().forEach { assertTrue("unknown student $it", it in studentIds) }
    }

    @Test
    fun idsAreUnique() {
        fun assertUnique(name: String, ids: List<String>) =
            assertEquals("duplicate $name ids", ids.size, ids.toSet().size)
        assertUnique("student", profile.students.map { it.id })
        assertUnique("session", profile.sessions.map { it.id })
        assertUnique("slot", profile.slots.map { it.id })
        assertUnique("poi", profile.defaultPois.map { it.id })
        assertUnique("fee", profile.feeTemplates.map { it.seriesId })
    }

    @Test
    fun slots_haveValidSchoolDayAndTimeRange() {
        profile.slots.forEach {
            assertTrue("${it.id} day ${it.dayOfWeek}", it.dayOfWeek in 1..5)
            assertTrue("${it.id} ${it.startTime}-${it.endTime}", TimeOfDay.isValidRange(it.startTime, it.endTime))
            assertEquals("times must be stored as HH:mm", TimeOfDay.normalize(it.startTime), it.startTime)
        }
    }

    @Test
    fun everyRoute_hasKnownWaypoints_andOneStepPerLeg() {
        val poiIds = profile.defaultPois.map { it.id }.toSet()
        RouteSelection.entries.forEach { route ->
            val r = profile.routes[route]
            assertNotNull("missing route $route", r)
            r!!
            assertTrue("$route needs ≥ 2 waypoints", r.waypointPoiIds.size >= 2)
            r.waypointPoiIds.forEach { assertTrue("$route → unknown POI $it", it in poiIds) }
            assertEquals("$route steps", r.waypointPoiIds.size - 1, r.steps.size)
        }
    }

    @Test
    fun routineZones_areOrderedAndCoverTheDay() {
        val studentIds = profile.students.map { it.id }.toSet()
        profile.routineZones.forEach { (sid, zones) ->
            assertTrue("unknown student $sid", sid in studentIds)
            assertEquals("zones for $sid must be ascending", zones.sortedBy { it.untilMinuteOfDay }, zones)
            assertEquals("last zone must end the day", 24 * 60, zones.last().untilMinuteOfDay)
        }
    }

    @Test
    fun zoneAt_picksFirstZoneEndingAfterMinute() {
        val zones = listOf(
            RoutineZone(7 * 60, "Rumah", "R"),
            RoutineZone(13 * 60, "Sekolah", "S"),
            RoutineZone(24 * 60, "Rumah petang", "R2")
        )
        assertEquals("R", zones.zoneAt(0)?.shortLabel)
        assertEquals("S", zones.zoneAt(7 * 60)?.shortLabel)       // boundary is exclusive
        assertEquals("S", zones.zoneAt(12 * 60 + 59)?.shortLabel)
        assertEquals("R2", zones.zoneAt(23 * 60)?.shortLabel)
        assertEquals(null, emptyList<RoutineZone>().zoneAt(600))
    }
}
