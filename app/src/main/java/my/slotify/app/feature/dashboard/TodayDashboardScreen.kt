package my.slotify.app.feature.dashboard

import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.slotify.app.R
import my.slotify.app.core.database.ExamEntity
import my.slotify.app.core.database.HomeworkTaskEntity
import my.slotify.app.core.database.SchoolSessionEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.designsystem.component.slotifySection
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.designsystem.theme.LocalAppLanguage
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import kotlinx.coroutines.flow.flowOf
import my.slotify.app.core.model.AttireGuide
import my.slotify.app.core.profile.FamilyProfileData
import my.slotify.app.core.rewards.RewardKeys
import my.slotify.app.core.rewards.StarRewards
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.slotify.app.core.di.SlotifyViewModelFactory
import my.slotify.app.core.time.ExamDates
import my.slotify.app.core.time.SchoolDay
import my.slotify.app.core.time.rememberNow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


enum class TaskFilter {
    ALL, PENDING, COMPLETED
}

@Composable
fun TodayDashboardScreen(
    students: List<StudentEntity>,
    onSelectStudent: (StudentEntity) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = SlotifyViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var showFocusDialog by rememberSaveable { mutableStateOf(false) }
    var showTakwimDialog by remember { mutableStateOf(false) }
    var showAddHomeworkDialog by remember { mutableStateOf(false) }
    var showSchoolBagDialog by remember { mutableStateOf(false) }
    var nowOpen by rememberSaveable { mutableStateOf(true) }
    var prepOpen by rememberSaveable { mutableStateOf(true) }
    var academicOpen by rememberSaveable { mutableStateOf(true) }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    var routineOpen by rememberSaveable { mutableStateOf(false) }

    val now by rememberNow()
    LaunchedEffect(now.toLocalDate()) { viewModel.setToday(now.toLocalDate()) }

    val str = strings
    val ds = str.dashboardUi
    val appLanguage = LocalAppLanguage.current
    val isEnglish = appLanguage == AppLanguage.EN

    val selectedStudent = state.student
    val tasks = state.tasks
    val exams = remember(state.exams, state.today) {
        state.exams.sortedWith(
            compareBy<ExamEntity>(
                { (ExamDates.daysUntil(it.examDate, state.today) ?: 0L) < 0L },
                { ExamDates.parse(it.examDate) })
        )
    }
    val upcomingExamCount = exams.count { (ExamDates.daysUntil(it.examDate, state.today) ?: 0L) >= 0L }
    val today = state.today
    // School-day convention: 1=Isnin..5=Jumaat, null on weekends (no "today" timetable).
    val todayDayOfWeek: Int? = state.todaySchoolDay
    val nextSchoolDayOfWeek: Int = state.nextSchoolDay
    // All of the student's sessions (e.g. SRA pagi + SK petang), ordered by start time.
    val todaySlots = state.todaySlots
    val tomorrowSlots = state.nextDaySlots
    // Bag-packed state is the persisted reward claim for today (shared with the checklist dialog).
    val bagPacked = state.bagPacked
    fun claimBagPacked() = viewModel.markBagPacked()

    val filteredTasks = when (selectedFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Child Profile Switcher Tabs (30% Secondary Structure)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = student.avatarEmoji, fontSize = 18.sp)
                                Spacer(Modifier.width(5.dp))
                                Column {
                                    Text(
                                        text = student.nickname,
                                        style = MaterialTheme.typography.titleSmall,
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

            slotifySection(
                key = "now",
                title = ds.groupNow,
                expanded = nowOpen,
                onToggle = { nowOpen = !nowOpen }
            ) {
                // ============================================================
                // 🌟 PILLAR 1: MYSTUDYLIFE REALTIME CLASS & TIMELINE PROGRESS
                // ============================================================
                item {
                    InHomeRealtimeClassWidget(
                        selectedStudent = selectedStudent,
                        todaySlots = todaySlots
                    )
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
                                    text = if (todayDayOfWeek == null) strings.tomorrowAttireTitle else strings.sectionAttire,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = AttireGuide.label(todayDayOfWeek ?: nextSchoolDayOfWeek, appLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "prep",
                title = ds.groupPrep,
                expanded = prepOpen,
                onToggle = { prepOpen = !prepOpen }
            ) {
                // ============================================================
                // 🎒 SMART SCHOOL BAG PACK CHECKLIST
                // ============================================================
                item {
                    val isPacked = bagPacked
                    val successColor = SlotifyColors.SuccessMint
                    val surfaceColor = SlotifyColors.SurfaceCard
                    val skyBlueColor = SlotifyColors.KpmSkyBlue
                    val primaryTextColor = SlotifyColors.TextPrimary
                    val secondaryTextColor = SlotifyColors.TextSecondary

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                            .background(
                                if (isPacked) successColor.copy(alpha = 0.1f) else surfaceColor,
                                SlotifyShapes.Large
                            )
                            .clickable { showSchoolBagDialog = true }
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (isPacked) "🎒" else "📚", fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isPacked) ds.bagCardPackedTitle else ds.bagCardPendingTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPacked) successColor else primaryTextColor
                                    )
                                    Text(
                                        text = if (isPacked) ds.bagCardPackedSubtitle else ds.bagCardPendingSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryTextColor
                                    )
                                }
                            }

                            Surface(
                                shape = SlotifyShapes.Small,
                                color = if (isPacked) successColor.copy(alpha = 0.2f) else skyBlueColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isPacked) ds.bagCardPackedBadge else ds.bagCardPendingBadge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isPacked) successColor else skyBlueColor
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
                                    text = FamilyProfileData.morningTransitReminder ?: strings.morningTransitDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "academic",
                title = ds.groupAcademic,
                expanded = academicOpen,
                onToggle = { academicOpen = !academicOpen },
                summary = ds.groupAcademicSummary(tasks.count { !it.isCompleted }, upcomingExamCount)
            ) {
                // ============================================================
                // 🌟 PILLAR 2: MYSTUDYLIFE TASKS & HOMEWORK TRACKER
                // ============================================================
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(ds.addTaskButton, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        color = if (pendingCount > 0) SlotifyColors.ActionAmber.copy(alpha = 0.15f) else SlotifyColors.SuccessMint.copy(
                                            alpha = 0.15f
                                        ),
                                        shape = SlotifyShapes.Small
                                    ) {
                                        Text(
                                            text = if (pendingCount > 0) "$pendingCount ${strings.pendingTasks}" else ds.allTasksDone,
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
                                            ds.emptyCompletedTasks
                                        else
                                            ds.emptyPendingTasks
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
                                            .toggleable(
                                                value = task.isCompleted,
                                                role = Role.Checkbox,
                                                onValueChange = { viewModel.toggleHomework(task) }
                                            )
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
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = subjectColor,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 5.dp,
                                                                    vertical = 1.dp
                                                                )
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
                                                    fontSize = 10.sp,
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
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    text = ds.examsDetectedCount(exams.size),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = SlotifyColors.TextSecondary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (exams.isEmpty()) {
                                    SlotifyEmptyState(
                                        emoji = "🎉",
                                        message = ds.emptyExams
                                    )
                                }
                                exams.forEach { exam ->
                                    // Fix 4.1: Use @Composable subjectColor() for theme-aware color resolution
                                    val subjectColor = SlotifyColors.subjectColor(exam.subjectCode)
                                    // Fix 2.2: Compute daysRemaining at runtime from examDate string
                                    val rawDays = remember(exam.examDate, today) {
                                        ExamDates.daysUntil(exam.examDate, today)?.toInt()
                                    }
                                    val isPast = rawDays != null && rawDays < 0
                                    val daysLeft = rawDays?.coerceAtLeast(0) ?: exam.daysRemaining
                                    val badgeColor = when {
                                        isPast -> SlotifyColors.TextSecondary
                                        daysLeft <= 4 -> SlotifyColors.EnglishRed
                                        else -> SlotifyColors.ActionAmber
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
                                                        .background(badgeColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = if (isPast) "✓" else "$daysLeft",
                                                            style = MaterialTheme.typography.titleLarge,
                                                            color = badgeColor
                                                        )
                                                        Text(
                                                            text = if (isPast) ds.examPastLabel else ds.examDaysUnit,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeColor
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
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = subjectColor,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 5.dp,
                                                                    vertical = 1.dp
                                                                )
                                                            )
                                                        }
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            // Fix 5.2: Simplify formal exam titles for kid-friendly display
                                                            text = exam.title
                                                                .replace(
                                                                    "Ujian Akhir Sesi Akademik (UASA)",
                                                                    ds.examFriendlyUasa
                                                                )
                                                                .replace(
                                                                    "Pentaksiran Bilik Darjah (PBD)",
                                                                    ds.examFriendlyPbd
                                                                )
                                                                .replace("Pentaksiran", ds.examFriendlyAssessment),
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
                                                fontSize = 10.sp,
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
                            .padding(12.dp)
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
                                    text = strings.startFocus,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "info",
                title = ds.groupInfo,
                expanded = infoOpen,
                onToggle = { infoOpen = !infoOpen }
            ) {
                // ============================================================
                // 🗓️ KPM 2026/2027 ACADEMIC CALENDAR & HOLIDAYS BANNER
                // ============================================================
                item {
                    val todayDate = remember { java.time.LocalDate.now() }
                    val currentHoliday =
                        remember { my.slotify.app.core.takwim.KpmTakwimRepository.isHoliday(todayDate) }
                    val nextHoliday = remember {
                        my.slotify.app.core.takwim.KpmTakwimRepository.getUpcomingHolidays(todayDate, limit = 1)
                            .firstOrNull()
                    }
                    val currentTerm =
                        remember { my.slotify.app.core.takwim.KpmTakwimRepository.getCurrentTerm(todayDate) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                            .clickable { showTakwimDialog = true }
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (currentHoliday != null) currentHoliday.emoji else (nextHoliday?.emoji
                                        ?: "🗓️"),
                                    fontSize = 22.sp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (currentHoliday != null) ds.holidayBannerTitle(if (isEnglish) currentHoliday.titleEn else currentHoliday.titleBm) else ds.takwimBannerTitle(
                                                currentTerm?.name ?: ds.takwimSessionFallback
                                            ),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (currentHoliday != null) SlotifyColors.ScienceGreen else SlotifyColors.TextPrimary
                                        )
                                    }
                                    if (currentHoliday != null) {
                                        Text(
                                            text = ds.holidayNoSchoolToday,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.ScienceGreen
                                        )
                                    } else if (nextHoliday != null) {
                                        val days = nextHoliday.daysUntil(todayDate)
                                        val text =
                                            if (days <= 0) ds.holidayCountdownToday else ds.holidayCountdownDays(days)
                                        Text(
                                            text = ds.nextHolidayLabel(
                                                if (isEnglish) nextHoliday.titleEn else nextHoliday.titleBm,
                                                text
                                            ),
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
                                    text = ds.schoolGroupBadge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.KpmBlue
                                )
                            }
                        }
                    }
                }

                // ============================================================
                // 🕌 WAKTU SOLAT JAKIM ZON SGR01 (KUALA LANGAT / SEPANG)
                // ============================================================
                item {
                    val prayerSchedule = remember { my.slotify.app.core.takwim.JakimPrayerTimeHelper.getSchedule() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🕌", fontSize = 16.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = ds.prayerTimesTitle("SGR01"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary
                                        )
                                        Text(
                                            text = FamilyProfileData.prayerZoneLabel,
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
                                        text = "${prayerSchedule.nextPrayerName}: ${ds.prayerCountdown(prayerSchedule.minutesUntilNext)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.SuccessMint,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // 5 Prayer Times Horizontal Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    "Subuh" to prayerSchedule.subuh,
                                    "Zohor" to prayerSchedule.zohor,
                                    "Asar" to prayerSchedule.asar,
                                    "Maghrib" to prayerSchedule.maghrib,
                                    "Isyak" to prayerSchedule.isyak
                                ).forEach { (name, time) ->
                                    val isCurrent = prayerSchedule.currentPrayer == name
                                    Surface(
                                        shape = SlotifyShapes.Small,
                                        color = if (isCurrent) SlotifyColors.KpmSkyBlue.copy(alpha = 0.18f) else SlotifyColors.CanvasBase,
                                        border = if (isCurrent) BorderStroke(
                                            1.dp,
                                            SlotifyColors.KpmSkyBlue.copy(alpha = 0.6f)
                                        ) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .semantics(mergeDescendants = true) {
                                                if (isCurrent) stateDescription = ds.currentPrayerStateDescription
                                            }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 10.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) SlotifyColors.KpmSkyBlue else SlotifyColors.TextSecondary
                                            )
                                            Text(
                                                text = time.replace(" AM", "").replace(" PM", ""),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) SlotifyColors.TextPrimary else SlotifyColors.TextPrimary.copy(
                                                    alpha = 0.8f
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Transit Note
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text("🕒", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = ds.prayerTransitNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.ActionAmber,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "routine",
                title = ds.groupRoutine,
                expanded = routineOpen,
                onToggle = { routineOpen = !routineOpen }
            ) {
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
                                            text = ds.routineAutomationTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary
                                        )
                                        Text(
                                            text = ds.routineAutomationSubtitle,
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
                                        text = ds.routineActiveBadge,
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
                                            s = str.systemUi,
                                            childName = students.joinToString(" & ") { it.nickname }
                                                .ifBlank { str.childFallback },
                                            transitMsg = FamilyProfileData.morningTransitReminder
                                                ?: ds.transitMessageFallback
                                        )
                                        notifFeedback = ds.transitTestSent
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ActionAmber),
                                    shape = SlotifyShapes.Small,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        ds.testTransitButton,
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        my.slotify.app.core.notification.SlotifyNotificationManager.showAttireReminder(
                                            context = context,
                                            s = str.systemUi,
                                            childName = selectedStudent?.nickname ?: str.childFallback,
                                            attire = AttireGuide.forSchoolDay(nextSchoolDayOfWeek, appLanguage),
                                            books = tomorrowSlots.filter { !it.isRehat }.map { it.subjectName }
                                                .distinct()
                                                .joinToString(", ").ifBlank { ds.attireBooksFallback }
                                        )
                                        notifFeedback = ds.attireTestSent
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                                    shape = SlotifyShapes.Small,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        ds.testAttireButton,
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FAB Bottom Clearance
            item {
                Spacer(Modifier.height(16.dp))
            }
        }

        // ============================================================
        // 🌟 FOCUS STUDY TIMER DIALOG (POMODORO)
        // ============================================================
        if (showFocusDialog) {
            FocusTimerModalDialog(
                childName = selectedStudent?.nickname ?: str.childFallback,
                onDismiss = { showFocusDialog = false },
                onComplete = {
                    showFocusDialog = false
                    viewModel.completeFocusSession()
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
                    viewModel.addHomework(newTask)
                    showAddHomeworkDialog = false
                }
            )
        }

        if (showSchoolBagDialog && selectedStudent != null) {
            SchoolBagPackDialog(
                student = selectedStudent,
                tomorrowSlots = tomorrowSlots,
                alreadyPacked = bagPacked,
                onAllPacked = { claimBagPacked() },
                onDismiss = { showSchoolBagDialog = false }
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
        mutableIntStateOf(c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))
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
    } catch (e: Exception) {
        0
    }

    val activeSlot = todaySlots.firstOrNull { slot ->
        !slot.isRehat &&
                parseTime(slot.startTime) <= nowHHMM &&
                parseTime(slot.endTime) > nowHHMM
    }
    val nextSlot = todaySlots.firstOrNull { slot ->
        !slot.isRehat && parseTime(slot.startTime) > nowHHMM
    }
    val ds = strings.dashboardUi
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        text = strings.todayDaySuffix(
                            AttireGuide.dayName(SchoolDay.of(java.time.LocalDate.now()), LocalAppLanguage.current)
                        ),
                        fontSize = 10.sp,
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.EnglishRed
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${activeSlot.startTime} – ${activeSlot.endTime} (${ds.minutesRemaining(baki)})",
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
                                append(selectedStudent?.className?.ifBlank { ds.classroomFallback }
                                    ?: ds.classroomFallback)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotifyColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "🟢 ${strings.ongoingClass}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.SuccessMint
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (todaySlots.isEmpty()) ds.timetableNotSet else ds.noClassNow,
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
                                    "${strings.nextClassPrefix} ${if (todaySlots.isEmpty()) ds.nextTimetableMissing else ds.noNextClass}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SlotifyColors.TextPrimary
                            )
                            if (nextSlot != null && nextSlot.requiresTextbook) {
                                Text(
                                    text = "${strings.requiredBookPrefix} ${ds.textbookFor(nextSlot.subjectName)}",
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
    var selectedMinutes by rememberSaveable { mutableIntStateOf(20) }
    var secondsRemaining by rememberSaveable { mutableIntStateOf(selectedMinutes * 60) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var isFinished by rememberSaveable { mutableStateOf(false) }
    val ds = strings.dashboardUi

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

    // A real window: covers the top bar and FAB, Back closes it, and an outside tap can't
    // end a running session by accident.
    Dialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(dismissOnClickOutside = !isRunning, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
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
                        Icon(
                            Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = SlotifyColors.TextSecondary
                        )
                    }
                }

                if (!isFinished) {
                    Text(
                        text = ds.focusSessionFor(childName),
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
                                    text = ds.focusMinutesOption(mins),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSel) Color.White else SlotifyColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Circular Countdown Timer Display
                    val minutesLeft = secondsRemaining / 60
                    val secsLeft = secondsRemaining % 60
                    val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutesLeft, secsLeft)

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
                                text = if (isRunning) ds.focusStatusRunning else ds.focusStatusReady,
                                fontSize = 10.sp,
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
                            text = "+${StarRewards.FOCUS_SESSION_STARS} ⭐",
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
                            Text(ds.claimStarsButton, fontWeight = FontWeight.Bold)
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
    val ds = strings.dashboardUi

    val subjects = listOf("BM", "MT", "SN", "BI", "PI", "BA", "SEJ", "RBT")
    // Stored value (persisted & matched elsewhere) to display label.
    val dueDates = listOf(
        "Hari Ini" to ds.dueToday,
        "Esok" to ds.dueTomorrow,
        "Isnin" to ds.dueMonday,
        "Khamis" to ds.dueThursday
    )

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
                        text = ds.addHomeworkTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = SlotifyColors.TextSecondary
                        )
                    }
                }

                // Subject Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        ds.subjectSelectorLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlotifyColors.TextSecondary
                    )
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
                    label = { Text(ds.taskTitleLabel) },
                    placeholder = { Text(ds.taskTitlePlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )

                // Due date selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        ds.dueDateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlotifyColors.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dueDates) { (due, dueLabel) ->
                            FilterChip(
                                selected = dueDateDesc == due,
                                onClick = { dueDateDesc = due },
                                label = { Text(dueLabel, fontSize = 11.sp) }
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
                                    text = if (hasPhotoProof) ds.photoAttachedTitle else ds.photoCaptureTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (hasPhotoProof) SlotifyColors.ScienceGreen else SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = if (hasPhotoProof) ds.photoAttachedFile else ds.photoCaptureHint,
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
                    label = { Text(ds.notesLabel) },
                    placeholder = { Text(ds.notesPlaceholder) },
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
                        Text(strings.cancelButton)
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
                        Text(ds.saveTaskButton, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
