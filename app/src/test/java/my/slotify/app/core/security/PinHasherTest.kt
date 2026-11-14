package my.slotify.app.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    private val fastIterations = 1_000 // keep unit tests quick; production uses PinHasher.ITERATIONS

    @Test
    fun matches_correctPin_only() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt, fastIterations)
        assertTrue(PinHasher.matches("4821", salt, hash, fastIterations))
        assertFalse(PinHasher.matches("4822", salt, hash, fastIterations))
        assertFalse(PinHasher.matches("482", salt, hash, fastIterations))
    }

    @Test
    fun samePin_differentSalt_differentHash() {
        val a = PinHasher.hash("1234", PinHasher.newSalt(), fastIterations)
        val b = PinHasher.hash("1234", PinHasher.newSalt(), fastIterations)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun encodeDecode_roundTrips() {
        val bytes = PinHasher.newSalt()
        assertArrayEquals(bytes, PinHasher.decode(PinHasher.encode(bytes)))
    }

    @Test
    fun isValidPin_requires4to6AsciiDigits() {
        assertTrue(PinHasher.isValidPin("0000"))
        assertTrue(PinHasher.isValidPin("123456"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("1234567"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertFalse(PinHasher.isValidPin("\u0661\u0662\u0663\u0664")) // Arabic-Indic digits
    }

    @Test
    fun lockout_escalatesEveryThirdFailure() {
        assertEquals(0L, PinHasher.lockoutDurationMs(1))
        assertEquals(0L, PinHasher.lockoutDurationMs(2))
        assertEquals(30_000L, PinHasher.lockoutDurationMs(3))
        assertEquals(0L, PinHasher.lockoutDurationMs(4))
        assertEquals(120_000L, PinHasher.lockoutDurationMs(6))
        assertEquals(600_000L, PinHasher.lockoutDurationMs(9))
        assertEquals(1_800_000L, PinHasher.lockoutDurationMs(12))
        assertEquals(1_800_000L, PinHasher.lockoutDurationMs(30))
    }
}
