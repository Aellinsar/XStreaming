// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.bridge

import androidx.compose.runtime.Immutable
import com.xstreaming.nativeui.core.UiEngineBootConfig

enum class AuthStatus {
    LOADING,
    UNAUTHENTICATED,
    DEVICE_CODE_PENDING,
    AUTHENTICATED,
}

enum class NxeDashboardChannel(val label: String) {
    HOME("My Xbox"),
    CLOUD("Cloud Gaming"),
    ACHIEVEMENTS("Achievements"),
    SETTINGS("System Settings"),
}

enum class NxeActiveModal {
    THEME_STUDIO,
    SETTINGS_APPEARANCE,
    SETTINGS_DISPLAY,
    SETTINGS_AUDIO,
    SETTINGS_CONTROLLER,
    SETTINGS_NETWORK,
    SETTINGS_ACCOUNT,
    SETTINGS_DEVELOPER,
    SESSION_REPORT,
    ABOUT,
    RELEASE_NOTES,
    DEVICE_CODE,
}

@Immutable
data class XboxConsoleItem(
    val serverId: String,
    val name: String,
    val locale: String = "en-US",
    val powerState: String = "ConnectedStandby",
    val consoleType: String = "XboxSeriesX",
    val isSameLanWarning: Boolean = false,
) {
    val isPoweredOn: Boolean
        get() = powerState.equals("On", ignoreCase = true)

    val isStandbyReady: Boolean
        get() = powerState.equals("ConnectedStandby", ignoreCase = true) || isPoweredOn

    val friendlyPowerBadge: String
        get() = when {
            isPoweredOn -> "ONLINE • READY"
            powerState.equals("ConnectedStandby", ignoreCase = true) -> "STANDBY • INSTANT WAKE"
            else -> powerState.uppercase()
        }
}

@Immutable
data class CloudCatalogItem(
    val titleId: String,
    val name: String,
    val category: String = "Game Pass",
    val subtitle: String = "Cloud Enabled • 60 FPS",
)

@Immutable
data class XboxAchievementItem(
    val titleId: String,
    val name: String,
    val lastUnlock: String = "",
    val currentGamerscore: Int = 0,
    val maxGamerscore: Int = 1000,
) {
    val progressFraction: Float
        get() = if (maxGamerscore <= 0) 0f else (currentGamerscore.toFloat() / maxGamerscore.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt()
}

@Immutable
data class StreamOverlayState(
    val isStreaming: Boolean = false,
    val streamType: String = "home",
    val targetName: String = "Xbox Series X",
    val connectionPhase: String = "",
    val resolutionText: String = "1080p HQ",
    val fps: Int = 60,
    val bitrateMbps: Double = 28.5,
    val rttMs: Int = 6,
    val jitterMs: Double = 0.8,
    val decodeMs: Double = 2.4,
    val packetLossPct: Double = 0.0,
    val isHudDrawerOpen: Boolean = false,
    val isPerformanceOverlayVisible: Boolean = false,
    val isTouchGamepadVisible: Boolean = false,
    val hasUserToggledTouchGamepad: Boolean = false,
    val isMicMuted: Boolean = true,
)

@Immutable
data class SessionReportSummary(
    val targetName: String,
    val streamType: String,
    val durationText: String,
    val avgFps: Int,
    val avgBitrateMbps: Double,
    val avgRttMs: Int,
    val avgDecodeMs: Double,
    val packetLossPct: Double,
    val exitReason: String,
) {
    fun toFormattedTelemetryReport(): String = buildString {
        appendLine("XStreaming Session Telemetry Summary")
        appendLine("Target: $targetName ($streamType)")
        appendLine("Duration: $durationText")
        appendLine("Average FPS: $avgFps fps")
        appendLine("Average Bitrate: ${String.format("%.1f", avgBitrateMbps)} Mbps")
        appendLine("Average Network RTT: $avgRttMs ms")
        appendLine("Average Hardware Decode: ${String.format("%.1f", avgDecodeMs)} ms")
        appendLine("Packet Loss: ${String.format("%.2f", packetLossPct)}%")
        appendLine("Exit Status: $exitReason")
    }
}

@Immutable
data class NativeSettingsState(
    val themePresetId: String = UiEngineBootConfig.DEFAULT_THEME_PRESET,
    val primaryColorHex: String = UiEngineBootConfig.DEFAULT_PRIMARY_COLOR,
    val accentColorHex: String = UiEngineBootConfig.DEFAULT_ACCENT_COLOR,
    val uiHapticsEnabled: Boolean = true,
    val showSessionReport: Boolean = true,
    val resolution: Int = 1081,
    val xhomeBitrateMode: String = "auto",
    val xhomeBitrateMbps: Int = 20,
    val maxBitrateMode: String = "Auto",
    val customBitrateKbps: Int = 35000,
    val codec: String = "",
    val lowLatencyDecoder: Boolean = true,
    val fsrEnabled: Boolean = true,
    val fsrSharpness: Int = 2,
    val stereoAudioEnabled: Boolean = true,
    val audioMode: String = "Stereo_LowLatency",
    val rumbleEnabled: Boolean = true,
    val preferIpv6: Boolean = false,
    val autoWakeConsole: Boolean = true,
    val powerOffOnExit: Boolean = false,
    val showPerformanceOverlay: Boolean = false,
    val showVirtualGamepad: Boolean = false,
    val virtualGamepadOpacity: Float = 0.75f,
    val isDeveloperModeAvailable: Boolean = false,
    val isDeveloperModeEnabled: Boolean = false,
    val versionTapCount: Int = 0,
    val nativeAndroidUiEnabled: Boolean = false,
) {
    val isDeveloperUiActive: Boolean
        get() = UiEngineBootConfig.isDeveloperUiActive(
            isDeveloperModeAvailable = isDeveloperModeAvailable,
            isDeveloperModeEnabled = isDeveloperModeEnabled,
        )
}

@Immutable
data class XStreamingUiState(
    val authStatus: AuthStatus = AuthStatus.LOADING,
    val authStatusMessage: String = "Checking Xbox Live tokens...",
    val deviceCode: String = "",
    val deviceVerificationUri: String = "https://microsoft.com/link",
    val gamertag: String = "",
    val gamerpicUrl: String = "",
    val consoles: List<XboxConsoleItem> = emptyList(),
    val selectedConsoleIndex: Int = 0,
    val selectedHomeSlotIndex: Int = 0,
    val selectedSettingsSlotIndex: Int = 0,
    val isRefreshingConsoles: Boolean = false,
    val cloudTitles: List<CloudCatalogItem> = emptyList(),
    val cloudSearchQuery: String = "",
    val isLoadingCloud: Boolean = false,
    val achievements: List<XboxAchievementItem> = emptyList(),
    val isLoadingAchievements: Boolean = false,
    val activeChannel: NxeDashboardChannel = NxeDashboardChannel.HOME,
    val activeModal: NxeActiveModal? = null,
    val isGamepadConnected: Boolean = false,
    val currentRouteScreen: String = "home",
    val streamState: StreamOverlayState = StreamOverlayState(),
    val lastSessionReport: SessionReportSummary? = null,
    val settings: NativeSettingsState = NativeSettingsState(),
) {
    val selectedConsole: XboxConsoleItem?
        get() = consoles.getOrNull(selectedConsoleIndex.coerceIn(0, (consoles.size - 1).coerceAtLeast(0)))

    val filteredCloudTitles: List<CloudCatalogItem>
        get() {
            val q = cloudSearchQuery.trim()
            if (q.isEmpty()) return cloudTitles
            return cloudTitles.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
            }
        }

    val totalCurrentGamerscore: Int
        get() = achievements.sumOf { it.currentGamerscore }

    val totalMaxGamerscore: Int
        get() = achievements.sumOf { it.maxGamerscore }

    val gamertagName: String
        get() {
            val trimmed = gamertag.trim()
            if (trimmed.isEmpty()) return ""
            val hashIdx = trimmed.indexOf('#')
            return if (hashIdx > 0) trimmed.substring(0, hashIdx).trim() else trimmed
        }

    val gamertagSuffix: String
        get() {
            val trimmed = gamertag.trim()
            val hashIdx = trimmed.indexOf('#')
            if (hashIdx <= 0 || hashIdx >= trimmed.length - 1) return ""
            val suffixDigits = trimmed.substring(hashIdx + 1).trim()
            return if (suffixDigits.isNotEmpty()) "#$suffixDigits" else ""
        }
}
