// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic design tokens for the 2009 Xbox 360 NXE ("Xbox Glass") + Material 3 design system,
 * aligned with Lab 151 design token standards (`engineering-design-and-test-standards`).
 */
@Immutable
data class NxeGlassTokens(
    val presetId: String,
    val presetName: String,
    val bgApp: Color,
    val bgStageMid: Color,
    val bgStageFloor: Color,
    val bgSurface: Color,
    val bgSurfaceElevated: Color,
    val bgInput: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val borderAccent: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentBgSubtle: Color,
    val accentBorderSubtle: Color,
    // Semantic Multi-Accent Channel Blades (No Monochrome Wash)
    val consoleEmerald: Color = Color(0xFF16A34A),
    val consoleEmeraldBright: Color = Color(0xFF4ADE80),
    val cloudCyan: Color = Color(0xFF00E5FF),
    val achievementsGold: Color = Color(0xFFF59E0B),
    val settingsAmethyst: Color = Color(0xFFA78BFA),
    // Standardized Lab 151 Fuchsia Developer Mode Palette
    val devBg: Color = Color(0x1AD946EF),
    val devSurface: Color = Color(0x4D4A044E),
    val devBorder: Color = Color(0x4DD946EF),
    val devText: Color = Color(0xFFF0ABFC),
    val devAccent: Color = Color(0xFFC026D3),
    val developerFuchsia: Color = Color(0xFFD946EF),
    val developerFuchsiaLight: Color = Color(0xFFF0ABFC),
    // 2008-2009 Xbox 360 NXE Studio Stage, Brushed Silver Bevels & Glass Tokens
    val bgSkyDome: Color = bgApp,
    val bgHorizonBloom: Color = bgStageMid,
    val glassHeroTop: Color = bgSurfaceElevated.copy(alpha = 0.92f),
    val glassHeroBottom: Color = bgSurface.copy(alpha = 0.96f),
    val glassBladeTop: Color = bgSurfaceElevated.copy(alpha = 0.80f),
    val glassBladeBottom: Color = bgSurface.copy(alpha = 0.92f),
    val glassChromeTop: Color = bgApp.copy(alpha = 0.82f),
    val glassChromeBottom: Color = bgApp.copy(alpha = 0.90f),
    val glassModalScrim: Color = Color(0xD6040B18),
    val glassSheenStart: Color = Color.White.copy(alpha = 0.14f),
    val glassSheenMid: Color = Color.White.copy(alpha = 0.04f),
    val borderFocusedCyan: Color = Color.White.copy(alpha = 0.38f),
    val borderGlassRest: Color = Color.White.copy(alpha = 0.14f),
)

@Immutable
data class NxeThemePresetSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val primaryHex: String,
    val accentHex: String,
    val bgAppArgb: Long,
    val bgStageMidArgb: Long,
    val bgStageFloorArgb: Long,
    val bgSurfaceArgb: Long,
    val bgSurfaceElevatedArgb: Long,
    val accentPrimaryArgb: Long,
    val accentSecondaryArgb: Long,
    val consoleTileArgb: Long = 0xFF1D4ED8,
    val cloudTileArgb: Long = 0xFF00E5FF,
    val achievementsTileArgb: Long = 0xFFF59E0B,
    val settingsTileArgb: Long = 0xFFA78BFA,
)

@Immutable
data class NxeColorSwatch(
    val id: String,
    val label: String,
    val hex: String,
    val argb: Long,
)

object NxeThemePresets {
    const val DEFAULT_PRESET_ID = "xbox_2008_blue"

    val PRESETS: List<NxeThemePresetSpec> = listOf(
        NxeThemePresetSpec(
            id = "xbox_2008_blue",
            name = "Xbox 360 Blue",
            subtitle = "Classic blue dashboard with silver accents",
            primaryHex = "#1D4ED8",
            accentHex = "#38BDF8",
            bgAppArgb = 0xFF071A38,
            bgStageMidArgb = 0xFF154284,
            bgStageFloorArgb = 0xFF1A365D,
            bgSurfaceArgb = 0xFF0E264A,
            bgSurfaceElevatedArgb = 0xFF1B3F76,
            accentPrimaryArgb = 0xFF1D4ED8,
            accentSecondaryArgb = 0xFF38BDF8,
        ),
        NxeThemePresetSpec(
            id = "xbox_2008_green",
            name = "Xbox 360 Green",
            subtitle = "Deep Xbox green with bright green accents",
            primaryHex = "#107C10",
            accentHex = "#6CC644",
            bgAppArgb = 0xFF0B1E11,
            bgStageMidArgb = 0xFF1B4D27,
            bgStageFloorArgb = 0xFF213D29,
            bgSurfaceArgb = 0xFF112E1A,
            bgSurfaceElevatedArgb = 0xFF1D4A2B,
            accentPrimaryArgb = 0xFF107C10,
            accentSecondaryArgb = 0xFF6CC644,
        ),
        NxeThemePresetSpec(
            id = "series_x_carbon",
            name = "Carbon Dark",
            subtitle = "Dark charcoal slate with green accents",
            primaryHex = "#16A34A",
            accentHex = "#22D3EE",
            bgAppArgb = 0xFF05070B,
            bgStageMidArgb = 0xFF0B1018,
            bgStageFloorArgb = 0xFF14212E,
            bgSurfaceArgb = 0xFF0F1722,
            bgSurfaceElevatedArgb = 0xFF192536,
            accentPrimaryArgb = 0xFF16A34A,
            accentSecondaryArgb = 0xFF22D3EE,
        ),
        NxeThemePresetSpec(
            id = "cyber_halo_violet",
            name = "Midnight Indigo",
            subtitle = "Deep indigo blue with cyan accents",
            primaryHex = "#6366F1",
            accentHex = "#38BDF8",
            bgAppArgb = 0xFF050517,
            bgStageMidArgb = 0xFF0E0F2E,
            bgStageFloorArgb = 0xFF1E1B4B,
            bgSurfaceArgb = 0xFF131438,
            bgSurfaceElevatedArgb = 0xFF1E2054,
            accentPrimaryArgb = 0xFF6366F1,
            accentSecondaryArgb = 0xFF38BDF8,
        ),
        NxeThemePresetSpec(
            id = "crimson_blade",
            name = "Crimson Dark",
            subtitle = "Dark red surfaces with amber accents",
            primaryHex = "#DC2626",
            accentHex = "#F59E0B",
            bgAppArgb = 0xFF110306,
            bgStageMidArgb = 0xFF24080E,
            bgStageFloorArgb = 0xFF3F101A,
            bgSurfaceArgb = 0xFF2B0A12,
            bgSurfaceElevatedArgb = 0xFF42121D,
            accentPrimaryArgb = 0xFFDC2626,
            accentSecondaryArgb = 0xFFF59E0B,
        ),
        NxeThemePresetSpec(
            id = "midnight_oled",
            name = "Pure Black (OLED)",
            subtitle = "True black background for OLED displays",
            primaryHex = "#0284C7",
            accentHex = "#00E5FF",
            bgAppArgb = 0xFF000000,
            bgStageMidArgb = 0xFF030914,
            bgStageFloorArgb = 0xFF08182E,
            bgSurfaceArgb = 0xFF071224,
            bgSurfaceElevatedArgb = 0xFF0E203D,
            accentPrimaryArgb = 0xFF0284C7,
            accentSecondaryArgb = 0xFF00E5FF,
        ),
    )

    val SWATCHES: List<NxeColorSwatch> = listOf(
        NxeColorSwatch("royal_sapphire", "Royal Sapphire", "#1D4ED8", 0xFF1D4ED8),
        NxeColorSwatch("electric_cyan", "Electric Cyan", "#00E5FF", 0xFF00E5FF),
        NxeColorSwatch("xbox_emerald", "Xbox 360 Green", "#107C10", 0xFF107C10),
        NxeColorSwatch("velocity_green", "Velocity Green", "#16A34A", 0xFF16A34A),
        NxeColorSwatch("nxe_lime", "NXE Lime", "#84CC16", 0xFF84CC16),
        NxeColorSwatch("sky_frost", "Sky Frost", "#38BDF8", 0xFF38BDF8),
        NxeColorSwatch("ocean_cobalt", "Ocean Cobalt", "#0284C7", 0xFF0284C7),
        NxeColorSwatch("cortana_indigo", "Cortana Indigo", "#6366F1", 0xFF6366F1),
        NxeColorSwatch("luminous_amethyst", "Luminous Amethyst", "#A78BFA", 0xFFA78BFA),
        NxeColorSwatch("plasma_violet", "Plasma Violet", "#8B5CF6", 0xFF8B5CF6),
        NxeColorSwatch("trophy_gold", "Trophy Gold", "#F59E0B", 0xFFF59E0B),
        NxeColorSwatch("solar_amber", "Solar Amber", "#FBBF24", 0xFFFBBF24),
        NxeColorSwatch("sunset_orange", "Sunset Orange", "#F97316", 0xFFF97316),
        NxeColorSwatch("gears_crimson", "Gears Crimson", "#DC2626", 0xFFDC2626),
        NxeColorSwatch("ruby_flare", "Ruby Flare", "#EF4444", 0xFFEF4444),
        NxeColorSwatch("neon_teal", "Neon Teal", "#14B8A6", 0xFF14B8A6),
        NxeColorSwatch("arctic_mint", "Arctic Mint", "#34D399", 0xFF34D399),
        NxeColorSwatch("titanium_silver", "Titanium Silver", "#94A3B8", 0xFF94A3B8),
    )

    @JvmStatic
    fun findPresetSpec(presetId: String?): NxeThemePresetSpec {
        if (presetId.isNullOrBlank()) {
            return PRESETS.first()
        }
        return PRESETS.firstOrNull { it.id == presetId } ?: PRESETS.first()
    }

    @JvmStatic
    fun parseHexToArgb(hex: String?, fallbackArgb: Long): Long {
        if (hex.isNullOrBlank()) return fallbackArgb
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6 && cleaned.length != 8) return fallbackArgb
        return try {
            val parsed = cleaned.toLong(16)
            if (cleaned.length == 6) (0xFF000000L or parsed) else parsed
        } catch (_: NumberFormatException) {
            fallbackArgb
        }
    }

    @JvmStatic
    fun resolveTokens(
        presetId: String? = DEFAULT_PRESET_ID,
        customPrimaryHex: String? = null,
        customAccentHex: String? = null,
    ): NxeGlassTokens {
        val spec = findPresetSpec(presetId)
        val primaryArgb = parseHexToArgb(customPrimaryHex, spec.accentPrimaryArgb)
        val secondaryArgb = parseHexToArgb(customAccentHex, spec.accentSecondaryArgb)
        val primaryColor = Color(primaryArgb)
        val secondaryColor = Color(secondaryArgb)

        return NxeGlassTokens(
            presetId = spec.id,
            presetName = spec.name,
            bgApp = Color(spec.bgAppArgb),
            bgStageMid = Color(spec.bgStageMidArgb),
            bgStageFloor = Color(spec.bgStageFloorArgb),
            bgSurface = Color(spec.bgSurfaceArgb),
            bgSurfaceElevated = Color(spec.bgSurfaceElevatedArgb),
            bgInput = Color(spec.bgAppArgb).copy(alpha = 0.88f),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFD6E2F0),
            textMuted = Color(0xFF94A3B8),
            borderSubtle = Color.White.copy(alpha = 0.12f),
            borderStrong = Color.White.copy(alpha = 0.24f),
            borderAccent = Color.White.copy(alpha = 0.46f),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            accentBgSubtle = primaryColor.copy(alpha = 0.24f),
            accentBorderSubtle = Color.White.copy(alpha = 0.18f),
            consoleEmerald = Color(0xFF16A34A),
            consoleEmeraldBright = Color(0xFF4ADE80),
            cloudCyan = Color(spec.cloudTileArgb),
            achievementsGold = Color(spec.achievementsTileArgb),
            settingsAmethyst = Color(spec.settingsTileArgb),
        )
    }
}
