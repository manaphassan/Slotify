package my.slotify.app.core.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import my.slotify.app.core.database.SchoolSessionEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity

/** Students, the app-wide selected student, and their sessions/timetable. */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentRepository(
    private val dao: SlotifyDao,
    private val settings: SettingsRepository
) {
    val students: Flow<List<StudentEntity>> = dao.getAllStudents()

    /** The persisted selection, falling back to the first student. Shared by every screen. */
    val selectedStudent: Flow<StudentEntity?> =
        combine(students, settings.settings.map { it.selectedStudentId }) { list, id ->
            list.find { it.id == id } ?: list.firstOrNull()
        }

    suspend fun select(studentId: String) = settings.setSelectedStudent(studentId)

    suspend fun update(student: StudentEntity) = dao.updateStudent(student)

    fun sessions(studentId: String?): Flow<List<SchoolSessionEntity>> =
        if (studentId == null) flowOf(emptyList()) else dao.getSessionsForStudent(studentId)

    fun slotsForSession(sessionId: String?): Flow<List<TimetableSlotEntity>> =
        if (sessionId == null) flowOf(emptyList()) else dao.getAllSlotsForSession(sessionId)

    /** All of a student's slots on [schoolDay] across sessions, or empty for weekends (null). */
    fun slotsForDay(studentId: String?, schoolDay: Int?): Flow<List<TimetableSlotEntity>> =
        if (studentId == null || schoolDay == null) flowOf(emptyList())
        else dao.getStudentSlotsForDay(studentId, schoolDay)

    fun selectedStudentSessions(): Flow<List<SchoolSessionEntity>> =
        selectedStudent.map { it?.id }.flatMapLatest { sessions(it) }

    suspend fun addSlot(slot: TimetableSlotEntity) = dao.insertSlots(listOf(slot))
    suspend fun deleteSlot(slot: TimetableSlotEntity) = dao.deleteSlot(slot)
}
