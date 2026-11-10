// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstreaming.nativeui.bridge.AuthStatus
import com.xstreaming.nativeui.bridge.SessionReportSummary
import com.xstreaming.nativeui.bridge.XStreamingStateStore
import com.xstreaming.nativeui.bridge.XStreamingUiState
import com.xstreaming.nativeui.components.NxeModalFrame
import com.xstreaming.nativeui.core.UiEngineBootConfig
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.LocalNxeHaptics
import com.xstreaming.nativeui.theme.NxeThemePresets

@Composable
internal fun AppearanceSettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val settings = state.settings
    val scrollState = rememberScrollState()

    NxeModalFrame(
        title = "Appearance",
        onDismissRequest = onDismiss,
        maxWidth = 680.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "COLOR THEMES",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NxeThemePresets.PRESETS.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = settings.themePresetId == preset.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(preset.bgSurfaceElevatedArgb))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.52f) else tokens.borderSubtle,
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clickable {
                                        haptics?.clickConfirm()
                                        UiEngineBootConfig.setThemePresetId(
                                            context = context,
                                            presetId = preset.id,
                                            primaryHex = preset.primaryHex,
                                            accentHex = preset.accentHex,
                                        )
                                        XStreamingStateStore.applyThemePreset(
                                            presetId = preset.id,
                                            primaryHex = preset.primaryHex,
                                            accentHex = preset.accentHex,
                                        )
                                    }
                                    .padding(11.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(preset.accentPrimaryArgb))
                                            .border(
                                                1.dp,
                                                Color(preset.accentSecondaryArgb).copy(alpha = 0.72f),
                                                RoundedCornerShape(3.dp),
                                            ),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "ACCENT COLOR",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NxeThemePresets.SWATCHES.chunked(6).forEach { rowSwatches ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowSwatches.forEach { swatch ->
                            val isSelected = settings.accentColorHex.equals(swatch.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(swatch.argb))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(3.dp),
                                    )
                                    .clickable {
                                        haptics?.tickBladeFocus()
                                        UiEngineBootConfig.setThemePresetId(
                                            context = context,
                                            presetId = settings.themePresetId,
                                            primaryHex = settings.primaryColorHex,
                                            accentHex = swatch.hex,
                                        )
                                        XStreamingStateStore.applyThemePreset(
                                            presetId = settings.themePresetId,
                                            primaryHex = settings.primaryColorHex,
                                            accentHex = swatch.hex,
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "INTERFACE & FEEDBACK",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )

            SettingToggleRow(
                title = "Android Native UI (Xbox 360 NXE)",
                subtitle = "Restart app to switch between Android Native UI and React Native UI",
                checked = settings.nativeAndroidUiEnabled,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    UiEngineBootConfig.setNativeUiEnabledAndRestart(context, enabled)
                },
            )

            SettingToggleRow(
                title = "Tactile UI Haptics",
                subtitle = "Mechanical feedback when navigating tabs, cards, and actions",
                checked = settings.uiHapticsEnabled,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    UiEngineBootConfig.setUiHapticsEnabled(context, enabled)
                    XStreamingStateStore.updateSettings { it.copy(uiHapticsEnabled = enabled) }
                },
            )

            SettingToggleRow(
                title = "Post-Stream Session Summary",
                subtitle = "Show frame rate, latency, and bitrate summary after disconnecting",
                checked = settings.showSessionReport,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(showSessionReport = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"show_session_report","value":$enabled}""",
                    )
                },
            )
        }
    }
}

@Composable
internal fun DisplaySettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val settings = state.settings
    val scrollState = rememberScrollState()

    NxeModalFrame(
        title = "Display",
        onDismissRequest = onDismiss,
        maxWidth = 640.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingToggleRow(
                title = "1080p HQ Resolution",
                subtitle = "Request highest-bitrate 1080p stream profile",
                checked = settings.resolution >= 1080,
                onCheckedChange = { highRes ->
                    haptics?.clickConfirm()
                    val nextRes = if (highRes) 1081 else 720
                    XStreamingStateStore.updateSettings { it.copy(resolution = nextRes) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"resolution","value":$nextRes}""",
                    )
                },
            )

            val bitrateSummary = if (settings.xhomeBitrateMode.equals("auto", ignoreCase = true)) {
                "Auto (Adaptive LAN Bitrate)"
            } else {
                "${settings.xhomeBitrateMbps} Mbps Custom Target"
            }
            val bitrateActionLabel = when {
                settings.xhomeBitrateMode.equals("auto", ignoreCase = true) -> "Auto"
                else -> "${settings.xhomeBitrateMbps} Mbps"
            }
            SettingActionRow(
                title = "Remote Play Video Bitrate",
                subtitle = bitrateSummary,
                actionLabel = bitrateActionLabel,
                accentColor = tokens.cloudCyan,
                onClick = {
                    val (nextMode, nextMbps) = when {
                        settings.xhomeBitrateMode.equals("auto", ignoreCase = true) -> "custom" to 20
                        settings.xhomeBitrateMbps < 35 -> "custom" to 35
                        settings.xhomeBitrateMbps < 50 -> "custom" to 50
                        else -> "auto" to 20
                    }
                    XStreamingStateStore.updateSettings {
                        it.copy(xhomeBitrateMode = nextMode, xhomeBitrateMbps = nextMbps)
                    }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"xhome_bitrate_mode","value":"$nextMode"}""",
                    )
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"xhome_bitrate","value":$nextMbps}""",
                    )
                },
            )

            val codecLabel = when {
                settings.codec.contains("hevc", ignoreCase = true) -> "HEVC"
                settings.codec.contains("avc", ignoreCase = true) -> "H.264 High"
                else -> "Auto"
            }
            SettingActionRow(
                title = "Preferred Video Codec",
                subtitle = "Tap to cycle between Auto, H.264 High Profile, and HEVC",
                actionLabel = codecLabel,
                accentColor = tokens.cloudCyan,
                onClick = {
                    val nextCodec = when {
                        settings.codec.isBlank() -> "video/avc"
                        settings.codec.contains("avc", ignoreCase = true) -> "video/hevc"
                        else -> ""
                    }
                    XStreamingStateStore.updateSettings { it.copy(codec = nextCodec) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"codec","value":"$nextCodec"}""",
                    )
                },
            )

            SettingToggleRow(
                title = "Hardware Low-Latency Decoder",
                subtitle = "Zero-buffer MediaCodec hardware video decoding for minimal input lag",
                checked = settings.lowLatencyDecoder,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(lowLatencyDecoder = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"native_low_latency_decoder","value":$enabled}""",
                    )
                },
            )

            SettingToggleRow(
                title = "AMD FidelityFX Super Resolution (FSR 1.0)",
                subtitle = "Edge-adaptive upscaling and sharpening for high-DPI screens",
                checked = settings.fsrEnabled,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(fsrEnabled = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"fsr","value":$enabled}""",
                    )
                },
            )

            SettingToggleRow(
                title = "Show Stream Stats Bar by Default",
                subtitle = "Display live FPS, bitrate, RTT, and decode time at top of stream",
                checked = settings.showPerformanceOverlay,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(showPerformanceOverlay = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"show_performance","value":$enabled}""",
                    )
                },
            )
        }
    }
}

@Composable
internal fun AudioSettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val haptics = LocalNxeHaptics.current
    val settings = state.settings

    NxeModalFrame(
        title = "Audio",
        onDismissRequest = onDismiss,
        maxWidth = 580.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingToggleRow(
                title = "Stereo Audio Output",
                subtitle = "Full 2-channel stereo PCM stream audio",
                checked = settings.stereoAudioEnabled,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(stereoAudioEnabled = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"enable_stereo_audio","value":$enabled}""",
                    )
                },
            )
        }
    }
}

@Composable
internal fun ControllerSettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val haptics = LocalNxeHaptics.current
    val settings = state.settings

    NxeModalFrame(
        title = "Controller",
        onDismissRequest = onDismiss,
        maxWidth = 580.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingToggleRow(
                title = "Controller Rumble",
                subtitle = "Forward in-game vibration to USB and Bluetooth controllers",
                checked = settings.rumbleEnabled,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(rumbleEnabled = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"vibration","value":$enabled}""",
                    )
                },
            )

            SettingToggleRow(
                title = "Always Show Touch Controls",
                subtitle = "Keep on-screen virtual controller visible even when a gamepad is paired",
                checked = settings.showVirtualGamepad,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(showVirtualGamepad = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"show_virtual_gamead","value":$enabled}""",
                    )
                },
            )
        }
    }
}

@Composable
internal fun NetworkSettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val haptics = LocalNxeHaptics.current
    val settings = state.settings

    NxeModalFrame(
        title = "Network & Console",
        onDismissRequest = onDismiss,
        maxWidth = 580.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingToggleRow(
                title = "Auto-Wake Xbox on Connect",
                subtitle = "Automatically wake Standby console when starting Remote Play",
                checked = settings.autoWakeConsole,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(autoWakeConsole = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"power_on","value":$enabled}""",
                    )
                },
            )

            SettingToggleRow(
                title = "Prefer IPv6 Routing",
                subtitle = "Leave off on home Wi-Fi or Ethernet so direct local IPv4 connects first",
                checked = settings.preferIpv6,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    XStreamingStateStore.updateSettings { it.copy(preferIpv6 = enabled) }
                    XStreamingStateStore.emitCommandToJs(
                        "updateSetting",
                        """{"key":"ipv6","value":$enabled}""",
                    )
                },
            )
        }
    }
}

@Composable
internal fun XboxAccountSettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val isSignedOut = state.authStatus == AuthStatus.UNAUTHENTICATED && state.consoles.isEmpty()

    NxeModalFrame(
        title = "Xbox Account & Session",
        onDismissRequest = onDismiss,
        maxWidth = 580.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Gamercard Account Summary Banner (Full-height flush left Gamerpic)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, tokens.borderSubtle, RoundedCornerShape(4.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NxeGamerpicAvatarBox(
                    gamerpicUrl = state.gamerpicUrl,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = state.gamertagName.ifEmpty {
                                if (isSignedOut) "Not Signed In" else "Xbox Network Account"
                            },
                            color = tokens.textPrimary,
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.gamertagSuffix.isNotEmpty()) {
                            Text(
                                text = state.gamertagSuffix,
                                color = Color(0xFF94A3B8),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                                maxLines = 1,
                            )
                        }
                    }

                    Text(
                        text = state.authStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isSignedOut) {
                SettingActionRow(
                    title = "1-Tap Browser Sign-In",
                    subtitle = "Sign in with your Microsoft account in Chrome Custom Tabs",
                    actionLabel = "Sign In",
                    accentColor = tokens.consoleEmeraldBright,
                    onClick = {
                        onDismiss()
                        XStreamingStateStore.emitCommandToJs("startWebLogin")
                    },
                )
                SettingActionRow(
                    title = "2nd-Screen Device Code",
                    subtitle = "Sign in from another phone or PC at microsoft.com/link",
                    actionLabel = "Get Code",
                    accentColor = tokens.accentSecondary,
                    onClick = {
                        onDismiss()
                        XStreamingStateStore.emitCommandToJs("startMsalLogin")
                    },
                )
            } else {
                SettingActionRow(
                    title = "Sign Out of Xbox Network",
                    subtitle = "Clear saved OAuth tokens and switch Microsoft accounts",
                    actionLabel = "Sign Out",
                    accentColor = Color(0xFFEF4444),
                    onClick = {
                        haptics?.tickBack()
                        onDismiss()
                        XStreamingStateStore.emitCommandToJs("signOut")
                    },
                )
            }
        }
    }
}

@Composable
internal fun DeveloperSettingsModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val settings = state.settings

    NxeModalFrame(
        title = "Developer Options",
        onDismissRequest = onDismiss,
        maxWidth = 600.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingToggleRow(
                title = "Enable Developer Tools",
                subtitle = "Expose UI engine boot toggle and telemetry preview controls",
                checked = settings.isDeveloperModeEnabled,
                accentColor = tokens.developerFuchsia,
                onCheckedChange = { enabled ->
                    haptics?.clickConfirm()
                    UiEngineBootConfig.setDeveloperModeEnabled(context, enabled)
                    XStreamingStateStore.setDeveloperModeEnabled(enabled)
                },
            )

            if (settings.isDeveloperUiActive) {
                val isFlagLocked = UiEngineBootConfig.isFeatureFlagLocked(
                    UiEngineBootConfig.FLAG_NATIVE_ANDROID_UI,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(tokens.devSurface)
                        .border(1.dp, tokens.devBorder, RoundedCornerShape(4.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "UI Engine Boot Flag (native_android_ui)",
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.devText,
                    )
                    Text(
                        text = if (isFlagLocked) {
                            "Locked via GLOBAL_FEATURE_FLAG_OVERRIDES in UiEngineBootConfig.kt"
                        } else {
                            "Switch cold-start UI engine between Native Android UI and Legacy React Native UI. Requires app restart."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textPrimary.copy(alpha = 0.85f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FuchsiaActionButton(
                            label = "Switch to Legacy UI",
                            enabled = !isFlagLocked,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics?.clickConfirm()
                                UiEngineBootConfig.setNativeUiEnabledAndRestart(context, false)
                            },
                        )

                        FuchsiaActionButton(
                            label = "Preview Telemetry",
                            enabled = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics?.clickConfirm()
                                XStreamingStateStore.presentSessionReport(
                                    SessionReportSummary(
                                        targetName = state.selectedConsole?.name ?: "Xbox Series X",
                                        streamType = "Remote Play (LAN)",
                                        durationText = "38m 42s",
                                        avgFps = 60,
                                        avgBitrateMbps = 34.8,
                                        avgRttMs = 4,
                                        avgDecodeMs = 1.8,
                                        packetLossPct = 0.0,
                                        exitReason = "Clean Console Shutdown",
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color = LocalNxeGlassTokens.current.accentSecondary,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, tokens.borderSubtle, RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.textPrimary,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = tokens.textMuted,
                uncheckedTrackColor = tokens.bgInput,
            ),
        )
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, tokens.borderSubtle, RoundedCornerShape(4.dp))
            .clickable {
                haptics?.clickConfirm()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.textPrimary,
            )
        }
    }
}

@Composable
private fun FuchsiaActionButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) tokens.devAccent else tokens.bgInput)
            .border(1.dp, tokens.devBorder, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
