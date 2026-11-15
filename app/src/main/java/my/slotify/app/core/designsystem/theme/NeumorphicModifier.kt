package my.slotify.app.core.designsystem.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NeumorphicElevation {
    FLAT,
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Card outline. Historically a dual-shadow neumorphic effect; the minimal theme draws a single
 * hairline instead (on top of the content, so it shows over the `.background()` that follows).
 * The signature is unchanged so existing call sites keep working.
 */
@Composable
fun Modifier.neumorphic(
    elevation: NeumorphicElevation = NeumorphicElevation.MEDIUM,
    cornerRadius: Dp = 16.dp,
    @Suppress("UNUSED_PARAMETER") lightColor: Color = Color.Unspecified,
    darkColor: Color = SlotifyColors.BorderSubtle,
    @Suppress("UNUSED_PARAMETER") isSunken: Boolean = false
): Modifier = if (elevation == NeumorphicElevation.FLAT) this else this.drawWithContent {
    drawContent()
    val stroke = 1.dp.toPx()
    val inset = stroke / 2f
    val radius = (cornerRadius.toPx() - inset).coerceAtLeast(0f)
    drawRoundRect(
        color = darkColor,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = stroke)
    )
}

@Composable
fun Modifier.neumorphicClickable(
    cornerRadius: Dp = 16.dp,
    onClick: () -> Unit
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .clickable(onClick = onClick)
    .neumorphic(cornerRadius = cornerRadius)
