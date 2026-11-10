// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui

import androidx.compose.ui.graphics.Color
import com.xstreaming.nativeui.theme.NxeThemePresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NxeThemePresetsTest {

    @Test
    fun `catalog exposes exactly 6 signature presets and 18 custom accent swatches`() {
        assertEquals(6, NxeThemePresets.PRESETS.size)
        assertEquals(18, NxeThemePresets.SWATCHES.size)

        val uniquePresetIds = NxeThemePresets.PRESETS.map { it.id }.toSet()
        assertEquals(6, uniquePresetIds.size)
        assertTrue(uniquePresetIds.contains(NxeThemePresets.DEFAULT_PRESET_ID))
    }

    @Test
    fun `default 2009 Xbox Glass preset enforces non-monochrome studio depth and distinct channel accents`() {
        val tokens = NxeThemePresets.resolveTokens("xbox_2008_blue", null, null)

        assertEquals("xbox_2008_blue", tokens.presetId)
        assertNotEquals(tokens.bgApp, tokens.accentPrimary)
        assertNotEquals(tokens.bgStageFloor, tokens.accentPrimary)
        assertNotEquals(tokens.bgApp, tokens.bgStageFloor)

        // Multi-accent channel tokens must be distinct, while body text stays cool brushed silver
        val channelAccents = setOf(
            tokens.consoleEmerald,
            tokens.cloudCyan,
            tokens.achievementsGold,
            tokens.settingsAmethyst,
        )
        assertEquals(4, channelAccents.size)
        assertEquals(Color(0xFFD6E2F0), tokens.textSecondary)
        assertNotEquals(tokens.accentSecondary, tokens.textSecondary)
    }

    @Test
    fun `developer mode tokens match standardized Lab 151 Fuchsia palette`() {
        val tokens = NxeThemePresets.resolveTokens("xbox_2008_blue", null, null)

        assertEquals(Color(0xFFD946EF), tokens.developerFuchsia)
        assertEquals(Color(0xFFF0ABFC), tokens.developerFuchsiaLight)
    }

    @Test
    fun `custom accent hex overrides primary and secondary accents while preserving deep glass backdrop`() {
        val baseTokens = NxeThemePresets.resolveTokens("xbox_2008_blue", null, null)
        val customTokens = NxeThemePresets.resolveTokens("xbox_2008_blue", "#FF2A5F", "#00E5FF")

        assertEquals(baseTokens.bgApp, customTokens.bgApp)
        assertEquals(baseTokens.bgStageFloor, customTokens.bgStageFloor)
        assertEquals(Color(0xFFFF2A5F), customTokens.accentPrimary)
        assertNotEquals(baseTokens.accentPrimary, customTokens.accentPrimary)
    }

    @Test
    fun `unknown preset id or malformed custom hex falls back safely without throwing`() {
        val fallbackPreset = NxeThemePresets.resolveTokens("non_existent_preset", "not-a-hex", "invalid")
        assertEquals(NxeThemePresets.DEFAULT_PRESET_ID, fallbackPreset.presetId)
        assertEquals(Color(0xFF1D4ED8), fallbackPreset.accentPrimary)
    }

    @Test
    fun `parseHexToArgb parses 6-digit and 8-digit hex codes accurately`() {
        val defaultArgb = 0xFF000000L
        assertEquals(0xFF00E5FFL, NxeThemePresets.parseHexToArgb("#00E5FF", defaultArgb))
        assertEquals(0x80107C10L, NxeThemePresets.parseHexToArgb("#80107C10", defaultArgb))
        assertEquals(defaultArgb, NxeThemePresets.parseHexToArgb("", defaultArgb))
        assertEquals(defaultArgb, NxeThemePresets.parseHexToArgb("#12345", defaultArgb))
    }
}
