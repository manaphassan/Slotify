package my.slotify.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.designsystem.theme.AppThemeMode

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.BM,
    val selectedStudentId: String? = null
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "slotify_settings")

/** User preferences persisted across launches (previously lost on every cold start). */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[THEME]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.LIGHT,
            language = p[LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.BM,
            selectedStudentId = p[SELECTED_STUDENT]
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setLanguage(language: AppLanguage) = store.edit { it[LANGUAGE] = language.name }
    suspend fun setSelectedStudent(id: String) = store.edit { it[SELECTED_STUDENT] = id }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val SELECTED_STUDENT = stringPreferencesKey("selected_student_id")
    }
}
