package my.slotify.app.core.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Money is stored as whole sen (`Long`, 1 RM = 100 sen) so repeated +/- RM0.50 adjustments
 * and sums never accumulate floating-point error.
 */
object Money {
    const val SEN_PER_RM = 100L

    fun fromRm(rm: Double): Long = (rm * SEN_PER_RM).roundToLong()

    fun toRm(sen: Long): Double = sen / SEN_PER_RM.toDouble()

    /** "RM 12.50" (negative values as "-RM 1.20"). */
    fun format(sen: Long, prefix: String = "RM "): String {
        val sign = if (sen < 0) "-" else ""
        val a = abs(sen)
        return String.format(Locale.US, "%s%s%d.%02d", sign, prefix, a / SEN_PER_RM, a % SEN_PER_RM)
    }

    /** Parses user input such as "12", "12.5", "12,50" or "RM 3.20". Returns null if invalid. */
    fun parse(text: String): Long? {
        val cleaned = text.trim().removePrefix("RM").trim().replace(',', '.')
        if (cleaned.isEmpty() || !cleaned.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) return null
        val parts = cleaned.split('.')
        val whole = parts[0].toLongOrNull() ?: return null
        val frac = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2).toLong()
        return whole * SEN_PER_RM + frac
    }
}
