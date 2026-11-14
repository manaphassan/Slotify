package my.slotify.app.feature.dashboard

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.profile.FamilyProfileData
import my.slotify.app.core.rewards.StarRewards
import java.time.LocalDate

data class BagItem(
    val id: String,
    val title: String,
    val category: String,
    val isMandatory: Boolean = true
)

object SchoolBagHelper {
    private const val PREFS_NAME = "slotify_bag_pack_prefs"

    fun isBagPackedToday(context: Context, studentId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = LocalDate.now().toString()
        return prefs.getBoolean("packed_${studentId}_$todayKey", false)
    }

    fun setBagPackedToday(context: Context, studentId: String, packed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = LocalDate.now().toString()
        prefs.edit().putBoolean("packed_${studentId}_$todayKey", packed).apply()
    }

    fun getDefaultItemsForStudent(student: StudentEntity, slots: List<TimetableSlotEntity>): List<BagItem> {
        val subjectItems = slots
            .filter { !it.isRehat }
            .distinctBy { it.subjectCode }
            .map { slot ->
                BagItem(
                    id = "sub-${slot.subjectCode}",
                    title = "Buku Teks & Aktiviti ${slot.subjectName}",
                    category = "Buku Pelajaran"
                )
            }

        val baseItems = FamilyProfileData.bagEssentials[student.id] ?: FamilyProfileData.defaultBagEssentials

        return if (subjectItems.isNotEmpty()) (subjectItems + baseItems).distinctBy { it.title } else baseItems
    }
}

/**
 * Checklist for the next school day's bag. When every item is ticked, [onAllPacked] is invoked;
 * the caller records the (idempotent, once-per-day) reward claim.
 */
@Composable
fun SchoolBagPackDialog(
    student: StudentEntity,
    tomorrowSlots: List<TimetableSlotEntity> = emptyList(),
    alreadyPacked: Boolean = false,
    onAllPacked: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val bagItems = remember(student.id, tomorrowSlots) {
        SchoolBagHelper.getDefaultItemsForStudent(student, tomorrowSlots)
    }

    val checkedStates = remember(student.id, bagItems) {
        mutableStateMapOf<String, Boolean>().apply {
            bagItems.forEach { item -> put(item.id, alreadyPacked) }
        }
    }

    val totalCount = bagItems.size
    val checkedCount = checkedStates.values.count { it }
    val isAllChecked = checkedCount == totalCount && totalCount > 0

    val surfaceCardColor = SlotifyColors.SurfaceCard
    val successColor = SlotifyColors.SuccessMint
    val skyBlueColor = SlotifyColors.KpmSkyBlue
    val starColor = SlotifyColors.StarGold
    val textPrimaryColor = SlotifyColors.TextPrimary
    val textSecondaryColor = SlotifyColors.TextSecondary
    val canvasBaseColor = SlotifyColors.CanvasBase

    LaunchedEffect(isAllChecked) {
        if (isAllChecked && !alreadyPacked) onAllPacked()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceCardColor,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(skyBlueColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎒", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Kemas Beg Sekolah",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = "${student.avatarEmoji} ${student.fullName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = skyBlueColor
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = textSecondaryColor)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isAllChecked) successColor else skyBlueColor,
                    trackColor = canvasBaseColor
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$checkedCount daripada $totalCount item dimasukkan",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondaryColor
                    )
                    if (isAllChecked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = starColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "+${StarRewards.BAG_PACKED_STARS} Bintang Diperoleh!",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = starColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Checklist Items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(bagItems, key = { _, item -> item.id }) { _, item ->
                        val isChecked = checkedStates[item.id] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isChecked) successColor.copy(alpha = 0.1f) else canvasBaseColor)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    checkedStates[item.id] = !isChecked
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) successColor else Color.Transparent)
                                    .border(
                                        width = 1.8.dp,
                                        color = if (isChecked) successColor else textSecondaryColor.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isChecked) FontWeight.Normal else FontWeight.SemiBold,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (isChecked) textSecondaryColor else textPrimaryColor
                                )
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondaryColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAllChecked) successColor else skyBlueColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAllChecked) "Selesai Mengemas ✓" else "Simpan & Tutup",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
