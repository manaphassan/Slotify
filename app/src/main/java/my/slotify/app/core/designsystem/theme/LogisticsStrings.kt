package my.slotify.app.core.designsystem.theme

import java.util.Locale

/** Logistics map, route planner and location picker copy. Accessed as `strings.logisticsUi`. */
data class LogisticsStrings(
    // Driver contact card
    val driverActiveBadge: String,
    val callDriver: String,

    // Vehicle simulation (HUD, status & notifications)
    val simNotificationTag: String,
    val simStatusArrived: String,
    val simStatusApproaching: String,
    val simStatusWithin1km: String,
    val simStatusWithin3km: String,
    val simStatusEnRoute: (Int) -> String,
    val simAlert3km: String,
    val simAlert1km: (String) -> String,
    val simAlert300m: String,
    val simAlertArrived: (String) -> String,
    val speedKmh: (Int) -> String,
    val simRemainingEta: (Double, Int) -> String,
    val muteVoice: String,
    val unmuteVoice: String,
    val followCamera: String,
    val stopSimulation: String,
    val vehicleMarkerSnippet: (Int, Double, Int) -> String,

    // GPS status & permission
    val gpsPermissionRequired: String,
    val gpsEnableService: String,
    val gpsAcquiring: String,
    val gpsWeakCenteringHome: String,
    val gpsPermissionRequiredForLocation: String,
    val gpsPermissionBanner: String,
    val grantButton: String,
    val geofenceArrivedAt: (String) -> String,
    val geofenceApproaching: (String, Int) -> String,

    // Map floating buttons
    val poiHomeFallbackName: String,
    val centeredOnHome: (String) -> String,
    val lockGpsLocation: String,
    val pickPreciseLocation: String,
    val mapAlignedNorth: String,
    val compassEnabled: String,
    val northLocked: String,
    val compassOrientation: String,

    // Selected POI & dropped pin cards
    val coordsFromHome: (Double, Double, Double) -> String,
    val directionsButton: String,
    val setRfCenterButton: String,
    val adjustButton: String,
    val editButton: String,
    val droppedPinTitle: String,
    val savePoiButton: String,
    val rfCenterMapPoint: (String) -> String,
    val rfCenterSelectedPoint: (String) -> String,
    val pickedPointMarkerTitle: String,

    // RF coverage section
    val rfCenterHome: String,
    val rfCenterCurrentGps: String,
    val rfCoverageTitle: String,
    val rfCoverageSubtitle: String,
    val rfPulseBadge: String,
    val rfTransmitterCenter: (String) -> String,
    val rfBaseHome: String,
    val rfBaseOffice: String,
    val rfBaseSchool: String,
    val rfGpsLocation: (String) -> String,
    val rfBasePickOnMap: String,
    val rfTapAnywhere: String,
    val rfAnalysisTitle: String,
    val rfSignalStrong: String,
    val rfSignalModerate: String,
    val rfOutOfRange: String,
    val rfOverlayCenterLabel: String,

    // Route chain card
    val recalculateRoute: String,
    val startSimShort: String,
    val stopSimShort: String,
    val routeCalculating: String,
    val routeEta: (Int, Double) -> String,
    val routeOffline: String,

    // POI list
    val poiListTitle: (Int) -> String,
    val poiListSubtitle: String,
    val resetDefaults: String,
    val addButton: String,
    val poiCoordsWithDistance: (Double, Double, String) -> String,
    val distanceCenter: String,
    val navigate: String,
    val adjustPrecise: String,
    val editPoi: String,
    val deletePoi: String,

    // Precise picker titles
    val adjustLocationTitle: (String) -> String,
    val pickPreciseLocationTitle: String,

    // Delete & reset dialogs
    val deletePoiTitle: String,
    val deletePoiMessage: (String) -> String,
    val deleteButton: String,
    val resetPoisTitle: String,
    val resetPoisMessage: String,
    val resetButton: String,

    // Add / edit POI dialog
    val editPoiTitle: String,
    val addPoiTitle: String,
    val categorySchool: String,
    val categoryTransit: String,
    val categoryHome: String,
    val categoryOffice: String,
    val poiNameLabel: String,
    val poiNamePlaceholder: String,
    val categoryLabel: String,
    val emojiLabel: String,
    val addressLabel: String,
    val addressPlaceholder: String,
    val latitudeLabel: String,
    val longitudeLabel: String,
    val pickOnMapButton: String,
    val presetOffice: String,
    val presetHome: String,
    val distanceLabel: String,
    val errorNameRequired: String,
    val errorInvalidCoordinates: String,
    val addPoiButton: String
)

val BmLogisticsStrings = LogisticsStrings(
    // Driver contact card
    driverActiveBadge = "Aktif",
    callDriver = "Panggil",

    // Vehicle simulation (HUD, status & notifications)
    simNotificationTag = "[Simulasi]",
    simStatusArrived = "Tiba di Destinasi",
    simStatusApproaching = "Menghampiri Destinasi",
    simStatusWithin1km = "Dalam Jarak 1.0km",
    simStatusWithin3km = "Dalam Jarak 3.0km",
    simStatusEnRoute = { percent -> "Dalam Perjalanan ($percent%)" },
    simAlert3km = "🚨 Radar 3,000m: Baki 3.0km ke destinasi • Bersedia!",
    simAlert1km = { vehicle -> "🚨 Radar 1,000m: $vehicle sedang menghampiri destinasi!" },
    simAlert300m = "🚨 Radar 300m: Ketibaan dalam 300m!",
    simAlertArrived = { vehicle -> "✅ Selamat Tiba: $vehicle telah sampai ke destinasi!" },
    speedKmh = { kmh -> "$kmh km/j" },
    simRemainingEta = { km, min -> "Baki ${String.format(Locale.US, "%.1f", km)} km • ETA ~$min min" },
    muteVoice = "Senyapkan Suara",
    unmuteVoice = "Buka Suara",
    followCamera = "Ikut Kamera",
    stopSimulation = "Henti Simulasi",
    vehicleMarkerSnippet = { kmh, km, min ->
        "$kmh km/j • Baki ${String.format(Locale.US, "%.1f", km)} km (~$min min)"
    },

    // GPS status & permission
    gpsPermissionRequired = "Kebenaran GPS diperlukan",
    gpsEnableService = "Sila aktifkan perkhidmatan GPS dalam tetapan peranti",
    gpsAcquiring = "Mendapatkan isyarat satelit GPS...",
    gpsWeakCenteringHome = "Isyarat GPS lemah. Memusatkan pada Rumah Kediaman.",
    gpsPermissionRequiredForLocation = "Kebenaran GPS diperlukan untuk menentukan lokasi semasa",
    gpsPermissionBanner = "Kebenaran GPS diperlukan untuk paparan lokasi tepat pada peta.",
    grantButton = "Beri",
    geofenceArrivedAt = { place -> "Tiba di $place" },
    geofenceApproaching = { place, meters -> "Menghampiri $place (${meters}m)" },

    // Map floating buttons
    poiHomeFallbackName = "Rumah",
    centeredOnHome = { place -> "🏠 Dipusatkan ke $place (Mod Rumah)" },
    lockGpsLocation = "Kunci Lokasi GPS",
    pickPreciseLocation = "Pilih Lokasi Tepat",
    mapAlignedNorth = "🧭 Peta Dilaras ke Arah Utara",
    compassEnabled = "🧭 Kompas Sensor Diaktifkan",
    northLocked = "🧭 Arah Utara Dikunci (0°)",
    compassOrientation = "Kompas & Orientasi",

    // Selected POI & dropped pin cards
    coordsFromHome = { lat, lng, km ->
        String.format(Locale.US, "%.5f° U, %.5f° T • %.1f km dari Rumah", lat, lng, km)
    },
    directionsButton = "Arah",
    setRfCenterButton = "📡 Pusat RF",
    adjustButton = "🎯 Laras",
    editButton = "Edit",
    droppedPinTitle = "Nod Ditandakan Pada Peta",
    savePoiButton = "Simpan POI",
    rfCenterMapPoint = { coords -> "Titik Peta ($coords)" },
    rfCenterSelectedPoint = { coords -> "Titik Dipilih ($coords)" },
    pickedPointMarkerTitle = "📍 Titik Dipilih Pada Peta",

    // RF coverage section
    rfCenterHome = "Rumah Kediaman",
    rfCenterCurrentGps = "GPS Semasa",
    rfCoverageTitle = "Liputan Gelombang RF (3.0 km)",
    rfCoverageSubtitle = "Simulasi liputan LoRa / Radio Paging / Tracker",
    rfPulseBadge = "3.0 km PULSE",
    rfTransmitterCenter = { label -> "Pusat Pemancar RF: $label" },
    rfBaseHome = "🏠 Rumah",
    rfBaseOffice = "🏢 Pejabat",
    rfBaseSchool = "Sekolah",
    rfGpsLocation = { quality -> "Lokasi GPS ($quality)" },
    rfBasePickOnMap = "🎯 Pilih Peta",
    rfTapAnywhere = "Sentuh mana-mana pada peta",
    rfAnalysisTitle = "Analisis Liputan Nod Lokasi:",
    rfSignalStrong = "Sinyal Sangat Kuat • Direct RF",
    rfSignalModerate = "Sinyal Sederhana • Dalam 3.0 km",
    rfOutOfRange = "Luar Jarak 3.0 km • Perlu Selular",
    rfOverlayCenterLabel = "Pusat RF 3.0 km",

    // Route chain card
    recalculateRoute = "Kira Semula OSRM",
    startSimShort = "▶️ Mula Simu",
    stopSimShort = "⏹️ Henti Simu",
    routeCalculating = "Mengira...",
    routeEta = { min, km -> "ETA ~$min min (${String.format(Locale.US, "%.1f", km)} km)" },
    routeOffline = "Luar talian • tiada ETA",

    // POI list
    poiListTitle = { count -> "Senarai Nod Lokasi ($count POI)" },
    poiListSubtitle = "Ketik kad untuk fokus peta • Urus nod lokasi",
    resetDefaults = "Set Semula Asal",
    addButton = "Tambah",
    poiCoordsWithDistance = { lat, lng, distance ->
        String.format(Locale.US, "%.5f° U, %.5f° T • %s", lat, lng, distance)
    },
    distanceCenter = "Pusat",
    navigate = "Navigasi",
    adjustPrecise = "Laras Tepat",
    editPoi = "Edit POI",
    deletePoi = "Padam POI",

    // Precise picker titles
    adjustLocationTitle = { place -> "Laras Lokasi: $place" },
    pickPreciseLocationTitle = "Pilih Lokasi Tepat di Peta",

    // Delete & reset dialogs
    deletePoiTitle = "Padam Nod POI?",
    deletePoiMessage = { place -> "Adakah anda pasti mahu memadam '$place' daripada senarai dan peta logistik?" },
    deleteButton = "Padam",
    resetPoisTitle = "Set Semula Senarai POI?",
    resetPoisMessage = "Tindakan ini akan mengembalikan semua nod lokasi kepada senarai asal profil keluarga.",
    resetButton = "Set Semula",

    // Add / edit POI dialog
    editPoiTitle = "Kemaskini Nod POI",
    addPoiTitle = "Tambah Nod POI Baru",
    categorySchool = "Sekolah",
    categoryTransit = "Transit",
    categoryHome = "Rumah",
    categoryOffice = "Pejabat",
    poiNameLabel = "Nama Lokasi / POI",
    poiNamePlaceholder = "cth: Pejabat / Tadika",
    categoryLabel = "Kategori Lokasi:",
    emojiLabel = "Pilih Ikon Emoji:",
    addressLabel = "Alamat / Keterangan Lokasi",
    addressPlaceholder = "cth: No 1, Jalan Contoh, Taman Contoh",
    latitudeLabel = "Latitude",
    longitudeLabel = "Longitude",
    pickOnMapButton = "🎯 Pilih Tepat di Peta (Grab-Style Reticle)",
    presetOffice = "📍 Pejabat",
    presetHome = "📍 Rumah",
    distanceLabel = "Jarak Anggaran dari Rumah (km)",
    errorNameRequired = "Sila masukkan nama lokasi",
    errorInvalidCoordinates = "Koordinat latitude & longitude tidak sah",
    addPoiButton = "Tambah POI"
)

val EnLogisticsStrings = LogisticsStrings(
    // Driver contact card
    driverActiveBadge = "Active",
    callDriver = "Call",

    // Vehicle simulation (HUD, status & notifications)
    simNotificationTag = "[Simulation]",
    simStatusArrived = "Arrived at Destination",
    simStatusApproaching = "Approaching Destination",
    simStatusWithin1km = "Within 1.0km",
    simStatusWithin3km = "Within 3.0km",
    simStatusEnRoute = { percent -> "En Route ($percent%)" },
    simAlert3km = "🚨 Radar 3,000m: 3.0km to destination • Get ready!",
    simAlert1km = { vehicle -> "🚨 Radar 1,000m: $vehicle is approaching the destination!" },
    simAlert300m = "🚨 Radar 300m: Arriving within 300m!",
    simAlertArrived = { vehicle -> "✅ Arrived Safely: $vehicle has reached the destination!" },
    speedKmh = { kmh -> "$kmh km/h" },
    simRemainingEta = { km, min -> "${String.format(Locale.US, "%.1f", km)} km left • ETA ~$min min" },
    muteVoice = "Mute voice",
    unmuteVoice = "Unmute voice",
    followCamera = "Follow camera",
    stopSimulation = "Stop simulation",
    vehicleMarkerSnippet = { kmh, km, min ->
        "$kmh km/h • ${String.format(Locale.US, "%.1f", km)} km left (~$min min)"
    },

    // GPS status & permission
    gpsPermissionRequired = "GPS permission required",
    gpsEnableService = "Please turn on location services in device settings",
    gpsAcquiring = "Acquiring GPS satellite signal...",
    gpsWeakCenteringHome = "Weak GPS signal. Centring on home.",
    gpsPermissionRequiredForLocation = "GPS permission is needed to find your current location",
    gpsPermissionBanner = "GPS permission is needed to show your exact location on the map.",
    grantButton = "Allow",
    geofenceArrivedAt = { place -> "Arrived at $place" },
    geofenceApproaching = { place, meters -> "Approaching $place (${meters}m)" },

    // Map floating buttons
    poiHomeFallbackName = "Home",
    centeredOnHome = { place -> "🏠 Centred on $place (Home mode)" },
    lockGpsLocation = "Lock GPS location",
    pickPreciseLocation = "Pick exact location",
    mapAlignedNorth = "🧭 Map aligned to north",
    compassEnabled = "🧭 Compass sensor enabled",
    northLocked = "🧭 North locked (0°)",
    compassOrientation = "Compass & orientation",

    // Selected POI & dropped pin cards
    coordsFromHome = { lat, lng, km ->
        String.format(Locale.US, "%.5f° N, %.5f° E • %.1f km from home", lat, lng, km)
    },
    directionsButton = "Directions",
    setRfCenterButton = "📡 RF Centre",
    adjustButton = "🎯 Adjust",
    editButton = "Edit",
    droppedPinTitle = "Point Marked on Map",
    savePoiButton = "Save POI",
    rfCenterMapPoint = { coords -> "Map point ($coords)" },
    rfCenterSelectedPoint = { coords -> "Selected point ($coords)" },
    pickedPointMarkerTitle = "📍 Selected Point on Map",

    // RF coverage section
    rfCenterHome = "Home",
    rfCenterCurrentGps = "Current GPS",
    rfCoverageTitle = "RF Wave Coverage (3.0 km)",
    rfCoverageSubtitle = "Simulated LoRa / radio paging / tracker coverage",
    rfPulseBadge = "3.0 km PULSE",
    rfTransmitterCenter = { label -> "RF transmitter centre: $label" },
    rfBaseHome = "🏠 Home",
    rfBaseOffice = "🏢 Office",
    rfBaseSchool = "School",
    rfGpsLocation = { quality -> "GPS location ($quality)" },
    rfBasePickOnMap = "🎯 Pick on Map",
    rfTapAnywhere = "Tap anywhere on the map",
    rfAnalysisTitle = "Location Coverage Analysis:",
    rfSignalStrong = "Very strong signal • Direct RF",
    rfSignalModerate = "Moderate signal • Within 3.0 km",
    rfOutOfRange = "Beyond 3.0 km • Needs cellular",
    rfOverlayCenterLabel = "RF centre 3.0 km",

    // Route chain card
    recalculateRoute = "Recalculate OSRM route",
    startSimShort = "▶️ Start Sim",
    stopSimShort = "⏹️ Stop Sim",
    routeCalculating = "Calculating...",
    routeEta = { min, km -> "ETA ~$min min (${String.format(Locale.US, "%.1f", km)} km)" },
    routeOffline = "Offline • no ETA",

    // POI list
    poiListTitle = { count -> "Location Nodes ($count ${if (count == 1) "POI" else "POIs"})" },
    poiListSubtitle = "Tap a card to focus the map • Manage locations",
    resetDefaults = "Reset to defaults",
    addButton = "Add",
    poiCoordsWithDistance = { lat, lng, distance ->
        String.format(Locale.US, "%.5f° N, %.5f° E • %s", lat, lng, distance)
    },
    distanceCenter = "Centre",
    navigate = "Navigate",
    adjustPrecise = "Fine-tune location",
    editPoi = "Edit POI",
    deletePoi = "Delete POI",

    // Precise picker titles
    adjustLocationTitle = { place -> "Adjust Location: $place" },
    pickPreciseLocationTitle = "Pick Exact Location on Map",

    // Delete & reset dialogs
    deletePoiTitle = "Delete POI?",
    deletePoiMessage = { place -> "Are you sure you want to delete '$place' from the list and logistics map?" },
    deleteButton = "Delete",
    resetPoisTitle = "Reset POI List?",
    resetPoisMessage = "This restores all locations to the family profile's original list.",
    resetButton = "Reset",

    // Add / edit POI dialog
    editPoiTitle = "Edit POI",
    addPoiTitle = "Add New POI",
    categorySchool = "School",
    categoryTransit = "Transit",
    categoryHome = "Home",
    categoryOffice = "Office",
    poiNameLabel = "Location / POI name",
    poiNamePlaceholder = "e.g. Office / Kindergarten",
    categoryLabel = "Location category:",
    emojiLabel = "Choose an emoji icon:",
    addressLabel = "Address / location details",
    addressPlaceholder = "e.g. No 1, Jalan Contoh, Taman Contoh",
    latitudeLabel = "Latitude",
    longitudeLabel = "Longitude",
    pickOnMapButton = "🎯 Pick Exactly on Map (Grab-style reticle)",
    presetOffice = "📍 Office",
    presetHome = "📍 Home",
    distanceLabel = "Approx. distance from home (km)",
    errorNameRequired = "Please enter a location name",
    errorInvalidCoordinates = "Invalid latitude & longitude coordinates",
    addPoiButton = "Add POI"
)
