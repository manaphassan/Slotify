package my.slotify.app.core.time

import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.SlotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlotStatesTest {
    private fun slot(id: String, start: String, end: String, rehat: Boolean = false) = TimetableSlotEntity(
        id = id, sessionId = "s", dayOfWeek = 1, periodIndex = 1, startTime = start, endTime = end,
        subjectCode = if (rehat) "REHAT" else "BM", subjectName = id, isRehat = rehat
    )

    private val day = listOf(
        slot("a", "07:30", "08:30"),
        slot("b", "08:30", "09:30"),
        slot("rehat", "09:30", "10:00", rehat = true),
        slot("c", "10:00", "11:00"),
        slot("d", "11:00", "12:00")
    )

    @Test
    fun midMorning_marksCompletedNowNextUpcoming() {
        val s = SlotStates.forDay(day, nowMinutes = 8 * 60 + 45).associate { it.first.id to it.second }
        assertEquals(SlotStatus.COMPLETED, s["a"]!!.status)
        assertEquals(SlotStatus.NOW, s["b"]!!.status)
        assertEquals(45, s["b"]!!.minutesLeft)
        assertEquals(SlotStatus.BREAK, s["rehat"]!!.status)
        assertEquals(SlotStatus.NEXT, s["c"]!!.status)
        assertEquals(75, s["c"]!!.minutesUntilStart)
        assertEquals(SlotStatus.UPCOMING, s["d"]!!.status)
    }

    @Test
    fun otherDay_hasNoLiveStates() {
        val s = SlotStates.forDay(day, nowMinutes = null)
        assertEquals(SlotStatus.BREAK, s.first { it.first.id == "rehat" }.second.status)
        assertEquals(4, s.count { it.second.status == SlotStatus.UPCOMING })
        assertNull(s.first().second.minutesUntilStart)
    }

    @Test
    fun afterSchool_everythingCompleted() {
        val s = SlotStates.forDay(day, nowMinutes = 13 * 60)
        assertEquals(day.size, s.count { it.second.status == SlotStatus.COMPLETED })
    }

    @Test
    fun clockMinutes_parsesAndRejects() {
        assertEquals(510, clockMinutes("08:30"))
        assertNull(clockMinutes("8.30"))
    }
}
