package my.slotify.app.core.model

import androidx.compose.ui.graphics.Color
import my.slotify.app.core.designsystem.theme.SlotifyColors

data class SubjectMeta(
    val code: String,
    val nameBm: String,
    val nameEn: String,
    val color: Color,
    val isCoreAcademic: Boolean = true,
    val isRehat: Boolean = false
)

object KpmSubjectCatalog {
    val subjects = mapOf(
        "BM" to SubjectMeta("BM", "Bahasa Melayu", "Malay Language", SlotifyColors.KpmBlue),
        "BI" to SubjectMeta("BI", "Bahasa Inggeris", "English Language", SlotifyColors.EnglishRed),
        "MT" to SubjectMeta("MT", "Matematik", "Mathematics", SlotifyColors.MathAmber),
        "SN" to SubjectMeta("SN", "Sains", "Science", SlotifyColors.ScienceGreen),
        "PI" to SubjectMeta("PI", "Pendidikan Islam", "Islamic Studies", SlotifyColors.IslamTeal),
        "BA" to SubjectMeta("BA", "Bahasa Arab", "Arabic Language", SlotifyColors.IslamTeal),
        "SEJ" to SubjectMeta("SEJ", "Sejarah", "History", SlotifyColors.SejarahOrange),
        "RBT" to SubjectMeta("RBT", "Reka Bentuk & Teknologi", "Design & Technology", SlotifyColors.RbtBrown),
        "MZ" to SubjectMeta("MZ", "Pendidikan Muzik", "Music", SlotifyColors.KokuPurple),
        "PSV" to SubjectMeta("PSV", "Pendidikan Seni Visual", "Visual Arts", SlotifyColors.KokuPurple),
        "PJ" to SubjectMeta("PJ", "Pendidikan Jasmani", "Physical Education", SlotifyColors.ScienceGreen),
        "PK" to SubjectMeta("PK", "Pendidikan Kesihatan", "Health Education", SlotifyColors.ScienceGreen),
        "REHAT" to SubjectMeta("REHAT", "Waktu Rehat", "Recess", SlotifyColors.MathAmber, isCoreAcademic = false, isRehat = true),
        
        // SRA Specific Subjects
        "TAWHID" to SubjectMeta("TAWHID", "Tauhid / Aqidah", "Tawheed", SlotifyColors.IslamTeal),
        "SIRAH" to SubjectMeta("SIRAH", "Sirah Nabawiyyah", "Seerah", SlotifyColors.IslamTeal),
        "AKHLAQ" to SubjectMeta("AKHLAQ", "Akhlak Islamiah", "Islamic Ethics", SlotifyColors.IslamTeal),
        "JAWI" to SubjectMeta("JAWI", "Jawi & Khat", "Jawi Script", SlotifyColors.IslamTeal),
        "TAHFIZ" to SubjectMeta("TAHFIZ", "Hafazan Al-Quran", "Quran Memorization", SlotifyColors.IslamTeal),
        
        // Tadika / PASTI
        "IQRA" to SubjectMeta("IQRA", "Iqra' & Bacaan", "Iqra' Reading", SlotifyColors.IslamTeal),
        "AWAL_SAINS" to SubjectMeta("AWAL_SAINS", "Awal Sains", "Early Science", SlotifyColors.ScienceGreen)
    )

    fun getSubject(code: String): SubjectMeta {
        return subjects[code.uppercase()] ?: SubjectMeta(
            code = code,
            nameBm = code,
            nameEn = code,
            color = SlotifyColors.KpmBlue
        )
    }
}
