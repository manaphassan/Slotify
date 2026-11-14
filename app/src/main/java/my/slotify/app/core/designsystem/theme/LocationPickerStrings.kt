package my.slotify.app.core.designsystem.theme

import java.util.Locale

/** Precise location picker modal copy. Accessed as `strings.locationPickerUi`. */
data class LocationPickerStrings(
    // Header
    val defaultTitle: String,
    val headerHint: String,
    val back: String,
    val gpsLocating: String,
    val manualButton: String,

    // Map overlay
    val preciseTarget: String,
    val coordinatesBadge: (lat: Double, lng: Double) -> String,

    // D-Pad & map controls
    val nudgeNorth: String,
    val nudgeWest: String,
    val nudgeEast: String,
    val nudgeSouth: String,
    val lockCurrentGps: String,
    val zoomIn: String,
    val zoomOut: String,

    // GPS status messages
    val gpsPermissionRequired: String,
    val gpsPermissionRequiredForCurrent: String,
    val gpsEnableInSettings: String,
    val gpsAcquiringSatellites: String,
    val gpsSignalWeak: String,

    // GPS accuracy quality labels
    val gpsQualityExcellent: String,
    val gpsQualityGood: String,
    val gpsQualityModerate: String,
    val gpsQualityCoarse: String,

    // Manual coordinate input
    val manualInputTitle: String,
    val latitudeLabel: String,
    val longitudeLabel: String,
    val goButton: String,
    val invalidCoordinates: String,

    // Landmark presets
    val quickJumpTitle: String,
    val homeLandmark: String,

    // Confirmation panel
    val loadingAddress: String,
    val verifyingAddress: String,
    val coordsWithHomeDistance: (lat: Double, lng: Double, distanceKm: Double) -> String,
    val confirmLocationButton: String
)

val BmLocationPickerStrings = LocationPickerStrings(
    // Header
    defaultTitle = "Pilih Lokasi Tepat di Peta",
    headerHint = "Gerakkan peta • Laras D-Pad • Kunci GPS",
    back = "Kembali",
    gpsLocating = "Mengesan",
    manualButton = "Manual",

    // Map overlay
    preciseTarget = "Sasaran Tepat",
    coordinatesBadge = { lat, lng -> String.format(Locale.US, "%.5f° U, %.5f° T", lat, lng) },

    // D-Pad & map controls
    nudgeNorth = "Utara",
    nudgeWest = "Barat",
    nudgeEast = "Timur",
    nudgeSouth = "Selatan",
    lockCurrentGps = "Kunci GPS Semasa",
    zoomIn = "Zum Masuk",
    zoomOut = "Zum Keluar",

    // GPS status messages
    gpsPermissionRequired = "Kebenaran GPS diperlukan",
    gpsPermissionRequiredForCurrent = "Kebenaran GPS diperlukan untuk menentukan lokasi semasa",
    gpsEnableInSettings = "Sila aktifkan GPS dalam tetapan peranti",
    gpsAcquiringSatellites = "Mendapatkan isyarat satelit GPS...",
    gpsSignalWeak = "Isyarat GPS lemah. Sila pastikan berada di kawasan lapang.",

    // GPS accuracy quality labels
    gpsQualityExcellent = "Tepat (Pintu/Pagar)",
    gpsQualityGood = "Baik (Jalan/Lorong)",
    gpsQualityModerate = "Sederhana",
    gpsQualityCoarse = "Kasar (Mencari Satelit)",

    // Manual coordinate input
    manualInputTitle = "Tampal / Masukkan Koordinat Manual:",
    latitudeLabel = "Latitude",
    longitudeLabel = "Longitude",
    goButton = "Pergi",
    invalidCoordinates = "Koordinat tidak sah (cth: 3.13900, 101.68690)",

    // Landmark presets
    quickJumpTitle = "Lompat Pantas ke Nod Tumpuan:",
    homeLandmark = "Rumah",

    // Confirmation panel
    loadingAddress = "Memuat alamat...",
    verifyingAddress = "Mengesahkan alamat sekitar...",
    coordsWithHomeDistance = { lat, lng, km ->
        String.format(Locale.US, "%.5f, %.5f • Jarak dari Rumah: %.1f km", lat, lng, km)
    },
    confirmLocationButton = "Sahkan Lokasi Ini"
)

val EnLocationPickerStrings = LocationPickerStrings(
    // Header
    defaultTitle = "Pick an Exact Location on the Map",
    headerHint = "Drag map • Fine-tune with D-Pad • Lock GPS",
    back = "Back",
    gpsLocating = "Locating",
    manualButton = "Manual",

    // Map overlay
    preciseTarget = "Precise Target",
    coordinatesBadge = { lat, lng -> String.format(Locale.US, "%.5f° N, %.5f° E", lat, lng) },

    // D-Pad & map controls
    nudgeNorth = "North",
    nudgeWest = "West",
    nudgeEast = "East",
    nudgeSouth = "South",
    lockCurrentGps = "Lock Current GPS",
    zoomIn = "Zoom In",
    zoomOut = "Zoom Out",

    // GPS status messages
    gpsPermissionRequired = "GPS permission required",
    gpsPermissionRequiredForCurrent = "GPS permission is required to find your current location",
    gpsEnableInSettings = "Please turn on GPS in device settings",
    gpsAcquiringSatellites = "Acquiring GPS satellite signal...",
    gpsSignalWeak = "Weak GPS signal. Please move to an open area.",

    // GPS accuracy quality labels
    gpsQualityExcellent = "Precise (Door/Gate)",
    gpsQualityGood = "Good (Street/Lane)",
    gpsQualityModerate = "Moderate",
    gpsQualityCoarse = "Coarse (Searching Satellites)",

    // Manual coordinate input
    manualInputTitle = "Paste / Enter Coordinates Manually:",
    latitudeLabel = "Latitude",
    longitudeLabel = "Longitude",
    goButton = "Go",
    invalidCoordinates = "Invalid coordinates (e.g. 3.13900, 101.68690)",

    // Landmark presets
    quickJumpTitle = "Quick Jump to Saved Places:",
    homeLandmark = "Home",

    // Confirmation panel
    loadingAddress = "Loading address...",
    verifyingAddress = "Verifying nearby address...",
    coordsWithHomeDistance = { lat, lng, km ->
        String.format(Locale.US, "%.5f, %.5f • Distance from Home: %.1f km", lat, lng, km)
    },
    confirmLocationButton = "Confirm This Location"
)
