// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui

import com.xstreaming.nativeui.core.UiEngineBootConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiEngineBootConfigTest {

    @Test
    fun `resolveFeatureFlag respects user preference when global override is absent`() {
        val overrides = emptyMap<String, Boolean>()

        assertTrue(
            UiEngineBootConfig.resolveFeatureFlag(
                flagKey = UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI,
                userPreference = true,
                defaultValue = true,
                overrides = overrides,
            ),
        )
        assertFalse(
            UiEngineBootConfig.resolveFeatureFlag(
                flagKey = UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI,
                userPreference = false,
                defaultValue = true,
                overrides = overrides,
            ),
        )
        assertTrue(
            UiEngineBootConfig.resolveFeatureFlag(
                flagKey = UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI,
                userPreference = null,
                defaultValue = true,
                overrides = overrides,
            ),
        )
    }

    @Test
    fun `resolveFeatureFlag enforces global override lock when present`() {
        val forcedOffOverrides = mapOf(
            UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI to false,
        )

        assertFalse(
            UiEngineBootConfig.resolveFeatureFlag(
                flagKey = UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI,
                userPreference = true,
                defaultValue = true,
                overrides = forcedOffOverrides,
            ),
        )
        assertTrue(
            UiEngineBootConfig.isFeatureFlagLocked(
                flagKey = UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI,
                overrides = forcedOffOverrides,
            ),
        )
    }

    @Test
    fun `two-tier developer mode requires 7 taps to unlock and both flags to activate`() {
        assertFalse(UiEngineBootConfig.shouldUnlockDeveloperMode(0))
        assertFalse(UiEngineBootConfig.shouldUnlockDeveloperMode(6))
        assertTrue(UiEngineBootConfig.shouldUnlockDeveloperMode(7))
        assertTrue(UiEngineBootConfig.shouldUnlockDeveloperMode(12))

        assertFalse(
            UiEngineBootConfig.isDeveloperUiActive(
                isDeveloperModeAvailable = false,
                isDeveloperModeEnabled = false,
            ),
        )
        assertFalse(
            UiEngineBootConfig.isDeveloperUiActive(
                isDeveloperModeAvailable = true,
                isDeveloperModeEnabled = false,
            ),
        )
        assertFalse(
            UiEngineBootConfig.isDeveloperUiActive(
                isDeveloperModeAvailable = false,
                isDeveloperModeEnabled = true,
            ),
        )
        assertTrue(
            UiEngineBootConfig.isDeveloperUiActive(
                isDeveloperModeAvailable = true,
                isDeveloperModeEnabled = true,
            ),
        )
    }
}
