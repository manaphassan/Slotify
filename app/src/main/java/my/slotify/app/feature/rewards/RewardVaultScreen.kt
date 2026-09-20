package my.slotify.app.feature.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.neumorphic

data class RewardCatalogItem(
    val title: String,
    val description: String,
    val pointsCost: Int,
    val iconEmoji: String
)

@Composable
fun RewardVaultScreen(
    selectedStudent: StudentEntity?,
    onRedeemReward: (RewardCatalogItem) -> Unit
) {
    val rewardItems = listOf(
        RewardCatalogItem("Main Roblox / iPad 1 Jam", "Ganjaran bermain peranti dengan had waktu", 50, "🎮"),
        RewardCatalogItem("Beli Mainan / Hot Wheels", "Tebus mainan kegemaran pilihan anak", 200, "🏎️"),
        RewardCatalogItem("Keluar Makan Ais Krim", "Ais krim / pencuci mulut hujung minggu", 30, "🍦"),
        RewardCatalogItem("Tidur Lewat 30 Minit (Malam Minggu)", "Kelonggaran waktu tidur hari Sabtu", 20, "🌙")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Star Balance Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 20.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tabung Bintang ${selectedStudent?.nickname ?: "Anak"}",
                        fontSize = 14.sp,
                        color = SlotifyColors.TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SlotifyColors.MathAmber, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${selectedStudent?.starCoinsBalance ?: 0} ⭐",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                    Text(
                        text = "Kumpul bintang dengan siapkan latihan & kemas beg sekolah",
                        fontSize = 11.sp,
                        color = SlotifyColors.TextSecondary
                    )
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Katalog Penebusan Hadiah",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlotifyColors.TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SlotifyColors.TextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Kelulusan Ibu Bapa", fontSize = 11.sp, color = SlotifyColors.TextTertiary)
                }
            }
        }

        // Reward Catalog Items
        items(rewardItems) { item ->
            val canAfford = (selectedStudent?.starCoinsBalance ?: 0) >= item.pointsCost
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(16.dp)
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
                        Text(text = item.iconEmoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlotifyColors.TextPrimary)
                            Text(text = item.description, fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                        }
                    }

                    Button(
                        onClick = { onRedeemReward(item) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlotifyColors.KokuPurple,
                            disabledContainerColor = SlotifyColors.TextTertiary.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "${item.pointsCost} ⭐", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
