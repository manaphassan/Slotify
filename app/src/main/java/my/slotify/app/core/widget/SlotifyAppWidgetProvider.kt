package my.slotify.app.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.work.*
import my.slotify.app.MainActivity
import my.slotify.app.R
import my.slotify.app.core.database.SlotifyDatabase
import my.slotify.app.feature.logistics.GeofenceManager
import my.slotify.app.feature.logistics.GpsLocationHelper
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
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
 * child name to cycle seamlessly between Adam, Aisyah, and Danial directly from
 * the Android home screen.
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
        val views = RemoteViews(context.packageName, R.layout.widget_slotify_class)
        views.setTextViewText(R.id.widget_child_name, "Memuatkan…")
        views.setTextViewText(R.id.widget_zone_text, "📍 Membaca Zon GPS…")
        views.setTextViewText(R.id.widget_current_class, "⏳ Mengambil jadual…")
        views.setTextViewText(R.id.widget_next_class, "")
        views.setTextViewText(R.id.widget_books, "")
        views.setTextViewText(R.id.widget_siblings_status, "👦 Adam • 👧 Aisyah • 🧒 Danial")
        views.setTextViewText(R.id.widget_attire, "")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val WORK_NAME          = "slotify_widget_update_once"
        const val PERIODIC_WORK_NAME = "slotify_widget_update_periodic"
        const val ACTION_SWITCH_STUDENT = "my.slotify.app.ACTION_SWITCH_STUDENT"
        const val PREFS_NAME         = "slotify_widget_prefs"
        const val PREF_STUDENT_INDEX = "key_selected_student_index"

        /**
         * Builds and pushes a fully-populated RemoteViews to all widget instances.
         * Called by [WidgetUpdateWorker] after querying Room and evaluating GPS on a background thread.
         */
        fun pushUpdate(
            context: Context,
            childName: String,
            sessionLabel: String,
            currentClass: String,
            nextClass: String,
            books: String,
            attire: String,
            locationZone: String = "📍 Zon: Memuatkan...",
            siblingsSummary: String = "👦 Adam • 👧 Aisyah • 🧒 Danial"
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, SlotifyAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)

            val views = RemoteViews(context.packageName, R.layout.widget_slotify_class)
            views.setTextViewText(R.id.widget_child_name, "$childName • $sessionLabel")
            views.setTextViewText(R.id.widget_zone_text, locationZone)
            views.setTextViewText(R.id.widget_current_class, currentClass)
            views.setTextViewText(R.id.widget_next_class, nextClass)
            views.setTextViewText(R.id.widget_books, books)
            views.setTextViewText(R.id.widget_siblings_status, siblingsSummary)
            views.setTextViewText(R.id.widget_attire, attire)

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

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

    override suspend fun doWork(): Result {
        return try {
            val dao = SlotifyDatabase.getDatabase(context).slotifyDao()

            // 1. Get all students and read current active index from SharedPreferences
            val students = dao.getAllStudentsOnce()
            if (students.isEmpty()) return pushFallback()

            val prefs = context.getSharedPreferences(SlotifyAppWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            val rawIndex = prefs.getInt(SlotifyAppWidgetProvider.PREF_STUDENT_INDEX, 0)
            val activeIndex = if (students.isNotEmpty()) (rawIndex % students.size + students.size) % students.size else 0
            prefs.edit().putInt(SlotifyAppWidgetProvider.PREF_STUDENT_INDEX, activeIndex).apply()

            val student = students[activeIndex]

            // 2. Get active school session for this child
            val sessions = dao.getSessionsOnce(student.id)
            if (sessions.isEmpty()) return pushFallback()

            val now = LocalTime.now()
            val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = when (calDay) {
                Calendar.MONDAY    -> 1
                Calendar.TUESDAY   -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY  -> 4
                Calendar.FRIDAY    -> 5
                else               -> -1 // Weekend
            }
            val isWeekend = dayOfWeek == -1
            val timeMinutes = now.hour * 60 + now.minute

            // Pick session: For dual-session (like Adam with SRA pagi + SK petang), choose based on current time
            val session = if (sessions.size > 1) {
                if (timeMinutes < 11 * 60 + 30) sessions.first() else sessions.last()
            } else {
                sessions.first()
            }

            // 3. Proximity / Zone for the active child
            val gpsFix = runCatching { GpsLocationHelper.getAccurateLocation(context, timeoutMs = 2500L) }.getOrNull()
            val zoneText: String = if (gpsFix != null) {
                val targets = GeofenceManager.getDefaultTargets(context)
                val closest = targets.minByOrNull { t ->
                    GeofenceManager.calculateDistanceMeters(gpsFix.latitude, gpsFix.longitude, t.lat, t.lng)
                }
                if (closest != null) {
                    val dist = GeofenceManager.calculateDistanceMeters(gpsFix.latitude, gpsFix.longitude, closest.lat, closest.lng)
                    when {
                        dist <= closest.radiusMeters -> "📍 Zon: ${closest.iconEmoji} ${closest.name} (Dalam Zon)"
                        dist <= closest.radiusMeters * 2 -> "📍 Menghampiri ${closest.iconEmoji} ${closest.name} (~${dist.roundToInt()}m)"
                        else -> "📍 Zon: ${closest.iconEmoji} ${closest.name} (~${String.format(Locale.US, "%.1f", dist / 1000.0)}km)"
                    }
                } else {
                    "📍 Koordinat: ${String.format(Locale.US, "%.4f", gpsFix.latitude)}, ${String.format(Locale.US, "%.4f", gpsFix.longitude)}"
                }
            } else {
                // Intelligent schedule-based estimation customized per student
                if (isWeekend) {
                    "📍 Zon: 🏠 Rumah (Hujung Minggu)"
                } else {
                    when (student.nickname) {
                        "Aisyah" -> when {
                            timeMinutes < 7 * 60 + 15 -> "📍 Zon: 🏠 Rumah (Persediaan)"
                            timeMinutes < 15 * 60 + 30 -> "📍 Zon: 🕌 SRAI Bukit Contoh (Seharian)"
                            timeMinutes < 16 * 60 + 15 -> "📍 Transit: 🚐 Van Sekolah B"
                            else -> "📍 Zon: 🏠 Rumah (Rehat & Solat)"
                        }
                        "Danial" -> when {
                            timeMinutes < 7 * 60 + 45 -> "📍 Zon: 🏠 Rumah (Persediaan)"
                            timeMinutes < 12 * 60 -> "📍 Zon: 🎒 Tadika Ceria (Adik Pintar)"
                            else -> "📍 Zon: 🏠 Rumah (Bermain & Rehat)"
                        }
                        else -> when { // Adam (default)
                            timeMinutes < 7 * 60 + 30 -> "📍 Zon: 🏠 Rumah (Persediaan)"
                            timeMinutes < 11 * 60 + 15 -> "📍 Zon: 🕌 SRA Taman Contoh (Sesi Pagi)"
                            timeMinutes < 12 * 60 + 45 -> "📍 Zon: 👶 Transit Taska Ceria"
                            timeMinutes < 18 * 60 -> "📍 Zon: 🏫 SK Taman Contoh (Sesi Petang)"
                            timeMinutes < 18 * 60 + 45 -> "📍 Transit: 🚐 Van Sekolah A"
                            else -> "📍 Zon: 🏠 Rumah (Ulangkaji & Rehat)"
                        }
                    }
                }
            }

            // 4. Multi-Child Siblings Summary Row (Highlighting active student!)
            val adamStatus = when {
                timeMinutes < 7 * 60 + 30 -> "Rumah"
                timeMinutes < 11 * 60 + 15 -> "SRA"
                timeMinutes < 12 * 60 + 45 -> "Transit"
                timeMinutes < 18 * 60 -> "SK"
                timeMinutes < 18 * 60 + 45 -> "Van"
                else -> "Rumah"
            }
            val aisyahStatus = when {
                timeMinutes in (7 * 60 + 30)..(15 * 60 + 30) -> "SRAI"
                timeMinutes in (15 * 60 + 30)..(16 * 60 + 15) -> "Van Sekolah B"
                else -> "Rumah"
            }
            val danialStatus = when {
                timeMinutes in (8 * 60)..(12 * 60) -> "Tadika"
                else -> "Rumah"
            }

            val siblingsSummary = if (isWeekend) {
                when (student.nickname) {
                    "Aisyah" -> "👦 Adam • [👧 Aisyah: 🏠] • 🧒 Danial"
                    "Danial" -> "👦 Adam • 👧 Aisyah • [🧒 Danial: 🏠]"
                    else -> "[👦 Adam: 🏠] • 👧 Aisyah • 🧒 Danial"
                }
            } else {
                when (student.nickname) {
                    "Aisyah" -> "👦 Adam: $adamStatus • [👧 Aisyah: $aisyahStatus] • 🧒 Danial: $danialStatus"
                    "Danial" -> "👦 Adam: $adamStatus • 👧 Aisyah: $aisyahStatus • [🧒 Danial: $danialStatus]"
                    else -> "[👦 Adam: $adamStatus] • 👧 Aisyah: $aisyahStatus • 🧒 Danial: $danialStatus"
                }
            }

            // Weekend state
            if (isWeekend) {
                SlotifyAppWidgetProvider.pushUpdate(
                    context = context,
                    childName = "${student.avatarEmoji} ${student.nickname}",
                    sessionLabel = sessionLabel(session.sessionTimeType),
                    currentClass = "🎉 Hujung Minggu! Rehat & Enjoy",
                    nextClass = "⏩ Sekolah semula Isnin depan",
                    books = "📚 Semak beg untuk Isnin",
                    attire = attireForDay(calDay),
                    locationZone = zoneText,
                    siblingsSummary = siblingsSummary
                )
                return Result.success()
            }

            // 5. Load today's slots & determine current / next
            val slots = dao.getSlotsOnce(session.id, dayOfWeek)
                .filter { !it.isRehat }

            val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

            val currentSlot = slots.lastOrNull { slot ->
                runCatching { LocalTime.parse(slot.startTime, timeFmt) <= now }.getOrDefault(false)
            }
            val nextSlot = slots.firstOrNull { slot ->
                runCatching { LocalTime.parse(slot.startTime, timeFmt) > now }.getOrDefault(false)
            }

            // 6. Compose display strings
            val currentText = if (currentSlot != null) {
                "⏳ ${currentSlot.subjectName} (${currentSlot.startTime} - ${currentSlot.endTime})"
            } else {
                "⏳ Belum bermula — bersedia sekarang!"
            }

            val nextText = if (nextSlot != null) {
                "⏩ Seterusnya: ${nextSlot.subjectName} (${nextSlot.startTime})"
            } else {
                "⏩ Tamat sesi hari ini ✓"
            }

            val booksText = bookHintForSubject(currentSlot?.subjectCode ?: nextSlot?.subjectCode)

            // 7. Push to widget
            SlotifyAppWidgetProvider.pushUpdate(
                context      = context,
                childName    = "${student.avatarEmoji} ${student.nickname}",
                sessionLabel = "${session.className} (${sessionLabel(session.sessionTimeType)})",
                currentClass = currentText,
                nextClass    = nextText,
                books        = booksText,
                attire       = attireForDay(calDay),
                locationZone = zoneText,
                siblingsSummary = siblingsSummary
            )
            Result.success()
        } catch (e: Exception) {
            pushFallback()
        }
    }

    private fun pushFallback(): Result {
        SlotifyAppWidgetProvider.pushUpdate(
            context = context,
            childName = "Murid",
            sessionLabel = "–",
            currentClass = "⏳ Buka Slotify untuk maklumat",
            nextClass = "",
            books = "📚 Buka app untuk semak beg",
            attire = "👕 Semak pakaian sekolah",
            locationZone = "📍 Zon: Buka Peta Logistik",
            siblingsSummary = "👦 Adam • 👧 Aisyah • 🧒 Danial: Di Rumah"
        )
        return Result.success()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun sessionLabel(sessionTimeType: String) = when (sessionTimeType) {
        "PAGI"     -> "Sesi Pagi"
        "PETANG"   -> "Sesi Petang"
        "FULL_DAY" -> "Seharian"
        else       -> sessionTimeType
    }

    private fun attireForDay(calDay: Int) = when (calDay) {
        Calendar.MONDAY    -> "👕 Uniform Penuh KPM"
        Calendar.TUESDAY   -> "👕 Baju Sukan Sekolah"
        Calendar.WEDNESDAY -> "👕 Badan Beruniform (Pengakap / PRS)"
        Calendar.THURSDAY  -> "👕 Baju Batik KPM"
        Calendar.FRIDAY    -> "👕 Baju Melayu Lengkap"
        else               -> "👕 Pakaian Bebas / Hujung Minggu"
    }

    private fun bookHintForSubject(subjectCode: String?) = when (subjectCode) {
        "BM"     -> "📚 Bawa: Buku Teks Bahasa Melayu Jilid 1 & Buku Latihan"
        "MT"     -> "📚 Bawa: Buku Teks Matematik & Buku Latihan Jilid 1"
        "SN"     -> "📚 Bawa: Buku Teks Sains & Buku Amali Sains"
        "BI"     -> "📚 Bawa: Buku Teks Bahasa Inggeris & Reading Log"
        "SEJ"    -> "📚 Bawa: Buku Teks Sejarah & Nota Sejarah"
        "PI"     -> "📚 Bawa: Buku Teks Pendidikan Islam & Buku Latihan PI"
        "TAWHID" -> "📚 Bawa: Kitab Tauhid & Buku Nota SRA"
        "JAWI"   -> "📚 Bawa: Buku Latihan Jawi & Kaedah Baghdadiyyah"
        "BA"     -> "📚 Bawa: Buku Teks Bahasa Arab & Kamus Arab-Melayu"
        "TAHFIZ",
        "QURAN"  -> "📚 Bawa: Al-Quran & Buku Rekod Hafazan"
        "FIQH"   -> "📚 Bawa: Buku Teks Fiqah & Buku Latihan"
        "PJPK"   -> "👟 Bawa: Kasut Sukan & Baju Sukan"
        "PSV"    -> "🎨 Bawa: Palet Warna & Buku Lukisan"
        "KH"     -> "🔧 Bawa: Set Jahitan / Alat Masakan (semak jadual)"
        "IQRA"   -> "📚 Bawa: Buku Iqra' & Buku Doa Harian"
        "RBT"    -> "🔩 Bawa: Buku Reka Bentuk & Teknologi"
        else     -> "📚 Semak beg mengikut jadual hari ini"
    }
}
