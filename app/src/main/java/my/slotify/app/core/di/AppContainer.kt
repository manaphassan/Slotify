package my.slotify.app.core.di

import android.content.Context
import my.slotify.app.core.data.FinanceRepository
import my.slotify.app.core.data.RewardsRepository
import my.slotify.app.core.data.SchoolworkRepository
import my.slotify.app.core.data.SettingsRepository
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.SlotifyDatabase

/**
 * Manual dependency container (no DI framework). Created once in SlotifyApplication and
 * read by [SlotifyViewModelFactory].
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: SlotifyDatabase by lazy { SlotifyDatabase.getDatabase(appContext) }
    val dao get() = database.slotifyDao()

    val settings by lazy { SettingsRepository(appContext) }
    val students by lazy { StudentRepository(dao, settings) }
    val rewards by lazy { RewardsRepository(dao) }
    val schoolwork by lazy { SchoolworkRepository(dao) }
    val finance by lazy { FinanceRepository(dao) }
}
