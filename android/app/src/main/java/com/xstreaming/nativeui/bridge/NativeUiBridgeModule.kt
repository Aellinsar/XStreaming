// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.xstreaming.nativeui.core.UiEngineBootConfig

/**
 * Bi-directional React Native <-> Native Kotlin StateFlow bridge (`NativeUiBridge`).
 *
 * Keeps the existing Hermes protocol engine (`MsalAuthentication.ts`, `XcloudApi.ts`,
 * `webrtc/index.ts`, `teredoCandidates.ts`, `streamReconnect.ts`) running as a headless
 * signaling core while driving the Jetpack Compose 2009 Xbox Glass UI at 120fps.
 */
class NativeUiBridgeModule(
    private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {

    init {
        XStreamingStateStore.commandEmitter = { command, payloadJson ->
            sendCommandToJs(command, payloadJson)
        }
    }

    override fun getName(): String = NAME

    private fun sendCommandToJs(command: String, payloadJson: String) {
        if (!reactContext.hasActiveReactInstance()) return
        runCatching {
            val params = Arguments.createMap().apply {
                putString("command", command)
                putString("payload", payloadJson)
            }
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(EVENT_NATIVE_UI_COMMAND, params)
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    fun isNativeUiEnabled(): Boolean {
        return UiEngineBootConfig.isNativeUiEnabled(reactContext)
    }

    @ReactMethod
    fun setNativeUiEnabledAndRestart(enabled: Boolean) {
        val activity = currentActivity ?: reactContext
        UiEngineBootConfig.setNativeUiEnabledAndRestart(activity, enabled)
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    fun saveBackupToken(key: String?, value: String?) {
        if (key.isNullOrBlank() || value == null) return
        UiEngineBootConfig.saveAuthVaultEntry(reactContext, key, value)
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    fun getBackupToken(key: String?): String? {
        if (key.isNullOrBlank()) return null
        return UiEngineBootConfig.getAuthVaultEntry(reactContext, key)
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    fun clearBackupToken(key: String?) {
        if (key.isNullOrBlank()) return
        UiEngineBootConfig.clearAuthVaultEntry(reactContext, key)
    }

    @ReactMethod
    fun syncAuthState(
        status: String,
        message: String?,
        deviceCode: String?,
        verificationUri: String?,
        gamertag: String?,
        gamerpicUrl: String?,
    ) {
        val cleanGamertag = gamertag?.trim().orEmpty()
        val cleanGamerpic = gamerpicUrl?.trim().orEmpty()
        if (status.equals("unauthenticated", ignoreCase = true) || status.equals("logged_out", ignoreCase = true)) {
            UiEngineBootConfig.clearCachedUserProfile(reactContext)
        } else if (cleanGamertag.isNotEmpty() || cleanGamerpic.isNotEmpty()) {
            UiEngineBootConfig.saveCachedUserProfile(reactContext, cleanGamertag, cleanGamerpic)
        }
        XStreamingStateStore.syncAuthState(
            status = status,
            message = message ?: "",
            deviceCode = deviceCode ?: "",
            verificationUri = verificationUri ?: "https://microsoft.com/link",
            gamertag = cleanGamertag,
            gamerpicUrl = cleanGamerpic,
        )
    }

    @ReactMethod
    fun syncConsoles(consolesArray: ReadableArray?) {
        if (consolesArray == null) {
            XStreamingStateStore.updateConsoles(emptyList())
            return
        }
        val items = mutableListOf<XboxConsoleItem>()
        for (i in 0 until consolesArray.size()) {
            val map = consolesArray.getMap(i)
            val serverId = map.getStringOrDefault("serverId", "")
            if (serverId.isBlank()) continue
            val deviceName = map.getStringOrDefault("deviceName", "Xbox Console")
            val locale = map.getStringOrDefault("locale", "en-US")
            val powerState = map.getStringOrDefault("powerState", "ConnectedStandby")
            val consoleType = map.getStringOrDefault("consoleType", "XboxSeriesX")
            val isSameLanWarning = if (map.hasKey("outOfHomeWarning") && !map.isNull("outOfHomeWarning")) {
                map.getBoolean("outOfHomeWarning")
            } else {
                false
            }
            items.add(
                XboxConsoleItem(
                    serverId = serverId,
                    name = deviceName,
                    locale = locale,
                    powerState = powerState,
                    consoleType = consoleType,
                    isSameLanWarning = isSameLanWarning,
                ),
            )
        }
        XStreamingStateStore.updateConsoles(items)
    }

    @ReactMethod
    fun syncStreamTelemetry(map: ReadableMap?) {
        if (map == null) return
        XStreamingStateStore.syncStreamTelemetry(
            isStreaming = map.getBooleanOrDefault("isStreaming", false),
            streamType = map.getStringOrDefault("streamType", "home"),
            targetName = map.getStringOrDefault("targetName", "Xbox Console"),
            connectionPhase = map.getStringOrDefault("connectionPhase", ""),
            resolutionText = map.getStringOrDefault("resolutionText", "1080p HQ"),
            fps = map.getIntOrDefault("fps", 60),
            bitrateMbps = map.getDoubleOrDefault("bitrateMbps", 0.0),
            rttMs = map.getIntOrDefault("rttMs", 0),
            jitterMs = map.getDoubleOrDefault("jitterMs", 0.0),
            decodeMs = map.getDoubleOrDefault("decodeMs", 0.0),
            packetLossPct = map.getDoubleOrDefault("packetLossPct", 0.0),
        )
    }

    @ReactMethod
    fun syncSessionReport(map: ReadableMap?) {
        if (map == null) return
        val report = SessionReportSummary(
            targetName = map.getStringOrDefault("targetName", "Xbox Series X"),
            streamType = map.getStringOrDefault("streamType", "home"),
            durationText = map.getStringOrDefault("durationText", "0m 00s"),
            avgFps = map.getIntOrDefault("avgFps", 60),
            avgBitrateMbps = map.getDoubleOrDefault("avgBitrateMbps", 24.0),
            avgRttMs = map.getIntOrDefault("avgRttMs", 8),
            avgDecodeMs = map.getDoubleOrDefault("avgDecodeMs", 2.5),
            packetLossPct = map.getDoubleOrDefault("packetLossPct", 0.0),
            exitReason = map.getStringOrDefault("exitReason", "Clean Session Exit"),
        )
        XStreamingStateStore.presentSessionReport(report)
    }

    @ReactMethod
    fun setRouteScreen(screenName: String?) {
        val resolved = screenName?.trim().orEmpty()
        if (resolved.isNotEmpty()) {
            XStreamingStateStore.setCurrentRouteScreen(resolved)
        }
    }

    @ReactMethod
    fun syncCloudTitles(titlesArray: ReadableArray?, isLoading: Boolean) {
        if (titlesArray == null) {
            XStreamingStateStore.updateCloudTitles(emptyList(), isLoading)
            return
        }
        val items = mutableListOf<CloudCatalogItem>()
        for (i in 0 until titlesArray.size()) {
            val map = titlesArray.getMap(i)
            val titleId = map.getStringOrDefault("titleId", "")
            val name = map.getStringOrDefault("name", "")
            if (titleId.isBlank() || name.isBlank()) continue
            val category = map.getStringOrDefault("category", "Game Pass")
            val subtitle = map.getStringOrDefault("subtitle", "Cloud Enabled • 60 FPS")
            items.add(
                CloudCatalogItem(
                    titleId = titleId,
                    name = name,
                    category = category,
                    subtitle = subtitle,
                ),
            )
        }
        XStreamingStateStore.updateCloudTitles(items, isLoading)
    }

    @ReactMethod
    fun syncAchievements(achievementsArray: ReadableArray?, isLoading: Boolean) {
        if (achievementsArray == null) {
            XStreamingStateStore.updateAchievements(emptyList(), isLoading)
            return
        }
        val items = mutableListOf<XboxAchievementItem>()
        for (i in 0 until achievementsArray.size()) {
            val map = achievementsArray.getMap(i)
            val titleId = map.getStringOrDefault("titleId", "$i")
            val name = map.getStringOrDefault("name", "Xbox Title")
            val lastUnlock = map.getStringOrDefault("lastUnlock", "")
            val currentGs = map.getIntOrDefault("currentGamerscore", 0)
            val maxGs = map.getIntOrDefault("maxGamerscore", 1000).coerceAtLeast(1)
            items.add(
                XboxAchievementItem(
                    titleId = titleId,
                    name = name,
                    lastUnlock = lastUnlock,
                    currentGamerscore = currentGs,
                    maxGamerscore = maxGs,
                ),
            )
        }
        XStreamingStateStore.updateAchievements(items, isLoading)
    }

    @ReactMethod
    fun syncSettings(map: ReadableMap?) {
        if (map == null) return
        val presetId = map.getStringOrDefault("theme", UiEngineBootConfig.DEFAULT_THEME_PRESET)
        val primaryHex = map.getStringOrDefault("theme_primary_color", UiEngineBootConfig.DEFAULT_PRIMARY_COLOR)
        val resolution = map.getIntOrDefault("resolution", 1081)
        val xhomeBitrateMode = map.getStringOrDefault("xhome_bitrate_mode", "auto")
        val xhomeBitrateMbps = map.getIntOrDefault("xhome_bitrate", 20)
        val codec = map.getStringOrDefault("codec", "")
        val lowLatencyDecoder = map.getBooleanOrDefault("native_low_latency_decoder", true)
        val fsr = map.getBooleanOrDefault("fsr", true)
        val stereoAudio = map.getBooleanOrDefault("enable_stereo_audio", true)
        val vibration = map.getBooleanOrDefault("vibration", true)
        val ipv6 = map.getBooleanOrDefault("ipv6", false)
        val powerOn = map.getBooleanOrDefault("power_on", true)
        val showSessionReport = map.getBooleanOrDefault("show_session_report", true)
        val showPerformance = map.getBooleanOrDefault("show_performance", false)
        val showVirtualGamepad = map.getBooleanOrDefault("show_virtual_gamead", false)
        val virtualGamepadOpacity = map.getDoubleOrDefault("virtual_gamepad_opacity", 0.75).toFloat().coerceIn(0.2f, 1.0f)

        XStreamingStateStore.updateSettings { current ->
            current.copy(
                themePresetId = presetId,
                primaryColorHex = primaryHex,
                resolution = resolution,
                xhomeBitrateMode = xhomeBitrateMode,
                xhomeBitrateMbps = xhomeBitrateMbps,
                codec = codec,
                lowLatencyDecoder = lowLatencyDecoder,
                fsrEnabled = fsr,
                stereoAudioEnabled = stereoAudio,
                rumbleEnabled = vibration,
                preferIpv6 = ipv6,
                autoWakeConsole = powerOn,
                showSessionReport = showSessionReport,
                showPerformanceOverlay = showPerformance,
                showVirtualGamepad = showVirtualGamepad,
                virtualGamepadOpacity = virtualGamepadOpacity,
            )
        }
    }

    @ReactMethod
    fun syncThemeSettings(presetId: String?, primaryHex: String?, accentHex: String?) {
        val resolvedPreset = presetId ?: UiEngineBootConfig.DEFAULT_THEME_PRESET
        val resolvedPrimary = primaryHex ?: UiEngineBootConfig.DEFAULT_PRIMARY_COLOR
        val resolvedAccent = accentHex ?: UiEngineBootConfig.DEFAULT_ACCENT_COLOR
        UiEngineBootConfig.setThemePresetId(
            context = reactContext,
            presetId = resolvedPreset,
            primaryHex = resolvedPrimary,
            accentHex = resolvedAccent,
        )
        XStreamingStateStore.updateSettings { current ->
            current.copy(
                themePresetId = resolvedPreset,
                primaryColorHex = resolvedPrimary,
                accentColorHex = resolvedAccent,
            )
        }
    }

    @ReactMethod
    @Suppress("UNUSED_PARAMETER")
    fun addListener(eventName: String?) {
        // Required by React Native NativeEventEmitter
    }

    @ReactMethod
    @Suppress("UNUSED_PARAMETER")
    fun removeListeners(count: Int) {
        // Required by React Native NativeEventEmitter
    }

    companion object {
        const val NAME = "NativeUiBridge"
        const val EVENT_NATIVE_UI_COMMAND = "onNativeUiCommand"
    }
}

private fun ReadableMap.getStringOrDefault(key: String, defaultValue: String): String =
    if (hasKey(key) && !isNull(key)) (getString(key) ?: defaultValue) else defaultValue

private fun ReadableMap.getBooleanOrDefault(key: String, defaultValue: Boolean): Boolean =
    if (hasKey(key) && !isNull(key)) getBoolean(key) else defaultValue

private fun ReadableMap.getIntOrDefault(key: String, defaultValue: Int): Int =
    if (hasKey(key) && !isNull(key)) getInt(key) else defaultValue

private fun ReadableMap.getDoubleOrDefault(key: String, defaultValue: Double): Double =
    if (hasKey(key) && !isNull(key)) getDouble(key) else defaultValue
