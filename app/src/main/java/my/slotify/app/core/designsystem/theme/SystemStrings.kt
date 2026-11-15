package my.slotify.app.core.designsystem.theme

import java.util.Locale

/**
 * Text rendered outside Compose: home-screen widget, notifications and channels, and
 * geocoder fallbacks. Accessed as `stringsFor(language).systemUi` (see `currentAppStrings`).
 */
data class SystemStrings(
    // Widget: static labels & loading state
    val widgetSwitch: String,
    val widgetMap: String,
    val widgetOpenTimetable: String,
    val widgetLoadingChild: String,
    val widgetLoadingZone: String,
    val widgetLoadingSchedule: String,

    // Widget: zone line
    val widgetZoneWeekendHome: String,
    val widgetZoneOpenMap: String,
    val widgetZoneCoordinates: (String) -> String,
    val widgetZoneInside: (String) -> String,
    val widgetZoneApproaching: (String, Int) -> String,
    val widgetZoneDistance: (String, String) -> String,

    // Widget: classes, bag, attire
    val widgetWeekendCurrent: String,
    val widgetWeekendNext: String,
    val widgetBagPacked: String,
    val widgetCheckBagMonday: String,
    val widgetCasualAttire: String,
    val widgetNotStarted: String,
    val widgetClassesDone: String,
    val widgetNextClass: (String, String) -> String,
    val widgetSessionOver: String,
    val widgetSessionMorning: String,
    val widgetSessionAfternoon: String,
    val widgetSessionFullDay: String,
    val widgetBookHint: (String?) -> String,

    // Widget: fallback when there is no data
    val widgetFallbackChild: String,
    val widgetFallbackCurrent: String,
    val widgetFallbackBooks: String,
    val widgetFallbackAttire: String,

    // Notification channels
    val channelAttireName: String,
    val channelAttireDesc: String,
    val channelTransitName: String,
    val channelTransitDesc: String,
    val channelFinanceName: String,
    val channelFinanceDesc: String,

    // Notifications
    val attireNotificationTitle: (String) -> String,
    val attireNotificationBody: (String, String) -> String,
    val attireNotificationShort: (String) -> String,
    val attireBooksFallback: String,
    val transitNotificationTitle: (String) -> String,
    val geofenceEnteredTitle: (String) -> String,
    val geofenceApproachingTitle: (String) -> String,
    val geofenceUpdateTitle: (String) -> String,
    val geofenceEnteredBody: (String, Int) -> String,
    val geofenceApproachingBody: (String, Int) -> String,
    val geofenceUpdateBody: (String) -> String,

    // Reverse-geocoding fallbacks
    val geocoderLanguageTag: String,
    val addressCoordinates: (Double, Double) -> String,
    val directionNorth: String,
    val directionSouth: String,
    val directionEast: String,
    val directionWest: String,
    val directionNorthEast: String,
    val directionNorthWest: String,
    val directionSouthEast: String,
    val directionSouthWest: String
)

private fun bmBookHint(code: String?): String = when (code) {
    "BM" -> "📚 Bawa: Buku Teks Bahasa Melayu Jilid 1 & Buku Latihan"
    "MT" -> "📚 Bawa: Buku Teks Matematik & Buku Latihan Jilid 1"
    "SN" -> "📚 Bawa: Buku Teks Sains & Buku Amali Sains"
    "BI" -> "📚 Bawa: Buku Teks Bahasa Inggeris & Reading Log"
    "SEJ" -> "📚 Bawa: Buku Teks Sejarah & Nota Sejarah"
    "PI" -> "📚 Bawa: Buku Teks Pendidikan Islam & Buku Latihan PI"
    "TAWHID" -> "📚 Bawa: Kitab Tauhid & Buku Nota SRA"
    "JAWI" -> "📚 Bawa: Buku Latihan Jawi & Kaedah Baghdadiyyah"
    "BA" -> "📚 Bawa: Buku Teks Bahasa Arab & Kamus Arab-Melayu"
    "TAHFIZ", "QURAN" -> "📚 Bawa: Al-Quran & Buku Rekod Hafazan"
    "FIQH" -> "📚 Bawa: Buku Teks Fiqah & Buku Latihan"
    "PJPK" -> "👟 Bawa: Kasut Sukan & Baju Sukan"
    "PSV" -> "🎨 Bawa: Palet Warna & Buku Lukisan"
    "KH" -> "🔧 Bawa: Set Jahitan / Alat Masakan (semak jadual)"
    "IQRA" -> "📚 Bawa: Buku Iqra' & Buku Doa Harian"
    "RBT" -> "🔩 Bawa: Buku Reka Bentuk & Teknologi"
    else -> "📚 Semak beg mengikut jadual hari ini"
}

private fun enBookHint(code: String?): String = when (code) {
    "BM" -> "📚 Bring: Bahasa Melayu Textbook Vol. 1 & Workbook"
    "MT" -> "📚 Bring: Mathematics Textbook & Workbook Vol. 1"
    "SN" -> "📚 Bring: Science Textbook & Science Practical Book"
    "BI" -> "📚 Bring: English Textbook & Reading Log"
    "SEJ" -> "📚 Bring: History Textbook & History Notes"
    "PI" -> "📚 Bring: Islamic Education Textbook & PI Workbook"
    "TAWHID" -> "📚 Bring: Tauhid Book & SRA Notebook"
    "JAWI" -> "📚 Bring: Jawi Workbook & Kaedah Baghdadiyyah"
    "BA" -> "📚 Bring: Arabic Textbook & Arabic-Malay Dictionary"
    "TAHFIZ", "QURAN" -> "📚 Bring: Al-Quran & Memorisation Record Book"
    "FIQH" -> "📚 Bring: Fiqh Textbook & Workbook"
    "PJPK" -> "👟 Bring: Sports Shoes & Sports Attire"
    "PSV" -> "🎨 Bring: Colour Palette & Sketchbook"
    "KH" -> "🔧 Bring: Sewing Set / Cooking Tools (check timetable)"
    "IQRA" -> "📚 Bring: Iqra' Book & Daily Prayer Book"
    "RBT" -> "🔩 Bring: Design & Technology Book"
    else -> "📚 Check the bag against today's timetable"
}

val BmSystemStrings = SystemStrings(
    widgetSwitch = "⇄ Tukar",
    widgetMap = "Peta 🗺️",
    widgetOpenTimetable = "Jadual ➔",
    widgetLoadingChild = "Memuatkan…",
    widgetLoadingZone = "📍 Membaca Zon GPS…",
    widgetLoadingSchedule = "⏳ Mengambil jadual…",

    widgetZoneWeekendHome = "📍 Zon: 🏠 Rumah (Hujung Minggu)",
    widgetZoneOpenMap = "📍 Zon: Buka Peta Logistik",
    widgetZoneCoordinates = { c -> "📍 Koordinat: $c" },
    widgetZoneInside = { name -> "📍 Zon: $name (Dalam Zon)" },
    widgetZoneApproaching = { name, m -> "📍 Menghampiri $name (~${m}m)" },
    widgetZoneDistance = { name, km -> "📍 Zon: $name (~$km)" },

    widgetWeekendCurrent = "🎉 Hujung Minggu! Rehat & Enjoy",
    widgetWeekendNext = "⏩ Sekolah semula Isnin",
    widgetBagPacked = "🎒 Beg Siap Dikemas ✓",
    widgetCheckBagMonday = "📚 Semak beg untuk Isnin",
    widgetCasualAttire = "Santai",
    widgetNotStarted = "⏳ Belum bermula — bersedia sekarang!",
    widgetClassesDone = "✅ Kelas hari ini selesai",
    widgetNextClass = { subject, time -> "⏩ Seterusnya: $subject ($time)" },
    widgetSessionOver = "⏩ Tamat sesi hari ini ✓",
    widgetSessionMorning = "Sesi Pagi",
    widgetSessionAfternoon = "Sesi Petang",
    widgetSessionFullDay = "Seharian",
    widgetBookHint = ::bmBookHint,

    widgetFallbackChild = "Murid",
    widgetFallbackCurrent = "⏳ Buka Slotify untuk maklumat",
    widgetFallbackBooks = "📚 Buka app untuk semak beg",
    widgetFallbackAttire = "👕 Semak pakaian sekolah",

    channelAttireName = "Slotify: Peringatan Uniform Sekolah",
    channelAttireDesc = "Peringatan jadual uniform KPM dan buku sekolah harian",
    channelTransitName = "Slotify: Transit & Logistik",
    channelTransitDesc = "Peringatan drop beg transit dan jadual van sekolah",
    channelFinanceName = "Slotify: Peringatan Yuran",
    channelFinanceDesc = "Peringatan komitmen yuran bulanan dan belanja kantin",

    attireNotificationTitle = { child -> "Peringatan Uniform Esok: $child" },
    attireNotificationBody = { attire, books -> "Pakaian: $attire\nBawa: $books" },
    attireNotificationShort = { attire -> "Uniform: $attire • Semak beg sekolah!" },
    attireBooksFallback = "Semak jadual esok",
    transitNotificationTitle = { child -> "Logistik Transit: $child" },
    geofenceEnteredTitle = { name -> "📍 Ketibaan di Zon: $name" },
    geofenceApproachingTitle = { name -> "🔔 Menghampiri: $name" },
    geofenceUpdateTitle = { name -> "📡 Zon Logistik: $name" },
    geofenceEnteredBody = { name, m -> "Kenderaan / anak kini telah tiba dalam radius ${m}m $name." },
    geofenceApproachingBody = { name, m -> "Kenderaan sedang menghampiri $name (~${m}m lagi)." },
    geofenceUpdateBody = { name -> "Status logistik dikemaskini untuk $name." },

    geocoderLanguageTag = "ms-MY",
    addressCoordinates = { lat, lng -> String.format(Locale.US, "Lokasi Peta (%.5f° U, %.5f° T)", lat, lng) },
    directionNorth = "Utara",
    directionSouth = "Selatan",
    directionEast = "Timur",
    directionWest = "Barat",
    directionNorthEast = "Timur Laut",
    directionNorthWest = "Barat Laut",
    directionSouthEast = "Tenggara",
    directionSouthWest = "Barat Daya"
)

val EnSystemStrings = SystemStrings(
    widgetSwitch = "⇄ Switch",
    widgetMap = "Map 🗺️",
    widgetOpenTimetable = "Timetable ➔",
    widgetLoadingChild = "Loading…",
    widgetLoadingZone = "📍 Reading GPS zone…",
    widgetLoadingSchedule = "⏳ Fetching timetable…",

    widgetZoneWeekendHome = "📍 Zone: 🏠 Home (Weekend)",
    widgetZoneOpenMap = "📍 Zone: Open Logistics Map",
    widgetZoneCoordinates = { c -> "📍 Coordinates: $c" },
    widgetZoneInside = { name -> "📍 Zone: $name (Inside)" },
    widgetZoneApproaching = { name, m -> "📍 Approaching $name (~${m}m)" },
    widgetZoneDistance = { name, km -> "📍 Zone: $name (~$km)" },

    widgetWeekendCurrent = "🎉 Weekend! Rest & enjoy",
    widgetWeekendNext = "⏩ School resumes Monday",
    widgetBagPacked = "🎒 Bag packed ✓",
    widgetCheckBagMonday = "📚 Check the bag for Monday",
    widgetCasualAttire = "Casual",
    widgetNotStarted = "⏳ Not started yet — get ready!",
    widgetClassesDone = "✅ Today's classes are done",
    widgetNextClass = { subject, time -> "⏩ Next: $subject ($time)" },
    widgetSessionOver = "⏩ Today's session is over ✓",
    widgetSessionMorning = "Morning Session",
    widgetSessionAfternoon = "Afternoon Session",
    widgetSessionFullDay = "Full Day",
    widgetBookHint = ::enBookHint,

    widgetFallbackChild = "Student",
    widgetFallbackCurrent = "⏳ Open Slotify for details",
    widgetFallbackBooks = "📚 Open the app to check the bag",
    widgetFallbackAttire = "👕 Check school attire",

    channelAttireName = "Slotify: School Uniform Reminders",
    channelAttireDesc = "Daily KPM uniform and school book reminders",
    channelTransitName = "Slotify: Transit & Logistics",
    channelTransitDesc = "Transit bag drop and school van reminders",
    channelFinanceName = "Slotify: Fee Reminders",
    channelFinanceDesc = "Monthly fee commitments and canteen spending reminders",

    attireNotificationTitle = { child -> "Tomorrow's uniform: $child" },
    attireNotificationBody = { attire, books -> "Attire: $attire\nBring: $books" },
    attireNotificationShort = { attire -> "Uniform: $attire • Check the school bag!" },
    attireBooksFallback = "Check tomorrow's timetable",
    transitNotificationTitle = { child -> "Transit logistics: $child" },
    geofenceEnteredTitle = { name -> "📍 Arrived at zone: $name" },
    geofenceApproachingTitle = { name -> "🔔 Approaching: $name" },
    geofenceUpdateTitle = { name -> "📡 Logistics zone: $name" },
    geofenceEnteredBody = { name, m -> "The vehicle / child is now within ${m}m of $name." },
    geofenceApproachingBody = { name, m -> "The vehicle is approaching $name (~${m}m to go)." },
    geofenceUpdateBody = { name -> "Logistics status updated for $name." },

    geocoderLanguageTag = "en-MY",
    addressCoordinates = { lat, lng -> String.format(Locale.US, "Map location (%.5f° N, %.5f° E)", lat, lng) },
    directionNorth = "North",
    directionSouth = "South",
    directionEast = "East",
    directionWest = "West",
    directionNorthEast = "North-east",
    directionNorthWest = "North-west",
    directionSouthEast = "South-east",
    directionSouthWest = "South-west"
)
