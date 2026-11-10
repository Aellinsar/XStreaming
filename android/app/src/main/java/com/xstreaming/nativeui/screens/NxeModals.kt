// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xstreaming.nativeui.bridge.NxeActiveModal
import com.xstreaming.nativeui.bridge.SessionReportSummary
import com.xstreaming.nativeui.bridge.XStreamingStateStore
import com.xstreaming.nativeui.bridge.XStreamingUiState
import com.xstreaming.nativeui.components.NxeModalBreadcrumb
import com.xstreaming.nativeui.components.NxeModalFrame
import com.xstreaming.nativeui.core.UiEngineBootConfig
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.LocalNxeHaptics
import com.xstreaming.nativeui.theme.NxeTypography

/**
 * Dispatcher for all modals in the Native Android UI, enforcing the 6 Lab 151 Modal Invariants
 * and zero redundant footer "Close" / "Done" buttons.
 */
@Composable
fun NxeActiveModalHost(state: XStreamingUiState) {
    when (state.activeModal) {
        NxeActiveModal.THEME_STUDIO,
        NxeActiveModal.SETTINGS_APPEARANCE,
        -> AppearanceSettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SETTINGS_DISPLAY -> DisplaySettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SETTINGS_AUDIO -> AudioSettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SETTINGS_CONTROLLER -> ControllerSettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SETTINGS_NETWORK -> NetworkSettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SETTINGS_ACCOUNT -> XboxAccountSettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SETTINGS_DEVELOPER -> DeveloperSettingsModal(
            state = state,
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.SESSION_REPORT -> {
            val report = state.lastSessionReport ?: SessionReportSummary(
                targetName = state.selectedConsole?.name ?: "Xbox Series X",
                streamType = "home",
                durationText = "24m 10s",
                avgFps = 60,
                avgBitrateMbps = 32.4,
                avgRttMs = 5,
                avgDecodeMs = 2.1,
                packetLossPct = 0.0,
                exitReason = "Clean Session Exit",
            )
            SessionReportModal(
                report = report,
                onDismiss = { dontShowAgain ->
                    if (dontShowAgain) {
                        XStreamingStateStore.updateSettings { it.copy(showSessionReport = false) }
                        XStreamingStateStore.emitCommandToJs(
                            "updateSetting",
                            """{"key":"show_session_report","value":false}""",
                        )
                    }
                    XStreamingStateStore.dismissModal()
                },
            )
        }
        NxeActiveModal.ABOUT -> AboutModal(
            state = state,
            onNavigateReleaseNotes = { XStreamingStateStore.openModal(NxeActiveModal.RELEASE_NOTES) },
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.RELEASE_NOTES -> ReleaseNotesModal(
            onNavigateBackToAbout = { XStreamingStateStore.openModal(NxeActiveModal.ABOUT) },
            onDismiss = { XStreamingStateStore.dismissModal() },
        )
        NxeActiveModal.DEVICE_CODE -> DeviceCodeAuthModal(
            state = state,
            onDismiss = {
                XStreamingStateStore.emitCommandToJs("cancelMsalLogin")
                XStreamingStateStore.dismissModal()
            },
        )
        null -> Unit
    }
}

@Composable
private fun SessionReportModal(
    report: SessionReportSummary,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    var dontShowAgain by remember { mutableStateOf(false) }

    NxeModalFrame(
        title = "Session Telemetry Report",
        onDismissRequest = { onDismiss(dontShowAgain) },
        maxWidth = 600.dp,
        footerContent = {
            // Leading edge: Don't show this again toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics?.tickBladeFocus()
                        dontShowAgain = !dontShowAgain
                    }
                    .padding(end = 8.dp),
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = tokens.accentPrimary,
                        uncheckedColor = tokens.textMuted,
                    ),
                )
                Text(
                    text = "Don't show this again",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Trailing edge: Meaningful Copy Telemetry action (zero redundant Close/Done button)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.accentPrimary)
                    .border(1.dp, tokens.borderAccent, RoundedCornerShape(10.dp))
                    .clickable {
                        haptics?.clickConfirm()
                        copyToClipboard(
                            context = context,
                            label = "XStreaming Telemetry",
                            text = report.toFormattedTelemetryReport(),
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Copy Telemetry",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = report.targetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                    )
                    Text(
                        text = "Mode: ${report.streamType} • ${report.exitReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.consoleEmeraldBright,
                    )
                }
                Text(
                    text = report.durationText,
                    style = NxeTypography.TelemetryValue,
                    color = tokens.accentSecondary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TelemetryMetricTile(
                    label = "AVG FRAME RATE",
                    value = "${report.avgFps} FPS",
                    accent = tokens.consoleEmeraldBright,
                    modifier = Modifier.weight(1f),
                )
                TelemetryMetricTile(
                    label = "AVG BITRATE",
                    value = String.format("%.1f Mbps", report.avgBitrateMbps),
                    accent = tokens.cloudCyan,
                    modifier = Modifier.weight(1f),
                )
                TelemetryMetricTile(
                    label = "NETWORK RTT",
                    value = "${report.avgRttMs} ms",
                    accent = tokens.accentSecondary,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TelemetryMetricTile(
                    label = "HW DECODE LATENCY",
                    value = String.format("%.1f ms", report.avgDecodeMs),
                    accent = tokens.settingsAmethyst,
                    modifier = Modifier.weight(1f),
                )
                TelemetryMetricTile(
                    label = "PACKET LOSS",
                    value = String.format("%.2f%%", report.packetLossPct),
                    accent = tokens.achievementsGold,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = NxeTypography.TelemetryLabel,
            color = tokens.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = NxeTypography.TelemetryValue,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AboutModal(
    state: XStreamingUiState,
    onNavigateReleaseNotes: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val settings = state.settings

    NxeModalFrame(
        title = "About XStreaming",
        onDismissRequest = onDismiss,
        footerContent = {
            Text(
                text = "Licensed under GNU AGPLv3 + MPL-2.0",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tokens.accentBgSubtle)
                    .border(1.dp, tokens.borderStrong, RoundedCornerShape(4.dp))
                    .clickable {
                        haptics?.clickConfirm()
                        onNavigateReleaseNotes()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "View Release Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textPrimary,
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, tokens.borderSubtle, RoundedCornerShape(4.dp))
                    .clickable {
                        haptics?.tickBladeFocus()
                        val unlockedNow = XStreamingStateStore.recordVersionBadgeTap()
                        if (unlockedNow) {
                            UiEngineBootConfig.setDeveloperModeEnabled(context, true)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "XStreaming v2.12.0-NXE",
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.textPrimary,
                    )
                    val tapHint = when {
                        settings.isDeveloperModeAvailable -> "Developer Options Unlocked"
                        settings.versionTapCount >= 2 -> {
                            val remaining = (UiEngineBootConfig.VERSION_TAP_UNLOCK_THRESHOLD - settings.versionTapCount).coerceAtLeast(1)
                            "Tap $remaining more times to unlock Developer Options"
                        }
                        else -> "Android 13+ Native Compose Edition"
                    }
                    Text(
                        text = tapHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.isDeveloperModeAvailable) tokens.developerFuchsiaLight else tokens.textMuted,
                    )
                }
            }

            Text(
                text = "An engineering-focused fork of Geocld/XStreaming featuring a Native Android Jetpack Compose UI, direct RFC1918 LAN IPv4 candidate prioritization, and clean <50ms console shutdown detection.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
            )
            Text(
                text = "Upstream Author & Copyright: Geocld and XStreaming contributors (GNU AGPLv3).",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary,
            )
        }
    }
}

@Composable
private fun ReleaseNotesModal(
    onNavigateBackToAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current

    NxeModalFrame(
        title = "Release Notes",
        onDismissRequest = onDismiss,
        headerBreadcrumb = {
            NxeModalBreadcrumb(
                parentLabel = "About XStreaming",
                currentLabel = "Release Notes (v2.12.0-NXE)",
                onNavigateParent = onNavigateBackToAbout,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "• Native Android Compose UI (minSdkVersion 33 / Android 13 / Pixel Tablet baseline) with AGSL RuntimeShader 2009 Xbox Glass refraction.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textPrimary,
            )
            Text(
                text = "• API 33 LRA hardware tactile haptics (NxeHapticsEngine) on D-Pad blade focus, LB/RB bumpers, and (A)/(B) actions.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textPrimary,
            )
            Text(
                text = "• Direct RFC1918 LAN IPv4 (192.168.x.x) ICE candidate prioritization and <50ms SCTP DataChannel console shutdown exit.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textPrimary,
            )
            Text(
                text = "• Two-Tier Fuchsia Developer Mode with conditional native_android_ui cold-start switcher.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textPrimary,
            )
        }
    }
}

@Composable
private fun DeviceCodeAuthModal(
    state: XStreamingUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current

    NxeModalFrame(
        title = "Xbox Live Sign-In (MSAL Device Code)",
        onDismissRequest = onDismiss,
        footerContent = {
            Text(
                text = "Waiting for browser confirmation...",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.consoleEmerald)
                    .border(1.dp, tokens.consoleEmeraldBright, RoundedCornerShape(10.dp))
                    .clickable {
                        haptics?.clickConfirm()
                        copyToClipboard(context, "Xbox Device Code", state.deviceCode)
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.deviceVerificationUri)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Copy Code & Open Browser",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textPrimary,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Visit ${state.deviceVerificationUri} and enter this one-time code:",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textPrimary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.bgInput)
                    .border(2.dp, tokens.borderAccent, RoundedCornerShape(14.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    text = state.deviceCode.ifBlank { "LOADING..." },
                    style = MaterialTheme.typography.headlineMedium,
                    color = tokens.accentSecondary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
