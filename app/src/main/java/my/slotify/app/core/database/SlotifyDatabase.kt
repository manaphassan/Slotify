package my.slotify.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudentEntity::class,
        SchoolSessionEntity::class,
        TimetableSlotEntity::class,
        LocationNodeEntity::class,
        HomeworkTaskEntity::class,
        ExamEntity::class,
        CommitmentFeeEntity::class,
        DailySpendingEntity::class,
        PbdAssessmentEntity::class,
        RewardClaimEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class SlotifyDatabase : RoomDatabase() {
    abstract fun slotifyDao(): SlotifyDao

    companion object {
        const val DATABASE_NAME = "slotify_database.db"

        @Volatile
        private var INSTANCE: SlotifyDatabase? = null

        fun getDatabase(context: Context): SlotifyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SlotifyDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*SlotifyMigrations.ALL)
                    // Versions 1–4 were pre-release builds without exported schemas.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
