package my.slotify.app.feature.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.slotify.app.core.data.RewardsRepository
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.rewards.RewardKeys
import java.time.LocalDate

data class RewardsUiState(
    val student: StudentEntity? = null,
    /** True until loaded, so the chest can never be tapped before its claim state is known. */
    val chestClaimed: Boolean = true
)

sealed interface RewardsEvent {
    data object ChestOpened : RewardsEvent
    data class Redeemed(val item: RewardCatalogItem) : RewardsEvent
    data class RedeemFailed(val item: RewardCatalogItem) : RewardsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModel(
    students: StudentRepository,
    private val rewards: RewardsRepository
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val _event = MutableStateFlow<RewardsEvent?>(null)
    val event: StateFlow<RewardsEvent?> = _event.asStateFlow()

    val uiState: StateFlow<RewardsUiState> = combine(students.selectedStudent, today) { s, d -> s to d }
        .flatMapLatest { (student, date) ->
            rewards.isClaimed(student?.let { RewardKeys.dailyChest(it.id, date) })
                .map { claimed -> RewardsUiState(student, student == null || claimed) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsUiState())

    /** Call when the visible date may have changed (e.g. on resume or at midnight). */
    fun setToday(date: LocalDate) {
        today.value = date
    }

    fun openChest() {
        val student = uiState.value.student ?: return
        viewModelScope.launch {
            if (rewards.claimDailyChest(student.id, today.value)) _event.value = RewardsEvent.ChestOpened
        }
    }

    /** Called after parent PIN approval. Spends atomically; success only if the balance covered it. */
    fun redeem(item: RewardCatalogItem) {
        val student = uiState.value.student ?: return
        viewModelScope.launch {
            _event.value = if (rewards.spend(student.id, item.pointsCost)) RewardsEvent.Redeemed(item)
            else RewardsEvent.RedeemFailed(item)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}
