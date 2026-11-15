package my.slotify.app.feature.rewards

import my.slotify.app.core.rewards.StarRewards
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import my.slotify.app.core.security.PinResult
import kotlinx.coroutines.launch
import my.slotify.app.core.database.StudentEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.slotify.app.core.di.SlotifyViewModelFactory
import my.slotify.app.core.time.rememberNow
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.security.PinManager
import my.slotify.app.core.designsystem.component.slotifySection
import androidx.compose.runtime.saveable.rememberSaveable

data class RewardCatalogItem(
    /** Stable, non-localised identifier (list key); [title]/[description] are display copy. */
    val id: String,
    val title: String,
    val description: String,
    val pointsCost: Int,
    val iconEmoji: String
)

@Composable
fun RewardVaultScreen(
    viewModel: RewardsViewModel = viewModel(factory = SlotifyViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val str = strings
    val selectedStudent = state.student
    // Persisted claim: reopening the tab or restarting the app cannot re-open the chest.
    val chestOpened = state.chestClaimed
    val now by rememberNow()
    LaunchedEffect(now.toLocalDate()) { viewModel.setToday(now.toLocalDate()) }
    var showChestDialog by remember { mutableStateOf(false) }

    var pendingRewardToRedeem by remember { mutableStateOf<RewardCatalogItem?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var redeemFailedMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf<RewardCatalogItem?>(null) }

    LaunchedEffect(event) {
        when (val e = event) {
            RewardsEvent.ChestOpened -> showChestDialog = true
            is RewardsEvent.Redeemed -> showSuccessDialog = e.item
            is RewardsEvent.RedeemFailed -> redeemFailedMessage = str.redeemNotEnoughStars(e.item.title)
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    val ss = str.shellUi
    val rewardItems = remember(ss) {
        listOf(
            RewardCatalogItem(
                "screen_time_1h",
                ss.rewardScreenTimeTitle,
                ss.rewardScreenTimeDesc,
                50,
                "🎮"
            ),
            RewardCatalogItem("toy", ss.rewardToyTitle, ss.rewardToyDesc, 200, "🏎️"),
            RewardCatalogItem("ice_cream", ss.rewardIceCreamTitle, ss.rewardIceCreamDesc, 30, "🍦"),
            RewardCatalogItem("late_bedtime_30m", ss.rewardLateBedtimeTitle, ss.rewardLateBedtimeDesc, 20, "🌙")
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

    var dailyOpen by rememberSaveable { mutableStateOf(true) }
    var catalogOpen by rememberSaveable { mutableStateOf(true) }
    var parentOpen by rememberSaveable { mutableStateOf(false) }

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
                        text = "${strings.starTabungTitle} (${selectedStudent?.nickname ?: strings.childFallback})",
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

        slotifySection(
            key = "daily",
            title = ss.groupDailyBonus,
            expanded = dailyOpen,
            onToggle = { dailyOpen = !dailyOpen }
        ) {
            // Mystery Star Chest Card (Daily Bonus)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = SlotifyShapes.LargeDp)
                        .background(SlotifyColors.SurfaceCard, SlotifyShapes.Large)
                        .clip(SlotifyShapes.Large)
                        .clickable(enabled = !chestOpened && selectedStudent != null) { viewModel.openChest() }
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
                                        color = if (chestOpened) SlotifyColors.ScienceGreen.copy(alpha = 0.15f) else SlotifyColors.MathAmber.copy(
                                            alpha = 0.15f
                                        ),
                                        shape = SlotifyShapes.XSmall
                                    ) {
                                        Text(
                                            text = if (chestOpened) ss.chestClaimedBadge else "+15 ⭐",
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
        }

        slotifySection(
            key = "catalog",
            title = ss.groupCatalog,
            expanded = catalogOpen,
            onToggle = { catalogOpen = !catalogOpen }
        ) {
            // Section Title: Reward Catalog
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
            items(rewardItems, key = { it.id }) { item ->
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
        }

        slotifySection(
            key = "parent",
            title = ss.groupParent,
            expanded = parentOpen,
            onToggle = { parentOpen = !parentOpen }
        ) {
            // Parent PIN management
            item {
                TextButton(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        str.changePinButton,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
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
                        text = strings.chestOpenedTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SlotifyColors.TextPrimary
                    )
                    Text(
                        text = strings.chestOpenedBody(StarRewards.DAILY_CHEST_STARS),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = SlotifyColors.TextSecondary
                    )
                    Text(
                        text = "+${StarRewards.DAILY_CHEST_STARS} ⭐",
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
                        Text(strings.thankYou, fontWeight = FontWeight.Bold)
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
                if (reward != null) viewModel.redeem(reward)
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(onDismiss = { showChangePinDialog = false })
    }

    redeemFailedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { redeemFailedMessage = null },
            confirmButton = { TextButton(onClick = { redeemFailedMessage = null }) { Text(str.ok) } },
            title = { Text(str.redeemFailedTitle) },
            text = { Text(msg) }
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
                        Text(strings.done, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


private enum class PinStep { VERIFY, CREATE, CONFIRM }

/**
 * Parent approval dialog. Creates the PIN on first use (no default PIN), otherwise verifies it.
 * Attempts and lockout are persisted by [PinManager], so dismissing the dialog does not reset them.
 */
@Composable
fun ParentPinModalDialog(
    targetReward: RewardCatalogItem,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
    val str = strings
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(if (PinManager.isPinSet(context)) PinStep.VERIFY else PinStep.CREATE) }
    var firstEntry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableIntStateOf(((PinManager.lockoutRemainingMs(context) + 999) / 1000).toInt()) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = ((PinManager.lockoutRemainingMs(context) + 999) / 1000).toInt()
        }
    }

    val title = when (step) {
        PinStep.VERIFY -> strings.parentPinTitle
        PinStep.CREATE -> str.pinCreateTitle
        PinStep.CONFIRM -> str.pinConfirmTitle
    }
    val subtitle = when (step) {
        PinStep.VERIFY -> "${strings.parentPinSubtitle}\n${targetReward.iconEmoji} ${targetReward.title} (-${targetReward.pointsCost} ⭐)"
        PinStep.CREATE -> str.pinCreateSubtitle
        PinStep.CONFIRM -> str.pinConfirmSubtitle
    }

    val pinIncorrectText = strings.pinIncorrect

    PinEntryDialog(
        title = title,
        subtitle = subtitle,
        message = if (lockoutSeconds > 0) str.pinLockedOut(lockoutSeconds.toLong()) else message,
        enabled = !busy && lockoutSeconds == 0,
        onDismiss = onDismiss,
        onPinComplete = { pin ->
            when (step) {
                PinStep.CREATE -> {
                    firstEntry = pin
                    message = null
                    step = PinStep.CONFIRM
                }

                PinStep.CONFIRM -> {
                    if (pin != firstEntry) {
                        message = str.pinMismatch
                        step = PinStep.CREATE
                    } else {
                        busy = true
                        scope.launch {
                            val r = withContext(Dispatchers.Default) { PinManager.setInitialPin(context, pin) }
                            busy = false
                            if (r == PinResult.Success) onPinVerified() else message = str.pinSaveFailed
                        }
                    }
                }

                PinStep.VERIFY -> {
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.Default) { PinManager.verifyPin(context, pin) }
                        busy = false
                        when (r) {
                            PinResult.Success -> onPinVerified()
                            is PinResult.Incorrect -> message =
                                "$pinIncorrectText (${str.pinAttemptsLeft(r.attemptsBeforeLockout)})"

                            is PinResult.LockedOut -> lockoutSeconds = ((r.remainingMs + 999) / 1000).toInt()
                            PinResult.NotSet -> step = PinStep.CREATE
                            else -> message = pinIncorrectText
                        }
                    }
                }
            }
        }
    )
}

private enum class ChangePinStep { CURRENT, NEW, CONFIRM }

/** Changes the parent PIN; requires the current PIN (or creates one if none exists). */
@Composable
fun ChangePinDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val str = strings
    val scope = rememberCoroutineScope()
    val hasPin = remember { PinManager.isPinSet(context) }
    var step by remember { mutableStateOf(if (hasPin) ChangePinStep.CURRENT else ChangePinStep.NEW) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    if (done) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text(str.ok) } },
            title = { Text(str.pinUpdatedTitle) },
            text = { Text(str.pinUpdatedBody) }
        )
        return
    }

    PinEntryDialog(
        title = when (step) {
            ChangePinStep.CURRENT -> str.pinCurrentTitle
            ChangePinStep.NEW -> str.pinNewTitle
            ChangePinStep.CONFIRM -> str.pinConfirmTitle
        },
        subtitle = when (step) {
            ChangePinStep.CURRENT -> str.pinCurrentSubtitle
            ChangePinStep.NEW -> str.pinNewSubtitle
            ChangePinStep.CONFIRM -> str.pinConfirmSubtitle
        },
        message = message,
        enabled = !busy,
        onDismiss = onDismiss,
        onPinComplete = { pin ->
            when (step) {
                ChangePinStep.CURRENT -> {
                    currentPin = pin; message = null; step = ChangePinStep.NEW
                }

                ChangePinStep.NEW -> {
                    newPin = pin; message = null; step = ChangePinStep.CONFIRM
                }

                ChangePinStep.CONFIRM -> {
                    if (pin != newPin) {
                        message = str.pinMismatch
                        step = ChangePinStep.NEW
                        return@PinEntryDialog
                    }
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.Default) {
                            if (hasPin) PinManager.changePin(context, currentPin, pin) else PinManager.setInitialPin(
                                context,
                                pin
                            )
                        }
                        busy = false
                        when (r) {
                            PinResult.Success -> done = true
                            is PinResult.LockedOut -> message =
                                str.pinLockedOut((r.remainingMs + 999) / 1000)

                            else -> {
                                message = str.pinCurrentWrong; step = ChangePinStep.CURRENT
                            }
                        }
                    }
                }
            }
        }
    )
}
