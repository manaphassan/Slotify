package my.slotify.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import my.slotify.app.core.database.SlotifyDatabase
import my.slotify.app.core.model.AttireGuide
import my.slotify.app.core.profile.FamilyProfileData
import my.slotify.app.core.time.SchoolDay
import java.time.LocalDate

/**
 * Daily routine alarms. Content is built from the database and the active family profile,
 * so nothing family-specific is hard-coded here.
 */
class SlotifyAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EVENING_ATTIRE = "my.slotify.app.ACTION_EVENING_ATTIRE"
        const val ACTION_MORNING_TRANSIT = "my.slotify.app.ACTION_MORNING_TRANSIT"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_EVENING_ATTIRE && action != ACTION_MORNING_TRANSIT) return

        val appContext = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    ACTION_EVENING_ATTIRE -> sendEveningReminder(appContext, LocalDate.now())
                    ACTION_MORNING_TRANSIT -> sendMorningTransit(appContext, LocalDate.now())
                }
            } catch (e: Exception) {
                Log.e("SlotifyAlarmReceiver", "Failed to build routine reminder", e)
            } finally {
                pending.finish()
            }
        }
    }

    /** Evening before a school day: tomorrow's attire and books for every child. */
    private suspend fun sendEveningReminder(context: Context, today: LocalDate) {
        val tomorrow = today.plusDays(1)
        val schoolDay = SchoolDay.of(tomorrow) ?: return // no school tomorrow
        val dao = SlotifyDatabase.getDatabase(context).slotifyDao()
        val students = dao.getAllStudentsOnce()
        if (students.isEmpty()) return

        val books = students.flatMap { dao.getStudentSlotsForDayOnce(it.id, schoolDay) }
            .filter { !it.isRehat }
            .map { it.subjectName }
            .distinct()
            .joinToString(", ")
            .ifBlank { "Semak jadual esok" }

        SlotifyNotificationManager.showAttireReminder(
            context = context,
            childName = students.joinToString(" & ") { it.nickname },
            attire = AttireGuide.forSchoolDay(schoolDay),
            books = books
        )
    }

    /** School-day morning: transit bag reminder from the family profile (if configured). */
    private suspend fun sendMorningTransit(context: Context, today: LocalDate) {
        if (SchoolDay.isWeekend(today)) return
        val message = FamilyProfileData.morningTransitReminder ?: return
        val students = SlotifyDatabase.getDatabase(context).slotifyDao().getAllStudentsOnce()
        if (students.isEmpty()) return
        SlotifyNotificationManager.showTransitAlert(
            context = context,
            childName = students.joinToString(" & ") { it.nickname },
            transitMsg = message
        )
    }
}
