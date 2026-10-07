package my.slotify.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NordThemeTest {

    @Test
    fun testNordPaletteValues() {
        // Polar Night
        assertEquals(Color(0xFF2E3440), NordColors.Nord0)
        assertEquals(Color(0xFF3B4252), NordColors.Nord1)
        assertEquals(Color(0xFF434C5E), NordColors.Nord2)
        assertEquals(Color(0xFF4C566A), NordColors.Nord3)

        // Snow Storm
        assertEquals(Color(0xFFD8DEE9), NordColors.Nord4)
        assertEquals(Color(0xFFE5E9F0), NordColors.Nord5)
        assertEquals(Color(0xFFECEFF4), NordColors.Nord6)

        // Frost
        assertEquals(Color(0xFF8FBCBB), NordColors.Nord7)
        assertEquals(Color(0xFF88C0D0), NordColors.Nord8)
        assertEquals(Color(0xFF81A1C1), NordColors.Nord9)
        assertEquals(Color(0xFF5E81AC), NordColors.Nord10)

        // Aurora
        assertEquals(Color(0xFFBF616A), NordColors.Nord11)
        assertEquals(Color(0xFFD08770), NordColors.Nord12)
        assertEquals(Color(0xFFEBCB8B), NordColors.Nord13)
        assertEquals(Color(0xFFA3BE8C), NordColors.Nord14)
        assertEquals(Color(0xFFB48EAD), NordColors.Nord15)
    }

    @Test
    fun testAppThemeModeEnum() {
        val modes = AppThemeMode.values()
        assertEquals(3, modes.size)
        assertEquals(AppThemeMode.LIGHT, modes[0])
        assertEquals(AppThemeMode.DARK, modes[1])
        assertEquals(AppThemeMode.NORD, modes[2])
    }

    @Test
    fun testNordSubjectColors() {
        assertEquals(NordColors.Nord9, NordColors.getSubjectColor("BM"))
        assertEquals(NordColors.Nord11, NordColors.getSubjectColor("BI"))
        assertEquals(NordColors.Nord13, NordColors.getSubjectColor("MT"))
        assertEquals(NordColors.Nord14, NordColors.getSubjectColor("SN"))
        assertEquals(NordColors.Nord7, NordColors.getSubjectColor("PI"))
        assertEquals(NordColors.Nord12, NordColors.getSubjectColor("SEJ"))
        assertEquals(NordColors.Nord15, NordColors.getSubjectColor("IQRA"))
        assertEquals(NordColors.Nord10, NordColors.getSubjectColor("RBT"))
    }

    @Test
    fun testSlotifySubjectColorWithNordMode() {
        val bmDefault = SlotifyColors.getSubjectColor("BM", AppThemeMode.LIGHT)
        val bmNord = SlotifyColors.getSubjectColor("BM", AppThemeMode.NORD)

        assertEquals(SlotifyColors.SubjectBm, bmDefault)
        assertEquals(NordColors.Nord9, bmNord)
        assertNotEquals(bmDefault, bmNord)
    }
}
