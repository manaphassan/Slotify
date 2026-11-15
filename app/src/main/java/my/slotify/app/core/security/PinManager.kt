package my.slotify.app.core.security

import android.content.Context
import androidx.core.content.edit

sealed interface PinResult {
    data object Success : PinResult

    /** No PIN has been created yet — the UI must run the setup flow. */
    data object NotSet : PinResult
    data class Incorrect(val attemptsBeforeLockout: Int) : PinResult
    data class LockedOut(val remainingMs: Long) : PinResult
    data object InvalidFormat : PinResult
    data object AlreadySet : PinResult
}

/**
 * Parent PIN storage.
 *
 * - There is no default PIN: [isPinSet] is false until the parent creates one.
 * - The PIN is stored as PBKDF2-HMAC-SHA256 with a random per-install salt.
 * - Failed attempts and the lockout deadline are persisted, so dismissing the
 *   dialog or killing the app does not reset them.
 *
 * Hashing is slow by design (~tens of ms); call [verifyPin]/[setPin]/[changePin]
 * off the main thread.
 */
object PinManager {

    private const val PREFS_NAME = "slotify_secure"
    private const val KEY_HASH = "parent_pin_pbkdf2"
    private const val KEY_SALT = "parent_pin_salt"
    private const val KEY_ITERATIONS = "parent_pin_iterations"
    private const val KEY_FAILED = "parent_pin_failed_attempts"
    private const val KEY_LOCKED_UNTIL = "parent_pin_locked_until"

    /** Unsalted SHA-256 hash written by v1.x builds; never trusted, only removed. */
    private const val LEGACY_KEY_SHA256 = "parent_pin_hash"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPinSet(context: Context): Boolean = prefs(context).contains(KEY_HASH)

    /** Milliseconds until the lockout ends (0 when not locked). */
    fun lockoutRemainingMs(context: Context, now: Long = System.currentTimeMillis()): Long =
        (prefs(context).getLong(KEY_LOCKED_UNTIL, 0L) - now).coerceAtLeast(0L)

    /** Creates the first PIN. Returns [PinResult.AlreadySet] if one exists (use [changePin]). */
    fun setInitialPin(context: Context, newPin: String): PinResult {
        if (isPinSet(context)) return PinResult.AlreadySet
        return storePin(context, newPin)
    }

    fun verifyPin(context: Context, input: String, now: Long = System.currentTimeMillis()): PinResult {
        val p = prefs(context)
        if (!isPinSet(context)) return PinResult.NotSet
        val remaining = lockoutRemainingMs(context, now)
        if (remaining > 0) return PinResult.LockedOut(remaining)

        val salt = PinHasher.decode(p.getString(KEY_SALT, null) ?: return PinResult.NotSet)
        val expected = PinHasher.decode(p.getString(KEY_HASH, null) ?: return PinResult.NotSet)
        val iterations = p.getInt(KEY_ITERATIONS, PinHasher.ITERATIONS)

        if (PinHasher.matches(input, salt, expected, iterations)) {
            p.edit { putInt(KEY_FAILED, 0); putLong(KEY_LOCKED_UNTIL, 0L) }
            return PinResult.Success
        }

        val failed = p.getInt(KEY_FAILED, 0) + 1
        val lockMs = PinHasher.lockoutDurationMs(failed)
        p.edit {
            putInt(KEY_FAILED, failed)
            putLong(KEY_LOCKED_UNTIL, if (lockMs > 0) now + lockMs else 0L)
        }
        return if (lockMs > 0) PinResult.LockedOut(lockMs) else PinResult.Incorrect(3 - failed % 3)
    }

    /** Replaces the PIN after verifying [currentPin]. */
    fun changePin(context: Context, currentPin: String, newPin: String): PinResult {
        if (!PinHasher.isValidPin(newPin)) return PinResult.InvalidFormat
        return when (val r = verifyPin(context, currentPin)) {
            PinResult.Success -> storePin(context, newPin)
            else -> r
        }
    }

    private fun storePin(context: Context, newPin: String): PinResult {
        if (!PinHasher.isValidPin(newPin)) return PinResult.InvalidFormat
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(newPin, salt)
        prefs(context).edit {
            putString(KEY_SALT, PinHasher.encode(salt))
            putString(KEY_HASH, PinHasher.encode(hash))
            putInt(KEY_ITERATIONS, PinHasher.ITERATIONS)
            putInt(KEY_FAILED, 0)
            putLong(KEY_LOCKED_UNTIL, 0L)
            remove(LEGACY_KEY_SHA256)
        }
        return PinResult.Success
    }
}
