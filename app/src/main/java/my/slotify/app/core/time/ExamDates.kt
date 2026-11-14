package my.slotify.app.core.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Parses human-entered exam dates such as "02 Okt 2026" (Malay) or "02 Oct 2026" (English). */
object ExamDates {
    private val MALAY_MONTHS =
        listOf("jan", "feb", "mac", "apr", "mei", "jun", "jul", "ogo", "sep", "okt", "nov", "dis")

    private val formatters: List<DateTimeFormatter> = listOf(Locale.ENGLISH).map { locale ->
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yyyy").toFormatter(locale)
    }

    fun parse(text: String): LocalDate? {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        for (f in formatters) {
            runCatching { return LocalDate.parse(trimmed, f) }
        }
        // Malay abbreviations are matched explicitly so parsing does not depend on the
        // device's locale data (e.g. "Ogo", "Dis", "Mac").
        val parts = trimmed.split(" ")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val monthIndex = MALAY_MONTHS.indexOf(parts[1].lowercase(Locale.ROOT).take(3))
        val year = parts[2].toIntOrNull() ?: return null
        if (monthIndex < 0) return null
        return runCatching { LocalDate.of(year, monthIndex + 1, day) }.getOrNull()
    }

    /** Whole days from [today] until [examDate] (negative if past), or null if unparseable. */
    fun daysUntil(examDate: String, today: LocalDate = LocalDate.now()): Long? =
        parse(examDate)?.let { ChronoUnit.DAYS.between(today, it) }
}
