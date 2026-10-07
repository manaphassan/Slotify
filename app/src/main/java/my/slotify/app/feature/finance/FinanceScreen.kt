package my.slotify.app.feature.finance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Finance screen — Room-backed ──────────────────────────────────────────────

@Composable
fun FinanceScreen(
    selectedStudent: StudentEntity?,
    dao: SlotifyDao? = null
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

    // ── Live Room flows ───────────────────────────────────────────────────────
    val commitmentList: List<CommitmentFeeEntity> by remember(billingMonth, dao) {
        dao?.getFeesForMonth(billingMonth) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val todaySpendingRow: DailySpendingEntity? by remember(selectedStudent?.id, todayKey, dao) {
        val sid = selectedStudent?.id ?: return@remember kotlinx.coroutines.flow.flowOf(null)
        dao?.getDailySpendingFlow(sid, todayKey) ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)

    // Derive spent from DB (fallback to 0.0 on first launch)
    val spentAmount = todaySpendingRow?.spentAmountRm ?: 0.0
    val budget = selectedStudent?.dailyCanteenBudget ?: 5.00
    val saved = (budget - spentAmount).coerceAtLeast(0.0)

    // ── Commitment summary ────────────────────────────────────────────────────
    val totalAmount = commitmentList.sumOf { it.amountRm }
    val paidAmount  = commitmentList.filter { it.isPaid }.sumOf { it.amountRm }
    val settledCount = commitmentList.count { it.isPaid }
    val progressFraction = if (totalAmount > 0) (paidAmount / totalAmount).toFloat() else 0f

    var notificationSentSnack by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(top = 4.dp)) {
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
                                text = "+ RM ${String.format("%.2f", saved)}",
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

        // Monthly Commitment Header & Progress Overview
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
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (progressFraction >= 1f) SlotifyColors.ScienceGreen else SlotifyColors.KpmBlue
                        )
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
                }
            }
        }

        // ── Interactive List of Fees from Room ────────────────────────────────
        if (commitmentList.isEmpty()) {
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
                            text = "Tiada yuran untuk bulan ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(commitmentList, key = { it.id }) { fee ->
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
}
