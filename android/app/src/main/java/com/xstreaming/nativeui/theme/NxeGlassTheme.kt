// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.xstreaming.nativeui.core.NxeHapticsEngine

val LocalNxeGlassTokens = staticCompositionLocalOf {
    NxeThemePresets.resolveTokens()
}

val LocalNxeHaptics = staticCompositionLocalOf<NxeHapticsEngine?> {
    null
}

@Composable
fun NxeGlassTheme(
    presetId: String = NxeThemePresets.DEFAULT_PRESET_ID,
    customPrimaryHex: String? = null,
    customAccentHex: String? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val tokens = remember(presetId, customPrimaryHex, customAccentHex) {
        NxeThemePresets.resolveTokens(
            presetId = presetId,
            customPrimaryHex = customPrimaryHex,
            customAccentHex = customAccentHex,
        )
    }
    val haptics = remember(context) {
        NxeHapticsEngine(context.applicationContext)
    }

    val colorScheme = remember(tokens) {
        darkColorScheme(
            primary = tokens.accentPrimary,
            onPrimary = tokens.textPrimary,
            secondary = tokens.accentSecondary,
            onSecondary = tokens.bgApp,
            tertiary = tokens.consoleEmerald,
            background = tokens.bgApp,
            onBackground = tokens.textPrimary,
            surface = tokens.bgSurface,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.bgSurfaceElevated,
            onSurfaceVariant = tokens.textMuted,
            outline = tokens.borderStrong,
            outlineVariant = tokens.borderSubtle,
        )
    }

    CompositionLocalProvider(
        LocalNxeGlassTokens provides tokens,
        LocalNxeHaptics provides haptics,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NxeTypography.MaterialTypography,
            content = content,
        )
    }
}
