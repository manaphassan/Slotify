package my.slotify.app.core.designsystem.theme

/** App shell (drawer, top bar, speed-dial) and the reward vault. Accessed as `strings.shellUi`. */
data class ShellStrings(
    // Speed-dial FAB
    val quickActionsMenu: String,

    // Drawer theme chips (short labels)
    val themeLightShort: String,
    val themeDarkShort: String,

    // Home-screen widget install
    val widgetPinRequesting: String,
    val widgetPinUnsupported: String,

    // Reward vault
    val chestClaimedBadge: String,

    // Default reward catalogue
    val rewardScreenTimeTitle: String,
    val rewardScreenTimeDesc: String,
    val rewardToyTitle: String,
    val rewardToyDesc: String,
    val rewardIceCreamTitle: String,
    val rewardIceCreamDesc: String,
    val rewardLateBedtimeTitle: String,
    val rewardLateBedtimeDesc: String
)

val BmShellStrings = ShellStrings(
    // Speed-dial FAB
    quickActionsMenu = "Menu Tindakan Pantas",

    // Drawer theme chips (short labels)
    themeLightShort = "Cerah",
    themeDarkShort = "Gelap",

    // Home-screen widget install
    widgetPinRequesting = "Membuka pengesahan widget skrin utama...",
    widgetPinUnsupported = "Sila tekan lama pada skrin utama dan pilih Widgets ➔ Slotify",

    // Reward vault
    chestClaimedBadge = "Dituntut",

    // Default reward catalogue
    rewardScreenTimeTitle = "Main Roblox / iPad 1 Jam",
    rewardScreenTimeDesc = "Ganjaran bermain peranti dengan had waktu berjadual",
    rewardToyTitle = "Beli Mainan / Hot Wheels",
    rewardToyDesc = "Tebus mainan kegemaran pilihan anak di kedai",
    rewardIceCreamTitle = "Keluar Makan Ais Krim",
    rewardIceCreamDesc = "Ais krim FamilyMart / Tealive hujung minggu",
    rewardLateBedtimeTitle = "Tidur Lewat 30 Minit",
    rewardLateBedtimeDesc = "Kelonggaran waktu tidur hari Sabtu / cuti sekolah"
)

val EnShellStrings = ShellStrings(
    // Speed-dial FAB
    quickActionsMenu = "Quick Actions Menu",

    // Drawer theme chips (short labels)
    themeLightShort = "Light",
    themeDarkShort = "Dark",

    // Home-screen widget install
    widgetPinRequesting = "Opening home screen widget confirmation...",
    widgetPinUnsupported = "Long-press your home screen and choose Widgets ➔ Slotify",

    // Reward vault
    chestClaimedBadge = "Claimed",

    // Default reward catalogue
    rewardScreenTimeTitle = "Roblox / iPad for 1 Hour",
    rewardScreenTimeDesc = "Device play time with a scheduled limit",
    rewardToyTitle = "Buy a Toy / Hot Wheels",
    rewardToyDesc = "Redeem a favourite toy of your child's choice at the shop",
    rewardIceCreamTitle = "Ice Cream Outing",
    rewardIceCreamDesc = "Weekend ice cream at FamilyMart / Tealive",
    rewardLateBedtimeTitle = "Stay Up 30 Minutes Late",
    rewardLateBedtimeDesc = "Later bedtime on Saturdays / school holidays"
)
