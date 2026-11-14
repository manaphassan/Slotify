package my.slotify.app.feature.logistics

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import my.slotify.app.core.designsystem.theme.LocationPickerStrings
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.profile.FamilyProfileData
import java.util.Locale

enum class NudgeStepSize(val label: String, val deltaDeg: Double) {
    STEP_1M("1m", 0.000010),
    STEP_10M("10m", 0.000090),
    STEP_50M("50m", 0.000450)
}

/**
 * Fullscreen Precision Location Picker Modal for OpenStreetMap.
 * Features:
 * 1. Grab/Uber-style center floating pin & crosshair reticle.
 * 2. Hardware GPS location synchronization with meter-level accuracy evaluation.
 * 3. Real-time 6-decimal latitude & longitude coordinate tracking.
 * 4. Micro-adjustment Stepper / Nudge D-Pad (1m, 10m, 50m increments) for zero-slip targeting.
 * 5. Automatic reverse-geocoding address lookup via LocationAddressHelper.
 * 6. One-tap preset jumps to the family's saved points of interest.
 * 7. Direct coordinate manual input & validation.
 * 8. Live distance calculation from Home.
 */
@Composable
fun PreciseLocationPickerModal(
    initialLat: Double,
    initialLng: Double,
    title: String = strings.locationPickerUi.defaultTitle,
    homeLat: Double = FamilyProfileData.mapCenter.lat,
    homeLng: Double = FamilyProfileData.mapCenter.lng,
    onDismiss: () -> Unit,
    onConfirmLocation: (lat: Double, lng: Double, address: String, distanceKm: Double) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val ps = strings.locationPickerUi
    val str = strings

    var centerLat by remember { mutableDoubleStateOf(if (initialLat != 0.0) initialLat else homeLat) }
    var centerLng by remember { mutableDoubleStateOf(if (initialLng != 0.0) initialLng else homeLng) }

    var selectedStep by remember { mutableStateOf(NudgeStepSize.STEP_10M) }
    var resolvedAddress by remember { mutableStateOf(ps.loadingAddress) }
    var isGeocoding by remember { mutableStateOf(false) }

    // Real Hardware GPS Location States
    var isGpsFetching by remember { mutableStateOf(false) }
    var activeGpsResult by remember { mutableStateOf<GpsFixResult?>(null) }
    var gpsNotification by remember { mutableStateOf<String?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Manual coordinate paste/input sheet
    var showManualInput by remember { mutableStateOf(false) }
    var manualLatInput by remember { mutableStateOf(String.format(Locale.US, "%.5f", centerLat)) }
    var manualLngInput by remember { mutableStateOf(String.format(Locale.US, "%.5f", centerLng)) }
    var manualInputError by remember { mutableStateOf<String?>(null) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Debounced reverse geocoding when camera movement stops
    LaunchedEffect(centerLat, centerLng) {
        isGeocoding = true
        delay(400) // 400ms debounce
        resolvedAddress = LocationAddressHelper.resolveAddress(context, centerLat, centerLng)
        isGeocoding = false
    }

    val liveDistance = remember(centerLat, centerLng, homeLat, homeLng) {
        calculateDistanceKm(homeLat, homeLng, centerLat, centerLng)
    }

    // Preset jumps: home plus the user's saved points of interest
    val landmarks = remember(context, ps) {
        listOf(Triple("🏠 ${ps.homeLandmark}", homeLat, homeLng)) +
                PoiStorage.loadPois(context)
                    .filter { it.category != PoiCategory.RUMAH }
                    .map { Triple("${it.iconEmoji} ${it.name.substringBefore(" (")}", it.lat, it.lng) }
    }

    // Hardware GPS Location Fetching Function
    suspend fun fetchGpsLocation() {
        if (!GpsLocationHelper.hasLocationPermission(context)) {
            gpsNotification = ps.gpsPermissionRequired
            return
        }

        if (!GpsLocationHelper.isLocationServiceEnabled(context)) {
            gpsNotification = ps.gpsEnableInSettings
            GpsLocationHelper.openLocationSettings(context)
            return
        }

        isGpsFetching = true
        gpsNotification = ps.gpsAcquiringSatellites
        val fix = GpsLocationHelper.getAccurateLocation(context, timeoutMs = 8000L)
        isGpsFetching = false

        if (fix != null) {
            activeGpsResult = fix
            centerLat = fix.latitude
            centerLng = fix.longitude
            gpsNotification = "${fix.quality.badgeEmoji} GPS: ±${
                String.format(
                    Locale.US,
                    "%.1f",
                    fix.accuracyMeters
                )
            }m (${fix.quality.localizedLabel(ps)})"
            mapViewRef?.controller?.apply {
                setZoom(18.0) // 1-meter ground resolution
                animateTo(GeoPoint(fix.latitude, fix.longitude))
            }
        } else {
            gpsNotification = ps.gpsSignalWeak
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            // Enable map overlay
            mapViewRef?.let { mv ->
                if (locationOverlayRef == null) {
                    val provider = GpsMyLocationProvider(context).apply {
                        addLocationSource(android.location.LocationManager.NETWORK_PROVIDER)
                    }
                    val overlay = MyLocationNewOverlay(provider, mv).apply {
                        enableMyLocation()
                        setDrawAccuracyEnabled(true)
                    }
                    locationOverlayRef = overlay
                    mv.overlays.add(overlay)
                    mv.invalidate()
                }
            }
            coroutineScope.launch {
                fetchGpsLocation()
            }
        } else {
            gpsNotification = ps.gpsPermissionRequiredForCurrent
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationOverlayRef?.disableMyLocation()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlotifyColors.CanvasBase)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SlotifyColors.SurfaceCard,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = ps.back,
                                    tint = SlotifyColors.TextPrimary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = ps.headerHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // GPS Sync Button in Header
                            FilledTonalButton(
                                onClick = {
                                    if (!GpsLocationHelper.hasLocationPermission(context)) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    } else {
                                        coroutineScope.launch { fetchGpsLocation() }
                                    }
                                },
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (activeGpsResult != null) SlotifyColors.ParentGreen.copy(alpha = 0.18f) else SlotifyColors.KpmSkyBlue.copy(
                                        alpha = 0.15f
                                    )
                                )
                            ) {
                                if (isGpsFetching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = SlotifyColors.KpmSkyBlue
                                    )
                                } else {
                                    Text("📡", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isGpsFetching) ps.gpsLocating else "GPS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeGpsResult != null) SlotifyColors.ParentGreen else SlotifyColors.KpmSkyBlue
                                )
                            }

                            // Manual Coordinate Input Button
                            FilledTonalButton(
                                onClick = {
                                    manualLatInput = String.format(Locale.US, "%.5f", centerLat)
                                    manualLngInput = String.format(Locale.US, "%.5f", centerLng)
                                    manualInputError = null
                                    showManualInput = !showManualInput
                                },
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(ps.manualButton, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Main Interactive Map Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // OpenStreetMap View
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance()
                                .load(ctx, ctx.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
                            Configuration.getInstance().userAgentValue =
                                my.slotify.app.core.routing.OsrmRoutingService.USER_AGENT
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                overlays.add(org.osmdroid.views.overlay.CopyrightOverlay(ctx))
                                controller.setZoom(17.5) // Street-level precision zoom
                                controller.setCenter(GeoPoint(centerLat, centerLng))

                                // Live GPS Blue Dot & Accuracy Circle Overlay (GPS + Network for indoors)
                                if (GpsLocationHelper.hasLocationPermission(ctx)) {
                                    val provider = GpsMyLocationProvider(ctx).apply {
                                        addLocationSource(android.location.LocationManager.NETWORK_PROVIDER)
                                    }
                                    val overlay = MyLocationNewOverlay(provider, this).apply {
                                        enableMyLocation()
                                        setDrawAccuracyEnabled(true)
                                    }
                                    locationOverlayRef = overlay
                                    overlays.add(overlay)
                                }

                                addMapListener(object : MapListener {
                                    override fun onScroll(event: ScrollEvent?): Boolean {
                                        val c = mapCenter
                                        centerLat = c.latitude
                                        centerLng = c.longitude
                                        return false
                                    }

                                    override fun onZoom(event: ZoomEvent?): Boolean {
                                        val c = mapCenter
                                        centerLat = c.latitude
                                        centerLng = c.longitude
                                        return false
                                    }
                                })

                                mapViewRef = this
                            }
                        },
                        update = { /* MapView handles scroll events directly */ },
                        onRelease = { mapView ->
                            locationOverlayRef?.disableMyLocation()
                            locationOverlayRef = null
                            if (mapViewRef === mapView) mapViewRef = null
                            mapView.onDetach()
                        }
                    )

                    // 3. Fixed Center Crosshair & Floating Reticle Pin (Grab / Uber style)
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ground Contact Reticle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.5.dp, SlotifyColors.KpmSkyBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Crosshair Lines
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(1.5.dp)
                                    .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.5f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(44.dp)
                                    .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.5f))
                            )
                            // Precision Center Dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                        }

                        // Floating Pin (Offset so tip touches center dot)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = (-28).dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SlotifyColors.KpmSkyBlue,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎯", fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = ps.preciseTarget,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            // Stem
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(10.dp)
                                    .background(SlotifyColors.KpmSkyBlue)
                            )
                        }
                    }

                    // 4. Live Coordinate & GPS Accuracy Badge (Top-Center of Map)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SlotifyColors.SurfaceCard.copy(alpha = 0.95f),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📍", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = ps.coordinatesBadge(centerLat, centerLng),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.KpmSkyBlue
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "•  ${formatKm(liveDistance)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }

                        if (gpsNotification != null || activeGpsResult != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (activeGpsResult != null) Color(activeGpsResult!!.quality.colorHex).copy(
                                    alpha = 0.15f
                                ) else SlotifyColors.SurfaceCard.copy(alpha = 0.95f),
                                border = if (activeGpsResult != null) BorderStroke(
                                    1.dp,
                                    Color(activeGpsResult!!.quality.colorHex).copy(alpha = 0.4f)
                                ) else null,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = gpsNotification ?: "${activeGpsResult!!.quality.badgeEmoji} GPS: ±${
                                            String.format(
                                                Locale.US,
                                                "%.1f",
                                                activeGpsResult!!.accuracyMeters
                                            )
                                        }m (${activeGpsResult!!.quality.localizedLabel(ps)})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeGpsResult != null) Color(activeGpsResult!!.quality.colorHex) else SlotifyColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 5. Micro-Adjustment Stepper D-Pad (Bottom-Left)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Step selector toggle
                        Surface(
                            shape = SlotifyShapes.Small,
                            color = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                NudgeStepSize.values().forEach { step ->
                                    val isSelected = selectedStep == step
                                    Box(
                                        modifier = Modifier
                                            .clip(SlotifyShapes.Small)
                                            .background(if (isSelected) SlotifyColors.KpmSkyBlue else Color.Transparent)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedStep = step
                                            }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = step.label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else SlotifyColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 4-Way D-Pad Buttons
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Up (North)
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newLat = centerLat + selectedStep.deltaDeg
                                        centerLat = newLat
                                        mapViewRef?.controller?.animateTo(GeoPoint(newLat, centerLng))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = ps.nudgeNorth,
                                        tint = SlotifyColors.KpmSkyBlue
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Left (West)
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newLng = centerLng - selectedStep.deltaDeg
                                            centerLng = newLng
                                            mapViewRef?.controller?.animateTo(GeoPoint(centerLat, newLng))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = ps.nudgeWest,
                                            tint = SlotifyColors.KpmSkyBlue
                                        )
                                    }

                                    // Center Nudge Icon
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "⊕",
                                            fontSize = 14.sp,
                                            color = SlotifyColors.KpmSkyBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Right (East)
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newLng = centerLng + selectedStep.deltaDeg
                                            centerLng = newLng
                                            mapViewRef?.controller?.animateTo(GeoPoint(centerLat, newLng))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = ps.nudgeEast,
                                            tint = SlotifyColors.KpmSkyBlue
                                        )
                                    }
                                }

                                // Down (South)
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newLat = centerLat - selectedStep.deltaDeg
                                        centerLat = newLat
                                        mapViewRef?.controller?.animateTo(GeoPoint(newLat, centerLng))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = ps.nudgeSouth,
                                        tint = SlotifyColors.KpmSkyBlue
                                    )
                                }
                            }
                        }
                    }

                    // 6. Zoom & Recenter Controls (Bottom-Right)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Live Hardware GPS Lock Button
                        SmallFloatingActionButton(
                            onClick = {
                                if (!GpsLocationHelper.hasLocationPermission(context)) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    coroutineScope.launch { fetchGpsLocation() }
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = if (isGpsFetching) SlotifyColors.ActionAmber else SlotifyColors.KpmSkyBlue,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            if (isGpsFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = ps.lockCurrentGps,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Zoom In (+)
                        SmallFloatingActionButton(
                            onClick = { mapViewRef?.controller?.zoomIn() },
                            modifier = Modifier.size(48.dp),
                            containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                            contentColor = SlotifyColors.KpmSkyBlue,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = ps.zoomIn, modifier = Modifier.size(20.dp))
                        }

                        // Zoom Out (-)
                        SmallFloatingActionButton(
                            onClick = { mapViewRef?.controller?.zoomOut() },
                            modifier = Modifier.size(48.dp),
                            containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                            contentColor = SlotifyColors.KpmSkyBlue,
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = ps.zoomOut,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Reset to Home
                        SmallFloatingActionButton(
                            onClick = {
                                centerLat = homeLat
                                centerLng = homeLng
                                mapViewRef?.controller?.apply {
                                    setZoom(16.5)
                                    animateTo(GeoPoint(homeLat, homeLng))
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                            contentColor = SlotifyColors.ParentGreen,
                            shape = CircleShape
                        ) {
                            Text("🏠", fontSize = 16.sp)
                        }
                    }
                }

                // 7. Manual Coordinates Input Drawer (Expandable)
                AnimatedVisibility(visible = showManualInput) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SlotifyColors.SurfaceCard,
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ps.manualInputTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualLatInput,
                                    onValueChange = { manualLatInput = it },
                                    label = { Text(ps.latitudeLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualLngInput,
                                    onValueChange = { manualLngInput = it },
                                    label = { Text(ps.longitudeLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val parsedLat = manualLatInput.toDoubleOrNull()
                                        val parsedLng = manualLngInput.toDoubleOrNull()
                                        if (parsedLat != null && parsedLng != null && parsedLat in -90.0..90.0 && parsedLng in -180.0..180.0) {
                                            centerLat = parsedLat
                                            centerLng = parsedLng
                                            mapViewRef?.controller?.apply {
                                                setZoom(16.5)
                                                animateTo(GeoPoint(parsedLat, parsedLng))
                                            }
                                            showManualInput = false
                                            manualInputError = null
                                        } else {
                                            manualInputError = ps.invalidCoordinates
                                        }
                                    },
                                    shape = SlotifyShapes.Small,
                                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text(ps.goButton, color = Color.White)
                                }
                            }
                            if (manualInputError != null) {
                                Text(
                                    text = manualInputError!!,
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // 8. Quick Landmarks Presets Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SlotifyColors.SurfaceCard
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            text = ps.quickJumpTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(landmarks) { (name, lat, lng) ->
                                val isNear =
                                    kotlin.math.abs(centerLat - lat) < 0.0003 && kotlin.math.abs(centerLng - lng) < 0.0003
                                Box(
                                    modifier = Modifier
                                        .clip(SlotifyShapes.Small)
                                        .background(if (isNear) SlotifyColors.KpmSkyBlue else SlotifyColors.CanvasBase)
                                        .clickable {
                                            centerLat = lat
                                            centerLng = lng
                                            mapViewRef?.controller?.apply {
                                                setZoom(16.5)
                                                animateTo(GeoPoint(lat, lng))
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNear) Color.White else SlotifyColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. Bottom Confirmation Panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SlotifyColors.SurfaceCard,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Resolved Address Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlotifyColors.CanvasBase, SlotifyShapes.Medium)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGeocoding) ps.verifyingAddress else resolvedAddress,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary,
                                    maxLines = 2
                                )
                                Text(
                                    text = ps.coordsWithHomeDistance(centerLat, centerLng, liveDistance),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }

                        // Action Buttons: Batal & Sahkan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = SlotifyShapes.Medium
                            ) {
                                Text(str.cancelButton, color = SlotifyColors.TextSecondary)
                            }

                            Button(
                                // Wait for reverse-geocoding so a placeholder or stale address is never saved.
                                enabled = !isGeocoding,
                                onClick = {
                                    onConfirmLocation(centerLat, centerLng, resolvedAddress, liveDistance)
                                },
                                modifier = Modifier.weight(2f),
                                shape = SlotifyShapes.Medium,
                                colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(ps.confirmLocationButton, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
