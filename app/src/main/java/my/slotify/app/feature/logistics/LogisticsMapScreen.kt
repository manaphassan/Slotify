package my.slotify.app.feature.logistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import java.util.UUID
import java.util.Locale
import kotlin.math.abs

enum class PoiCategory {
    ALL, SEKOLAH, TRANSIT, RUMAH, PEJABAT
}

data class PoiItem(
    val id: String,
    val name: String,
    val category: PoiCategory,
    val iconEmoji: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
    val address: String = ""
)

@Composable
fun poiCategoryColor(category: PoiCategory): Color = when (category) {
    PoiCategory.RUMAH -> SlotifyColors.ParentGreen
    PoiCategory.SEKOLAH -> SlotifyColors.KpmSkyBlue
    PoiCategory.TRANSIT -> SlotifyColors.ActionAmber
    PoiCategory.PEJABAT -> SlotifyColors.TextSecondary
    PoiCategory.ALL -> SlotifyColors.KpmSkyBlue
}

enum class RouteSelection {
    ABAH_PICKUP,
    VAN_PRIMARY,
    VAN_SECONDARY
}

@Composable
fun LogisticsMapScreen(locationGranted: Boolean = false) {
    val context = LocalContext.current
    var selectedRoute by remember { mutableStateOf(RouteSelection.ABAH_PICKUP) }
    var selectedPoiCategory by remember { mutableStateOf(PoiCategory.ALL) }
    var simulationStep by remember { mutableIntStateOf(0) }
    var isSimulating by remember { mutableStateOf(false) }

    // Persistent POI list loaded from storage
    var pois by remember { mutableStateOf(PoiStorage.loadPois(context)) }

    // Dialog & Management states
    var editingPoi by remember { mutableStateOf<PoiItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var poiToDelete by remember { mutableStateOf<PoiItem?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // RF Range Pulse States (3.0 km)
    var isRfPulseActive by remember { mutableStateOf(false) }
    var rfCenterGeoPoint by remember { mutableStateOf<GeoPoint?>(GeoPoint(3.1390, 101.6869)) } // Default: Taman Contoh
    var rfCenterLabel by remember { mutableStateOf("Rumah (Taman Contoh)") }

    // Map Coordinate Picker Mode
    var isMapPickerMode by remember { mutableStateOf(false) }
    var pickedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var pendingPoiDraft by remember { mutableStateOf<PoiItem?>(null) }

    // Reference to MapView for programmatic camera animations
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val filteredPois = if (selectedPoiCategory == PoiCategory.ALL) pois else pois.filter { it.category == selectedPoiCategory }

    // Map tap handler for selecting coordinates or relocating RF transmitter
    val handleMapTap: (GeoPoint) -> Unit = { geo ->
        if (isMapPickerMode) {
            pickedGeoPoint = geo
        } else {
            rfCenterGeoPoint = geo
            rfCenterLabel = "Titik Peta (${String.format(Locale.US, "%.4f, %.4f", geo.latitude, geo.longitude)})"
            isRfPulseActive = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Permission banner if location is not granted
        if (!locationGranted) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlotifyColors.ActionAmber.copy(alpha = 0.12f), SlotifyShapes.Medium)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Slotify perlukan kebenaran lokasi untuk tunjukkan kedudukan sebenar dalam peta. Sila buka Tetapan > Kebenaran > Lokasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlotifyColors.TextSecondary
                    )
                }
            }
        }

        // Header
        item {
            Column {
                Text(
                    text = strings.mapTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = SlotifyColors.TextPrimary
                )
                Text(
                    text = strings.mapSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlotifyColors.TextSecondary
                )
            }
        }

        // Live OpenStreetMap (osmdroid) Map
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                    .clip(SlotifyShapes.Large)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            // Intercept touch to prevent LazyColumn scrolling
                            setOnTouchListener { v, _ ->
                                v.parent.requestDisallowInterceptTouchEvent(true)
                                false
                            }

                            // Adjust zoom and center to fit all markers (Bandar Contoh Office to Taman Contoh)
                            controller.setZoom(12.6)
                            controller.setCenter(GeoPoint(3.1390, 101.6869))

                            // Initial overlay setup
                            setupMapOverlays(this, pois, rfCenterGeoPoint, isRfPulseActive, pickedGeoPoint, handleMapTap)
                            mapViewRef = this
                        }
                    },
                    update = { mapView ->
                        setupMapOverlays(mapView, pois, rfCenterGeoPoint, isRfPulseActive, pickedGeoPoint, handleMapTap)
                    }
                )

                // Floating Action Buttons on Map (Top-Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Recenter Map Button
                    SmallFloatingActionButton(
                        onClick = {
                            mapViewRef?.controller?.apply {
                                setZoom(12.6)
                                animateTo(GeoPoint(3.1390, 101.6869))
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                        contentColor = SlotifyColors.KpmSkyBlue,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Pusatkan Peta", modifier = Modifier.size(18.dp))
                    }

                    // 2. RF Range 3.0 km Radar Pulse Toggle
                    SmallFloatingActionButton(
                        onClick = {
                            isRfPulseActive = !isRfPulseActive
                            if (isRfPulseActive && rfCenterGeoPoint == null) {
                                rfCenterGeoPoint = GeoPoint(3.1390, 101.6869)
                                rfCenterLabel = "Rumah (Taman Contoh)"
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        containerColor = if (isRfPulseActive) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                        contentColor = if (isRfPulseActive) Color.White else SlotifyColors.KpmSkyBlue,
                        shape = CircleShape
                    ) {
                        Text("📡", fontSize = 16.sp)
                    }
                }

                // Interactive Map Picker Banner (Bottom-Center of Map)
                if (isMapPickerMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = SlotifyShapes.Medium,
                        color = SlotifyColors.SurfaceCard.copy(alpha = 0.96f),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Mod Pilih Lokasi di Atas Peta",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                    Text(
                                        text = pickedGeoPoint?.let { "Koordinat: ${String.format(Locale.US, "%.4f, %.4f", it.latitude, it.longitude)}" }
                                            ?: "Sentuh mana-mana titik pada peta untuk pilih...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (pickedGeoPoint != null) SlotifyColors.KpmSkyBlue else SlotifyColors.TextSecondary
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        isMapPickerMode = false
                                        pickedGeoPoint = null
                                        if (pendingPoiDraft != null) {
                                            editingPoi = pendingPoiDraft
                                            showEditDialog = true
                                        }
                                    }
                                ) {
                                    Text("Batal", color = SlotifyColors.TextSecondary, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        pickedGeoPoint?.let { pt ->
                                            if (pendingPoiDraft != null) {
                                                val home = pois.find { it.category == PoiCategory.RUMAH }
                                                val dist = if (home != null) {
                                                    calculateDistanceKm(home.lat, home.lng, pt.latitude, pt.longitude)
                                                } else 0.0
                                                editingPoi = pendingPoiDraft?.copy(
                                                    lat = pt.latitude,
                                                    lng = pt.longitude,
                                                    distanceKm = dist
                                                )
                                                showEditDialog = true
                                            }
                                            rfCenterGeoPoint = pt
                                            rfCenterLabel = "Titik Peta (${String.format(Locale.US, "%.4f, %.4f", pt.latitude, pt.longitude)})"
                                        }
                                        isMapPickerMode = false
                                    },
                                    enabled = pickedGeoPoint != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                                    shape = SlotifyShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("✅ Gunakan Koordinat", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // POI Category Filter Pills
        item {
            val categories = listOf(
                PoiCategory.ALL to strings.poiAll,
                PoiCategory.SEKOLAH to strings.poiSchool,
                PoiCategory.TRANSIT to strings.poiTransit,
                PoiCategory.RUMAH to strings.poiHome,
                PoiCategory.PEJABAT to strings.poiOffice
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { (cat, label) ->
                    val isSelected = selectedPoiCategory == cat
                    Box(
                        modifier = Modifier
                            .neumorphicClickable(cornerRadius = 14.dp) { selectedPoiCategory = cat }
                            .background(
                                if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard,
                                SlotifyShapes.Medium
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                        )
                    }
                }
            }
        }

        // RF Range Coverage Diagnostic Section (3.0 km Animated Pulse Analysis)
        if (isRfPulseActive) {
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
                                Text("📡", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Liputan Gelombang RF (3.0 km)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                    Text(
                                        text = "Simulasi liputan LoRa / Radio Paging / Tracker",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.TextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f), SlotifyShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "3.0 km PULSE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.KpmSkyBlue
                                )
                            }
                        }

                        // Transmitter Center Switcher
                        Text(
                            text = "Pusat Pemancar RF: $rfCenterLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.KpmSkyBlue
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val quickBases = listOf(
                                Triple("🏠 Rumah", GeoPoint(3.1390, 101.6869), "Rumah (Taman Contoh)"),
                                Triple("🏢 Pejabat", GeoPoint(3.1390, 101.6869), "Pejabat Abah (Bandar Contoh)"),
                                Triple("🏫 SK", GeoPoint(3.1390, 101.6869), "SK Taman Contoh"),
                                Triple("🎯 Pilih Peta", null, "Sentuh mana-mana pada peta")
                            )
                            items(quickBases) { (btnLabel, geo, name) ->
                                val isSelected = if (geo == null) {
                                    isMapPickerMode
                                } else {
                                    rfCenterGeoPoint?.let { abs(it.latitude - geo.latitude) < 0.0001 && abs(it.longitude - geo.longitude) < 0.0001 } == true
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(SlotifyShapes.Small)
                                        .background(if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
                                        .clickable {
                                            if (geo != null) {
                                                rfCenterGeoPoint = geo
                                                rfCenterLabel = name
                                                mapViewRef?.controller?.animateTo(geo)
                                            } else {
                                                isMapPickerMode = true
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = btnLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                                    )
                                }
                            }
                        }

                        // Analysis of all POIs against the 3.0 km radius
                        rfCenterGeoPoint?.let { center ->
                            Text(
                                text = "Analisis Liputan Nod Lokasi:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextSecondary
                            )

                            val poiAnalysis = pois.map { poi ->
                                val dist = calculateDistanceKm(center.latitude, center.longitude, poi.lat, poi.lng)
                                val inRange = dist <= 3.0
                                Triple(poi, dist, inRange)
                            }.sortedBy { it.second }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                poiAnalysis.forEach { (poi, dist, inRange) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (inRange) SlotifyColors.SuccessMint.copy(alpha = 0.08f)
                                                else Color(0xFFEF4444).copy(alpha = 0.08f),
                                                SlotifyShapes.Small
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(poi.iconEmoji, fontSize = 16.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = poi.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SlotifyColors.TextPrimary
                                                )
                                                Text(
                                                    text = if (inRange) {
                                                        if (dist <= 1.5) "Sinyal Sangat Kuat • Direct RF"
                                                        else "Sinyal Sederhana • Dalam 3.0 km"
                                                    } else {
                                                        "Luar Jarak 3.0 km • Perlu Selular"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (inRange) SlotifyColors.SuccessMint else Color(0xFFEF4444)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${dist} km",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (inRange) SlotifyColors.SuccessMint else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Route Selector Cards: [Abah | Van A | Van Sekolah B]
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
                            Icon(Icons.Default.Route, contentDescription = null, tint = SlotifyColors.KpmSkyBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.routeChainTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                        }

                        // Simulation button
                        Button(
                            onClick = {
                                isSimulating = true
                                simulationStep = (simulationStep + 1) % 4
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ActionAmber),
                            shape = SlotifyShapes.Small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (simulationStep == 0) strings.startSimulation else "${strings.stepSimulationPrefix} ${simulationStep}/3",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // 3 Route Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RouteTabCard(
                            title = strings.routeAbah,
                            subtitle = "ETA ~18 Minit",
                            isSelected = selectedRoute == RouteSelection.ABAH_PICKUP,
                            modifier = Modifier.weight(1f)
                        ) { selectedRoute = RouteSelection.ABAH_PICKUP }

                        RouteTabCard(
                            title = strings.routeVanPrimary,
                            subtitle = "Adam (SK)",
                            isSelected = selectedRoute == RouteSelection.VAN_PRIMARY,
                            modifier = Modifier.weight(1f)
                        ) { selectedRoute = RouteSelection.VAN_PRIMARY }

                        RouteTabCard(
                            title = strings.routeVanSecondary,
                            subtitle = "Aisyah (SRAI)",
                            isSelected = selectedRoute == RouteSelection.VAN_SECONDARY,
                            modifier = Modifier.weight(1f)
                        ) { selectedRoute = RouteSelection.VAN_SECONDARY }
                    }
                }
            }
        }

        // Turn-by-Turn Directions Steps for Selected Route
        item {
            val steps = when (selectedRoute) {
                RouteSelection.ABAH_PICKUP -> listOf(
                    Triple("1. Pejabat Abah (Bandar Contoh) ➔ Tadika Danial", "7.2 km • 12 min", "Ambil Danial dari Daycare"),
                    Triple("2. Tadika Danial ➔ Taska Ceria", "1.2 km • 3 min", "Ambil Aisyah & beg transit Adam"),
                    Triple("3. Taska Ceria ➔ Rumah", "1.5 km • 3 min", "Laluan pulang ke Taman Contoh")
                )
                RouteSelection.VAN_PRIMARY -> listOf(
                    Triple("1. Rumah ➔ SRA Taman Contoh", "1.8 km • 5 min", "Sesi Pagi SRA (07:30 AM)"),
                    Triple("2. SRA ➔ Taska Ceria", "1.2 km • 4 min", "Transit tengah hari (11:15 AM)"),
                    Triple("3. Taska ➔ SK Taman Contoh", "1.5 km • 5 min", "Sesi Petang SK (12:45 PM)"),
                    Triple("4. SK ➔ Rumah", "2.4 km • 8 min", "Hantar pulang petang (06:10 PM)")
                )
                RouteSelection.VAN_SECONDARY -> listOf(
                    Triple("1. Rumah ➔ SRAI Bukit Contoh", "4.2 km • 10 min", "Sesi Penuh SRAI (07:15 AM)"),
                    Triple("2. SRAI ➔ Taska Ceria", "3.8 km • 8 min", "Ambil Aisyah selepas asar (04:00 PM)"),
                    Triple("3. Taska ➔ Rumah", "1.2 km • 4 min", "Hantar pulang ke rumah")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.turnByTurnTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )

                steps.forEachIndexed { index, (stepTitle, stepEta, stepDesc) ->
                    val isCurrentSim = isSimulating && simulationStep == (index + 1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(
                                elevation = if (isCurrentSim) NeumorphicElevation.HIGH else NeumorphicElevation.LOW,
                                cornerRadius = 14.dp
                            )
                            .background(
                                if (isCurrentSim) SlotifyColors.ActionAmber.copy(alpha = 0.12f) else SlotifyColors.SurfaceCard,
                                SlotifyShapes.Medium
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (isCurrentSim) SlotifyColors.ActionAmber else SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrentSim) Color.White else SlotifyColors.KpmSkyBlue
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stepTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                    Text(
                                        text = stepDesc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = stepEta,
                                modifier = Modifier.padding(end = 64.dp), // Clearance for Speed Dial FAB
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isCurrentSim) SlotifyColors.ActionAmber else SlotifyColors.SuccessMint
                            )
                        }
                    }
                }
            }
        }

        // Points of Interest (POI) Listing & Management Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Senarai Nod Lokasi (${filteredPois.size} POI)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.TextPrimary
                        )
                        Text(
                            text = "Ketik kad untuk fokus peta • Urus nod lokasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlotifyColors.TextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset to default POIs
                        IconButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = "Set Semula Asal",
                                tint = SlotifyColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Add new POI button
                        Button(
                            onClick = {
                                editingPoi = null
                                showEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                            shape = SlotifyShapes.Small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                filteredPois.forEach { poi ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 14.dp)
                            .background(SlotifyColors.SurfaceCard, SlotifyShapes.Medium)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left clickable area to focus map & open infowindow
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(SlotifyShapes.Small)
                                    .clickable {
                                        mapViewRef?.let { mv ->
                                            mv.controller.animateTo(GeoPoint(poi.lat, poi.lng))
                                            mv.controller.setZoom(14.5)
                                            mv.overlays.filterIsInstance<Marker>().find { it.id == poi.id }?.showInfoWindow()
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(poi.iconEmoji, fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = poi.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                    if (poi.address.isNotBlank()) {
                                        Text(
                                            text = poi.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Koordinat: ${String.format("%.4f, %.4f", poi.lat, poi.lng)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.TextSecondary
                                    )
                                }
                            }

                            // Right action buttons with distinct touch targets
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val poiColor = poiCategoryColor(poi.category)
                                Box(
                                    modifier = Modifier
                                        .background(poiColor.copy(alpha = 0.15f), SlotifyShapes.XSmall)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (poi.distanceKm > 0) "${poi.distanceKm} km" else "Pusat",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = poiColor
                                    )
                                }

                                // Edit POI button
                                IconButton(
                                    onClick = {
                                        editingPoi = poi
                                        showEditDialog = true
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.12f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit POI",
                                        tint = SlotifyColors.KpmSkyBlue,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Delete POI button
                                IconButton(
                                    onClick = {
                                        poiToDelete = poi
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Padam POI",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
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

    // Add / Edit POI Dialog
    if (showEditDialog) {
        PoiEditDialog(
            poi = editingPoi,
            onDismiss = {
                showEditDialog = false
                editingPoi = null
                pendingPoiDraft = null
            },
            onPickFromMap = { draft ->
                pendingPoiDraft = draft
                showEditDialog = false
                isMapPickerMode = true
                pickedGeoPoint = GeoPoint(draft.lat, draft.lng)
                mapViewRef?.controller?.apply {
                    setZoom(13.5)
                    animateTo(GeoPoint(draft.lat, draft.lng))
                }
            },
            onSave = { savedPoi ->
                val updated = if (editingPoi != null && pois.any { it.id == savedPoi.id }) {
                    pois.map { if (it.id == savedPoi.id) savedPoi else it }
                } else {
                    pois + savedPoi
                }
                pois = updated
                PoiStorage.savePois(context, updated)
                showEditDialog = false
                editingPoi = null
                pendingPoiDraft = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (poiToDelete != null) {
        AlertDialog(
            onDismissRequest = { poiToDelete = null },
            title = {
                Text(
                    text = "Padam Nod POI?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Adakah anda pasti mahu memadam '${poiToDelete?.name}' daripada senarai dan peta logistik?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlotifyColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        poiToDelete?.let { target ->
                            val updated = pois.filter { it.id != target.id }
                            pois = updated
                            PoiStorage.savePois(context, updated)
                        }
                        poiToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Padam", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { poiToDelete = null }) {
                    Text("Batal", color = SlotifyColors.TextSecondary)
                }
            }
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = "Set Semula Senarai POI?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Tindakan ini akan mengembalikan semua nod lokasi kepada senarai asal Slotify (Rumah, SK, SRA, Taska, Tadika, SRAI, dan Pejabat Abah Bandar Contoh).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlotifyColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val defaults = PoiStorage.resetToDefaults(context)
                        pois = defaults
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ActionAmber)
                ) {
                    Text("Set Semula", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Batal", color = SlotifyColors.TextSecondary)
                }
            }
        )
    }
}

/**
 * Distance calculator between two coordinates in kilometers (1 decimal place)
 */
fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
    return String.format(Locale.US, "%.1f", results[0] / 1000.0).toDoubleOrNull() ?: 0.0
}

/**
 * Renders Route polyline, POI markers, RF Pulse 3km overlay, and picked point onto MapView
 */
private fun setupMapOverlays(
    mapView: MapView,
    pois: List<PoiItem>,
    rfCenter: GeoPoint?,
    isRfActive: Boolean,
    pickedPoint: GeoPoint?,
    onMapTapped: (GeoPoint) -> Unit
) {
    // Keep tile overlays, clear custom Markers, Polylines and RfPulseOverlay
    mapView.overlays.removeAll { it is Marker || it is Polyline || it is RfPulseOverlay }

    // 1. RF Range Pulse 3.0 km Overlay (animated radar waves and distance markers)
    val rfOverlay = RfPulseOverlay(
        center = rfCenter,
        isPulseActive = isRfActive,
        maxRadiusMeters = 3000.0,
        centerLabel = "Pusat RF 3.0 km",
        onMapTapped = onMapTapped,
        onMapLongPressed = onMapTapped
    )
    mapView.overlays.add(rfOverlay)

    // 2. Route polyline: Office (or first Pejabat) → Tadika → Taska → Home
    val officePoi = pois.find { it.category == PoiCategory.PEJABAT }
    val tadikaPoi = pois.find { it.id == "poi-5" } ?: pois.find { it.category == PoiCategory.TRANSIT }
    val taskaPoi = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT && it.id != tadikaPoi?.id }
    val homePoi = pois.find { it.category == PoiCategory.RUMAH }

    val routePoints = mutableListOf<GeoPoint>()
    officePoi?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
    tadikaPoi?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
    taskaPoi?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
    homePoi?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }

    if (routePoints.size >= 2) {
        val polyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = android.graphics.Color.parseColor("#0284C7")
            outlinePaint.strokeWidth = 10f
        }
        mapView.overlays.add(polyline)
    }

    // 3. POI Markers
    val markerColors = mapOf(
        PoiCategory.RUMAH    to android.graphics.Color.parseColor("#22C55E"),
        PoiCategory.SEKOLAH  to android.graphics.Color.parseColor("#0284C7"),
        PoiCategory.TRANSIT  to android.graphics.Color.parseColor("#F59E0B"),
        PoiCategory.PEJABAT  to android.graphics.Color.parseColor("#64748B")
    )

    pois.forEach { poi ->
        val marker = Marker(mapView).apply {
            id = poi.id
            position = GeoPoint(poi.lat, poi.lng)
            title = poi.name
            snippet = if (poi.address.isNotBlank()) {
                "${poi.iconEmoji} ${poi.address} (${if (poi.distanceKm > 0) "${poi.distanceKm} km" else "Pusat"})"
            } else {
                "${poi.iconEmoji} • ${if (poi.distanceKm > 0) "${poi.distanceKm} km" else "Pusat"}"
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(markerColors[poi.category] ?: android.graphics.Color.GRAY)
                setStroke(4, android.graphics.Color.WHITE)
                setSize(48, 48)
            }
        }
        mapView.overlays.add(marker)
    }

    // 4. Temporary Marker for picked coordinate on map
    if (pickedPoint != null) {
        val pickMarker = Marker(mapView).apply {
            id = "temp_picked_pin"
            position = pickedPoint
            title = "📍 Lokasi Dipilih di Peta"
            snippet = String.format(Locale.US, "%.4f, %.4f", pickedPoint.latitude, pickedPoint.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#EF4444")) // Crimson Red
                setStroke(4, android.graphics.Color.WHITE)
                setSize(52, 52)
            }
        }
        mapView.overlays.add(pickMarker)
        pickMarker.showInfoWindow()
    }

    mapView.invalidate()
}

/**
 * Add / Edit POI Dialog with Emoji selection, Category chips, Map Picker, and Coordinates
 */
@Composable
private fun PoiEditDialog(
    poi: PoiItem?,
    onDismiss: () -> Unit,
    onPickFromMap: (PoiItem) -> Unit,
    onSave: (PoiItem) -> Unit
) {
    val isEdit = poi != null
    var name by remember { mutableStateOf(poi?.name ?: "") }
    var category by remember { mutableStateOf(poi?.category ?: PoiCategory.SEKOLAH) }
    var iconEmoji by remember { mutableStateOf(poi?.iconEmoji ?: "🏢") }
    var address by remember { mutableStateOf(poi?.address ?: "") }
    var latText by remember { mutableStateOf(if (poi != null) String.format(Locale.US, "%.4f", poi.lat) else "3.1390") }
    var lngText by remember { mutableStateOf(if (poi != null) String.format(Locale.US, "%.4f", poi.lng) else "101.6869") }
    var distanceText by remember { mutableStateOf(if (poi != null) String.format(Locale.US, "%.1f", poi.distanceKm) else "5.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetEmojis = listOf("🏢", "🏫", "🎒", "🏠", "🕌", "🧒", "📚", "🏬", "🩺", "🚌", "📍")
    val categories = listOf(
        PoiCategory.SEKOLAH to "Sekolah",
        PoiCategory.TRANSIT to "Transit",
        PoiCategory.RUMAH to "Rumah",
        PoiCategory.PEJABAT to "Pejabat"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Kemaskini Nod POI" else "Tambah Nod POI Baru",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SlotifyColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lokasi / POI") },
                    placeholder = { Text("cth: Pejabat Bandar Contoh / Tadika") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Text(
                    text = "Kategori Lokasi:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (cat, label) ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SlotifyShapes.Small)
                                .background(
                                    if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard
                                )
                                .clickable { category = cat }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                            )
                        }
                    }
                }

                // Emoji Selection
                Text(
                    text = "Pilih Ikon Emoji:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetEmojis) { emoji ->
                        val isSelected = iconEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) SlotifyColors.KpmSkyBlue.copy(alpha = 0.25f) else SlotifyColors.SurfaceCard
                                )
                                .clickable { iconEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat / Keterangan Lokasi") },
                    placeholder = { Text("cth: No 9, Tingkat 2, Jalan Contoh...") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Coordinates: Lat & Lng
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Map Picker button & Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val currentDraft = PoiItem(
                                id = poi?.id ?: "poi-${UUID.randomUUID().toString().take(8)}",
                                name = name.trim(),
                                category = category,
                                iconEmoji = iconEmoji,
                                lat = latText.toDoubleOrNull() ?: 3.1390,
                                lng = lngText.toDoubleOrNull() ?: 101.6869,
                                distanceKm = distanceText.toDoubleOrNull() ?: 0.0,
                                address = address.trim()
                            )
                            onPickFromMap(currentDraft)
                        },
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("🗺️ Pilih di Peta", fontSize = 10.sp, color = SlotifyColors.KpmSkyBlue, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = {
                                latText = "3.1390"
                                lngText = "101.6869"
                                if (address.isBlank()) address = "Pusat Bandar Contoh, Bandar Contoh, Selangor"
                            },
                            shape = SlotifyShapes.Small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("📍 Bandar Contoh", fontSize = 10.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                latText = "3.1390"
                                lngText = "101.6869"
                                if (address.isBlank()) address = "Taman Contoh, Bandar Contoh"
                            },
                            shape = SlotifyShapes.Small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("📍 Taman Contoh", fontSize = 10.sp)
                        }
                    }
                }

                // Distance Km
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("Jarak Anggaran dari Rumah (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val latVal = latText.toDoubleOrNull()
                    val lngVal = lngText.toDoubleOrNull()
                    val distVal = distanceText.toDoubleOrNull() ?: 0.0

                    if (name.isBlank()) {
                        errorMessage = "Sila masukkan nama lokasi"
                        return@Button
                    }
                    if (latVal == null || lngVal == null) {
                        errorMessage = "Koordinat latitude & longitude tidak sah"
                        return@Button
                    }

                    val updatedItem = PoiItem(
                        id = poi?.id ?: "poi-${UUID.randomUUID().toString().take(8)}",
                        name = name.trim(),
                        category = category,
                        iconEmoji = iconEmoji,
                        lat = latVal,
                        lng = lngVal,
                        distanceKm = distVal,
                        address = address.trim()
                    )
                    onSave(updatedItem)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue)
            ) {
                Text(if (isEdit) "Simpan" else "Tambah POI", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = SlotifyColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun RouteTabCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(SlotifyShapes.Medium)
            .background(if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else SlotifyColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else SlotifyColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}
