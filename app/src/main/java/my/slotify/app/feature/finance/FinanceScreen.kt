package my.slotify.app.feature.finance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import my.slotify.app.core.database.CommitmentFeeEntity
import my.slotify.app.core.database.StudentEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.slotify.app.core.di.SlotifyViewModelFactory
import my.slotify.app.core.model.Money
import my.slotify.app.core.time.rememberNow
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.finance.FeeRepository
import my.slotify.app.core.notification.SlotifyNotificationManager
import my.slotify.app.core.takwim.KpmTakwimRepository
import my.slotify.app.feature.takwim.TakwimCalendarDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ── Finance screen — Room-backed with Tabung Maya, Fee Management & Takwim KPM ──

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel = viewModel(factory = SlotifyViewModelFactory)
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val vmMessage by viewModel.message.collectAsStateWithLifecycle()

    val now by rememberNow()
    // Recurring fees from the previous month are copied in when a new month starts.
    LaunchedEffect(now.toLocalDate()) { viewModel.setToday(now.toLocalDate()) }
    val todayDate = now.toLocalDate()

    val selectedStudent = state.student
    val allStudents = state.students
    val billingMonth = state.billingMonth

    // Dialog & UI states
    var showAddFeeDialog by remember { mutableStateOf(false) }
    var showTakwimDialog by remember { mutableStateOf(false) }
    var feeFilter by remember { mutableStateOf(0) } // 0: Semua, 1: Belum Bayar, 2: Selesai
    var celebrationMessage by remember { mutableStateOf<String?>(null) }
    var notificationSentSnack by remember { mutableStateOf(false) }
    LaunchedEffect(vmMessage) {
        vmMessage?.let { celebrationMessage = it; viewModel.consumeMessage() }
    }

    val commitmentList: List<CommitmentFeeEntity> = state.fees
    val settledCount = commitmentList.count { it.isPaid }
    val pendingCount = commitmentList.size - settledCount
    val progressFraction = if (state.totalFeesSen > 0) state.paidFeesSen.toFloat() / state.totalFeesSen else 0f

    val filteredFees = remember(commitmentList, feeFilter) {
        when (feeFilter) {
            1 -> commitmentList.filter { !it.isPaid }
            2 -> commitmentList.filter { it.isPaid }
            else -> commitmentList
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.financeTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = strings.financeSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlotifyColors.TextSecondary
                    )
                }

                // Button to open KPM Takwim
                FilledTonalButton(
                    onClick = { showTakwimDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SlotifyColors.KpmBlue.copy(alpha = 0.12f),
                        contentColor = SlotifyColors.KpmBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🗓️ Takwim KPM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Celebration Banner when converting savings to stars
        if (celebrationMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlotifyColors.MathAmber.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 24.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = celebrationMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                        }
                        IconButton(onClick = { celebrationMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // Daily Pocket Money Card (Duit Poket)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SlotifyColors.KpmBlue.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SlotifyColors.KpmBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${strings.dailyPocketMoneyTitle} (${selectedStudent?.nickname ?: "Anak"})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = "${strings.canteenBudgetLabel}: ${Money.format(state.budgetSen)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = SlotifyColors.KpmBlue
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SlotifyColors.TextTertiary.copy(alpha = 0.2f), thickness = 1.dp)

                    // Spent and Savings counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.canteenSpentLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
                            Text(
                                text = Money.format(state.spentTodaySen),
                                style = MaterialTheme.typography.headlineMedium,
                                color = SlotifyColors.EnglishRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.savingsLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
                            Text(
                                text = "+ ${Money.format(state.savedTodaySen)}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SlotifyColors.ScienceGreen
                            )
                        }
                    }

                    // Interactive allowance adjustments (+ / - RM0.50 & quick presets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.quickPresetsLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SlotifyColors.TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // - RM 0.50
                            FilledTonalIconButton(
                                onClick = { viewModel.adjustSpentToday(-50) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = SlotifyColors.CanvasBase
                                )
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Kurang",
                                    modifier = Modifier.size(16.dp),
                                    tint = SlotifyColors.TextPrimary
                                )
                            }
                            // Quick RM 3.00
                            OutlinedButton(
                                onClick = { viewModel.setSpentToday(300) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = SlotifyShapes.Small
                            ) {
                                Text(
                                    "RM 3",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            // Quick RM 5.00
                            OutlinedButton(
                                onClick = { viewModel.setSpentToday(500) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = SlotifyShapes.Small
                            ) {
                                Text(
                                    "RM 5",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            // + RM 0.50
                            FilledTonalIconButton(
                                onClick = { viewModel.adjustSpentToday(50) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = SlotifyColors.CanvasBase
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    modifier = Modifier.size(16.dp),
                                    tint = SlotifyColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tabung Maya (Virtual Piggy Bank) Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SlotifyColors.ScienceGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🪙", fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tabung Maya Simpanan",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )
                                Text(
                                    text = "Jumlah penjimatan duit belanja kantin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlotifyColors.TextSecondary
                                )
                            }
                        }

                        Text(
                            text = Money.format(state.tabungSen),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.ScienceGreen
                        )
                    }

                    HorizontalDivider(color = SlotifyColors.TextTertiary.copy(alpha = 0.15f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tukar RM 10 simpanan kepada 10 Bintang ganjaran anak",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlotifyColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.convertSavings() },
                            shape = SlotifyShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ScienceGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("+10 ⭐ Bintang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // KPM Takwim Status Banner
        item {
            val upcomingHoliday =
                remember { KpmTakwimRepository.getUpcomingHolidays(todayDate, limit = 1).firstOrNull() }
            val currentTerm = remember { KpmTakwimRepository.getCurrentTerm(todayDate) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 14.dp)
                    .background(SlotifyColors.SurfaceCard, RoundedCornerShape(14.dp))
                    .clickable { showTakwimDialog = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(upcomingHoliday?.emoji ?: "🗓️", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Takwim KPM: ${currentTerm?.name ?: "Sesi 2026/2027"}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                            if (upcomingHoliday != null) {
                                val days = upcomingHoliday.daysUntil(todayDate)
                                val daysText = if (days <= 0) "Sedang Berlangsung!" else "lagi $days hari"
                                Text(
                                    text = "${upcomingHoliday.titleBm} ($daysText)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlotifyColors.KpmBlue
                                )
                            }
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Buka Takwim",
                        tint = SlotifyColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Monthly Commitment Header & Add Fee Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.monthlyCommitmentTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                text = "$settledCount / ${commitmentList.size} ${strings.paidLabel} (${Money.format(state.paidFeesSen)} / ${Money.format(state.totalFeesSen)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
                        }

                        Button(
                            onClick = { showAddFeeDialog = true },
                            shape = SlotifyShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah Yuran", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(SlotifyShapes.XSmall),
                        color = SlotifyColors.ScienceGreen,
                        trackColor = SlotifyColors.TextTertiary.copy(alpha = 0.2f)
                    )

                    // Fee Filter Tabs
                    TabRow(
                        selectedTabIndex = feeFilter,
                        containerColor = SlotifyColors.CanvasBase,
                        contentColor = SlotifyColors.KpmBlue,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = feeFilter == 0,
                            onClick = { feeFilter = 0 },
                            text = {
                                Text(
                                    "Semua (${commitmentList.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                        Tab(
                            selected = feeFilter == 1,
                            onClick = { feeFilter = 1 },
                            text = {
                                Text(
                                    "Belum Bayar ($pendingCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                        Tab(
                            selected = feeFilter == 2,
                            onClick = { feeFilter = 2 },
                            text = {
                                Text(
                                    "Selesai ($settledCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                }
            }
        }

        // ── Interactive List of Fees from Room ────────────────────────────────
        if (filteredFees.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (commitmentList.isEmpty()) "Tiada yuran untuk bulan ini" else "Tiada rekod yuran dalam kategori ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredFees, key = { it.id }) { fee ->
                val isPaid = fee.isPaid
                val badgeBg by animateColorAsState(
                    targetValue = if (isPaid) SlotifyColors.ScienceGreen.copy(alpha = 0.15f) else SlotifyColors.MathAmber.copy(
                        alpha = 0.15f
                    ),
                    label = "feeBadge"
                )
                val badgeText = if (isPaid) SlotifyColors.ScienceGreen else SlotifyColors.MathAmber

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 14.dp)
                        .background(SlotifyColors.SurfaceCard, SlotifyShapes.Medium)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category emoji badge
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(badgeBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fee.categoryEmoji,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = fee.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = fee.dueDateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotifyColors.TextSecondary
                                    )
                                    Text(
                                        text = " • ${fee.childTarget}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SlotifyColors.KpmBlue
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = Money.format(fee.amountSen),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )

                                // Toggle button: taps persist to Room via upsertCommitmentFee
                                Surface(
                                    shape = SlotifyShapes.XSmall,
                                    color = badgeBg,
                                    modifier = Modifier.clickable { viewModel.togglePaid(fee) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                                            contentDescription = null,
                                            tint = badgeText,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (isPaid) strings.paidLabel else strings.pendingLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeText
                                        )
                                    }
                                }
                            }

                            // Delete fee button
                            IconButton(
                                onClick = { viewModel.deleteFee(fee) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Padam Yuran",
                                    tint = SlotifyColors.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Notification Action Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 14.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Medium)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = SlotifyColors.KokuPurple,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.testNotificationTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                text = "Uji notifikasi peringatan yuran & jadual",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val unpaid = commitmentList.filter { !it.isPaid }
                            SlotifyNotificationManager.showTestNotification(
                                context,
                                "Slotify: Peringatan Yuran Bulanan",
                                if (unpaid.isEmpty()) "Semua yuran bulan ini telah dijelaskan. ✓"
                                else unpaid.joinToString("\n") {
                                    "${it.title} (${Money.format(it.amountSen)}) • ${it.dueDateLabel}"
                                }
                            )
                            notificationSentSnack = true
                        },
                        shape = SlotifyShapes.Small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlotifyColors.KokuPurple
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "Uji Sekarang",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // FAB clearance spacer
        item {
            Spacer(Modifier.height(115.dp))
        }
    }

    // Dialogs
    if (showAddFeeDialog) {
        AddCommitmentFeeDialog(
            billingMonth = billingMonth,
            childOptions = allStudents.map { it.nickname } + "Semua Anak",
            onDismissRequest = { showAddFeeDialog = false },
            onSaveFee = { newFee ->
                viewModel.saveFee(newFee)
                showAddFeeDialog = false
            }
        )
    }

    if (showTakwimDialog) {
        TakwimCalendarDialog(
            onDismissRequest = { showTakwimDialog = false }
        )
    }
}

@Composable
fun AddCommitmentFeeDialog(
    billingMonth: String,
    childOptions: List<String>,
    onDismissRequest: () -> Unit,
    onSaveFee: (CommitmentFeeEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedChild by remember { mutableStateOf(childOptions.firstOrNull() ?: "Semua Anak") }
    var selectedCategoryEmoji by remember { mutableStateOf("🚌") }
    var dueDateLabel by remember { mutableStateOf("1hb Setiap Bulan") }

    val children = childOptions
    val emojis = listOf("🚌", "🏫", "📋", "🏊‍♂️", "🥋", "📚", "🎨", "💰")
    val duePresets = listOf("1hb Setiap Bulan", "5hb Setiap Bulan", "10hb Setiap Bulan", "Hujung Bulan", "Sekali Bayar")

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SlotifyColors.CanvasBase,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tambah Komitmen Yuran",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlotifyColors.TextPrimary
                )

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Yuran / Komitmen") },
                    placeholder = { Text("Contoh: Yuran Kelas Renang") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Jumlah (RM)") },
                    placeholder = { Text("Contoh: 120.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Target child selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Penerima / Anak:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlotifyColors.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(children) { child ->
                            FilterChip(
                                selected = selectedChild == child,
                                onClick = { selectedChild = child },
                                label = { Text(child, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Category Emoji selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Kategori Ikon:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlotifyColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = selectedCategoryEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) SlotifyColors.KpmBlue.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { selectedCategoryEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Due date preset selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Tarikh Bayaran:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlotifyColors.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(duePresets) { due ->
                            FilterChip(
                                selected = dueDateLabel == due,
                                onClick = { dueDateLabel = due },
                                label = { Text(due, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amountSen = Money.parse(amountText) ?: 0L
                            if (title.isNotBlank() && amountSen > 0) {
                                val seriesId = UUID.randomUUID().toString()
                                val newFee = CommitmentFeeEntity(
                                    id = FeeRepository.feeId(seriesId, billingMonth),
                                    title = title.trim(),
                                    childTarget = selectedChild,
                                    amountSen = amountSen,
                                    dueDateLabel = dueDateLabel,
                                    billingMonth = billingMonth,
                                    isPaid = false,
                                    categoryEmoji = selectedCategoryEmoji,
                                    seriesId = seriesId,
                                    isRecurring = FeeRepository.looksRecurring(dueDateLabel)
                                )
                                onSaveFee(newFee)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmBlue)
                    ) {
                        Text("Simpan Yuran")
                    }
                }
            }
        }
    }
}
