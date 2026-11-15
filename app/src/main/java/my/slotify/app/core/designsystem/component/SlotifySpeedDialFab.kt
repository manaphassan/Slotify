package my.slotify.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.slotify.app.Screen
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.designsystem.theme.AppThemeMode
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.NordColors
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings

/**
 * Tactile Neumorphic Speed Dial Floating Action Button
 * Replaces the traditional bottom navigation bar on compact screens.
 */
@Composable
fun SlotifySpeedDialFab(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    appThemeMode: AppThemeMode = if (isDarkMode) AppThemeMode.DARK else AppThemeMode.LIGHT
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "FabRotation"
    )

    Column(
        modifier = modifier.padding(bottom = 6.dp, end = 6.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Speed dial menu items (revealed when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(180)),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(150))
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Profile / Rewards
                SpeedDialActionItem(
                    label = strings.profile,
                    icon = Icons.Default.Person,
                    iconTint = SlotifyColors.KpmSkyBlue,
                    isActive = currentScreen == Screen.PROFILES,
                    onClick = { onNavigate(Screen.PROFILES) }
                )

                // 2. Logistics & Map
                SpeedDialActionItem(
                    label = strings.logisticsMap,
                    icon = Icons.Default.DirectionsBus,
                    iconTint = SlotifyColors.SuccessMint,
                    isActive = currentScreen == Screen.LOGISTICS,
                    onClick = { onNavigate(Screen.LOGISTICS) }
                )

                // 3. Timetable
                SpeedDialActionItem(
                    label = strings.timetable,
                    icon = Icons.Default.CalendarToday,
                    iconTint = SlotifyColors.ActionAmber,
                    isActive = currentScreen == Screen.TIMETABLE,
                    onClick = { onNavigate(Screen.TIMETABLE) }
                )

                // 4. Today
                SpeedDialActionItem(
                    label = strings.today,
                    icon = Icons.Default.Home,
                    iconTint = SlotifyColors.KpmSkyBlue,
                    isActive = currentScreen == Screen.DASHBOARD,
                    onClick = { onNavigate(Screen.DASHBOARD) }
                )
            }
        }

        // Main Primary Floating Action Button
        Box(
            modifier = Modifier
                .size(50.dp)
                .neumorphic(
                    elevation = if (isExpanded) NeumorphicElevation.LOW else NeumorphicElevation.HIGH,
                    cornerRadius = 25.dp
                )
                .background(
                    if (isExpanded) SlotifyColors.ActionAmber else SlotifyColors.KpmSkyBlue,
                    CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onToggleExpand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Widgets,
                contentDescription = strings.shellUi.quickActionsMenu,
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun SpeedDialActionItem(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 4.dp)
    ) {
        // Label badge
        Box(
            modifier = Modifier
                .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 12.dp)
                .background(
                    if (isActive) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) Color.White else SlotifyColors.TextPrimary
            )
        }

        Spacer(Modifier.width(8.dp))

        // Circular Icon button
        Box(
            modifier = Modifier
                .size(40.dp)
                .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 20.dp)
                .background(
                    if (isActive) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard,
                    CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
