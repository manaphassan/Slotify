package my.slotify.app.feature.logistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.routing.OsrmRouteResponse
import my.slotify.app.core.routing.OsrmRoutingService

@Composable
fun LogisticsMapScreen() {
    var routeData by remember { mutableStateOf<OsrmRouteResponse?>(null) }
    val routingService = remember { OsrmRoutingService() }

    LaunchedEffect(Unit) {
        routeData = routingService.getDrivingRoute("101.6869,3.1390;101.6869,3.1390;101.6869,3.1390")
    }

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
                text = "Logistik & Laluan Berantai",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SlotifyColors.TextPrimary
            )
            Text(
                text = "Pengesanan Van Sekolah & Laluan Ambilan Abah",
                fontSize = 12.sp,
                color = SlotifyColors.TextSecondary
            )
        }

        // Parent Chained Route Card (Pejabat ➔ Tadika ➔ Taska ➔ Rumah)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SlotifyColors.ParentGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Laluan Ambilan Petang Abah",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SlotifyColors.TextPrimary
                            )
                        }
                        Text(
                            text = "ETA: ~${routeData?.totalDurationMinutes?.toInt() ?: 28} Minit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlotifyColors.ParentGreen
                        )
                    }

                    Divider(color = SlotifyColors.BackgroundLight, thickness = 1.dp)

                    // Waypoints list
                    val steps = listOf(
                        "1. Pejabat ➔ Tadika Danial (Daycare)" to "10 min (5.2 km)",
                        "2. Tadika Danial ➔ Taska Ceria (Aisyah)" to "8 min (3.1 km)",
                        "3. Taska Ceria ➔ Rumah" to "10 min (6.2 km)"
                    )
                    steps.forEach { (stepTitle, stepEta) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = SlotifyColors.ScienceGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = stepTitle, fontSize = 12.sp, color = SlotifyColors.TextPrimary)
                            }
                            Text(text = stepEta, fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // Van Streams Status
        item {
            Text(
                text = "Status Pengangkutan Van Anak",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SlotifyColors.TextPrimary
            )
        }

        // Van Adam (Pemandu Van A)
        item {
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
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = SlotifyColors.VanPrimaryBlue)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(text = "Van Sekolah A (Adam)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlotifyColors.TextPrimary)
                            Text(text = "Laluan: SK ➔ Rumah (Petang 06:00 PM)", fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                        }
                    }
                    Text(text = "Dalam Sesi", fontSize = 11.sp, color = SlotifyColors.ScienceGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Van Aisyah (Van Sekolah B)
        item {
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
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = SlotifyColors.VanSecondaryGold)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(text = "Van Sekolah B (Aisyah)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlotifyColors.TextPrimary)
                            Text(text = "Laluan: SRAI ➔ Taska Ceria", fontSize = 11.sp, color = SlotifyColors.TextSecondary)
                        }
                    }
                    Text(text = "Transit Siap", fontSize = 11.sp, color = SlotifyColors.MathAmber, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
