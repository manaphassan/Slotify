package my.slotify.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.slotify.app.core.data.SettingsRepository
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.designsystem.theme.AppThemeMode

data class MainUiState(
    val isLoaded: Boolean = false,
    val students: List<StudentEntity> = emptyList(),
    val selectedStudent: StudentEntity? = null,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.BM
)

/** App shell state: student selection, theme and language — all persisted. */
class MainViewModel(
    private val settings: SettingsRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        studentRepository.students,
        studentRepository.selectedStudent,
        settings.settings
    ) { students, selected, prefs ->
        MainUiState(true, students, selected, prefs.themeMode, prefs.language)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun selectStudent(student: StudentEntity) {
        viewModelScope.launch { studentRepository.select(student.id) }
    }

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun cycleTheme() = setTheme(
        when (uiState.value.themeMode) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.NORD
            AppThemeMode.NORD -> AppThemeMode.LIGHT
        }
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun toggleLanguage() = setLanguage(if (uiState.value.language == AppLanguage.BM) AppLanguage.EN else AppLanguage.BM)
}
