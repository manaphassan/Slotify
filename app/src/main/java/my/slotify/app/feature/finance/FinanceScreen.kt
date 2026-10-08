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
import my.slotify.app.core.database.DailySpendingEntity
import my.slotify.app.core.database.SlotifyDao
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.notification.SlotifyNotificationManager
import my.slotify.app.core.takwim.KpmTakwimRepository
import my.slotify.app.feature.takwim.TakwimCalendarDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ── Finance screen — Room-backed with Tabung Maya, Fee Management & Takwim KPM ──

@Composable
fun FinanceScreen(
    selectedStudent: StudentEntity?,
    dao: SlotifyDao? = null,
    onRewardStars: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Billing month key: "2026-09"
    val billingMonth = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }
    // Today's date key: "2026-09-30"
    val todayKey = remember {
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val todayDate = remember { LocalDate.now() }

    // Dialog & UI states
    var showAddFeeDialog by remember { mutableStateOf(false) }
    var showTakwimDialog by remember { mutableStateOf(false) }
    var feeFilter by remember { mutableStateOf(0) } // 0: Semua, 1: Belum Bayar, 2: Selesai
    var celebrationMessage by remember { mutableStateOf<String?>(null) }
    var notificationSentSnack by remember { mutableStateOf(false) }

    // ── Live Room flows ───────────────────────────────────────────────────────
    val commitmentList: List<CommitmentFeeEntity> by remember(billingMonth, dao) {
        dao?.getFeesForMonth(billingMonth) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val todaySpendingRow: DailySpendingEntity? by remember(selectedStudent?.id, todayKey, dao) {
        val sid = selectedStudent?.id ?: return@remember kotlinx.coroutines.flow.flowOf(null)
        dao?.getDailySpendingFlow(sid, todayKey) ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)

    val allSpendings: List<DailySpendingEntity> by remember(selectedStudent?.id, dao) {
        val sid = selectedStudent?.id ?: return@remember kotlinx.coroutines.flow.flowOf(emptyList())
        dao?.getAllSpendingForStudent(sid) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    // Derive spent from DB (fallback to 0.0 on first launch)
    val spentAmount = todaySpendingRow?.spentAmountRm ?: 0.0
    val budget = selectedStudent?.dailyCanteenBudget ?: 5.00
    val todaySaved = (budget - spentAmount).coerceAtLeast(0.0)

    // Cumulative Tabung Maya savings
    val cumulativeSavings = remember(allSpendings, budget, todaySaved) {
        if (allSpendings.isEmpty()) {
            25.00 + todaySaved
        } else {
            allSpendings.sumOf { (budget - it.spentAmountRm).coerceAtLeast(0.0) }
        }
    }

    // ── Commitment summary ────────────────────────────────────────────────────
    val totalAmount = commitmentList.sumOf { it.amountRm }
    val paidAmount  = commitmentList.filter { it.isPaid }.sumOf { it.amountRm }
    val settledCount = commitmentList.count { it.isPaid }
    val pendingCount = commitmentList.size - settledCount
    val progressFraction = if (totalAmount > 0) (paidAmount / totalAmount).toFloat() else 0f

    val filteredFees = remember(commitmentList, feeFilter) {
        when (feeFilter) {
            1 -> commitmentList.filter { !it.isPaid }
            2 -> commitmentList.filter { it.isPaid }
            else -> commitmentList
        }
    }

    // Helper: persist spending change to Room
    fun updateSpending(newAmount: Double) {
        val sid = selectedStudent?.id ?: return
        scope.launch {
            dao?.upsertDailySpending(
                DailySpendingEntity(
                    studentId = sid,
                    dateKey   = todayKey,
                    spentAmountRm = newAmount.coerceAtLeast(0.0)
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    text = "${strings.canteenBudgetLabel}: RM ${String.format("%.2f", budget)}",
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
                            Text(text = strings.canteenSpentLabel, style = MaterialTheme.typography.bodySmall, color = SlotifyColors.TextSecondary)
                            Text(
                                text = "RM ${String.format("%.2f", spentAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SlotifyColors.EnglishRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = strings.savingsLabel, style = MaterialTheme.typography.bodySmall, color = SlotifyColors.TextSecondary)
                            Text(
                                text = "+ RM ${String.format("%.2f", todaySaved)}",
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
                                onClick = { updateSpending(spentAmount - 0.50) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = SlotifyColors.CanvasBase
                                )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp), tint = SlotifyColors.TextPrimary)
                            }
                            // Quick RM 3.00
                            OutlinedButton(
                                onClick = { updateSpending(3.00) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = SlotifyShapes.Small
                            ) {
                                Text("RM 3", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            // Quick RM 5.00
                            OutlinedButton(
                                onClick = { updateSpending(5.00) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = SlotifyShapes.Small
                            ) {
                                Text("RM 5", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            // + RM 0.50
                            FilledTonalIconButton(
                                onClick = { updateSpending(spentAmount + 0.50) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = SlotifyColors.CanvasBase
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp), tint = SlotifyColors.TextPrimary)
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
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            text = "RM ${String.format("%.2f", cumulativeSavings)}",
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
                            onClick = {
                                if (cumulativeSavings >= 10.0) {
                                    onRewardStars?.invoke(10)
                                    celebrationMessage = "🎉 Tahniah! RM 10 simpanan ditukarkan kepada +10 ⭐ Bintang untuk ${selectedStudent?.nickname ?: "Anak"}!"
                                } else {
                                    celebrationMessage = "Simpan sekurang-kurangnya RM 10 untuk menebus 10 ⭐ Bintang!"
                                }
                            },
                            shape = SlotifyShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ScienceGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("+10 ⭐ Bintang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // KPM Takwim Status Banner
        item {
            val upcomingHoliday = remember { KpmTakwimRepository.getUpcomingHolidays(todayDate, limit = 1).firstOrNull() }
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
                                text = "$settledCount / ${commitmentList.size} ${strings.paidLabel} (RM ${String.format("%.0f", paidAmount)} / RM ${String.format("%.0f", totalAmount)})",
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
                            text = { Text("Semua (${commitmentList.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = feeFilter == 1,
                            onClick = { feeFilter = 1 },
                            text = { Text("Belum Bayar ($pendingCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = feeFilter == 2,
                            onClick = { feeFilter = 2 },
                            text = { Text("Selesai ($settledCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
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
                    targetValue = if (isPaid) SlotifyColors.ScienceGreen.copy(alpha = 0.15f) else SlotifyColors.MathAmber.copy(alpha = 0.15f),
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
                                    text = "RM ${String.format("%.2f", fee.amountRm)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SlotifyColors.TextPrimary
                                )

                                // Toggle button: taps persist to Room via upsertCommitmentFee
                                Surface(
                                    shape = SlotifyShapes.XSmall,
                                    color = badgeBg,
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            dao?.upsertCommitmentFee(fee.copy(isPaid = !fee.isPaid))
                                        }
                                    }
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
                                onClick = {
                                    scope.launch {
                                        dao?.deleteCommitmentFee(fee)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
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
                            SlotifyNotificationManager.showTestNotification(
                                context,
                                "Slotify: Peringatan Yuran Bulanan",
                                "Yuran Van Sekolah A (RM 80) perlu dijelaskan sebelum 1hb!"
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
                        Text("Uji Sekarang", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // FAB clearance spacer
        item {
            Spacer(Modifier.height(70.dp))
        }
    }

    // Dialogs
    if (showAddFeeDialog) {
        AddCommitmentFeeDialog(
            billingMonth = billingMonth,
            onDismissRequest = { showAddFeeDialog = false },
            onSaveFee = { newFee ->
                scope.launch {
                    dao?.upsertCommitmentFee(newFee)
                    showAddFeeDialog = false
                }
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
    onDismissRequest: () -> Unit,
    onSaveFee: (CommitmentFeeEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedChild by remember { mutableStateOf("Adam") }
    var selectedCategoryEmoji by remember { mutableStateOf("🚌") }
    var dueDateLabel by remember { mutableStateOf("1hb Setiap Bulan") }

    val children = listOf("Adam", "Aisyah", "Danial", "Semua Anak")
    val emojis = listOf("🚌", "🏫", "📋", "🏊‍♂️", "🥋", "📚", "🎨", "💰")
    val duePresets = listOf("1hb Setiap Bulan", "5hb Setiap Bulan", "10hb Setiap Bulan", "Hujung Bulan")

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
                    Text("Penerima / Anak:", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                    Text("Kategori Ikon:", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                    Text("Tarikh Bayaran:", style = MaterialTheme.typography.labelMedium, color = SlotifyColors.TextSecondary)
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
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                val newFee = CommitmentFeeEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    childTarget = selectedChild,
                                    amountRm = amt,
                                    dueDateLabel = dueDateLabel,
                                    billingMonth = billingMonth,
                                    isPaid = false,
                                    categoryEmoji = selectedCategoryEmoji
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
