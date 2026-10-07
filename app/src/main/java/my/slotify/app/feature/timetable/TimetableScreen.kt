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
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.model.KpmSubjectCatalog
import java.util.Calendar
import java.util.UUID

enum class TimetableViewMode {
    DAY,
    WEEK,
    TOMORROW
}

@Composable
fun TimetableScreen(
    selectedStudent: StudentEntity?,
    slots: List<TimetableSlotEntity>,
    onToggleYearInversion: () -> Unit,
    dao: SlotifyDao? = null,
    session: SchoolSessionEntity? = null,
    sessions: List<SchoolSessionEntity> = emptyList(),
    activeSessionIndex: Int = 0
) {
    var viewMode by remember { mutableStateOf(TimetableViewMode.DAY) }
    var selectedDay by remember { mutableIntStateOf(1) } // 1=Isnin
    var showAddSlotDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Simulated / current time state
    var simulatedHour by remember { mutableIntStateOf(8) }
    var simulatedMinute by remember { mutableIntStateOf(15) }

    val days = listOf("Isnin" to 1, "Selasa" to 2, "Rabu" to 3, "Khamis" to 4, "Jumaat" to 5)

    val sampleSlots = remember {
        listOf(
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "08:30", subjectCode = "BM", subjectName = "Bahasa Melayu", teacherName = "Cikgu Rosli"),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 2, startTime = "08:30", endTime = "09:30", subjectCode = "MT", subjectName = "Matematik", teacherName = "Cikgu Halim"),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 3, startTime = "09:30", endTime = "10:00", subjectCode = "REHAT", subjectName = "Waktu Rehat SRA", isRehat = true),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 4, startTime = "10:00", endTime = "11:00", subjectCode = "TAHFIZ", subjectName = "Hafazan Al-Quran", teacherName = "Ustaz Zakaria"),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 5, startTime = "13:00", endTime = "14:00", subjectCode = "SN", subjectName = "Sains (Makmal)", teacherName = "Puan Hasnah"),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 6, startTime = "14:00", endTime = "15:00", subjectCode = "BI", subjectName = "English Language", teacherName = "Miss Cheryl"),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 7, startTime = "15:00", endTime = "15:30", subjectCode = "REHAT", subjectName = "Rehat Petang SK", isRehat = true),
            TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 8, startTime = "15:30", endTime = "17:00", subjectCode = "PI", subjectName = "Pendidikan Islam", teacherName = "Ustazah Zaimah")
        )
    }
    val allSlots = if (slots.isNotEmpty()) slots else sampleSlots
    val currentSlots = allSlots.filter { it.dayOfWeek == selectedDay }

    // Compute realtime current and next class
    val currentSimMinutes = simulatedHour * 60 + simulatedMinute
    val activeSlot = currentSlots.firstOrNull { slot ->
        val startMin = parseTimeToMinutes(slot.startTime)
        val endMin = parseTimeToMinutes(slot.endTime)
        currentSimMinutes in startMin until endMin
    }
    val nextSlot = currentSlots.firstOrNull { slot ->
        val startMin = parseTimeToMinutes(slot.startTime)
        startMin > currentSimMinutes
    }

    // Tomorrow's slots for book list
    val todayDow = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; else -> 1
        }
    }
    val tomorrowDow = if (todayDow >= 5) 1 else todayDow + 1
    val tomorrowSlots = allSlots.filter { it.dayOfWeek == tomorrowDow }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SlotifyColors.CanvasBase)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.timetableTitle,
                            style = MaterialTheme.typography.headlineLarge,
                            color = SlotifyColors.TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${selectedStudent?.nickname ?: "Murid"} • ${selectedStudent?.currentLevel ?: "KPM"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
                            // Session badge — shows active session context (SRA Pagi / SK Petang)
                            if (session != null) {
                                val sessionLabel = when (session.sessionTimeType) {
                                    "PAGI"     -> "🌅 ${session.schoolName.substringBefore(" ").take(3)} Pagi"
                                    "PETANG"   -> "🌆 ${session.schoolName.substringBefore(" ").take(3)} Petang"
                                    "FULL_DAY" -> "🏫 ${session.schoolName.substringBefore(" ").take(4)}"
                                    else       -> session.sessionTimeType
                                }
                                val badgeColor = when (session.sessionTimeType) {
                                    "PAGI"     -> SlotifyColors.KpmSkyBlue
                                    "PETANG"   -> SlotifyColors.ActionAmber
                                    "FULL_DAY" -> SlotifyColors.SuccessMint
                                    else       -> SlotifyColors.TextSecondary
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
                        if (dao != null && session != null) {
                            IconButton(
                                onClick = { showAddSlotDialog = true },
                                modifier = Modifier
                                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                                    .background(SlotifyColors.KpmSkyBlue, SlotifyShapes.Large)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah Slot",
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
                                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                                    .size(36.dp)
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
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                        .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
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

            // Realtime Tracker Card
            item {
                RealtimeClassTrackerCard(
                    activeSlot = activeSlot,
                    nextSlot = nextSlot,
                    simulatedTime = String.format("%02d:%02d", simulatedHour, simulatedMinute),
                    onSimulateMorning = { simulatedHour = 8; simulatedMinute = 15 },
                    onSimulateNoon = { simulatedHour = 10; simulatedMinute = 15 },
                    onSimulateAfternoon = { simulatedHour = 14; simulatedMinute = 30 }
                )
            }

            // View Content
            when (viewMode) {
                TimetableViewMode.DAY -> {
                    // Days Filter Tabs (Isnin - Jumaat)
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            items(days) { (dayName, dayIndex) ->
                                val isSelected = selectedDay == dayIndex
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(SlotifyShapes.Medium)
                                        .background(
                                            if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard
                                        )
                                        .clickable { selectedDay = dayIndex }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Slots list
                    if (currentSlots.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📅", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Tiada slot untuk hari ini.\nTekan + untuk tambah waktu belajar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(currentSlots, key = { it.id }) { slot ->
                            TimetableSlotCard(
                                slot = slot,
                                canDelete = dao != null && session != null,
                                onDelete = {
                                    coroutineScope.launch { dao?.deleteSlot(slot) }
                                }
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
                        TomorrowViewSection(tomorrowSlots)
                    }
                }
            }

            item { Spacer(Modifier.height(70.dp)) }
        }

        // Add Slot Dialog
        if (showAddSlotDialog && dao != null && session != null) {
            AddSlotDialog(
                session = session,
                preselectedDay = selectedDay,
                existingSlotsCount = currentSlots.size,
                onDismiss = { showAddSlotDialog = false },
                onConfirm = { newSlot ->
                    coroutineScope.launch {
                        dao.insertSlots(listOf(newSlot))
                        showAddSlotDialog = false
                    }
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
    val days = listOf("Isnin" to 1, "Selasa" to 2, "Rabu" to 3, "Khamis" to 4, "Jumaat" to 5)
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
                        text = "➕ Tambah Waktu Belajar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SlotifyColors.TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary)
                    }
                }

                HorizontalDivider(color = SlotifyColors.BorderSubtle)

                // Day Selector
                Text("Hari", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                Text("Subjek", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                                    fontSize = 9.sp
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
                                            fontSize = 9.sp,
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
                        Text("Mula (HH:MM)", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                        Text("Tamat (HH:MM)", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                Text("Nama Guru (pilihan)", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    placeholder = { Text("cth: Cikgu Rosli") },
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
                            Text("📘 Buku Teks", style = MaterialTheme.typography.bodySmall, color = SlotifyColors.TextPrimary)
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
                            Text("📙 Buku Aktiviti", style = MaterialTheme.typography.bodySmall, color = SlotifyColors.TextPrimary)
                        }
                    }
                }

                // Confirm Button
                Button(
                    onClick = {
                        val slot = TimetableSlotEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = session.id,
                            dayOfWeek = selectedDay,
                            periodIndex = existingSlotsCount + 1,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
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
                        "Simpan Waktu",
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
            .clip(SlotifyShapes.Medium)
            .background(if (isSelected) SlotifyColors.KpmSkyBlue else Color.Transparent)
            .neumorphicClickable(cornerRadius = SlotifyShapes.MediumDp, onClick = onClick)
            .padding(vertical = 8.dp),
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
    onSimulateAfternoon: () -> Unit
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
                            Text(strings.ongoingStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlotifyColors.ActionAmber)
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
                                text = "Tiada Kelas / Rehat",
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
                        Text(strings.nextStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlotifyColors.KpmSkyBlue)
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
                                text = "Sesi Selesai",
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
        Text(text = label, fontSize = 9.sp, color = SlotifyColors.TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimetableSlotCard(
    slot: TimetableSlotEntity,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {}
) {
    val subject = KpmSubjectCatalog.getSubject(slot.subjectCode)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .neumorphic(
                elevation = if (slot.isRehat) NeumorphicElevation.LOW else NeumorphicElevation.MEDIUM,
                cornerRadius = SlotifyShapes.MediumDp
            )
            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Medium)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(subject.color.copy(alpha = 0.15f), SlotifyShapes.Small),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subject.code,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = subject.color,
                            fontSize = if (subject.code.length > 4) 7.sp else 10.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = slot.subjectName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = slot.teacherName ?: if (slot.isRehat) "Kantin / Rehat" else "Guru Subjek",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(SlotifyColors.CanvasBase, SlotifyShapes.Small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${slot.startTime} – ${slot.endTime}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SlotifyColors.TextPrimary
                        )
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = { showDeleteConfirm = !showDeleteConfirm },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (showDeleteConfirm) Icons.Default.ExpandLess else Icons.Default.MoreVert,
                                contentDescription = "Opsyen",
                                tint = SlotifyColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showDeleteConfirm) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = SlotifyColors.TextSecondary)
                    ) { Text("Batal") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.EnglishRed),
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Padam Slot", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekOverviewView(slots: List<TimetableSlotEntity>) {
    val days = listOf(
        Triple("Isnin", 1, "Uniform Lengkap KPM"),
        Triple("Selasa", 2, "Uniform Sukan / PJ"),
        Triple("Rabu", 3, "Baju Batik Sekolah"),
        Triple("Khamis", 4, "Uniform Lengkap KPM"),
        Triple("Jumaat", 5, "Baju Melayu / Kurung")
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
                        Text(dayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = SlotifyColors.TextPrimary)
                        Text(attire, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = SlotifyColors.ActionAmber)
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
                                    Text(slot.subjectCode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = meta.color)
                                }
                            }
                        }
                    } else {
                        Text("Tiada slot lagi", style = MaterialTheme.typography.labelSmall, color = SlotifyColors.TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TomorrowViewSection(tomorrowSlots: List<TimetableSlotEntity>) {
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
                    Text(strings.tomorrowAttireTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SlotifyColors.TextPrimary)
                    Text("T-Shirt Sukan SK + Seluar Track Gelap", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = SlotifyColors.ScienceGreen)
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
                        if (slot.requiresTextbook) append("Buku Teks")
                        if (slot.requiresTextbook && slot.requiresWorkbook) append(" + ")
                        if (slot.requiresWorkbook) append("Buku Aktiviti")
                    }
                    "📚 ${slot.subjectName} (${slot.subjectCode}) — $type"
                }
                .distinct()
                .ifEmpty {
                    listOf("✅ Semua buku sudah disemak untuk esok!")
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
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SlotifyColors.SuccessMint, modifier = Modifier.size(16.dp))
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
