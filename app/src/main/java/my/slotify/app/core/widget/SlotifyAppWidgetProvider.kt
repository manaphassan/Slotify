package my.slotify.app.core.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.*
import my.slotify.app.MainActivity
import my.slotify.app.R
import my.slotify.app.core.data.currentAppLanguage
import my.slotify.app.core.database.SlotifyDatabase
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.designsystem.theme.SystemStrings
import my.slotify.app.core.designsystem.theme.stringsFor
import my.slotify.app.core.model.AttireGuide
import my.slotify.app.core.profile.FamilyProfileData
import my.slotify.app.core.profile.zoneAt
import my.slotify.app.core.takwim.JakimPrayerTimeHelper
import my.slotify.app.core.time.SchoolDay
import my.slotify.app.feature.dashboard.SchoolBagHelper
import my.slotify.app.feature.logistics.GeofenceManager
import my.slotify.app.feature.logistics.GpsLocationHelper
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Home screen widget provider with interactive multi-child toggling.
 *
 * onUpdate() is called by the system on boot, manual add, and periodic refresh.
 * It delegates all Room DB work to [WidgetUpdateWorker] (a coroutine Worker) so
 * this class stays on the main thread — AppWidgetProvider cannot suspend itself.
 *
 * onReceive() intercepts [ACTION_SWITCH_STUDENT] when user taps "⇄ Tukar" or
 * child name to cycle between the family's children directly from the home screen.
 */
class SlotifyAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SWITCH_STUDENT) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentIndex = prefs.getInt(PREF_STUDENT_INDEX, 0)
            prefs.edit().putInt(PREF_STUDENT_INDEX, currentIndex + 1).apply()

            // Trigger immediate background update with newly selected student
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Immediately show a "loading" state while the worker queries Room
        for (appWidgetId in appWidgetIds) {
            showLoadingState(context, appWidgetManager, appWidgetId)
        }

        // Enqueue the DB-backed update
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )

        // Schedule a periodic refresh every 15 min (minimum WorkManager interval)
        val periodic = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
    }

    private fun showLoadingState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val s = cachedStrings(context)
        val views = RemoteViews(context.packageName, R.layout.widget_slotify_class)
        applyStaticLabels(views, s)
        views.setTextViewText(R.id.widget_child_name, s.widgetLoadingChild)
        views.setTextViewText(R.id.widget_zone_text, s.widgetLoadingZone)
        views.setTextViewText(R.id.widget_current_class, s.widgetLoadingSchedule)
        views.setTextViewText(R.id.widget_next_class, "")
        views.setTextViewText(R.id.widget_books, "")
        views.setTextViewText(R.id.widget_siblings_status, "")
        views.setTextViewText(R.id.widget_attire, "")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val WORK_NAME          = "slotify_widget_update_once"
        const val PERIODIC_WORK_NAME = "slotify_widget_update_periodic"
        const val ACTION_SWITCH_STUDENT = "my.slotify.app.ACTION_SWITCH_STUDENT"
        const val PREFS_NAME         = "slotify_widget_prefs"
        const val PREF_STUDENT_INDEX = "key_selected_student_index"
        const val PREF_LANGUAGE      = "key_language"

        /** Re-renders the widget now, e.g. after the in-app language changes. */
        fun requestUpdate(context: Context) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, SlotifyAppWidgetProvider::class.java))
            if (ids.isEmpty()) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            )
        }

        /** Last language the worker rendered with, for the synchronous loading state. */
        internal fun cachedStrings(context: Context): SystemStrings {
            val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREF_LANGUAGE, null)
            val language = name?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.BM
            return stringsFor(language).systemUi
        }

        private fun applyStaticLabels(views: RemoteViews, s: SystemStrings) {
            views.setTextViewText(R.id.widget_switch_child_btn, s.widgetSwitch)
            views.setTextViewText(R.id.widget_zone_btn, s.widgetMap)
            views.setTextViewText(R.id.widget_open_action, s.widgetOpenTimetable)
        }

        /**
         * Builds and pushes a fully-populated RemoteViews to all widget instances.
         * Called by [WidgetUpdateWorker] after querying Room and evaluating GPS on a background thread.
         */
        fun pushUpdate(
            context: Context,
            s: SystemStrings,
            childName: String,
            sessionLabel: String,
            currentClass: String,
            nextClass: String,
            books: String,
            attire: String,
            locationZone: String,
            siblingsSummary: String = ""
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, SlotifyAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)

            val views = RemoteViews(context.packageName, R.layout.widget_slotify_class)
            applyStaticLabels(views, s)
            views.setTextViewText(R.id.widget_child_name, "$childName • $sessionLabel")
            views.setTextViewText(R.id.widget_zone_text, locationZone)
            views.setTextViewText(R.id.widget_current_class, currentClass)
            views.setTextViewText(R.id.widget_next_class, nextClass)
            views.setTextViewText(R.id.widget_books, books)
            views.setTextViewText(R.id.widget_siblings_status, siblingsSummary)
            views.setTextViewText(R.id.widget_attire, attire)

            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            // 1. Tap Root or Timetable: Opens TimetableScreen
            val timetableIntent = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_SCREEN", "TIMETABLE")
            }
            val timetablePendingIntent = PendingIntent.getActivity(context, 0, timetableIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.widget_root, timetablePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_open_action, timetablePendingIntent)

            // 2. Tap Zone Container: Deep-links directly into LogisticsMapScreen
            val logisticsIntent = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_SCREEN", "LOGISTICS")
            }
            val logisticsPendingIntent = PendingIntent.getActivity(context, 1, logisticsIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.widget_zone_container, logisticsPendingIntent)

            // 3. Tap Child Switcher or Name Header: Cycles to next sibling
            val switchIntent = Intent(context, SlotifyAppWidgetProvider::class.java).apply {
                action = ACTION_SWITCH_STUDENT
            }
            val switchPendingIntent = PendingIntent.getBroadcast(context, 2, switchIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.widget_switch_child_btn, switchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_child_header_container, switchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }
}

/**
 * WorkManager coroutine worker that queries Room and GPS on a background thread and
 * pushes live timetable, multi-child switching, and geofence zone data to the home screen widget.
 */
class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var s: SystemStrings = stringsFor(AppLanguage.BM).systemUi

    override suspend fun doWork(): Result {
        val language = runCatching { context.currentAppLanguage() }.getOrDefault(AppLanguage.BM)
        val appStrings = stringsFor(language)
        s = appStrings.systemUi
        context.getSharedPreferences(SlotifyAppWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(SlotifyAppWidgetProvider.PREF_LANGUAGE, language.name).apply()
        return try {
            val dao = SlotifyDatabase.getDatabase(context).slotifyDao()

            // 1. Students and the widget's active index
            val students = dao.getAllStudentsOnce()
            if (students.isEmpty()) return pushFallback()

            val prefs = context.getSharedPreferences(SlotifyAppWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            val rawIndex = prefs.getInt(SlotifyAppWidgetProvider.PREF_STUDENT_INDEX, 0)
            val activeIndex = ((rawIndex % students.size) + students.size) % students.size
            prefs.edit().putInt(SlotifyAppWidgetProvider.PREF_STUDENT_INDEX, activeIndex).apply()
            val student = students[activeIndex]

            val sessions = dao.getSessionsOnce(student.id).sortedBy { it.startTime }
            if (sessions.isEmpty()) return pushFallback()

            val now = LocalTime.now()
            val today = LocalDate.now()
            val schoolDay = SchoolDay.of(today)
            val isWeekend = schoolDay == null
            val minuteOfDay = now.hour * 60 + now.minute
            val nowText = String.format(Locale.US, "%02d:%02d", now.hour, now.minute)

            // Session in progress, else the next one today, else the last one.
            val session = sessions.firstOrNull { it.startTime <= nowText && nowText < it.endTime }
                ?: sessions.firstOrNull { it.startTime > nowText }
                ?: sessions.last()

            // 2. Zone: GPS only when background location is granted, else the profile routine.
            val gpsFix = if (hasBackgroundLocation()) {
                runCatching { GpsLocationHelper.getAccurateLocation(context, timeoutMs = 2500L) }.getOrNull()
            } else null
            val routine = FamilyProfileData.routineZones[student.id].orEmpty()
            val zoneText: String = when {
                gpsFix != null -> gpsZoneText(gpsFix.latitude, gpsFix.longitude)
                isWeekend -> s.widgetZoneWeekendHome
                else -> routine.zoneAt(minuteOfDay)?.let { "📍 ${it.label}" } ?: s.widgetZoneOpenMap
            }

            // 3. Siblings summary (active child in brackets)
            val siblingsSummary = students.joinToString(" • ") { s ->
                val status = if (isWeekend) "🏠"
                else FamilyProfileData.routineZones[s.id].orEmpty().zoneAt(minuteOfDay)?.shortLabel ?: "–"
                val label = "${s.avatarEmoji} ${s.nickname}: $status"
                if (s.id == student.id) "[$label]" else label
            }

            val prayerSchedule = JakimPrayerTimeHelper.getSchedule(now)
            val prayerText = "🕌 ${prayerSchedule.nextPrayerName} (${appStrings.dashboardUi.prayerCountdown(prayerSchedule.minutesUntilNext)})"
            val isBagPacked = SchoolBagHelper.isBagPackedToday(context, student.id)

            if (isWeekend) {
                SlotifyAppWidgetProvider.pushUpdate(
                    context = context,
                    s = s,
                    childName = "${student.avatarEmoji} ${student.nickname}",
                    sessionLabel = sessionLabel(session.sessionTimeType),
                    currentClass = s.widgetWeekendCurrent,
                    nextClass = s.widgetWeekendNext,
                    books = if (isBagPacked) s.widgetBagPacked else s.widgetCheckBagMonday,
                    attire = "👕 ${s.widgetCasualAttire} • $prayerText",
                    locationZone = zoneText,
                    siblingsSummary = siblingsSummary
                )
                return Result.success()
            }

            // 4. Today's slots across all of the student's sessions
            val slots = dao.getStudentSlotsForDayOnce(student.id, schoolDay!!).filter { !it.isRehat }
            val currentSlot = slots.lastOrNull { it.startTime <= nowText && nowText < it.endTime }
            val nextSlot = slots.firstOrNull { it.startTime > nowText }

            val currentText = currentSlot?.let { "⏳ ${it.subjectName} (${it.startTime} - ${it.endTime})" }
                ?: if (nextSlot != null) s.widgetNotStarted else s.widgetClassesDone
            val nextText = nextSlot?.let { s.widgetNextClass(it.subjectName, it.startTime) } ?: s.widgetSessionOver

            SlotifyAppWidgetProvider.pushUpdate(
                context      = context,
                s            = s,
                childName    = "${student.avatarEmoji} ${student.nickname}",
                sessionLabel = "${session.className} (${sessionLabel(session.sessionTimeType)})",
                currentClass = currentText,
                nextClass    = nextText,
                books        = if (isBagPacked) s.widgetBagPacked else s.widgetBookHint(currentSlot?.subjectCode ?: nextSlot?.subjectCode),
                attire       = "👕 ${AttireGuide.forSchoolDay(schoolDay, language)} • $prayerText",
                locationZone = zoneText,
                siblingsSummary = siblingsSummary
            )
            Result.success()
        } catch (e: Exception) {
            pushFallback()
        }
    }

    private fun hasBackgroundLocation(): Boolean {
        fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        val foreground = granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        return foreground && background
    }

    private fun gpsZoneText(lat: Double, lng: Double): String {
        val targets = GeofenceManager.getDefaultTargets(context)
        val closest = targets.minByOrNull { GeofenceManager.calculateDistanceMeters(lat, lng, it.lat, it.lng) }
            ?: return s.widgetZoneCoordinates(String.format(Locale.US, "%.4f, %.4f", lat, lng))
        val dist = GeofenceManager.calculateDistanceMeters(lat, lng, closest.lat, closest.lng)
        return when {
            dist <= closest.radiusMeters -> s.widgetZoneInside("${closest.iconEmoji} ${closest.name}")
            dist <= closest.radiusMeters * 2 -> s.widgetZoneApproaching("${closest.iconEmoji} ${closest.name}", dist.roundToInt())
            else -> s.widgetZoneDistance(
                "${closest.iconEmoji} ${closest.name}",
                "${String.format(Locale.US, "%.1f", dist / 1000.0)}km"
            )
        }
    }

    private fun pushFallback(): Result {
        SlotifyAppWidgetProvider.pushUpdate(
            context = context,
            s = s,
            childName = s.widgetFallbackChild,
            sessionLabel = "–",
            currentClass = s.widgetFallbackCurrent,
            nextClass = "",
            books = s.widgetFallbackBooks,
            attire = s.widgetFallbackAttire,
            locationZone = s.widgetZoneOpenMap,
            siblingsSummary = ""
        )
        return Result.success()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun sessionLabel(sessionTimeType: String) = when (sessionTimeType) {
        "PAGI"     -> s.widgetSessionMorning
        "PETANG"   -> s.widgetSessionAfternoon
        "FULL_DAY" -> s.widgetSessionFullDay
        else       -> sessionTimeType
    }
}
