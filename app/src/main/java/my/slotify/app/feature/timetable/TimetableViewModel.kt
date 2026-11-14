package my.slotify.app.feature.timetable

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.SchoolSessionEntity
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity

data class TimetableUiState(
    val student: StudentEntity? = null,
    val sessions: List<SchoolSessionEntity> = emptyList(),
    val activeSessionIndex: Int = 0,
    val activeSession: SchoolSessionEntity? = null,
    val slots: List<TimetableSlotEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModel(
    private val students: StudentRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    /** Session index per student, kept across rotation and process death. */
    private val sessionIndexByStudent = savedState.getStateFlow(KEY_INDEX, emptyMap<String, Int>())

    val uiState: StateFlow<TimetableUiState> = students.selectedStudent
        .flatMapLatest { student ->
            combine(students.sessions(student?.id), sessionIndexByStudent) { sessions, indices ->
                val sorted = sessions.sortedBy { it.startTime }
                val index = (indices[student?.id] ?: 0).coerceIn(0, (sorted.size - 1).coerceAtLeast(0))
                Triple(student, sorted, index)
            }
        }
        .flatMapLatest { (student, sessions, index) ->
            val active = sessions.getOrNull(index)
            students.slotsForSession(active?.id).map { slots ->
                TimetableUiState(student, sessions, index, active, slots)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimetableUiState())

    /** Cycles to the student's next session (e.g. SRA pagi ⇄ SK petang). */
    fun nextSession() {
        val state = uiState.value
        val student = state.student ?: return
        if (state.sessions.size < 2) return
        val next = (state.activeSessionIndex + 1) % state.sessions.size
        savedState[KEY_INDEX] = sessionIndexByStudent.value + (student.id to next)
    }

    fun addSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch { students.addSlot(slot) }
    }

    fun deleteSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch { students.deleteSlot(slot) }
    }

    private companion object {
        const val KEY_INDEX = "session_index_by_student"
    }
}
