package my.slotify.app.core.finance

import my.slotify.app.core.database.CommitmentFeeEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.profile.FeeTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FeeRepository {
    private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun monthKey(date: LocalDate = LocalDate.now()): String = date.format(MONTH_FORMAT)

    fun feeId(seriesId: String, billingMonth: String) = "${seriesId}_$billingMonth"

    /** Due labels that imply a monthly fee. */
    fun looksRecurring(dueDateLabel: String): Boolean =
        dueDateLabel.contains("Setiap Bulan", ignoreCase = true) ||
                dueDateLabel.contains("Hujung Bulan", ignoreCase = true) ||
                dueDateLabel.contains("Monthly", ignoreCase = true)

    /** Copies of [fees] for [billingMonth], unpaid, keeping their series. */
    fun rollOver(fees: List<CommitmentFeeEntity>, billingMonth: String): List<CommitmentFeeEntity> =
        fees.filter { it.isRecurring }.map { fee ->
            val series = fee.seriesId.ifBlank { fee.id }
            fee.copy(id = feeId(series, billingMonth), seriesId = series, billingMonth = billingMonth, isPaid = false)
        }

    fun fromTemplate(t: FeeTemplate, billingMonth: String) = CommitmentFeeEntity(
        id = feeId(t.seriesId, billingMonth),
        title = t.title,
        childTarget = t.childTarget,
        amountRm = t.amountRm,
        dueDateLabel = t.dueDateLabel,
        billingMonth = billingMonth,
        isPaid = false,
        categoryEmoji = t.categoryEmoji,
        seriesId = t.seriesId,
        isRecurring = t.isRecurring
    )

    /**
     * Makes sure [billingMonth] has its recurring fees. When the month has no fees yet,
     * recurring fees from the most recent earlier month are copied in as unpaid.
     * Safe to call repeatedly.
     */
    suspend fun ensureMonth(dao: SlotifyDao, billingMonth: String) {
        if (dao.countFeesForMonth(billingMonth) > 0) return
        val previous = dao.latestFeeMonthBefore(billingMonth) ?: return
        val copies = rollOver(dao.getRecurringFeesForMonth(previous), billingMonth)
        if (copies.isNotEmpty()) dao.seedFees(copies)
    }
}
