package my.slotify.app.core.designsystem.theme

/** Today dashboard and school-bag dialog copy. Accessed as `strings.dashboardUi`. */
data class DashboardStrings(
    // Takwim (academic calendar) banner
    val holidayBannerTitle: (String) -> String,
    val takwimBannerTitle: (String) -> String,
    val takwimSessionFallback: String,
    val holidayNoSchoolToday: String,
    val holidayCountdownToday: String,
    val holidayCountdownDays: (Long) -> String,
    val nextHolidayLabel: (String, String) -> String,
    val schoolGroupBadge: String,

    // Prayer times card
    val prayerTimesTitle: (String) -> String,
    val currentPrayerStateDescription: String,
    val prayerCountdown: (Int) -> String,
    val prayerTransitNote: String,

    // School bag summary card
    val bagCardPackedTitle: String,
    val bagCardPendingTitle: String,
    val bagCardPackedSubtitle: String,
    val bagCardPendingSubtitle: String,
    val bagCardPackedBadge: String,
    val bagCardPendingBadge: String,

    // Homework tracker
    val addTaskButton: String,
    val allTasksDone: String,
    val emptyCompletedTasks: String,
    val emptyPendingTasks: String,

    // Exams countdown
    val examsDetectedCount: (Int) -> String,
    val emptyExams: String,
    val examDaysUnit: String,
    val examFriendlyUasa: String,
    val examFriendlyPbd: String,
    val examFriendlyAssessment: String,

    // Ongoing session card
    val vanFallback: String,
    val weekendNoClass: String,
    val noMoreClassesToday: String,
    val slotUpcoming: String,
    val slotInProgress: String,

    // Tomorrow's books
    val textbook: String,
    val activityBook: String,
    val textbookFor: (String) -> String,
    val fallbackBookMalay: String,
    val fallbackBookMaths: String,
    val fallbackBookScience: String,
    val fallbackBookEnglish: String,

    // Routine automation & notifications card
    val routineAutomationTitle: String,
    val routineAutomationSubtitle: String,
    val routineActiveBadge: String,
    val transitMessageFallback: String,
    val attireBooksFallback: String,
    val transitTestSent: String,
    val attireTestSent: String,
    val testTransitButton: String,
    val testAttireButton: String,

    // In-home realtime class widget
    val minutesRemaining: (Int) -> String,
    val classroomFallback: String,
    val timetableNotSet: String,
    val noClassNow: String,
    val nextTimetableMissing: String,
    val noNextClass: String,

    // Focus timer dialog
    val focusSessionFor: (String) -> String,
    val focusMinutesOption: (Int) -> String,
    val focusStatusRunning: String,
    val focusStatusReady: String,
    val claimStarsButton: String,

    // Add homework dialog
    val addHomeworkTitle: String,
    val subjectSelectorLabel: String,
    val taskTitleLabel: String,
    val taskTitlePlaceholder: String,
    val dueDateLabel: String,
    val dueToday: String,
    val dueTomorrow: String,
    val dueMonday: String,
    val dueThursday: String,
    val photoAttachedTitle: String,
    val photoCaptureTitle: String,
    val photoAttachedFile: String,
    val photoCaptureHint: String,
    val notesLabel: String,
    val notesPlaceholder: String,
    val saveTaskButton: String,

    // School bag pack dialog
    val bagSubjectBooksItem: (String) -> String,
    val bagCategoryStudyBooks: String,
    val bagDonePackingButton: String,
    val bagSaveAndCloseButton: String
)

val BmDashboardStrings = DashboardStrings(
    // Takwim (academic calendar) banner
    holidayBannerTitle = { title -> "Cuti: $title" },
    takwimBannerTitle = { term -> "Takwim KPM • $term" },
    takwimSessionFallback = "Sesi 2026/2027",
    holidayNoSchoolToday = "Tiada sesi persekolahan hari ini • Selamat bercuti!",
    holidayCountdownToday = "Hari Ini!",
    holidayCountdownDays = { days -> "lagi $days hari" },
    nextHolidayLabel = { title, countdown -> "Cuti seterusnya: $title ($countdown)" },
    schoolGroupBadge = "Kumpulan B",

    // Prayer times card
    prayerTimesTitle = { zone -> "Waktu Solat • Zon $zone" },
    currentPrayerStateDescription = "Waktu solat semasa",
    prayerCountdown = { m -> if (m < 60) "Lagi $m min" else if (m % 60 == 0) "Lagi ${m / 60} jam" else "Lagi ${m / 60}j ${m % 60}m" },
    prayerTransitNote = "Solat Zohor & Transit SRA: 01:15 PM – 02:00 PM",

    // School bag summary card
    bagCardPackedTitle = "Beg Sekolah Siap Dikemas! ✨",
    bagCardPendingTitle = "Kemas Beg Sekolah Esok",
    bagCardPackedSubtitle = "Semua buku & kelengkapan telah dimasukkan",
    bagCardPendingSubtitle = "Semak jadual & masukkan buku mengikut subjek",
    bagCardPackedBadge = "Lengkap ✓",
    bagCardPendingBadge = "Kemas ➔",

    // Homework tracker
    addTaskButton = "Tugasan",
    allTasksDone = "Semua Siap! 🎉",
    emptyCompletedTasks = "Belum ada tugasan yang siap lagi. Teruskan!",
    emptyPendingTasks = "Tiada tugasan buat masa ini. Syabas! ⭐",

    // Exams countdown
    examsDetectedCount = { n -> "$n Ujian Dikesan" },
    emptyExams = "Tiada ujian berdaftar. Teruskan usaha!",
    examDaysUnit = "HARI",
    examFriendlyUasa = "Peperiksaan Besar",
    examFriendlyPbd = "Ujian Kelas",
    examFriendlyAssessment = "Ujian",

    // Ongoing session card
    vanFallback = "Van Sekolah",
    weekendNoClass = "Hujung minggu — tiada kelas",
    noMoreClassesToday = "Tiada kelas lagi hari ini",
    slotUpcoming = "seterusnya",
    slotInProgress = "sedang berlangsung",

    // Tomorrow's books
    textbook = "Buku Teks",
    activityBook = "Buku Aktiviti",
    textbookFor = { subject -> "Buku Teks $subject" },
    fallbackBookMalay = "📘 Buku Teks Bahasa Melayu Jilid 1",
    fallbackBookMaths = "📙 Buku Aktiviti Matematik",
    fallbackBookScience = "📗 Buku Latihan Sains",
    fallbackBookEnglish = "📕 Buku Teks English",

    // Routine automation & notifications card
    routineAutomationTitle = "Automasi & Notifikasi Rutin",
    routineAutomationSubtitle = "Jadual harian penggera & peringatan telefon",
    routineActiveBadge = "Aktif",
    transitMessageFallback = "Semak beg transit anak-anak.",
    attireBooksFallback = "Semak jadual",
    transitTestSent = "Notifikasi 07:00 AM telah dihantar ke bar notifikasi!",
    attireTestSent = "Notifikasi 08:30 PM pakaian esok telah dihantar!",
    testTransitButton = "🔔 Uji 07:00 AM\nBeg Transit",
    testAttireButton = "👕 Uji 08:30 PM\nUniform Esok",

    // In-home realtime class widget
    minutesRemaining = { min -> "Baki: ${min}min" },
    classroomFallback = "Bilik Darjah",
    timetableNotSet = "Jadual belum disediakan",
    noClassNow = "Tiada kelas sekarang",
    nextTimetableMissing = "(jadual belum ada)",
    noNextClass = "Tiada kelas seterusnya",

    // Focus timer dialog
    focusSessionFor = { name -> "Sesi ulangkaji berjadual untuk $name" },
    focusMinutesOption = { mins -> "$mins Minit" },
    focusStatusRunning = "SEDANG BERJALAN",
    focusStatusReady = "BERSEDIA",
    claimStarsButton = "Tuntut Bintang!",

    // Add homework dialog
    addHomeworkTitle = "Tambah Tugasan & Kerja Rumah",
    subjectSelectorLabel = "Subjek KPM:",
    taskTitleLabel = "Nama Tugasan / Muka Surat",
    taskTitlePlaceholder = "Contoh: Buku Aktiviti Jilid 1 M/S 30",
    dueDateLabel = "Tarikh Hantar:",
    dueToday = "Hari Ini",
    dueTomorrow = "Esok",
    dueMonday = "Isnin",
    dueThursday = "Khamis",
    photoAttachedTitle = "Gambar Lembaran Kerja Dilampirkan",
    photoCaptureTitle = "Tangkap Gambar Lembaran Kerja",
    photoAttachedFile = "lembaran_kerja_aktiviti.jpg (Disahkan)",
    photoCaptureHint = "Sentuh untuk simulasi kamera tangkapan",
    notesLabel = "Nota Tambahan (Pilihan)",
    notesPlaceholder = "Contoh: Semak bersama Ibu sebelum tidur",
    saveTaskButton = "Simpan Tugasan (+5 ⭐)",

    // School bag pack dialog
    bagSubjectBooksItem = { subject -> "Buku Teks & Aktiviti $subject" },
    bagCategoryStudyBooks = "Buku Pelajaran",
    bagDonePackingButton = "Selesai Mengemas ✓",
    bagSaveAndCloseButton = "Simpan & Tutup"
)

val EnDashboardStrings = DashboardStrings(
    // Takwim (academic calendar) banner
    holidayBannerTitle = { title -> "Holiday: $title" },
    takwimBannerTitle = { term -> "KPM Takwim • $term" },
    takwimSessionFallback = "Session 2026/2027",
    holidayNoSchoolToday = "No school today • Enjoy the holiday!",
    holidayCountdownToday = "Today!",
    holidayCountdownDays = { days -> "in $days days" },
    nextHolidayLabel = { title, countdown -> "Next holiday: $title ($countdown)" },
    schoolGroupBadge = "Group B",

    // Prayer times card
    prayerTimesTitle = { zone -> "Prayer Times • Zone $zone" },
    currentPrayerStateDescription = "Current prayer time",
    prayerCountdown = { m -> if (m < 60) "In $m min" else if (m % 60 == 0) "In ${m / 60} h" else "In ${m / 60}h ${m % 60}m" },
    prayerTransitNote = "Zohor prayer & SRA transit: 01:15 PM – 02:00 PM",

    // School bag summary card
    bagCardPackedTitle = "School Bag Packed! ✨",
    bagCardPendingTitle = "Pack Tomorrow's School Bag",
    bagCardPackedSubtitle = "All books & supplies are packed",
    bagCardPendingSubtitle = "Check the timetable & pack books by subject",
    bagCardPackedBadge = "Complete ✓",
    bagCardPendingBadge = "Pack ➔",

    // Homework tracker
    addTaskButton = "Task",
    allTasksDone = "All Done! 🎉",
    emptyCompletedTasks = "No completed tasks yet. Keep going!",
    emptyPendingTasks = "No tasks right now. Well done! ⭐",

    // Exams countdown
    examsDetectedCount = { n -> "$n Exams Found" },
    emptyExams = "No exams registered. Keep up the effort!",
    examDaysUnit = "DAYS",
    examFriendlyUasa = "Big Exam",
    examFriendlyPbd = "Class Test",
    examFriendlyAssessment = "Test",

    // Ongoing session card
    vanFallback = "School Van",
    weekendNoClass = "Weekend — no classes",
    noMoreClassesToday = "No more classes today",
    slotUpcoming = "next",
    slotInProgress = "in progress",

    // Tomorrow's books
    textbook = "Textbook",
    activityBook = "Activity Book",
    textbookFor = { subject -> "$subject Textbook" },
    fallbackBookMalay = "📘 Bahasa Melayu Textbook Vol. 1",
    fallbackBookMaths = "📙 Mathematics Activity Book",
    fallbackBookScience = "📗 Science Exercise Book",
    fallbackBookEnglish = "📕 English Textbook",

    // Routine automation & notifications card
    routineAutomationTitle = "Routine Automation & Notifications",
    routineAutomationSubtitle = "Daily alarm & phone reminder schedule",
    routineActiveBadge = "Active",
    transitMessageFallback = "Check the children's transit bags.",
    attireBooksFallback = "Check the timetable",
    transitTestSent = "07:00 AM notification sent to the notification bar!",
    attireTestSent = "08:30 PM tomorrow's attire notification sent!",
    testTransitButton = "🔔 Test 07:00 AM\nTransit Bag",
    testAttireButton = "👕 Test 08:30 PM\nTomorrow's Uniform",

    // In-home realtime class widget
    minutesRemaining = { min -> "Left: ${min}min" },
    classroomFallback = "Classroom",
    timetableNotSet = "Timetable not set up yet",
    noClassNow = "No class right now",
    nextTimetableMissing = "(no timetable yet)",
    noNextClass = "No next class",

    // Focus timer dialog
    focusSessionFor = { name -> "Scheduled revision session for $name" },
    focusMinutesOption = { mins -> "$mins Min" },
    focusStatusRunning = "RUNNING",
    focusStatusReady = "READY",
    claimStarsButton = "Claim Stars!",

    // Add homework dialog
    addHomeworkTitle = "Add Task & Homework",
    subjectSelectorLabel = "KPM Subject:",
    taskTitleLabel = "Task Name / Page",
    taskTitlePlaceholder = "e.g. Activity Book Vol. 1 p. 30",
    dueDateLabel = "Due Date:",
    dueToday = "Today",
    dueTomorrow = "Tomorrow",
    dueMonday = "Monday",
    dueThursday = "Thursday",
    photoAttachedTitle = "Worksheet Photo Attached",
    photoCaptureTitle = "Capture Worksheet Photo",
    photoAttachedFile = "activity_worksheet.jpg (Verified)",
    photoCaptureHint = "Tap to simulate a camera capture",
    notesLabel = "Additional Notes (Optional)",
    notesPlaceholder = "e.g. Review with Mum before bed",
    saveTaskButton = "Save Task (+5 ⭐)",

    // School bag pack dialog
    bagSubjectBooksItem = { subject -> "$subject Textbook & Activity Book" },
    bagCategoryStudyBooks = "Study Books",
    bagDonePackingButton = "Done Packing ✓",
    bagSaveAndCloseButton = "Save & Close"
)
