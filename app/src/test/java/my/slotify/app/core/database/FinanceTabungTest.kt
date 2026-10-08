package my.slotify.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FinanceTabungTest {

    @Test
    fun testCommitmentFeeEntityCreationAndToggle() {
        val fee = CommitmentFeeEntity(
            id = UUID.randomUUID().toString(),
            title = "Yuran Van Sekolah A",
            childTarget = "Adam",
            amountRm = 120.00,
            dueDateLabel = "1hb Setiap Bulan",
            billingMonth = "2026-10",
            isPaid = false,
            categoryEmoji = "🚌"
        )

        assertEquals("Yuran Van Sekolah A", fee.title)
        assertEquals(120.00, fee.amountRm, 0.001)
        assertFalse(fee.isPaid)

        val paidFee = fee.copy(isPaid = true)
        assertTrue(paidFee.isPaid)
    }

    @Test
    fun testDailySavingsCalculation() {
        val budget = 5.00
        val spent = 3.20
        val saved = (budget - spent).coerceAtLeast(0.0)

        assertEquals(1.80, saved, 0.001)

        // Case when spent exceeds budget
        val overSpent = 6.00
        val zeroSaved = (budget - overSpent).coerceAtLeast(0.0)
        assertEquals(0.00, zeroSaved, 0.001)
    }

    @Test
    fun testCumulativeSavingsToStarConversion() {
        // Business logic: Every RM 10 saved can be converted to 10 Star Coins
        fun calculateConvertibleStars(cumulativeSavedRm: Double): Int {
            val multiplesOfTen = (cumulativeSavedRm / 10.0).toInt()
            return multiplesOfTen * 10
        }

        assertEquals(0, calculateConvertibleStars(9.50))
        assertEquals(10, calculateConvertibleStars(10.00))
        assertEquals(10, calculateConvertibleStars(19.90))
        assertEquals(20, calculateConvertibleStars(24.50))
        assertEquals(50, calculateConvertibleStars(55.00))
    }
}
