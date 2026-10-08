package my.slotify.app.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.slotify.app.R
import my.slotify.app.core.database.ExamEntity
import my.slotify.app.core.database.HomeworkTaskEntity
import my.slotify.app.core.database.SchoolSessionEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


enum class TaskFilter {
    ALL, PENDING, COMPLETED
}

@Composable
fun TodayDashboardScreen(
    students: List<StudentEntity>,
    selectedStudent: StudentEntity?,
    dao: SlotifyDao? = null,
    onSelectStudent: (StudentEntity) -> Unit,
    onRewardEarned: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var bagPacked by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var showFocusDialog by remember { mutableStateOf(false) }
    var showTakwimDialog by remember { mutableStateOf(false) }
    var showAddHomeworkDialog by remember { mutableStateOf(false) }

    // Fix 4.3: Guard null selectedStudent — return empty list rather than tasks with empty studentId
    val defaultTasks = remember(selectedStudent?.id) {
        if (selectedStudent == null) emptyList()
        else listOf(
            HomeworkTaskEntity(
                id = "def-1",
                studentId = selectedStudent.id,
                subjectCode = "MT",
                subjectName = "Matematik",
                title = "Buku Aktiviti Jilid 1 M/S 24-26",
                dueDateDescription = "Hari Ini",
                isCompleted = false,
                rewardStars = 5,
                notes = "Selesaikan operasi tambah 2 digit & semak bersama Ibu"
            ),
            HomeworkTaskEntity(
                id = "def-2",
                studentId = selectedStudent.id,
                subjectCode = "PI",
                subjectName = "Pendidikan Islam",
                title = "Hafazan Surah Al-Balad (Ayat 1-10)",
                dueDateDescription = "Esok",
                isCompleted = false,
                rewardStars = 10,
                notes = "Tasmik bersama Ustazah waktu pagi"
            ),
            HomeworkTaskEntity(
                id = "def-3",
                studentId = selectedStudent.id,
                subjectCode = "BM",
                subjectName = "Bahasa Melayu",
                title = "Salin Karangan 'Keluarga Bahagia'",
                dueDateDescription = "Khamis",
                isCompleted = true,
                rewardStars = 5,
                notes = "Buku Garis Tiga • Tulisan kemas"
            )
        )
    }

    val defaultExams = remember(selectedStudent?.id) {
        listOf(
            ExamEntity(
                id = "def-exam-1",
                studentId = selectedStudent?.id ?: "",
                subjectCode = "SN",
                subjectName = "Sains",
                title = "Ujian Akhir Sesi Akademik (UASA) Sains",
                examDate = "25 Sep 2026",
                daysRemaining = 4,
                venue = "Makmal Sains SK",
                syllabusNote = "Unit 1 - Unit 4: Inkuiri, Manusia & Haiwan"
            ),
            ExamEntity(
                id = "def-exam-2",
                studentId = selectedStudent?.id ?: "",
                subjectCode = "MT",
                subjectName = "Matematik",
                title = "Pentaksiran Bilik Darjah (PBD) Matematik",
                examDate = "28 Sep 2026",
                daysRemaining = 7,
                venue = "Bilik Darjah",
                syllabusNote = "Operasi Tambah, Tolak & Wang RM"
            )
        )
    }

    var fallbackTasks by remember(selectedStudent?.id) {
        mutableStateOf(defaultTasks)
    }

    val liveTasks by (dao?.getHomeworkTasksForStudent(selectedStudent?.id ?: "")
        ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    val tasks = if (liveTasks.isNotEmpty()) liveTasks else fallbackTasks

    val liveExams by (dao?.getExamsForStudent(selectedStudent?.id ?: "")
        ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    val exams = if (liveExams.isNotEmpty()) liveExams else defaultExams

    val filteredTasks = when (selectedFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
    }

    // Fix 2.1 & 2.3: Collect today's timetable slots from DB for the widget and bag helper
    val todayDayOfWeek = remember {
        val cal = Calendar.getInstance()
        // Convert Calendar.DAY_OF_WEEK (Sun=1) to Slotify convention (Isnin=1..Jumaat=5)
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> 1
            Calendar.TUESDAY   -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY  -> 4
            Calendar.FRIDAY    -> 5
            else               -> 1
        }
    }
    val tomorrowDayOfWeek = if (todayDayOfWeek >= 5) 1 else todayDayOfWeek + 1

    val liveSessions by (dao?.getSessionsForStudent(selectedStudent?.id ?: "")
        ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    // Use first session's slots (primary school) for the widget; combine all for bag helper
    val primarySessionId = liveSessions.firstOrNull()?.id ?: ""

    val todaySlots by (if (primarySessionId.isNotEmpty())
        dao?.getSlotsForDay(primarySessionId, todayDayOfWeek)?.collectAsState(initial = emptyList())
        else null) ?: remember { mutableStateOf(emptyList()) }

    val tomorrowSlots by (if (primarySessionId.isNotEmpty())
        dao?.getSlotsForDay(primarySessionId, tomorrowDayOfWeek)?.collectAsState(initial = emptyList())
        else null) ?: remember { mutableStateOf(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        // Child Profile Switcher Tabs (30% Secondary Structure)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                items(students) { student ->
                    val isSelected = student.id == selectedStudent?.id
                    Box(
                        modifier = Modifier
                            .neumorphicClickable(cornerRadius = SlotifyShapes.LargeDp) { onSelectStudent(student) }
                            .background(
                                if (isSelected) SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f) else SlotifyColors.SurfaceCard,
                                SlotifyShapes.Large
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = student.avatarEmoji, fontSize = 20.sp)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = student.nickname,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = student.currentLevel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ============================================================
        // 🗓️ KPM 2026/2027 ACADEMIC CALENDAR & HOLIDAYS BANNER
        // ============================================================
        item {
            val todayDate = remember { java.time.LocalDate.now() }
            val currentHoliday = remember { my.slotify.app.core.takwim.KpmTakwimRepository.isHoliday(todayDate) }
            val nextHoliday = remember { my.slotify.app.core.takwim.KpmTakwimRepository.getUpcomingHolidays(todayDate, limit = 1).firstOrNull() }
            val currentTerm = remember { my.slotify.app.core.takwim.KpmTakwimRepository.getCurrentTerm(todayDate) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .clickable { showTakwimDialog = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentHoliday != null) currentHoliday.emoji else (nextHoliday?.emoji ?: "🗓️"),
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (currentHoliday != null) "Cuti: ${currentHoliday.titleBm}" else "Takwim KPM • ${currentTerm?.name ?: "Sesi 2026/2027"}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (currentHoliday != null) SlotifyColors.ScienceGreen else SlotifyColors.TextPrimary
                                )
                            }
                            if (currentHoliday != null) {
                                Text(
                                    text = "Tiada sesi persekolahan hari ini • Selamat bercuti!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.ScienceGreen
                                )
                            } else if (nextHoliday != null) {
                                val days = nextHoliday.daysUntil(todayDate)
                                val text = if (days <= 0) "Hari Ini!" else "lagi $days hari"
                                Text(
                                    text = "Cuti seterusnya: ${nextHoliday.titleBm} ($text)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.KpmBlue
                                )
                            }
                        }
                    }

                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        color = SlotifyColors.KpmBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Kumpulan B",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.KpmBlue
                        )
                    }
                }
            }
        }

        // ============================================================
        // 🌟 PILLAR 1: MYSTUDYLIFE REALTIME CLASS & TIMELINE PROGRESS
        // ============================================================
        item {
            InHomeRealtimeClassWidget(
                selectedStudent = selectedStudent,
                todaySlots = todaySlots
            )
        }

        // ============================================================
        // 🌟 PILLAR 2: MYSTUDYLIFE TASKS & HOMEWORK TRACKER
        // ============================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header with Task Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📝", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.homeworkTrackerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SlotifyColors.TextPrimary
                            )
                        }

                        val pendingCount = tasks.count { !it.isCompleted }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { showAddHomeworkDialog = true },
                                shape = SlotifyShapes.Small,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f),
                                    contentColor = SlotifyColors.KpmSkyBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tugasan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = if (pendingCount > 0) SlotifyColors.ActionAmber.copy(alpha = 0.15f) else SlotifyColors.SuccessMint.copy(alpha = 0.15f),
                                shape = SlotifyShapes.Small
                            ) {
                                Text(
                                    text = if (pendingCount > 0) "$pendingCount ${strings.pendingTasks}" else "Semua Siap! 🎉",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (pendingCount > 0) SlotifyColors.ActionAmber else SlotifyColors.SuccessMint,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Filter Chips: Semua / Belum Siap / Selesai
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            TaskFilter.ALL to strings.allTasks,
                            TaskFilter.PENDING to strings.pendingTasks,
                            TaskFilter.COMPLETED to strings.completedTasks
                        ).forEach { (filter, label) ->
                            val isSel = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(SlotifyShapes.Small)
                                    .background(if (isSel) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
                                    .clickable { selectedFilter = filter }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)

                    // Task List Items
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (filteredTasks.isEmpty()) {
                            SlotifyEmptyState(
                                emoji = if (selectedFilter == TaskFilter.COMPLETED) "🎉" else "📚",
                                message = if (selectedFilter == TaskFilter.COMPLETED)
                                    "Belum ada tugasan yang siap lagi. Teruskan!"
                                else
                                    "Tiada tugasan buat masa ini. Syabas! ⭐"
                            )
                        }
                        filteredTasks.forEach { task ->
                            // Fix 4.1: Use @Composable subjectColor() for theme-aware badge colors
                            val subjectColor = SlotifyColors.subjectColor(task.subjectCode)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(SlotifyShapes.Medium)
                                    .background(SlotifyColors.CanvasBase)
                                    .clickable {
                                        coroutineScope.launch {
                                            if (liveTasks.any { it.id == task.id }) {
                                                dao?.setHomeworkCompleted(task.id, !task.isCompleted)
                                            } else {
                                                fallbackTasks = fallbackTasks.map {
                                                    if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
                                                }
                                            }
                                            if (!task.isCompleted) {
                                                onRewardEarned(task.rewardStars)
                                            }
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Checkbox with tactile styling
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (task.isCompleted) SlotifyColors.SuccessMint else Color.Transparent
                                                )
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (task.isCompleted) SlotifyColors.SuccessMint else SlotifyColors.TextTertiary,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (task.isCompleted) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Subject badge
                                                Surface(
                                                    color = subjectColor.copy(alpha = 0.15f),
                                                    shape = SlotifyShapes.XSmall
                                                ) {
                                                    Text(
                                                        text = task.subjectCode,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = subjectColor,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (task.isCompleted) SlotifyColors.TextSecondary else SlotifyColors.TextPrimary,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            }

                                            if (task.notes.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = task.notes,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SlotifyColors.TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Due Date & Star Reward Tag
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = task.dueDateDescription,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (task.dueDateDescription == "Hari Ini") SlotifyColors.EnglishRed else SlotifyColors.ActionAmber
                                        )
                                        Text(
                                            text = "+${task.rewardStars} ⭐",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.StarGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ============================================================
        // 🌟 PILLAR 3: MYSTUDYLIFE EXAMS & ASSESSMENTS COUNTDOWN
        // ============================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.examsTrackerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SlotifyColors.TextPrimary
                            )
                        }

                        Text(
                            text = "${exams.size} Ujian Dikesan",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SlotifyColors.TextSecondary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (exams.isEmpty()) {
                            SlotifyEmptyState(
                                emoji = "🎉",
                                message = "Tiada ujian berdaftar. Teruskan usaha!"
                            )
                        }
                        exams.forEach { exam ->
                            // Fix 4.1: Use @Composable subjectColor() for theme-aware color resolution
                            val subjectColor = SlotifyColors.subjectColor(exam.subjectCode)
                            // Fix 2.2: Compute daysRemaining at runtime from examDate string
                            val daysLeft = remember(exam.examDate) {
                                try {
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    val examMillis = sdf.parse(exam.examDate)?.time ?: return@remember exam.daysRemaining
                                    val todayStart = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    ((examMillis - todayStart) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                                } catch (e: Exception) { exam.daysRemaining }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(SlotifyShapes.Medium)
                                    .background(SlotifyColors.CanvasBase)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Days Remaining Circle Badge
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (daysLeft <= 4) SlotifyColors.EnglishRed.copy(alpha = 0.15f)
                                                    else SlotifyColors.ActionAmber.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$daysLeft",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = if (daysLeft <= 4) SlotifyColors.EnglishRed else SlotifyColors.ActionAmber
                                                )
                                                Text(
                                                    text = "HARI",
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (daysLeft <= 4) SlotifyColors.EnglishRed else SlotifyColors.ActionAmber
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = subjectColor.copy(alpha = 0.15f),
                                                    shape = SlotifyShapes.XSmall
                                                ) {
                                                    Text(
                                                        text = exam.subjectCode,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = subjectColor,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    // Fix 5.2: Simplify formal exam titles for kid-friendly display
                                                    text = exam.title
                                                        .replace("Ujian Akhir Sesi Akademik (UASA)", "Peperiksaan Besar")
                                                        .replace("Pentaksiran Bilik Darjah (PBD)", "Ujian Kelas")
                                                        .replace("Pentaksiran", "Ujian"),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SlotifyColors.TextPrimary
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${exam.venue} • ${exam.syllabusNote}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlotifyColors.TextSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = exam.examDate,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlotifyColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ============================================================
        // 🌟 POMODORO STUDY FOCUS TIMER BANNER (MOD FOKUS ULANGKAJI)
        // ============================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .clip(SlotifyShapes.Large)
                    .clickable { showFocusDialog = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SlotifyColors.KokuPurple.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⏱️", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.focusTimerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                text = strings.focusTimerSubtitle,
                                fontSize = 10.5.sp,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showFocusDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Mula Fokus",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ============================================================
        // 🌟 TRANSIT & LOGISTICS SUPERPOWER
        // ============================================================
        // Morning Transit Drop Reminder (Parent & Student Friendly)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SlotifyColors.IslamTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎒", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.morningTransitTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = strings.morningTransitDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Ongoing Session Card with 10% Radiant Accent & Van Status
        item {
            val vanName = selectedStudent?.vanDriverName ?: "Van Sekolah"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_active_slot),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.ActionAmber // 10% Radiant Action Glow
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = SlotifyColors.VanPrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = vanName,
                                style = MaterialTheme.typography.labelSmall,
                                color = SlotifyColors.VanPrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Sains — Tahun 4 (Makmal)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = "03:10 PM – 04:10 PM (Sesi Petang SK)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlotifyColors.TextSecondary
                    )
                }
            }
        }

        // Daily Attire Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👕", fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.section_attire),
                            style = MaterialTheme.typography.titleMedium,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = "Isnin: Uniform Lengkap KPM (Kemeja Putih + Seluar Biru)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }
            }
        }

        // School Bag Pack Helper (10% Accent CTA Action)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.section_bag_pack),
                        style = MaterialTheme.typography.titleMedium,
                        color = SlotifyColors.TextPrimary
                    )

                    // Fix 2.3: Derive tomorrow's books from real timetable instead of hardcoded list
                    val books = if (tomorrowSlots.isNotEmpty()) {
                        tomorrowSlots
                            .filter { !it.isRehat && (it.requiresTextbook || it.requiresWorkbook) }
                            .map { slot ->
                                val type = buildString {
                                    if (slot.requiresTextbook) append("Buku Teks")
                                    if (slot.requiresTextbook && slot.requiresWorkbook) append(" + ")
                                    if (slot.requiresWorkbook) append("Buku Aktiviti")
                                }
                                "📚 ${slot.subjectName} (${slot.subjectCode}) — $type"
                            }
                            .distinct()
                    } else {
                        // Fallback when DB has no slots yet
                        listOf(
                            "📘 Buku Teks Bahasa Melayu Jilid 1",
                            "📙 Buku Aktiviti Matematik",
                            "📗 Buku Latihan Sains",
                            "📕 Buku Teks English"
                        )
                    }
                    books.forEach { book ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SlotifyColors.SuccessMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = book, style = MaterialTheme.typography.bodySmall, color = SlotifyColors.TextPrimary)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 10% Radiant Accent Action Button
                    Button(
                        onClick = {
                            if (!bagPacked) {
                                bagPacked = true
                                onRewardEarned(5)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bagPacked) SlotifyColors.SuccessMint else SlotifyColors.ActionAmber
                        ),
                        shape = SlotifyShapes.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = if (bagPacked) stringResource(R.string.bag_packed_done) else stringResource(R.string.btn_bag_packed),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Routine Notification & Automation Card
        item {
            val context = LocalContext.current
            var notifFeedback by remember { mutableStateOf<String?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Automasi & Notifikasi Rutin",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = "Jadual harian penggera & peringatan telefon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = SlotifyShapes.Small,
                            color = SlotifyColors.SuccessMint.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.SuccessMint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (notifFeedback != null) {
                        Surface(
                            shape = SlotifyShapes.Small,
                            color = SlotifyColors.KpmSkyBlue.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = notifFeedback!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlotifyColors.KpmSkyBlue,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                my.slotify.app.core.notification.SlotifyNotificationManager.showTransitAlert(
                                    context = context,
                                    childName = selectedStudent?.nickname ?: "Adam & Adam",
                                    transitMsg = "Hantar Beg Transit & Baju SK ke Taska Ceria!"
                                )
                                notifFeedback = "Notifikasi 07:00 AM telah dihantar ke bar notifikasi!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ActionAmber),
                            shape = SlotifyShapes.Small,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Text("🔔 Uji 07:00 AM\nBeg Transit", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                my.slotify.app.core.notification.SlotifyNotificationManager.showAttireReminder(
                                    context = context,
                                    childName = selectedStudent?.nickname ?: "Adam",
                                    attire = "Baju T-Shirt Sukan SK / Seluar Track",
                                    books = "Buku Teks Matematik, Sains, B. Melayu"
                                )
                                notifFeedback = "Notifikasi 08:30 PM pakaian esok telah dihantar!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                            shape = SlotifyShapes.Small,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Text("👕 Uji 08:30 PM\nUniform Esok", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // FAB Bottom Clearance
        item {
            Spacer(Modifier.height(70.dp))
        }
        }

        // ============================================================
        // 🌟 FOCUS STUDY TIMER DIALOG (POMODORO MODAL OVERLAY)
        // ============================================================
        if (showFocusDialog) {
            FocusTimerModalDialog(
                childName = selectedStudent?.nickname ?: "Anak",
                onDismiss = { showFocusDialog = false },
                onComplete = {
                    showFocusDialog = false
                    onRewardEarned(10)
                }
            )
        }

        if (showTakwimDialog) {
            my.slotify.app.feature.takwim.TakwimCalendarDialog(
                onDismissRequest = { showTakwimDialog = false }
            )
        }

        if (showAddHomeworkDialog && selectedStudent != null) {
            AddHomeworkTaskDialog(
                studentId = selectedStudent.id,
                onDismissRequest = { showAddHomeworkDialog = false },
                onSaveTask = { newTask ->
                    coroutineScope.launch {
                        dao?.insertHomeworkTask(newTask)
                        fallbackTasks = listOf(newTask) + fallbackTasks
                        showAddHomeworkDialog = false
                    }
                }
            )
        }
    }
}

/**
 * Fix 2.1: Tactical In-Home Realtime Widget showing current class & next upcoming subject.
 * Now reads live timetable data and a live clock (refreshes every minute).
 */
@Composable
fun InHomeRealtimeClassWidget(
    selectedStudent: StudentEntity?,
    todaySlots: List<TimetableSlotEntity> = emptyList()
) {
    // Fix 2.1: Live clock that refreshes every minute
    var nowHHMM by remember {
        val c = Calendar.getInstance()
        mutableStateOf(c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            val c = Calendar.getInstance()
            nowHHMM = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        }
    }

    // Parse "08:30" -> minutes since midnight
    fun parseTime(t: String): Int = try {
        val parts = t.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) { 0 }

    val activeSlot = todaySlots.firstOrNull { slot ->
        !slot.isRehat &&
        parseTime(slot.startTime) <= nowHHMM &&
        parseTime(slot.endTime) > nowHHMM
    }
    val nextSlot = todaySlots.firstOrNull { slot ->
        !slot.isRehat && parseTime(slot.startTime) > nowHHMM
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Widget Title & Live Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SlotifyColors.SuccessMint)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.realtimeWidgetTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(SlotifyColors.CanvasBase, SlotifyShapes.Small)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = strings.todayDaySuffix,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlotifyColors.KpmSkyBlue
                    )
                }
            }

            HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)

            // Current Class (Sedang Berjalan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (activeSlot != null) {
                        val baki = parseTime(activeSlot.endTime) - nowHHMM
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔴 ${strings.ongoingClass}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.EnglishRed
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${activeSlot.startTime} – ${activeSlot.endTime} (Baki: ${baki}min)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${activeSlot.subjectName} (${activeSlot.subjectCode})",
                            style = MaterialTheme.typography.titleLarge,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = buildString {
                                activeSlot.teacherName?.let { append("$it • ") }
                                append(selectedStudent?.className?.ifBlank { "Bilik Darjah" } ?: "Bilik Darjah")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotifyColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "🟢 ${strings.ongoingClass}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.SuccessMint
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (todaySlots.isEmpty()) "Jadual belum disediakan" else "Tiada kelas sekarang",
                            style = MaterialTheme.typography.titleLarge,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = selectedStudent?.nickname ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }

                // Fix 3.3: SlotifyShapes.Small replaces raw SlotifyShapes.Small
                val badgeColor = if (activeSlot != null)
                    SlotifyColors.subjectColor(activeSlot.subjectCode)
                else SlotifyColors.TextTertiary
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(badgeColor.copy(alpha = 0.15f), SlotifyShapes.Small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeSlot?.subjectCode ?: "–",
                        style = MaterialTheme.typography.titleMedium,
                        color = badgeColor
                    )
                }
            }

            // Next Class (Seterusnya)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Fix 3.3: SlotifyShapes.Small replaces raw SlotifyShapes.Small
                    .background(SlotifyColors.CanvasBase, SlotifyShapes.Small)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏩", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                // Fix 2.1: Real next class from timetable
                                text = if (nextSlot != null)
                                    "${strings.nextClassPrefix} ${nextSlot.subjectName} (${nextSlot.subjectCode}) • ${nextSlot.startTime}"
                                else
                                    "${strings.nextClassPrefix} ${if (todaySlots.isEmpty()) "(jadual belum ada)" else "Tiada kelas seterusnya"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SlotifyColors.TextPrimary
                            )
                            if (nextSlot != null && nextSlot.requiresTextbook) {
                                Text(
                                    text = "${strings.requiredBookPrefix} Buku Teks ${nextSlot.subjectName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.ActionAmber
                                )
                            }
                        }
                    }
                    Icon(
                        // Fix deprecation: use AutoMirrored variant for RTL support
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = SlotifyColors.TextTertiary
                    )
                }
            }
        }
    }
}

/**
 * Fix 3.1: Reusable empty state composable for lists with no items.
 * Shows a centred emoji + message to avoid blank whitespace confusion.
 */
@Composable
fun SlotifyEmptyState(emoji: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = emoji, fontSize = 32.sp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = SlotifyColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Child Focus Study Timer Modal Dialog (Pomodoro for School Revision)
 */
@Composable
fun FocusTimerModalDialog(
    childName: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(20) }
    var secondsRemaining by remember { mutableStateOf(selectedMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    // Reset the seconds when a new duration is selected (only when not running)
    LaunchedEffect(selectedMinutes) {
        if (!isRunning) {
            secondsRemaining = selectedMinutes * 60
        }
    }

    // Fix 3.5: Single coroutine with while-loop — avoids spawning 60 coroutines/min
    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
            if (secondsRemaining <= 0) {
                isRunning = false
                isFinished = true
            }
        }
    }

    // Fix 3.4: Safe dismiss resets all timer state before closing
    val safeDismiss: () -> Unit = {
        isRunning = false
        isFinished = false
        secondsRemaining = selectedMinutes * 60
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = safeDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
                .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 22.dp)
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.focusTimerTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SlotifyColors.TextPrimary
                        )
                    }

                    // Fix 3.2: Removed size(24.dp) override — IconButton is natively 48×48dp
                    IconButton(onClick = safeDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary)
                    }
                }

                if (!isFinished) {
                    Text(
                        text = "Sesi ulangkaji berjadual untuk $childName",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlotifyColors.TextSecondary
                    )

                    // Preset buttons (15m, 20m, 25m)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(15, 20, 25).forEach { mins ->
                            val isSel = selectedMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(SlotifyShapes.Small)
                                    .background(if (isSel) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
                                    .clickable {
                                        if (!isRunning) {
                                            selectedMinutes = mins
                                        }
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins Minit",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSel) Color.White else SlotifyColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Circular Countdown Timer Display
                    val minutesLeft = secondsRemaining / 60
                    val secsLeft = secondsRemaining % 60
                    val timeString = String.format("%02d:%02d", minutesLeft, secsLeft)

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(SlotifyColors.CanvasBase, CircleShape)
                            .border(4.dp, SlotifyColors.KpmSkyBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeString,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                text = if (isRunning) "SEDANG BERJALAN" else "BERSEDIA",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) SlotifyColors.SuccessMint else SlotifyColors.ActionAmber
                            )
                        }
                    }

                    // Action Controls: Start / Pause / Reset
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { isRunning = !isRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) SlotifyColors.ActionAmber else SlotifyColors.SuccessMint
                            ),
                            shape = SlotifyShapes.Medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isRunning) strings.pauseFocus else strings.startFocus,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                secondsRemaining = selectedMinutes * 60
                            },
                            shape = SlotifyShapes.Medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = strings.resetFocus,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }
                } else {
                    // Finished Celebration State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎉 🌟 👏", fontSize = 36.sp)
                        Text(
                            text = strings.focusCompleteTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = strings.focusCompleteBody,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            color = SlotifyColors.TextSecondary
                        )
                        Text(
                            text = "+10 ⭐",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = SlotifyColors.StarGold
                        )
                        Button(
                            onClick = onComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.SuccessMint),
                            shape = SlotifyShapes.Medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tuntut Bintang!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddHomeworkTaskDialog(
    studentId: String,
    onDismissRequest: () -> Unit,
    onSaveTask: (HomeworkTaskEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("MT") }
    var dueDateDesc by remember { mutableStateOf("Hari Ini") }
    var notes by remember { mutableStateOf("") }
    var hasPhotoProof by remember { mutableStateOf(false) }

    val subjects = listOf("BM", "MT", "SN", "BI", "PI", "BA", "SEJ", "RBT")
    val dueDates = listOf("Hari Ini", "Esok", "Isnin", "Khamis")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = SlotifyColors.CanvasBase,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah Tugasan & Kerja Rumah",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary)
                    }
                }

                // Subject Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Subjek KPM:", style = MaterialTheme.typography.labelSmall, color = SlotifyColors.TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { code ->
                            FilterChip(
                                selected = selectedSubject == code,
                                onClick = { selectedSubject = code },
                                label = { Text(code, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tugasan / Muka Surat") },
                    placeholder = { Text("Contoh: Buku Aktiviti Jilid 1 M/S 30") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )

                // Due date selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tarikh Hantar:", style = MaterialTheme.typography.labelSmall, color = SlotifyColors.TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dueDates) { due ->
                            FilterChip(
                                selected = dueDateDesc == due,
                                onClick = { dueDateDesc = due },
                                label = { Text(due, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Worksheet Camera Capture Simulation Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .background(SlotifyColors.SurfaceCard)
                        .clickable { hasPhotoProof = !hasPhotoProof }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (hasPhotoProof) "📸" else "📷", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (hasPhotoProof) "Gambar Lembaran Kerja Dilampirkan" else "Tangkap Gambar Lembaran Kerja",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (hasPhotoProof) SlotifyColors.ScienceGreen else SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = if (hasPhotoProof) "lembaran_kerja_aktiviti.jpg (Disahkan)" else "Sentuh untuk simulasi kamera tangkapan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }

                        if (hasPhotoProof) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SlotifyColors.ScienceGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota Tambahan (Pilihan)") },
                    placeholder = { Text("Contoh: Semak bersama Ibu sebelum tidur") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newTask = HomeworkTaskEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    studentId = studentId,
                                    subjectCode = selectedSubject,
                                    subjectName = my.slotify.app.core.model.KpmSubjectCatalog.getSubject(selectedSubject).nameBm,
                                    title = title.trim(),
                                    dueDateDescription = dueDateDesc,
                                    isCompleted = false,
                                    rewardStars = 5,
                                    notes = if (hasPhotoProof) "📸 [Foto Lembaran] $notes".trim() else notes.trim()
                                )
                                onSaveTask(newTask)
                            }
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue)
                    ) {
                        Text("Simpan Tugasan (+5 ⭐)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

