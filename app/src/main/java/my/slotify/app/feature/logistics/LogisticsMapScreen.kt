package my.slotify.app.feature.logistics

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import my.slotify.app.core.routing.OsrmRoutingService
import my.slotify.app.core.routing.OsrmRouteResponse
import my.slotify.app.core.notification.SlotifyNotificationManager
import my.slotify.app.core.designsystem.theme.LogisticsStrings
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.core.designsystem.theme.strings
import java.util.UUID
import java.util.Locale
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.slotify.app.core.di.SlotifyViewModelFactory
import org.osmdroid.views.overlay.CopyrightOverlay
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.roundToInt
import my.slotify.app.core.profile.FamilyProfileData
import my.slotify.app.core.profile.RouteProfile
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import my.slotify.app.core.designsystem.component.slotifySection
import androidx.compose.runtime.saveable.rememberSaveable
import my.slotify.app.core.designsystem.component.HeroCard
import my.slotify.app.core.designsystem.component.HeroMetaLine
import my.slotify.app.core.designsystem.component.ScheduleCard
import my.slotify.app.core.designsystem.component.SlotifyScreenTitle
import my.slotify.app.core.designsystem.theme.SlotStatus
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity

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

@Composable
fun poiCategoryLabel(category: PoiCategory): String = when (category) {
    PoiCategory.RUMAH -> strings.poiHome
    PoiCategory.SEKOLAH -> strings.poiSchool
    PoiCategory.TRANSIT -> strings.poiTransit
    PoiCategory.PEJABAT -> strings.poiOffice
    PoiCategory.ALL -> strings.poiAll
}

enum class RouteSelection {
    PARENT_PICKUP,
    VAN_PRIMARY,
    VAN_SECONDARY
}

/** Route waypoints from the user's POIs (by profile id), falling back to profile defaults. */
fun resolveRouteWaypoints(route: RouteSelection, pois: List<PoiItem>): List<PoiItem> =
    FamilyProfileData.routes[route]?.waypointPoiIds.orEmpty().mapNotNull { id ->
        pois.find { it.id == id } ?: FamilyProfileData.defaultPois.find { it.id == id }
    }

/** OSRM "lon,lat;lon,lat" coordinate list. */
fun osrmCoordinateString(points: List<PoiItem>): String =
    points.joinToString(";") { String.format(Locale.US, "%.5f,%.5f", it.lng, it.lat) }

fun launchNavigationIntent(context: Context, lat: Double, lng: Double, label: String) {
    try {
        val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = "https://www.google.com/maps/search/?api=1&query=$lat,$lng".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

/**
 * Real-time animated vehicle state for smooth polyline tracking
 */
data class VehicleSimState(
    val point: GeoPoint,
    val speedKmh: Int,
    val distanceRemainingKm: Double,
    val etaMinutes: Int,
    val statusText: String,
    val vehicleEmoji: String = "🚐"
)

data class DriverContact(
    val name: String,
    val role: String,
    val phone: String,
    val emoji: String,
    val defaultMessage: String
)

fun launchWhatsAppIntent(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "").let {
            if (it.startsWith("0")) "+60" + it.substring(1) else it
        }
        val uri = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            context.startActivity(dialIntent)
        } catch (_: Exception) {
        }
    }
}

fun launchDialerIntent(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

/**
 * Interpolates a polyline into a fixed target count of smooth intermediate coordinates
 */
fun interpolateRoutePoints(rawPoints: List<GeoPoint>, targetCount: Int = 36): List<GeoPoint> {
    if (rawPoints.isEmpty()) return emptyList()
    if (rawPoints.size == 1) return List(targetCount) { rawPoints[0] }
    if (rawPoints.size >= targetCount) {
        val step = (rawPoints.size - 1).toDouble() / (targetCount - 1)
        return (0 until targetCount).map { i ->
            val index = (i * step).toInt().coerceIn(0, rawPoints.lastIndex)
            rawPoints[index]
        }
    }
    val result = mutableListOf<GeoPoint>()
    val segmentCount = rawPoints.size - 1
    val pointsPerSegment = (targetCount - 1) / segmentCount
    val remainder = (targetCount - 1) % segmentCount

    for (i in 0 until segmentCount) {
        val p1 = rawPoints[i]
        val p2 = rawPoints[i + 1]
        val count = pointsPerSegment + if (i < remainder) 1 else 0
        for (j in 0 until count) {
            val fraction = j.toDouble() / count
            val lat = p1.latitude + fraction * (p2.latitude - p1.latitude)
            val lng = p1.longitude + fraction * (p2.longitude - p1.longitude)
            result.add(GeoPoint(lat, lng))
        }
    }
    result.add(rawPoints.last())
    return result
}

/**
 * 1-Tap Driver WhatsApp Quick-Dispatch & Direct Dialer Card
 */
@Composable
fun DriverContactCard(
    selectedRoute: RouteSelection,
    onWhatsAppClick: (phone: String, message: String) -> Unit,
    onCallClick: (phone: String) -> Unit
) {
    val contact = FamilyProfileData.routes[selectedRoute]?.driver ?: return
    val ls = strings.logisticsUi

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SlotifyShapes.Large,
        color = SlotifyColors.SurfaceCard,
        border = BorderStroke(1.dp, SlotifyColors.BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(contact.emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = SlotifyShapes.Small,
                            color = SlotifyColors.SuccessMint.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = ls.driverActiveBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.SuccessMint,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = contact.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlotifyColors.TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // WhatsApp Button
                Button(
                    onClick = { onWhatsAppClick(contact.phone, contact.defaultMessage) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = SlotifyShapes.Small,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("💬 WhatsApp", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Call Button
                OutlinedButton(
                    onClick = { onCallClick(contact.phone) },
                    shape = SlotifyShapes.Small,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = ls.callDriver,
                        tint = SlotifyColors.KpmSkyBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogisticsMapScreen(
    locationGranted: Boolean = false,
    viewModel: LogisticsViewModel = viewModel(factory = SlotifyViewModelFactory)
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    // State-backed so long-running effects and map callbacks always see the current language.
    val ls by rememberUpdatedState(strings.logisticsUi)
    val ps by rememberUpdatedState(strings.locationPickerUi)
    val sys by rememberUpdatedState(strings.systemUi)
    val logisticsState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRoute = logisticsState.selectedRoute
    var selectedPoiCategory by remember { mutableStateOf(PoiCategory.ALL) }
    var simulationStep by remember { mutableIntStateOf(0) }
    var isSimulating by remember { mutableStateOf(false) }

    // Live Vehicle Smooth Polyline Simulation States
    var simulatedVehicleState by remember { mutableStateOf<VehicleSimState?>(null) }
    var vehicleFollowCamera by remember { mutableStateOf(true) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Voice Announcement & Audio Chime Helper
    val voiceHelper = remember(context) { VoiceAnnouncementHelper(context) }
    var isVoiceMuted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.shutdown()
        }
    }

    // POIs and OSRM routing live in LogisticsViewModel (persisted, survive rotation).
    val pois = logisticsState.pois
    val osrmRoute = logisticsState.route
    val isOsrmLoading = logisticsState.isRouteLoading

    // Dialog & Management states
    var editingPoi by remember { mutableStateOf<PoiItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var poiToDelete by remember { mutableStateOf<PoiItem?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Dynamic ground-truth home reference
    val defaultHomeGeo = remember(pois) {
        pois.find { it.category == PoiCategory.RUMAH }?.let { GeoPoint(it.lat, it.lng) }
            ?: GeoPoint(FamilyProfileData.mapCenter.lat, FamilyProfileData.mapCenter.lng)
    }

    // Realtime Smooth Vehicle Simulation Coroutine
    LaunchedEffect(isSimulating, selectedRoute, osrmRoute) {
        if (!isSimulating) {
            simulatedVehicleState = null
            return@LaunchedEffect
        }

        val routeProfile = FamilyProfileData.routes[selectedRoute]
        val vehicleEmoji = routeProfile?.vehicleEmoji ?: "🚐"
        val vanName = routeProfile?.vehicleLabel ?: "Kenderaan"

        val rawPoints = if (osrmRoute != null && osrmRoute!!.geometryPoints.isNotEmpty()) {
            osrmRoute!!.geometryPoints.map { GeoPoint(it.first, it.second) }
        } else {
            resolveRouteWaypoints(selectedRoute, pois).map { GeoPoint(it.lat, it.lng) }
        }

        val animPoints = interpolateRoutePoints(rawPoints, targetCount = 36)
        if (animPoints.isEmpty()) {
            isSimulating = false
            return@LaunchedEffect
        }

        // Remaining distance along the route (not straight-line to home), so a loop route that
        // starts at home does not fire every proximity alert immediately.
        val remainingKm = DoubleArray(animPoints.size)
        for (i in animPoints.lastIndex - 1 downTo 0) {
            val a = animPoints[i]
            val b = animPoints[i + 1]
            remainingKm[i] = remainingKm[i + 1] + calculateDistanceKm(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        val startKm = remainingKm.firstOrNull() ?: 0.0
        // Alerts only fire for thresholds the route actually crosses.
        var alerted3km = startKm <= 3.0
        var alerted1km = startKm <= 1.0
        var alerted300m = startKm <= 0.35
        var alertedArrived = false
        val simTag = ls.simNotificationTag

        for (idx in animPoints.indices) {
            if (!isSimulating) break
            val pt = animPoints[idx]
            val distToHomeKm = remainingKm[idx]
            val speed = 38 + (idx % 5) * 4
            val etaMin = maxOf(1, ((distToHomeKm / speed) * 60).roundToInt())
            val progressPercent = ((idx.toDouble() / (animPoints.size - 1).coerceAtLeast(1)) * 100).toInt()

            val status = when {
                idx == animPoints.lastIndex -> ls.simStatusArrived
                distToHomeKm <= 0.35 -> ls.simStatusApproaching
                distToHomeKm <= 1.0 -> ls.simStatusWithin1km
                distToHomeKm <= 3.0 -> ls.simStatusWithin3km
                else -> ls.simStatusEnRoute(progressPercent)
            }

            // Proximity alerts with haptic, Malay voice TTS, & a notification clearly marked as simulation
            if (distToHomeKm <= 3.0 && !alerted3km) {
                alerted3km = true
                simulationStep = 1
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SlotifyNotificationManager.showTransitAlert(
                    context = context,
                    s = sys,
                    childName = "$simTag $vanName",
                    transitMsg = ls.simAlert3km
                )
                if (!isVoiceMuted) {
                    voiceHelper.speak("Simulasi: $vanName kini berada tiga kilometer dari destinasi.")
                }
            }
            if (distToHomeKm <= 1.0 && !alerted1km) {
                alerted1km = true
                simulationStep = 2
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SlotifyNotificationManager.showTransitAlert(
                    context = context,
                    s = sys,
                    childName = "$simTag $vanName",
                    transitMsg = ls.simAlert1km(vanName)
                )
                if (!isVoiceMuted) {
                    voiceHelper.speak("Simulasi: $vanName kini berada satu kilometer dari destinasi.")
                }
            }
            if (distToHomeKm <= 0.35 && !alerted300m) {
                alerted300m = true
                simulationStep = 3
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SlotifyNotificationManager.showTransitAlert(
                    context = context,
                    s = sys,
                    childName = "$simTag $vanName",
                    transitMsg = ls.simAlert300m
                )
                if (!isVoiceMuted) {
                    voiceHelper.speak("Simulasi: $vanName menghampiri destinasi. Baki tiga ratus meter.")
                }
            }
            if (idx == animPoints.lastIndex && !alertedArrived) {
                alertedArrived = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SlotifyNotificationManager.showTransitAlert(
                    context = context,
                    s = sys,
                    childName = "$simTag $vanName",
                    transitMsg = ls.simAlertArrived(vanName)
                )
                if (!isVoiceMuted) {
                    voiceHelper.speak("Simulasi: $vanName telah selamat tiba di destinasi.")
                }
            }

            simulatedVehicleState = VehicleSimState(
                point = pt,
                speedKmh = speed,
                distanceRemainingKm = distToHomeKm,
                etaMinutes = etaMin,
                statusText = status,
                vehicleEmoji = vehicleEmoji
            )

            if (vehicleFollowCamera) {
                mapViewRef?.controller?.apply {
                    setZoom(16.5)
                    animateTo(pt)
                }
            }

            delay(450L)
        }

        delay(3000L)
        isSimulating = false
        simulatedVehicleState = null
        simulationStep = 0
    }

    // RF Range Pulse States (3.0 km)
    var isRfPulseActive by remember { mutableStateOf(false) }
    var rfCenterGeoPoint by remember { mutableStateOf<GeoPoint?>(defaultHomeGeo) }
    var rfCenterLabel by remember { mutableStateOf(ls.rfCenterHome) }

    // Interactive Map Selected POI & Dropped Pin States
    var selectedMapPoi by remember { mutableStateOf<PoiItem?>(null) }
    var droppedMapPin by remember { mutableStateOf<GeoPoint?>(null) }

    // Dedicated Fullscreen Precision Location Picker Modal
    var showPrecisePicker by remember { mutableStateOf(false) }
    var precisePickerTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var precisePickerPoi by remember { mutableStateOf<PoiItem?>(null) }
    var pendingPoiDraft by remember { mutableStateOf<PoiItem?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Real Hardware GPS Location States
    var hasLocationPermission by remember {
        mutableStateOf(
            locationGranted || GpsLocationHelper.hasLocationPermission(
                context
            )
        )
    }
    var isGpsLocating by remember { mutableStateOf(false) }
    var userGpsFix by remember { mutableStateOf<GpsFixResult?>(null) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var compassOverlayRef by remember { mutableStateOf<CompassOverlay?>(null) }

    val filteredPois =
        if (selectedPoiCategory == PoiCategory.ALL) pois else pois.filter { it.category == selectedPoiCategory }

    // Function to fetch high-precision hardware GPS fix and center map
    val locateUserWithGps: suspend () -> Unit = {
        if (!GpsLocationHelper.hasLocationPermission(context)) {
            hasLocationPermission = false
            gpsStatusMessage = ls.gpsPermissionRequired
        } else if (!GpsLocationHelper.isLocationServiceEnabled(context)) {
            gpsStatusMessage = ls.gpsEnableService
            GpsLocationHelper.openLocationSettings(context)
        } else {
            isGpsLocating = true
            gpsStatusMessage = ls.gpsAcquiring
            val fix = GpsLocationHelper.getAccurateLocation(context, timeoutMs = 8000L)
            isGpsLocating = false

            if (fix != null) {
                userGpsFix = fix

                // Real-time Geofence Proximity Check
                val proximityEvents = GeofenceManager.evaluateProximity(context, fix.latitude, fix.longitude, sys)
                val targetEvent =
                    proximityEvents.firstOrNull { it.eventType == GeofenceEventType.ENTERED || it.eventType == GeofenceEventType.APPROACHING }

                if (targetEvent != null) {
                    val statusText =
                        if (targetEvent.eventType == GeofenceEventType.ENTERED) ls.geofenceArrivedAt(targetEvent.target.name) else ls.geofenceApproaching(
                            targetEvent.target.name,
                            targetEvent.distanceMeters.toInt()
                        )
                    gpsStatusMessage = "${targetEvent.target.iconEmoji} $statusText • ±${
                        String.format(
                            Locale.US,
                            "%.1f",
                            fix.accuracyMeters
                        )
                    }m"
                } else {
                    gpsStatusMessage = "${fix.quality.badgeEmoji} GPS: ±${
                        String.format(
                            Locale.US,
                            "%.1f",
                            fix.accuracyMeters
                        )
                    }m (${fix.quality.localizedLabel(ps)})"
                }

                mapViewRef?.controller?.apply {
                    setZoom(16.5)
                    animateTo(GeoPoint(fix.latitude, fix.longitude))
                }
            } else {
                gpsStatusMessage = ls.gpsWeakCenteringHome
                mapViewRef?.controller?.apply {
                    setZoom(16.5)
                    animateTo(defaultHomeGeo)
                }
            }
        }
    }

    // Continuous Realtime Location Tracking
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            GpsLocationHelper.observeLocationUpdates(context, minTimeMs = 2500L, minDistanceM = 3.0f)
                .collect { fix ->
                    userGpsFix = fix
                    val proximityEvents = GeofenceManager.evaluateProximity(context, fix.latitude, fix.longitude, sys)
                    val targetEvent =
                        proximityEvents.firstOrNull { it.eventType == GeofenceEventType.ENTERED || it.eventType == GeofenceEventType.APPROACHING }
                    if (targetEvent != null) {
                        val statusText =
                            if (targetEvent.eventType == GeofenceEventType.ENTERED) ls.geofenceArrivedAt(targetEvent.target.name) else ls.geofenceApproaching(
                                targetEvent.target.name,
                                targetEvent.distanceMeters.toInt()
                            )
                        gpsStatusMessage = "${targetEvent.target.iconEmoji} $statusText • ±${
                            String.format(
                                Locale.US,
                                "%.1f",
                                fix.accuracyMeters
                            )
                        }m"
                    } else {
                        gpsStatusMessage = "${fix.quality.badgeEmoji} GPS: ±${
                            String.format(
                                Locale.US,
                                "%.1f",
                                fix.accuracyMeters
                            )
                        }m (${fix.quality.localizedLabel(ps)})"
                    }
                }
        }
    }

    // Auto-locate user on initial screen display
    LaunchedEffect(Unit) {
        if (hasLocationPermission && GpsLocationHelper.isLocationServiceEnabled(context)) {
            locateUserWithGps()
        }
    }

    // Permission can change in system Settings or via MainActivity's request.
    LaunchedEffect(locationGranted) {
        if (locationGranted) hasLocationPermission = true
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasLocationPermission = GpsLocationHelper.hasLocationPermission(context)
                    mapViewRef?.onResume()
                }

                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
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
                locateUserWithGps()
            }
        } else {
            gpsStatusMessage = ls.gpsPermissionRequiredForLocation
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationOverlayRef?.disableMyLocation()
            compassOverlayRef?.disableCompass()
        }
    }

    // Map tap handler for dropping a pin or selecting a node
    val handleMapTap: (GeoPoint) -> Unit = { geo ->
        selectedMapPoi = null
        droppedMapPin = geo
    }

    val handlePoiSelected: (PoiItem) -> Unit = { poi ->
        droppedMapPin = null
        selectedMapPoi = poi
    }

    var routesOpen by rememberSaveable { mutableStateOf(true) }
    var directionsOpen by rememberSaveable { mutableStateOf(false) }
    var placesOpen by rememberSaveable { mutableStateOf(false) }

    // The map sits outside the LazyColumn so it is never recycled while scrolling
    // (recycling destroyed the MapView and orphaned its GPS/compass overlays).
    val sch = strings.scheduleUi
    val greeting = when (java.time.LocalTime.now().hour) {
        in 0..11 -> sch.greetingMorning
        in 12..18 -> sch.greetingAfternoon
        else -> sch.greetingEvening
    }
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val mapHeight = if (windowHeight < 720.dp) 190.dp else 260.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        SlotifyScreenTitle(
            title = greeting,
            subtitle = FamilyProfileData.routes[selectedRoute]?.tabTitle ?: strings.logisticsMap
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                .clip(SlotifyShapes.Large)
        ) {
            val overlayKeyHolder = remember { MapOverlayKeyHolder() }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance()
                        .load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    Configuration.getInstance().userAgentValue = OsrmRoutingService.USER_AGENT
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // Keep map gestures from being intercepted by parent scroll containers
                        setOnTouchListener { v, event ->
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            if (event.action == android.view.MotionEvent.ACTION_UP) v.performClick()
                            false
                        }

                        // Initial zoom and centre on home
                        controller.setZoom(13.5)
                        controller.setCenter(defaultHomeGeo)

                        // Initial overlay setup
                        setupMapOverlays(
                            context = ctx,
                            mapView = this,
                            pois = filteredPois,
                            rfCenter = rfCenterGeoPoint,
                            isRfActive = isRfPulseActive,
                            pickedPoint = droppedMapPin,
                            selectedPoiId = selectedMapPoi?.id,
                            selectedRoute = selectedRoute,
                            osrmRoute = osrmRoute,
                            simulatedVehicle = simulatedVehicleState,
                            ls = ls,
                            onMapTapped = handleMapTap,
                            onPoiSelected = handlePoiSelected
                        )

                        // Live Hardware GPS location overlay with real-time accuracy circle (GPS + Network for indoors)
                        if (hasLocationPermission && locationOverlayRef == null) {
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

                        // Live Magnetic Compass Overlay
                        if (compassOverlayRef == null) {
                            val compass = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this).apply {
                                enableCompass()
                                setCompassCenter(28f, 28f)
                            }
                            compassOverlayRef = compass
                            overlays.add(compass)
                        }

                        mapViewRef = this
                    }
                },
                onRelease = { mapView ->
                    // Stop sensors/GPS and drop overlay refs when the map leaves composition
                    // so a re-created MapView gets fresh overlays.
                    locationOverlayRef?.disableMyLocation()
                    compassOverlayRef?.disableCompass()
                    locationOverlayRef = null
                    compassOverlayRef = null
                    if (mapViewRef === mapView) mapViewRef = null
                    mapView.onDetach()
                },
                update = { mapView ->
                    if (hasLocationPermission && locationOverlayRef == null) {
                        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                            enableMyLocation()
                            setDrawAccuracyEnabled(true)
                        }
                        locationOverlayRef = overlay
                        mapView.overlays.add(overlay)
                    }

                    if (compassOverlayRef == null) {
                        val compass =
                            CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
                                enableCompass()
                                setCompassCenter(28f, 28f)
                            }
                        compassOverlayRef = compass
                        mapView.overlays.add(compass)
                    }

                    // Rebuild overlays only when their inputs change; during a simulation
                    // tick only the vehicle marker is moved.
                    val key = MapOverlayKey(
                        filteredPois, rfCenterGeoPoint, isRfPulseActive, droppedMapPin,
                        selectedMapPoi?.id, selectedRoute, osrmRoute, ls
                    )
                    if (key != overlayKeyHolder.lastKey) {
                        overlayKeyHolder.lastKey = key
                        overlayKeyHolder.lastVehicle = simulatedVehicleState
                        setupMapOverlays(
                            context = context,
                            mapView = mapView,
                            pois = filteredPois,
                            rfCenter = rfCenterGeoPoint,
                            isRfActive = isRfPulseActive,
                            pickedPoint = droppedMapPin,
                            selectedPoiId = selectedMapPoi?.id,
                            selectedRoute = selectedRoute,
                            osrmRoute = osrmRoute,
                            simulatedVehicle = simulatedVehicleState,
                            ls = ls,
                            onMapTapped = handleMapTap,
                            onPoiSelected = handlePoiSelected
                        )
                    } else if (simulatedVehicleState != overlayKeyHolder.lastVehicle) {
                        overlayKeyHolder.lastVehicle = simulatedVehicleState
                        updateVehicleMarker(context, mapView, simulatedVehicleState, selectedRoute, ls)
                    }
                }
            )

            // GPS Status / Accuracy Notification Pill
            if (gpsStatusMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = SlotifyColors.SurfaceCard.copy(alpha = 0.96f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = gpsStatusMessage!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { gpsStatusMessage = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = SlotifyColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Floating Action Buttons on Map (Top-Right)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 0. Quick-Snap "Saya di Rumah" Button
                SmallFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val homePoi = pois.find { it.category == PoiCategory.RUMAH } ?: PoiItem(
                            "poi-home",
                            ls.poiHomeFallbackName,
                            PoiCategory.RUMAH,
                            "🏠",
                            FamilyProfileData.mapCenter.lat,
                            FamilyProfileData.mapCenter.lng,
                            0.0
                        )
                        val homePoint = GeoPoint(homePoi.lat, homePoi.lng)
                        mapViewRef?.controller?.apply {
                            setZoom(17.5)
                            animateTo(homePoint)
                        }
                        rfCenterGeoPoint = homePoint
                        rfCenterLabel = homePoi.name
                        gpsStatusMessage = ls.centeredOnHome(homePoi.name)
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                    contentColor = SlotifyColors.ParentGreen,
                    shape = CircleShape
                ) {
                    Text("🏠", fontSize = 16.sp)
                }

                // 1. Hardware GPS Recenter / Lock Button
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            locateUserWithGps()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (isGpsLocating) SlotifyColors.ActionAmber else SlotifyColors.SurfaceCard.copy(
                        alpha = 0.94f
                    ),
                    contentColor = if (isGpsLocating) Color.White else SlotifyColors.KpmSkyBlue,
                    shape = CircleShape
                ) {
                    if (isGpsLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = ls.lockGpsLocation,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. High Precision Location Picker Button
                SmallFloatingActionButton(
                    onClick = {
                        precisePickerTarget =
                            userGpsFix?.let { GeoPoint(it.latitude, it.longitude) } ?: defaultHomeGeo
                        precisePickerPoi = null
                        showPrecisePicker = true
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                    contentColor = SlotifyColors.ActionAmber,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = ls.pickPreciseLocation,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. RF Range 3.0 km Radar Pulse Toggle
                SmallFloatingActionButton(
                    onClick = {
                        isRfPulseActive = !isRfPulseActive
                        if (isRfPulseActive && rfCenterGeoPoint == null) {
                            rfCenterGeoPoint =
                                userGpsFix?.let { GeoPoint(it.latitude, it.longitude) } ?: defaultHomeGeo
                            rfCenterLabel = if (userGpsFix != null) ls.rfCenterCurrentGps else ls.rfCenterHome
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (isRfPulseActive) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard.copy(
                        alpha = 0.94f
                    ),
                    contentColor = if (isRfPulseActive) Color.White else SlotifyColors.KpmSkyBlue,
                    shape = CircleShape
                ) {
                    Text("📡", fontSize = 16.sp)
                }

                // 4. Compass / North Heading Reset
                SmallFloatingActionButton(
                    onClick = {
                        mapViewRef?.let { mv ->
                            if (mv.mapOrientation != 0f) {
                                mv.mapOrientation = 0f
                                gpsStatusMessage = ls.mapAlignedNorth
                            } else {
                                compassOverlayRef?.let { comp ->
                                    if (!comp.isCompassEnabled) {
                                        comp.enableCompass()
                                        gpsStatusMessage = ls.compassEnabled
                                    } else {
                                        gpsStatusMessage = ls.northLocked
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                    contentColor = SlotifyColors.KpmSkyBlue,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = ls.compassOrientation,
                        modifier = Modifier.size(18.dp)
                    )
                }

            }

            // 1. Selected POI Preview Bottom Card
            selectedMapPoi?.let { selPoi ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = SlotifyShapes.Medium,
                    color = SlotifyColors.SurfaceCard.copy(alpha = 0.97f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                val pColor = poiCategoryColor(selPoi.category)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(pColor.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selPoi.iconEmoji, fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selPoi.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = SlotifyShapes.Small,
                                            color = pColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = poiCategoryLabel(selPoi.category),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = pColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (selPoi.address.isNotBlank()) {
                                        Text(
                                            text = selPoi.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = ls.coordsFromHome(selPoi.lat, selPoi.lng, selPoi.distanceKm),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.KpmSkyBlue
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedMapPoi = null },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = strings.close,
                                    tint = SlotifyColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    launchNavigationIntent(context, selPoi.lat, selPoi.lng, selPoi.name)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    ls.directionsButton,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    rfCenterGeoPoint = GeoPoint(selPoi.lat, selPoi.lng)
                                    rfCenterLabel = "${selPoi.iconEmoji} ${selPoi.name}"
                                    isRfPulseActive = true
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    ls.setRfCenterButton,
                                    fontSize = 11.sp,
                                    color = SlotifyColors.KpmSkyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    precisePickerTarget = GeoPoint(selPoi.lat, selPoi.lng)
                                    precisePickerPoi = selPoi
                                    showPrecisePicker = true
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    ls.adjustButton,
                                    fontSize = 11.sp,
                                    color = SlotifyColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingPoi = selPoi
                                    showEditDialog = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = ls.editButton,
                                    tint = SlotifyColors.KpmSkyBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Dropped Map Pin Floating Card (on empty map tap)
            if (selectedMapPoi == null && droppedMapPin != null) {
                val dropPt = droppedMapPin!!
                val home = pois.find { it.category == PoiCategory.RUMAH }
                val dist = if (home != null) calculateDistanceKm(
                    home.lat,
                    home.lng,
                    dropPt.latitude,
                    dropPt.longitude
                ) else 0.0

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = SlotifyShapes.Medium,
                    color = SlotifyColors.SurfaceCard.copy(alpha = 0.97f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = ls.droppedPinTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.TextPrimary
                                    )
                                    Text(
                                        text = ls.coordsFromHome(dropPt.latitude, dropPt.longitude, dist),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.KpmSkyBlue
                                    )
                                }
                            }

                            IconButton(
                                onClick = { droppedMapPin = null },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = strings.close,
                                    tint = SlotifyColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val addr = LocationAddressHelper.resolveAddress(
                                            context,
                                            dropPt.latitude,
                                            dropPt.longitude,
                                            sys
                                        )
                                        editingPoi = PoiItem(
                                            id = "poi-${UUID.randomUUID().toString().take(8)}",
                                            name = "",
                                            category = PoiCategory.SEKOLAH,
                                            iconEmoji = "📍",
                                            lat = dropPt.latitude,
                                            lng = dropPt.longitude,
                                            distanceKm = dist,
                                            address = addr
                                        )
                                        showEditDialog = true
                                        droppedMapPin = null
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmSkyBlue),
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddLocationAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    ls.savePoiButton,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    precisePickerTarget = dropPt
                                    precisePickerPoi = null
                                    showPrecisePicker = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    ls.adjustButton,
                                    fontSize = 11.sp,
                                    color = SlotifyColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    rfCenterGeoPoint = dropPt
                                    rfCenterLabel = ls.rfCenterMapPoint(
                                        String.format(
                                            Locale.US,
                                            "%.4f, %.4f",
                                            dropPt.latitude,
                                            dropPt.longitude
                                        )
                                    )
                                    isRfPulseActive = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = SlotifyShapes.Small,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "📡 RF",
                                    fontSize = 11.sp,
                                    color = SlotifyColors.KpmSkyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Active Vehicle Simulation Floating HUD (Bottom-Center)
            androidx.compose.animation.AnimatedVisibility(
                visible = simulatedVehicleState != null && selectedMapPoi == null && droppedMapPin == null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                simulatedVehicleState?.let { sim ->
                    Surface(
                        shape = SlotifyShapes.Medium,
                        color = SlotifyColors.SurfaceCard.copy(alpha = 0.97f),
                        border = BorderStroke(1.dp, SlotifyColors.ActionAmber.copy(alpha = 0.6f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(SlotifyColors.ActionAmber.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(sim.vehicleEmoji, fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sim.statusText,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = SlotifyShapes.Small,
                                            color = SlotifyColors.ActionAmber.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = ls.speedKmh(sim.speedKmh),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SlotifyColors.ActionAmber,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = ls.simRemainingEta(sim.distanceRemainingKm, sim.etaMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.SuccessMint,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isVoiceMuted = !isVoiceMuted
                                        if (isVoiceMuted) voiceHelper.stop()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isVoiceMuted) ls.unmuteVoice else ls.muteVoice,
                                        tint = if (isVoiceMuted) SlotifyColors.TextSecondary else SlotifyColors.SuccessMint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        vehicleFollowCamera = !vehicleFollowCamera
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        if (vehicleFollowCamera) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                        contentDescription = ls.followCamera,
                                        tint = if (vehicleFollowCamera) SlotifyColors.KpmSkyBlue else SlotifyColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        voiceHelper.stop()
                                        isSimulating = false
                                        simulatedVehicleState = null
                                        simulationStep = 0
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.StopCircle,
                                        contentDescription = ls.stopSimulation,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Permission banner if location is not granted
            if (!hasLocationPermission) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlotifyColors.ActionAmber.copy(alpha = 0.12f), SlotifyShapes.Medium)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ls.gpsPermissionBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ActionAmber),
                            shape = SlotifyShapes.Small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(ls.grantButton, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                                            text = ls.rfCoverageTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary
                                        )
                                        Text(
                                            text = ls.rfCoverageSubtitle,
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
                                        text = ls.rfPulseBadge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SlotifyColors.KpmSkyBlue
                                    )
                                }
                            }

                            // Transmitter Center Switcher
                            Text(
                                text = ls.rfTransmitterCenter(rfCenterLabel),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.KpmSkyBlue
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val quickBases = listOfNotNull(
                                    Triple(ls.rfBaseHome, defaultHomeGeo, ls.rfCenterHome),
                                    pois.firstOrNull { it.category == PoiCategory.PEJABAT }
                                        ?.let { Triple(ls.rfBaseOffice, GeoPoint(it.lat, it.lng), it.name) },
                                    pois.firstOrNull { it.category == PoiCategory.SEKOLAH }
                                        ?.let {
                                            Triple(
                                                "${it.iconEmoji} ${ls.rfBaseSchool}",
                                                GeoPoint(it.lat, it.lng),
                                                it.name
                                            )
                                        },
                                    userGpsFix?.let { fix ->
                                        Triple(
                                            "📡 GPS",
                                            GeoPoint(fix.latitude, fix.longitude),
                                            ls.rfGpsLocation(fix.quality.localizedLabel(ps))
                                        )
                                    },
                                    Triple(ls.rfBasePickOnMap, null, ls.rfTapAnywhere)
                                )
                                items(quickBases) { (btnLabel, geo, name) ->
                                    val isSelected = if (geo == null) {
                                        showPrecisePicker
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
                                                    precisePickerTarget = rfCenterGeoPoint ?: defaultHomeGeo
                                                    precisePickerPoi = null
                                                    showPrecisePicker = true
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
                                    text = ls.rfAnalysisTitle,
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
                                                            if (dist <= 1.5) ls.rfSignalStrong
                                                            else ls.rfSignalModerate
                                                        } else {
                                                            ls.rfOutOfRange
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (inRange) SlotifyColors.SuccessMint else Color(
                                                            0xFFEF4444
                                                        )
                                                    )
                                                }
                                            }

                                            Text(
                                                text = formatKm(dist),
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

            // Trip hero: estimated arrival for the selected route
            item {
                val route = FamilyProfileData.routes[selectedRoute]
                val destination = resolveRouteWaypoints(selectedRoute, pois).lastOrNull()
                val arrival = osrmRoute?.let {
                    java.time.LocalTime.now().plusMinutes(it.totalDurationMinutes.toLong())
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                }
                HeroCard(
                    label = ls.tripHeroLabel,
                    title = when {
                        isOsrmLoading -> ls.routeCalculating
                        arrival != null -> arrival
                        else -> ls.routeOffline
                    },
                    chip = if (osrmRoute != null) ls.routeLive else if (!isOsrmLoading) sch.statusOffline else null,
                    chipStatus = if (osrmRoute != null) SlotStatus.ON_TIME else SlotStatus.OFFLINE
                ) {
                    osrmRoute?.let {
                        HeroMetaLine(Icons.Outlined.Schedule, ls.routeEta(it.totalDurationMinutes.toInt(), it.totalDistanceKm))
                    }
                    route?.let { HeroMetaLine(Icons.Default.DirectionsBus, "${it.vehicleEmoji} ${it.vehicleLabel} • ${it.passengerLabel}") }
                    destination?.let { HeroMetaLine(Icons.Default.Place, it.name) }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                destination?.let { launchNavigationIntent(context, it.lat, it.lng, it.name) }
                            },
                            enabled = destination != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SlotifyColors.HeroContainer),
                            shape = SlotifyShapes.Medium,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ls.navigateButton, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSimulating = !isSimulating
                                if (!isSimulating) {
                                    voiceHelper.stop()
                                    simulatedVehicleState = null
                                    simulationStep = 0
                                }
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = SlotifyShapes.Medium,
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text(if (isSimulating) ls.stopSimShort else ls.startSimShort, style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(onClick = { viewModel.refreshRoute() }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = ls.recalculateRoute, tint = Color.White)
                        }
                    }
                }
            }

            slotifySection(
                key = "routes",
                title = ls.groupRoutes,
                expanded = routesOpen,
                onToggle = { routesOpen = !routesOpen }
            ) {
                // Route options (labels from FamilyProfile)
                items(RouteSelection.entries.filter { FamilyProfileData.routes[it] != null }, key = { "route-" + it.name }) { option ->
                    val profile = FamilyProfileData.routes.getValue(option)
                    val selected = option == selectedRoute
                    ScheduleCard(
                        title = profile.tabTitle,
                        time = when {
                            selected && isOsrmLoading -> ls.routeCalculating
                            selected && osrmRoute != null -> ls.routeEta(osrmRoute!!.totalDurationMinutes.toInt(), osrmRoute!!.totalDistanceKm)
                            selected -> ls.routeOffline
                            else -> profile.passengerLabel
                        },
                        status = if (selected) SlotStatus.NOW else SlotStatus.UPCOMING,
                        subtitle = "${profile.vehicleEmoji} ${profile.vehicleLabel}",
                        subtitleIcon = Icons.Default.DirectionsBus,
                        chip = if (selected) sch.statusNow else null,
                        accent = when (option) {
                            RouteSelection.PARENT_PICKUP -> SlotifyColors.ParentGreen
                            RouteSelection.VAN_PRIMARY -> SlotifyColors.VanPrimaryBlue
                            RouteSelection.VAN_SECONDARY -> SlotifyColors.VanSecondaryGold
                        }.takeUnless { selected },
                        onClick = { viewModel.selectRoute(option) }
                    )
                }

                // Driver WhatsApp & Direct Call Dispatch Card
                item {
                    DriverContactCard(
                        selectedRoute = selectedRoute,
                        onWhatsAppClick = { phone, msg ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            launchWhatsAppIntent(context, phone, msg)
                        },
                        onCallClick = { phone ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            launchDialerIntent(context, phone)
                        }
                    )
                }
            }

            slotifySection(
                key = "directions",
                title = ls.groupDirections,
                expanded = directionsOpen,
                onToggle = { directionsOpen = !directionsOpen }
            ) {
                // Turn-by-Turn Directions Steps for Selected Route
                item {
                    val routeLegs = osrmRoute?.legs.orEmpty()
                    val steps = FamilyProfileData.routes[selectedRoute]?.steps.orEmpty().mapIndexed { i, step ->
                        val leg = routeLegs.getOrNull(i)
                        val detail = if (leg != null) {
                            "${formatKm(leg.distanceMeters / 1000.0)} • ${
                                (leg.durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                            } min"
                        } else "—"
                        Triple("${i + 1}. ${step.title}", detail, step.note)
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
                                                    if (isCurrentSim) SlotifyColors.ActionAmber else SlotifyColors.KpmSkyBlue.copy(
                                                        alpha = 0.15f
                                                    ),
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
                                        modifier = Modifier.padding(end = 64.dp), 
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isCurrentSim) SlotifyColors.ActionAmber else SlotifyColors.SuccessMint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            slotifySection(
                key = "places",
                title = ls.groupPlaces,
                expanded = placesOpen,
                onToggle = { placesOpen = !placesOpen },
                summary = ls.placesSummary(pois.size)
            ) {
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
                                    text = ls.poiListTitle(filteredPois.size),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = ls.poiListSubtitle,
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
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.RestartAlt,
                                        contentDescription = ls.resetDefaults,
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
                                    Text(ls.addButton, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        filteredPois.forEach { poi ->
                            val isSelected = selectedMapPoi?.id == poi.id
                            val poiColor = poiCategoryColor(poi.category)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphic(
                                        elevation = if (isSelected) NeumorphicElevation.MEDIUM else NeumorphicElevation.LOW,
                                        cornerRadius = 14.dp
                                    )
                                    .background(
                                        if (isSelected) SlotifyColors.KpmSkyBlue.copy(alpha = 0.08f) else SlotifyColors.SurfaceCard,
                                        SlotifyShapes.Medium
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left clickable area to focus map & open bottom preview
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(SlotifyShapes.Small)
                                            .clickable {
                                                selectedMapPoi = poi
                                                droppedMapPin = null
                                                mapViewRef?.let { mv ->
                                                    mv.controller.animateTo(GeoPoint(poi.lat, poi.lng))
                                                    mv.controller.setZoom(15.5)
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(poiColor.copy(alpha = 0.16f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(poi.iconEmoji, fontSize = 20.sp)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = poi.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SlotifyColors.TextPrimary
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = SlotifyShapes.Small,
                                                    color = poiColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = poiCategoryLabel(poi.category),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = poiColor,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
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
                                                text = ls.poiCoordsWithDistance(
                                                    poi.lat,
                                                    poi.lng,
                                                    if (poi.distanceKm > 0) "${poi.distanceKm} km" else ls.distanceCenter
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlotifyColors.KpmSkyBlue
                                            )
                                        }
                                    }

                                    // Right action buttons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 1. Navigation Button
                                        IconButton(
                                            onClick = {
                                                launchNavigationIntent(context, poi.lat, poi.lng, poi.name)
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.12f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Navigation,
                                                contentDescription = ls.navigate,
                                                tint = SlotifyColors.KpmSkyBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 2. Laras Tepat Button
                                        IconButton(
                                            onClick = {
                                                precisePickerTarget = GeoPoint(poi.lat, poi.lng)
                                                precisePickerPoi = poi
                                                showPrecisePicker = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(SlotifyColors.ActionAmber.copy(alpha = 0.12f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.PinDrop,
                                                contentDescription = ls.adjustPrecise,
                                                tint = SlotifyColors.ActionAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 3. Edit POI button
                                        IconButton(
                                            onClick = {
                                                editingPoi = poi
                                                showEditDialog = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.12f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = ls.editPoi,
                                                tint = SlotifyColors.KpmSkyBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 4. Delete POI button
                                        IconButton(
                                            onClick = {
                                                poiToDelete = poi
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFFEF4444).copy(alpha = 0.12f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = ls.deletePoi,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Add / Edit POI Dialog
    if (showEditDialog) {
        PoiEditDialog(
            poi = editingPoi,
            userGpsFix = userGpsFix,
            onDismiss = {
                showEditDialog = false
                editingPoi = null
                pendingPoiDraft = null
            },
            onPickFromMap = { draft ->
                pendingPoiDraft = draft
                showEditDialog = false
                precisePickerTarget = GeoPoint(draft.lat, draft.lng)
                precisePickerPoi = null
                showPrecisePicker = true
            },
            onSave = { savedPoi ->
                val updated = if (editingPoi != null && pois.any { it.id == savedPoi.id }) {
                    pois.map { if (it.id == savedPoi.id) savedPoi else it }
                } else {
                    pois + savedPoi
                }
                viewModel.setPois(updated)
                showEditDialog = false
                editingPoi = null
                pendingPoiDraft = null
            }
        )
    }

    // Dedicated Fullscreen Precision Location Picker Modal
    if (showPrecisePicker) {
        val home = pois.find { it.category == PoiCategory.RUMAH }
        PreciseLocationPickerModal(
            initialLat = precisePickerTarget?.latitude ?: (editingPoi?.lat ?: (home?.lat
                ?: FamilyProfileData.mapCenter.lat)),
            initialLng = precisePickerTarget?.longitude ?: (editingPoi?.lng ?: (home?.lng
                ?: FamilyProfileData.mapCenter.lng)),
            title = if (precisePickerPoi != null) ls.adjustLocationTitle(precisePickerPoi!!.name) else ls.pickPreciseLocationTitle,
            homeLat = home?.lat ?: FamilyProfileData.mapCenter.lat,
            homeLng = home?.lng ?: FamilyProfileData.mapCenter.lng,
            onDismiss = {
                showPrecisePicker = false
                if (pendingPoiDraft != null) {
                    editingPoi = pendingPoiDraft
                    showEditDialog = true
                    pendingPoiDraft = null
                }
                precisePickerPoi = null
            },
            onConfirmLocation = { confirmedLat, confirmedLng, confirmedAddress, confirmedDist ->
                if (pendingPoiDraft != null) {
                    editingPoi = pendingPoiDraft!!.copy(
                        lat = confirmedLat,
                        lng = confirmedLng,
                        distanceKm = confirmedDist,
                        address = if (pendingPoiDraft!!.address.isBlank()) confirmedAddress else pendingPoiDraft!!.address
                    )
                    showEditDialog = true
                    pendingPoiDraft = null
                } else if (precisePickerPoi != null) {
                    val updatedPoi = precisePickerPoi!!.copy(
                        lat = confirmedLat,
                        lng = confirmedLng,
                        distanceKm = confirmedDist,
                        address = if (precisePickerPoi!!.address.isBlank()) confirmedAddress else precisePickerPoi!!.address
                    )
                    val updatedList = pois.map { if (it.id == updatedPoi.id) updatedPoi else it }
                    viewModel.setPois(updatedList)
                    selectedMapPoi = updatedPoi
                    precisePickerPoi = null
                } else {
                    droppedMapPin = GeoPoint(confirmedLat, confirmedLng)
                }

                rfCenterGeoPoint = GeoPoint(confirmedLat, confirmedLng)
                rfCenterLabel =
                    ls.rfCenterSelectedPoint(String.format(Locale.US, "%.4f, %.4f", confirmedLat, confirmedLng))
                mapViewRef?.controller?.apply {
                    setZoom(15.5)
                    animateTo(GeoPoint(confirmedLat, confirmedLng))
                }
                showPrecisePicker = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (poiToDelete != null) {
        AlertDialog(
            onDismissRequest = { poiToDelete = null },
            title = {
                Text(
                    text = ls.deletePoiTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = ls.deletePoiMessage(poiToDelete?.name.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlotifyColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        poiToDelete?.let { target ->
                            val updated = pois.filter { it.id != target.id }
                            viewModel.setPois(updated)
                        }
                        poiToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(ls.deleteButton, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { poiToDelete = null }) {
                    Text(strings.cancelButton, color = SlotifyColors.TextSecondary)
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
                    text = ls.resetPoisTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = ls.resetPoisMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlotifyColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPois()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ActionAmber)
                ) {
                    Text(ls.resetButton, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(strings.cancelButton, color = SlotifyColors.TextSecondary)
                }
            }
        )
    }
}

/**
 * Distance calculator between two coordinates in kilometers (1 decimal place)
 */
/** Exact great-circle distance in km. Round only for display (see [formatKm]). */
fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double =
    GeofenceManager.calculateDistanceMeters(lat1, lng1, lat2, lng2) / 1000.0

fun formatKm(km: Double): String = String.format(Locale.US, "%.1f km", km)

/**
 * Renders Route polyline, POI markers, RF Pulse 3km overlay, and picked point onto MapView
 */
private const val VEHICLE_MARKER_ID = "simulated_vehicle_marker"

/** Inputs that require a full overlay rebuild (the simulated vehicle is handled separately). */
private data class MapOverlayKey(
    val pois: List<PoiItem>,
    val rfCenter: GeoPoint?,
    val isRfActive: Boolean,
    val pickedPoint: GeoPoint?,
    val selectedPoiId: String?,
    val selectedRoute: RouteSelection,
    val osrmRoute: OsrmRouteResponse?,
    val ls: LogisticsStrings
)

/** Non-observable holder: written from AndroidView.update without triggering recomposition. */
private class MapOverlayKeyHolder {
    var lastKey: MapOverlayKey? = null
    var lastVehicle: VehicleSimState? = null
}

private fun addVehicleMarker(
    context: Context,
    mapView: MapView,
    simulatedVehicle: VehicleSimState,
    selectedRoute: RouteSelection,
    ls: LogisticsStrings
) {
    val vehicleMarker = Marker(mapView).apply {
        id = VEHICLE_MARKER_ID
        position = simulatedVehicle.point
        title = FamilyProfileData.routes[selectedRoute]?.let { "${it.vehicleEmoji} ${it.vehicleLabel}" } ?: "🚐"
        snippet = ls.vehicleMarkerSnippet(
            simulatedVehicle.speedKmh,
            simulatedVehicle.distanceRemainingKm,
            simulatedVehicle.etaMinutes
        )
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = PoiMarkerHelper.getVehicleMarker(context, simulatedVehicle.vehicleEmoji)
    }
    mapView.overlays.add(vehicleMarker)

}

/** Moves (or removes) only the simulated vehicle marker. */
private fun updateVehicleMarker(
    context: Context,
    mapView: MapView,
    vehicle: VehicleSimState?,
    selectedRoute: RouteSelection,
    ls: LogisticsStrings
) {
    mapView.overlays.removeAll { it is Marker && it.id == VEHICLE_MARKER_ID }
    if (vehicle != null) addVehicleMarker(context, mapView, vehicle, selectedRoute, ls)
    mapView.invalidate()
}

private fun setupMapOverlays(
    context: Context,
    mapView: MapView,
    pois: List<PoiItem>,
    rfCenter: GeoPoint?,
    isRfActive: Boolean,
    pickedPoint: GeoPoint?,
    selectedPoiId: String?,
    selectedRoute: RouteSelection,
    osrmRoute: OsrmRouteResponse?,
    simulatedVehicle: VehicleSimState? = null,
    ls: LogisticsStrings,
    onMapTapped: (GeoPoint) -> Unit,
    onPoiSelected: (PoiItem) -> Unit
) {
    // Keep tile overlays, clear custom Markers, Polylines and RfPulseOverlay
    mapView.overlays.removeAll { it is Marker || it is Polyline || it is RfPulseOverlay }

    // Required "© OpenStreetMap contributors" attribution (added once per MapView).
    if (mapView.overlays.none { it is CopyrightOverlay }) {
        mapView.overlays.add(CopyrightOverlay(context))
    }

    // 1. RF Range Pulse 3.0 km Overlay. It also handles map taps, so it is always present,
    //    but it only draws the 3 km rings when the RF toggle is on.
    val rfOverlay = RfPulseOverlay(
        center = if (isRfActive) rfCenter else null,
        isPulseActive = isRfActive,
        maxRadiusMeters = 3000.0,
        centerLabel = ls.rfOverlayCenterLabel,
        onMapTapped = onMapTapped,
        onMapLongPressed = onMapTapped
    )
    mapView.overlays.add(rfOverlay)

    // 2. Route polyline from OSRM GeoJSON geometry or waypoints
    val routePoints = mutableListOf<GeoPoint>()
    if (osrmRoute != null && osrmRoute.geometryPoints.isNotEmpty()) {
        routePoints.addAll(osrmRoute.geometryPoints.map { GeoPoint(it.first, it.second) })
    } else {
        resolveRouteWaypoints(selectedRoute, pois).forEach { routePoints.add(GeoPoint(it.lat, it.lng)) }
    }

    if (routePoints.size >= 2) {
        val polyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = when (selectedRoute) {
                RouteSelection.PARENT_PICKUP -> "#0284C7".toColorInt()
                RouteSelection.VAN_PRIMARY -> "#2563EB".toColorInt()
                RouteSelection.VAN_SECONDARY -> "#F59E0B".toColorInt()
            }
            outlinePaint.strokeWidth = 10f
        }
        mapView.overlays.add(polyline)
    }

    // 3. POI Markers with Custom Teardrop Pin and Embedded Emoji Badge
    pois.forEach { poi ->
        val isSelected = poi.id == selectedPoiId
        val marker = Marker(mapView).apply {
            id = poi.id
            position = GeoPoint(poi.lat, poi.lng)
            title = poi.name
            snippet = if (poi.address.isNotBlank()) {
                "${poi.iconEmoji} ${poi.address} (${if (poi.distanceKm > 0) "${poi.distanceKm} km" else ls.distanceCenter})"
            } else {
                "${poi.iconEmoji} • ${if (poi.distanceKm > 0) "${poi.distanceKm} km" else ls.distanceCenter}"
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = PoiMarkerHelper.getPoiMarker(context, poi, isSelected)
            setOnMarkerClickListener { _, mv ->
                onPoiSelected(poi)
                mv.controller.animateTo(GeoPoint(poi.lat, poi.lng))
                true
            }
        }
        mapView.overlays.add(marker)
    }

    // 4. Temporary Marker for dropped / picked coordinate on map
    if (pickedPoint != null) {
        val pickMarker = Marker(mapView).apply {
            id = "temp_picked_pin"
            position = pickedPoint
            title = ls.pickedPointMarkerTitle
            snippet = String.format(Locale.US, "%.5f, %.5f", pickedPoint.latitude, pickedPoint.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = PoiMarkerHelper.getTargetPin(context, "🎯")
            setOnMarkerClickListener { _, _ ->
                onMapTapped(pickedPoint)
                true
            }
        }
        mapView.overlays.add(pickMarker)
        pickMarker.showInfoWindow()
    }

    // 5. Simulated Animated Vehicle Marker (Van / Car)
    if (simulatedVehicle != null) addVehicleMarker(context, mapView, simulatedVehicle, selectedRoute, ls)

    mapView.invalidate()
}

/**
 * Add / Edit POI Dialog with Emoji selection, Category chips, Map Picker, and Coordinates
 */
@Composable
private fun PoiEditDialog(
    poi: PoiItem?,
    userGpsFix: GpsFixResult? = null,
    onDismiss: () -> Unit,
    onPickFromMap: (PoiItem) -> Unit,
    onSave: (PoiItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ls = strings.logisticsUi
    val sys = strings.systemUi
    val isEdit = poi != null
    var name by remember { mutableStateOf(poi?.name ?: "") }
    var category by remember { mutableStateOf(poi?.category ?: PoiCategory.SEKOLAH) }
    var iconEmoji by remember { mutableStateOf(poi?.iconEmoji ?: "🏢") }
    var address by remember { mutableStateOf(poi?.address ?: "") }
    var latText by remember {
        mutableStateOf(
            if (poi != null) String.format(
                Locale.US,
                "%.5f",
                poi.lat
            ) else String.format(Locale.US, "%.5f", FamilyProfileData.mapCenter.lat)
        )
    }
    var lngText by remember {
        mutableStateOf(
            if (poi != null) String.format(
                Locale.US,
                "%.5f",
                poi.lng
            ) else String.format(Locale.US, "%.5f", FamilyProfileData.mapCenter.lng)
        )
    }
    var distanceText by remember {
        mutableStateOf(
            if (poi != null) String.format(
                Locale.US,
                "%.1f",
                poi.distanceKm
            ) else "5.0"
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetEmojis = listOf("🏢", "🏫", "🎒", "🏠", "🕌", "🧒", "📚", "🏬", "🩺", "🚌", "📍")
    val categories = listOf(
        PoiCategory.SEKOLAH to ls.categorySchool,
        PoiCategory.TRANSIT to ls.categoryTransit,
        PoiCategory.RUMAH to ls.categoryHome,
        PoiCategory.PEJABAT to ls.categoryOffice
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) ls.editPoiTitle else ls.addPoiTitle,
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
                    label = { Text(ls.poiNameLabel) },
                    placeholder = { Text(ls.poiNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Text(
                    text = ls.categoryLabel,
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
                    text = ls.emojiLabel,
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
                    label = { Text(ls.addressLabel) },
                    placeholder = { Text(ls.addressPlaceholder) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Coordinates: Lat & Lng
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text(ls.latitudeLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text(ls.longitudeLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // High Precision Location Picker Button
                OutlinedButton(
                    onClick = {
                        val currentDraft = PoiItem(
                            id = poi?.id ?: "poi-${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            category = category,
                            iconEmoji = iconEmoji,
                            lat = latText.toDoubleOrNull() ?: FamilyProfileData.mapCenter.lat,
                            lng = lngText.toDoubleOrNull() ?: FamilyProfileData.mapCenter.lng,
                            distanceKm = distanceText.toDoubleOrNull() ?: 0.0,
                            address = address.trim()
                        )
                        onPickFromMap(currentDraft)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SlotifyShapes.Medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SlotifyColors.KpmSkyBlue.copy(alpha = 0.08f),
                        contentColor = SlotifyColors.KpmSkyBlue
                    )
                ) {
                    Text(ls.pickOnMapButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Preset Quick Chips (including live GPS if available)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (userGpsFix != null) {
                        FilledTonalButton(
                            onClick = {
                                latText = String.format(Locale.US, "%.5f", userGpsFix.latitude)
                                lngText = String.format(Locale.US, "%.5f", userGpsFix.longitude)
                                coroutineScope.launch {
                                    val resolved = LocationAddressHelper.resolveAddress(
                                        context,
                                        userGpsFix.latitude,
                                        userGpsFix.longitude,
                                        sys
                                    )
                                    if (address.isBlank() && resolved.isNotBlank()) {
                                        address = resolved
                                    }
                                }
                            },
                            shape = SlotifyShapes.Small,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(28.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SlotifyColors.ActionAmber.copy(
                                    alpha = 0.2f
                                )
                            )
                        ) {
                            Text(
                                "📡 GPS (${userGpsFix.quality.localizedLabel(strings.locationPickerUi)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.ActionAmber
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            FamilyProfileData.defaultPois.firstOrNull { it.category == PoiCategory.PEJABAT }?.let {
                                latText = String.format(Locale.US, "%.5f", it.lat)
                                lngText = String.format(Locale.US, "%.5f", it.lng)
                                if (address.isBlank()) address = it.address
                            }
                        },
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Text(ls.presetOffice, fontSize = 10.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            latText = String.format(Locale.US, "%.5f", FamilyProfileData.mapCenter.lat)
                            lngText = String.format(Locale.US, "%.5f", FamilyProfileData.mapCenter.lng)
                        },
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Text(ls.presetHome, fontSize = 10.sp)
                    }
                }

                // Distance Km
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text(ls.distanceLabel) },
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
                        errorMessage = ls.errorNameRequired
                        return@Button
                    }
                    if (latVal == null || lngVal == null) {
                        errorMessage = ls.errorInvalidCoordinates
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
                Text(if (isEdit) strings.saveButton else ls.addPoiButton, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelButton, color = SlotifyColors.TextSecondary)
            }
        }
    )
}
