package my.slotify.app.core.rewards

import my.slotify.app.core.database.RewardClaimEntity
import my.slotify.app.core.database.SlotifyDao
import java.time.LocalDate

/**
 * Claim keys define the scope of a one-time reward. Two claims with the same key
 * can never both pay out. See MASTER_DOCUMENTATION §7.1.
 */
object RewardKeys {
    const val SAVINGS_PREFIX = "savings:"

    fun homework(taskId: String) = "homework:$taskId"
    fun bagPacked(studentId: String, date: LocalDate) = "bag:$studentId:$date"
    fun dailyChest(studentId: String, date: LocalDate) = "chest:$studentId:$date"
    fun focusSession(studentId: String, date: LocalDate) = "focus:$studentId:$date"

    /** [index] is 1-based: the n-th RM 10 conversion for this student. */
    fun savingsConversion(studentId: String, index: Int) = "$SAVINGS_PREFIX$studentId:$index"
    fun pbd(studentId: String, subjectCode: String, period: String) = "pbd:$studentId:$subjectCode:$period"
}

object StarRewards {
    const val BAG_PACKED_STARS = 5
    const val DAILY_CHEST_STARS = 15
    const val FOCUS_SESSION_STARS = 10
    const val SAVINGS_CONVERSION_STARS = 10
    const val SAVINGS_CONVERSION_RM = 10.0
    const val PBD_HIGH_STARS = 15   // TP5 / TP6
    const val PBD_BASE_STARS = 5    // TP1 – TP4

    fun pbdStars(tahapPenguasaan: Int): Int = if (tahapPenguasaan >= 5) PBD_HIGH_STARS else PBD_BASE_STARS

    /** Credits [stars] once for [claimKey]. Returns true if this call paid out. */
    suspend fun claimOnce(dao: SlotifyDao, studentId: String, claimKey: String, stars: Int): Boolean {
        if (stars <= 0) return false
        return dao.claimReward(RewardClaimEntity(claimKey = claimKey, studentId = studentId, stars = stars))
    }

    /** Spends [cost] stars if the balance covers it. Returns true on success. */
    suspend fun spend(dao: SlotifyDao, studentId: String, cost: Int): Boolean {
        if (cost <= 0) return false
        return dao.trySpendStars(studentId, cost) == 1
    }

    /**
     * Converts RM 10 of canteen savings into stars. [availableSavingsRm] must already have
     * previous conversions deducted (see [tabungBalance]).
     */
    suspend fun convertSavings(dao: SlotifyDao, studentId: String, availableSavingsRm: Double): Boolean {
        if (availableSavingsRm + 1e-9 < SAVINGS_CONVERSION_RM) return false
        val next = dao.countClaimsWithPrefixOnce(studentId, RewardKeys.SAVINGS_PREFIX) + 1
        return claimOnce(dao, studentId, RewardKeys.savingsConversion(studentId, next), SAVINGS_CONVERSION_STARS)
    }

    /** Tabung Maya balance: Σ max(0, budget − spent) − RM 10 × conversions. */
    fun tabungBalance(dailyBudgetRm: Double, spentPerDayRm: List<Double>, conversions: Int): Double {
        val saved = spentPerDayRm.sumOf { (dailyBudgetRm - it).coerceAtLeast(0.0) }
        return (saved - conversions * SAVINGS_CONVERSION_RM).coerceAtLeast(0.0)
    }
}
