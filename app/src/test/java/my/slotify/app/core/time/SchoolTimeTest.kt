package my.slotify.app.core.time

import my.slotify.app.core.database.IcsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SchoolTimeTest {

    // 2026-11-13 is a Friday
    private val friday = LocalDate.of(2026, 11, 13)
    private val saturday = friday.plusDays(1)
    private val sunday = friday.plusDays(2)
    private val monday = friday.plusDays(3)

    @Test
    fun schoolDay_mapsWeekdays_andRejectsWeekends() {
        assertEquals(1, SchoolDay.of(monday))
        assertEquals(5, SchoolDay.of(friday))
        assertNull(SchoolDay.of(saturday))
        assertNull(SchoolDay.of(sunday))
        assertTrue(SchoolDay.isWeekend(sunday))
    }

    @Test
    fun nextSchoolDay_fromFridayAndWeekend_isMonday() {
        assertEquals(1, SchoolDay.nextAfter(friday))
        assertEquals(1, SchoolDay.nextAfter(saturday))
        assertEquals(1, SchoolDay.nextAfter(sunday))   // previously mapped to Tuesday
        assertEquals(2, SchoolDay.nextAfter(monday))
        assertEquals(monday, SchoolDay.nextSchoolDate(friday))
    }

    @Test
    fun examDates_parseMalayAndEnglishMonths() {
        assertEquals(LocalDate.of(2026, 10, 2), ExamDates.parse("02 Okt 2026"))
        assertEquals(LocalDate.of(2026, 10, 2), ExamDates.parse("02 Oct 2026"))
        assertEquals(LocalDate.of(2026, 8, 5), ExamDates.parse("5 Ogo 2026"))
        assertEquals(LocalDate.of(2026, 12, 4), ExamDates.parse("04 Dis 2026"))
        assertEquals(LocalDate.of(2026, 3, 1), ExamDates.parse("1 Mac 2026"))
        assertNull(ExamDates.parse("next week"))
        assertEquals(7L, ExamDates.daysUntil("21 Nov 2026", LocalDate.of(2026, 11, 14)))
    }

    @Test
    fun timeOfDay_normalizesAndValidates() {
        assertEquals("08:05", TimeOfDay.normalize("8:05"))
        assertEquals("13:30", TimeOfDay.normalize("13.30"))
        assertNull(TimeOfDay.normalize("24:00"))
        assertNull(TimeOfDay.normalize("8:5"))
        assertTrue(TimeOfDay.isValidRange("08:00", "8:30"))
        assertFalse(TimeOfDay.isValidRange("09:00", "08:30"))
        assertFalse(TimeOfDay.isValidRange("09:00", "09:00"))
        assertEquals(8 * 60 + 5, TimeOfDay.toMinutes("08:05"))
    }

    @Test
    fun icsParser_skipsWeekendEvents_andIndexesByStartTime() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT", "SUMMARY:Sains",
            "DTSTART;TZID=Asia/Kuala_Lumpur:20261116T140000", "DTEND;TZID=Asia/Kuala_Lumpur:20261116T150000",
            "END:VEVENT",
            "BEGIN:VEVENT", "SUMMARY:Bahasa Melayu",
            "DTSTART;TZID=Asia/Kuala_Lumpur:20261116T130000", "DTEND;TZID=Asia/Kuala_Lumpur:20261116T140000",
            "END:VEVENT",
            "BEGIN:VEVENT", "SUMMARY:Kelas Tambahan Sabtu",
            "DTSTART;TZID=Asia/Kuala_Lumpur:20261114T090000", "DTEND;TZID=Asia/Kuala_Lumpur:20261114T100000",
            "END:VEVENT",
            "END:VCALENDAR"
        ).joinToString("\n")
        val slots = IcsParser.parse(ics.byteInputStream(), "session-x")
        assertEquals(2, slots.size)                 // Saturday event dropped (was mapped to Monday)
        assertTrue(slots.all { it.dayOfWeek == 1 })
        val byPeriod = slots.sortedBy { it.periodIndex }
        assertEquals("BM", byPeriod[0].subjectCode)
        assertEquals("13:00", byPeriod[0].startTime)
        assertEquals("SN", byPeriod[1].subjectCode)
    }
}
