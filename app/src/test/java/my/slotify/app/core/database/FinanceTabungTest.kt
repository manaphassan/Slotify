package my.slotify.app.core.database

import my.slotify.app.core.finance.FeeRepository
import my.slotify.app.core.profile.FeeTemplate
import my.slotify.app.core.rewards.RewardKeys
import my.slotify.app.core.rewards.StarRewards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceTabungTest {

    @Test
    fun tabungBalance_sumsDailySavings_andIgnoresOverspend() {
        // budget RM 5.00: saved 1.80 + 0 (overspent) + 5.00 (nothing spent)
        assertEquals(680L, StarRewards.tabungBalanceSen(500, listOf(320, 600, 0), conversions = 0))
    }

    @Test
    fun tabungBalance_deductsRm10PerConversion_andNeverGoesNegative() {
        val spent = List(5) { 100L } // 5 days × RM 4 saved = RM 20
        assertEquals(2_000L, StarRewards.tabungBalanceSen(500, spent, 0))
        assertEquals(1_000L, StarRewards.tabungBalanceSen(500, spent, 1))
        assertEquals(0L, StarRewards.tabungBalanceSen(500, spent, 3))
    }

    @Test
    fun tabungBalance_hasNoSyntheticStartingBalance() {
        assertEquals(0L, StarRewards.tabungBalanceSen(500, emptyList(), 0))
    }

    @Test
    fun tabungBalance_repeatedHalfRinggitSteps_areExact() {
        // 7 × RM 0.10 spent against RM 1.00 budget = RM 0.30 saved — exact in sen (0.1 * 7 != 0.7 in Double).
        assertEquals(30L, StarRewards.tabungBalanceSen(100, listOf(7 * 10L), 0))
    }

    @Test
    fun rewardKeys_scopeOneTimeRewards() {
        val d = LocalDate.of(2026, 11, 14)
        assertEquals(RewardKeys.dailyChest("s1", d), RewardKeys.dailyChest("s1", d))
        assertNotEquals(RewardKeys.dailyChest("s1", d), RewardKeys.dailyChest("s1", d.plusDays(1)))
        assertNotEquals(RewardKeys.bagPacked("s1", d), RewardKeys.bagPacked("s2", d))
        assertNotEquals(RewardKeys.savingsConversion("s1", 1), RewardKeys.savingsConversion("s1", 2))
        assertTrue(RewardKeys.savingsConversion("s1", 1).startsWith(RewardKeys.SAVINGS_PREFIX))
        assertEquals(StarRewards.PBD_HIGH_STARS, StarRewards.pbdStars(5))
        assertEquals(StarRewards.PBD_BASE_STARS, StarRewards.pbdStars(4))
    }

    @Test
    fun rollOver_copiesOnlyRecurringFees_asUnpaid_withStableSeries() {
        val sept = listOf(
            fee("van_2026-09", series = "van", recurring = true, paid = true),
            fee("pibg_2026-09", series = "pibg", recurring = false, paid = true)
        )
        val nov = FeeRepository.rollOver(sept, "2026-11")
        assertEquals(1, nov.size)
        val copy = nov.single()
        assertEquals("van_2026-11", copy.id)
        assertEquals("van", copy.seriesId)
        assertEquals("2026-11", copy.billingMonth)
        assertFalse(copy.isPaid)
    }

    @Test
    fun rollOver_legacyRowWithoutSeries_usesIdAsSeries() {
        val copy = FeeRepository.rollOver(listOf(fee("abc-uuid", series = "", recurring = true)), "2026-12").single()
        assertEquals("abc-uuid", copy.seriesId)
        assertEquals("abc-uuid_2026-12", copy.id)
    }

    @Test
    fun fromTemplate_buildsCurrentMonthFee() {
        val f = FeeRepository.fromTemplate(
            FeeTemplate("van", "Van", "Adam", 80.0, "1hb Setiap Bulan", "🚌", true),
            "2026-11"
        )
        assertEquals("van_2026-11", f.id)
        assertEquals(8_000L, f.amountSen)
        assertTrue(f.isRecurring)
        assertFalse(f.isPaid)
    }

    @Test
    fun looksRecurring_detectsMonthlyLabels() {
        assertTrue(FeeRepository.looksRecurring("1hb Setiap Bulan"))
        assertTrue(FeeRepository.looksRecurring("Hujung Bulan"))
        assertFalse(FeeRepository.looksRecurring("15hb September"))
        assertFalse(FeeRepository.looksRecurring("Sekali Bayar"))
    }

    @Test
    fun monthKey_isIsoYearMonth() {
        assertEquals("2026-02", FeeRepository.monthKey(LocalDate.of(2026, 2, 28)))
    }

    private fun fee(id: String, series: String, recurring: Boolean, paid: Boolean = false) = CommitmentFeeEntity(
        id = id, title = "t", childTarget = "c", amountSen = 1_000, dueDateLabel = "x",
        billingMonth = id.substringAfterLast('_', "2026-09"), isPaid = paid,
        seriesId = series, isRecurring = recurring
    )
}
