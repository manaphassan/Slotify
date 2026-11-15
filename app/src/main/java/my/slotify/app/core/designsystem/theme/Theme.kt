package my.slotify.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
    primary = SlotifyColors.DefaultKpmSkyBlue,
    onPrimary = SlotifyColors.LightHighlightLight,
    secondary = SlotifyColors.DefaultActionAmber,
    onSecondary = SlotifyColors.LightHighlightLight,
    tertiary = SlotifyColors.DefaultSuccessMint,
    background = SlotifyColors.LightCanvasBase,
    surface = SlotifyColors.LightSurfaceCard,
    surfaceVariant = SlotifyColors.LightCanvasBase,
    outline = SlotifyColors.LightBorderSubtle,
    outlineVariant = SlotifyColors.LightBorderSubtle,
    onBackground = SlotifyColors.LightTextPrimary,
    onSurface = SlotifyColors.LightTextPrimary,
    onSurfaceVariant = SlotifyColors.LightTextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = SlotifyColors.DefaultKpmSkyBlue,
    onPrimary = SlotifyColors.DarkTextPrimary,
    secondary = SlotifyColors.DefaultActionAmber,
    background = SlotifyColors.CanvasBaseDark,
    surface = SlotifyColors.SurfaceCardDark,
    surfaceVariant = SlotifyColors.CanvasBaseDark,
    outline = SlotifyColors.DarkBorderSubtle,
    outlineVariant = SlotifyColors.DarkBorderSubtle,
    onBackground = SlotifyColors.DarkTextPrimary,
    onSurface = SlotifyColors.DarkTextPrimary,
    onSurfaceVariant = SlotifyColors.DarkTextSecondary
)

private val NordColorScheme = darkColorScheme(
    primary = NordColors.Nord8,           // Frost Glacial Ice Cyan
    onPrimary = NordColors.Nord0,           // Dark Polar Night on primary
    primaryContainer = NordColors.Nord10,          // Deep Fjord Blue
    onPrimaryContainer = NordColors.Nord6,           // Crisp Snow White
    secondary = NordColors.Nord12,          // Aurora Nordic Orange
    onSecondary = NordColors.Nord0,           // Dark Polar Night
    secondaryContainer = NordColors.Nord1,           // Elevated Polar Night
    onSecondaryContainer = NordColors.Nord5,           // Bright Snow Storm
    tertiary = NordColors.Nord14,          // Aurora Green
    onTertiary = NordColors.Nord0,
    tertiaryContainer = NordColors.Nord2,
    onTertiaryContainer = NordColors.Nord6,
    background = NordColors.Nord0,           // Polar Night 0: #2E3440
    surface = NordColors.Nord1,           // Polar Night 1: #3B4252
    surfaceVariant = NordColors.Nord2,           // Polar Night 2: #434C5E
    onBackground = NordColors.Nord6,           // Snow Storm 6: #ECEFF4
    onSurface = NordColors.Nord6,           // Snow Storm 6: #ECEFF4
    onSurfaceVariant = NordColors.Nord4,           // Snow Storm 4: #D8DEE9
    outline = NordColors.Nord3            // Polar Night 3: #4C566A
)

@Composable
fun SlotifyTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    darkTheme: Boolean = (themeMode == AppThemeMode.DARK || themeMode == AppThemeMode.NORD),
    content: @Composable () -> Unit
) {
    val effectiveThemeMode = when {
        themeMode != AppThemeMode.LIGHT -> themeMode
        darkTheme -> AppThemeMode.DARK
        else -> AppThemeMode.LIGHT
    }

    val colorScheme = when (effectiveThemeMode) {
        AppThemeMode.NORD -> NordColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalAppThemeMode provides effectiveThemeMode,
        LocalThemeIsDark provides (effectiveThemeMode != AppThemeMode.LIGHT)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SlotifyTypography,
            shapes = SlotifyM3Shapes,
            content = content
        )
    }
}
