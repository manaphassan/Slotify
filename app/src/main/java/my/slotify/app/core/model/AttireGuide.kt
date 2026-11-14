package my.slotify.app.core.model

/** KPM weekly attire guide, keyed by SchoolDay index (1 = Isnin … 5 = Jumaat; null = weekend). */
object AttireGuide {
    fun forSchoolDay(day: Int?): String = when (day) {
        1 -> "Uniform Penuh KPM (Kemeja Putih + Seluar Biru / Baju Kurung)"
        2 -> "Baju Sukan / Rumah Sukan"
        3 -> "Uniform Badan Beruniform (Pengakap / TKRS / Puteri Islam)"
        4 -> "Baju Koko T-Shirt / Batik Sekolah"
        5 -> "Baju Melayu Lengkap / Baju Kurung"
        else -> "Pakaian Bebas (Hujung Minggu)"
    }

    fun dayName(day: Int?): String = when (day) {
        1 -> "Isnin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Khamis"
        5 -> "Jumaat"
        else -> "Hujung Minggu"
    }

    /** "Isnin: Uniform Penuh KPM …" */
    fun label(day: Int?): String = if (day == null) forSchoolDay(null) else "${dayName(day)}: ${forSchoolDay(day)}"
}
