package my.slotify.app.core.takwim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KpmTakwimRepositoryTest {

    @Test
    fun testSchoolTermsDefinition() {
        val terms = KpmTakwimRepository.schoolTerms
        assertEquals(3, terms.size)

        val term1 = terms[0]
        assertEquals(1, term1.termNumber)
        assertEquals("Penggal 1", term1.name)
        assertEquals(LocalDate.of(2026, 3, 16), term1.startDate)
        assertEquals(LocalDate.of(2026, 5, 22), term1.endDate)

        val term3 = terms[2]
        assertEquals(3, term3.termNumber)
        assertEquals("Penggal 3", term3.name)
    }

    @Test
    fun testIsHolidayDetection() {
        // 1 Mei 2026 is Hari Pekerja (Cuti Umum during Penggal 1)
        val labourDay = LocalDate.of(2026, 5, 1)
        val labourHoliday = KpmTakwimRepository.isHoliday(labourDay)
        assertNotNull(labourHoliday)
        assertEquals("Hari Pekerja", labourHoliday?.titleBm)
        assertEquals(HolidayType.CUTI_UMUM, labourHoliday?.type)

        // 11 Disember 2026 is Hari Keputeraan Sultan Selangor
        val sultanSelangorDay = LocalDate.of(2026, 12, 11)
        val sultanHoliday = KpmTakwimRepository.isHoliday(sultanSelangorDay)
        assertNotNull(sultanHoliday)
        assertEquals("Hari Keputeraan Sultan Selangor", sultanHoliday?.titleBm)
        assertEquals(HolidayType.CUTI_UMUM, sultanHoliday?.type)

        // 31 Ogos 2026 falls within Cuti Penggal 2
        val merdekaDay = LocalDate.of(2026, 8, 31)
        val augustHoliday = KpmTakwimRepository.isHoliday(merdekaDay)
        assertNotNull(augustHoliday)

        // A regular school day, e.g., Wednesday 15 April 2026
        val normalDay = LocalDate.of(2026, 4, 15)
        val nonHoliday = KpmTakwimRepository.isHoliday(normalDay)
        assertNull(nonHoliday)
    }

    @Test
    fun testIsSchoolDay() {
        // Wednesday 15 April 2026 is a normal school day
        val normalDay = LocalDate.of(2026, 4, 15)
        assertTrue(KpmTakwimRepository.isSchoolDay(normalDay))

        // Saturday 18 April 2026 is weekend
        val weekend = LocalDate.of(2026, 4, 18)
        assertFalse(KpmTakwimRepository.isSchoolDay(weekend))

        // 1 Mei 2026 is Hari Pekerja (Friday, but holiday)
        val labourDay = LocalDate.of(2026, 5, 1)
        assertFalse(KpmTakwimRepository.isSchoolDay(labourDay))
    }

    @Test
    fun testUpcomingHolidaysSorting() {
        val checkDate = LocalDate.of(2026, 6, 1)
        val upcoming = KpmTakwimRepository.getUpcomingHolidays(checkDate, limit = 4)

        assertFalse(upcoming.isEmpty())
        assertTrue(upcoming.size <= 4)

        // Ensure chronological order
        for (i in 0 until upcoming.size - 1) {
            assertFalse(upcoming[i].startDate.isAfter(upcoming[i + 1].startDate))
        }
    }

    @Test
    fun testSchoolTermLookup() {
        val dateInTerm1 = LocalDate.of(2026, 4, 20)
        val term = KpmTakwimRepository.getCurrentTerm(dateInTerm1)
        assertNotNull(term)
        assertEquals(1, term?.termNumber)
    }
}
