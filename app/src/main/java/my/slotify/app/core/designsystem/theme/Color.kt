package my.slotify.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Slotify 60 : 30 : 10 Color System
 * - 60% Dominant Base: CanvasBase (#F4F7FB) / SurfaceCard (#E8EFF8)
 * - 30% Structural Secondary: KpmSkyBlue (#0284C7) / TextPrimary (#0F172A) / TextSecondary (#64748B)
 * - 10% Radiant Accent: ActionAmber (#FF7A00) / SuccessMint (#10B981)
 */
object SlotifyColors {
    // 60% — Dominant Base Canvas (Neumorphic Foundation)
    val CanvasBase        = Color(0xFFF4F7FB) // Soft Cloud Pearl
    val SurfaceCard       = Color(0xFFE8EFF8) // Tactile Off-White Squircle
    val LightHighlight    = Color(0xFFFFFFFF) // Specular Light Glow (Top-Left)
    val DarkShadow        = Color(0xFFCBD5E1) // Soft Slate Shadow (Bottom-Right)

    // Dark Mode Canvas Alternative
    val CanvasBaseDark    = Color(0xFF111622) // Deep Twilight Slate
    val SurfaceCardDark   = Color(0xFF192030) // Elevated Dark Slate
    val LightHighlightDark= Color(0xFF283244)
    val DarkShadowDark    = Color(0xFF0A0D14)

    // 30% — Structural Secondary (Typography, Navigation & Framework)
    val KpmSkyBlue        = Color(0xFF0284C7) // BM & Primary Brand Blue
    val TextPrimary       = Color(0xFF0F172A) // Deep Navy Slate (Ultra-Readable)
    val TextSecondary     = Color(0xFF64748B) // Cool Slate (Subtitles & Timestamps)
    val TextTertiary      = Color(0xFF94A3B8) // Muted Hint Gray
    val BorderSubtle      = Color(0xFFE2E8F0) // 1dp boundary divider

    // 10% — Radiant Accent (Action CTAs, Star Rewards & Active Focus)
    val ActionAmber       = Color(0xFFFF7A00) // Radiant Tangerine Amber (Primary Action)
    val StarGold          = Color(0xFFF59E0B) // Reward Star Coins ⭐
    val SuccessMint       = Color(0xFF10B981) // Completed Tasks & Safe Arrival
    val KokuPurple        = Color(0xFF8B5CF6) // Unit Beruniform & Rewards

    // Subject Badges (Pastel Tint Backgrounds with Saturated Labels)
    val ScienceGreen      = Color(0xFF10B981)
    val IslamTeal         = Color(0xFF0D9488)
    val EnglishRed        = Color(0xFFEF4444)
    val SejarahOrange     = Color(0xFFEA580C)
    val RbtBrown          = Color(0xFFB45309)

    // Logistics & Vehicles
    val VanPrimaryBlue     = Color(0xFF2563EB)
    val VanSecondaryGold   = Color(0xFFD97706)
    val ParentGreen       = Color(0xFF059669)
}
