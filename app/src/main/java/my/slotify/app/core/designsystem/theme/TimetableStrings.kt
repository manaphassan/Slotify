package my.slotify.app.core.designsystem.theme

/** Timetable screen and KPM takwim calendar dialog copy. Accessed as `strings.timetableUi`. */
data class TimetableStrings(
    // School weekdays (1 = Monday … 5 = Friday)
    val dayMonday: String,
    val dayTuesday: String,
    val dayWednesday: String,
    val dayThursday: String,
    val dayFriday: String,

    // Header & session badge
    val studentFallback: String,
    val sessionBadgeMorning: (String) -> String,
    val sessionBadgeAfternoon: (String) -> String,
    val addSlotDesc: String,

    // Realtime tracker
    val clockLiveTag: String,
    val clockPreviewTag: String,
    val simLiveChip: String,
    val noClassOrBreak: String,
    val sessionFinished: String,

    // Day view & slot card
    val emptyDaySlots: String,
    val breakFallbackLabel: String,
    val subjectTeacherFallback: String,
    val slotOptionsDesc: String,
    val deleteSlotButton: String,

    // Week view
    val noSlotsYet: String,

    // Tomorrow view
    val bookTypeTextbook: String,
    val bookTypeWorkbook: String,
    val allBooksChecked: String,

    // Add slot dialog
    val addSlotTitle: String,
    val dayLabel: String,
    val subjectLabel: String,
    val startTimeLabel: String,
    val endTimeLabel: String,
    val teacherNameLabel: String,
    val teacherNamePlaceholder: String,
    val textbookToggle: String,
    val workbookToggle: String,
    val invalidTimeMessage: String,
    val saveSlotButton: String,

    // KPM takwim calendar dialog
    val takwimTitle: String,
    val takwimSubtitle: String,
    val academicStatusLabel: String,
    val currentTermOngoing: (String) -> String,
    val sessionYearFallback: String,
    val groupBadge: String,
    val takwimTabAll: String,
    val takwimTabTermBreaks: String,
    val takwimTabPublicHolidays: String,
    val holidayOngoing: String,
    val holidayToday: String,
    val holidayDaysUntil: (Long) -> String,
    val holidayPast: String,
    val closeTakwimButton: String
)

val BmTimetableStrings = TimetableStrings(
    // School weekdays
    dayMonday = "Isnin",
    dayTuesday = "Selasa",
    dayWednesday = "Rabu",
    dayThursday = "Khamis",
    dayFriday = "Jumaat",

    // Header & session badge
    studentFallback = "Murid",
    sessionBadgeMorning = { school -> "🌅 $school Pagi" },
    sessionBadgeAfternoon = { school -> "🌆 $school Petang" },
    addSlotDesc = "Tambah Slot",

    // Realtime tracker
    clockLiveTag = "kini",
    clockPreviewTag = "pratonton",
    simLiveChip = "Kini",
    noClassOrBreak = "Tiada Kelas / Rehat",
    sessionFinished = "Sesi Selesai",

    // Day view & slot card
    emptyDaySlots = "Tiada slot untuk hari ini.\nTekan + untuk tambah waktu belajar.",
    breakFallbackLabel = "Kantin / Rehat",
    subjectTeacherFallback = "Guru Subjek",
    slotOptionsDesc = "Opsyen",
    deleteSlotButton = "Padam Slot",

    // Week view
    noSlotsYet = "Tiada slot lagi",

    // Tomorrow view
    bookTypeTextbook = "Buku Teks",
    bookTypeWorkbook = "Buku Aktiviti",
    allBooksChecked = "✅ Semua buku sudah disemak untuk esok!",

    // Add slot dialog
    addSlotTitle = "➕ Tambah Waktu Belajar",
    dayLabel = "Hari",
    subjectLabel = "Subjek",
    startTimeLabel = "Mula (HH:MM)",
    endTimeLabel = "Tamat (HH:MM)",
    teacherNameLabel = "Nama Guru (pilihan)",
    teacherNamePlaceholder = "cth: Cikgu Rosli",
    textbookToggle = "📘 Buku Teks",
    workbookToggle = "📙 Buku Aktiviti",
    invalidTimeMessage = "Masa tidak sah: gunakan format HH:MM dan pastikan masa mula sebelum masa tamat.",
    saveSlotButton = "Simpan Waktu",

    // KPM takwim calendar dialog
    takwimTitle = "Takwim Persekolahan KPM",
    takwimSubtitle = "Sesi 2026/2027 • Kumpulan B (Selangor)",
    academicStatusLabel = "STATUS SESI AKADEMIK",
    currentTermOngoing = { term -> "$term (Sedang Berjalan)" },
    sessionYearFallback = "Sesi 2026/2027",
    groupBadge = "Kumpulan B",
    takwimTabAll = "Semua",
    takwimTabTermBreaks = "Cuti Penggal",
    takwimTabPublicHolidays = "Cuti Umum",
    holidayOngoing = "Sedang Berlangsung!",
    holidayToday = "Hari Ini!",
    holidayDaysUntil = { days -> "Lagi $days hari" },
    holidayPast = "Telah Lepas",
    closeTakwimButton = "Tutup Takwim"
)

val EnTimetableStrings = TimetableStrings(
    // School weekdays
    dayMonday = "Monday",
    dayTuesday = "Tuesday",
    dayWednesday = "Wednesday",
    dayThursday = "Thursday",
    dayFriday = "Friday",

    // Header & session badge
    studentFallback = "Student",
    sessionBadgeMorning = { school -> "🌅 $school Morning" },
    sessionBadgeAfternoon = { school -> "🌆 $school Afternoon" },
    addSlotDesc = "Add Slot",

    // Realtime tracker
    clockLiveTag = "live",
    clockPreviewTag = "preview",
    simLiveChip = "Now",
    noClassOrBreak = "No Class / Break",
    sessionFinished = "Session Over",

    // Day view & slot card
    emptyDaySlots = "No slots for this day.\nTap + to add a study period.",
    breakFallbackLabel = "Canteen / Break",
    subjectTeacherFallback = "Subject Teacher",
    slotOptionsDesc = "Options",
    deleteSlotButton = "Delete Slot",

    // Week view
    noSlotsYet = "No slots yet",

    // Tomorrow view
    bookTypeTextbook = "Textbook",
    bookTypeWorkbook = "Activity Book",
    allBooksChecked = "✅ All books checked for tomorrow!",

    // Add slot dialog
    addSlotTitle = "➕ Add Study Period",
    dayLabel = "Day",
    subjectLabel = "Subject",
    startTimeLabel = "Start (HH:MM)",
    endTimeLabel = "End (HH:MM)",
    teacherNameLabel = "Teacher Name (optional)",
    teacherNamePlaceholder = "e.g. Cikgu Rosli",
    textbookToggle = "📘 Textbook",
    workbookToggle = "📙 Activity Book",
    invalidTimeMessage = "Invalid time: use the HH:MM format and make sure the start time is before the end time.",
    saveSlotButton = "Save Period",

    // KPM takwim calendar dialog
    takwimTitle = "KPM School Calendar (Takwim)",
    takwimSubtitle = "Session 2026/2027 • Group B (Selangor)",
    academicStatusLabel = "ACADEMIC SESSION STATUS",
    currentTermOngoing = { term -> "$term (Ongoing)" },
    sessionYearFallback = "Session 2026/2027",
    groupBadge = "Group B",
    takwimTabAll = "All",
    takwimTabTermBreaks = "Term Breaks",
    takwimTabPublicHolidays = "Public Holidays",
    holidayOngoing = "Happening Now!",
    holidayToday = "Today!",
    holidayDaysUntil = { days -> "In $days days" },
    holidayPast = "Past",
    closeTakwimButton = "Close Calendar"
)
