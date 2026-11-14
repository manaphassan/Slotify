package my.slotify.app.core.profile

import my.slotify.app.core.database.ExamEntity
import my.slotify.app.core.database.HomeworkTaskEntity
import my.slotify.app.core.database.LocationNodeEntity
import my.slotify.app.core.database.PbdAssessmentEntity
import my.slotify.app.core.database.SchoolSessionEntity
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.feature.dashboard.BagItem
import my.slotify.app.feature.logistics.DriverContact
import my.slotify.app.feature.logistics.PoiCategory
import my.slotify.app.feature.logistics.PoiItem
import my.slotify.app.feature.logistics.RouteSelection

/**
 * DEMO family profile — entirely fictional. Committed to git and used by fresh clones / CI.
 * To use your own family, copy this directory to `app/family-profile/private/` and edit it.
 */
object FamilyProfileData : FamilyProfile {
    override val profileName = "demo"
    override val seedVersion = 1

    private const val CHILD_A = "student-demo-adam"
    private const val CHILD_B = "student-demo-aisyah"
    private const val CHILD_C = "student-demo-danial"

    private const val SRA_A = "session-demo-sra-a"
    private const val SK_A = "session-demo-sk-a"
    private const val SRAI_B = "session-demo-srai-b"
    private const val TADIKA_C = "session-demo-tadika-c"

    override val students = listOf(
        StudentEntity(
            id = CHILD_A, fullName = "Adam Bin Ahmad", nickname = "Adam", currentLevel = "Darjah 4",
            avatarEmoji = "👦", primarySchoolName = "SK Taman Contoh", secondarySchoolName = "SRA Taman Contoh",
            sessionType = "DUAL_SRA_SK", academicYearShift = "YEAR_4_STANDARD",
            vanDriverName = "Van Sekolah A", vanDriverPhone = "+60100000001",
            starCoinsBalance = 50, dailyCanteenBudget = 5.00, age = 10, className = "4 Bestari"
        ),
        StudentEntity(
            id = CHILD_B, fullName = "Aisyah Binti Ahmad", nickname = "Aisyah", currentLevel = "Darjah 2",
            avatarEmoji = "👧", primarySchoolName = "SRAI Bukit Contoh", secondarySchoolName = null,
            sessionType = "INTEGRATED_SRAISM", academicYearShift = "SRAISM_COMBINED",
            vanDriverName = "Van Sekolah B", vanDriverPhone = "+60100000002",
            starCoinsBalance = 40, dailyCanteenBudget = 4.00, age = 8, className = "2 Cemerlang"
        ),
        StudentEntity(
            id = CHILD_C, fullName = "Danial Bin Ahmad", nickname = "Danial", currentLevel = "Tadika 5Y",
            avatarEmoji = "🧒", primarySchoolName = "Tadika Ceria", secondarySchoolName = null,
            sessionType = "TADIKA_DAYCARE", academicYearShift = "TADIKA_STANDARD",
            vanDriverName = "Dihantar Ibu Bapa", vanDriverPhone = null,
            starCoinsBalance = 30, dailyCanteenBudget = 0.0, age = 5, className = "Tadika 5 Tahun"
        )
    )

    override val sessions = listOf(
        SchoolSessionEntity(
            id = SRA_A,
            studentId = CHILD_A,
            schoolName = "SRA Taman Contoh",
            className = "4A",
            sessionTimeType = "PAGI",
            startTime = "08:00",
            endTime = "11:00"
        ),
        SchoolSessionEntity(
            id = SK_A,
            studentId = CHILD_A,
            schoolName = "SK Taman Contoh",
            className = "4 Bestari",
            sessionTimeType = "PETANG",
            startTime = "13:00",
            endTime = "18:00"
        ),
        SchoolSessionEntity(
            id = SRAI_B,
            studentId = CHILD_B,
            schoolName = "SRAI Bukit Contoh",
            className = "2 Cemerlang",
            sessionTimeType = "FULL_DAY",
            startTime = "07:30",
            endTime = "15:30"
        ),
        SchoolSessionEntity(
            id = TADIKA_C,
            studentId = CHILD_C,
            schoolName = "Tadika Ceria",
            className = "Ceria 5Y",
            sessionTimeType = "PAGI",
            startTime = "08:00",
            endTime = "12:00"
        )
    )

    override val icsImports = emptyList<IcsImport>()

    private fun slot(
        session: String,
        day: Int,
        period: Int,
        start: String,
        end: String,
        code: String,
        name: String,
        rehat: Boolean = false
    ) =
        TimetableSlotEntity(
            id = "slot-$session-$day-$period", sessionId = session, dayOfWeek = day, periodIndex = period,
            startTime = start, endTime = end, subjectCode = code, subjectName = name,
            isRehat = rehat, requiresTextbook = !rehat, requiresWorkbook = !rehat
        )

    private fun sraDay(d: Int) = listOf(
        slot(
            SRA_A,
            d,
            1,
            "08:00",
            "08:40",
            if (d % 2 == 0) "BA" else "JAWI",
            if (d % 2 == 0) "Bahasa Arab" else "Jawi & Khat"
        ),
        slot(
            SRA_A,
            d,
            2,
            "08:40",
            "09:20",
            if (d == 5) "QURAN" else "TAWHID",
            if (d == 5) "Tilawah Al-Quran" else "Tauhid / Aqidah"
        ),
        slot(SRA_A, d, 3, "09:20", "09:40", "REHAT", "Waktu Rehat", rehat = true),
        slot(SRA_A, d, 4, "09:40", "10:20", "FIQH", "Fiqah & Amali"),
        slot(SRA_A, d, 5, "10:20", "11:00", "TAHFIZ", "Hafazan")
    )

    private fun skDay(d: Int) = listOf(
        slot(SK_A, d, 1, "13:00", "14:00", if (d == 3) "MT" else "BM", if (d == 3) "Matematik" else "Bahasa Melayu"),
        slot(SK_A, d, 2, "14:00", "15:00", if (d % 2 == 0) "SN" else "MT", if (d % 2 == 0) "Sains" else "Matematik"),
        slot(SK_A, d, 3, "15:00", "15:20", "REHAT", "Waktu Rehat", rehat = true),
        slot(SK_A, d, 4, "15:20", "16:20", "BI", "Bahasa Inggeris"),
        slot(SK_A, d, 5, "16:20", "17:10", if (d == 5) "PJPK" else "SEJ", if (d == 5) "PJ & Kesihatan" else "Sejarah"),
        slot(
            SK_A,
            d,
            6,
            "17:10",
            "18:00",
            if (d == 2) "PSV" else "PI",
            if (d == 2) "Seni Visual" else "Pendidikan Islam"
        )
    )

    private fun sraiDay(d: Int) = listOf(
        slot(
            SRAI_B,
            d,
            1,
            "07:30",
            "08:30",
            if (d % 2 == 0) "MT" else "BM",
            if (d % 2 == 0) "Matematik" else "Bahasa Melayu"
        ),
        slot(SRAI_B, d, 2, "08:30", "09:30", "PI", "Pendidikan Islam"),
        slot(SRAI_B, d, 3, "09:30", "10:00", "REHAT", "Waktu Rehat", rehat = true),
        slot(SRAI_B, d, 4, "10:00", "11:30", "BI", "Bahasa Inggeris"),
        slot(SRAI_B, d, 5, "11:30", "12:30", "SN", "Sains"),
        slot(SRAI_B, d, 6, "12:30", "13:30", "REHAT", "Makan & Solat Zohor", rehat = true),
        slot(
            SRAI_B,
            d,
            7,
            "13:30",
            "15:30",
            if (d == 5) "QURAN" else "BA",
            if (d == 5) "Tilawah Al-Quran" else "Bahasa Arab"
        )
    )

    private fun tadikaDay(d: Int) = listOf(
        slot(TADIKA_C, d, 1, "08:00", "09:00", "IQRA", "Iqra' & Doa"),
        slot(
            TADIKA_C,
            d,
            2,
            "09:00",
            "10:00",
            if (d % 2 == 0) "BM" else "AWAL_SAINS",
            if (d % 2 == 0) "Bahasa Melayu Asas" else "Awal Sains"
        ),
        slot(TADIKA_C, d, 3, "10:00", "10:30", "REHAT", "Minum Pagi", rehat = true),
        slot(TADIKA_C, d, 4, "10:30", "12:00", "SENI", "Seni Kreatif")
    )

    override val slots: List<TimetableSlotEntity> =
        (1..5).flatMap { sraDay(it) + skDay(it) + sraiDay(it) + tadikaDay(it) }

    // Fictional coordinates around a generic town centre.
    private const val HOME_LAT = 2.9300
    private const val HOME_LNG = 101.6900

    override val locationNodes = listOf(
        LocationNodeEntity(
            id = "loc-demo-a-home",
            studentId = CHILD_A,
            name = "Rumah",
            nodeType = "HOME",
            latitude = HOME_LAT,
            longitude = HOME_LNG
        ),
        LocationNodeEntity(
            id = "loc-demo-a-sra",
            studentId = CHILD_A,
            name = "SRA Taman Contoh",
            nodeType = "SCHOOL",
            latitude = 2.9350,
            longitude = 101.6950
        ),
        LocationNodeEntity(
            id = "loc-demo-a-sk",
            studentId = CHILD_A,
            name = "SK Taman Contoh",
            nodeType = "SCHOOL",
            latitude = 2.9365,
            longitude = 101.6990
        ),
        LocationNodeEntity(
            id = "loc-demo-b-srai",
            studentId = CHILD_B,
            name = "SRAI Bukit Contoh",
            nodeType = "SCHOOL",
            latitude = 2.9420,
            longitude = 101.6800
        ),
        LocationNodeEntity(
            id = "loc-demo-c-tadika",
            studentId = CHILD_C,
            name = "Tadika Ceria",
            nodeType = "TADIKA",
            latitude = 2.9280,
            longitude = 101.6980
        )
    )

    override val homework = listOf(
        HomeworkTaskEntity(
            id = "task-demo-a-1",
            studentId = CHILD_A,
            subjectCode = "MT",
            subjectName = "Matematik",
            title = "Latihan Pecahan M/S 12",
            dueDateDescription = "Esok",
            rewardStars = 5
        ),
        HomeworkTaskEntity(
            id = "task-demo-a-2",
            studentId = CHILD_A,
            subjectCode = "BI",
            subjectName = "Bahasa Inggeris",
            title = "Reading Log: 1 short story",
            dueDateDescription = "Khamis",
            rewardStars = 5
        ),
        HomeworkTaskEntity(
            id = "task-demo-b-1",
            studentId = CHILD_B,
            subjectCode = "PI",
            subjectName = "Pendidikan Islam",
            title = "Hafazan Surah Al-Kauthar",
            dueDateDescription = "Esok",
            rewardStars = 10
        ),
        HomeworkTaskEntity(
            id = "task-demo-c-1",
            studentId = CHILD_C,
            subjectCode = "SENI",
            subjectName = "Seni Kreatif",
            title = "Mewarna gambar haiwan",
            dueDateDescription = "Hari Ini",
            rewardStars = 5
        )
    )

    override val exams = listOf(
        ExamEntity(
            id = "exam-demo-a-1",
            studentId = CHILD_A,
            subjectCode = "SN",
            subjectName = "Sains",
            title = "Ujian Akhir Sesi Akademik (UASA) Sains",
            examDate = "04 Dis 2026",
            daysRemaining = 0,
            venue = "Makmal Sains",
            syllabusNote = "Unit 1 - Unit 4"
        ),
        ExamEntity(
            id = "exam-demo-b-1",
            studentId = CHILD_B,
            subjectCode = "MT",
            subjectName = "Matematik",
            title = "PBD Matematik",
            examDate = "27 Nov 2026",
            daysRemaining = 0,
            venue = "Bilik Darjah",
            syllabusNote = "Operasi Tambah & Tolak"
        )
    )

    override val pbdAssessments = listOf(
        PbdAssessmentEntity(
            id = "pbd-demo-a-bm",
            studentId = CHILD_A,
            subjectCode = "BM",
            subjectName = "Bahasa Melayu",
            tahapPenguasaan = 4,
            teacherComment = "Penulisan karangan baik."
        ),
        PbdAssessmentEntity(
            id = "pbd-demo-b-mt",
            studentId = CHILD_B,
            subjectCode = "MT",
            subjectName = "Matematik",
            tahapPenguasaan = 5,
            teacherComment = "Pantas mengira sifir."
        )
    )

    override val feeTemplates = listOf(
        FeeTemplate("demo_van_a", "Yuran Van Sekolah A", "Adam", 80.00, "1hb Setiap Bulan", "🚌", isRecurring = true),
        FeeTemplate("demo_van_b", "Yuran Van Sekolah B", "Aisyah", 70.00, "1hb Setiap Bulan", "🚌", isRecurring = true),
        FeeTemplate(
            "demo_tadika_c",
            "Yuran Tadika Ceria",
            "Danial",
            150.00,
            "5hb Setiap Bulan",
            "🏫",
            isRecurring = true
        )
    )

    // ── Logistics ─────────────────────────────────────────────────────────────
    override val defaultPois = listOf(
        PoiItem("poi-1", "Rumah", PoiCategory.RUMAH, "🏠", HOME_LAT, HOME_LNG, 0.0, "Taman Contoh"),
        PoiItem("poi-2", "SK Taman Contoh", PoiCategory.SEKOLAH, "🏫", 2.9365, 101.6990, 1.2, "Jalan Contoh 1"),
        PoiItem("poi-3", "SRA Taman Contoh", PoiCategory.SEKOLAH, "🕌", 2.9350, 101.6950, 0.8, "Jalan Contoh 2"),
        PoiItem("poi-4", "Taska Ceria (Transit)", PoiCategory.TRANSIT, "🎒", 2.9310, 101.6940, 0.5, "Jalan Contoh 3"),
        PoiItem("poi-5", "Tadika Ceria", PoiCategory.TRANSIT, "🧒", 2.9280, 101.6980, 0.9, "Jalan Contoh 4"),
        PoiItem("poi-6", "SRAI Bukit Contoh", PoiCategory.SEKOLAH, "📚", 2.9420, 101.6800, 1.7, "Jalan Bukit Contoh"),
        PoiItem("poi-7", "Pejabat", PoiCategory.PEJABAT, "🏢", 2.9150, 101.6700, 2.8, "Pusat Bandar Contoh")
    )

    override val mapCenter = GeoCoordinate(HOME_LAT, HOME_LNG)

    override val addressLandmarks = defaultPois.map { AddressLandmark(it.lat, it.lng, "${it.name}, ${it.address}") }

    override val routes = mapOf(
        RouteSelection.PARENT_PICKUP to RouteProfile(
            tabTitle = "Laluan Ibu Bapa", vehicleLabel = "Kereta Keluarga", vehicleEmoji = "🚗",
            passengerLabel = "Danial & Aisyah",
            waypointPoiIds = listOf("poi-7", "poi-5", "poi-4", "poi-1"),
            driver = DriverContact(
                "Ibu Bapa",
                "Kereta Keluarga",
                "+60100000003",
                "🚗",
                "Salam, anak-anak sudah bersedia untuk diambil."
            ),
            steps = listOf(
                RouteStep("Pejabat ➔ Tadika", "Ambil Danial"),
                RouteStep("Tadika ➔ Taska", "Ambil Aisyah & beg transit"),
                RouteStep("Taska ➔ Rumah", "Pulang ke rumah")
            )
        ),
        RouteSelection.VAN_PRIMARY to RouteProfile(
            tabTitle = "Van A", vehicleLabel = "Van Sekolah A", vehicleEmoji = "🚐",
            passengerLabel = "Adam",
            waypointPoiIds = listOf("poi-1", "poi-3", "poi-4", "poi-2", "poi-1"),
            driver = DriverContact(
                "Pemandu Van A",
                "Van SK & SRA Taman Contoh",
                "+60100000001",
                "🚐",
                "Salam, Adam sudah bersedia di pintu pagar."
            ),
            steps = listOf(
                RouteStep("Rumah ➔ SRA", "Sesi pagi SRA"),
                RouteStep("SRA ➔ Taska", "Transit tengah hari"),
                RouteStep("Taska ➔ SK", "Sesi petang SK"),
                RouteStep("SK ➔ Rumah", "Hantar pulang petang")
            )
        ),
        RouteSelection.VAN_SECONDARY to RouteProfile(
            tabTitle = "Van B", vehicleLabel = "Van Sekolah B", vehicleEmoji = "🚐",
            passengerLabel = "Aisyah",
            waypointPoiIds = listOf("poi-1", "poi-6", "poi-4", "poi-1"),
            driver = DriverContact(
                "Pemandu Van B",
                "Van SRAI Bukit Contoh",
                "+60100000002",
                "🚐",
                "Salam, Aisyah sudah bersedia. Terima kasih."
            ),
            steps = listOf(
                RouteStep("Rumah ➔ SRAI", "Sesi penuh"),
                RouteStep("SRAI ➔ Taska", "Selepas sekolah"),
                RouteStep("Taska ➔ Rumah", "Hantar pulang")
            )
        )
    )

    // ── Routines ──────────────────────────────────────────────────────────────
    private fun hm(h: Int, m: Int) = h * 60 + m
    private const val END_OF_DAY = 24 * 60

    override val routineZones = mapOf(
        CHILD_A to listOf(
            RoutineZone(hm(7, 30), "Zon: 🏠 Rumah (Persediaan)", "Rumah"),
            RoutineZone(hm(11, 0), "Zon: 🕌 SRA (Sesi Pagi)", "SRA"),
            RoutineZone(hm(12, 45), "Zon: 🎒 Transit Taska", "Transit"),
            RoutineZone(hm(18, 0), "Zon: 🏫 SK (Sesi Petang)", "SK"),
            RoutineZone(hm(18, 30), "Transit: 🚐 Van Sekolah A", "Van"),
            RoutineZone(END_OF_DAY, "Zon: 🏠 Rumah", "Rumah")
        ),
        CHILD_B to listOf(
            RoutineZone(hm(7, 15), "Zon: 🏠 Rumah (Persediaan)", "Rumah"),
            RoutineZone(hm(15, 30), "Zon: 📚 SRAI (Seharian)", "SRAI"),
            RoutineZone(hm(16, 0), "Transit: 🚐 Van Sekolah B", "Van"),
            RoutineZone(END_OF_DAY, "Zon: 🏠 Rumah", "Rumah")
        ),
        CHILD_C to listOf(
            RoutineZone(hm(7, 45), "Zon: 🏠 Rumah (Persediaan)", "Rumah"),
            RoutineZone(hm(12, 0), "Zon: 🧒 Tadika", "Tadika"),
            RoutineZone(END_OF_DAY, "Zon: 🏠 Rumah", "Rumah")
        )
    )

    override val bagEssentials = mapOf(
        CHILD_B to listOf(BagItem("demo-b-1", "Telekung & Sejadah Kecil", "Solat")),
        CHILD_C to listOf(BagItem("demo-c-1", "Baju Persalinan Tambahan", "Pakaian"))
    )

    override val defaultBagEssentials = listOf(
        BagItem("base-1", "Kotak Pensel & Alat Tulis", "Alat Tulis"),
        BagItem("base-2", "Botol Air Minuman", "Keperluan")
    )

    override val morningTransitReminder: String? = "Hantar beg transit & baju sekolah petang ke taska!"
}
