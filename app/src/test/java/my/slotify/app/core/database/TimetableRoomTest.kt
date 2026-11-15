package my.slotify.app.core.database

import my.slotify.app.feature.timetable.slotPlaceLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimetableRoomTest {

    private val ics = """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:Bahasa Melayu
        LOCATION:Bilik 2 Imtiaz\, Blok A
        DTSTART;TZID=Asia/Kuala_Lumpur:20261116T073000
        DTEND;TZID=Asia/Kuala_Lumpur:20261116T083000
        END:VEVENT
        BEGIN:VEVENT
        SUMMARY:Matematik
        DTSTART;TZID=Asia/Kuala_Lumpur:20261116T083000
        DTEND;TZID=Asia/Kuala_Lumpur:20261116T093000
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()

    @Test
    fun icsLocation_becomesRoom_andDoesNotLeakToNextEvent() {
        val slots = IcsParser.parse(ics.byteInputStream(), "sess").sortedBy { it.startTime }
        assertEquals(2, slots.size)
        assertEquals("Bilik 2 Imtiaz, Blok A", slots[0].room)
        assertNull(slots[1].room)
    }

    private fun slot(room: String?, teacher: String?) = TimetableSlotEntity(
        sessionId = "s", dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "09:00",
        subjectCode = "BM", subjectName = "BM", teacherName = teacher, room = room
    )

    @Test
    fun placeLine_combinesRoomAndTeacher() {
        assertEquals("Bilik 2 • Cikgu A", slotPlaceLine(slot("Bilik 2", "Cikgu A")))
        assertEquals("Bilik 2", slotPlaceLine(slot("Bilik 2", null)))
        assertEquals("Cikgu A", slotPlaceLine(slot(" ", "Cikgu A")))
        assertNull(slotPlaceLine(slot(null, null)))
    }
}
