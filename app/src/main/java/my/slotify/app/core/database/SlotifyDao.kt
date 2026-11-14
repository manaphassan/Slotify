package my.slotify.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Conflict-strategy rules (see MASTER_DOCUMENTATION §9):
 * - Never use REPLACE on parent tables (`students`, `school_sessions`): SQLite implements it
 *   as DELETE + INSERT, which fires ON DELETE CASCADE and wipes child rows. Use @Upsert.
 * - Seeding uses IGNORE so it never overwrites user edits.
 */
@Dao
interface SlotifyDao {
    // ── Students ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    suspend fun getAllStudentsOnce(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): StudentEntity?

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    @Upsert
    suspend fun upsertStudent(student: StudentEntity)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET starCoinsBalance = starCoinsBalance + :delta WHERE id = :studentId")
    suspend fun updateStarCoins(studentId: String, delta: Int)

    /** Deducts [cost] only if the balance covers it. Returns the number of rows updated (0 or 1). */
    @Query("UPDATE students SET starCoinsBalance = starCoinsBalance - :cost WHERE id = :studentId AND starCoinsBalance >= :cost")
    suspend fun trySpendStars(studentId: String, cost: Int): Int

    // ── Sessions & timetable ─────────────────────────────────────────────────
    @Query("SELECT * FROM school_sessions WHERE studentId = :studentId")
    fun getSessionsForStudent(studentId: String): Flow<List<SchoolSessionEntity>>

    @Query("SELECT * FROM school_sessions WHERE studentId = :studentId")
    suspend fun getSessionsOnce(studentId: String): List<SchoolSessionEntity>

    @Upsert
    suspend fun upsertSessions(sessions: List<SchoolSessionEntity>)

    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId AND dayOfWeek = :dayOfWeek ORDER BY startTime ASC, periodIndex ASC")
    fun getSlotsForDay(sessionId: String, dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId AND dayOfWeek = :dayOfWeek ORDER BY startTime ASC, periodIndex ASC")
    suspend fun getSlotsOnce(sessionId: String, dayOfWeek: Int): List<TimetableSlotEntity>

    @Query("SELECT * FROM timetable_slots WHERE sessionId = :sessionId ORDER BY dayOfWeek ASC, startTime ASC, periodIndex ASC")
    fun getAllSlotsForSession(sessionId: String): Flow<List<TimetableSlotEntity>>

    /** All slots of a student's sessions on [dayOfWeek] (used by the bag checklist and reminders). */
    @Query(
        "SELECT t.* FROM timetable_slots t INNER JOIN school_sessions s ON t.sessionId = s.id " +
                "WHERE s.studentId = :studentId AND t.dayOfWeek = :dayOfWeek ORDER BY t.startTime ASC"
    )
    suspend fun getStudentSlotsForDayOnce(studentId: String, dayOfWeek: Int): List<TimetableSlotEntity>

    @Query(
        "SELECT t.* FROM timetable_slots t INNER JOIN school_sessions s ON t.sessionId = s.id " +
                "WHERE s.studentId = :studentId AND t.dayOfWeek = :dayOfWeek ORDER BY t.startTime ASC"
    )
    fun getStudentSlotsForDay(studentId: String, dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlotEntity>)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlotEntity)

    // ── Locations ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM location_nodes WHERE studentId = :studentId")
    fun getLocationNodesForStudent(studentId: String): Flow<List<LocationNodeEntity>>

    @Query("SELECT * FROM location_nodes WHERE studentId = :studentId")
    suspend fun getLocationNodesOnce(studentId: String): List<LocationNodeEntity>

    @Query("SELECT * FROM location_nodes")
    suspend fun getAllLocationsOnce(): List<LocationNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationNodeEntity>)

    // ── Homework & exams ─────────────────────────────────────────────────────
    @Query("SELECT * FROM homework_tasks WHERE studentId = :studentId ORDER BY isCompleted ASC")
    fun getHomeworkTasksForStudent(studentId: String): Flow<List<HomeworkTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkTask(task: HomeworkTaskEntity)

    @Update
    suspend fun updateHomeworkTask(task: HomeworkTaskEntity)

    @Query("UPDATE homework_tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun setHomeworkCompleted(taskId: String, isCompleted: Boolean)

    @Query("SELECT * FROM exams WHERE studentId = :studentId ORDER BY daysRemaining ASC")
    fun getExamsForStudent(studentId: String): Flow<List<ExamEntity>>

    // ── Finance ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM commitment_fees WHERE billingMonth = :billingMonth ORDER BY isPaid ASC, amountRm DESC")
    fun getFeesForMonth(billingMonth: String): Flow<List<CommitmentFeeEntity>>

    @Query("SELECT COUNT(*) FROM commitment_fees WHERE billingMonth = :billingMonth")
    suspend fun countFeesForMonth(billingMonth: String): Int

    /** Most recent billing month strictly before [billingMonth] that has any fee, or null. */
    @Query("SELECT MAX(billingMonth) FROM commitment_fees WHERE billingMonth < :billingMonth")
    suspend fun latestFeeMonthBefore(billingMonth: String): String?

    @Query("SELECT * FROM commitment_fees WHERE billingMonth = :billingMonth AND isRecurring = 1")
    suspend fun getRecurringFeesForMonth(billingMonth: String): List<CommitmentFeeEntity>

    /** Leaf table — REPLACE is safe here and used to toggle isPaid. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommitmentFee(fee: CommitmentFeeEntity)

    @Delete
    suspend fun deleteCommitmentFee(fee: CommitmentFeeEntity)

    @Query("SELECT * FROM daily_spending WHERE studentId = :studentId AND dateKey = :dateKey LIMIT 1")
    suspend fun getDailySpending(studentId: String, dateKey: String): DailySpendingEntity?

    @Query("SELECT * FROM daily_spending WHERE studentId = :studentId AND dateKey = :dateKey LIMIT 1")
    fun getDailySpendingFlow(studentId: String, dateKey: String): Flow<DailySpendingEntity?>

    @Query("SELECT * FROM daily_spending WHERE studentId = :studentId ORDER BY dateKey DESC")
    fun getAllSpendingForStudent(studentId: String): Flow<List<DailySpendingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySpending(entry: DailySpendingEntity)

    // ── PBD assessments ──────────────────────────────────────────────────────
    @Query("SELECT * FROM pbd_assessments WHERE studentId = :studentId ORDER BY subjectCode ASC")
    fun getAssessmentsForStudent(studentId: String): Flow<List<PbdAssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssessment(assessment: PbdAssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: PbdAssessmentEntity)

    // ── Rewards ──────────────────────────────────────────────────────────────
    /** Returns the new rowId, or -1 if [claim] was already recorded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRewardClaim(claim: RewardClaimEntity): Long

    /** Records [claim] and credits its stars atomically. Returns false if already claimed. */
    @Transaction
    suspend fun claimReward(claim: RewardClaimEntity): Boolean {
        if (insertRewardClaim(claim) == -1L) return false
        updateStarCoins(claim.studentId, claim.stars)
        return true
    }

    @Query("SELECT EXISTS(SELECT 1 FROM reward_claims WHERE claimKey = :claimKey)")
    fun isClaimed(claimKey: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM reward_claims WHERE claimKey = :claimKey)")
    suspend fun isClaimedOnce(claimKey: String): Boolean

    /** Number of claims for [studentId] whose key starts with [keyPrefix] (e.g. savings conversions). */
    @Query("SELECT COUNT(*) FROM reward_claims WHERE studentId = :studentId AND claimKey LIKE :keyPrefix || '%'")
    fun countClaimsWithPrefix(studentId: String, keyPrefix: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM reward_claims WHERE studentId = :studentId AND claimKey LIKE :keyPrefix || '%'")
    suspend fun countClaimsWithPrefixOnce(studentId: String, keyPrefix: String): Int

    // ── Seeding (never overwrites existing rows) ─────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedStudents(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedSessions(sessions: List<SchoolSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedSlots(slots: List<TimetableSlotEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedLocations(locations: List<LocationNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedHomework(tasks: List<HomeworkTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedExams(exams: List<ExamEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedAssessments(assessments: List<PbdAssessmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedFees(fees: List<CommitmentFeeEntity>)

    @Query("SELECT COUNT(*) FROM timetable_slots WHERE sessionId = :sessionId")
    suspend fun countSlotsForSession(sessionId: String): Int
}
