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
    val dailyCanteenBudget: Double = 5.00
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
