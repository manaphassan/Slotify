package my.slotify.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SlotifyDao {
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): StudentEntity?

    @Query("SELECT * FROM school_sessions WHERE studentId = :studentId")
    fun getSessionsForStudent(studentId: String): Flow<List<SchoolSessionEntity>>

    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId AND dayOfWeek = :dayOfWeek ORDER BY periodIndex ASC")
    fun getSlotsForDay(sessionId: String, dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM location_nodes WHERE studentId = :studentId")
    fun getLocationNodesForStudent(studentId: String): Flow<List<LocationNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SchoolSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationNodeEntity>)

    @Query("UPDATE students SET starCoinsBalance = starCoinsBalance + :delta WHERE id = :studentId")
    suspend fun updateStarCoins(studentId: String, delta: Int)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int
}
