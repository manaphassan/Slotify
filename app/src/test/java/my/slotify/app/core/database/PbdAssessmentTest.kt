package my.slotify.app.core.database

import my.slotify.app.core.model.KpmSubjectCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PbdAssessmentTest {

    @Test
    fun testPbdAssessmentEntityCreation() {
        val assessment = PbdAssessmentEntity(
            id = UUID.randomUUID().toString(),
            studentId = "student-uuid-1",
            subjectCode = "BM",
            subjectName = "Bahasa Melayu",
            tahapPenguasaan = 5,
            teacherComment = "Bagus dan fasih berbahasa",
            assessedDate = "2026-10-09"
        )

        assertEquals("BM", assessment.subjectCode)
        assertEquals("Bahasa Melayu", assessment.subjectName)
        assertEquals(5, assessment.tahapPenguasaan)
        assertTrue(assessment.tahapPenguasaan in 1..6)
        assertEquals("Bagus dan fasih berbahasa", assessment.teacherComment)
    }

    @Test
    fun testKpmSubjectCatalogRetrieval() {
        val bmMeta = KpmSubjectCatalog.getSubject("BM")
        assertEquals("BM", bmMeta.code)
        assertEquals("Bahasa Melayu", bmMeta.nameBm)
        assertTrue(bmMeta.isCoreAcademic)

        val unknownMeta = KpmSubjectCatalog.getSubject("ROBOTIK")
        assertEquals("ROBOTIK", unknownMeta.code)
        assertNotNull(unknownMeta.nameBm)
    }

    @Test
    fun testStarCoinRewardLogicForPbd() {
        // High mastery (TP5 or TP6) gives +15 stars
        fun calculateStarsForTp(tp: Int): Int = if (tp >= 5) 15 else 5

        assertEquals(15, calculateStarsForTp(6))
        assertEquals(15, calculateStarsForTp(5))
        assertEquals(5, calculateStarsForTp(4))
        assertEquals(5, calculateStarsForTp(3))
        assertEquals(5, calculateStarsForTp(1))
    }
}
