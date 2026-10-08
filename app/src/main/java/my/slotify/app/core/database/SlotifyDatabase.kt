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
        PbdAssessmentEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SlotifyDatabase : RoomDatabase() {
    abstract fun slotifyDao(): SlotifyDao

    companion object {
        @Volatile
        private var INSTANCE: SlotifyDatabase? = null

        fun getDatabase(context: Context): SlotifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SlotifyDatabase::class.java,
                    "slotify_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
