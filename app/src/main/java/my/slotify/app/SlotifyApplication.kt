package my.slotify.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.slotify.app.core.database.DatabaseSeeder
import my.slotify.app.core.database.SlotifyDatabase

class SlotifyApplication : Application() {

    val database: SlotifyDatabase by lazy {
        SlotifyDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Seed initial family data (Adam, Aisyah, Danial)
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder.seedInitialData(database.slotifyDao())
        }
    }
}
