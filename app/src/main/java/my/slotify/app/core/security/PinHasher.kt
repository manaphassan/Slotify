package my.slotify.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Pure, Android-free PIN hashing and lockout policy (unit-testable on the JVM). */
object PinHasher {
    const val ITERATIONS = 60_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison. */
    fun matches(pin: String, salt: ByteArray, expected: ByteArray, iterations: Int = ITERATIONS): Boolean =
        MessageDigest.isEqual(hash(pin, salt, iterations), expected)

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    /** A valid PIN is 4–6 ASCII digits. */
    fun isValidPin(pin: String): Boolean = pin.length in 4..6 && pin.all { it in '0'..'9' }

    /**
     * Lockout applied after the [failedAttempts]-th consecutive failure.
     * Every 3rd failure locks: 30 s, 2 min, 10 min, then 30 min.
     */
    fun lockoutDurationMs(failedAttempts: Int): Long {
        if (failedAttempts <= 0 || failedAttempts % 3 != 0) return 0L
        return when (failedAttempts / 3) {
            1 -> 30_000L
            2 -> 2 * 60_000L
            3 -> 10 * 60_000L
            else -> 30 * 60_000L
        }
    }
}
