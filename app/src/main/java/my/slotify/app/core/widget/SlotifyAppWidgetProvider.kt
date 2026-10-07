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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Home screen widget provider.
 *
 * onUpdate() is called by the system on boot, manual add, and periodic refresh.
 * It delegates all Room DB work to [WidgetUpdateWorker] (a coroutine Worker) so
 * this class stays on the main thread — AppWidgetProvider cannot suspend itself.
 */
class SlotifyAppWidgetProvider : AppWidgetProvider() {

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
        views.setTextViewText(R.id.widget_current_class, "⏳ Mengambil jadual…")
        views.setTextViewText(R.id.widget_next_class, "")
        views.setTextViewText(R.id.widget_books, "")
        views.setTextViewText(R.id.widget_attire, "")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val WORK_NAME          = "slotify_widget_update_once"
        const val PERIODIC_WORK_NAME = "slotify_widget_update_periodic"

        /**
         * Builds and pushes a fully-populated RemoteViews to all widget instances.
         * Called by [WidgetUpdateWorker] after querying Room on a background thread.
         */
        fun pushUpdate(
            context: Context,
            childName: String,
            sessionLabel: String,
            currentClass: String,
            nextClass: String,
            books: String,
            attire: String
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, SlotifyAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)

            val views = RemoteViews(context.packageName, R.layout.widget_slotify_class)
            views.setTextViewText(R.id.widget_child_name, "$childName • $sessionLabel")
            views.setTextViewText(R.id.widget_current_class, currentClass)
            views.setTextViewText(R.id.widget_next_class, nextClass)
            views.setTextViewText(R.id.widget_books, books)
            views.setTextViewText(R.id.widget_attire, attire)

            // Tap to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }
}

/**
 * WorkManager coroutine worker that queries Room on a background thread and
 * pushes real timetable data to the home screen widget.
 *
 * Data flow:
 *   1. Load first student from Room
 *   2. Load their first school session
 *   3. Determine today's dayOfWeek (1=Isnin … 5=Jumaat)
 *   4. Load today's slots, find current + next based on LocalTime.now()
 *   5. Derive attire from day, books from current slot's subjectName
 *   6. Push via SlotifyAppWidgetProvider.pushUpdate()
 */
class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dao = SlotifyDatabase.getDatabase(context).slotifyDao()

            // 1. Get first student (widget shows the "primary" child)
            val students = dao.getAllStudentsOnce()
            val student  = students.firstOrNull() ?: return pushFallback()

            // 2. Get their first active session
            val sessions = dao.getSessionsOnce(student.id)
            val session  = sessions.firstOrNull() ?: return pushFallback()

            // 3. Today in Malaysian school week (Isnin=1 … Jumaat=5)
            val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = when (calDay) {
                Calendar.MONDAY    -> 1
                Calendar.TUESDAY   -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY  -> 4
                Calendar.FRIDAY    -> 5
                else               -> -1 // Weekend
            }

            // Weekend state
            if (dayOfWeek == -1) {
                SlotifyAppWidgetProvider.pushUpdate(
                    context,
                    student.nickname,
                    sessionLabel(session.sessionTimeType),
                    "🎉 Hujung Minggu! Rehat & Enjoy",
                    "⏩ Sekolah semula Isnin depan",
                    "📚 Semak beg untuk Isnin",
                    attireForDay(calDay)
                )
                return Result.success()
            }

            // 4. Load today's slots & determine current / next
            val slots = dao.getSlotsOnce(session.id, dayOfWeek)
                .filter { !it.isRehat } // Skip rehat for widget display

            val now = LocalTime.now()
            val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

            val currentSlot = slots.lastOrNull { slot ->
                runCatching { LocalTime.parse(slot.startTime, timeFmt) <= now }.getOrDefault(false)
            }
            val nextSlot = slots.firstOrNull { slot ->
                runCatching { LocalTime.parse(slot.startTime, timeFmt) > now }.getOrDefault(false)
            }

            // 5. Compose display strings
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

            // Books: derive from current slot's subject code
            val booksText = bookHintForSubject(currentSlot?.subjectCode ?: nextSlot?.subjectCode)

            // 6. Push to widget
            SlotifyAppWidgetProvider.pushUpdate(
                context     = context,
                childName   = student.nickname,
                sessionLabel = sessionLabel(session.sessionTimeType),
                currentClass = currentText,
                nextClass    = nextText,
                books        = booksText,
                attire       = attireForDay(calDay)
            )
            Result.success()
        } catch (e: Exception) {
            pushFallback()
        }
    }

    private fun pushFallback(): Result {
        SlotifyAppWidgetProvider.pushUpdate(
            context, "Murid", "–",
            "⏳ Buka Slotify untuk maklumat",
            "", "📚 Buka app untuk semak beg", "👕 Semak pakaian sekolah"
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
