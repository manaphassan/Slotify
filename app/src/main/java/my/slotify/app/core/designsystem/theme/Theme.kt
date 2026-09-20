package my.slotify.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SlotifyColors.KpmBlue,
    onPrimary = SlotifyColors.LightHighlight,
    secondary = SlotifyColors.MathAmber,
    onSecondary = SlotifyColors.TextPrimary,
    tertiary = SlotifyColors.ScienceGreen,
    background = SlotifyColors.BackgroundLight,
    surface = SlotifyColors.SurfaceCard,
    onBackground = SlotifyColors.TextPrimary,
    onSurface = SlotifyColors.TextPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = SlotifyColors.KpmBlue,
    onPrimary = SlotifyColors.TextPrimary,
    secondary = SlotifyColors.MathAmber,
    background = SlotifyColors.BackgroundDark,
    surface = SlotifyColors.SurfaceDark,
    onBackground = SlotifyColors.LightHighlight,
    onSurface = SlotifyColors.LightHighlight
)

@Composable
fun SlotifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
