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
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val nunitoFont = GoogleFont("Nunito")
private val interFont  = GoogleFont("Inter")

val NunitoFamily = FontFamily(
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = nunitoFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val InterFamily = FontFamily(
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Bold),
)

val SlotifyTypography = Typography(
    displayLarge   = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    displayMedium  = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp),
    displaySmall   = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineLarge  = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    headlineMedium = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    headlineSmall  = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge     = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    titleMedium    = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    titleSmall     = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    bodySmall      = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelMedium    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.4.sp),
)
