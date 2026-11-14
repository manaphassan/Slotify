package my.slotify.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.slotify.app.core.data.SchoolworkRepository
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.PbdAssessmentEntity
import my.slotify.app.core.database.StudentEntity

data class ProfileUiState(
    val student: StudentEntity? = null,
    val assessments: List<PbdAssessmentEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val students: StudentRepository,
    private val schoolwork: SchoolworkRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = students.selectedStudent
        .flatMapLatest { student -> schoolwork.assessments(student?.id).map { ProfileUiState(student, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun saveProfile(updated: StudentEntity) {
        viewModelScope.launch { students.update(updated) }
    }

    /** One award per subject per assessment period, however often it is re-recorded. */
    fun recordAssessment(assessment: PbdAssessmentEntity) {
        viewModelScope.launch {
            val stars = schoolwork.recordAssessment(assessment)
            _message.value = if (stars > 0) {
                "🎉 TP${assessment.tahapPenguasaan} ${assessment.subjectName} direkodkan! +$stars ⭐"
            } else {
                "TP${assessment.tahapPenguasaan} ${assessment.subjectName} dikemas kini."
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
