package my.slotify.app.feature.rewards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.security.PinManager

data class RewardCatalogItem(
    val title: String,
    val description: String,
    val pointsCost: Int,
    val iconEmoji: String
)

@Composable
fun RewardVaultScreen(
    selectedStudent: StudentEntity?,
    onRedeemReward: (RewardCatalogItem) -> Unit,
    onBonusStarsAwarded: (Int) -> Unit = {}
) {
    var chestOpened by remember { mutableStateOf(false) }
    var showChestDialog by remember { mutableStateOf(false) }

    var pendingRewardToRedeem by remember { mutableStateOf<RewardCatalogItem?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf<RewardCatalogItem?>(null) }

    val rewardItems = remember {
        listOf(
            RewardCatalogItem("Main Roblox / iPad 1 Jam", "Ganjaran bermain peranti dengan had waktu berjadual", 50, "🎮"),
            RewardCatalogItem("Beli Mainan / Hot Wheels", "Tebus mainan kegemaran pilihan anak di kedai", 200, "🏎️"),
            RewardCatalogItem("Keluar Makan Ais Krim", "Ais krim FamilyMart / Tealive hujung minggu", 30, "🍦"),
            RewardCatalogItem("Tidur Lewat 30 Minit", "Kelonggaran waktu tidur hari Sabtu / cuti sekolah", 20, "🌙")
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "chestPulse")
    val chestScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!chestOpened) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chestPulseScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlotifyColors.CanvasBase)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Star Balance Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 20.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${strings.starTabungTitle} (${selectedStudent?.nickname ?: "Anak"})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = SlotifyColors.TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = SlotifyColors.MathAmber,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${selectedStudent?.starCoinsBalance ?: 0} ⭐",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                    Text(
                        text = strings.starTabungSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = SlotifyColors.TextSecondary
                    )
                }
            }
        }

        // Mystery Star Chest Card (Daily Bonus)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                    .clip(SlotifyShapes.Large)
                    .clickable(enabled = !chestOpened) {
                        chestOpened = true
                        onBonusStarsAwarded(15)
                        showChestDialog = true
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(if (!chestOpened) chestScale else 1f)
                                .background(
                                    if (chestOpened) SlotifyColors.TextTertiary.copy(alpha = 0.15f)
                                    else SlotifyColors.MathAmber.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chestOpened) "🎁" else "📦",
                                fontSize = 26.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.mysteryChestTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SlotifyColors.TextPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = if (chestOpened) SlotifyColors.ScienceGreen.copy(alpha = 0.15f) else SlotifyColors.MathAmber.copy(alpha = 0.15f),
                                    shape = SlotifyShapes.XSmall
                                ) {
                                    Text(
                                        text = if (chestOpened) "Claimed" else "+15 ⭐",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (chestOpened) SlotifyColors.ScienceGreen else SlotifyColors.MathAmber,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (chestOpened) strings.mysteryChestOpened else strings.mysteryChestSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }

                    if (!chestOpened) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SlotifyColors.KpmBlue
                        )
                    }
                }
            }
        }

        // Section Title: Reward Catalog
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.rewardCatalogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SlotifyColors.TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = SlotifyColors.TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.parentApprovalRequired,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlotifyColors.TextTertiary
                    )
                }
            }
        }

        // Reward Catalog Items
        items(rewardItems) { item ->
            val canAfford = (selectedStudent?.starCoinsBalance ?: 0) >= item.pointsCost
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = NeumorphicElevation.MEDIUM, cornerRadius = 16.dp)
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
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
                        Text(text = item.iconEmoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SlotifyColors.TextPrimary
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = SlotifyColors.TextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (canAfford) {
                                pendingRewardToRedeem = item
                                showPinDialog = true
                            }
                        },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlotifyColors.KokuPurple,
                            disabledContainerColor = SlotifyColors.TextTertiary.copy(alpha = 0.2f),
                            disabledContentColor = SlotifyColors.TextTertiary
                        ),
                        shape = SlotifyShapes.Small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${item.pointsCost} ⭐",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Clearance spacer for floating action button
        item {
            Spacer(Modifier.height(70.dp))
        }
    }

    // Mystery Chest Celebration Dialog
    if (showChestDialog) {
        Dialog(onDismissRequest = { showChestDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                    .border(2.dp, SlotifyColors.MathAmber.copy(alpha = 0.4f), SlotifyShapes.XLarge)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🎉 🎁 🎉", fontSize = 32.sp)
                    Text(
                        text = "Peti Bintang Misteri Terbuka!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = "Hebat! Anda berjaya mengutip bonus harian +15 Bintang untuk tabung anda!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = SlotifyColors.TextSecondary
                    )
                    Text(
                        text = "+15 ⭐",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = SlotifyColors.MathAmber
                    )
                    Button(
                        onClick = { showChestDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.KpmBlue),
                        shape = SlotifyShapes.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Terima Kasih!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Parent PIN Dialog
    if (showPinDialog && pendingRewardToRedeem != null) {
        ParentPinModalDialog(
            targetReward = pendingRewardToRedeem!!,
            onDismiss = {
                showPinDialog = false
                pendingRewardToRedeem = null
            },
            onPinVerified = {
                val reward = pendingRewardToRedeem
                showPinDialog = false
                pendingRewardToRedeem = null
                if (reward != null) {
                    onRedeemReward(reward)
                    showSuccessDialog = reward
                }
            }
        )
    }

    // Redemption Success Dialog
    if (showSuccessDialog != null) {
        val reward = showSuccessDialog!!
        Dialog(onDismissRequest = { showSuccessDialog = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = reward.iconEmoji, fontSize = 36.sp)
                    Text(
                        text = strings.redemptionSuccessTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = strings.enjoyRewardDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = SlotifyColors.TextSecondary
                    )
                    Surface(
                        color = SlotifyColors.ScienceGreen.copy(alpha = 0.15f),
                        shape = SlotifyShapes.Small
                    ) {
                        Text(
                            text = "${reward.title} (-${reward.pointsCost} ⭐)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SlotifyColors.ScienceGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Button(
                        onClick = { showSuccessDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = SlotifyColors.ScienceGreen),
                        shape = SlotifyShapes.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selesai", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ParentPinModalDialog(
    targetReward: RewardCatalogItem,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    // Fix 1.1: 3-attempt lockout with 30-second countdown
    var attemptCount by remember { mutableStateOf(0) }
    var lockoutSeconds by remember { mutableStateOf(0) }
    val isLockedOut = lockoutSeconds > 0

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds -= 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = SlotifyColors.KpmBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.parentPinTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SlotifyColors.TextSecondary)
                    }
                }

                Text(
                    text = strings.parentPinSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = SlotifyColors.TextSecondary
                )

                // PIN indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (isFilled) SlotifyColors.KpmBlue else SlotifyColors.TextTertiary.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (isLockedOut) {
                    Text(
                        text = "🔒 Terlalu banyak cubaan. Cuba lagi dalam ${lockoutSeconds}s...",
                        color = SlotifyColors.EnglishRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                } else if (isError) {
                    Text(
                        text = strings.pinIncorrect,
                        color = SlotifyColors.EnglishRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Numeric Keypad 1-9, Backspace, 0, OK
                val keyRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in keyRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (key in row) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable {
                                            when (key) {
                                                "C" -> {
                                                    enteredPin = ""
                                                    isError = false
                                                }
                                                "⌫" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        isError = false
                                                    }
                                                }
                                                else -> {
                                                    if (!isLockedOut && enteredPin.length < 4) {
                                                        val next = enteredPin + key
                                                        enteredPin = next
                                                        if (next.length == 4) {
                                                            // Fix 1.1: Use PinManager (SHA-256) instead of hardcoded "1234"
                                                            if (PinManager.verifyPin(context, next)) {
                                                                attemptCount = 0
                                                                onPinVerified()
                                                            } else {
                                                                attemptCount += 1
                                                                isError = true
                                                                enteredPin = ""
                                                                if (attemptCount >= 3) {
                                                                    lockoutSeconds = 30
                                                                    attemptCount = 0
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    shape = SlotifyShapes.Small,
                                    color = SlotifyColors.CanvasBase
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = SlotifyColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
