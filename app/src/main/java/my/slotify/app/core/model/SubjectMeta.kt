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
        "BM" to SubjectMeta("BM", "Bahasa Melayu", "Malay Language", SlotifyColors.SubjectBm),
        "BI" to SubjectMeta("BI", "Bahasa Inggeris", "English Language", SlotifyColors.SubjectBi),
        "MT" to SubjectMeta("MT", "Matematik", "Mathematics", SlotifyColors.SubjectMt),
        "SN" to SubjectMeta("SN", "Sains", "Science", SlotifyColors.SubjectSn),
        "PI" to SubjectMeta("PI", "Pendidikan Islam", "Islamic Studies", SlotifyColors.SubjectPi),
        "BA" to SubjectMeta("BA", "Bahasa Arab", "Arabic Language", SlotifyColors.SubjectPi),
        "SEJ" to SubjectMeta("SEJ", "Sejarah", "History", SlotifyColors.SubjectSej),
        "RBT" to SubjectMeta("RBT", "Reka Bentuk & Teknologi", "Design & Technology", SlotifyColors.SubjectRbt),
        "MZ" to SubjectMeta("MZ", "Pendidikan Muzik", "Music", SlotifyColors.SubjectIqra),
        "PSV" to SubjectMeta("PSV", "Pendidikan Seni Visual", "Visual Arts", SlotifyColors.SubjectIqra),
        "PJ" to SubjectMeta("PJ", "Pendidikan Jasmani", "Physical Education", SlotifyColors.SubjectSn),
        "PK" to SubjectMeta("PK", "Pendidikan Kesihatan", "Health Education", SlotifyColors.SubjectSn),
        "REHAT" to SubjectMeta("REHAT", "Waktu Rehat", "Recess", SlotifyColors.SubjectMt, isCoreAcademic = false, isRehat = true),
        
        // SRA Specific Subjects
        "TAWHID" to SubjectMeta("TAWHID", "Tauhid / Aqidah", "Tawheed", SlotifyColors.SubjectPi),
        "SIRAH" to SubjectMeta("SIRAH", "Sirah Nabawiyyah", "Seerah", SlotifyColors.SubjectPi),
        "AKHLAQ" to SubjectMeta("AKHLAQ", "Akhlak Islamiah", "Islamic Ethics", SlotifyColors.SubjectPi),
        "JAWI" to SubjectMeta("JAWI", "Jawi & Khat", "Jawi Script", SlotifyColors.SubjectPi),
        "TAHFIZ" to SubjectMeta("TAHFIZ", "Hafazan Al-Quran", "Quran Memorization", SlotifyColors.SubjectPi),
        
        // Tadika / PASTI
        "IQRA" to SubjectMeta("IQRA", "Iqra' & Bacaan", "Iqra' Reading", SlotifyColors.SubjectIqra),
        "AWAL_SAINS" to SubjectMeta("AWAL_SAINS", "Awal Sains", "Early Science", SlotifyColors.SubjectSn)
    )

    fun getSubject(code: String): SubjectMeta {
        return subjects[code.uppercase()] ?: SubjectMeta(
            code = code,
            nameBm = code,
            nameEn = code,
            color = SlotifyColors.getSubjectColor(code)
        )
    }
}
