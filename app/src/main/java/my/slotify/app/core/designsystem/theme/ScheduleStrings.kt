package my.slotify.app.core.designsystem.theme

/** Schedule cards, status chips and hero cards shared by Today, Timetable and Logistics. Accessed as `strings.scheduleUi`. */
data class ScheduleStrings(
    // Status chips
    val statusCompleted: String,
    val statusNow: String,
    val statusUpcoming: String,
    val statusIn: (Int) -> String,
    val statusBreak: String,
    val statusExam: String,
    val statusOnTime: String,
    val statusOffline: String,

    // Hero card
    val heroCurrentClass: String,
    val heroNextClass: String,
    val heroNoClassToday: String,
    val heroNextSchoolDay: (String) -> String,
    val heroAllDoneTitle: String,
    val heroAllDoneSubtitle: String,
    val heroTimeLeft: (Int) -> String,

    // Lists
    val todaySchedule: String,
    val freeDayTitle: String,
    val freeDaySubtitle: String,
    val viewFullWeek: String,
    val weekRange: (String) -> String,

    // Greeting
    val greetingMorning: String,
    val greetingAfternoon: String,
    val greetingEvening: String
)

private fun bmDuration(m: Int) = when {
    m < 60 -> "${m}m"
    m % 60 == 0 -> "${m / 60}j"
    else -> "${m / 60}j ${m % 60}m"
}

private fun enDuration(m: Int) = when {
    m < 60 -> "${m}m"
    m % 60 == 0 -> "${m / 60}h"
    else -> "${m / 60}h ${m % 60}m"
}

val BmScheduleStrings = ScheduleStrings(
    statusCompleted = "Selesai",
    statusNow = "Sekarang",
    statusUpcoming = "Akan datang",
    statusIn = { m -> "Dalam ${bmDuration(m)}" },
    statusBreak = "Rehat",
    statusExam = "Ujian",
    statusOnTime = "Tepat masa",
    statusOffline = "Luar talian",

    heroCurrentClass = "KELAS SEMASA",
    heroNextClass = "KELAS SETERUSNYA",
    heroNoClassToday = "Tiada kelas hari ini",
    heroNextSchoolDay = { day -> "Sekolah seterusnya: $day" },
    heroAllDoneTitle = "Semua kelas selesai!",
    heroAllDoneSubtitle = "Hari persekolahan sudah tamat.",
    heroTimeLeft = { m -> "Baki ${bmDuration(m)}" },

    todaySchedule = "Jadual hari ini",
    freeDayTitle = "Hari bebas",
    freeDaySubtitle = "Tiada kelas hari ini",
    viewFullWeek = "Lihat jadual minggu →",
    weekRange = { range -> range },

    greetingMorning = "Selamat pagi",
    greetingAfternoon = "Selamat petang",
    greetingEvening = "Selamat malam"
)

val EnScheduleStrings = ScheduleStrings(
    statusCompleted = "Completed",
    statusNow = "Now",
    statusUpcoming = "Upcoming",
    statusIn = { m -> "In ${enDuration(m)}" },
    statusBreak = "Break",
    statusExam = "Exam",
    statusOnTime = "On time",
    statusOffline = "Offline",

    heroCurrentClass = "CURRENT CLASS",
    heroNextClass = "NEXT CLASS",
    heroNoClassToday = "No classes today",
    heroNextSchoolDay = { day -> "Next school day: $day" },
    heroAllDoneTitle = "All classes completed!",
    heroAllDoneSubtitle = "The school day is over.",
    heroTimeLeft = { m -> "${enDuration(m)} left" },

    todaySchedule = "Today's schedule",
    freeDayTitle = "Free day",
    freeDaySubtitle = "No classes today",
    viewFullWeek = "View full week →",
    weekRange = { range -> range },

    greetingMorning = "Good morning",
    greetingAfternoon = "Good afternoon",
    greetingEvening = "Good evening"
)
