package my.slotify.app.core.takwim

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class HolidayType {
    CUTI_PENGGAL,
    CUTI_UMUM,
    CUTI_PERISTIWA,
    CUTI_AKHIR_SESI
}

data class SchoolHoliday(
    val id: String,
    val titleBm: String,
    val titleEn: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: HolidayType,
    val emoji: String,
    val description: String = ""
) {
    fun isOngoing(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)

    fun daysUntil(from: LocalDate): Long =
        ChronoUnit.DAYS.between(from, startDate)
}

data class SchoolTerm(
    val termNumber: Int,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)
}

/**
 * Official KPM Academic Calendar (Takwim Persekolahan KPM) 2026/2027
 * Specifically calibrated for KUMPULAN B:
 * Selangor, Kuala Lumpur, Putrajaya, Melaka, Negeri Sembilan, Pahang, Perak, Perlis, Pulau Pinang, Sabah, Sarawak.
 */
object KpmTakwimRepository {

    val schoolTerms = listOf(
        SchoolTerm(1, "Penggal 1", LocalDate.of(2026, 3, 16), LocalDate.of(2026, 5, 22)),
        SchoolTerm(2, "Penggal 2", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 28)),
        SchoolTerm(3, "Penggal 3", LocalDate.of(2026, 9, 7), LocalDate.of(2027, 2, 12))
    )

    val holidays = listOf(
        // Cuti Penggal KPM
        SchoolHoliday(
            id = "cuti-penggal-1",
            titleBm = "Cuti Penggal 1, Sesi 2026/2027",
            titleEn = "Term 1 School Holidays",
            startDate = LocalDate.of(2026, 5, 23),
            endDate = LocalDate.of(2026, 5, 31),
            type = HolidayType.CUTI_PENGGAL,
            emoji = "🏖️",
            description = "Cuti persekolahan pertengahan tahun penggal pertama (9 Hari)"
        ),
        SchoolHoliday(
            id = "cuti-penggal-2",
            titleBm = "Cuti Penggal 2, Sesi 2026/2027",
            titleEn = "Term 2 School Holidays",
            startDate = LocalDate.of(2026, 8, 29),
            endDate = LocalDate.of(2026, 9, 6),
            type = HolidayType.CUTI_PENGGAL,
            emoji = "🎒",
            description = "Cuti persekolahan penggal kedua (9 Hari)"
        ),
        SchoolHoliday(
            id = "cuti-penggal-3",
            titleBm = "Cuti Penggal 3, Sesi 2026/2027",
            titleEn = "Term 3 School Holidays",
            startDate = LocalDate.of(2026, 12, 19),
            endDate = LocalDate.of(2027, 1, 10),
            type = HolidayType.CUTI_PENGGAL,
            emoji = "🌴",
            description = "Cuti persekolahan akhir tahun / musim tengkujuh (23 Hari)"
        ),
        SchoolHoliday(
            id = "cuti-akhir-sesi",
            titleBm = "Cuti Akhir Persekolahan Sesi 2026/2027",
            titleEn = "End of Academic Year Holidays",
            startDate = LocalDate.of(2027, 2, 13),
            endDate = LocalDate.of(2027, 3, 14),
            type = HolidayType.CUTI_AKHIR_SESI,
            emoji = "🎓",
            description = "Cuti peralihan tahun persekolahan baharu (30 Hari)"
        ),

        // Cuti Umum & Kelepasan Negeri Selangor 2026
        SchoolHoliday(
            id = "cuti-tahun-baru",
            titleBm = "Tahun Baharu 2026",
            titleEn = "New Year's Day",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 1),
            type = HolidayType.CUTI_UMUM,
            emoji = "🎆",
            description = "Cuti umum seluruh Selangor & Malaysia"
        ),
        SchoolHoliday(
            id = "cuti-thaipusam",
            titleBm = "Hari Thaipusam",
            titleEn = "Thaipusam",
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 2, 1),
            type = HolidayType.CUTI_UMUM,
            emoji = "🪔",
            description = "Cuti kelepasan am Negeri Selangor"
        ),
        SchoolHoliday(
            id = "cuti-cny",
            titleBm = "Tahun Baharu Cina",
            titleEn = "Chinese New Year",
            startDate = LocalDate.of(2026, 2, 17),
            endDate = LocalDate.of(2026, 2, 18),
            type = HolidayType.CUTI_UMUM,
            emoji = "🧧",
            description = "Cuti kelepasan am persekutuan (Tahun Kuda)"
        ),
        SchoolHoliday(
            id = "cuti-raya-aidilfitri",
            titleBm = "Hari Raya Aidilfitri",
            titleEn = "Hari Raya Aidilfitri",
            startDate = LocalDate.of(2026, 3, 20),
            endDate = LocalDate.of(2026, 3, 22),
            type = HolidayType.CUTI_UMUM,
            emoji = "🌙",
            description = "Hari Raya Puasa 1 & 2 Syawal 1447H"
        ),
        SchoolHoliday(
            id = "cuti-hari-pekerja",
            titleBm = "Hari Pekerja",
            titleEn = "Labour Day",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 1),
            type = HolidayType.CUTI_UMUM,
            emoji = "🛠️",
            description = "Hari Pekerja Antarabangsa"
        ),
        SchoolHoliday(
            id = "cuti-wesak",
            titleBm = "Hari Wesak",
            titleEn = "Wesak Day",
            startDate = LocalDate.of(2026, 5, 31),
            endDate = LocalDate.of(2026, 5, 31),
            type = HolidayType.CUTI_UMUM,
            emoji = "🪷",
            description = "Hari Wesak"
        ),
        SchoolHoliday(
            id = "cuti-agong",
            titleBm = "Hari Keputeraan YDP Agong",
            titleEn = "King's Birthday",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 1),
            type = HolidayType.CUTI_UMUM,
            emoji = "👑",
            description = "Ulang Tahun Keputeraan Seri Paduka Baginda YDP Agong"
        ),
        SchoolHoliday(
            id = "cuti-raya-haji",
            titleBm = "Hari Raya Aidiladha",
            titleEn = "Hari Raya Haji",
            startDate = LocalDate.of(2026, 5, 27),
            endDate = LocalDate.of(2026, 5, 28),
            type = HolidayType.CUTI_UMUM,
            emoji = "🐑",
            description = "Hari Raya Qurban 10 Zulhijjah 1447H"
        ),
        SchoolHoliday(
            id = "cuti-awal-muharram",
            titleBm = "Awal Muharram (Maal Hijrah)",
            titleEn = "Awal Muharram",
            startDate = LocalDate.of(2026, 6, 16),
            endDate = LocalDate.of(2026, 6, 16),
            type = HolidayType.CUTI_UMUM,
            emoji = "🕌",
            description = "Tahun Baharu Hijrah 1448H"
        ),
        SchoolHoliday(
            id = "cuti-merdeka",
            titleBm = "Hari Kebangsaan Ke-69",
            titleEn = "National Day",
            startDate = LocalDate.of(2026, 8, 31),
            endDate = LocalDate.of(2026, 8, 31),
            type = HolidayType.CUTI_UMUM,
            emoji = "🇲🇾",
            description = "Hari Kemerdekaan Malaysia"
        ),
        SchoolHoliday(
            id = "cuti-hari-malaysia",
            titleBm = "Hari Malaysia",
            titleEn = "Malaysia Day",
            startDate = LocalDate.of(2026, 9, 16),
            endDate = LocalDate.of(2026, 9, 16),
            type = HolidayType.CUTI_UMUM,
            emoji = "🇲🇾",
            description = "Hari Pembentukan Malaysia"
        ),
        SchoolHoliday(
            id = "cuti-maulidur-rasul",
            titleBm = "Maulidur Rasul",
            titleEn = "Prophet Muhammad's Birthday",
            startDate = LocalDate.of(2026, 8, 26),
            endDate = LocalDate.of(2026, 8, 26),
            type = HolidayType.CUTI_UMUM,
            emoji = "📖",
            description = "Sambutan Kelahiran Nabi Muhammad S.A.W."
        ),
        SchoolHoliday(
            id = "cuti-deepavali",
            titleBm = "Hari Deepavali",
            titleEn = "Deepavali",
            startDate = LocalDate.of(2026, 11, 8),
            endDate = LocalDate.of(2026, 11, 9),
            type = HolidayType.CUTI_UMUM,
            emoji = "✨",
            description = "Pesta Cahaya Deepavali"
        ),
        SchoolHoliday(
            id = "cuti-sultan-selangor",
            titleBm = "Hari Keputeraan Sultan Selangor",
            titleEn = "Sultan of Selangor's Birthday",
            startDate = LocalDate.of(2026, 12, 11),
            endDate = LocalDate.of(2026, 12, 11),
            type = HolidayType.CUTI_UMUM,
            emoji = "👑",
            description = "Hari Kelepasan Am Negeri Selangor Darul Ehsan"
        ),
        SchoolHoliday(
            id = "cuti-krismas",
            titleBm = "Hari Krismas",
            titleEn = "Christmas Day",
            startDate = LocalDate.of(2026, 12, 25),
            endDate = LocalDate.of(2026, 12, 25),
            type = HolidayType.CUTI_UMUM,
            emoji = "🎄",
            description = "Hari Natal"
        )
    )

    /**
     * Check if a specific date is a recognized holiday.
     */
    fun isHoliday(date: LocalDate): SchoolHoliday? {
        return holidays.firstOrNull { it.isOngoing(date) }
    }

    /**
     * Check if school is in session (not weekend and not holiday).
     */
    fun isSchoolDay(date: LocalDate): Boolean {
        val isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY
        if (isWeekend) return false
        return isHoliday(date) == null
    }

    /**
     * Get upcoming holidays starting from [fromDate], sorted by nearest start date.
     */
    fun getUpcomingHolidays(fromDate: LocalDate, limit: Int = 5): List<SchoolHoliday> {
        return holidays
            .filter { !it.endDate.isBefore(fromDate) }
            .sortedBy { it.startDate }
            .take(limit)
    }

    /**
     * Find current term for the given date.
     */
    fun getCurrentTerm(date: LocalDate): SchoolTerm? {
        return schoolTerms.firstOrNull { it.contains(date) }
    }
}
