package my.slotify.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import my.slotify.app.core.database.DatabaseSeeder
import my.slotify.app.core.database.SlotifyDatabase
import my.slotify.app.core.finance.FeeRepository
import my.slotify.app.core.notification.SlotifyNotificationManager
import my.slotify.app.feature.logistics.GeofenceManager

class SlotifyApplication : Application() {

    /** Process-wide scope for fire-and-forget work that must outlive any screen. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: SlotifyDatabase by lazy {
        SlotifyDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        SlotifyNotificationManager.createNotificationChannels(this)
        GeofenceManager.scheduleBackgroundMonitoring(this)

        // Single seeding path (versioned, insert-or-ignore). See DatabaseSeeder.
        applicationScope.launch {
            try {
                val dao = database.slotifyDao()
                DatabaseSeeder.seedIfNeeded(this@SlotifyApplication, dao)
                FeeRepository.ensureMonth(dao, FeeRepository.monthKey())
            } catch (e: Exception) {
                Log.e("SlotifyApplication", "Initial seeding failed", e)
            }
        }
    }
}
