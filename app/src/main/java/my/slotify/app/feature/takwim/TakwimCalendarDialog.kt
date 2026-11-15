package my.slotify.app.feature.takwim

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.takwim.HolidayType
import my.slotify.app.core.takwim.KpmTakwimRepository
import my.slotify.app.core.takwim.SchoolHoliday
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TakwimCalendarDialog(
    onDismissRequest: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val ts = strings.timetableUi
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: Cuti Penggal, 2: Cuti Umum

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ms-MY"))
    }

    val filteredHolidays = remember(selectedTab) {
        when (selectedTab) {
            1 -> KpmTakwimRepository.holidays.filter { it.type == HolidayType.CUTI_PENGGAL || it.type == HolidayType.CUTI_AKHIR_SESI }
            2 -> KpmTakwimRepository.holidays.filter { it.type == HolidayType.CUTI_UMUM || it.type == HolidayType.CUTI_PERISTIWA }
            else -> KpmTakwimRepository.holidays
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(SlotifyColors.CanvasBase)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SlotifyColors.KpmBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗓️", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = ts.takwimTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                text = ts.takwimSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.KpmBlue
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = SlotifyColors.TextSecondary)
                    }
                }

                // Current Term Banner
                val currentTerm = remember { KpmTakwimRepository.getCurrentTerm(today) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 12.dp)
                        .background(SlotifyColors.SurfaceCard, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ts.academicStatusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextSecondary
                            )
                            Text(
                                text = currentTerm?.let { ts.currentTermOngoing(it.name) } ?: ts.sessionYearFallback,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.ScienceGreen
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlotifyColors.ScienceGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = ts.groupBadge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.ScienceGreen
                            )
                        }
                    }
                }

                // Filter Tabs (Semua / Cuti Penggal / Cuti Umum)
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SlotifyColors.SurfaceCard,
                    contentColor = SlotifyColors.KpmBlue,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(ts.takwimTabAll, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(ts.takwimTabTermBreaks, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(ts.takwimTabPublicHolidays, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Holidays List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredHolidays, key = { it.id }) { holiday ->
                        val isOngoing = holiday.isOngoing(today)
                        val daysUntil = holiday.daysUntil(today)
                        val isPast = holiday.endDate.isBefore(today)

                        val badgeBg = when {
                            isOngoing -> SlotifyColors.ScienceGreen.copy(alpha = 0.15f)
                            daysUntil in 1..14 -> SlotifyColors.MathAmber.copy(alpha = 0.15f)
                            isPast -> SlotifyColors.TextTertiary.copy(alpha = 0.1f)
                            else -> SlotifyColors.KpmBlue.copy(alpha = 0.1f)
                        }

                        val badgeColor = when {
                            isOngoing -> SlotifyColors.ScienceGreen
                            daysUntil in 1..14 -> SlotifyColors.MathAmber
                            isPast -> SlotifyColors.TextTertiary
                            else -> SlotifyColors.KpmBlue
                        }

                        val badgeText = when {
                            isOngoing -> ts.holidayOngoing
                            daysUntil == 0L -> ts.holidayToday
                            daysUntil > 0 -> ts.holidayDaysUntil(daysUntil)
                            else -> ts.holidayPast
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 12.dp)
                                .background(SlotifyColors.SurfaceCard, RoundedCornerShape(12.dp))
                                .padding(12.dp)
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
                                    Text(holiday.emoji, fontSize = 24.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = holiday.titleBm,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary
                                        )
                                        val dateText = if (holiday.startDate == holiday.endDate) {
                                            holiday.startDate.format(dateFormatter)
                                        } else {
                                            "${holiday.startDate.format(dateFormatter)} – ${holiday.endDate.format(dateFormatter)}"
                                        }
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.TextSecondary
                                        )
                                        if (holiday.description.isNotEmpty()) {
                                            Text(
                                                text = holiday.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SlotifyColors.TextTertiary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = badgeText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Close Button
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlotifyColors.KpmBlue
                    )
                ) {
                    Text(ts.closeTakwimButton, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
