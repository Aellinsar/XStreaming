// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.core

import android.content.Context
import android.content.Intent

/**
 * Synchronous boot configuration, Two-Tier Developer Mode state, and feature flag resolver.
 *
 * Uses standard Android [ android.content.SharedPreferences ] ("xstreaming_native_prefs") so
 * [ com.xstreaming.MainActivity.onCreate ] can evaluate `native_android_ui` in <1ms on cold
 * start before React Native's JNI MMKV instance initializes.
 */
object UiEngineBootConfig {
    const val PREFS_NAME = "xstreaming_native_prefs"
    const val AUTH_VAULT_PREFS_NAME = "xstreaming_auth_vault"

    const val FLAG_NATIVE_ANDROID_UI = "native_android_ui"
    const val KEY_DEV_MODE_AVAILABLE = "is_developer_mode_available"
    const val KEY_DEV_MODE_ENABLED = "is_developer_mode_enabled"
    const val KEY_UI_HAPTICS_ENABLED = "ui_haptics_enabled"
    const val KEY_THEME_PRESET = "theme_preset"
    const val KEY_PRIMARY_COLOR = "primary_color"
    const val KEY_ACCENT_COLOR = "accent_color"
    const val KEY_CACHED_GAMERTAG = "cached_gamertag"
    const val KEY_CACHED_GAMERPIC_URL = "cached_gamerpic_url"

    const val DEFAULT_THEME_PRESET = "xbox_2008_blue"
    const val DEFAULT_PRIMARY_COLOR = "#1D4ED8"
    const val DEFAULT_ACCENT_COLOR = "#00E5FF"
    const val VERSION_TAP_UNLOCK_THRESHOLD = 7

    /**
     * Global feature flag overrides (`GLOBAL_FEATURE_FLAG_OVERRIDES`).
     * When a flag key is present in this map, its boolean value overrides local user storage
     * and the Developer Options UI displays a lock indicator.
     */
    val GLOBAL_FEATURE_FLAG_OVERRIDES: Map<String, Boolean> = emptyMap()

    /**
     * Pure deterministic feature flag resolver (unit-tested in JUnit 4).
     */
    @JvmStatic
    fun resolveFeatureFlag(
        flagKey: String,
        userPreference: Boolean?,
        defaultValue: Boolean = true,
        overrides: Map<String, Boolean> = GLOBAL_FEATURE_FLAG_OVERRIDES,
    ): Boolean {
        if (overrides.containsKey(flagKey)) {
            return overrides.getValue(flagKey)
        }
        return userPreference ?: defaultValue
    }

    /**
     * Pure helper verifying whether a feature flag is locked by [GLOBAL_FEATURE_FLAG_OVERRIDES].
     */
    @JvmStatic
    fun isFeatureFlagLocked(
        flagKey: String,
        overrides: Map<String, Boolean> = GLOBAL_FEATURE_FLAG_OVERRIDES,
    ): Boolean = overrides.containsKey(flagKey)

    /**
     * Pure helper for Two-Tier Developer Mode unlock progression (7 taps on version badge).
     */
    @JvmStatic
    fun shouldUnlockDeveloperMode(tapCount: Int): Boolean =
        tapCount >= VERSION_TAP_UNLOCK_THRESHOLD

    /**
     * Pure helper enforcing the Lab 151 Two-Tier Developer Mode rule:
     * active developer UI elements only render when BOTH `isDeveloperModeAvailable`
     * and `isDeveloperModeEnabled` are true.
     */
    @JvmStatic
    fun isDeveloperUiActive(
        isDeveloperModeAvailable: Boolean,
        isDeveloperModeEnabled: Boolean,
    ): Boolean = isDeveloperModeAvailable && isDeveloperModeEnabled

    @JvmStatic
    fun isNativeUiEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasUserPref = prefs.contains(FLAG_NATIVE_ANDROID_UI)
        val userPref = if (hasUserPref) prefs.getBoolean(FLAG_NATIVE_ANDROID_UI, true) else null
        return resolveFeatureFlag(
            flagKey = FLAG_NATIVE_ANDROID_UI,
            userPreference = userPref,
            defaultValue = true,
        )
    }

    @JvmStatic
    fun setNativeUiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(FLAG_NATIVE_ANDROID_UI, enabled)
            .commit()
    }

    /**
     * Persists the selected UI engine mode (`true` = Android Native Compose UI,
     * `false` = Legacy React Native UI) and cleanly restarts the application process.
     */
    @JvmStatic
    fun setNativeUiEnabledAndRestart(context: Context, enabled: Boolean) {
        setNativeUiEnabled(context, enabled)
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = launchIntent?.component
        if (componentName != null) {
            val restartIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    @JvmStatic
    fun isDeveloperModeAvailable(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEV_MODE_AVAILABLE, false)
    }

    @JvmStatic
    fun setDeveloperModeAvailable(context: Context, available: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEV_MODE_AVAILABLE, available)
            .apply()
    }

    @JvmStatic
    fun isDeveloperModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val available = prefs.getBoolean(KEY_DEV_MODE_AVAILABLE, false)
        val enabled = prefs.getBoolean(KEY_DEV_MODE_ENABLED, false)
        return isDeveloperUiActive(available, enabled)
    }

    @JvmStatic
    fun setDeveloperModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEV_MODE_AVAILABLE, true)
            .putBoolean(KEY_DEV_MODE_ENABLED, enabled)
            .apply()
    }

    @JvmStatic
    fun isUiHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_UI_HAPTICS_ENABLED, true)
    }

    @JvmStatic
    fun setUiHapticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UI_HAPTICS_ENABLED, enabled)
            .apply()
    }

    @JvmStatic
    fun getThemePresetId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_PRESET, DEFAULT_THEME_PRESET) ?: DEFAULT_THEME_PRESET
    }

    @JvmStatic
    fun setThemePresetId(context: Context, presetId: String, primaryHex: String, accentHex: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_PRESET, presetId)
            .putString(KEY_PRIMARY_COLOR, primaryHex)
            .putString(KEY_ACCENT_COLOR, accentHex)
            .apply()
    }

    @JvmStatic
    fun saveCachedUserProfile(context: Context, gamertag: String, gamerpicUrl: String) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        if (gamertag.isNotBlank()) {
            editor.putString(KEY_CACHED_GAMERTAG, gamertag.trim())
        }
        if (gamerpicUrl.isNotBlank()) {
            editor.putString(KEY_CACHED_GAMERPIC_URL, gamerpicUrl.trim())
        }
        editor.apply()
    }

    @JvmStatic
    fun clearCachedUserProfile(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CACHED_GAMERTAG)
            .remove(KEY_CACHED_GAMERPIC_URL)
            .apply()
    }

    /**
     * Synchronously persists a serialized authentication token payload into the native
     * Android SharedPreferences vault (`xstreaming_auth_vault`) so 90-day OAuth refresh tokens
     * survive MMKV resets, JS reloads, or UI engine switches.
     */
    @JvmStatic
    fun saveAuthVaultEntry(context: Context, key: String, value: String) {
        if (key.isBlank()) return
        context.getSharedPreferences(AUTH_VAULT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .commit()
    }

    @JvmStatic
    fun getAuthVaultEntry(context: Context, key: String): String? {
        if (key.isBlank()) return null
        return context.getSharedPreferences(AUTH_VAULT_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
    }

    @JvmStatic
    fun clearAuthVaultEntry(context: Context, key: String) {
        if (key.isBlank()) return
        context.getSharedPreferences(AUTH_VAULT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .commit()
    }
}
