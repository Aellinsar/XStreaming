// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xstreaming.nativeui.bridge.StreamOverlayState
import com.xstreaming.nativeui.bridge.XStreamingStateStore
import com.xstreaming.nativeui.components.NxeGlassCard
import com.xstreaming.nativeui.components.NxeStudioBackdrop
import com.xstreaming.nativeui.components.rememberNxeSystemBarInsets
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.LocalNxeHaptics
import com.xstreaming.nativeui.theme.NxeTypography
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Native 2009 Xbox 360 Ring-of-Light Stream HUD, Quick-Action Drawer, and
 * Multi-Touch Virtual Controller Overlay (`Xbox-16`).
 *
 * - While connecting (`streamState.connectionPhase.isNotBlank()`), renders an opaque
 *   [NxeStudioBackdrop] behind the Ring-of-Light Connection HUD Card so the underlying
 *   React Native views are never visible.
 * - Once connected (`streamState.connectionPhase.isBlank()`):
 *   1. Hides the telemetry stats bar by default (`!streamState.isPerformanceOverlayVisible`),
 *      showing only a compact, low-opacity Xbox Guide handle (`(X) Guide`) at top-center.
 *   2. Expands to live 1Hz WebRTC telemetry when `isPerformanceOverlayVisible` is enabled.
 *   3. Renders the Native 2009 Xbox Glass Touch Controller Overlay (`NxeTouchGamepadOverlay`)
 *      when `streamState.isTouchGamepadVisible` is `true` (auto-enabled when no physical
 *      gamepad is connected to the Android device, and toggleable in 1 tap from the drawer).
 */
@Composable
fun StreamOverlayHud(
    streamState: StreamOverlayState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val (topInsetDp, bottomInsetDp) = rememberNxeSystemBarInsets()
    val blockingBackdropInteraction = remember { MutableInteractionSource() }
    val isConnecting = streamState.connectionPhase.isNotBlank()

    BackHandler(enabled = streamState.isHudDrawerOpen || isConnecting) {
        haptics?.tickBack()
        if (streamState.isHudDrawerOpen) {
            XStreamingStateStore.toggleStreamHudDrawer()
        } else {
            XStreamingStateStore.emitCommandToJs("exitStream")
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 0. Opaque 2009 Xbox 360 Studio Horizon Backdrop while waking/connecting
        // so the underlying React Native UI never flashes through before video starts.
        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = blockingBackdropInteraction,
                        indication = null,
                        onClick = { /* Block touch passthrough to React Native while connecting */ },
                    ),
            ) {
                NxeStudioBackdrop()
            }
        }

        // 1. Native 2009 Xbox Glass Multi-Touch Virtual Controller Overlay
        if (!isConnecting && streamState.isTouchGamepadVisible && !streamState.isHudDrawerOpen) {
            NxeTouchGamepadOverlay(
                topSafeDp = topInsetDp,
                bottomSafeDp = bottomInsetDp,
            )
        }

        // 2. Top-Center Xbox Guide Handle (only shown AFTER stream is connected; hidden while connecting)
        if (!isConnecting) {
            val showFullTelemetry = streamState.isPerformanceOverlayVisible
            val topPillAlpha = when {
                streamState.isHudDrawerOpen || streamState.isPerformanceOverlayVisible -> 0.94f
                else -> 0.26f
            }

            Row(
                modifier = Modifier
                    .padding(top = topInsetDp + 6.dp)
                    .alpha(topPillAlpha)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.glassChromeTop)
                    .border(
                        width = 1.25.dp,
                        color = if (streamState.isHudDrawerOpen || showFullTelemetry) {
                            tokens.borderAccent
                        } else {
                            tokens.borderSubtle
                        },
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable {
                        haptics?.clickConfirm()
                        XStreamingStateStore.toggleStreamHudDrawer()
                    }
                    .padding(
                        horizontal = if (showFullTelemetry) 14.dp else 11.dp,
                        vertical = if (showFullTelemetry) 6.dp else 4.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Glowing 2009 Xbox 360 Ring-of-Light Sphere Indicator
                Box(
                    modifier = Modifier
                        .size(if (showFullTelemetry) 14.dp else 16.dp)
                        .clip(CircleShape)
                        .background(tokens.consoleEmerald)
                        .border(1.5.dp, tokens.consoleEmeraldBright, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "X",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                val pillText = if (streamState.isPerformanceOverlayVisible) {
                    buildString {
                        append(streamState.resolutionText)
                        append(" • ")
                        append("${streamState.fps} FPS")
                        append(" • ")
                        append(String.format(Locale.US, "%.1f Mbps", streamState.bitrateMbps))
                        append(" • ")
                        append("${streamState.rttMs}ms RTT")
                        append(" • ")
                        append(String.format(Locale.US, "%.1fms Dec", streamState.decodeMs))
                        append(" • ")
                        append(String.format(Locale.US, "%.2f%% Loss", streamState.packetLossPct))
                    }
                } else {
                    "Guide"
                }

                Text(
                    text = pillText,
                    style = if (showFullTelemetry) NxeTypography.TelemetryValue else MaterialTheme.typography.labelSmall,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 3. Centered 2009 Xbox 360 Ring-of-Light Connection HUD (while connecting / waking console)
        AnimatedVisibility(
            visible = isConnecting && !streamState.isHudDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            val phaseParts = remember(streamState.connectionPhase, streamState.streamType) {
                val raw = streamState.connectionPhase.split("|")
                val headline = raw.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Waking Xbox Console"
                val defaultSubtitle = if (streamState.streamType.equals("cloud", ignoreCase = true)) {
                    "Allocating cloud session & negotiating WebRTC..."
                } else {
                    "Powering on console & initializing remote play..."
                }
                val subtitle = raw.getOrNull(1)?.takeIf { it.isNotBlank() } ?: defaultSubtitle
                val step = raw.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 4) ?: 1
                Triple(headline, subtitle, step)
            }
            val (headlineText, subtitleText, currentStep) = phaseParts

            val stepTransition = rememberInfiniteTransition(label = "nxe_step_pulse")
            val activeStepPulse by stepTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 750, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "active_step_alpha",
            )

            NxeGlassCard(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                cornerRadius = 4.dp,
                isHero = true,
                accentOverride = tokens.consoleEmeraldBright,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Animated 2009 Xbox 360 Ring-of-Light + 3D Metallic Guide Sphere
                    NxeRingOfLightOrb(
                        primaryColor = tokens.consoleEmerald,
                        brightColor = tokens.consoleEmeraldBright,
                    )

                    // Crisp Rectangular Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(tokens.consoleEmerald.copy(alpha = 0.22f))
                            .border(1.dp, tokens.consoleEmeraldBright.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(tokens.consoleEmeraldBright),
                        )
                        Text(
                            text = if (streamState.streamType.equals("cloud", ignoreCase = true)) {
                                "XBOX CLOUD GAMING"
                            } else {
                                "XBOX REMOTE PLAY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.consoleEmeraldBright,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // 4-Quadrant Animated Connection Step Bar
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp, bottom = 2.dp)
                            .width(240.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (idx in 1..4) {
                            val isCompleted = idx < currentStep
                            val isCurrent = idx == currentStep
                            val segAlpha = when {
                                isCompleted -> 1.0f
                                isCurrent -> activeStepPulse
                                else -> 0.22f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .alpha(segAlpha)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isCompleted || isCurrent) {
                                            tokens.consoleEmeraldBright
                                        } else {
                                            tokens.textSecondary
                                        },
                                    )
                                    .border(
                                        width = if (isCurrent) 1.dp else 0.dp,
                                        color = if (isCurrent) Color.White.copy(alpha = 0.85f) else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp),
                                    ),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, tokens.borderStrong, RoundedCornerShape(4.dp))
                            .clickable {
                                haptics?.tickBack()
                                XStreamingStateStore.emitCommandToJs("exitStream")
                            }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = "Cancel Stream (B)",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // 4. Refracted Glass Quick-Action Stream Drawer (6 Modal Invariants)
        AnimatedVisibility(
            visible = streamState.isHudDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val scrimInteraction = remember { MutableInteractionSource() }
            val cardInteraction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.glassModalScrim)
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = {
                            haptics?.tickBack()
                            XStreamingStateStore.toggleStreamHudDrawer()
                        },
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                NxeGlassCard(
                    modifier = Modifier
                        .widthIn(max = 580.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = { /* Isolate click propagation */ },
                        ),
                    cornerRadius = 4.dp,
                    isHero = true,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Xbox Guide & Stream Controls",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${streamState.targetName} (${streamState.streamType.uppercase()})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.consoleEmeraldBright,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, tokens.borderSubtle, CircleShape)
                                    .clickable {
                                        haptics?.tickBack()
                                        XStreamingStateStore.toggleStreamHudDrawer()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "X",
                                    color = tokens.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HudActionTile(
                                title = "Xbox Guide (Nexus)",
                                subtitle = "Open console Guide",
                                accent = tokens.consoleEmeraldBright,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics?.clickConfirm()
                                    XStreamingStateStore.emitCommandToJs("sendXboxGuideButton")
                                    XStreamingStateStore.toggleStreamHudDrawer()
                                },
                            )
                            HudActionTile(
                                title = if (streamState.isTouchGamepadVisible) {
                                    "Hide Touch Controls"
                                } else {
                                    "Show Touch Controls"
                                },
                                subtitle = if (streamState.isTouchGamepadVisible) {
                                    "On-screen gamepad ON"
                                } else {
                                    "On-screen gamepad OFF"
                                },
                                accent = tokens.accentPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics?.clickConfirm()
                                    XStreamingStateStore.toggleStreamTouchGamepad()
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HudActionTile(
                                title = if (streamState.isPerformanceOverlayVisible) {
                                    "Hide Stats Bar"
                                } else {
                                    "Show Stats Bar"
                                },
                                subtitle = if (streamState.isPerformanceOverlayVisible) {
                                    "FPS, Bitrate & RTT ON"
                                } else {
                                    "Telemetry bar hidden"
                                },
                                accent = tokens.cloudCyan,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics?.tickBladeFocus()
                                    XStreamingStateStore.toggleStreamPerformanceOverlay()
                                    XStreamingStateStore.emitCommandToJs(
                                        "updateSetting",
                                        """{"key":"show_performance","value":${!streamState.isPerformanceOverlayVisible}}""",
                                    )
                                },
                            )
                            HudActionTile(
                                title = if (streamState.isMicMuted) "Unmute Party Mic" else "Mute Party Mic",
                                subtitle = if (streamState.isMicMuted) "Mic is muted" else "Mic is live",
                                accent = tokens.settingsAmethyst,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics?.clickConfirm()
                                    XStreamingStateStore.toggleStreamMicMute()
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Critical Protocol Invariant: Only offer Reconnect on Cloud streams,
                            // never auto-call startSession() on xHome Console Remote Play.
                            if (streamState.streamType.equals("cloud", ignoreCase = true)) {
                                HudActionTile(
                                    title = "Reconnect Cloud",
                                    subtitle = "Refresh WebRTC",
                                    accent = tokens.achievementsGold,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptics?.clickConfirm()
                                        XStreamingStateStore.emitCommandToJs("reconnectStream")
                                        XStreamingStateStore.toggleStreamHudDrawer()
                                    },
                                )
                            }

                            HudActionTile(
                                title = "Disconnect Stream",
                                subtitle = "Exit to My Xbox",
                                accent = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics?.tickBack()
                                    XStreamingStateStore.toggleStreamHudDrawer()
                                    XStreamingStateStore.emitCommandToJs("exitStream")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose 2009 Xbox Glass Multi-Touch Virtual Controller Overlay.
 *
 * Streams digital button states (`A`, `B`, `X`, `Y`, `DPad*`, `LeftShoulder`, `RightShoulder`,
 * `LeftTrigger`, `RightTrigger`, `LeftThumb`, `RightThumb`, `View`, `Menu`, `Nexus`) and
 * normalized analog stick axes (`LeftThumbXAxis`, `LeftThumbYAxis`, `RightThumbXAxis`,
 * `RightThumbYAxis`) directly into the WebRTC input channel.
 */
@Composable
private fun NxeTouchGamepadOverlay(
    topSafeDp: Dp,
    bottomSafeDp: Dp,
) {
    val tokens = LocalNxeGlassTokens.current

    DisposableEffect(Unit) {
        onDispose {
            XStreamingStateStore.sendVirtualGamepadInput("""{"type":"reset"}""")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = topSafeDp + 10.dp,
                bottom = bottomSafeDp + 12.dp,
                start = 18.dp,
                end = 18.dp,
            ),
    ) {
        // 1. Top-Left Shoulder Cluster (LT / LB)
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TouchHoldPillButton(
                label = "LT",
                buttonKey = "LeftTrigger",
                width = 62.dp,
                height = 30.dp,
                accentColor = tokens.cloudCyan,
            )
            TouchHoldPillButton(
                label = "LB",
                buttonKey = "LeftShoulder",
                width = 62.dp,
                height = 28.dp,
                accentColor = tokens.borderAccent,
            )
        }

        // 2. Top-Right Shoulder Cluster (RT / RB)
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End,
        ) {
            TouchHoldPillButton(
                label = "RT",
                buttonKey = "RightTrigger",
                width = 62.dp,
                height = 30.dp,
                accentColor = tokens.cloudCyan,
            )
            TouchHoldPillButton(
                label = "RB",
                buttonKey = "RightShoulder",
                width = 62.dp,
                height = 28.dp,
                accentColor = tokens.borderAccent,
            )
        }

        // 3. Top-Center Utility Buttons (View / Menu) flanking the Guide pill
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TouchHoldPillButton(
                label = "VIEW",
                buttonKey = "View",
                width = 48.dp,
                height = 24.dp,
                accentColor = tokens.borderStrong,
            )
            TouchHoldPillButton(
                label = "MENU",
                buttonKey = "Menu",
                width = 48.dp,
                height = 24.dp,
                accentColor = tokens.borderStrong,
            )
        }

        // 4. Bottom-Left Control Cluster: D-Pad + Left Analog Stick + L3
        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TouchHoldPillButton(
                    label = "L3",
                    buttonKey = "LeftThumb",
                    width = 42.dp,
                    height = 24.dp,
                    accentColor = tokens.borderAccent,
                )
                TouchAnalogThumbstick(
                    stickId = "left",
                    label = "LS",
                    diameterDp = 102.dp,
                    accentColor = tokens.accentPrimary,
                )
            }

            TouchDPadCluster()
        }

        // 5. Bottom-Right Control Cluster: Right Analog Stick + R3 + 2009 Xbox ABXY Diamond
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TouchHoldPillButton(
                    label = "R3",
                    buttonKey = "RightThumb",
                    width = 42.dp,
                    height = 24.dp,
                    accentColor = tokens.borderAccent,
                )
                TouchAnalogThumbstick(
                    stickId = "right",
                    label = "RS",
                    diameterDp = 102.dp,
                    accentColor = tokens.cloudCyan,
                )
            }

            TouchAbxyDiamond()
        }
    }
}

@Composable
private fun TouchDPadCluster() {
    val tokens = LocalNxeGlassTokens.current
    val btnSize = 36.dp
    Box(
        modifier = Modifier.size(114.dp),
        contentAlignment = Alignment.Center,
    ) {
        TouchHoldCircleButton(
            label = "▲",
            buttonKey = "DPadUp",
            size = btnSize,
            accentColor = tokens.borderAccent,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        TouchHoldCircleButton(
            label = "▼",
            buttonKey = "DPadDown",
            size = btnSize,
            accentColor = tokens.borderAccent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        TouchHoldCircleButton(
            label = "◀",
            buttonKey = "DPadLeft",
            size = btnSize,
            accentColor = tokens.borderAccent,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        TouchHoldCircleButton(
            label = "▶",
            buttonKey = "DPadRight",
            size = btnSize,
            accentColor = tokens.borderAccent,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun TouchAbxyDiamond() {
    val btnSize = 40.dp
    Box(
        modifier = Modifier.size(122.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Y (Top - Xbox Gold)
        TouchHoldCircleButton(
            label = "Y",
            buttonKey = "Y",
            size = btnSize,
            accentColor = Color(0xFFF59E0B),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        // B (Right - Xbox Crimson)
        TouchHoldCircleButton(
            label = "B",
            buttonKey = "B",
            size = btnSize,
            accentColor = Color(0xFFEF4444),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        // X (Left - Xbox Sapphire)
        TouchHoldCircleButton(
            label = "X",
            buttonKey = "X",
            size = btnSize,
            accentColor = Color(0xFF3B82F6),
            modifier = Modifier.align(Alignment.CenterStart),
        )
        // A (Bottom - Xbox Emerald)
        TouchHoldCircleButton(
            label = "A",
            buttonKey = "A",
            size = btnSize,
            accentColor = Color(0xFF10B981),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TouchHoldPillButton(
    label: String,
    buttonKey: String,
    width: Dp,
    height: Dp,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .alpha(if (isPressed) 0.78f else 0.24f)
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.36f)
                } else {
                    Color.Black.copy(alpha = 0.22f)
                },
            )
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                color = if (isPressed) accentColor else Color.White.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp),
            )
            .pointerInput(buttonKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    haptics?.tickBladeFocus()
                    XStreamingStateStore.sendVirtualGamepadInput(
                        """{"type":"button","button":"$buttonKey","pressed":true}""",
                    )
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        isPressed = false
                        XStreamingStateStore.sendVirtualGamepadInput(
                            """{"type":"button","button":"$buttonKey","pressed":false}""",
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TouchHoldCircleButton(
    label: String,
    buttonKey: String,
    size: Dp,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .alpha(if (isPressed) 0.82f else 0.26f)
            .size(size)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.42f)
                } else {
                    Color.Black.copy(alpha = 0.22f)
                },
            )
            .border(
                width = if (isPressed) 1.75.dp else 1.dp,
                color = if (isPressed) accentColor else accentColor.copy(alpha = 0.65f),
                shape = CircleShape,
            )
            .pointerInput(buttonKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    haptics?.tickBladeFocus()
                    XStreamingStateStore.sendVirtualGamepadInput(
                        """{"type":"button","button":"$buttonKey","pressed":true}""",
                    )
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        isPressed = false
                        XStreamingStateStore.sendVirtualGamepadInput(
                            """{"type":"button","button":"$buttonKey","pressed":false}""",
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textPrimary,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun TouchAnalogThumbstick(
    stickId: String,
    label: String,
    diameterDp: Dp,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val density = LocalDensity.current
    val maxRadiusPx = remember(diameterDp, density) {
        with(density) { (diameterDp.toPx() * 0.34f).coerceAtLeast(1f) }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .alpha(if (isActive) 0.76f else 0.22f)
            .size(diameterDp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.18f))
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) accentColor else Color.White.copy(alpha = 0.38f),
                shape = CircleShape,
            )
            .pointerInput(stickId, maxRadiusPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    isActive = true
                    haptics?.tickBladeFocus()

                    fun updateStickFromPosition(px: Float, py: Float) {
                        val rawDx = px - centerX
                        val rawDy = py - centerY
                        val dist = hypot(rawDx, rawDy)
                        val clampedScale = if (dist > maxRadiusPx && dist > 0f) {
                            maxRadiusPx / dist
                        } else {
                            1f
                        }
                        val clampedDx = rawDx * clampedScale
                        val clampedDy = rawDy * clampedScale
                        offsetX = clampedDx
                        offsetY = clampedDy

                        val normX = (clampedDx / maxRadiusPx).coerceIn(-1f, 1f)
                        // Standard screen-space Y axis (input.ts negates Y internally for Xbox protocol)
                        val normY = (clampedDy / maxRadiusPx).coerceIn(-1f, 1f)
                        XStreamingStateStore.sendVirtualGamepadInput(
                            String.format(
                                Locale.US,
                                """{"type":"stick","stick":"%s","x":%.3f,"y":%.3f}""",
                                stickId,
                                normX,
                                normY,
                            ),
                        )
                    }

                    updateStickFromPosition(down.position.x, down.position.y)

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                break
                            }
                            updateStickFromPosition(change.position.x, change.position.y)
                            change.consume()
                        }
                    } finally {
                        isActive = false
                        offsetX = 0f
                        offsetY = 0f
                        XStreamingStateStore.sendVirtualGamepadInput(
                            """{"type":"stick","stick":"$stickId","x":0.0,"y":0.0}""",
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(diameterDp * 0.44f)
                .clip(CircleShape)
                .background(
                    if (isActive) {
                        accentColor.copy(alpha = 0.55f)
                    } else {
                        tokens.glassChromeTop.copy(alpha = 0.72f)
                    },
                )
                .border(
                    width = 1.5.dp,
                    color = if (isActive) Color.White else accentColor.copy(alpha = 0.7f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HudActionTile(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Animated 2009 Xbox 360 Ring-of-Light + 3D Metallic Guide Sphere spinner.
 */
@Composable
private fun NxeRingOfLightOrb(
    primaryColor: Color,
    brightColor: Color,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nxe_rol_transition")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rol_rotation",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rol_pulse",
    )

    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Breathing outer halo glow
        Box(
            modifier = Modifier
                .size(82.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.18f))
                .border(1.dp, brightColor.copy(alpha = 0.38f), CircleShape),
        )

        // Rotating 4-Quadrant Ring of Light
        Canvas(
            modifier = Modifier
                .size(76.dp)
                .rotate(rotationDeg),
        ) {
            val strokePx = 4.2.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Dim ring track
            drawArc(
                color = brightColor.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )

            // 4 Xbox 360 Ring-of-Light Quadrants (with leading bright quadrant)
            val quadrantAngles = floatArrayOf(-78f, 12f, 102f, 192f)
            val quadrantAlphas = floatArrayOf(1.0f, 0.82f, 0.55f, 0.32f)
            for (i in quadrantAngles.indices) {
                drawArc(
                    color = if (i == 0) Color.White.copy(alpha = 0.92f) else brightColor.copy(alpha = quadrantAlphas[i]),
                    startAngle = quadrantAngles[i],
                    sweepAngle = 62f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        // Inner 3D Metallic Silver Xbox Guide Sphere
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFD6DEE6),
                            Color(0xFF84909E),
                            Color(0xFF4A5460),
                        ),
                    ),
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "X",
                style = MaterialTheme.typography.headlineSmall,
                color = primaryColor,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
