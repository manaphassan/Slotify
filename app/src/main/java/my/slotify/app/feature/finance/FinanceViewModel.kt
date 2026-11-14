package my.slotify.app.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.slotify.app.core.data.FinanceRepository
import my.slotify.app.core.data.RewardsRepository
import my.slotify.app.core.data.StudentRepository
import my.slotify.app.core.database.CommitmentFeeEntity
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.finance.FeeRepository
import my.slotify.app.core.rewards.StarRewards
import java.time.LocalDate

data class FinanceUiState(
    val student: StudentEntity? = null,
    val students: List<StudentEntity> = emptyList(),
    val billingMonth: String = FeeRepository.monthKey(),
    val fees: List<CommitmentFeeEntity> = emptyList(),
    val budgetSen: Long = 0,
    val spentTodaySen: Long = 0,
    /** Tabung Maya after deducting RM 10 per star conversion. */
    val tabungSen: Long = 0
) {
    val savedTodaySen: Long get() = (budgetSen - spentTodaySen).coerceAtLeast(0)
    val totalFeesSen: Long get() = fees.sumOf { it.amountSen }
    val paidFeesSen: Long get() = fees.filter { it.isPaid }.sumOf { it.amountSen }
    val canConvertSavings: Boolean get() = tabungSen >= StarRewards.SAVINGS_CONVERSION_SEN
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    students: StudentRepository,
    private val finance: FinanceRepository,
    private val rewards: RewardsRepository
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val _message = MutableStateFlow<String?>(null)

    /** One-shot user message (celebration / validation); clear with [consumeMessage]. */
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<FinanceUiState> =
        combine(students.selectedStudent, students.students, today) { s, all, d -> Triple(s, all, d) }
            .flatMapLatest { (student, all, date) ->
                val month = FeeRepository.monthKey(date)
                val id = student?.id
                combine(
                    finance.fees(month),
                    finance.spending(id, date.toString()),
                    finance.allSpending(id),
                    rewards.savingsConversions(id)
                ) { fees, todayRow, history, conversions ->
                    val budget = student?.dailyCanteenBudgetSen ?: 0L
                    FinanceUiState(
                        student = student,
                        students = all,
                        billingMonth = month,
                        fees = fees,
                        budgetSen = budget,
                        spentTodaySen = todayRow?.spentSen ?: 0L,
                        tabungSen = StarRewards.tabungBalanceSen(budget, history.map { it.spentSen }, conversions)
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    init {
        viewModelScope.launch { finance.ensureMonth(FeeRepository.monthKey(today.value)) }
    }

    fun setToday(date: LocalDate) {
        if (date == today.value) return
        today.value = date
        viewModelScope.launch { finance.ensureMonth(FeeRepository.monthKey(date)) }
    }

    fun setSpentToday(spentSen: Long) {
        val student = uiState.value.student ?: return
        viewModelScope.launch { finance.setSpending(student.id, today.value.toString(), spentSen) }
    }

    fun adjustSpentToday(deltaSen: Long) = setSpentToday(uiState.value.spentTodaySen + deltaSen)

    fun convertSavings() {
        val state = uiState.value
        val student = state.student ?: return
        if (!state.canConvertSavings) {
            _message.value = "Simpan sekurang-kurangnya RM 10 untuk menebus 10 ⭐ Bintang!"
            return
        }
        viewModelScope.launch {
            if (rewards.convertSavings(student.id, state.tabungSen)) {
                _message.value = "🎉 Tahniah! RM 10 simpanan ditukarkan kepada +10 ⭐ Bintang untuk ${student.nickname}!"
            }
        }
    }

    fun togglePaid(fee: CommitmentFeeEntity) {
        viewModelScope.launch { finance.togglePaid(fee) }
    }

    fun deleteFee(fee: CommitmentFeeEntity) {
        viewModelScope.launch { finance.deleteFee(fee) }
    }

    fun saveFee(fee: CommitmentFeeEntity) {
        viewModelScope.launch { finance.saveFee(fee) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
