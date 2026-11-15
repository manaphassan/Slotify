package my.slotify.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import my.slotify.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.slotify_gms_fonts_certs
)

private val nunitoFont = GoogleFont("Nunito")
private val spaceGroteskFont = GoogleFont("Space Grotesk")
private val interFont  = GoogleFont("Inter")

val NunitoFamily = FontFamily(
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

/** Headings, titles and class names (style guide: Space Grotesk 500–700). */
val SpaceGroteskFamily = FontFamily(
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.Bold),
)

val InterFamily = FontFamily(
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Bold),
)

private fun heading(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) =
    TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

private fun body(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) =
    TextStyle(fontFamily = InterFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

/**
 * Space Grotesk for headings, Inter for interface text. Sizes follow the style guide, scaled
 * slightly down for the denser cards (e.g. screen title 30 instead of 32).
 */
val SlotifyTypography = Typography(
    displayLarge   = heading(FontWeight.Bold, 30, 36, -0.5),     // Screen title ("Hari Ini")
    displayMedium  = heading(FontWeight.Bold, 26, 32, -0.3),
    displaySmall   = heading(FontWeight.SemiBold, 22, 28, -0.2),  // Section heading ("Jadual hari ini")
    headlineLarge  = heading(FontWeight.SemiBold, 22, 28, -0.2),
    headlineMedium = heading(FontWeight.SemiBold, 19, 24),        // Hero class name
    headlineSmall  = heading(FontWeight.SemiBold, 17, 22),        // Card heading
    titleLarge     = heading(FontWeight.SemiBold, 16, 22),
    titleMedium    = heading(FontWeight.SemiBold, 15, 20),
    titleSmall     = body(FontWeight.SemiBold, 13, 18),
    bodyLarge      = body(FontWeight.Normal, 15, 22),
    bodyMedium     = body(FontWeight.Normal, 13, 19),
    bodySmall      = body(FontWeight.Normal, 12, 17),
    labelLarge     = body(FontWeight.SemiBold, 13, 18),           // Buttons
    labelMedium    = body(FontWeight.Medium, 12, 16),             // Metadata (time, room)
    labelSmall     = body(FontWeight.SemiBold, 11, 14, 0.4),      // Small caps labels ("KELAS SEMASA")
)
