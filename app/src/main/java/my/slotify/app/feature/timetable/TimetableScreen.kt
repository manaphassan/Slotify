package my.slotify.app.feature.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import my.slotify.app.core.database.SchoolSessionEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.slotify.app.core.di.SlotifyViewModelFactory
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.LocalAppLanguage
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.model.KpmSubjectCatalog
import java.util.Calendar
import java.util.UUID
import androidx.compose.runtime.saveable.rememberSaveable
import my.slotify.app.core.time.SchoolDay
import my.slotify.app.core.model.AttireGuide
import my.slotify.app.core.time.TimeOfDay
import my.slotify.app.core.time.rememberNow
import my.slotify.app.core.designsystem.component.slotifySection
import my.slotify.app.core.designsystem.component.ScheduleCard
import my.slotify.app.core.designsystem.component.SlotifyScreenTitle
import my.slotify.app.core.designsystem.theme.SlotStatus
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.time.SlotState
import my.slotify.app.core.time.SlotStates
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale

enum class TimetableViewMode {
    DAY,
    WEEK,
    TOMORROW
}

@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel = viewModel(factory = SlotifyViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedStudent = state.student
    val slots = state.slots
    val session = state.activeSession
    val sessions = state.sessions
    val onToggleYearInversion: () -> Unit = viewModel::nextSession
    val liveNow by rememberNow()
    val today = liveNow.toLocalDate()
    val todayDow: Int? = SchoolDay.of(today)

    var viewMode by remember { mutableStateOf(TimetableViewMode.DAY) }
    var selectedDay by rememberSaveable { mutableIntStateOf(todayDow ?: SchoolDay.MONDAY) }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    // Optional time preview (null = live clock).
    var simulatedMinutes by remember { mutableStateOf<Int?>(null) }

    val ts = strings.timetableUi
    val days =
        listOf(ts.dayMonday to 1, ts.dayTuesday to 2, ts.dayWednesday to 3, ts.dayThursday to 4, ts.dayFriday to 5)

    val allSlots = slots
    val currentSlots = allSlots.filter { it.dayOfWeek == selectedDay }

    // School week shown: this week on school days, next week at the weekend.
    val appLanguage = LocalAppLanguage.current
    val dateLocale = if (appLanguage == AppLanguage.EN) Locale.ENGLISH else Locale.forLanguageTag("ms-MY")
    val weekStart = remember(today) {
        if (todayDow == null) SchoolDay.nextSchoolDate(today) else today.minusDays((todayDow - 1).toLong())
    }

    fun dateOf(dayIndex: Int) = weekStart.plusDays((dayIndex - 1).toLong())
    val weekRangeLabel = remember(weekStart, dateLocale) {
        val end = weekStart.plusDays(4)
        val dm = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", dateLocale)
        if (weekStart.month == end.month) "${weekStart.dayOfMonth}–${end.format(dm)}"
        else "${
            weekStart.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "d MMM",
                    dateLocale
                )
            )
        } – ${end.format(dm)}"
    }
    val dayStates = remember(currentSlots, selectedDay, todayDow, liveNow.hour, liveNow.minute) {
        SlotStates.forDay(currentSlots, if (selectedDay == todayDow) liveNow.hour * 60 + liveNow.minute else null)
    }
    val sch = strings.scheduleUi

    // Live tracking uses today's slots (none on weekends); a time preview uses the selected day.
    val currentSimMinutes = simulatedMinutes ?: (liveNow.hour * 60 + liveNow.minute)
    val trackedSlots = if (simulatedMinutes != null) currentSlots
    else if (todayDow != null) allSlots.filter { it.dayOfWeek == todayDow } else emptyList()
    val activeSlot = trackedSlots.firstOrNull { slot ->
        val startMin = parseTimeToMinutes(slot.startTime)
        val endMin = parseTimeToMinutes(slot.endTime)
        currentSimMinutes in startMin until endMin
    }
    val nextSlot = trackedSlots.firstOrNull { slot ->
        val startMin = parseTimeToMinutes(slot.startTime)
        startMin > currentSimMinutes
    }

    // Next school day's slots for the book list (Friday/weekend → Monday)
    val tomorrowDow = SchoolDay.nextAfter(today)
    val tomorrowSlots = allSlots.filter { it.dayOfWeek == tomorrowDow }

    var trackerOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SlotifyColors.CanvasBase)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SlotifyScreenTitle(title = strings.timetable, subtitle = weekRangeLabel) }

            // Session badge and actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Session badge — shows active session context (SRA Pagi / SK Petang)
                            if (session != null) {
                                val sessionLabel = when (session.sessionTimeType) {
                                    "PAGI" -> ts.sessionBadgeMorning(session.schoolName.substringBefore(" ").take(3))
                                    "PETANG" -> ts.sessionBadgeAfternoon(
                                        session.schoolName.substringBefore(" ").take(3)
                                    )

                                    "FULL_DAY" -> "🏫 ${session.schoolName.substringBefore(" ").take(4)}"
                                    else -> session.sessionTimeType
                                }
                                val badgeColor = when (session.sessionTimeType) {
                                    "PAGI" -> SlotifyColors.KpmSkyBlue
                                    "PETANG" -> SlotifyColors.ActionAmber
                                    "FULL_DAY" -> SlotifyColors.SuccessMint
                                    else -> SlotifyColors.TextSecondary
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), SlotifyShapes.Small)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sessionLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Add Slot FAB-style button (only when DB is available)
                        if (session != null) {
                            IconButton(
                                onClick = { showAddSlotDialog = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SlotifyColors.Brand, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = ts.addSlotDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Session Toggle: only shown when student has multiple sessions
                        if (sessions.size > 1) {
                            IconButton(
                                onClick = onToggleYearInversion,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SlotifyColors.SurfaceCard, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Autorenew,
                                    contentDescription = strings.sessionToggleDesc,
                                    tint = SlotifyColors.KpmSkyBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // View Mode Switcher
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(SlotifyColors.SurfaceCard)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ViewModeTab(
                            title = strings.dayView,
                            icon = Icons.Default.CalendarToday,
                            isSelected = viewMode == TimetableViewMode.DAY,
                            modifier = Modifier.weight(1f)
                        ) { viewMode = TimetableViewMode.DAY }

                        ViewModeTab(
                            title = strings.weekView,
                            icon = Icons.Default.ViewWeek,
                            isSelected = viewMode == TimetableViewMode.WEEK,
                            modifier = Modifier.weight(1f)
                        ) { viewMode = TimetableViewMode.WEEK }

                        ViewModeTab(
                            title = strings.tomorrowView,
                            icon = Icons.Default.FastForward,
                            isSelected = viewMode == TimetableViewMode.TOMORROW,
                            modifier = Modifier.weight(1f)
                        ) { viewMode = TimetableViewMode.TOMORROW }
                    }
                }
            }

            // View Content
            when (viewMode) {
                TimetableViewMode.DAY -> {
                    // Days Filter Tabs (Isnin - Jumaat)
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            items(days) { (dayName, dayIndex) ->
                                val isSelected = selectedDay == dayIndex
                                val fg = if (isSelected) Color.White else SlotifyColors.TextPrimary
                                Column(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) SlotifyColors.Brand else SlotifyColors.SurfaceCard)
                                        .clickable { selectedDay = dayIndex }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(dayName.take(3), style = MaterialTheme.typography.labelLarge, color = fg)
                                        if (dayIndex == todayDow) {
                                            Spacer(Modifier.width(4.dp))
                                            Box(
                                                Modifier.size(5.dp).background(
                                                    if (isSelected) Color.White else SlotifyColors.Brand,
                                                    CircleShape
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        "${dateOf(dayIndex).dayOfMonth}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else SlotifyColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                days.first { it.second == selectedDay }.first,
                                style = MaterialTheme.typography.headlineLarge,
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                dateOf(selectedDay).format(
                                    java.time.format.DateTimeFormatter.ofPattern(
                                        "d MMMM",
                                        dateLocale
                                    )
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }

                    // Slots list
                    if (currentSlots.isEmpty()) {
                        item {
                            ScheduleCard(title = sch.freeDayTitle, time = ts.emptyDaySlots, status = SlotStatus.NOW)
                        }
                    } else {
                        items(dayStates, key = { it.first.id }) { (slot, st) ->
                            TimetableSlotCard(
                                slot = slot,
                                state = st,
                                canDelete = session != null,
                                onDelete = { viewModel.deleteSlot(slot) }
                            )
                        }
                    }
                }

                TimetableViewMode.WEEK -> {
                    item {
                        WeekOverviewView(allSlots)
                    }
                }

                TimetableViewMode.TOMORROW -> {
                    item {
                        TomorrowViewSection(tomorrowSlots, tomorrowDow)
                    }
                }
            }

            slotifySection(
                key = "tracker",
                title = ts.groupTracker,
                expanded = trackerOpen,
                onToggle = { trackerOpen = !trackerOpen },
                summary = activeSlot?.let { ts.trackerSummary(it.subjectName) }
            ) {
                // Realtime Tracker Card
                item {
                    RealtimeClassTrackerCard(
                        activeSlot = activeSlot,
                        nextSlot = nextSlot,
                        simulatedTime = String.format(
                            java.util.Locale.US,
                            "%02d:%02d",
                            currentSimMinutes / 60,
                            currentSimMinutes % 60
                        ) +
                                if (simulatedMinutes == null) " (${ts.clockLiveTag})" else " (${ts.clockPreviewTag})",
                        onSimulateMorning = { simulatedMinutes = 8 * 60 + 15 },
                        onSimulateNoon = { simulatedMinutes = 10 * 60 + 15 },
                        onSimulateAfternoon = { simulatedMinutes = 14 * 60 + 30 },
                        onLive = { simulatedMinutes = null }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // Add Slot Dialog
        if (showAddSlotDialog && session != null) {
            AddSlotDialog(
                session = session,
                preselectedDay = selectedDay,
                existingSlotsCount = currentSlots.size,
                onDismiss = { showAddSlotDialog = false },
                onConfirm = { newSlot ->
                    viewModel.addSlot(newSlot)
                    showAddSlotDialog = false
                }
            )
        }
    }
}

@Composable
private fun AddSlotDialog(
    session: SchoolSessionEntity,
    preselectedDay: Int,
    existingSlotsCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (TimetableSlotEntity) -> Unit
) {
    val ts = strings.timetableUi
    val days =
        listOf(ts.dayMonday to 1, ts.dayTuesday to 2, ts.dayWednesday to 3, ts.dayThursday to 4, ts.dayFriday to 5)
    val subjects = KpmSubjectCatalog.subjects.values.toList()

    var selectedDay by remember { mutableIntStateOf(preselectedDay) }
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("08:30") }
    var teacherName by remember { mutableStateOf("") }
    var requiresTextbook by remember { mutableStateOf(true) }
    var requiresWorkbook by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.XLargeDp)
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ts.addSlotTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = SlotifyColors.TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = SlotifyColors.BorderSubtle)

                // Day Selector
                Text(ts.dayLabel, style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(days) { (dayName, dayIdx) ->
                        val isSel = selectedDay == dayIdx
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(SlotifyShapes.Small)
                                .background(if (isSel) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
                                .clickable { selectedDay = dayIdx }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else SlotifyColors.TextPrimary
                            )
                        }
                    }
                }

                // Subject Picker
                Text(ts.subjectLabel, style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(selectedSubject.color.copy(alpha = 0.08f), SlotifyShapes.Medium)
                        .padding(10.dp)
                ) {
                    // Show current selection + scroll grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Selected subject preview
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(selectedSubject.color.copy(alpha = 0.15f), SlotifyShapes.XSmall),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    selectedSubject.code,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = selectedSubject.color,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                selectedSubject.nameBm,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlotifyColors.TextPrimary
                            )
                        }
                        // Subject chip grid
                        val rows = subjects.chunked(5)
                        rows.forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowItems.forEach { subj ->
                                    val isSel = selectedSubject.code == subj.code
                                    Box(
                                        modifier = Modifier
                                            .clip(SlotifyShapes.XSmall)
                                            .background(if (isSel) subj.color else SlotifyColors.CanvasBase)
                                            .clickable { selectedSubject = subj }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            subj.code,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else subj.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Time inputs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ts.startTimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = SlotifyColors.TextSecondary
                        )
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { if (it.length <= 5) startTime = it },
                            placeholder = { Text("08:00") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = SlotifyShapes.Small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SlotifyColors.KpmSkyBlue,
                                unfocusedBorderColor = SlotifyColors.BorderSubtle,
                                focusedTextColor = SlotifyColors.TextPrimary,
                                unfocusedTextColor = SlotifyColors.TextPrimary
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ts.endTimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = SlotifyColors.TextSecondary
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { if (it.length <= 5) endTime = it },
                            placeholder = { Text("08:30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = SlotifyShapes.Small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SlotifyColors.KpmSkyBlue,
                                unfocusedBorderColor = SlotifyColors.BorderSubtle,
                                focusedTextColor = SlotifyColors.TextPrimary,
                                unfocusedTextColor = SlotifyColors.TextPrimary
                            )
                        )
                    }
                }

                // Teacher name
                Text(
                    ts.teacherNameLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = SlotifyColors.TextSecondary
                )
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    placeholder = { Text(ts.teacherNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SlotifyShapes.Small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SlotifyColors.KpmSkyBlue,
                        unfocusedBorderColor = SlotifyColors.BorderSubtle,
                        focusedTextColor = SlotifyColors.TextPrimary,
                        unfocusedTextColor = SlotifyColors.TextPrimary
                    )
                )

                // Book toggles
                if (!selectedSubject.isRehat) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(SlotifyShapes.Small)
                                .background(if (requiresTextbook) SlotifyColors.KpmSkyBlue.copy(0.1f) else SlotifyColors.CanvasBase)
                                .clickable { requiresTextbook = !requiresTextbook }
                                .padding(8.dp)
                        ) {
                            Checkbox(checked = requiresTextbook, onCheckedChange = { requiresTextbook = it })
                            Text(
                                ts.textbookToggle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(SlotifyShapes.Small)
                                .background(if (requiresWorkbook) SlotifyColors.ActionAmber.copy(0.1f) else SlotifyColors.CanvasBase)
                                .clickable { requiresWorkbook = !requiresWorkbook }
                                .padding(8.dp)
                        ) {
                            Checkbox(checked = requiresWorkbook, onCheckedChange = { requiresWorkbook = it })
                            Text(
                                ts.workbookToggle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextPrimary
                            )
                        }
                    }
                }

                // Confirm Button
                val normalizedStart = TimeOfDay.normalize(startTime)
                val normalizedEnd = TimeOfDay.normalize(endTime)
                val timesValid = normalizedStart != null && normalizedEnd != null && normalizedStart < normalizedEnd
                if (!timesValid && startTime.isNotBlank() && endTime.isNotBlank()) {
                    Text(
                        ts.invalidTimeMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlotifyColors.EnglishRed
                    )
                }
                Button(
                    enabled = timesValid,
                    onClick = {
                        val slot = TimetableSlotEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = session.id,
                            dayOfWeek = selectedDay,
                            periodIndex = existingSlotsCount + 1,
                            startTime = normalizedStart!!,
                            endTime = normalizedEnd!!,
                            subjectCode = selectedSubject.code,
                            subjectName = selectedSubject.nameBm,
                            teacherName = teacherName.ifBlank { null },
                            isRehat = selectedSubject.isRehat,
                            requiresTextbook = if (selectedSubject.isRehat) false else requiresTextbook,
                            requiresWorkbook = if (selectedSubject.isRehat) false else requiresWorkbook
                        )
                        onConfirm(slot)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                    shape = SlotifyShapes.Medium
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ts.saveSlotButton,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) SlotifyColors.Brand else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else SlotifyColors.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else SlotifyColors.TextPrimary
            )
        }
    }
}

@Composable
private fun RealtimeClassTrackerCard(
    activeSlot: TimetableSlotEntity?,
    nextSlot: TimetableSlotEntity?,
    simulatedTime: String,
    onSimulateMorning: () -> Unit,
    onSimulateNoon: () -> Unit,
    onSimulateAfternoon: () -> Unit,
    onLive: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SlotifyColors.SuccessMint)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${strings.realtimeTrackerTitle} ($simulatedTime)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                }

                // Time Simulation Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SimTimeChip(strings.timetableUi.simLiveChip, onClick = onLive)
                    SimTimeChip("08:15", onClick = onSimulateMorning)
                    SimTimeChip("10:15", onClick = onSimulateNoon)
                    SimTimeChip("14:30", onClick = onSimulateAfternoon)
                }
            }

            HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlotifyColors.ActionAmber.copy(alpha = 0.12f), SlotifyShapes.Medium)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                strings.ongoingStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.ActionAmber
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        if (activeSlot != null) {
                            Text(
                                text = activeSlot.subjectName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${activeSlot.startTime} – ${activeSlot.endTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlotifyColors.TextSecondary
                            )
                        } else {
                            Text(
                                text = strings.timetableUi.noClassOrBreak,
                                style = MaterialTheme.typography.labelLarge,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.12f), SlotifyShapes.Medium)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            strings.nextStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.KpmSkyBlue
                        )
                        Spacer(Modifier.height(4.dp))
                        if (nextSlot != null) {
                            Text(
                                text = nextSlot.subjectName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${strings.startPrefix} ${nextSlot.startTime} (${nextSlot.subjectCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlotifyColors.TextSecondary
                            )
                        } else {
                            Text(
                                text = strings.timetableUi.sessionFinished,
                                style = MaterialTheme.typography.labelLarge,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimTimeChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(SlotifyShapes.XSmall)
            .background(SlotifyColors.CanvasBase)
            .neumorphicClickable(cornerRadius = 6.dp, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = SlotifyColors.TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimetableSlotCard(
    slot: TimetableSlotEntity,
    state: SlotState,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {}
) {
    val sch = strings.scheduleUi
    val ts = strings.timetableUi
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val chip = when (state.status) {
        SlotStatus.COMPLETED -> sch.statusCompleted
        SlotStatus.NOW -> sch.statusNow
        SlotStatus.NEXT -> state.minutesUntilStart?.let { sch.statusIn(it) }
        SlotStatus.BREAK -> sch.statusBreak
        else -> null
    }
    ScheduleCard(
        title = slot.subjectName,
        time = "${slot.startTime} – ${slot.endTime}",
        status = state.status,
        modifier = Modifier.animateContentSize(),
        subtitle = slot.teacherName ?: if (slot.isRehat) ts.breakFallbackLabel else null,
        subtitleIcon = Icons.Default.Person,
        chip = chip,
        accent = when (state.status) {
            SlotStatus.COMPLETED, SlotStatus.NOW, SlotStatus.BREAK -> null
            else -> SlotifyColors.subjectColor(slot.subjectCode)
        },
        trailing = if (canDelete) {
            {
                IconButton(onClick = { showDeleteConfirm = !showDeleteConfirm }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (showDeleteConfirm) Icons.Default.ExpandLess else Icons.Default.MoreVert,
                        contentDescription = ts.slotOptionsDesc,
                        tint = SlotifyColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        footer = {
            AnimatedVisibility(visible = showDeleteConfirm) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = SlotifyColors.TextSecondary)
                    ) { Text(strings.cancelButton) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.EnglishRed),
                        shape = SlotifyShapes.Medium,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(ts.deleteSlotButton, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }
    )
}

@Composable
private fun WeekOverviewView(slots: List<TimetableSlotEntity>) {
    val ts = strings.timetableUi
    val appLanguage = LocalAppLanguage.current
    val days = listOf(
        Triple(ts.dayMonday, 1, AttireGuide.forSchoolDay(1, appLanguage)),
        Triple(ts.dayTuesday, 2, AttireGuide.forSchoolDay(2, appLanguage)),
        Triple(ts.dayWednesday, 3, AttireGuide.forSchoolDay(3, appLanguage)),
        Triple(ts.dayThursday, 4, AttireGuide.forSchoolDay(4, appLanguage)),
        Triple(ts.dayFriday, 5, AttireGuide.forSchoolDay(5, appLanguage))
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = strings.weeklyAttireSummaryTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SlotifyColors.TextPrimary
        )

        days.forEach { (dayName, dayIdx, attire) ->
            val daySlots = slots.filter { it.dayOfWeek == dayIdx }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.MediumDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Medium)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            dayName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            attire,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SlotifyColors.ActionAmber
                        )
                    }
                    if (daySlots.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            daySlots.take(7).forEach { slot ->
                                val meta = KpmSubjectCatalog.getSubject(slot.subjectCode)
                                Box(
                                    modifier = Modifier
                                        .background(meta.color.copy(alpha = 0.2f), SlotifyShapes.XSmall)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        slot.subjectCode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = meta.color
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            ts.noSlotsYet,
                            style = MaterialTheme.typography.labelSmall,
                            color = SlotifyColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TomorrowViewSection(tomorrowSlots: List<TimetableSlotEntity>, tomorrowDow: Int) {
    val ts = strings.timetableUi
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = SlotifyShapes.LargeDp)
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👕", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        strings.tomorrowAttireTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        AttireGuide.label(tomorrowDow, LocalAppLanguage.current),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SlotifyColors.ScienceGreen
                    )
                }
            }
        }

        Text(
            text = strings.tomorrowBooksTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SlotifyColors.TextPrimary
        )

        // Wire to real timetable data; fall back to hardcoded if DB empty
        val books = if (tomorrowSlots.isNotEmpty()) {
            tomorrowSlots
                .filter { !it.isRehat && (it.requiresTextbook || it.requiresWorkbook) }
                .map { slot ->
                    val type = buildString {
                        if (slot.requiresTextbook) append(ts.bookTypeTextbook)
                        if (slot.requiresTextbook && slot.requiresWorkbook) append(" + ")
                        if (slot.requiresWorkbook) append(ts.bookTypeWorkbook)
                    }
                    "📚 ${slot.subjectName} (${slot.subjectCode}) — $type"
                }
                .distinct()
                .ifEmpty {
                    listOf(ts.allBooksChecked)
                }
        } else {
            listOf(
                "📘 Buku Teks Bahasa Melayu Jilid 1",
                "📙 Buku Aktiviti Matematik Bahagian 1",
                "📗 Buku Amali Sains & Modul PBD",
                "📕 Super Minds 4 English Workbook"
            )
        }

        books.forEach { book ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.MediumDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Medium)
                    .padding(10.dp)
            ) {
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
        }
    }
}

private fun parseTimeToMinutes(timeStr: String): Int {
    val parts = timeStr.split(":")
    if (parts.size != 2) return 0
    val h = parts[0].trim().toIntOrNull() ?: 0
    val m = parts[1].trim().toIntOrNull() ?: 0
    return h * 60 + m
}
