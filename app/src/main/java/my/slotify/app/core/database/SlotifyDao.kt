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

    /** One-shot read for WidgetUpdateWorker (non-Flow, suspends once). */
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    suspend fun getAllStudentsOnce(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): StudentEntity?

    @Query("SELECT * FROM school_sessions WHERE studentId = :studentId")
    fun getSessionsForStudent(studentId: String): Flow<List<SchoolSessionEntity>>

    /** One-shot read for WidgetUpdateWorker. */
    @Query("SELECT * FROM school_sessions WHERE studentId = :studentId")
    suspend fun getSessionsOnce(studentId: String): List<SchoolSessionEntity>

    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId AND dayOfWeek = :dayOfWeek ORDER BY periodIndex ASC")
    fun getSlotsForDay(sessionId: String, dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    /** One-shot read for WidgetUpdateWorker. */
    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId AND dayOfWeek = :dayOfWeek ORDER BY periodIndex ASC")
    suspend fun getSlotsOnce(sessionId: String, dayOfWeek: Int): List<TimetableSlotEntity>

    @Query("SELECT * FROM location_nodes WHERE studentId = :studentId")
    fun getLocationNodesForStudent(studentId: String): Flow<List<LocationNodeEntity>>

    /** One-shot read for WidgetUpdateWorker. */
    @Query("SELECT * FROM location_nodes WHERE studentId = :studentId")
    suspend fun getLocationNodesOnce(studentId: String): List<LocationNodeEntity>

    @Query("SELECT * FROM location_nodes")
    suspend fun getAllLocationsOnce(): List<LocationNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @androidx.room.Update
    suspend fun updateStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SchoolSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlotEntity>)

    @androidx.room.Delete
    suspend fun deleteSlot(slot: TimetableSlotEntity)

    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId ORDER BY dayOfWeek ASC, periodIndex ASC")
    fun getAllSlotsForSession(sessionId: String): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationNodeEntity>)

    @Query("UPDATE students SET starCoinsBalance = starCoinsBalance + :delta WHERE id = :studentId")
    suspend fun updateStarCoins(studentId: String, delta: Int)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    // MyStudyLife Homework & Tasks
    @Query("SELECT * FROM homework_tasks WHERE studentId = :studentId ORDER BY isCompleted ASC")
    fun getHomeworkTasksForStudent(studentId: String): Flow<List<HomeworkTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkTasks(tasks: List<HomeworkTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkTask(task: HomeworkTaskEntity)

    @androidx.room.Update
    suspend fun updateHomeworkTask(task: HomeworkTaskEntity)

    @Query("UPDATE homework_tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun setHomeworkCompleted(taskId: String, isCompleted: Boolean)

    // MyStudyLife Exams & Assessments
    @Query("SELECT * FROM exams WHERE studentId = :studentId ORDER BY daysRemaining ASC")
    fun getExamsForStudent(studentId: String): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    // ── Finance ──────────────────────────────────────────────────────────────

    /** Live stream of all commitment fees for a billing month (e.g. "2026-09"). */
    @Query("SELECT * FROM commitment_fees WHERE billingMonth = :billingMonth ORDER BY isPaid ASC, amountRm DESC")
    fun getFeesForMonth(billingMonth: String): Flow<List<CommitmentFeeEntity>>

    /** Upserts a fee row — used both for initial seeding and toggling isPaid. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommitmentFee(fee: CommitmentFeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommitmentFees(fees: List<CommitmentFeeEntity>)

    /** Get today's spending row for a student (null if none recorded yet). */
    @Query("SELECT * FROM daily_spending WHERE studentId = :studentId AND dateKey = :dateKey LIMIT 1")
    suspend fun getDailySpending(studentId: String, dateKey: String): DailySpendingEntity?

    /** Live stream of today's spending — drives the canteen budget widget. */
    @Query("SELECT * FROM daily_spending WHERE studentId = :studentId AND dateKey = :dateKey LIMIT 1")
    fun getDailySpendingFlow(studentId: String, dateKey: String): Flow<DailySpendingEntity?>

    /** Live stream of all daily spending entries for a student (for Tabung Maya cumulative savings calculation). */
    @Query("SELECT * FROM daily_spending WHERE studentId = :studentId ORDER BY dateKey DESC")
    fun getAllSpendingForStudent(studentId: String): Flow<List<DailySpendingEntity>>

    /** Save/update canteen spending (REPLACE updates existing row). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySpending(entry: DailySpendingEntity)

    /** Delete a commitment fee. */
    @androidx.room.Delete
    suspend fun deleteCommitmentFee(fee: CommitmentFeeEntity)

    // ── Academic & PBD Assessments ───────────────────────────────────────────

    @Query("SELECT * FROM pbd_assessments WHERE studentId = :studentId ORDER BY subjectCode ASC")
    fun getAssessmentsForStudent(studentId: String): Flow<List<PbdAssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<PbdAssessmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssessment(assessment: PbdAssessmentEntity)

    @androidx.room.Delete
    suspend fun deleteAssessment(assessment: PbdAssessmentEntity)
}

