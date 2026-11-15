package my.slotify.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import my.slotify.app.core.database.StudentEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.slotify.app.core.di.SlotifyViewModelFactory
import my.slotify.app.core.database.PbdAssessmentEntity
import my.slotify.app.core.model.KpmSubjectCatalog
import my.slotify.app.core.designsystem.theme.AppStrings
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import kotlinx.coroutines.launch
import java.util.UUID
import my.slotify.app.core.designsystem.component.slotifySection
import androidx.compose.runtime.saveable.rememberSaveable
import my.slotify.app.core.designsystem.component.SlotifyScreenTitle

// Fix 1.4: Mask child national ID — show only last 4 digits by default
private fun String.maskMyKid(): String {
    if (length < 6) return this
    return "••••-••-${takeLast(4)}"
}

// Fix 4.2: Map raw database enum strings to human-readable localised labels
private fun String.toSessionTypeLabel(str: AppStrings): String = when (this) {
    "DUAL_SRA_SK" -> str.profileUi.sessionDual
    "INTEGRATED_SRAISM" -> str.profileUi.sessionIntegratedIslamic
    "TADIKA_DAYCARE" -> str.profileUi.sessionTadikaDaycare
    "FULL_DAY" -> str.sessionFull
    "MORNING" -> str.sessionMorning
    "AFTERNOON" -> str.sessionAfternoon
    else -> this
}

@Composable
fun StudentProfileScreen(
    students: List<StudentEntity>,
    onSelectStudent: (StudentEntity) -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = SlotifyViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val vmMessage by viewModel.message.collectAsStateWithLifecycle()
    val selectedStudent = state.student
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddPbdDialog by remember { mutableStateOf(false) }
    var pbdCelebrationMessage by remember { mutableStateOf<PbdRecordResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fix 1.4: Show/hide toggle for MyKid number — hidden by default for privacy
    var showMyKid by remember { mutableStateOf(false) }

    val assessments = state.assessments
    LaunchedEffect(vmMessage) {
        vmMessage?.let { pbdCelebrationMessage = it; viewModel.consumeMessage() }
    }

    val str = strings
    val ps = str.profileUi
    val pbdCelebrationText = pbdCelebrationMessage?.let { r ->
        if (r.starsAwarded > 0) {
            ps.pbdRecordedWithStars(r.tahapPenguasaan, r.subjectName, r.starsAwarded)
        } else {
            ps.pbdUpdated(r.tahapPenguasaan, r.subjectName)
        }
    }

    var schoolOpen by rememberSaveable { mutableStateOf(true) }
    var assessmentOpen by rememberSaveable { mutableStateOf(true) }
    var personalOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SlotifyScreenTitle(title = strings.profile, subtitle = strings.profileSubtitle) }

        // Child switcher and edit button
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
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
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = student.avatarEmoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = student.nickname,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = strings.editProfileTitle,
                        tint = SlotifyColors.KpmSkyBlue
                    )
                }
            }
        }

        // Selected Student Hero Card
        selectedStudent?.let { s ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 20.dp)
                        .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = s.avatarEmoji, fontSize = 28.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                color = SlotifyColors.TextPrimary,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(SlotifyColors.KpmSkyBlue, SlotifyShapes.XSmall)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = s.currentLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (s.className.isNotBlank()) ps.classWithName(s.className) else ps.classFallback,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "school",
                title = ps.groupSchool,
                expanded = schoolOpen,
                onToggle = { schoolOpen = !schoolOpen }
            ) {
                // School & Session Info Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = SlotifyShapes.LargeDp)
                            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = SlotifyColors.ScienceGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.schoolInstTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )
                            }

                            // Primary School
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = strings.mainSchoolLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlotifyColors.TextSecondary
                                    )
                                    Text(
                                        text = s.primarySchoolName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SlotifyColors.TextPrimary
                                    )
                                }
                                // Fix 4.2: Resolve raw DB sessionType string to readable label
                                Text(
                                    text = s.sessionType.toSessionTypeLabel(str),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.ScienceGreen
                                )
                            }

                            // Secondary / Agama School
                            s.secondarySchoolName?.let { sec ->
                                HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = ps.secondarySchoolLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SlotifyColors.TextSecondary
                                        )
                                        Text(
                                            text = sec,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SlotifyColors.TextPrimary
                                        )
                                    }
                                    Text(
                                        text = strings.sessionMorning,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.IslamTeal
                                    )
                                }
                            }
                        }
                    }
                }

                // Transport & Van Contact
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = SlotifyShapes.LargeDp)
                            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = SlotifyColors.VanPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = s.vanDriverName ?: ps.dropOffByParents,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                    Text(
                                        text = s.vanDriverPhone ?: ps.ownVehicle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlotifyColors.TextSecondary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(SlotifyColors.SuccessMint.copy(alpha = 0.15f), SlotifyShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ps.transportActive,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.SuccessMint
                                )
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "assessment",
                title = ps.groupAssessment,
                expanded = assessmentOpen,
                onToggle = { assessmentOpen = !assessmentOpen }
            ) {
                // ── Pentaksiran Bilik Darjah (PBD) & Akademik Section ──
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
                                    Text("🎓", fontSize = 22.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = ps.pbdTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary
                                        )
                                        Text(
                                            text = ps.pbdSubtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.TextSecondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showAddPbdDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                                    shape = SlotifyShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        ps.recordTpButton,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (pbdCelebrationText != null) {
                                Surface(
                                    shape = SlotifyShapes.Small,
                                    color = SlotifyColors.SuccessMint.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⭐", fontSize = 16.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = pbdCelebrationText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.SuccessMint
                                        )
                                    }
                                }
                            }

                            if (assessments.isEmpty()) {
                                Text(
                                    text = ps.pbdEmpty(s.nickname),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    assessments.forEach { assessment ->
                                        val subjectColor = SlotifyColors.subjectColor(assessment.subjectCode)
                                        val tpLabel = when (assessment.tahapPenguasaan) {
                                            6 -> ps.tpLevel6
                                            5 -> ps.tpLevel5
                                            4 -> ps.tpLevel4
                                            3 -> ps.tpLevel3
                                            2 -> ps.tpLevel2
                                            else -> ps.tpLevel1
                                        }
                                        val tpColor = when (assessment.tahapPenguasaan) {
                                            6 -> Color(0xFF8B5CF6)
                                            5 -> SlotifyColors.SuccessMint
                                            4 -> SlotifyColors.KpmSkyBlue
                                            3 -> SlotifyColors.ActionAmber
                                            else -> Color(0xFFEF4444)
                                        }

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = SlotifyShapes.Small,
                                            color = SlotifyColors.CanvasBase
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = SlotifyShapes.XSmall,
                                                            color = subjectColor.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = assessment.subjectCode,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = subjectColor,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 6.dp,
                                                                    vertical = 2.dp
                                                                )
                                                            )
                                                        }
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = assessment.subjectName,
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = SlotifyColors.TextPrimary
                                                        )
                                                    }

                                                    Surface(
                                                        shape = SlotifyShapes.XSmall,
                                                        color = tpColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = tpLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = tpColor,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                // 6-segment TP Progress Bar
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    for (step in 1..6) {
                                                        val isAchieved = step <= assessment.tahapPenguasaan
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(5.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(
                                                                    if (isAchieved) tpColor else SlotifyColors.TextSecondary.copy(
                                                                        alpha = 0.2f
                                                                    )
                                                                )
                                                        )
                                                    }
                                                }

                                                if (assessment.teacherComment.isNotBlank()) {
                                                    Text(
                                                        text = ps.teacherCommentQuote(assessment.teacherComment),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SlotifyColors.TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "personal",
                title = ps.groupPersonal,
                expanded = personalOpen,
                onToggle = { personalOpen = !personalOpen }
            ) {
                // Biometrics & MyKid Card
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
                                    Icon(
                                        Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = SlotifyColors.KpmSkyBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.officialMyKidTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                }
                                // Fix 1.4: Show masked MyKid by default; toggle to reveal on tap
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showMyKid = !showMyKid }
                                ) {
                                    Text(
                                        text = if (s.myKid.isNotBlank()) {
                                            if (showMyKid) s.myKid else s.myKid.maskMyKid()
                                        } else "-",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.KpmSkyBlue
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (showMyKid) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showMyKid) ps.hideMyKid else ps.showMyKid,
                                        tint = SlotifyColors.TextTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 1.dp)

                            // Biometrics 3-Column Metrics (Umur, Tinggi, Berat)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BiometricMetricItem(
                                    title = strings.ageLabel,
                                    value = if (s.age > 0) "${s.age} ${strings.ageUnit}" else "-"
                                )
                                BiometricMetricItem(
                                    title = strings.heightLabel,
                                    value = if (s.heightCm > 0) "${s.heightCm} cm" else "-"
                                )
                                BiometricMetricItem(
                                    title = strings.weightLabel,
                                    value = if (s.weightKg > 0) "${s.weightKg} kg" else "-"
                                )
                                // BMI computation
                                val bmi = if (s.heightCm > 0 && s.weightKg > 0) {
                                    val hM = s.heightCm / 100.0
                                    s.weightKg / (hM * hM)
                                } else 0.0

                                BiometricMetricItem(
                                    title = strings.bmiLabel,
                                    value = if (bmi > 0) String.format(java.util.Locale.US, "%.1f", bmi) else "-"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clearance spacer for Speed Dial FAB
        item {
            Spacer(Modifier.height(70.dp))
        }
    }

    // Edit Profile Dialog
    if (showEditDialog && selectedStudent != null) {
        EditStudentProfileDialog(
            student = selectedStudent,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                viewModel.saveProfile(updated)
                showEditDialog = false
            }
        )
    }

    if (showAddPbdDialog && selectedStudent != null) {
        AddPbdAssessmentDialog(
            student = selectedStudent,
            onDismiss = { showAddPbdDialog = false },
            onSave = { assessment ->
                showAddPbdDialog = false
                viewModel.recordAssessment(assessment)
            }
        )
    }

    pbdCelebrationText?.let { msg ->
        AlertDialog(
            onDismissRequest = { pbdCelebrationMessage = null },
            confirmButton = { TextButton(onClick = { pbdCelebrationMessage = null }) { Text(str.ok) } },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun BiometricMetricItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = SlotifyColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SlotifyColors.TextPrimary
        )
    }
}

@Composable
fun EditStudentProfileDialog(
    student: StudentEntity,
    onDismiss: () -> Unit,
    onConfirm: (StudentEntity) -> Unit
) {
    var fullName by remember { mutableStateOf(student.fullName) }
    var nickname by remember { mutableStateOf(student.nickname) }
    var myKid by remember { mutableStateOf(student.myKid) }
    var ageStr by remember { mutableStateOf(if (student.age > 0) student.age.toString() else "") }
    var heightStr by remember { mutableStateOf(if (student.heightCm > 0) student.heightCm.toString() else "") }
    var weightStr by remember { mutableStateOf(if (student.weightKg > 0) student.weightKg.toString() else "") }
    var className by remember { mutableStateOf(student.className) }
    var primarySchool by remember { mutableStateOf(student.primarySchoolName) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.XLargeDp)
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.editProfileTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SlotifyColors.TextPrimary
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(strings.fullNameLabel, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text(strings.nicknameLabel, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = myKid,
                        onValueChange = { myKid = it },
                        label = { Text(strings.myKidLabel, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text(strings.ageLabel, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("${strings.heightLabel} (cm)", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("${strings.weightLabel} (kg)", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text(strings.classLabel, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = primarySchool,
                    onValueChange = { primarySchool = it },
                    label = { Text(strings.schoolLabel, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancelButton, color = SlotifyColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = student.copy(
                                fullName = fullName,
                                nickname = nickname,
                                myKid = myKid,
                                age = ageStr.toIntOrNull() ?: student.age,
                                heightCm = heightStr.toDoubleOrNull() ?: student.heightCm,
                                weightKg = weightStr.toDoubleOrNull() ?: student.weightKg,
                                className = className,
                                primarySchoolName = primarySchool
                            )
                            onConfirm(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue)
                    ) {
                        Text(strings.saveButton, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPbdAssessmentDialog(
    student: StudentEntity,
    onDismiss: () -> Unit,
    onSave: (PbdAssessmentEntity) -> Unit
) {
    val subjects = listOf(
        Pair("BM", "Bahasa Melayu"),
        Pair("MT", "Matematik"),
        Pair("SN", "Sains"),
        Pair("BI", "Bahasa Inggeris"),
        Pair("PI", "Pendidikan Islam"),
        Pair("BA", "Bahasa Arab"),
        Pair("SEJ", "Sejarah"),
        Pair("RBT", "RBT"),
        Pair("TAHFIZ", "Hafazan Al-Quran"),
        Pair("IQRA", "Iqra' Asas")
    )
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var selectedTp by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val ps = strings.profileUi

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.XLargeDp)
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = ps.addPbdTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
                Text(
                    text = ps.addPbdSubtitle(student.nickname),
                    style = MaterialTheme.typography.labelSmall,
                    color = SlotifyColors.TextSecondary
                )

                Text(
                    text = ps.subjectLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { subj ->
                        val isSelected = subj.first == selectedSubject.first
                        Box(
                            modifier = Modifier
                                .clip(SlotifyShapes.Small)
                                .background(if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
                                .clickable { selectedSubject = subj }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = subj.first,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                            )
                        }
                    }
                }

                Text(
                    text = ps.masteryLevelLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (tp in 1..6) {
                        val isSel = selectedTp == tp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(SlotifyShapes.Small)
                                .background(
                                    if (isSel) {
                                        if (tp >= 5) SlotifyColors.SuccessMint else SlotifyColors.KpmSkyBlue
                                    } else SlotifyColors.CanvasBase
                                )
                                .clickable { selectedTp = tp },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TP$tp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else SlotifyColors.TextPrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(ps.teacherCommentLabel, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                if (selectedTp >= 5) {
                    Surface(
                        shape = SlotifyShapes.Small,
                        color = SlotifyColors.MathAmber.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ps.bonusRewardHint(selectedTp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.MathAmber,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancelButton, color = SlotifyColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newAssessment = PbdAssessmentEntity(
                                id = UUID.randomUUID().toString(),
                                studentId = student.id,
                                subjectCode = selectedSubject.first,
                                subjectName = selectedSubject.second,
                                tahapPenguasaan = selectedTp,
                                teacherComment = comment.ifBlank { "Penguasaan kemahiran subjek memuaskan." },
                                assessedDate = java.time.LocalDate.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern(
                                        "MMM yyyy",
                                        java.util.Locale.forLanguageTag("ms-MY")
                                    )
                                )
                            )
                            onSave(newAssessment)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue)
                    ) {
                        Text(ps.saveTpButton, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
