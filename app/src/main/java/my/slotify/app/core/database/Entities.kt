package my.slotify.app.core.database

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
    val primarySchoolName: String,      // "SK Taman Contoh"
    val secondarySchoolName: String?,   // "SRA Taman Contoh"
    val sessionType: String,            // "DUAL_SRA_SK", "INTEGRATED_SRAISM", "TADIKA_DAYCARE"
    val academicYearShift: String,      // "YEAR_4_STANDARD", "YEAR_5_INVERTED"
    val vanDriverName: String?,         // "Pemandu Van A", "Van Sekolah B"
    val vanDriverPhone: String?,
    val starCoinsBalance: Int = 50,
    val dailyCanteenBudget: Double = 5.00,
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
    val schoolName: String,             // "SRA Taman Contoh"
    val className: String,              // "4B"
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
    val name: String,                   // "Rumah", "SK", "SRA", "Taska Ceria"
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
    val venue: String,                  // "Dewan Terbuka SK"
    val syllabusNote: String = "Unit 1 - Unit 4"
)

// ── Finance Entities ──────────────────────────────────────────────────────────

/**
 * Monthly recurring commitment fee (Van, Transit, PIBG, Daycare).
 * isPaid persists across app restarts — the whole point of Room here.
 * billingMonth: "2026-09" (yyyy-MM) for monthly scoping.
 */
@Entity(tableName = "commitment_fees")
data class CommitmentFeeEntity(
    @PrimaryKey val id: String,         // e.g. "van_vana", "transit_taska"
    val title: String,                  // "Yuran Van Sekolah A"
    val childTarget: String,            // "Adam", "Adam & Adam"
    val amountRm: Double,               // 80.00
    val dueDateLabel: String,           // "1hb Setiap Bulan"
    val billingMonth: String,           // "2026-09"  (scope resets each month)
    val isPaid: Boolean = false,
    val categoryEmoji: String = "💰"    // "🚌", "🏫", "📋" etc.
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
    val spentAmountRm: Double = 0.0
)


