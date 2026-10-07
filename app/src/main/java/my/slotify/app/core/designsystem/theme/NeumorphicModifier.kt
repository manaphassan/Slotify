package my.slotify.app.core.designsystem.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NeumorphicElevation {
    FLAT,
    LOW,
    MEDIUM,
    HIGH
}

@Composable
fun Modifier.neumorphic(
    elevation: NeumorphicElevation = NeumorphicElevation.MEDIUM,
    cornerRadius: Dp = 16.dp,
    lightColor: Color = when (LocalAppThemeMode.current) {
        AppThemeMode.NORD -> NordColors.NordHighlight.copy(alpha = 0.6f)
        AppThemeMode.DARK -> SlotifyColors.LightHighlightDark.copy(alpha = 0.5f)
        AppThemeMode.LIGHT -> SlotifyColors.LightHighlightLight.copy(alpha = 0.9f)
    },
    darkColor: Color = when (LocalAppThemeMode.current) {
        AppThemeMode.NORD -> NordColors.NordShadow.copy(alpha = 0.95f)
        AppThemeMode.DARK -> SlotifyColors.DarkShadowDark.copy(alpha = 0.9f)
        AppThemeMode.LIGHT -> SlotifyColors.LightDarkShadow.copy(alpha = 0.8f)
    },
    isSunken: Boolean = false
): Modifier = this.drawBehind {
    val elevPx = when (elevation) {
        NeumorphicElevation.FLAT -> 0f
        NeumorphicElevation.LOW -> 4.dp.toPx()
        NeumorphicElevation.MEDIUM -> 8.dp.toPx()
        NeumorphicElevation.HIGH -> 14.dp.toPx()
    }
    
    if (elevPx == 0f) return@drawBehind

    val cornerPx = cornerRadius.toPx()

    if (!isSunken) {
        // Draw Dark Drop Shadow (Bottom-Right)
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = darkColor
                asFrameworkPaint().maskFilter = BlurMaskFilter(elevPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawRoundRect(
                left = elevPx / 2f,
                top = elevPx / 2f,
                right = size.width + elevPx / 2f,
                bottom = size.height + elevPx / 2f,
                radiusX = cornerPx,
                radiusY = cornerPx,
                paint = paint
            )
        }

        // Draw Light Specular Glow (Top-Left)
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = lightColor
                asFrameworkPaint().maskFilter = BlurMaskFilter(elevPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawRoundRect(
                left = -elevPx / 2f,
                top = -elevPx / 2f,
                right = size.width - elevPx / 2f,
                bottom = size.height - elevPx / 2f,
                radiusX = cornerPx,
                radiusY = cornerPx,
                paint = paint
            )
        }
    }
}

@Composable
fun Modifier.neumorphicClickable(
    cornerRadius: Dp = 16.dp,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = tween(durationMillis = 100),
        label = "NeumorphicPress"
    )

    return this
        .clip(RoundedCornerShape(cornerRadius))
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        .neumorphic(
            elevation = if (isPressed) NeumorphicElevation.LOW else NeumorphicElevation.MEDIUM,
            cornerRadius = cornerRadius,
            isSunken = isPressed
        )
}
