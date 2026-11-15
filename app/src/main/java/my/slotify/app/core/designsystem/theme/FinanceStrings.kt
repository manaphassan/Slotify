package my.slotify.app.core.designsystem.theme

/** Finance screen, fee dialogs and finance ViewModel messages. Accessed as `strings.financeUi`. */
data class FinanceStrings(
    // Section headers
    val groupPocket: String,
    val groupFees: String,
    val groupTakwim: String,
    val groupNotifications: String,
    val pocketSummary: (String, String) -> String,
    val feesSummary: (Int, Int) -> String,

    // Pocket money card
    val decreaseSpent: String,
    val increaseSpent: String,

    // Tabung Maya card
    val tabungTitle: String,
    val tabungSubtitle: String,
    val convertSavingsHint: String,
    val convertSavingsButton: String,
    val savingsTooLow: String,
    val savingsConverted: (String) -> String,

    // Takwim KPM banner
    val takwimSessionFallback: String,
    val holidayOngoing: String,
    val holidayInDays: (Long) -> String,
    val openTakwim: String,

    // Fee list
    val addFeeButton: String,
    val noFeesThisMonth: String,
    val noFeesInFilter: String,
    val deleteFee: String,

    // Test notification card
    val testNotificationDesc: String,
    val feeReminderNotificationTitle: String,
    val allFeesSettledNotification: String,
    val testNowButton: String,

    // Add fee dialog
    val addFeeDialogTitle: String,
    val feeNameLabel: String,
    val feeNamePlaceholder: String,
    val feeAmountLabel: String,
    val feeAmountPlaceholder: String,
    val recipientLabel: String,
    val categoryIconLabel: String,
    val paymentDateLabel: String,
    val saveFeeButton: String,

    // Display labels for persisted keys (stored values stay in BM)
    val allChildren: String,
    val dueFirstOfMonth: String,
    val dueFifthOfMonth: String,
    val dueTenthOfMonth: String,
    val dueEndOfMonth: String,
    val dueOneTime: String
)

val BmFinanceStrings = FinanceStrings(
    groupPocket = "Duit Poket & Tabung",
    groupFees = "Yuran Bulanan",
    groupTakwim = "Takwim KPM",
    groupNotifications = "Notifikasi",
    pocketSummary = { spent, saved -> "Belanja $spent • Tabung $saved" },
    feesSummary = { pending, total -> "$pending belum bayar daripada $total" },

    // Pocket money card
    decreaseSpent = "Kurang",
    increaseSpent = "Tambah",

    // Tabung Maya card
    tabungTitle = "Tabung Maya Simpanan",
    tabungSubtitle = "Jumlah penjimatan duit belanja kantin",
    convertSavingsHint = "Tukar RM 10 simpanan kepada 10 Bintang ganjaran anak",
    convertSavingsButton = "+10 ⭐ Bintang",
    savingsTooLow = "Simpan sekurang-kurangnya RM 10 untuk menebus 10 ⭐ Bintang!",
    savingsConverted = { nickname -> "🎉 Tahniah! RM 10 simpanan ditukarkan kepada +10 ⭐ Bintang untuk $nickname!" },

    // Takwim KPM banner
    takwimSessionFallback = "Sesi 2026/2027",
    holidayOngoing = "Sedang Berlangsung!",
    holidayInDays = { days -> "lagi $days hari" },
    openTakwim = "Buka Takwim",

    // Fee list
    addFeeButton = "Tambah Yuran",
    noFeesThisMonth = "Tiada yuran untuk bulan ini",
    noFeesInFilter = "Tiada rekod yuran dalam kategori ini",
    deleteFee = "Padam Yuran",

    // Test notification card
    testNotificationDesc = "Uji notifikasi peringatan yuran & jadual",
    feeReminderNotificationTitle = "Slotify: Peringatan Yuran Bulanan",
    allFeesSettledNotification = "Semua yuran bulan ini telah dijelaskan. ✓",
    testNowButton = "Uji Sekarang",

    // Add fee dialog
    addFeeDialogTitle = "Tambah Komitmen Yuran",
    feeNameLabel = "Nama Yuran / Komitmen",
    feeNamePlaceholder = "Contoh: Yuran Kelas Renang",
    feeAmountLabel = "Jumlah (RM)",
    feeAmountPlaceholder = "Contoh: 120.00",
    recipientLabel = "Penerima / Anak:",
    categoryIconLabel = "Kategori Ikon:",
    paymentDateLabel = "Tarikh Bayaran:",
    saveFeeButton = "Simpan Yuran",

    // Display labels for persisted keys
    allChildren = "Semua Anak",
    dueFirstOfMonth = "1hb Setiap Bulan",
    dueFifthOfMonth = "5hb Setiap Bulan",
    dueTenthOfMonth = "10hb Setiap Bulan",
    dueEndOfMonth = "Hujung Bulan",
    dueOneTime = "Sekali Bayar"
)

val EnFinanceStrings = FinanceStrings(
    groupPocket = "Pocket Money & Savings",
    groupFees = "Monthly Fees",
    groupTakwim = "KPM Calendar",
    groupNotifications = "Notifications",
    pocketSummary = { spent, saved -> "Spent $spent • Savings $saved" },
    feesSummary = { pending, total -> "$pending of $total unpaid" },

    // Pocket money card
    decreaseSpent = "Decrease",
    increaseSpent = "Increase",

    // Tabung Maya card
    tabungTitle = "Virtual Savings Jar",
    tabungSubtitle = "Total saved from canteen allowance",
    convertSavingsHint = "Convert RM 10 of savings into 10 reward stars for your child",
    convertSavingsButton = "+10 ⭐ Stars",
    savingsTooLow = "Save at least RM 10 to redeem 10 ⭐ Stars!",
    savingsConverted = { nickname -> "🎉 Congratulations! RM 10 of savings converted to +10 ⭐ Stars for $nickname!" },

    // Takwim KPM banner
    takwimSessionFallback = "Session 2026/2027",
    holidayOngoing = "Happening now!",
    holidayInDays = { days -> "in $days days" },
    openTakwim = "Open Takwim",

    // Fee list
    addFeeButton = "Add Fee",
    noFeesThisMonth = "No fees for this month",
    noFeesInFilter = "No fee records in this category",
    deleteFee = "Delete Fee",

    // Test notification card
    testNotificationDesc = "Test fee & schedule reminder notifications",
    feeReminderNotificationTitle = "Slotify: Monthly Fee Reminder",
    allFeesSettledNotification = "All fees for this month have been settled. ✓",
    testNowButton = "Test Now",

    // Add fee dialog
    addFeeDialogTitle = "Add Fee Commitment",
    feeNameLabel = "Fee / Commitment Name",
    feeNamePlaceholder = "e.g. Swimming Class Fee",
    feeAmountLabel = "Amount (RM)",
    feeAmountPlaceholder = "e.g. 120.00",
    recipientLabel = "Recipient / Child:",
    categoryIconLabel = "Category Icon:",
    paymentDateLabel = "Payment Date:",
    saveFeeButton = "Save Fee",

    // Display labels for persisted keys
    allChildren = "All Children",
    dueFirstOfMonth = "1st of Every Month",
    dueFifthOfMonth = "5th of Every Month",
    dueTenthOfMonth = "10th of Every Month",
    dueEndOfMonth = "End of Month",
    dueOneTime = "One-time Payment"
)
