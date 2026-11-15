package my.slotify.app.core.time

import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.SlotStatus

/** Minutes since midnight for "HH:mm", or null when malformed. */
fun clockMinutes(hhmm: String): Int? {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

/** A slot's state on the day being viewed. [nowMinutes] is null for any day other than today. */
data class SlotState(val status: SlotStatus, val minutesUntilStart: Int? = null, val minutesLeft: Int? = null)

object SlotStates {
    /**
     * Today: finished → COMPLETED, running → NOW, the first one after now → NEXT (with a
     * countdown), later ones → UPCOMING; breaks stay BREAK unless finished. Other days: UPCOMING / BREAK.
     */
    fun forDay(slots: List<TimetableSlotEntity>, nowMinutes: Int?): List<Pair<TimetableSlotEntity, SlotState>> {
        var nextAssigned = false
        return slots.sortedBy { clockMinutes(it.startTime) ?: 0 }.map { slot ->
            val start = clockMinutes(slot.startTime) ?: 0
            val end = clockMinutes(slot.endTime) ?: start
            val state = when {
                nowMinutes == null -> SlotState(if (slot.isRehat) SlotStatus.BREAK else SlotStatus.UPCOMING)
                end <= nowMinutes -> SlotState(SlotStatus.COMPLETED)
                start <= nowMinutes -> SlotState(
                    if (slot.isRehat) SlotStatus.BREAK else SlotStatus.NOW,
                    minutesLeft = end - nowMinutes
                )

                slot.isRehat -> SlotState(SlotStatus.BREAK, minutesUntilStart = start - nowMinutes)
                !nextAssigned -> {
                    nextAssigned = true; SlotState(SlotStatus.NEXT, minutesUntilStart = start - nowMinutes)
                }

                else -> SlotState(SlotStatus.UPCOMING, minutesUntilStart = start - nowMinutes)
            }
            slot to state
        }
    }
}
