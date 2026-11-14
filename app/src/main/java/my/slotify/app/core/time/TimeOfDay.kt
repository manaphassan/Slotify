package my.slotify.app.core.time

/** Helpers for "HH:mm" strings as stored in the timetable (lexicographic order == time order). */
object TimeOfDay {
    private val PATTERN = Regex("^(\\d{1,2})[:.](\\d{2})$")

    /** Normalises "8:05" / "08.05" to "08:05"; returns null if not a valid 24-hour time. */
    fun normalize(input: String): String? {
        val m = PATTERN.matchEntire(input.trim()) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h !in 0..23 || min !in 0..59) return null
        return String.format(java.util.Locale.US, "%02d:%02d", h, min)
    }

    /** True if both are valid and [start] is strictly before [end]. */
    fun isValidRange(start: String, end: String): Boolean {
        val s = normalize(start) ?: return false
        val e = normalize(end) ?: return false
        return s < e
    }

    fun toMinutes(hhmm: String): Int? =
        normalize(hhmm)?.let { it.substring(0, 2).toInt() * 60 + it.substring(3).toInt() }
}
