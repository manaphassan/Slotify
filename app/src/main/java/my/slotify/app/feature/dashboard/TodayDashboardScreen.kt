package my.slotify.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.slotify.app.R
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable

@Composable
fun TodayDashboardScreen(
    students: List<StudentEntity>,
    selectedStudent: StudentEntity?,
    onSelectStudent: (StudentEntity) -> Unit,
    onRewardEarned: (Int) -> Unit
) {
    var bagPacked by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase) // 60% Dominant Base
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = stringResource(R.string.app_name),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlotifyColors.KpmSkyBlue // 30% Structural Blue
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        fontSize = 12.sp,
                        color = SlotifyColors.TextSecondary
                    )
                }

                // 10% Radiant Star Accent
                Box(
                    modifier = Modifier
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 20.dp)
                        .background(SlotifyColors.SurfaceCard, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = SlotifyColors.StarGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${selectedStudent?.starCoinsBalance ?: 0} ⭐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                }
            }
        }

        // Child Profile Switcher Tabs (30% Secondary Structure)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(students) { student ->
                    val isSelected = student.id == selectedStudent?.id
                    Box(
                        modifier = Modifier
                            .neumorphicClickable(cornerRadius = 24.dp) { onSelectStudent(student) }
                            .background(
                                if (isSelected) SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f) else SlotifyColors.SurfaceCard,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = student.avatarEmoji, fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = student.nickname,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = student.currentLevel,
                                    fontSize = 10.sp,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Morning Transit Drop Reminder (Parent & Student Friendly)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 20.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SlotifyColors.IslamTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎒", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.section_transit_drop),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = "Hantar Beg Transit & Baju SK ke Taska Ceria",
                            fontSize = 12.sp,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Ongoing Session Card with 10% Radiant Accent
        item {
            val vanName = selectedStudent?.vanDriverName ?: "Van Sekolah"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 20.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_active_slot),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.ActionAmber // 10% Radiant Action Glow
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = SlotifyColors.VanPrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = vanName,
                                fontSize = 11.sp,
                                color = SlotifyColors.VanPrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Sains — Tahun 4 (Makmal)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = "03:10 PM – 04:10 PM (Sesi Petang SK)",
                        fontSize = 13.sp,
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
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 20.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👕", fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.section_attire),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = "Isnin: Uniform Lengkap KPM (Kemeja Putih + Seluar Biru)",
                            fontSize = 12.sp,
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
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 20.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.section_bag_pack),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SlotifyColors.TextPrimary
                    )

                    val books = listOf(
                        "📘 Buku Teks Bahasa Melayu Jilid 1",
                        "📙 Buku Aktiviti Matematik",
                        "📗 Buku Latihan Sains",
                        "📕 Buku Teks English"
                    )
                    books.forEach { book ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SlotifyColors.SuccessMint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = book, fontSize = 13.sp, color = SlotifyColors.TextPrimary)
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (bagPacked) stringResource(R.string.bag_packed_done) else stringResource(R.string.btn_bag_packed),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
