package my.slotify.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

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
 * Slotify colour system (Bordeaux)
 * - Warm off-white canvas, white cards with a hairline outline
 * - One brand accent (Bordeaux, `KpmSkyBlue`/`Brand`); timetable states use [StatusColors]
 */
object SlotifyColors {
    // Static base colors for Light Mode
    val LightCanvasBase       = Color(0xFFF8F6F7) // Warm off-white background
    val LightSurfaceCard      = Color(0xFFFFFFFF) // White card
    val LightHighlightLight   = Color(0xFFFFFFFF) // Specular Light Glow (Top-Left)
    val LightDarkShadow       = Color(0xFFEEE8EA) // Hairline outline
    val LightTextPrimary      = Color(0xFF120A0A) // Text primary
    val LightTextSecondary    = Color(0xFF75696D) // Text secondary
    val LightBorderSubtle     = Color(0xFFEEE8EA) // 1dp outline / divider

    // Static base colors for Dark Mode
    val CanvasBaseDark        = Color(0xFF141012) // Warm dark canvas
    val SurfaceCardDark       = Color(0xFF1E191B) // Dark card
    val LightHighlightDark    = Color(0xFF283244) // Specular top-left glow in dark
    val DarkShadowDark        = Color(0xFF2E272A) // Hairline outline in dark
    val DarkTextPrimary       = Color(0xFFF5EFF1) // Off-white
    val DarkTextSecondary     = Color(0xFFA89CA0) // Warm grey
    val DarkBorderSubtle      = Color(0xFF2E272A) // Dark outline / divider

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
    val DefaultKpmSkyBlue     = Color(0xFF9A1F3F) // Brand Bordeaux (token name kept for call sites)
    val BrandBordeauxDark     = Color(0xFF7C1D33)
    val BrandOnDark           = Color(0xFFC94A68) // Brand accent on dark surfaces
    val SelectedPink          = Color(0xFFF3C4D0)
    val SurfacePink           = Color(0xFFFBE7EC)
    val DefaultActionAmber    = Color(0xFFD97706)
    val DefaultStarGold       = Color(0xFFF59E0B)
    val DefaultSuccessMint    = Color(0xFF059669)
    val DefaultKokuPurple     = Color(0xFF7C3AED)

    // Dynamic 30% Structural Secondary
    /** The single brand accent (Bordeaux). Name kept from the KPM-blue era to avoid churn. */
    val KpmSkyBlue: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord8
            AppThemeMode.DARK -> BrandOnDark
            AppThemeMode.LIGHT -> DefaultKpmSkyBlue
        }

    val Brand: Color
        @Composable get() = KpmSkyBlue

    /** Hero cards (current class, next trip): deep brand in every theme. */
    val HeroContainer: Color
        @Composable get() = when (LocalAppThemeMode.current) {
            AppThemeMode.NORD -> NordColors.Nord10
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

/** Timetable / trip states: each has a tinted card background, an accent bar and chip colour. */
enum class SlotStatus { COMPLETED, NOW, NEXT, UPCOMING, BREAK, EXAM, CANCELLED, ROOM_CHANGED, MAKEUP, ON_TIME, DELAYED, OFFLINE }

data class StatusColors(val container: Color, val accent: Color, val onContainer: Color)

private fun lightStatus(status: SlotStatus): StatusColors = when (status) {
    SlotStatus.COMPLETED    -> StatusColors(Color(0xFFEAF4EC), Color(0xFF3E8E5A), Color(0xFF2E6B44))
    SlotStatus.NOW          -> StatusColors(Color(0xFFFBE7EC), Color(0xFF9A1F3F), Color(0xFF7C1D33))
    SlotStatus.NEXT         -> StatusColors(Color(0xFFF3EFEF), Color(0xFF9A1F3F), Color(0xFF5E5458))
    SlotStatus.UPCOMING     -> StatusColors(Color(0xFFF3EFEF), Color(0xFF9C9296), Color(0xFF5E5458))
    SlotStatus.BREAK        -> StatusColors(Color(0xFFF8EEDC), Color(0xFFB7843B), Color(0xFF7A5520))
    SlotStatus.EXAM         -> StatusColors(Color(0xFFF0EAF8), Color(0xFF7C4DBA), Color(0xFF5B3590))
    SlotStatus.CANCELLED    -> StatusColors(Color(0xFFFDE8E8), Color(0xFFC0392B), Color(0xFF962D22))
    SlotStatus.ROOM_CHANGED -> StatusColors(Color(0xFFFFF1D6), Color(0xFFC77D12), Color(0xFF8C560B))
    SlotStatus.MAKEUP       -> StatusColors(Color(0xFFE8EFFA), Color(0xFF3A6EA5), Color(0xFF2A527D))
    SlotStatus.ON_TIME      -> StatusColors(Color(0xFFEAF4EC), Color(0xFF3E8E5A), Color(0xFF2E6B44))
    SlotStatus.DELAYED      -> StatusColors(Color(0xFFFFF1D6), Color(0xFFC77D12), Color(0xFF8C560B))
    SlotStatus.OFFLINE      -> StatusColors(Color(0xFFF3EFEF), Color(0xFF9C9296), Color(0xFF5E5458))
}

@Composable
fun statusColors(status: SlotStatus): StatusColors {
    val light = lightStatus(status)
    return if (LocalThemeIsDark.current) {
        // Dark themes: tint the dark card with the accent instead of a pastel fill.
        StatusColors(
            container = light.accent.copy(alpha = 0.16f).compositeOver(SlotifyColors.SurfaceCard),
            accent = light.accent,
            onContainer = SlotifyColors.TextPrimary
        )
    } else light
}
