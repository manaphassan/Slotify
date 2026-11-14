package my.slotify.app.feature.logistics

import my.slotify.app.feature.logistics.GeofenceEventType.APPROACHING
import my.slotify.app.feature.logistics.GeofenceEventType.ENTERED
import my.slotify.app.feature.logistics.GeofenceEventType.EXITED
import my.slotify.app.feature.logistics.GeofenceEventType.INSIDE
import my.slotify.app.feature.logistics.GeofenceEventType.NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceManagerTest {

    private val radius = 300.0

    /** Runs the state machine over a sequence of distances; returns states and alerts. */
    private fun run(vararg distances: Double): Pair<List<GeofenceEventType>, List<GeofenceEventType>> {
        var state = NONE
        val states = mutableListOf<GeofenceEventType>()
        val alerts = mutableListOf<GeofenceEventType>()
        for (d in distances) {
            val next = GeofenceStateMachine.next(state, d, radius)
            GeofenceStateMachine.alertFor(state, next)?.let { alerts += it }
            state = next
            states += next
        }
        return states to alerts
    }

    @Test
    fun haversineDistance_oneHundredthDegreeLatitude_isAbout1112m() {
        val d = GeofenceManager.calculateDistanceMeters(3.0, 101.0, 3.01, 101.0)
        assertTrue("was $d", d in 1100.0..1125.0)
    }

    @Test
    fun arriving_firesApproachingThenEntered_once() {
        val (states, alerts) = run(2000.0, 500.0, 400.0, 250.0, 100.0, 50.0)
        assertEquals(listOf(NONE, APPROACHING, APPROACHING, ENTERED, INSIDE, INSIDE), states)
        assertEquals(listOf(APPROACHING, ENTERED), alerts)
    }

    @Test
    fun startingInside_firesEnteredOnly() {
        val (_, alerts) = run(10.0, 20.0)
        assertEquals(listOf(ENTERED), alerts)
    }

    @Test
    fun leaving_neverReportsApproaching() {
        val (states, alerts) = run(50.0, 320.0, 400.0, 550.0, 700.0, 2000.0)
        // 320 m is within the 10 % exit hysteresis, so still INSIDE.
        assertEquals(listOf(ENTERED, INSIDE, EXITED, EXITED, NONE, NONE), states)
        assertEquals(listOf(ENTERED), alerts)
    }

    @Test
    fun gpsJitterAtBoundary_doesNotRefireEntered() {
        val (_, alerts) = run(290.0, 310.0, 295.0, 320.0, 299.0)
        assertEquals(listOf(ENTERED), alerts)
    }

    @Test
    fun reEnteringAfterExit_firesEnteredAgain() {
        val (_, alerts) = run(50.0, 400.0, 100.0)
        assertEquals(listOf(ENTERED, ENTERED), alerts)
    }

    @Test
    fun noAlertWhileStayingApproaching() {
        assertNull(GeofenceStateMachine.alertFor(APPROACHING, APPROACHING))
        assertNull(GeofenceStateMachine.alertFor(INSIDE, INSIDE))
    }
}
