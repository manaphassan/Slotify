package my.slotify.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val nickname: String,
    val currentLevel: String,           // e.g. "Darjah 4", "Darjah 2", "Tadika 4Y"
    val avatarEmoji: String,            // e.g. "👦", "📚", "🧒"
    val primarySchoolName: String,
    val secondarySchoolName: String?,
    val sessionType: String,            // "DUAL_SRA_SK", "INTEGRATED_SRAISM", "TADIKA_DAYCARE"
    val academicYearShift: String,      // "YEAR_4_STANDARD", "YEAR_5_INVERTED"
    val vanDriverName: String?,
    val vanDriverPhone: String?,
    val starCoinsBalance: Int = 50,
    /** Daily canteen allowance in sen (RM 5.00 = 500). */
    val dailyCanteenBudgetSen: Long = 500,
    val myKid: String = "",
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val age: Int = 0,
    val className: String = ""
)

@Entity(
    tableName = "school_sessions",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class SchoolSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val schoolName: String,
    val className: String,
    val sessionTimeType: String,        // "PAGI", "PETANG", "FULL_DAY"
    val startTime: String,              // "08:00"
    val endTime: String,                // "11:00"
    val fridayEndTime: String = "11:30"
)

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = SchoolSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class TimetableSlotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val dayOfWeek: Int,                 // 1=Isnin, 2=Selasa, 3=Rabu, 4=Khamis, 5=Jumaat
    val periodIndex: Int,               // 1, 2, 3..
    val startTime: String,              // "08:00"
    val endTime: String,                // "08:30"
    val subjectCode: String,            // "BM", "MT", "SN", "REHAT"
    val subjectName: String,
    val teacherName: String? = null,
    /** Classroom / venue, e.g. "Bilik 2 Imtiaz" or "Makmal Sains" (v8). */
    val room: String? = null,
    val isRehat: Boolean = false,
    val requiresTextbook: Boolean = true,
    val requiresWorkbook: Boolean = true
)

@Entity(
    tableName = "location_nodes",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class LocationNodeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val name: String,                   // "Rumah", "Sekolah", "Taska"
    val nodeType: String,               // "HOME", "SCHOOL", "TRANSIT", "TADIKA"
    val latitude: Double,
    val longitude: Double,
    val geofenceRadiusMeters: Float = 500f
)

@Entity(
    tableName = "homework_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class HomeworkTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val subjectCode: String,            // "BM", "MT", "SN", "BI", "PI"
    val subjectName: String,            // "Matematik"
    val title: String,                  // "Buku Aktiviti Jilid 1 M/S 24-26"
    val dueDateDescription: String,     // "Hari Ini", "Esok", "Khamis"
    val isCompleted: Boolean = false,
    val rewardStars: Int = 5,
    val notes: String = ""
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class ExamEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val subjectCode: String,            // "SN", "MT", "PI"
    val subjectName: String,            // "Sains"
    val title: String,                  // "Ujian Akhir Sesi Akademik (UASA)"
    val examDate: String,               // "25 Sep 2026"
    val daysRemaining: Int,             // 4
    val venue: String,
    val syllabusNote: String = "Unit 1 - Unit 4"
)

// ── Finance Entities ──────────────────────────────────────────────────────────

/**
 * Monthly commitment fee (Van, Transit, PIBG, Daycare).
 * One row per fee per [billingMonth] ("yyyy-MM"); [seriesId] links the same fee across
 * months. Recurring fees are copied into each new month as unpaid by FeeRepository.
 */
@Entity(tableName = "commitment_fees")
data class CommitmentFeeEntity(
    @PrimaryKey val id: String,         // "<seriesId>_<yyyy-MM>" for recurring fees
    val title: String,
    val childTarget: String,
    val amountSen: Long,                // RM 80.00 = 8000
    val dueDateLabel: String,           // "1hb Setiap Bulan"
    val billingMonth: String,           // "2026-09"
    val isPaid: Boolean = false,
    val categoryEmoji: String = "💰",
    @ColumnInfo(defaultValue = "") val seriesId: String = id,
    @ColumnInfo(defaultValue = "0") val isRecurring: Boolean = false
)

/**
 * Per-student daily canteen spending.
 * dateKey: "2026-09-30" (ISO date string) — one row per student per day.
 * Amount is additive; use REPLACE strategy to update on the same day.
 */
@Entity(
    tableName = "daily_spending",
    primaryKeys = ["studentId", "dateKey"]
)
data class DailySpendingEntity(
    val studentId: String,
    val dateKey: String,                // "2026-09-30"
    val spentSen: Long = 0
)

/**
 * Malaysian KPM Pentaksiran Bilik Darjah (PBD) & Academics.
 * Tracks Tahap Penguasaan (TP 1 to 6) per subject for each student.
 */
@Entity(
    tableName = "pbd_assessments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class PbdAssessmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val subjectCode: String,            // "BM", "MT", "SN", "PI", "BI", "BA"
    val subjectName: String,            // "Bahasa Melayu", "Matematik"
    val tahapPenguasaan: Int,           // 1 to 6 (TP1 - TP6)
    val assessmentPeriod: String = "Sesi 2026/2027", // "Pertengahan Sesi", "Akhir Sesi"
    val teacherComment: String = "",    // "Penguasaan kosa kata dan lisan cemerlang"
    val assessedDate: String = "Okt 2026"
)

/**
 * One row per star award. [claimKey] is unique per reward opportunity (see RewardKeys),
 * which makes every award idempotent.
 */
@Entity(tableName = "reward_claims", indices = [Index("studentId")])
data class RewardClaimEntity(
    @PrimaryKey val claimKey: String,
    val studentId: String,
    val stars: Int,
    val claimedAt: Long = System.currentTimeMillis()
)
