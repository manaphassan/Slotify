package my.slotify.app.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import my.slotify.app.core.database.CommitmentFeeEntity
import my.slotify.app.core.database.DailySpendingEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.finance.FeeRepository

/** Canteen spending and monthly commitment fees. All amounts are in sen. */
class FinanceRepository(private val dao: SlotifyDao) {

    fun fees(billingMonth: String): Flow<List<CommitmentFeeEntity>> = dao.getFeesForMonth(billingMonth)

    suspend fun ensureMonth(billingMonth: String) = FeeRepository.ensureMonth(dao, billingMonth)

    suspend fun saveFee(fee: CommitmentFeeEntity) = dao.upsertCommitmentFee(fee)

    suspend fun togglePaid(fee: CommitmentFeeEntity) = dao.upsertCommitmentFee(fee.copy(isPaid = !fee.isPaid))

    suspend fun deleteFee(fee: CommitmentFeeEntity) = dao.deleteCommitmentFee(fee)

    fun spending(studentId: String?, dateKey: String): Flow<DailySpendingEntity?> =
        if (studentId == null) flowOf(null) else dao.getDailySpendingFlow(studentId, dateKey)

    fun allSpending(studentId: String?): Flow<List<DailySpendingEntity>> =
        if (studentId == null) flowOf(emptyList()) else dao.getAllSpendingForStudent(studentId)

    suspend fun setSpending(studentId: String, dateKey: String, spentSen: Long) =
        dao.upsertDailySpending(DailySpendingEntity(studentId, dateKey, spentSen.coerceAtLeast(0L)))
}
