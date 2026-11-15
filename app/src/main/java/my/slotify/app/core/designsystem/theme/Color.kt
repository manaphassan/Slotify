package my.slotify.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    LIGHT,
    DARK,
    NORD
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.LIGHT }
val LocalThemeIsDark = compositionLocalOf { false }

/**
 * Official Nord Palette Specification (Arctic Ice Studio)
 * 16 curated color values organized into four distinct palettes:
 * - Polar Night (Nord 0 - 3): Deep arctic backgrounds & dark card surfaces
 * - Snow Storm (Nord 4 - 6): Crisp snow whites & high-contrast text
 * - Frost (Nord 7 - 10): Frozen turquoise, glacial ice & arctic river blues
 * - Aurora (Nord 11 - 15): Vivid northern light accents (red, orange, yellow, green, purple)
 * https://www.nordtheme.com
 */
object NordColors {
    // Polar Night (Dark Canvas & Surfaces)
    val Nord0  = Color(0xFF2E3440) // Polar Night 0: Deepest canvas base
    val Nord1  = Color(0xFF3B4252) // Polar Night 1: Elevated card container
    val Nord2  = Color(0xFF434C5E) // Polar Night 2: Selection / active card
    val Nord3  = Color(0xFF4C566A) // Polar Night 3: Muted borders / comments / dividers

    // Snow Storm (Text & Light Highlights)
    val Nord4  = Color(0xFFD8DEE9) // Snow Storm 4: Secondary muted text
    val Nord5  = Color(0xFFE5E9F0) // Snow Storm 5: Elevated white / bright label
    val Nord6  = Color(0xFFECEFF4) // Snow Storm 6: Purest snow white primary text

    // Frost (Arctic Signature Accents)
    val Nord7  = Color(0xFF8FBCBB) // Frost 7: Calming sea-green / icy teal
    val Nord8  = Color(0xFF88C0D0) // Frost 8: Glacial ice cyan (Primary Hero Accent)
    val Nord9  = Color(0xFF81A1C1) // Frost 9: Arctic steel blue
    val Nord10 = Color(0xFF5E81AC) // Frost 10: Deep royal fjord blue

    // Aurora (Functional Accents)
    val Nord11 = Color(0xFFBF616A) // Aurora 11: Nordic soft red (danger / alert / English)
    val Nord12 = Color(0xFFD08770) // Aurora 12: Nordic soft orange (warning / attire / Sejarah)
    val Nord13 = Color(0xFFEBCB8B) // Aurora 13: Nordic golden yellow (Star coins / Math)
    val Nord14 = Color(0xFFA3BE8C) // Aurora 14: Nordic aurora green (success / arrival / Science)
    val Nord15 = Color(0xFFB48EAD) // Aurora 15: Nordic soft purple (koku / Islamic studies / rewards)

    // Nord Neumorphic Specular Highlight & Deep Drop Shadow
    val NordHighlight = Color(0xFF434C5E) // Specular glow in polar night
    val NordShadow    = Color(0xFF434C5E) // Hairline outline (Nord2)

    fun getSubjectColor(code: String): Color = when (code.uppercase()) {
        "BM" -> Nord9
        "BI" -> Nord11
        "MT" -> Nord13
        "SN", "AWAL_SAINS" -> Nord14
        "PI", "TAWHID", "JAWI", "BA", "TAHFIZ", "SIRAH" -> Nord7
        "SEJ" -> Nord12
        "IQRA" -> Nord15
        "RBT" -> Nord10
        else -> Nord8
    }
}

/**
 * Slotify minimal colour system
 * - Neutral canvas with white (light) or near-black (dark) cards separated by a hairline outline
 * - One accent: KpmSkyBlue; status colours (ActionAmber / SuccessMint / StarGold) are used sparingly
 */
object SlotifyColors {
    // Static base colors for Light Mode
    val LightCanvasBase       = Color(0xFFF6F7F9) // Neutral canvas
    val LightSurfaceCard      = Color(0xFFFFFFFF) // White card
    val LightHighlightLight   = Color(0xFFFFFFFF) // Specular Light Glow (Top-Left)
    val LightDarkShadow       = Color(0xFFE6E8EC) // Hairline outline
    val LightTextPrimary      = Color(0xFF111827) // Near-black
    val LightTextSecondary    = Color(0xFF6B7280) // Neutral grey
    val LightBorderSubtle     = Color(0xFFE6E8EC) // 1dp outline / divider

    // Static base colors for Dark Mode
    val CanvasBaseDark        = Color(0xFF0E1014) // Neutral dark canvas
    val SurfaceCardDark       = Color(0xFF171A1F) // Dark card
    val LightHighlightDark    = Color(0xFF283244) // Specular top-left glow in dark
    val DarkShadowDark        = Color(0xFF262A31) // Hairline outline in dark
    val DarkTextPrimary       = Color(0xFFF3F4F6) // Off-white
    val DarkTextSecondary     = Color(0xFF9CA3AF) // Neutral grey
    val DarkBorderSubtle      = Color(0xFF262A31) // Dark outline / divider

    // Static base colors for Nord Mode
    val CanvasBaseNord        = NordColors.Nord0
    val SurfaceCardNord       = NordColors.Nord1
    val HighlightNord         = NordColors.NordHighlight
    val DarkShadowNord        = NordColors.NordShadow
    val TextPrimaryNord       = NordColors.Nord6
    val TextSecondaryNord     = NordColors.Nord4
    val BorderSubtleNord      = NordColors.Nord2

    // Dynamic 60% Dominant Base (Adapts dynamically to Light / Dark / Nord Theme)
    val CanvasBase: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> CanvasBaseNord
            AppThemeMode.DARK -> CanvasBaseDark
            AppThemeMode.LIGHT -> LightCanvasBase
        }

    val BackgroundLight: Color
        @Composable get() = CanvasBase

    val SurfaceCard: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> SurfaceCardNord
            AppThemeMode.DARK -> SurfaceCardDark
            AppThemeMode.LIGHT -> LightSurfaceCard
        }

    val LightHighlight: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> HighlightNord
            AppThemeMode.DARK -> LightHighlightDark
            AppThemeMode.LIGHT -> LightHighlightLight
        }

    val DarkShadow: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> DarkShadowNord
            AppThemeMode.DARK -> DarkShadowDark
            AppThemeMode.LIGHT -> LightDarkShadow
        }

    // Default static colors (for static theme scheme initialization)
    val DefaultKpmSkyBlue     = Color(0xFF0284C7)
    val DefaultActionAmber    = Color(0xFFD97706)
    val DefaultStarGold       = Color(0xFFF59E0B)
    val DefaultSuccessMint    = Color(0xFF059669)
    val DefaultKokuPurple     = Color(0xFF7C3AED)

    // Dynamic 30% Structural Secondary
    val KpmSkyBlue: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord8
            else -> DefaultKpmSkyBlue
        }

    val KpmBlue: Color
        @Composable get() = KpmSkyBlue

    val TextPrimary: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> TextPrimaryNord
            AppThemeMode.DARK -> DarkTextPrimary
            AppThemeMode.LIGHT -> LightTextPrimary
        }

    val TextSecondary: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> TextSecondaryNord
            AppThemeMode.DARK -> DarkTextSecondary
            AppThemeMode.LIGHT -> LightTextSecondary
        }

    val TextTertiary: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD  -> NordColors.Nord4  // #D8DEE9 — contrast ~8.5:1 on Nord0 (WCAG AAA ✓)
            AppThemeMode.DARK  -> Color(0xFF6B7280)
            AppThemeMode.LIGHT -> Color(0xFF9CA3AF)
        }

    val BorderSubtle: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> BorderSubtleNord
            AppThemeMode.DARK -> DarkBorderSubtle
            AppThemeMode.LIGHT -> LightBorderSubtle
        }

    // 10% — Radiant Accent
    val ActionAmber: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord12
            else -> DefaultActionAmber
        }

    val MathAmber: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord13
            else -> DefaultStarGold
        }

    val StarGold: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord13
            else -> DefaultStarGold
        }

    val SuccessMint: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord14
            else -> DefaultSuccessMint
        }

    val KokuPurple: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord15
            else -> DefaultKokuPurple
        }

    // Subject Badges
    val ScienceGreen: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord14
            else -> Color(0xFF10B981)
        }

    val IslamTeal: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord7
            else -> Color(0xFF0D9488)
        }

    val EnglishRed: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord11
            else -> Color(0xFFEF4444)
        }

    val SejarahOrange: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord12
            else -> Color(0xFFEA580C)
        }

    val RbtBrown: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord10
            else -> Color(0xFFB45309)
        }

    // Logistics & Vehicles
    val VanPrimaryBlue: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord9
            else -> Color(0xFF2563EB)
        }

    val VanSecondaryGold: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord13
            else -> Color(0xFFD97706)
        }

    val ParentGreen: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord14
            else -> Color(0xFF059669)
        }

    // Static MyStudyLife-Inspired Subject Palette for Static Models
    val SubjectBm             = Color(0xFF0284C7) // Sky Blue
    val SubjectBi             = Color(0xFFE11D48) // Rose Red
    val SubjectMt             = Color(0xFFF59E0B) // Amber Gold
    val SubjectSn             = Color(0xFF10B981) // Emerald Green
    val SubjectPi             = Color(0xFF0D9488) // Teal Cyan
    val SubjectSej            = Color(0xFFEA580C) // Warm Ochre
    val SubjectIqra           = Color(0xFF8B5CF6) // Violet Purple
    val SubjectRbt            = Color(0xFF78350F) // Bronze Brown
    val SubjectDefault        = Color(0xFF64748B) // Slate

    fun getSubjectColor(code: String, themeMode: AppThemeMode = AppThemeMode.LIGHT): Color {
        if (themeMode == AppThemeMode.NORD) {
            return NordColors.getSubjectColor(code)
        }
        return when (code.uppercase()) {
            "BM" -> SubjectBm
            "BI" -> SubjectBi
            "MT" -> SubjectMt
            "SN", "AWAL_SAINS" -> SubjectSn
            "PI", "TAWHID", "JAWI", "BA", "TAHFIZ", "SIRAH" -> SubjectPi
            "SEJ" -> SubjectSej
            "IQRA" -> SubjectIqra
            "RBT" -> SubjectRbt
            else -> SubjectDefault
        }
    }

    /**
     * Fix 4.1: @Composable accessor that reads LocalAppThemeMode so Nord subject
     * badge colors are resolved correctly at the call-site instead of defaulting to Light.
     * Use this instead of getSubjectColor() everywhere in UI composables.
     */
    @Composable
    fun subjectColor(code: String): Color {
        val mode = LocalAppThemeMode.current
        return getSubjectColor(code, mode)
    }
}
