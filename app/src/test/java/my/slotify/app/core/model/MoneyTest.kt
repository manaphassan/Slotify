package my.slotify.app.core.model

import my.slotify.app.core.database.CommitmentFeeEntity
import my.slotify.app.feature.finance.FinanceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun format_alwaysTwoDecimals_andHandlesNegatives() {
        assertEquals("RM 0.00", Money.format(0))
        assertEquals("RM 5.00", Money.format(500))
        assertEquals("RM 12.05", Money.format(1205))
        assertEquals("-RM 1.20", Money.format(-120))
        assertEquals("1.50", Money.format(150, prefix = ""))
    }

    @Test
    fun parse_acceptsCommonInputs() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1250L, Money.parse("12.5"))
        assertEquals(1250L, Money.parse("12,50"))
        assertEquals(320L, Money.parse("RM 3.20"))
        assertEquals(5L, Money.parse("0.05"))
    }

    @Test
    fun parse_rejectsInvalidInputs() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1.234"))
        assertNull(Money.parse("-5"))
    }

    @Test
    fun fromRm_roundsToNearestSen() {
        assertEquals(1999L, Money.fromRm(19.99))
        assertEquals(30L, Money.fromRm(0.1 + 0.2)) // 0.30000000000000004
    }

    @Test
    fun financeUiState_derivesTotalsInSen() {
        val fees = listOf(fee("a", 8_000, paid = true), fee("b", 1_050, paid = false))
        val state = FinanceUiState(fees = fees, budgetSen = 500, spentTodaySen = 650, tabungSen = 999)
        assertEquals(9_050L, state.totalFeesSen)
        assertEquals(8_000L, state.paidFeesSen)
        assertEquals(0L, state.savedTodaySen) // overspent
        assertFalse(state.canConvertSavings)
        assertTrue(state.copy(tabungSen = 1_000).canConvertSavings)
    }

    private fun fee(id: String, sen: Long, paid: Boolean) = CommitmentFeeEntity(
        id = id, title = id, childTarget = "c", amountSen = sen, dueDateLabel = "1hb Setiap Bulan",
        billingMonth = "2026-11", isPaid = paid
    )
}
