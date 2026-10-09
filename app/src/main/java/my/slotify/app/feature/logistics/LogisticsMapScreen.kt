package my.slotify.app.feature.logistics

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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

fun launchNavigationIntent(context: Context, lat: Double, lng: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
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

    // Live OSRM Driving Engine Integration
    val osrmService = remember { OsrmRoutingService() }
    var osrmRoute by remember { mutableStateOf<OsrmRouteResponse?>(null) }
    var isOsrmLoading by remember { mutableStateOf(false) }
    var osrmRefreshTrigger by remember { mutableIntStateOf(0) }

    // Fetch OSRM route whenever selectedRoute changes or user triggers refresh
    LaunchedEffect(selectedRoute, pois, osrmRefreshTrigger) {
        isOsrmLoading = true
        val coords = when (selectedRoute) {
            RouteSelection.ABAH_PICKUP -> {
                val office = pois.find { it.category == PoiCategory.PEJABAT } ?: PoiItem("def-pej", "Pejabat", PoiCategory.PEJABAT, "🏢", 3.1390, 101.6869, 0.0)
                val tadika = pois.find { it.id == "poi-5" } ?: pois.find { it.category == PoiCategory.TRANSIT } ?: PoiItem("def-tad", "Tadika", PoiCategory.TRANSIT, "🏫", 3.1390, 101.6869, 0.0)
                val taska = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT && it.id != tadika.id } ?: PoiItem("def-tas", "Taska", PoiCategory.TRANSIT, "👶", 3.1390, 101.6869, 0.0)
                val home = pois.find { it.category == PoiCategory.RUMAH } ?: PoiItem("def-hom", "Rumah", PoiCategory.RUMAH, "🏠", 3.1390, 101.6869, 0.0)
                String.format(Locale.US, "%.4f,%.4f;%.4f,%.4f;%.4f,%.4f;%.4f,%.4f",
                    office.lng, office.lat,
                    tadika.lng, tadika.lat,
                    taska.lng, taska.lat,
                    home.lng, home.lat
                )
            }
            RouteSelection.VAN_PRIMARY -> {
                val home = pois.find { it.category == PoiCategory.RUMAH } ?: PoiItem("def-hom", "Rumah", PoiCategory.RUMAH, "🏠", 3.1390, 101.6869, 0.0)
                val sra = pois.find { it.name.contains("SRA") } ?: PoiItem("def-sra", "SRA", PoiCategory.SEKOLAH, "🕌", 3.1390, 101.6869, 0.0)
                val taska = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT } ?: PoiItem("def-tas", "Taska", PoiCategory.TRANSIT, "👶", 3.1390, 101.6869, 0.0)
                val sk = pois.find { it.name.contains("SK") } ?: PoiItem("def-sko", "SK", PoiCategory.SEKOLAH, "🏫", 3.1390, 101.6869, 0.0)
                String.format(Locale.US, "%.4f,%.4f;%.4f,%.4f;%.4f,%.4f;%.4f,%.4f;%.4f,%.4f",
                    home.lng, home.lat,
                    sra.lng, sra.lat,
                    taska.lng, taska.lat,
                    sk.lng, sk.lat,
                    home.lng, home.lat
                )
            }
            RouteSelection.VAN_SECONDARY -> {
                val home = pois.find { it.category == PoiCategory.RUMAH } ?: PoiItem("def-hom", "Rumah", PoiCategory.RUMAH, "🏠", 3.1390, 101.6869, 0.0)
                val srai = pois.find { it.name.contains("Bukit Contoh") || it.name.contains("SRAI") } ?: PoiItem("def-srai", "SRAI", PoiCategory.SEKOLAH, "🕌", 3.1390, 101.6869, 0.0)
                val taska = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT } ?: PoiItem("def-tas", "Taska", PoiCategory.TRANSIT, "👶", 3.1390, 101.6869, 0.0)
                String.format(Locale.US, "%.4f,%.4f;%.4f,%.4f;%.4f,%.4f;%.4f,%.4f",
                    home.lng, home.lat,
                    srai.lng, srai.lat,
                    taska.lng, taska.lat,
                    home.lng, home.lat
                )
            }
        }
        osrmRoute = osrmService.getDrivingRoute(coords)
        isOsrmLoading = false
    }

    // Dialog & Management states
    var editingPoi by remember { mutableStateOf<PoiItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var poiToDelete by remember { mutableStateOf<PoiItem?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Dynamic ground-truth home reference
    val defaultHomeGeo = remember(pois) {
        pois.find { it.category == PoiCategory.RUMAH }?.let { GeoPoint(it.lat, it.lng) } ?: GeoPoint(3.1390, 101.6869)
    }

    // RF Range Pulse States (3.0 km)
    var isRfPulseActive by remember { mutableStateOf(false) }
    var rfCenterGeoPoint by remember { mutableStateOf<GeoPoint?>(defaultHomeGeo) }
    var rfCenterLabel by remember { mutableStateOf("Rumah Kediaman") }

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
    var hasLocationPermission by remember { mutableStateOf(locationGranted || GpsLocationHelper.hasLocationPermission(context)) }
    var isGpsLocating by remember { mutableStateOf(false) }
    var userGpsFix by remember { mutableStateOf<GpsFixResult?>(null) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var compassOverlayRef by remember { mutableStateOf<CompassOverlay?>(null) }
    var isOfflineTileDownloading by remember { mutableStateOf(false) }
    var isOfflineCached by remember { mutableStateOf(OfflineMapManager.isOfflineCacheReady(context)) }

    // Reference to MapView for programmatic camera animations
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val filteredPois = if (selectedPoiCategory == PoiCategory.ALL) pois else pois.filter { it.category == selectedPoiCategory }

    // Function to fetch high-precision hardware GPS fix and center map
    val locateUserWithGps: suspend () -> Unit = {
        if (!GpsLocationHelper.hasLocationPermission(context)) {
            hasLocationPermission = false
            gpsStatusMessage = "Kebenaran GPS diperlukan"
        } else if (!GpsLocationHelper.isLocationServiceEnabled(context)) {
            gpsStatusMessage = "Sila aktifkan perkhidmatan GPS dalam tetapan peranti"
            GpsLocationHelper.openLocationSettings(context)
        } else {
            isGpsLocating = true
            gpsStatusMessage = "Mendapatkan isyarat satelit GPS..."
            val fix = GpsLocationHelper.getAccurateLocation(context, timeoutMs = 8000L)
            isGpsLocating = false

            if (fix != null) {
                userGpsFix = fix

                // Real-time Geofence Proximity Check
                val proximityEvents = GeofenceManager.evaluateProximity(context, fix.latitude, fix.longitude)
                val targetEvent = proximityEvents.firstOrNull { it.eventType == GeofenceEventType.ENTERED || it.eventType == GeofenceEventType.APPROACHING }

                if (targetEvent != null) {
                    val statusText = if (targetEvent.eventType == GeofenceEventType.ENTERED) "Tiba di ${targetEvent.target.name}" else "Menghampiri ${targetEvent.target.name} (${targetEvent.distanceMeters.toInt()}m)"
                    gpsStatusMessage = "${targetEvent.target.iconEmoji} $statusText • ±${String.format(Locale.US, "%.1f", fix.accuracyMeters)}m"
                } else {
                    gpsStatusMessage = "${fix.quality.badgeEmoji} GPS: ±${String.format(Locale.US, "%.1f", fix.accuracyMeters)}m (${fix.quality.label})"
                }

                mapViewRef?.controller?.apply {
                    setZoom(16.5)
                    animateTo(GeoPoint(fix.latitude, fix.longitude))
                }
            } else {
                gpsStatusMessage = "Isyarat GPS lemah. Memusatkan pada Rumah Kediaman."
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
                    val proximityEvents = GeofenceManager.evaluateProximity(context, fix.latitude, fix.longitude)
                    val targetEvent = proximityEvents.firstOrNull { it.eventType == GeofenceEventType.ENTERED || it.eventType == GeofenceEventType.APPROACHING }
                    if (targetEvent != null) {
                        val statusText = if (targetEvent.eventType == GeofenceEventType.ENTERED) "Tiba di ${targetEvent.target.name}" else "Menghampiri ${targetEvent.target.name} (${targetEvent.distanceMeters.toInt()}m)"
                        gpsStatusMessage = "${targetEvent.target.iconEmoji} $statusText • ±${String.format(Locale.US, "%.1f", fix.accuracyMeters)}m"
                    } else {
                        gpsStatusMessage = "${fix.quality.badgeEmoji} GPS: ±${String.format(Locale.US, "%.1f", fix.accuracyMeters)}m (${fix.quality.label})"
                    }
                }
        }
    }

    // Auto-locate user on initial screen display
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            locateUserWithGps()
        }
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
            gpsStatusMessage = "Kebenaran GPS diperlukan untuk menentukan lokasi semasa"
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Permission banner if location is not granted
        if (!hasLocationPermission) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlotifyColors.ActionAmber.copy(alpha = 0.12f), SlotifyShapes.Medium)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kebenaran GPS diperlukan untuk paparan lokasi tepat pada peta.",
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
                        Text("Beri", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
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
                    .height(330.dp)
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
                            val compass = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
                                enableCompass()
                                setCompassCenter(28f, 28f)
                            }
                            compassOverlayRef = compass
                            mapView.overlays.add(compass)
                        }

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
                            onMapTapped = handleMapTap,
                            onPoiSelected = handlePoiSelected
                        )
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
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Floating Action Buttons on Map (Top-Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 0. Quick-Snap "Saya di Rumah" Button
                    SmallFloatingActionButton(
                        onClick = {
                            val homePoi = pois.find { it.category == PoiCategory.RUMAH } ?: PoiItem("poi-1", "Rumah Kediaman", PoiCategory.RUMAH, "🏠", 3.1390, 101.6869, 0.0)
                            val homePoint = GeoPoint(homePoi.lat, homePoi.lng)
                            mapViewRef?.controller?.apply {
                                setZoom(17.5)
                                animateTo(homePoint)
                            }
                            rfCenterGeoPoint = homePoint
                            rfCenterLabel = homePoi.name
                            gpsStatusMessage = "🏠 Dipusatkan ke ${homePoi.name} (Mod Rumah)"
                        },
                        modifier = Modifier.size(36.dp),
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
                        modifier = Modifier.size(36.dp),
                        containerColor = if (isGpsLocating) SlotifyColors.ActionAmber else SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                        contentColor = if (isGpsLocating) Color.White else SlotifyColors.KpmSkyBlue,
                        shape = CircleShape
                    ) {
                        if (isGpsLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Kunci Lokasi GPS", modifier = Modifier.size(18.dp))
                        }
                    }

                    // 2. High Precision Location Picker Button
                    SmallFloatingActionButton(
                        onClick = {
                            precisePickerTarget = userGpsFix?.let { GeoPoint(it.latitude, it.longitude) } ?: defaultHomeGeo
                            precisePickerPoi = null
                            showPrecisePicker = true
                        },
                        modifier = Modifier.size(36.dp),
                        containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                        contentColor = SlotifyColors.ActionAmber,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.PinDrop, contentDescription = "Pilih Lokasi Tepat", modifier = Modifier.size(18.dp))
                    }

                    // 3. RF Range 3.0 km Radar Pulse Toggle
                    SmallFloatingActionButton(
                        onClick = {
                            isRfPulseActive = !isRfPulseActive
                            if (isRfPulseActive && rfCenterGeoPoint == null) {
                                rfCenterGeoPoint = userGpsFix?.let { GeoPoint(it.latitude, it.longitude) } ?: defaultHomeGeo
                                rfCenterLabel = if (userGpsFix != null) "GPS Semasa" else "Rumah Kediaman"
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        containerColor = if (isRfPulseActive) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
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
                                    gpsStatusMessage = "🧭 Peta Dilaras ke Arah Utara"
                                } else {
                                    compassOverlayRef?.let { comp ->
                                        if (!comp.isCompassEnabled) {
                                            comp.enableCompass()
                                            gpsStatusMessage = "🧭 Kompas Sensor Diaktifkan"
                                        } else {
                                            gpsStatusMessage = "🧭 Arah Utara Dikunci (0°)"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        containerColor = SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                        contentColor = SlotifyColors.KpmSkyBlue,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = "Kompas & Orientasi", modifier = Modifier.size(18.dp))
                    }

                    // 5. Offline Tile Pre-Cache Button (Bandar Contoh Corridor)
                    SmallFloatingActionButton(
                        onClick = {
                            mapViewRef?.let { mv ->
                                if (!isOfflineTileDownloading) {
                                    isOfflineTileDownloading = true
                                    gpsStatusMessage = "⬇️ Memuat turun jubin peta luar talian (Bandar Contoh)..."
                                    OfflineMapManager.downloadFamilyCorridorTiles(
                                        context = context,
                                        mapView = mv,
                                        minZoom = 12,
                                        maxZoom = 16,
                                        onProgress = { cur, tot ->
                                            gpsStatusMessage = "⬇️ Menyimpan peta luar talian ($cur/$tot)..."
                                        },
                                        onComplete = { tot ->
                                            isOfflineTileDownloading = false
                                            isOfflineCached = true
                                            gpsStatusMessage = "✅ Peta Luar Talian Sedia ($tot jubin)!"
                                        },
                                        onError = { err ->
                                            isOfflineTileDownloading = false
                                            gpsStatusMessage = "⚠️ $err"
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        containerColor = if (isOfflineCached) SlotifyColors.ParentGreen.copy(alpha = 0.18f) else SlotifyColors.SurfaceCard.copy(alpha = 0.94f),
                        contentColor = if (isOfflineCached) SlotifyColors.ParentGreen else SlotifyColors.TextSecondary,
                        shape = CircleShape
                    ) {
                        if (isOfflineTileDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SlotifyColors.KpmSkyBlue)
                        } else {
                            Icon(if (isOfflineCached) Icons.Default.CloudDone else Icons.Default.CloudDownload, contentDescription = "Simpan Peta Luar Talian", modifier = Modifier.size(18.dp))
                        }
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
                                                    text = selPoi.category.name,
                                                    fontSize = 8.5.sp,
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
                                            text = String.format(Locale.US, "%.5f° U, %.5f° T • %.1f km dari Rumah", selPoi.lat, selPoi.lng, selPoi.distanceKm),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.KpmSkyBlue
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedMapPoi = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary, modifier = Modifier.size(16.dp))
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
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Arah", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                                    Text("📡 Pusat RF", fontSize = 11.sp, color = SlotifyColors.KpmSkyBlue, fontWeight = FontWeight.Bold)
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
                                    Text("🎯 Laras", fontSize = 11.sp, color = SlotifyColors.TextPrimary, fontWeight = FontWeight.Bold)
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
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SlotifyColors.KpmSkyBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 2. Dropped Map Pin Floating Card (on empty map tap)
                if (selectedMapPoi == null && droppedMapPin != null) {
                    val dropPt = droppedMapPin!!
                    val home = pois.find { it.category == PoiCategory.RUMAH }
                    val dist = if (home != null) calculateDistanceKm(home.lat, home.lng, dropPt.latitude, dropPt.longitude) else 0.0

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
                                            text = "Nod Ditandakan Pada Peta",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SlotifyColors.TextPrimary
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.5f° U, %.5f° T • %.1f km dari Rumah", dropPt.latitude, dropPt.longitude, dist),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotifyColors.KpmSkyBlue
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { droppedMapPin = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val addr = LocationAddressHelper.resolveAddress(context, dropPt.latitude, dropPt.longitude)
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
                                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Simpan POI", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                                    Text("🎯 Laras", fontSize = 11.sp, color = SlotifyColors.TextPrimary, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        rfCenterGeoPoint = dropPt
                                        rfCenterLabel = "Titik Peta (${String.format(Locale.US, "%.4f, %.4f", dropPt.latitude, dropPt.longitude)})"
                                        isRfPulseActive = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = SlotifyShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("📡 RF", fontSize = 11.sp, color = SlotifyColors.KpmSkyBlue, fontWeight = FontWeight.Bold)
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
                            val quickBases = listOfNotNull(
                                Triple("🏠 Rumah", defaultHomeGeo, "Rumah Kediaman"),
                                Triple("🏢 Pejabat", GeoPoint(3.1390, 101.6869), "Pejabat Abah (Bandar Contoh)"),
                                Triple("🏫 SK", GeoPoint(3.1390, 101.6869), "SK Taman Contoh"),
                                userGpsFix?.let { fix ->
                                    Triple("📡 GPS", GeoPoint(fix.latitude, fix.longitude), "Lokasi GPS (${fix.quality.label})")
                                },
                                Triple("🎯 Pilih Peta", null, "Sentuh mana-mana pada peta")
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
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = SlotifyShapes.Small,
                                color = SlotifyColors.SuccessMint.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isOsrmLoading) "OSRM..." else "OSRM Live",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlotifyColors.SuccessMint,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { osrmRefreshTrigger++ },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Kira Semula OSRM",
                                    tint = SlotifyColors.KpmSkyBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Simulation button with Geofence Alerts
                            Button(
                                onClick = {
                                    isSimulating = true
                                    simulationStep = (simulationStep + 1) % 4
                                    if (simulationStep > 0) {
                                        val vanName = when (selectedRoute) {
                                            RouteSelection.ABAH_PICKUP -> "Kereta Abah"
                                            RouteSelection.VAN_PRIMARY -> "Van Sekolah A"
                                            RouteSelection.VAN_SECONDARY -> "Van Sekolah B"
                                        }
                                        val (radLabel, desc) = when (simulationStep) {
                                            1 -> Pair("3,000m", "Geofens 3.0km dikesan • Bersedia!")
                                            2 -> Pair("1,000m", "Geofens 1.0km • Van sedang menghampiri lokasi!")
                                            3 -> Pair("300m", "Ketibaan dalam 300m • Sila ke pintu pagar!")
                                            else -> Pair("", "")
                                        }
                                        if (radLabel.isNotEmpty()) {
                                            SlotifyNotificationManager.showTransitAlert(
                                                context = context,
                                                childName = vanName,
                                                transitMsg = "🚨 Radar $radLabel: $desc"
                                            )
                                        }
                                    }
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
                    }

                    // 3 Route Tabs with dynamic OSRM ETA / KM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val abahSubtitle = if (isOsrmLoading && selectedRoute == RouteSelection.ABAH_PICKUP) "Mengira..."
                            else if (osrmRoute != null && selectedRoute == RouteSelection.ABAH_PICKUP) "ETA ~${osrmRoute!!.totalDurationMinutes.toInt()} min (${String.format(Locale.US, "%.1f", osrmRoute!!.totalDistanceKm)} km)"
                            else "ETA ~18 Minit"
                        RouteTabCard(
                            title = strings.routeAbah,
                            subtitle = abahSubtitle,
                            isSelected = selectedRoute == RouteSelection.ABAH_PICKUP,
                            modifier = Modifier.weight(1f)
                        ) { selectedRoute = RouteSelection.ABAH_PICKUP }

                        val vanPrimarySubtitle = if (isOsrmLoading && selectedRoute == RouteSelection.VAN_PRIMARY) "Mengira..."
                            else if (osrmRoute != null && selectedRoute == RouteSelection.VAN_PRIMARY) "ETA ~${osrmRoute!!.totalDurationMinutes.toInt()} min (${String.format(Locale.US, "%.1f", osrmRoute!!.totalDistanceKm)} km)"
                            else "Adam (SK)"
                        RouteTabCard(
                            title = strings.routeVanPrimary,
                            subtitle = vanPrimarySubtitle,
                            isSelected = selectedRoute == RouteSelection.VAN_PRIMARY,
                            modifier = Modifier.weight(1f)
                        ) { selectedRoute = RouteSelection.VAN_PRIMARY }

                        val vanSecondarySubtitle = if (isOsrmLoading && selectedRoute == RouteSelection.VAN_SECONDARY) "Mengira..."
                            else if (osrmRoute != null && selectedRoute == RouteSelection.VAN_SECONDARY) "ETA ~${osrmRoute!!.totalDurationMinutes.toInt()} min (${String.format(Locale.US, "%.1f", osrmRoute!!.totalDistanceKm)} km)"
                            else "Aisyah (SRAI)"
                        RouteTabCard(
                            title = strings.routeVanSecondary,
                            subtitle = vanSecondarySubtitle,
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
                                                text = poi.category.name,
                                                fontSize = 8.sp,
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
                                        text = String.format(Locale.US, "%.5f° U, %.5f° T • %s", poi.lat, poi.lng, if (poi.distanceKm > 0) "${poi.distanceKm} km" else "Pusat"),
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
                                        contentDescription = "Navigasi",
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
                                        contentDescription = "Laras Tepat",
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
                                        contentDescription = "Edit POI",
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
                                        contentDescription = "Padam POI",
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

        // Clearance spacer for Speed Dial FAB
        item {
            Spacer(Modifier.height(70.dp))
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
                pois = updated
                PoiStorage.savePois(context, updated)
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
            initialLat = precisePickerTarget?.latitude ?: (editingPoi?.lat ?: (home?.lat ?: 3.1390)),
            initialLng = precisePickerTarget?.longitude ?: (editingPoi?.lng ?: (home?.lng ?: 101.6869)),
            title = if (precisePickerPoi != null) "Laras Lokasi: ${precisePickerPoi!!.name}" else "Pilih Lokasi Tepat di Peta",
            homeLat = home?.lat ?: 3.1390,
            homeLng = home?.lng ?: 101.6869,
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
                    pois = updatedList
                    PoiStorage.savePois(context, updatedList)
                    selectedMapPoi = updatedPoi
                    precisePickerPoi = null
                } else {
                    droppedMapPin = GeoPoint(confirmedLat, confirmedLng)
                }

                rfCenterGeoPoint = GeoPoint(confirmedLat, confirmedLng)
                rfCenterLabel = "Titik Dipilih (${String.format(Locale.US, "%.4f, %.4f", confirmedLat, confirmedLng)})"
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
    context: Context,
    mapView: MapView,
    pois: List<PoiItem>,
    rfCenter: GeoPoint?,
    isRfActive: Boolean,
    pickedPoint: GeoPoint?,
    selectedPoiId: String?,
    selectedRoute: RouteSelection,
    osrmRoute: OsrmRouteResponse?,
    onMapTapped: (GeoPoint) -> Unit,
    onPoiSelected: (PoiItem) -> Unit
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

    // 2. Route polyline from OSRM GeoJSON geometry or waypoints
    val routePoints = mutableListOf<GeoPoint>()
    if (osrmRoute != null && osrmRoute.geometryPoints.isNotEmpty()) {
        routePoints.addAll(osrmRoute.geometryPoints.map { GeoPoint(it.first, it.second) })
    } else {
        when (selectedRoute) {
            RouteSelection.ABAH_PICKUP -> {
                val office = pois.find { it.category == PoiCategory.PEJABAT }
                val tadika = pois.find { it.id == "poi-5" } ?: pois.find { it.category == PoiCategory.TRANSIT }
                val taska = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT && it.id != tadika?.id }
                val home = pois.find { it.category == PoiCategory.RUMAH }
                office?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                tadika?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                taska?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                home?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
            }
            RouteSelection.VAN_PRIMARY -> {
                val home = pois.find { it.category == PoiCategory.RUMAH }
                val sra = pois.find { it.name.contains("SRA") }
                val taska = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT }
                val sk = pois.find { it.name.contains("SK") }
                home?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                sra?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                taska?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                sk?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                home?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
            }
            RouteSelection.VAN_SECONDARY -> {
                val home = pois.find { it.category == PoiCategory.RUMAH }
                val srai = pois.find { it.name.contains("Bukit Contoh") || it.name.contains("SRAI") }
                val taska = pois.find { it.id == "poi-4" } ?: pois.find { it.category == PoiCategory.TRANSIT }
                home?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                srai?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                taska?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
                home?.let { routePoints.add(GeoPoint(it.lat, it.lng)) }
            }
        }
    }

    if (routePoints.size >= 2) {
        val polyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = when (selectedRoute) {
                RouteSelection.ABAH_PICKUP -> android.graphics.Color.parseColor("#0284C7")
                RouteSelection.VAN_PRIMARY -> android.graphics.Color.parseColor("#2563EB")
                RouteSelection.VAN_SECONDARY -> android.graphics.Color.parseColor("#F59E0B")
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
                "${poi.iconEmoji} ${poi.address} (${if (poi.distanceKm > 0) "${poi.distanceKm} km" else "Pusat"})"
            } else {
                "${poi.iconEmoji} • ${if (poi.distanceKm > 0) "${poi.distanceKm} km" else "Pusat"}"
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
            title = "📍 Titik Dipilih Pada Peta"
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
    val isEdit = poi != null
    var name by remember { mutableStateOf(poi?.name ?: "") }
    var category by remember { mutableStateOf(poi?.category ?: PoiCategory.SEKOLAH) }
    var iconEmoji by remember { mutableStateOf(poi?.iconEmoji ?: "🏢") }
    var address by remember { mutableStateOf(poi?.address ?: "") }
    var latText by remember { mutableStateOf(if (poi != null) String.format(Locale.US, "%.5f", poi.lat) else "3.1390") }
    var lngText by remember { mutableStateOf(if (poi != null) String.format(Locale.US, "%.5f", poi.lng) else "101.6869") }
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

                // High Precision Location Picker Button
                OutlinedButton(
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = SlotifyShapes.Medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SlotifyColors.KpmSkyBlue.copy(alpha = 0.08f),
                        contentColor = SlotifyColors.KpmSkyBlue
                    )
                ) {
                    Text("🎯 Pilih Tepat di Peta (Grab-Style Reticle)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    val resolved = LocationAddressHelper.resolveAddress(context, userGpsFix.latitude, userGpsFix.longitude)
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
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SlotifyColors.ActionAmber.copy(alpha = 0.2f))
                        ) {
                            Text("📡 GPS (${userGpsFix.quality.label})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlotifyColors.ActionAmber)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            latText = "3.1390"
                            lngText = "101.6869"
                            if (address.isBlank()) address = "Pusat Bandar Contoh, Bandar Contoh, Selangor"
                        },
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Text("📍 Taman Contoh", fontSize = 10.sp)
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
