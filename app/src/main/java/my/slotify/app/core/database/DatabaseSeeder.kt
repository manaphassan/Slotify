package my.slotify.app.core.database

import java.util.UUID

object DatabaseSeeder {

    suspend fun seedInitialData(dao: SlotifyDao) {
        val adamCheck = dao.getStudentById("student-adam-01")
        if (adamCheck != null && adamCheck.myKid.isNotBlank()) {
            seedSessionsIfMissing(dao) // Self-heal sessions on repeat installs
            seedFullWeekSlotsIfMissing(dao) // Extend Mon-only slots to full week
            seedHomeworkAndExamsIfMissing(dao)
            return
        }

        // 1. Adam (Darjah 4)
        val adamId = "student-adam-01"
        val adam = StudentEntity(
            id = adamId,
            fullName = "Adam Bin Ahmad",
            nickname = "Adam",
            currentLevel = "Darjah 4",
            avatarEmoji = "👦",
            primarySchoolName = "SK Taman Contoh",
            secondarySchoolName = "SRA Taman Contoh",
            sessionType = "DUAL_SRA_SK",
            academicYearShift = "YEAR_4_STANDARD",
            vanDriverName = "Pemandu Van A",
            vanDriverPhone = "010-0000000",
            starCoinsBalance = 120,
            dailyCanteenBudget = 5.00,
            myKid = "000000-00-0000",
            heightCm = 135.0,
            weightKg = 32.5,
            age = 10,
            className = "4 A / 4 B"
        )
        dao.insertStudent(adam)

        val sraSessionId = "session-sra-4b"
        val skSessionId = "session-sk-4a"

        val adamSessions = listOf(
            SchoolSessionEntity(
                id = sraSessionId,
                studentId = adamId,
                schoolName = "SRA Taman Contoh",
                className = "4B",
                sessionTimeType = "PAGI",
                startTime = "08:00",
                endTime = "11:00"
            ),
            SchoolSessionEntity(
                id = skSessionId,
                studentId = adamId,
                schoolName = "SK Taman Contoh",
                className = "4A",
                sessionTimeType = "PETANG",
                startTime = "13:00",
                endTime = "18:00"
            )
        )
        dao.insertSessions(adamSessions)

        // Seed sample timetable slots for Adam (Isnin)
        val adamSlots = listOf(
            // SRA Pagi (Isnin)
            TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "08:30", subjectCode = "TAWHID", subjectName = "Tauhid / Aqidah", teacherName = "Ustazah"),
            TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "08:30", endTime = "09:00", subjectCode = "JAWI", subjectName = "Jawi & Khat"),
            TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "09:00", endTime = "09:30", subjectCode = "BA", subjectName = "Bahasa Arab"),
            TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "09:30", endTime = "10:00", subjectCode = "TAHFIZ", subjectName = "Hafazan"),
            TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 5, startTime = "10:00", endTime = "10:20", subjectCode = "REHAT", subjectName = "Waktu Rehat SRA", isRehat = true),
            TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 6, startTime = "10:20", endTime = "11:00", subjectCode = "SIRAH", subjectName = "Sirah Nabawiyyah"),

            // SK Petang (Isnin)
            TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "13:00", endTime = "14:00", subjectCode = "BM", subjectName = "Bahasa Melayu"),
            TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "14:00", endTime = "14:50", subjectCode = "MT", subjectName = "Matematik"),
            TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "14:50", endTime = "15:10", subjectCode = "REHAT", subjectName = "Waktu Rehat SK", isRehat = true),
            TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "15:10", endTime = "16:10", subjectCode = "SN", subjectName = "Sains"),
            TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 5, startTime = "16:10", endTime = "17:10", subjectCode = "BI", subjectName = "Bahasa Inggeris"),
            TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 6, startTime = "17:10", endTime = "18:00", subjectCode = "SEJ", subjectName = "Sejarah")
        )
        dao.insertSlots(adamSlots)

        // 2. Aisyah (Darjah 2 · SRAI)
        val aisyahId = "student-aisyah-02"
        val aisyah = StudentEntity(
            id = aisyahId,
            fullName = "Aisyah Binti Ahmad",
            nickname = "Aisyah",
            currentLevel = "Darjah 2",
            avatarEmoji = "📚",
            primarySchoolName = "SRAI Bukit Contoh",
            secondarySchoolName = null,
            sessionType = "INTEGRATED_SRAISM",
            academicYearShift = "SRAISM_COMBINED",
            vanDriverName = "Van Sekolah B",
            vanDriverPhone = "010-0000000",
            starCoinsBalance = 90,
            dailyCanteenBudget = 4.00,
            myKid = "000000-00-0000",
            heightCm = 122.0,
            weightKg = 24.0,
            age = 8,
            className = "2 Cemerlang"
        )
        dao.insertStudent(aisyah)

        val sraiSessionId = "session-srai-2b"
        val aisyahSession = SchoolSessionEntity(
            id = sraiSessionId,
            studentId = aisyahId,
            schoolName = "SRA Integrasi Bukit Contoh",
            className = "2B",
            sessionTimeType = "FULL_DAY",
            startTime = "07:30",
            endTime = "15:30"
        )
        dao.insertSessions(listOf(aisyahSession))

        val aisyahSlots = listOf(
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "07:30", endTime = "08:30", subjectCode = "BM", subjectName = "Bahasa Melayu"),
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "08:30", endTime = "09:30", subjectCode = "PI", subjectName = "Pendidikan Islam"),
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "09:30", endTime = "10:00", subjectCode = "REHAT", subjectName = "Waktu Rehat 1", isRehat = true),
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "10:00", endTime = "11:30", subjectCode = "MT", subjectName = "Matematik"),
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 5, startTime = "11:30", endTime = "12:30", subjectCode = "BI", subjectName = "Bahasa Inggeris"),
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 6, startTime = "12:30", endTime = "13:30", subjectCode = "REHAT", subjectName = "Makan & Solat Zohor", isRehat = true),
            TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 7, startTime = "13:30", endTime = "15:30", subjectCode = "SN", subjectName = "Sains & Aktiviti")
        )
        dao.insertSlots(aisyahSlots)

        // 3. Danial (Tadika 4Y)
        val danialId = "student-danial-03"
        val danial = StudentEntity(
            id = danialId,
            fullName = "Danial Bin Ahmad",
            nickname = "Danial",
            currentLevel = "Tadika 4Y",
            avatarEmoji = "🧒",
            primarySchoolName = "Tadika Ceria Adik Pintar",
            secondarySchoolName = "PASTI (Tahun Hadapan)",
            sessionType = "TADIKA_DAYCARE",
            academicYearShift = "TADIKA_STANDARD",
            vanDriverName = "Dihantar Abah",
            vanDriverPhone = null,
            starCoinsBalance = 50,
            dailyCanteenBudget = 0.0,
            myKid = "000000-00-0000",
            heightCm = 98.0,
            weightKg = 15.0,
            age = 4,
            className = "Tadika 4 Tahun"
        )
        dao.insertStudent(danial)

        val tadikaSessionId = "session-tadika-danial"
        val danialSession = SchoolSessionEntity(
            id = tadikaSessionId,
            studentId = danialId,
            schoolName = "Tadika Ceria",
            className = "Adik Pintar 4Y",
            sessionTimeType = "PAGI",
            startTime = "08:00",
            endTime = "12:00"
        )
        dao.insertSessions(listOf(danialSession))

        val danialSlots = listOf(
            TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "09:00", subjectCode = "IQRA", subjectName = "Iqra' & Doa"),
            TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "09:00", endTime = "10:00", subjectCode = "BM", subjectName = "Bahasa Melayu Asas"),
            TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "10:00", endTime = "10:30", subjectCode = "REHAT", subjectName = "Minum Pagi", isRehat = true),
            TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "10:30", endTime = "12:00", subjectCode = "AWAL_SAINS", subjectName = "Aktiviti & Seni")
        )
        dao.insertSlots(danialSlots)

        // Seed Geofence Location Nodes
        val locations = listOf(
            LocationNodeEntity(studentId = adamId, name = "Rumah", nodeType = "HOME", latitude = 3.1390, longitude = 101.6869),
            LocationNodeEntity(studentId = adamId, name = "SRA Taman Contoh", nodeType = "SCHOOL", latitude = 3.1390, longitude = 101.6869),
            LocationNodeEntity(studentId = adamId, name = "Taska Ceria (Transit)", nodeType = "TRANSIT", latitude = 3.1390, longitude = 101.6869),
            LocationNodeEntity(studentId = adamId, name = "SK Taman Contoh", nodeType = "SCHOOL", latitude = 3.1390, longitude = 101.6869),
            LocationNodeEntity(studentId = aisyahId, name = "SRAI Bukit Contoh", nodeType = "SCHOOL", latitude = 3.1390, longitude = 101.6869),
            LocationNodeEntity(studentId = danialId, name = "Tadika Ceria Adik Pintar", nodeType = "TADIKA", latitude = 3.1390, longitude = 101.6869)
        )
        dao.insertLocations(locations)

        seedHomeworkAndExamsIfMissing(dao)
    }

    /**
     * Self-healing: Re-seeds school sessions + timetable slots for students
     * that exist in the DB but have no associated sessions. This prevents
     * the slot editor FAB from disappearing on repeat installs / DB migrations.
     */
    suspend fun seedSessionsIfMissing(dao: SlotifyDao) {
        val adamId = "student-adam-01"
        val aisyahId   = "student-aisyah-02"
        val danialId  = "student-danial-03"

        val sraSessionId = "session-sra-4b"
        val skSessionId  = "session-sk-4a"
        val sraiSessionId = "session-srai-2b"
        val tadikaSessionId = "session-tadika-danial"

        // Adam — dual session SRA + SK
        if (dao.getStudentById(adamId) != null) {
            dao.insertSessions(listOf(
                SchoolSessionEntity(
                    id = sraSessionId, studentId = adamId,
                    schoolName = "SRA Taman Contoh", className = "4B",
                    sessionTimeType = "PAGI", startTime = "08:00", endTime = "11:00"
                ),
                SchoolSessionEntity(
                    id = skSessionId, studentId = adamId,
                    schoolName = "SK Taman Contoh", className = "4A",
                    sessionTimeType = "PETANG", startTime = "13:00", endTime = "18:00"
                )
            ))
            dao.insertSlots(listOf(
                TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "08:30", subjectCode = "TAWHID", subjectName = "Tauhid / Aqidah", teacherName = "Ustazah"),
                TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "08:30", endTime = "09:00", subjectCode = "JAWI", subjectName = "Jawi & Khat"),
                TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "09:00", endTime = "09:30", subjectCode = "BA", subjectName = "Bahasa Arab"),
                TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "09:30", endTime = "10:00", subjectCode = "TAHFIZ", subjectName = "Hafazan"),
                TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 5, startTime = "10:00", endTime = "10:20", subjectCode = "REHAT", subjectName = "Waktu Rehat SRA", isRehat = true),
                TimetableSlotEntity(sessionId = sraSessionId, dayOfWeek = 1, periodIndex = 6, startTime = "10:20", endTime = "11:00", subjectCode = "SIRAH", subjectName = "Sirah Nabawiyyah"),
                TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "13:00", endTime = "14:00", subjectCode = "BM", subjectName = "Bahasa Melayu"),
                TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "14:00", endTime = "14:50", subjectCode = "MT", subjectName = "Matematik"),
                TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "14:50", endTime = "15:10", subjectCode = "REHAT", subjectName = "Waktu Rehat SK", isRehat = true),
                TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "15:10", endTime = "16:10", subjectCode = "SN", subjectName = "Sains"),
                TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 5, startTime = "16:10", endTime = "17:10", subjectCode = "BI", subjectName = "Bahasa Inggeris"),
                TimetableSlotEntity(sessionId = skSessionId, dayOfWeek = 1, periodIndex = 6, startTime = "17:10", endTime = "18:00", subjectCode = "SEJ", subjectName = "Sejarah")
            ))
        }

        // Aisyah — integrated SRAI
        if (dao.getStudentById(aisyahId) != null) {
            dao.insertSessions(listOf(
                SchoolSessionEntity(
                    id = sraiSessionId, studentId = aisyahId,
                    schoolName = "SRA Integrasi Bukit Contoh", className = "2B",
                    sessionTimeType = "FULL_DAY", startTime = "07:30", endTime = "15:30"
                )
            ))
            dao.insertSlots(listOf(
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "07:30", endTime = "08:30", subjectCode = "BM", subjectName = "Bahasa Melayu"),
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "08:30", endTime = "09:30", subjectCode = "PI", subjectName = "Pendidikan Islam"),
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "09:30", endTime = "10:00", subjectCode = "REHAT", subjectName = "Waktu Rehat 1", isRehat = true),
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "10:00", endTime = "11:30", subjectCode = "MT", subjectName = "Matematik"),
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 5, startTime = "11:30", endTime = "12:30", subjectCode = "BI", subjectName = "Bahasa Inggeris"),
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 6, startTime = "12:30", endTime = "13:30", subjectCode = "REHAT", subjectName = "Makan & Solat Zohor", isRehat = true),
                TimetableSlotEntity(sessionId = sraiSessionId, dayOfWeek = 1, periodIndex = 7, startTime = "13:30", endTime = "15:30", subjectCode = "SN", subjectName = "Sains & Aktiviti")
            ))
        }

        // Danial — Tadika
        if (dao.getStudentById(danialId) != null) {
            dao.insertSessions(listOf(
                SchoolSessionEntity(
                    id = tadikaSessionId, studentId = danialId,
                    schoolName = "Tadika Ceria", className = "Adik Pintar 4Y",
                    sessionTimeType = "PAGI", startTime = "08:00", endTime = "12:00"
                )
            ))
            dao.insertSlots(listOf(
                TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "09:00", subjectCode = "IQRA", subjectName = "Iqra' & Doa"),
                TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 2, startTime = "09:00", endTime = "10:00", subjectCode = "BM", subjectName = "Bahasa Melayu Asas"),
                TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 3, startTime = "10:00", endTime = "10:30", subjectCode = "REHAT", subjectName = "Minum Pagi", isRehat = true),
                TimetableSlotEntity(sessionId = tadikaSessionId, dayOfWeek = 1, periodIndex = 4, startTime = "10:30", endTime = "12:00", subjectCode = "AWAL_SAINS", subjectName = "Aktiviti & Seni")
            ))
        }
    }

    suspend fun seedHomeworkAndExamsIfMissing(dao: SlotifyDao) {

        val adamId = "student-adam-01"
        val aisyahId = "student-aisyah-02"
        val danialId = "student-danial-03"

        val tasks = listOf(
            // Aisyah Tasks
            HomeworkTaskEntity(
                id = "task-aisyah-1",
                studentId = aisyahId,
                subjectCode = "MT",
                subjectName = "Matematik",
                title = "Buku Aktiviti Jilid 1 M/S 24-26",
                dueDateDescription = "Hari Ini",
                isCompleted = false,
                rewardStars = 5,
                notes = "Selesaikan operasi tambah 2 digit & semak bersama Ibu"
            ),
            HomeworkTaskEntity(
                id = "task-aisyah-2",
                studentId = aisyahId,
                subjectCode = "PI",
                subjectName = "Pendidikan Islam",
                title = "Hafazan Surah Al-Balad (Ayat 1-10)",
                dueDateDescription = "Esok",
                isCompleted = false,
                rewardStars = 10,
                notes = "Tasmik bersama Ustazah waktu pagi"
            ),
            HomeworkTaskEntity(
                id = "task-aisyah-3",
                studentId = aisyahId,
                subjectCode = "BM",
                subjectName = "Bahasa Melayu",
                title = "Salin Karangan 'Keluarga Bahagia'",
                dueDateDescription = "Khamis",
                isCompleted = true,
                rewardStars = 5,
                notes = "Buku Garis Tiga • Tulisan kemas"
            ),

            // Adam Tasks
            HomeworkTaskEntity(
                id = "task-adam-1",
                studentId = adamId,
                subjectCode = "SN",
                subjectName = "Sains",
                title = "Laporan Eksperimen Daya & Geseran",
                dueDateDescription = "Hari Ini",
                isCompleted = false,
                rewardStars = 5,
                notes = "Buku Latihan Sains M/S 42 • Carta palang"
            ),
            HomeworkTaskEntity(
                id = "task-adam-2",
                studentId = adamId,
                subjectCode = "BM",
                subjectName = "Bahasa Melayu",
                title = "Latihan Tatabahasa Kata Ganda Berentak",
                dueDateDescription = "Esok",
                isCompleted = false,
                rewardStars = 5,
                notes = "10 soalan objektif & 5 ayat binaan"
            ),
            HomeworkTaskEntity(
                id = "task-adam-3",
                studentId = adamId,
                subjectCode = "SEJ",
                subjectName = "Sejarah",
                title = "Peta Minda Zaman Air Batu Akhir",
                dueDateDescription = "Jumaat",
                isCompleted = true,
                rewardStars = 5,
                notes = "Lukis dan warnakan garis masa benua"
            ),

            // Danial Tasks
            HomeworkTaskEntity(
                id = "task-danial-1",
                studentId = danialId,
                subjectCode = "IQRA",
                subjectName = "Iqra' & Doa",
                title = "Mewarna Huruf Hijaiyyah Alif - Jim",
                dueDateDescription = "Hari Ini",
                isCompleted = false,
                rewardStars = 5,
                notes = "Buku Lembaran Kerja Adik Pintar"
            ),
            HomeworkTaskEntity(
                id = "task-danial-2",
                studentId = danialId,
                subjectCode = "AWAL_SAINS",
                subjectName = "Aktiviti Seni",
                title = "Tampalan Kolaj Kertas Rama-Rama",
                dueDateDescription = "Esok",
                isCompleted = false,
                rewardStars = 5,
                notes = "Gunting & tampal kertas warna"
            )
        )
        dao.insertHomeworkTasks(tasks)

        val exams = listOf(
            // Aisyah Exams
            ExamEntity(
                id = "exam-aisyah-1",
                studentId = aisyahId,
                subjectCode = "MT",
                subjectName = "Matematik",
                title = "Pentaksiran Bilik Darjah (PBD) Matematik",
                examDate = "28 Sep 2026",
                daysRemaining = 7,
                venue = "Kelas 2B SRAI",
                syllabusNote = "Operasi Tambah, Tolak & Wang RM"
            ),
            ExamEntity(
                id = "exam-aisyah-2",
                studentId = aisyahId,
                subjectCode = "PI",
                subjectName = "Pendidikan Islam",
                title = "Ujian Hafazan Juz Amma SRAI",
                examDate = "02 Okt 2026",
                daysRemaining = 11,
                venue = "Musolla SRAI",
                syllabusNote = "Surah Asy-Syams hingga An-Nas"
            ),

            // Adam Exams
            ExamEntity(
                id = "exam-adam-1",
                studentId = adamId,
                subjectCode = "SN",
                subjectName = "Sains",
                title = "Ujian Akhir Sesi Akademik (UASA) Sains",
                examDate = "25 Sep 2026",
                daysRemaining = 4,
                venue = "Makmal Sains SK",
                syllabusNote = "Unit 1 - Unit 4: Kemahiran Saintifik, Manusia, Haiwan"
            ),
            ExamEntity(
                id = "exam-adam-2",
                studentId = adamId,
                subjectCode = "TAWHID",
                subjectName = "Tauhid / Aqidah",
                title = "Ujian Pertengahan Tahun SRA",
                examDate = "30 Sep 2026",
                daysRemaining = 9,
                venue = "Bilik Darjah 4B",
                syllabusNote = "Rukun Iman, Rukun Islam & Sifat 20"
            ),

            // Danial Assessments
            ExamEntity(
                id = "exam-danial-1",
                studentId = danialId,
                subjectCode = "AWAL_SAINS",
                subjectName = "Awal Sains & Numerasi",
                title = "Minggu Penilaian Perkembangan Adik Pintar",
                examDate = "05 Okt 2026",
                daysRemaining = 14,
                venue = "Tadika Ceria",
                syllabusNote = "Mengecam Warna, Bentuk & Angka 1-10"
            )
        )
        dao.insertExams(exams)
        seedCommitmentFeesIfMissing(dao)
    }

    /**
     * Seeds Selasa–Jumaat (dayOfWeek 2–5) timetable slots for all sessions.
     * Only seeds if slot count for that day is 0 (idempotent via REPLACE).
     * Malaysian school week: Isnin=1, Selasa=2, Rabu=3, Khamis=4, Jumaat=5
     */
    suspend fun seedFullWeekSlotsIfMissing(dao: SlotifyDao) {
        val sraId = "session-sra-4b"
        val skId  = "session-sk-4a"
        val sraiId = "session-srai-2b"
        val tadikaId = "session-tadika-danial"

        // Helper to build a slot list for a given day — subject rotation
        fun sraDay(day: Int): List<TimetableSlotEntity> = listOf(
            TimetableSlotEntity(sessionId = sraId, dayOfWeek = day, periodIndex = 1, startTime = "08:00", endTime = "08:30",
                subjectCode = if (day == 5) "TAHFIZ" else "TAWHID", subjectName = if (day == 5) "Hafazan" else "Tauhid / Aqidah"),
            TimetableSlotEntity(sessionId = sraId, dayOfWeek = day, periodIndex = 2, startTime = "08:30", endTime = "09:00",
                subjectCode = if (day % 2 == 0) "BA" else "JAWI", subjectName = if (day % 2 == 0) "Bahasa Arab" else "Jawi & Khat"),
            TimetableSlotEntity(sessionId = sraId, dayOfWeek = day, periodIndex = 3, startTime = "09:00", endTime = "09:30",
                subjectCode = "FIQH", subjectName = "Fiqah & Amali"),
            TimetableSlotEntity(sessionId = sraId, dayOfWeek = day, periodIndex = 4, startTime = "09:30", endTime = "10:00",
                subjectCode = if (day == 4) "PI" else "TAHFIZ", subjectName = if (day == 4) "Pendidikan Islam" else "Hafazan"),
            TimetableSlotEntity(sessionId = sraId, dayOfWeek = day, periodIndex = 5, startTime = "10:00", endTime = "10:20",
                subjectCode = "REHAT", subjectName = "Waktu Rehat SRA", isRehat = true),
            TimetableSlotEntity(sessionId = sraId, dayOfWeek = day, periodIndex = 6, startTime = "10:20", endTime = "11:00",
                subjectCode = if (day == 5) "QURAN" else "SIRAH", subjectName = if (day == 5) "Tilawah Al-Quran" else "Sirah Nabawiyyah")
        )

        fun skDay(day: Int): List<TimetableSlotEntity> = listOf(
            TimetableSlotEntity(sessionId = skId, dayOfWeek = day, periodIndex = 1, startTime = "13:00", endTime = "14:00",
                subjectCode = if (day == 3) "MT" else "BM", subjectName = if (day == 3) "Matematik" else "Bahasa Melayu"),
            TimetableSlotEntity(sessionId = skId, dayOfWeek = day, periodIndex = 2, startTime = "14:00", endTime = "14:50",
                subjectCode = if (day == 2) "SN" else if (day == 4) "BI" else "MT", subjectName = if (day == 2) "Sains" else if (day == 4) "Bahasa Inggeris" else "Matematik"),
            TimetableSlotEntity(sessionId = skId, dayOfWeek = day, periodIndex = 3, startTime = "14:50", endTime = "15:10",
                subjectCode = "REHAT", subjectName = "Waktu Rehat SK", isRehat = true),
            TimetableSlotEntity(sessionId = skId, dayOfWeek = day, periodIndex = 4, startTime = "15:10", endTime = "16:10",
                subjectCode = if (day == 5) "PJPK" else if (day == 3) "SN" else "SEJ", subjectName = if (day == 5) "PJ & Kesihatan" else if (day == 3) "Sains" else "Sejarah"),
            TimetableSlotEntity(sessionId = skId, dayOfWeek = day, periodIndex = 5, startTime = "16:10", endTime = "17:10",
                subjectCode = if (day == 5) "PSV" else "BI", subjectName = if (day == 5) "Seni Visual" else "Bahasa Inggeris"),
            TimetableSlotEntity(sessionId = skId, dayOfWeek = day, periodIndex = 6, startTime = "17:10", endTime = "18:00",
                subjectCode = if (day == 5) "KH" else "SEJ", subjectName = if (day == 5) "Kemahiran Hidup" else "Sejarah")
        )

        fun sraiDay(day: Int): List<TimetableSlotEntity> = listOf(
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 1, startTime = "07:30", endTime = "08:30",
                subjectCode = if (day % 2 == 0) "MT" else "BM", subjectName = if (day % 2 == 0) "Matematik" else "Bahasa Melayu"),
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 2, startTime = "08:30", endTime = "09:30",
                subjectCode = if (day == 5) "QURAN" else "PI", subjectName = if (day == 5) "Tilawah Al-Quran" else "Pendidikan Islam"),
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 3, startTime = "09:30", endTime = "10:00",
                subjectCode = "REHAT", subjectName = "Waktu Rehat 1", isRehat = true),
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 4, startTime = "10:00", endTime = "11:30",
                subjectCode = if (day == 5) "PSV" else "BI", subjectName = if (day == 5) "Seni & Aktiviti Kreatif" else "Bahasa Inggeris"),
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 5, startTime = "11:30", endTime = "12:30",
                subjectCode = if (day % 2 == 0) "BM" else "MT", subjectName = if (day % 2 == 0) "Bahasa Melayu" else "Matematik"),
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 6, startTime = "12:30", endTime = "13:30",
                subjectCode = "REHAT", subjectName = "Makan & Solat Zohor", isRehat = true),
            TimetableSlotEntity(sessionId = sraiId, dayOfWeek = day, periodIndex = 7, startTime = "13:30", endTime = "15:30",
                subjectCode = "SN", subjectName = "Sains & Amali")
        )

        fun tadikaDay(day: Int): List<TimetableSlotEntity> = listOf(
            TimetableSlotEntity(sessionId = tadikaId, dayOfWeek = day, periodIndex = 1, startTime = "08:00", endTime = "09:00",
                subjectCode = if (day == 5) "IQRA" else "BM", subjectName = if (day == 5) "Iqra' & Doa Pagi" else "Bahasa Melayu Asas"),
            TimetableSlotEntity(sessionId = tadikaId, dayOfWeek = day, periodIndex = 2, startTime = "09:00", endTime = "10:00",
                subjectCode = if (day % 2 == 0) "IQRA" else "AWAL_SAINS", subjectName = if (day % 2 == 0) "Iqra' & Hafazan" else "Awal Sains & Alam Sekitar"),
            TimetableSlotEntity(sessionId = tadikaId, dayOfWeek = day, periodIndex = 3, startTime = "10:00", endTime = "10:30",
                subjectCode = "REHAT", subjectName = "Minum Pagi", isRehat = true),
            TimetableSlotEntity(sessionId = tadikaId, dayOfWeek = day, periodIndex = 4, startTime = "10:30", endTime = "12:00",
                subjectCode = if (day == 5) "PJPK" else "SENI", subjectName = if (day == 5) "Senam Pagi & Permainan" else "Seni Kreatif & Mewarna")
        )

        // Seed Selasa (2) through Jumaat (5) for each session
        val weekSlots = mutableListOf<TimetableSlotEntity>()
        for (day in 2..5) {
            weekSlots += sraDay(day)
            weekSlots += skDay(day)
            weekSlots += sraiDay(day)
            if (day in 2..5) weekSlots += tadikaDay(day)
        }
        dao.insertSlots(weekSlots)
    }

    /**
     * Seeds commitment fees for the current billing month.
     * Uses "2026-09" format — adjust to current month on real devices.
     * REPLACE strategy means re-seeding won't wipe the parent's isPaid marks.
     */
    suspend fun seedCommitmentFeesIfMissing(dao: SlotifyDao) {
        val billingMonth = "2026-09"
        val fees = listOf(
            CommitmentFeeEntity(
                id = "van_vana_$billingMonth",
                title = "Yuran Van Sekolah A",
                childTarget = "Adam",
                amountRm = 80.00,
                dueDateLabel = "1hb Setiap Bulan",
                billingMonth = billingMonth,
                categoryEmoji = "🚌"
            ),
            CommitmentFeeEntity(
                id = "pibg_sk_$billingMonth",
                title = "Yuran PIBG SK Taman Contoh",
                childTarget = "Adam",
                amountRm = 10.00,
                dueDateLabel = "15hb September",
                billingMonth = billingMonth,
                categoryEmoji = "🏫"
            ),
            CommitmentFeeEntity(
                id = "transit_srai_$billingMonth",
                title = "Yuran Transit SRA Integrasi",
                childTarget = "Aisyah",
                amountRm = 60.00,
                dueDateLabel = "1hb Setiap Bulan",
                billingMonth = billingMonth,
                categoryEmoji = "🚌"
            ),
            CommitmentFeeEntity(
                id = "pibg_srai_$billingMonth",
                title = "Yuran PIBG SRA Bukit Contoh",
                childTarget = "Aisyah",
                amountRm = 10.00,
                dueDateLabel = "30hb September",
                billingMonth = billingMonth,
                categoryEmoji = "📋"
            ),
            CommitmentFeeEntity(
                id = "tadika_danial_$billingMonth",
                title = "Yuran Tadika Ceria",
                childTarget = "Danial",
                amountRm = 100.00,
                dueDateLabel = "5hb Setiap Bulan",
                billingMonth = billingMonth,
                categoryEmoji = "🏫"
            ),
            CommitmentFeeEntity(
                id = "pibg_sra_$billingMonth",
                title = "Derma SRA Taman Contoh",
                childTarget = "Adam",
                amountRm = 5.00,
                dueDateLabel = "20hb September",
                billingMonth = billingMonth,
                categoryEmoji = "💝"
            )
        )
        dao.upsertCommitmentFees(fees)
    }
}


