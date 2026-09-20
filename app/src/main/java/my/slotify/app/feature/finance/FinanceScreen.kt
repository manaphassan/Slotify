package my.slotify.app.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.NotificationsActive
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

@Composable
fun FinanceScreen(
    selectedStudent: StudentEntity?
) {
    var spentAmount by remember { mutableDoubleStateOf(3.50) }
    val budget = selectedStudent?.dailyCanteenBudget ?: 5.00
    val saved = (budget - spentAmount).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Kewangan & Duit Poket",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SlotifyColors.TextPrimary
            )
            Text(
                text = "Pantau Duit Belanja Kantin & Peringatan Yuran Sekolah",
                fontSize = 12.sp,
                color = SlotifyColors.TextSecondary
            )
        }

        // Daily Pocket Money Card (Duit Poket)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 18.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SlotifyColors.KpmBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Duit Poket Hari Ini (${selectedStudent?.nickname ?: "Anak"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SlotifyColors.TextPrimary
                            )
                        }
                        Text(
                            text = "Bajet: RM ${String.format("%.2f", budget)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlotifyColors.KpmBlue
                        )
                    }

                    Divider(color = SlotifyColors.BackgroundLight, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Belanja Kantin", fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                            Text(text = "RM ${String.format("%.2f", spentAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlotifyColors.EnglishRed)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Lebihan / Tabung", fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                            Text(text = "+ RM ${String.format("%.2f", saved)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlotifyColors.ScienceGreen)
                        }
                    }
                }
            }
        }

        // Fee Reminders Section
        item {
            Text(
                text = "Pusat Peringatan Yuran Bulanan & PIBG",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SlotifyColors.TextPrimary
            )
        }

        val fees = listOf(
            Triple("Yuran Van Sekolah A (Adam)", "RM 100.00 / bulan", "1hb Setiap Bulan"),
            Triple("Yuran Transit Taska Ceria", "RM 500.00 / bulan", "28hb Setiap Bulan"),
            Triple("Yuran Tadika & Daycare Danial", "RM 450.00 / bulan", "1hb Setiap Bulan"),
            Triple("Yuran PIBG SK 2026", "RM 50.00 / setahun", "Tahunan (April)")
        )

        items(fees.size) { index ->
            val (title, amount, due) = fees[index]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 14.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SlotifyColors.MathAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlotifyColors.TextPrimary)
                            Text(text = due, fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                        }
                    }
                    Text(text = amount, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlotifyColors.TextPrimary)
                }
            }
        }
    }
}
