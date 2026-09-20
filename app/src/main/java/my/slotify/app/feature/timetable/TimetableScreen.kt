package my.slotify.app.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.model.KpmSubjectCatalog

@Composable
fun TimetableScreen(
    selectedStudent: StudentEntity?,
    slots: List<TimetableSlotEntity>,
    onToggleYearInversion: () -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(1) } // 1=Isnin
    val days = listOf("Isnin" to 1, "Selasa" to 2, "Rabu" to 3, "Khamis" to 4, "Jumaat" to 5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Session Inversion Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Jadual Waktu Kelas",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlotifyColors.TextPrimary
                )
                Text(
                    text = "${selectedStudent?.fullName ?: "Murid"} (${selectedStudent?.currentLevel ?: ""})",
                    fontSize = 12.sp,
                    color = SlotifyColors.TextSecondary
                )
            }

            // Year 4 vs Year 5 Inversion Button
            IconButton(
                onClick = onToggleYearInversion,
                modifier = Modifier
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 24.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.Autorenew, contentDescription = "Tukar Sesi", tint = SlotifyColors.KpmBlue)
            }
        }

        // Days Filter Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { (dayName, dayIndex) ->
                val isSelected = selectedDay == dayIndex
                Box(
                    modifier = Modifier
                        .neumorphicClickable(cornerRadius = 14.dp) { selectedDay = dayIndex }
                        .background(
                            if (isSelected) SlotifyColors.KpmBlue else SlotifyColors.SurfaceCard,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = dayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                    )
                }
            }
        }

        // Timetable Slots List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val displaySlots = if (slots.isEmpty()) {
                // Fallback default sample slots for demonstration
                listOf(
                    TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 1, startTime = "08:00", endTime = "08:30", subjectCode = "BM", subjectName = "Bahasa Melayu"),
                    TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 2, startTime = "08:30", endTime = "09:30", subjectCode = "MT", subjectName = "Matematik"),
                    TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 3, startTime = "09:30", endTime = "10:00", subjectCode = "REHAT", subjectName = "Waktu Rehat", isRehat = true),
                    TimetableSlotEntity(sessionId = "1", dayOfWeek = 1, periodIndex = 4, startTime = "10:00", endTime = "11:00", subjectCode = "SN", subjectName = "Sains")
                )
            } else slots

            items(displaySlots) { slot ->
                val subject = KpmSubjectCatalog.getSubject(slot.subjectCode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(
                            elevation = if (slot.isRehat) NeumorphicElevation.LOW else NeumorphicElevation.MEDIUM,
                            cornerRadius = 14.dp
                        )
                        .background(SlotifyColors.SurfaceCard, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(subject.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = subject.code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = subject.color
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = slot.subjectName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = slot.teacherName ?: "Guru Subjek",
                                    fontSize = 11.sp,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "${slot.startTime} – ${slot.endTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
