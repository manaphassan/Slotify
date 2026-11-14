package my.slotify.app.core.time

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Malaysian school-week convention used by [my.slotify.app.core.database.TimetableSlotEntity.dayOfWeek]:
 * 1 = Isnin … 5 = Jumaat. Weekends have no timetable.
 */
object SchoolDay {
    const val MONDAY = 1
    const val FRIDAY = 5

    /** School day index for [date], or null on Saturday/Sunday. */
    fun of(date: LocalDate): Int? = of(date.dayOfWeek)

    fun of(dayOfWeek: DayOfWeek): Int? = when (dayOfWeek) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> null
        else -> dayOfWeek.value // java.time: MONDAY = 1 … FRIDAY = 5
    }

    /** School day index of the next school day strictly after [date] (Friday/Saturday/Sunday → Monday). */
    fun nextAfter(date: LocalDate): Int = of(nextSchoolDate(date))!!

    /** Calendar date of the next school day strictly after [date]. */
    fun nextSchoolDate(date: LocalDate): LocalDate {
        var d = date.plusDays(1)
        while (of(d) == null) d = d.plusDays(1)
        return d
    }

    /** True on Saturday and Sunday. */
    fun isWeekend(date: LocalDate): Boolean = of(date) == null
}
