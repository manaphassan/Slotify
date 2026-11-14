package my.slotify.app.core.model

import my.slotify.app.core.designsystem.theme.AppLanguage

/** KPM weekly attire guide, keyed by SchoolDay index (1 = Isnin … 5 = Jumaat; null = weekend). */
object AttireGuide {
    fun forSchoolDay(day: Int?, language: AppLanguage = AppLanguage.BM): String =
        if (language == AppLanguage.EN) {
            when (day) {
                1 -> "Full KPM Uniform (White Shirt + Blue Trousers / Baju Kurung)"
                2 -> "Sports Attire / Sports House T-Shirt"
                3 -> "Uniformed Body Attire (Scouts / TKRS / Puteri Islam)"
                4 -> "Co-curricular T-Shirt / School Batik"
                5 -> "Full Baju Melayu / Baju Kurung"
                else -> "Casual Wear (Weekend)"
            }
        } else {
            when (day) {
                1 -> "Uniform Penuh KPM (Kemeja Putih + Seluar Biru / Baju Kurung)"
                2 -> "Baju Sukan / Rumah Sukan"
                3 -> "Uniform Badan Beruniform (Pengakap / TKRS / Puteri Islam)"
                4 -> "Baju Koko T-Shirt / Batik Sekolah"
                5 -> "Baju Melayu Lengkap / Baju Kurung"
                else -> "Pakaian Bebas (Hujung Minggu)"
            }
        }

    fun dayName(day: Int?, language: AppLanguage = AppLanguage.BM): String =
        if (language == AppLanguage.EN) {
            when (day) {
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                else -> "Weekend"
            }
        } else {
            when (day) {
                1 -> "Isnin"
                2 -> "Selasa"
                3 -> "Rabu"
                4 -> "Khamis"
                5 -> "Jumaat"
                else -> "Hujung Minggu"
            }
        }

    /** "Isnin: Uniform Penuh KPM …" */
    fun label(day: Int?, language: AppLanguage = AppLanguage.BM): String =
        if (day == null) forSchoolDay(null, language) else "${dayName(day, language)}: ${forSchoolDay(day, language)}"
}
