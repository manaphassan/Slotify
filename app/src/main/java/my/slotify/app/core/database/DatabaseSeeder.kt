package my.slotify.app.core.database

import java.util.UUID

object DatabaseSeeder {

    suspend fun seedInitialData(dao: SlotifyDao) {
        if (dao.getStudentCount() > 0) return // Already seeded

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
            dailyCanteenBudget = 5.00
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
            dailyCanteenBudget = 4.00
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
            dailyCanteenBudget = 0.0
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
    }
}
