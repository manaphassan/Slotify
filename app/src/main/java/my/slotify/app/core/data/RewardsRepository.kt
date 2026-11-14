package my.slotify.app.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import my.slotify.app.core.database.ExamEntity
import my.slotify.app.core.database.HomeworkTaskEntity
import my.slotify.app.core.database.PbdAssessmentEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.rewards.RewardKeys
import my.slotify.app.core.rewards.StarRewards
import java.time.LocalDate

/** Star rewards: every award goes through an idempotent claim key (see RewardKeys). */
class RewardsRepository(private val dao: SlotifyDao) {

    fun isClaimed(claimKey: String?): Flow<Boolean> =
        if (claimKey == null) flowOf(false) else dao.isClaimed(claimKey)

    fun savingsConversions(studentId: String?): Flow<Int> =
        if (studentId == null) flowOf(0) else dao.countClaimsWithPrefix(studentId, RewardKeys.SAVINGS_PREFIX)

    suspend fun claimDailyChest(studentId: String, today: LocalDate): Boolean =
        StarRewards.claimOnce(dao, studentId, RewardKeys.dailyChest(studentId, today), StarRewards.DAILY_CHEST_STARS)

    suspend fun claimBagPacked(studentId: String, today: LocalDate): Boolean =
        StarRewards.claimOnce(dao, studentId, RewardKeys.bagPacked(studentId, today), StarRewards.BAG_PACKED_STARS)

    suspend fun claimFocusSession(studentId: String, today: LocalDate): Boolean =
        StarRewards.claimOnce(
            dao,
            studentId,
            RewardKeys.focusSession(studentId, today),
            StarRewards.FOCUS_SESSION_STARS
        )

    suspend fun spend(studentId: String, cost: Int): Boolean = StarRewards.spend(dao, studentId, cost)

    suspend fun convertSavings(studentId: String, availableSavingsSen: Long): Boolean =
        StarRewards.convertSavings(dao, studentId, availableSavingsSen)
}

/** Homework, exams and PBD assessments, including their one-time star awards. */
class SchoolworkRepository(private val dao: SlotifyDao) {

    fun homework(studentId: String?): Flow<List<HomeworkTaskEntity>> =
        if (studentId == null) flowOf(emptyList()) else dao.getHomeworkTasksForStudent(studentId)

    fun exams(studentId: String?): Flow<List<ExamEntity>> =
        if (studentId == null) flowOf(emptyList()) else dao.getExamsForStudent(studentId)

    fun assessments(studentId: String?): Flow<List<PbdAssessmentEntity>> =
        if (studentId == null) flowOf(emptyList()) else dao.getAssessmentsForStudent(studentId)

    suspend fun addHomework(task: HomeworkTaskEntity) = dao.insertHomeworkTask(task)

    /** Toggles completion; the task's stars are paid at most once, ever. */
    suspend fun toggleHomework(task: HomeworkTaskEntity) {
        dao.setHomeworkCompleted(task.id, !task.isCompleted)
        if (!task.isCompleted) {
            StarRewards.claimOnce(dao, task.studentId, RewardKeys.homework(task.id), task.rewardStars)
        }
    }

    /** Saves a PBD record. Returns the stars paid (0 if this subject/period was already rewarded). */
    suspend fun recordAssessment(assessment: PbdAssessmentEntity): Int {
        dao.upsertAssessment(assessment)
        val stars = StarRewards.pbdStars(assessment.tahapPenguasaan)
        val key = RewardKeys.pbd(assessment.studentId, assessment.subjectCode, assessment.assessmentPeriod)
        return if (StarRewards.claimOnce(dao, assessment.studentId, key, stars)) stars else 0
    }

    suspend fun deleteAssessment(assessment: PbdAssessmentEntity) = dao.deleteAssessment(assessment)
}
