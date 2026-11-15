package my.slotify.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object SlotifyShapes {
    val XSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp) // Button radius
    val Large = RoundedCornerShape(16.dp) // Card radius
    val XLarge = RoundedCornerShape(24.dp)
    val Full = RoundedCornerShape(50.dp)

    val XSmallDp = 4.dp
    val SmallDp = 8.dp
    val MediumDp = 12.dp
    val LargeDp = 16.dp
    val XLargeDp = 24.dp
    val FullDp = 50.dp
}

val SlotifyM3Shapes = Shapes(
    extraSmall = SlotifyShapes.XSmall,
    small = SlotifyShapes.Small,
    medium = SlotifyShapes.Medium,
    large = SlotifyShapes.Large,
    extraLarge = SlotifyShapes.XLarge,
)
