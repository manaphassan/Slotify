package my.slotify.app.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.slotify.app.core.data.RewardsRepository
import my.slotify.app.core.data.SchoolworkRepository
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.ExamEntity
import my.slotify.app.core.database.HomeworkTaskEntity
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.rewards.RewardKeys
import my.slotify.app.core.time.SchoolDay
import java.time.LocalDate

data class DashboardUiState(
    val student: StudentEntity? = null,
    val today: LocalDate = LocalDate.now(),
    /** 1=Isnin..5=Jumaat, null on weekends. */
    val todaySchoolDay: Int? = SchoolDay.of(LocalDate.now()),
    val nextSchoolDay: Int = SchoolDay.nextAfter(LocalDate.now()),
    val tasks: List<HomeworkTaskEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val todaySlots: List<TimetableSlotEntity> = emptyList(),
    val nextDaySlots: List<TimetableSlotEntity> = emptyList(),
    val bagPacked: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val appContext: Context,
    students: StudentRepository,
    private val schoolwork: SchoolworkRepository,
    private val rewards: RewardsRepository
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DashboardUiState> = combine(students.selectedStudent, today) { s, d -> s to d }
        .flatMapLatest { (student, date) ->
            val id = student?.id
            val todayDay = SchoolDay.of(date)
            val nextDay = SchoolDay.nextAfter(date)
            combine(
                schoolwork.homework(id),
                schoolwork.exams(id),
                students.slotsForDay(id, todayDay),
                students.slotsForDay(id, nextDay),
                rewards.isClaimed(id?.let { RewardKeys.bagPacked(it, date) })
            ) { tasks, exams, todaySlots, nextSlots, bag ->
                DashboardUiState(student, date, todayDay, nextDay, tasks, exams, todaySlots, nextSlots, bag)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun setToday(date: LocalDate) {
        today.value = date
    }

    fun toggleHomework(task: HomeworkTaskEntity) {
        viewModelScope.launch { schoolwork.toggleHomework(task) }
    }

    fun addHomework(task: HomeworkTaskEntity) {
        viewModelScope.launch { schoolwork.addHomework(task) }
    }

    /** Once-per-day bag reward; also sets the flag the home-screen widget reads. */
    fun markBagPacked() {
        val student = uiState.value.student ?: return
        viewModelScope.launch {
            SchoolBagHelper.setBagPackedToday(appContext, student.id, true)
            rewards.claimBagPacked(student.id, today.value)
        }
    }

    fun completeFocusSession() {
        val student = uiState.value.student ?: return
        viewModelScope.launch { rewards.claimFocusSession(student.id, today.value) }
    }
}
