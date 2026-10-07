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
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings

// Fix 1.4: Mask child national ID — show only last 4 digits by default
private fun String.maskMyKid(): String {
    if (length < 6) return this
    return "••••-••-${takeLast(4)}"
}

// Fix 4.2: Map raw database enum strings to human-readable Bahasa Melayu labels
private fun String.toSessionTypeLabel(): String = when (this) {
    "DUAL_SRA_SK"       -> "Sesi Dual (SRA + SK)"
    "INTEGRATED_SRAISM" -> "Sesi Bersepadu Islam"
    "TADIKA_DAYCARE"    -> "Tadika & Daycare"
    "FULL_DAY"          -> "Sesi Penuh"
    "MORNING"           -> "Sesi Pagi"
    "AFTERNOON"         -> "Sesi Petang"
    else                -> this
}

@Composable
fun StudentProfileScreen(
    students: List<StudentEntity>,
    selectedStudent: StudentEntity?,
    onSelectStudent: (StudentEntity) -> Unit,
    onSaveProfile: (StudentEntity) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    // Fix 1.4: Show/hide toggle for MyKid number — hidden by default for privacy
    var showMyKid by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.profileTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = strings.profileSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlotifyColors.TextSecondary
                    )
                }

                // Edit Button
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = SlotifyShapes.LargeDp)
                        .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = SlotifyColors.KpmSkyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Child Switcher Row
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
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (s.className.isNotBlank()) "Kelas: ${s.className}" else "Kelas KPM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

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
                                    contentDescription = if (showMyKid) "Sembunyikan" else "Tunjukkan",
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
                                value = if (bmi > 0) String.format("%.1f", bmi) else "-"
                            )
                        }
                    }
                }
            }

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
                                text = s.sessionType.toSessionTypeLabel(),
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
                                        text = "Sekolah Agama / Transit:",
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
                                    text = s.vanDriverName ?: "Dihantar Ibu Bapa",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = s.vanDriverPhone ?: "Kenderaan Sendiri",
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
                                text = "Pengangkutan Aktif",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.SuccessMint
                            )
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
                onSaveProfile(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun BiometricMetricItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = SlotifyColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SlotifyColors.TextPrimary)
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
