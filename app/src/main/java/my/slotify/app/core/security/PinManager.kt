package my.slotify.app.core.security

import android.content.Context
import java.security.MessageDigest

/**
 * Fix 1.1: PinManager — stores the parent PIN as a SHA-256 hash in private SharedPreferences.
 * The default PIN is "1234" but it is never stored or compared in plain text.
 *
 * Usage:
 *   PinManager.verifyPin(context, enteredPin)   // returns Boolean
 *   PinManager.setPin(context, newPin)           // updates the stored hash
 */
object PinManager {

    private const val PREFS_NAME = "slotify_secure"
    private const val KEY_PIN_HASH = "parent_pin_hash"
    private const val DEFAULT_PIN = "1234"

    /** Returns the stored SHA-256 hash, initialising to the default if absent. */
    fun getStoredHash(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN_HASH, hashPin(DEFAULT_PIN)) ?: hashPin(DEFAULT_PIN)
    }

    /** Returns true if [input] matches the stored PIN hash. */
    fun verifyPin(context: Context, input: String): Boolean =
        hashPin(input) == getStoredHash(context)

    /** Persists a new PIN (stored only as its SHA-256 hash). */
    fun setPin(context: Context, newPin: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_HASH, hashPin(newPin))
            .apply()
    }

    /** Produces a lowercase hex SHA-256 digest of [pin]. */
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
