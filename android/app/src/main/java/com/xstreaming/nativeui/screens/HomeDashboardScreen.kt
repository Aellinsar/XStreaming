// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstreaming.nativeui.bridge.AuthStatus
import com.xstreaming.nativeui.bridge.NxeActiveModal
import com.xstreaming.nativeui.bridge.NxeDashboardChannel
import com.xstreaming.nativeui.bridge.XStreamingStateStore
import com.xstreaming.nativeui.bridge.XStreamingUiState
import com.xstreaming.nativeui.components.NxeGlassCard
import com.xstreaming.nativeui.components.NxeStudioBackdrop
import com.xstreaming.nativeui.components.nxeRootBackdropBlur
import com.xstreaming.nativeui.components.rememberNxeSystemBarInsets
import com.xstreaming.nativeui.core.UiEngineBootConfig
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.LocalNxeHaptics
import com.xstreaming.nativeui.theme.NxeTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 2008-2009 Xbox 360 New Xbox Experience (NXE) Home Stage (`HomeDashboardScreen`).
 *
 * Spatial architecture modeled directly after the November 2008 Xbox 360 NXE dashboard
 * with crisp rectangular beveled cards (`3.dp` corner radius):
 * - No web/Bootstrap top navbar; open lit studio sky and brushed reflective horizon floor.
 * - Receding 3D vertical channel hierarchy (`System Settings` -> `Achievements` -> `Cloud Gaming` -> `My Xbox`)
 *   anchored directly above the horizontal card row on widescreen displays.
 * - Top-right rectangular Xbox Live Gamercard strip + compact square Refresh & Theme icon tiles.
 * - Single cohesive horizontal row of 4 rectangular NXE cards sitting flush on the studio floor plane,
 *   each casting an aligned vertical mirror reflection onto the floor below.
 * - Bottom studio floor slot counter (`1 of 4`) and glossy circular 3D Xbox 360 controller button legend.
 */
@Composable
fun HomeDashboardScreen(
    state: XStreamingUiState,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val haptics = LocalNxeHaptics.current
    val (topInsetDp, bottomInsetDp) = rememberNxeSystemBarInsets()
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 600

    BackHandler(enabled = state.activeModal == null && state.activeChannel != NxeDashboardChannel.HOME) {
        haptics?.tickBack()
        XStreamingStateStore.selectChannel(NxeDashboardChannel.HOME)
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .nxeRootBackdropBlur(isModalOpen = state.activeModal != null),
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isWideLayout = isLandscape || screenWidth >= 600.dp
            val isCompactLandscape = isWideLayout && screenHeight < 500.dp
            val isWideCardStage = isWideLayout && (
                state.activeChannel == NxeDashboardChannel.HOME ||
                    state.activeChannel == NxeDashboardChannel.SETTINGS
                )
            val floorHorizonFraction = if (isCompactLandscape) 0.77f else 0.72f
            val horizontalPad = when {
                screenWidth >= 960.dp -> 44.dp
                screenWidth >= 600.dp -> 28.dp
                else -> 18.dp
            }

            // 1. 2008-2009 Xbox 360 NXE Lit Studio Wall + Reflective Horizon Floor
            NxeStudioBackdrop(
                showStudioFloorPlane = isWideCardStage,
                floorHorizonFraction = floorHorizonFraction,
            )

            if (isWideLayout && state.activeChannel == NxeDashboardChannel.HOME) {
                // Widescreen 2008-2009 NXE Horizontal Slot Carousel Stage (Pixel-Locked to Floor Horizon)
                NxeWidescreenMyXboxStage(
                    state = state,
                    screenHeight = screenHeight,
                    floorHorizonFraction = floorHorizonFraction,
                    topInsetDp = topInsetDp,
                    bottomInsetDp = bottomInsetDp,
                    horizontalPad = horizontalPad,
                    isCompactLandscape = isCompactLandscape,
                )
            } else if (isWideLayout && state.activeChannel == NxeDashboardChannel.SETTINGS) {
                // Widescreen 2008-2009 NXE Horizontal System Settings Card Stage
                NxeWidescreenSettingsStage(
                    state = state,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    floorHorizonFraction = floorHorizonFraction,
                    topInsetDp = topInsetDp,
                    bottomInsetDp = bottomInsetDp,
                    horizontalPad = horizontalPad,
                    isCompactLandscape = isCompactLandscape,
                )
            } else {
                // Sub-Channels (Cloud Gaming, Achievements) & Portrait Phone Stage
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topInsetDp + 10.dp),
                ) {
                    NxeSubChannelOrPortraitHeader(
                        state = state,
                        isCompactLandscape = isCompactLandscape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad),
                    )

                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 8.dp else 14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = horizontalPad, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (state.activeChannel) {
                            NxeDashboardChannel.HOME -> {
                                NxePortraitMyXboxColumn(state = state)
                            }
                            NxeDashboardChannel.CLOUD -> {
                                CloudGamingStageBlade(state = state, isWideTablet = isWideLayout)
                            }
                            NxeDashboardChannel.ACHIEVEMENTS -> {
                                AchievementsAndTelemetryStageBlade(state = state)
                            }
                            NxeDashboardChannel.SETTINGS -> {
                                NxePortraitSettingsColumn(state = state)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    NxeStudioFloorFooter(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = horizontalPad,
                                end = horizontalPad,
                                top = 6.dp,
                                bottom = bottomInsetDp + 6.dp,
                            ),
                    )
                }
            }
        }

        // Active Modal Overlay Host (Enforces 6 Lab 151 Modal Invariants)
        NxeActiveModalHost(state = state)
    }
}

/**
 * Full-bleed Widescreen 2008-2009 Xbox 360 NXE "My Xbox" Stage:
 * - Upper-right floating Xbox Live Gamercard pod.
 * - Left-aligned receding 3D channel hierarchy cascading directly down onto the Hero Console Card.
 * - 4 crisp rectangular NXE cards sitting flush on `screenHeight * floorHorizonFraction`.
 * - Aligned vertical mirror reflections fading into the studio floor below `floorHorizonFraction`.
 * - Bottom studio floor controller button legend and `1 of 4` slot counter.
 */
@Composable
private fun NxeWidescreenMyXboxStage(
    state: XStreamingUiState,
    screenHeight: Dp,
    floorHorizonFraction: Float,
    topInsetDp: Dp,
    bottomInsetDp: Dp,
    horizontalPad: Dp,
    isCompactLandscape: Boolean,
) {
    val horizonYDp = screenHeight * floorHorizonFraction
    val floorHeightDp = (screenHeight - horizonYDp).coerceAtLeast(80.dp)

    // Compute crisp NXE rectangular card dimensions proportional to the upper studio wall
    val heroCardHeight = if (isCompactLandscape) {
        (horizonYDp * 0.66f).coerceIn(156.dp, 214.dp)
    } else {
        (horizonYDp * 0.56f).coerceIn(230.dp, 332.dp)
    }
    val regularCardHeight = heroCardHeight * 0.89f
    val subtleFocusLift = if (isCompactLandscape) 6.dp else 10.dp
    val focusedStandardCardHeight = regularCardHeight + subtleFocusLift
    val reflectionHeight = if (isCompactLandscape) {
        (floorHeightDp * 0.40f).coerceIn(24.dp, 40.dp)
    } else {
        (floorHeightDp * 0.36f).coerceIn(48.dp, 82.dp)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Top-Right Floating Xbox 360 Gamercard Strip (and compact inline header on short phones)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = topInsetDp + (if (isCompactLandscape) 6.dp else 14.dp),
                    start = horizontalPad,
                    end = horizontalPad,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            if (isCompactLandscape) {
                NxeInlineChannelRow(activeChannel = state.activeChannel)
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            NxeGamercardPod(state = state)
        }

        // 2. Upper Wall Zone (Exact height = horizonYDp so cards sit pixel-flush on the floor horizon)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(horizonYDp)
                .padding(horizontal = horizontalPad),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Authentic 2008 NXE Receding Vertical Channel Stack perched right above the Hero Card
            if (!isCompactLandscape) {
                NxeRecedingChannelStack(
                    activeChannel = state.activeChannel,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }

            // Horizontal Row of 4 Crisp Rectangular NXE Slot Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 12.dp else 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Slot 0: Primary Xbox Console Remote Play Card
                NxeConsoleSlotCard(
                    state = state,
                    isSelectedSlot = state.selectedHomeSlotIndex == 0,
                    targetHeight = if (state.selectedHomeSlotIndex == 0) heroCardHeight else (heroCardHeight - subtleFocusLift),
                    isCompact = isCompactLandscape,
                    modifier = Modifier.weight(1.30f),
                )

                // Slot 1: Cloud Gaming Card
                NxeStandardSlotCard(
                    slotIndex = 1,
                    isSelectedSlot = state.selectedHomeSlotIndex == 1,
                    title = "Cloud Gaming",
                    subtitle = if (state.cloudTitles.isNotEmpty()) {
                        "${state.cloudTitles.size} Game Pass Titles"
                    } else {
                        "Xbox Game Pass"
                    },
                    targetHeight = if (state.selectedHomeSlotIndex == 1) focusedStandardCardHeight else regularCardHeight,
                    isCompact = isCompactLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        XStreamingStateStore.selectHomeSlot(1)
                        XStreamingStateStore.selectChannel(NxeDashboardChannel.CLOUD)
                    },
                ) {
                    NxeControllerStageArtwork()
                }

                // Slot 2: Achievements & Session Telemetry Card
                val lastReport = state.lastSessionReport
                NxeStandardSlotCard(
                    slotIndex = 2,
                    isSelectedSlot = state.selectedHomeSlotIndex == 2,
                    title = "Achievements",
                    subtitle = when {
                        state.totalCurrentGamerscore > 0 -> "${state.totalCurrentGamerscore} G Unlocked"
                        lastReport != null -> "Last: ${lastReport.avgRttMs} ms RTT"
                        else -> "Gamerscore & Stats"
                    },
                    targetHeight = if (state.selectedHomeSlotIndex == 2) focusedStandardCardHeight else regularCardHeight,
                    isCompact = isCompactLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        XStreamingStateStore.selectHomeSlot(2)
                        XStreamingStateStore.selectChannel(NxeDashboardChannel.ACHIEVEMENTS)
                    },
                ) {
                    NxeTrophyStageArtwork(gamerscore = state.totalCurrentGamerscore)
                }

                // Slot 3: System Settings & Theme Studio Card
                val resTag = if (state.settings.resolution >= 1080) "1080p" else "720p"
                val fsrTag = if (state.settings.fsrEnabled) "FSR" else "Raw"
                NxeStandardSlotCard(
                    slotIndex = 3,
                    isSelectedSlot = state.selectedHomeSlotIndex == 3,
                    title = "System Settings",
                    subtitle = "Display, Audio & Themes",
                    targetHeight = if (state.selectedHomeSlotIndex == 3) focusedStandardCardHeight else regularCardHeight,
                    isCompact = isCompactLandscape,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        XStreamingStateStore.selectHomeSlot(3)
                        XStreamingStateStore.selectChannel(NxeDashboardChannel.SETTINGS)
                    },
                ) {
                    NxeSettingsStageArtwork(badgeText = "$resTag · $fsrTag")
                }
            }
        }

        // 3. Lower Reflective Studio Floor Zone (Starts right at y = horizonYDp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(floorHeightDp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = horizontalPad),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Aligned Mirror Reflections directly under all 4 cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 12.dp else 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                NxeCardFloorReflection(
                    isSelected = state.selectedHomeSlotIndex == 0,
                    height = reflectionHeight,
                    modifier = Modifier.weight(1.30f),
                )
                NxeCardFloorReflection(
                    isSelected = state.selectedHomeSlotIndex == 1,
                    height = reflectionHeight,
                    modifier = Modifier.weight(1f),
                )
                NxeCardFloorReflection(
                    isSelected = state.selectedHomeSlotIndex == 2,
                    height = reflectionHeight,
                    modifier = Modifier.weight(1f),
                )
                NxeCardFloorReflection(
                    isSelected = state.selectedHomeSlotIndex == 3,
                    height = reflectionHeight,
                    modifier = Modifier.weight(1f),
                )
            }

            // Bottom NXE Controller Legend & Slot Counter (`1 of 4`)
            NxeStudioFloorFooter(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomInsetDp + (if (isCompactLandscape) 4.dp else 10.dp)),
            )
        }
    }
}

private data class SettingsCardSpec(
    val title: String,
    val subtitle: String,
    val modal: NxeActiveModal,
    val artwork: @Composable BoxScope.() -> Unit,
)

@Composable
private fun rememberSettingsCardSpecs(state: XStreamingUiState): List<SettingsCardSpec> {
    val tokens = LocalNxeGlassTokens.current
    val s = state.settings
    val resLabel = if (s.resolution >= 1080) "1080p HQ" else "720p"
    val bitrateLabel = if (s.xhomeBitrateMode.equals("auto", ignoreCase = true)) {
        "Auto Bitrate"
    } else {
        "${s.xhomeBitrateMbps} Mbps"
    }
    val isSignedOut = state.authStatus == AuthStatus.UNAUTHENTICATED && state.consoles.isEmpty()
    val accountSubtitle = when {
        isSignedOut -> "Not Signed In"
        state.gamertagName.isNotEmpty() -> state.gamertagName
        else -> "Signed In"
    }

    return buildList {
        add(
            SettingsCardSpec(
                title = "Appearance",
                subtitle = "${tokens.presetName} Theme",
                modal = NxeActiveModal.SETTINGS_APPEARANCE,
            ) {
                NxeAppearanceStageArtwork(
                    primaryColor = tokens.accentPrimary,
                    secondaryColor = tokens.accentSecondary,
                )
            },
        )
        add(
            SettingsCardSpec(
                title = "Display",
                subtitle = "$resLabel · $bitrateLabel",
                modal = NxeActiveModal.SETTINGS_DISPLAY,
            ) {
                NxeSettingsStageArtwork(badgeText = "")
            },
        )
        add(
            SettingsCardSpec(
                title = "Audio",
                subtitle = if (s.stereoAudioEnabled) "Stereo Output" else "Mono Output",
                modal = NxeActiveModal.SETTINGS_AUDIO,
            ) {
                NxeAudioStageArtwork()
            },
        )
        add(
            SettingsCardSpec(
                title = "Controller",
                subtitle = if (s.rumbleEnabled) "Rumble & Input" else "Input & Touch HUD",
                modal = NxeActiveModal.SETTINGS_CONTROLLER,
            ) {
                NxeGamepadSettingsStageArtwork()
            },
        )
        add(
            SettingsCardSpec(
                title = "Network & Console",
                subtitle = if (s.preferIpv6) "IPv6 + Auto-Wake" else "LAN IPv4 Priority",
                modal = NxeActiveModal.SETTINGS_NETWORK,
            ) {
                NxeNetworkStageArtwork()
            },
        )
        add(
            SettingsCardSpec(
                title = "Xbox Account",
                subtitle = accountSubtitle,
                modal = NxeActiveModal.SETTINGS_ACCOUNT,
            ) {
                NxeAccountStageArtwork(
                    gamerpicUrl = state.gamerpicUrl,
                    isSignedIn = !isSignedOut,
                )
            },
        )
        add(
            SettingsCardSpec(
                title = "About XStreaming",
                subtitle = "v2.12.0-NXE · AGPLv3",
                modal = NxeActiveModal.ABOUT,
            ) {
                NxeAboutStageArtwork()
            },
        )
        if (s.isDeveloperModeAvailable) {
            add(
                SettingsCardSpec(
                    title = "Developer Options",
                    subtitle = if (s.isDeveloperModeEnabled) "Active · Fuchsia" else "Disabled",
                    modal = NxeActiveModal.SETTINGS_DEVELOPER,
                ) {
                    NxeDeveloperStageArtwork()
                },
            )
        }
    }
}

/**
 * Widescreen 2008-2009 Xbox 360 NXE Horizontal System Settings Card Stage.
 * Uses the exact same studio floor horizon, card height, and aligned vertical mirror reflections as `My Xbox`.
 */
@Composable
private fun NxeWidescreenSettingsStage(
    state: XStreamingUiState,
    screenWidth: Dp,
    screenHeight: Dp,
    floorHorizonFraction: Float,
    topInsetDp: Dp,
    bottomInsetDp: Dp,
    horizontalPad: Dp,
    isCompactLandscape: Boolean,
) {
    val horizonYDp = screenHeight * floorHorizonFraction
    val floorHeightDp = (screenHeight - horizonYDp).coerceAtLeast(80.dp)

    val heroCardHeight = if (isCompactLandscape) {
        (horizonYDp * 0.66f).coerceIn(156.dp, 214.dp)
    } else {
        (horizonYDp * 0.56f).coerceIn(230.dp, 332.dp)
    }
    val regularCardHeight = heroCardHeight * 0.89f
    val subtleFocusLift = if (isCompactLandscape) 6.dp else 10.dp
    val focusedStandardCardHeight = regularCardHeight + subtleFocusLift
    val reflectionHeight = if (isCompactLandscape) {
        (floorHeightDp * 0.40f).coerceIn(24.dp, 40.dp)
    } else {
        (floorHeightDp * 0.36f).coerceIn(48.dp, 82.dp)
    }

    val cardSpacing = if (isCompactLandscape) 12.dp else 16.dp
    val availableRowWidth = (screenWidth - horizontalPad * 2 - cardSpacing * 3).coerceAtLeast(560.dp)
    val cardWidth = (availableRowWidth / 4.22f).coerceIn(196.dp, 272.dp)

    val haptics = LocalNxeHaptics.current
    val cards = rememberSettingsCardSpecs(state)
    val listState = rememberLazyListState()
    var lastScrollDrivenSlot by remember { mutableStateOf(-1) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Horizontal touch/drag scroll -> smoothly updates the emphasized card & bottom slot counter
    LaunchedEffect(listState, cards.size) {
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }.collect { (scrolling, firstIdx, firstOffset) ->
            if (!scrolling || isProgrammaticScroll || cards.isEmpty()) return@collect
            val layoutInfo = listState.layoutInfo
            val sampleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@collect
            val viewportW = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
            val focusedIdx = XStreamingStateStore.computeHorizontalCarouselFocusIndex(
                totalItemsCount = cards.size,
                firstVisibleItemIndex = firstIdx,
                firstVisibleItemScrollOffsetPx = firstOffset,
                itemSizePx = sampleItem.size,
                itemSpacingPx = layoutInfo.mainAxisItemSpacing,
                viewportWidthPx = viewportW,
                horizontalContentPaddingPx = layoutInfo.beforeContentPadding,
            )
            if (focusedIdx != XStreamingStateStore.state.value.selectedSettingsSlotIndex) {
                lastScrollDrivenSlot = focusedIdx
                XStreamingStateStore.selectSettingsSlot(focusedIdx)
                haptics?.tickBladeFocus()
            }
        }
    }

    // Controller D-Pad / Profile tap / edge-card tap -> scrolls card into view without fighting touch scroll
    LaunchedEffect(state.selectedSettingsSlotIndex, cards.size) {
        if (cards.isEmpty()) return@LaunchedEffect
        val target = state.selectedSettingsSlotIndex.coerceIn(0, cards.lastIndex)
        if (target == lastScrollDrivenSlot) {
            return@LaunchedEffect
        }
        lastScrollDrivenSlot = -1
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset
        val isFullyVisible = visibleItems.any { info ->
            info.index == target &&
                info.offset >= viewportStart &&
                (info.offset + info.size) <= viewportEnd
        }
        if (!isFullyVisible) {
            isProgrammaticScroll = true
            try {
                listState.animateScrollToItem((target - 1).coerceAtLeast(0))
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Top-Right Floating Xbox 360 Gamercard Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = topInsetDp + (if (isCompactLandscape) 6.dp else 14.dp),
                    start = horizontalPad,
                    end = horizontalPad,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            if (isCompactLandscape) {
                NxeInlineChannelRow(activeChannel = state.activeChannel)
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            NxeGamercardPod(state = state)
        }

        // 2. Receding NXE Channel Stack perched at the exact same Y coordinate as `My Xbox`
        if (!isCompactLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((horizonYDp - heroCardHeight).coerceAtLeast(0.dp))
                    .padding(horizontal = horizontalPad),
                contentAlignment = Alignment.BottomStart,
            ) {
                NxeRecedingChannelStack(
                    activeChannel = state.activeChannel,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
        }

        // 3. Horizontal Row of NXE Settings Cards Sitting Pixel-Flush on `horizonYDp` + Paired Floor Reflections
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = (horizonYDp - heroCardHeight).coerceAtLeast(0.dp)),
            contentPadding = PaddingValues(horizontal = horizontalPad),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        ) {
            itemsIndexed(cards) { index, spec ->
                val isSelected = state.selectedSettingsSlotIndex == index
                Column(modifier = Modifier.width(cardWidth)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroCardHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        NxeStandardSlotCard(
                            slotIndex = index,
                            isSelectedSlot = isSelected,
                            title = spec.title,
                            subtitle = spec.subtitle,
                            targetHeight = if (isSelected) focusedStandardCardHeight else regularCardHeight,
                            isCompact = isCompactLandscape,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                lastScrollDrivenSlot = -1
                                XStreamingStateStore.selectSettingsSlot(index)
                                XStreamingStateStore.openModal(spec.modal)
                            },
                            artwork = spec.artwork,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    NxeCardFloorReflection(
                        isSelected = isSelected,
                        height = reflectionHeight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 4. Bottom NXE Studio Floor Controller Legend (`1 of 7`)
        NxeStudioFloorFooter(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    bottom = bottomInsetDp + (if (isCompactLandscape) 4.dp else 10.dp),
                ),
        )
    }
}

/**
 * Authentic 2008-2009 Xbox 360 NXE Receding Vertical Channel Stack.
 * Inactive channels recede upward in Z-depth (smaller font + softer alpha + slight perspective step),
 * culminating in the bold foreground active channel header directly above the card row.
 */
@Composable
private fun NxeRecedingChannelStack(
    activeChannel: NxeDashboardChannel,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalNxeHaptics.current
    val tokens = LocalNxeGlassTokens.current
    val allChannels = NxeDashboardChannel.values()
    val activeIdx = allChannels.indexOf(activeChannel).coerceAtLeast(0)

    // Order from furthest back (top) -> closest inactive -> active foreground (bottom)
    val distant3 = allChannels[(activeIdx + 3) % allChannels.size]
    val distant2 = allChannels[(activeIdx + 2) % allChannels.size]
    val distant1 = allChannels[(activeIdx + 1) % allChannels.size]

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        RecedingChannelItem(
            label = distant3.label,
            fontSizeSp = 13f,
            alpha = 0.38f,
            indentDp = 14.dp,
            onClick = {
                haptics?.thudBumper()
                XStreamingStateStore.selectChannel(distant3)
            },
        )
        RecedingChannelItem(
            label = distant2.label,
            fontSizeSp = 15.5f,
            alpha = 0.56f,
            indentDp = 9.dp,
            onClick = {
                haptics?.thudBumper()
                XStreamingStateStore.selectChannel(distant2)
            },
        )
        RecedingChannelItem(
            label = distant1.label,
            fontSizeSp = 18.5f,
            alpha = 0.76f,
            indentDp = 4.dp,
            onClick = {
                haptics?.thudBumper()
                XStreamingStateStore.selectChannel(distant1)
            },
        )

        // Active Foreground NXE Channel Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White, tokens.accentSecondary),
                        ),
                    ),
            )
            Text(
                text = activeChannel.label,
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecedingChannelItem(
    label: String,
    fontSizeSp: Float,
    alpha: Float,
    indentDp: Dp,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = Color.White.copy(alpha = alpha),
        fontSize = fontSizeSp.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = indentDp)
            .clip(RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@Composable
private fun NxeInlineChannelRow(activeChannel: NxeDashboardChannel) {
    val haptics = LocalNxeHaptics.current
    val tokens = LocalNxeGlassTokens.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NxeDashboardChannel.values().forEach { channel ->
            val isSelected = channel == activeChannel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable {
                        haptics?.thudBumper()
                        XStreamingStateStore.selectChannel(channel)
                    }
                    .padding(horizontal = 4.dp, vertical = 3.dp),
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(tokens.accentSecondary),
                    )
                }
                Text(
                    text = channel.label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.56f),
                    fontSize = if (isSelected) 20.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NxeSubChannelOrPortraitHeader(
    state: XStreamingUiState,
    isCompactLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            if (isCompactLandscape) {
                NxeInlineChannelRow(activeChannel = state.activeChannel)
            } else {
                NxeRecedingChannelStack(activeChannel = state.activeChannel)
            }
        }
        NxeGamercardPod(state = state)
    }
}

/**
 * 2008-2009 Xbox 360 Top-Right Rectangular Gamercard (True Edge-to-Edge Left Gamerpic) + Refresh Tile.
 */
@Composable
private fun NxeGamercardPod(state: XStreamingUiState) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Single Clean Rectangular Xbox Live Gamercard:
        // Gamerpic occupies 100% of top/left/bottom edge with zero 1dp outer border inset.
        Row(
            modifier = Modifier
                .height(46.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    haptics?.clickConfirm()
                    XStreamingStateStore.openAccountSettings()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NxeGamerpicAvatarBox(
                gamerpicUrl = state.gamerpicUrl,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
            )

            val isSignedOut = state.authStatus == AuthStatus.UNAUTHENTICATED && state.consoles.isEmpty()
            val primaryName = state.gamertagName
            val suffixTag = state.gamertagSuffix
            val subLineText = when {
                suffixTag.isNotEmpty() -> suffixTag
                primaryName.isNotEmpty() && state.totalCurrentGamerscore > 0 -> "${state.totalCurrentGamerscore} G"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                tokens.bgSurfaceElevated.copy(alpha = 0.90f),
                                tokens.bgSurface.copy(alpha = 0.96f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color(0xFF94A3B8).copy(alpha = 0.08f),
                            ),
                        ),
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    )
                    .padding(start = 11.dp, end = 13.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = when {
                            primaryName.isNotEmpty() -> primaryName
                            isSignedOut -> "Sign In"
                            else -> "Signed In"
                        },
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 13.5.sp,
                            lineHeight = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subLineText.isNotEmpty()) {
                        Text(
                            text = subLineText,
                            color = Color(0xFF94A3B8),
                            style = TextStyle(
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Medium,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Compact Square Icon Tile: Refresh Consoles
        NxeSquareIconTile(
            contentDescription = "Refresh",
            onClick = {
                haptics?.tickBladeFocus()
                XStreamingStateStore.emitCommandToJs("refreshConsoles")
            },
        ) {
            NxeRefreshIconCanvas(modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun NxeSquareIconTile(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    Box(
        modifier = Modifier
            .size(46.dp)
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color(0xFF94A3B8).copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable(onClick = onClick)
            .padding(1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tokens.bgSurfaceElevated.copy(alpha = 0.90f),
                            tokens.bgSurface.copy(alpha = 0.96f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * Slot 0: Primary Rectangular 2008-2009 NXE Console Remote Play Card.
 */
@Composable
private fun NxeConsoleSlotCard(
    state: XStreamingUiState,
    isSelectedSlot: Boolean,
    targetHeight: Dp,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val selectedConsole = state.selectedConsole
    val isSignedOut = state.authStatus == AuthStatus.UNAUTHENTICATED && state.consoles.isEmpty()

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nxeConsoleSlotHeight",
    )

    val shouldWakeFirst = selectedConsole != null && !selectedConsole.isPoweredOn && state.settings.autoWakeConsole

    NxeRectangularSlotFrame(
        isFocusedSlot = isSelectedSlot,
        modifier = modifier.height(animatedHeight),
        onClick = {
            XStreamingStateStore.selectHomeSlot(0)
            haptics?.clickConfirm()
            when {
                isSignedOut -> XStreamingStateStore.emitCommandToJs("startWebLogin")
                selectedConsole != null -> {
                    XStreamingStateStore.emitCommandToJs(
                        "startConsoleStream",
                        """{"serverId":"${selectedConsole.serverId}","powerOnFirst":$shouldWakeFirst}""",
                    )
                }
                else -> XStreamingStateStore.emitCommandToJs("refreshConsoles")
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Upper ~70% Glossy Artwork Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                NxeConsoleStageArtwork(
                    isPoweredOn = selectedConsole?.isPoweredOn == true,
                )

                // Minimal Top Status Row (Clean NXE overlay, zero nested dark boxes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSignedOut -> Color(0xFFF59E0B)
                                        selectedConsole?.isPoweredOn == true -> Color(0xFF6CC644)
                                        else -> Color(0xFF93C5FD)
                                    },
                                ),
                        )
                        Text(
                            text = when {
                                isSignedOut -> "Sign In Required"
                                selectedConsole == null -> "Searching..."
                                selectedConsole.isPoweredOn -> "Powered On"
                                else -> "Standby"
                            },
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }

                    if (state.consoles.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Black.copy(alpha = 0.40f))
                                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(2.dp))
                                .clickable {
                                    haptics?.tickBladeFocus()
                                    val nextIdx = (state.selectedConsoleIndex + 1) % state.consoles.size
                                    XStreamingStateStore.selectConsoleIndex(nextIdx)
                                }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${state.selectedConsoleIndex + 1} / ${state.consoles.size} ▸",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else if (isSignedOut) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Black.copy(alpha = 0.42f))
                                .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(2.dp))
                                .clickable {
                                    haptics?.clickConfirm()
                                    XStreamingStateStore.emitCommandToJs("startMsalLogin")
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "Device Code",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Subtle 1dp Horizontal Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )

            // Lower ~30% Brushed NXE Title Band
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A1931).copy(alpha = 0.88f),
                                Color(0xFF050D1C).copy(alpha = 0.96f),
                            ),
                        ),
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (isCompact) 8.dp else 12.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = when {
                            isSignedOut -> "Connect Xbox"
                            selectedConsole != null -> selectedConsole.name
                            else -> "Discover Consoles"
                        },
                        color = Color.White,
                        fontSize = if (isCompact) 16.sp else 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            isSignedOut -> "1-Tap Xbox Live Sign-In"
                            shouldWakeFirst -> "Wake & Play Remote Console"
                            selectedConsole != null -> "Play Remote Console"
                            else -> state.authStatusMessage
                        },
                        color = tokens.textSecondary,
                        fontSize = if (isCompact) 11.sp else 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Authentic Glossy 3D Green Xbox 360 (A) Sphere Button
                NxeXboxSphereBadge(
                    letter = "A",
                    baseColor = Color(0xFF107C10),
                    highlightColor = Color(0xFF7CE84A),
                    sizeDp = if (isCompact) 26.dp else 30.dp,
                )
            }
        }
    }
}

/**
 * Slots 1-3: Cohesive Rectangular 2008-2009 NXE Channel Slot Card (`3.dp` corners, unified brushed silver & glass).
 */
@Composable
private fun NxeStandardSlotCard(
    slotIndex: Int,
    isSelectedSlot: Boolean,
    title: String,
    subtitle: String,
    targetHeight: Dp,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nxeSlotHeight_$slotIndex",
    )
    val focusAlpha by animateFloatAsState(
        targetValue = if (isSelectedSlot) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nxeStandardCardFocus_$slotIndex",
    )

    NxeRectangularSlotFrame(
        isFocusedSlot = isSelectedSlot,
        modifier = modifier.height(animatedHeight),
        onClick = {
            haptics?.clickConfirm()
            onClick()
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Upper Artwork Stage (calm 0.80 contrast when unfocused -> crisp 1.00 when focused)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.80f + 0.20f * focusAlpha },
                contentAlignment = Alignment.Center,
                content = artwork,
            )

            // Luminous 1dp Horizontal Divider (illuminates gently on the focused card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                tokens.accentSecondary.copy(alpha = 0.08f + 0.48f * focusAlpha),
                                Color.White.copy(alpha = 0.08f + 0.30f * focusAlpha),
                                Color.White.copy(alpha = 0.06f + 0.10f * focusAlpha),
                            ),
                        ),
                    ),
            )

            // Lower Brushed Title Band with subtle (A) action badge on focused card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0C203E).copy(alpha = 0.84f + 0.10f * focusAlpha),
                                Color(0xFF050D1C).copy(alpha = 0.95f),
                            ),
                        ),
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = if (isCompact) 8.dp else 12.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (focusAlpha > 0.05f) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.88f + 0.12f * focusAlpha),
                        fontSize = if (isCompact) 14.5.sp else 16.5.sp,
                        fontWeight = if (isSelectedSlot) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        color = tokens.textSecondary.copy(alpha = 0.82f + 0.18f * focusAlpha),
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (focusAlpha > 0.05f) {
                    Box(modifier = Modifier.graphicsLayer { alpha = focusAlpha }) {
                        NxeXboxSphereBadge(
                            letter = "A",
                            baseColor = Color(0xFF107C10),
                            highlightColor = Color(0xFF7CE84A),
                            sizeDp = if (isCompact) 20.dp else 24.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared 2008-2009 Xbox 360 NXE Crisp Rectangular Card Frame:
 * - Concentric unclipped integer-DP metallic bevel (`4.dp` outer -> `1.dp` bevel -> `3.dp` inner)
 *   so every horizontal, vertical, and corner edge renders with full Skia coverage anti-aliasing and zero
 *   half-clipped sub-pixel stroke artifacts.
 * - Upper curved Frutiger-Aero / NXE glass highlight arc across the artwork window.
 */
@Composable
private fun NxeRectangularSlotFrame(
    isFocusedSlot: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val outerShape = RoundedCornerShape(4.dp)
    val innerShape = RoundedCornerShape(3.dp)
    val bevelPadding = 1.dp
    val interactionSource = remember { MutableInteractionSource() }

    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocusedSlot) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nxeSlotFocusAlpha",
    )

    val bevelBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f + 0.42f * focusAlpha),
            tokens.accentSecondary.copy(alpha = 0.06f + 0.30f * focusAlpha),
            Color(0xFF94A3B8).copy(alpha = 0.05f + 0.14f * focusAlpha),
        ),
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = (9.dp + 13.dp * focusAlpha).toPx()
                shape = outerShape
                clip = false
            }
            .clip(outerShape)
            .background(bevelBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(bevelPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(innerShape),
        ) {
            // 1. Cohesive Lit NXE Glass Background + Curved Upper Specular Gloss Arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val topTint = if (isFocusedSlot) {
                    tokens.bgStageMid
                } else {
                    tokens.bgSurfaceElevated.copy(alpha = 0.88f)
                }
                val bottomTint = tokens.bgSurface.copy(alpha = 0.96f)

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(topTint, bottomTint),
                    ),
                )

                // Subtle interior studio spotlight behind artwork
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f + focusAlpha * 0.13f),
                            tokens.accentSecondary.copy(alpha = 0.08f + focusAlpha * 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.50f, h * 0.38f),
                        radius = w * 0.70f,
                    ),
                )

                // Signature 2008 Xbox 360 NXE Curved Upper Glass Reflection Pane
                val glossPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.24f)
                    quadraticBezierTo(w * 0.46f, h * 0.42f, 0f, h * 0.50f)
                    close()
                }
                drawPath(
                    path = glossPath,
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.15f + focusAlpha * 0.10f),
                        1.0f to Color.White.copy(alpha = 0.02f),
                    ),
                )
            }

            // 2. Foreground Artwork & Title Band Content
            content()
        }
    }
}

/**
 * Borderless Fading Vertical Mirror Reflection beneath each rectangular NXE card on the brushed studio floor.
 */
@Composable
private fun NxeCardFloorReflection(
    isSelected: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val focusAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nxeReflectionFocus",
    )
    val topAlpha = 0.14f + 0.20f * focusAlpha

    Canvas(
        modifier = modifier
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Smoothly fade reflection into the studio floor with zero hard box border
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Black,
                        0.55f to Color.Black.copy(alpha = 0.18f),
                        1.0f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val cr = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.07f + 0.18f * focusAlpha),
                    tokens.accentSecondary.copy(alpha = 0.03f + 0.14f * focusAlpha),
                    Color(0xFF0A1931).copy(alpha = topAlpha + 0.08f),
                    tokens.bgSurfaceElevated.copy(alpha = topAlpha * 0.5f),
                ),
            ),
            cornerRadius = cr,
        )
    }
}

/**
 * Portrait Phone Layout for "My Xbox" (Clean Rectangular Hero Console Card + 3 Cohesive NXE Rows).
 */
@Composable
private fun NxePortraitMyXboxColumn(state: XStreamingUiState) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NxeConsoleSlotCard(
            state = state,
            isSelectedSlot = true,
            targetHeight = 224.dp,
            isCompact = false,
            modifier = Modifier.fillMaxWidth(),
        )

        NxePortraitSlotRow(
            title = "Cloud Gaming",
            subtitle = if (state.cloudTitles.isNotEmpty()) {
                "${state.cloudTitles.size} Xbox Game Pass Titles"
            } else {
                "Xbox Game Pass Library"
            },
            onClick = {
                haptics?.clickConfirm()
                XStreamingStateStore.selectChannel(NxeDashboardChannel.CLOUD)
            },
        ) {
            NxeControllerStageArtwork()
        }

        val lastReport = state.lastSessionReport
        NxePortraitSlotRow(
            title = "Achievements",
            subtitle = when {
                state.totalCurrentGamerscore > 0 -> "${state.totalCurrentGamerscore} G Unlocked"
                lastReport != null -> "Last Session: ${lastReport.avgRttMs} ms RTT"
                else -> "Gamerscore & Session Diagnostics"
            },
            onClick = {
                haptics?.clickConfirm()
                XStreamingStateStore.selectChannel(NxeDashboardChannel.ACHIEVEMENTS)
            },
        ) {
            NxeTrophyStageArtwork(gamerscore = 0)
        }

        val resTag = if (state.settings.resolution >= 1080) "1080p" else "720p"
        NxePortraitSlotRow(
            title = "System Settings",
            subtitle = "Display ($resTag), Audio & Themes (${tokens.presetName})",
            onClick = {
                haptics?.clickConfirm()
                XStreamingStateStore.selectChannel(NxeDashboardChannel.SETTINGS)
            },
        ) {
            NxeSettingsStageArtwork(badgeText = "")
        }
    }
}

/**
 * Portrait Phone Layout for "System Settings" (Cohesive NXE Category Cards).
 */
@Composable
private fun NxePortraitSettingsColumn(state: XStreamingUiState) {
    val haptics = LocalNxeHaptics.current
    val cards = rememberSettingsCardSpecs(state)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEachIndexed { index, spec ->
            NxePortraitSlotRow(
                title = spec.title,
                subtitle = spec.subtitle,
                onClick = {
                    haptics?.clickConfirm()
                    XStreamingStateStore.selectSettingsSlot(index)
                    XStreamingStateStore.openModal(spec.modal)
                },
                artwork = spec.artwork,
            )
        }
    }
}

@Composable
private fun NxePortraitSlotRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    NxeRectangularSlotFrame(
        isFocusedSlot = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
                content = artwork,
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = tokens.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "▸",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 14.dp),
            )
        }
    }
}

/**
 * Bottom Studio Floor Controller Legend (`1 of 4`, glossy 3D `(A) Select`, `(Y) Refresh`)
 * and subtle `v2.12.0-NXE` 7-tap developer mode badge.
 */
@Composable
private fun NxeStudioFloorFooter(
    state: XStreamingUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val settingsCount = XStreamingStateStore.settingsSlotCount(state.settings.isDeveloperModeAvailable)
    val slotCounterLabel = when (state.activeChannel) {
        NxeDashboardChannel.HOME -> "${state.selectedHomeSlotIndex + 1} of 4"
        NxeDashboardChannel.SETTINGS -> "${(state.selectedSettingsSlotIndex + 1).coerceAtMost(settingsCount)} of $settingsCount"
        else -> null
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (slotCounterLabel != null) {
                Text(
                    text = slotCounterLabel,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.25f)),
                )
            }

            // (A) Select / Play
            NxeFloorButtonLegend(
                letter = "A",
                baseColor = Color(0xFF107C10),
                highlightColor = Color(0xFF7CE84A),
                label = if (state.activeChannel == NxeDashboardChannel.HOME && state.selectedHomeSlotIndex == 0) {
                    "Play"
                } else {
                    "Select"
                },
                onClick = {
                    haptics?.clickConfirm()
                    when (state.activeChannel) {
                        NxeDashboardChannel.HOME -> {
                            when (state.selectedHomeSlotIndex) {
                                0 -> {
                                    val console = state.selectedConsole
                                    if (state.authStatus == AuthStatus.UNAUTHENTICATED && state.consoles.isEmpty()) {
                                        XStreamingStateStore.emitCommandToJs("startWebLogin")
                                    } else if (console != null) {
                                        val wake = !console.isPoweredOn && state.settings.autoWakeConsole
                                        XStreamingStateStore.emitCommandToJs(
                                            "startConsoleStream",
                                            """{"serverId":"${console.serverId}","powerOnFirst":$wake}""",
                                        )
                                    }
                                }
                                1 -> XStreamingStateStore.selectChannel(NxeDashboardChannel.CLOUD)
                                2 -> XStreamingStateStore.selectChannel(NxeDashboardChannel.ACHIEVEMENTS)
                                3 -> XStreamingStateStore.selectChannel(NxeDashboardChannel.SETTINGS)
                            }
                        }
                        NxeDashboardChannel.SETTINGS -> {
                            XStreamingStateStore.openModal(
                                XStreamingStateStore.modalForSettingsSlot(
                                    state.selectedSettingsSlotIndex,
                                    state.settings.isDeveloperModeAvailable,
                                ),
                            )
                        }
                        else -> Unit
                    }
                },
            )

            // (Y) Refresh (shown on data channels)
            if (state.activeChannel != NxeDashboardChannel.SETTINGS) {
                NxeFloorButtonLegend(
                    letter = "Y",
                    baseColor = Color(0xFFD97706),
                    highlightColor = Color(0xFFFDE047),
                    label = "Refresh",
                    onClick = {
                        haptics?.tickBladeFocus()
                        when (state.activeChannel) {
                            NxeDashboardChannel.CLOUD -> XStreamingStateStore.emitCommandToJs("refreshCloudTitles")
                            NxeDashboardChannel.ACHIEVEMENTS -> XStreamingStateStore.emitCommandToJs("refreshAchievements")
                            else -> XStreamingStateStore.emitCommandToJs("refreshConsoles")
                        }
                    },
                )
            }

            if (state.activeChannel != NxeDashboardChannel.HOME) {
                NxeFloorButtonLegend(
                    letter = "B",
                    baseColor = Color(0xFFB91C1C),
                    highlightColor = Color(0xFFF87171),
                    label = "My Xbox",
                    onClick = {
                        haptics?.tickBack()
                        XStreamingStateStore.selectChannel(NxeDashboardChannel.HOME)
                    },
                )
            }
        }

        // Subtle Floor Version Watermark (7 taps unlocks Two-Tier Fuchsia Developer Mode)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(
                    if (state.settings.isDeveloperUiActive) tokens.devBg else Color.Transparent,
                )
                .border(
                    width = 1.dp,
                    color = if (state.settings.isDeveloperUiActive) tokens.devBorder else Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(3.dp),
                )
                .clickable {
                    haptics?.tickBladeFocus()
                    val unlockedNow = XStreamingStateStore.recordVersionBadgeTap()
                    if (unlockedNow) {
                        UiEngineBootConfig.setDeveloperModeEnabled(context, true)
                        XStreamingStateStore.selectChannel(NxeDashboardChannel.SETTINGS)
                    }
                }
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (state.settings.isDeveloperUiActive) "v2.12.0-NXE · DEV" else "v2.12.0-NXE",
                color = if (state.settings.isDeveloperUiActive) tokens.devText else Color.White.copy(alpha = 0.52f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NxeFloorButtonLegend(
    letter: String,
    baseColor: Color,
    highlightColor: Color,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        NxeXboxSphereBadge(
            letter = letter,
            baseColor = baseColor,
            highlightColor = highlightColor,
            sizeDp = 20.dp,
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * Glossy 3D Spherical Xbox 360 Face Button (`A` Green, `B` Red, `X` Blue, `Y` Amber).
 */
@Composable
private fun NxeXboxSphereBadge(
    letter: String,
    baseColor: Color,
    highlightColor: Color,
    sizeDp: Dp,
) {
    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Sphere body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(highlightColor, baseColor, Color(0xFF071108)),
                    center = Offset(c.x - r * 0.25f, c.y - r * 0.30f),
                    radius = r * 1.25f,
                ),
                radius = r,
                center = c,
            )
            // Top glossy specular lens highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.36f),
                radius = r * 0.52f,
                center = Offset(c.x, c.y - r * 0.34f),
            )
            // Crisp silver rim
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = r - 0.5.dp.toPx(),
                center = c,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Text(
            text = letter,
            color = Color.White,
            fontSize = (sizeDp.value * 0.48f).sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ============================================================================
// Custom Vector Stage Illustrations (Zero Plain-Text Placeholder Letters)
// ============================================================================

/**
 * Slot 0 Artwork: Xbox Series X Monolith + Classic 2008 Xbox 360 Metallic Sphere & Ring of Light.
 */
@Composable
private fun NxeConsoleStageArtwork(isPoweredOn: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.55f

        val ringGreen = if (isPoweredOn) Color(0xFF7CE84A) else Color(0xFF60A5FA)
        val deepGreen = if (isPoweredOn) Color(0xFF107C10) else Color(0xFF1D4ED8)

        // Studio pedestal shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.44f),
            topLeft = Offset(cx - w * 0.28f, cy + h * 0.23f),
            size = Size(w * 0.56f, h * 0.11f),
        )

        // Ambient glow behind hardware
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ringGreen.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(cx, cy),
                radius = h * 0.46f,
            ),
            radius = h * 0.46f,
            center = Offset(cx, cy),
        )

        // Xbox Series X Monolith Tower (Left-of-center)
        val towerW = (h * 0.30f).coerceAtMost(w * 0.26f)
        val towerH = h * 0.56f
        val towerX = cx - towerW * 0.82f
        val towerY = cy - towerH * 0.52f

        // Top emerald vent glow strip on monolith
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(deepGreen, ringGreen, deepGreen),
            ),
            topLeft = Offset(towerX + 2.dp.toPx(), towerY - 3.dp.toPx()),
            size = Size(towerW - 4.dp.toPx(), 5.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )

        // Matte carbon monolith body
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF313D4F), Color(0xFF151D29), Color(0xFF0B1018)),
                start = Offset(towerX, towerY),
                end = Offset(towerX + towerW, towerY + towerH),
            ),
            topLeft = Offset(towerX, towerY),
            size = Size(towerW, towerH),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(towerX, towerY),
            size = Size(towerW, towerH),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx()),
        )

        // Console power jewel LED + vertical disc slot
        drawCircle(
            color = Color.White,
            radius = 2.6.dp.toPx(),
            center = Offset(towerX + towerW * 0.24f, towerY + towerH * 0.14f),
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.7f),
            start = Offset(towerX + towerW * 0.18f, towerY + towerH * 0.58f),
            end = Offset(towerX + towerW * 0.18f, towerY + towerH * 0.90f),
            strokeWidth = 1.5.dp.toPx(),
        )

        // Overlapping 2008 Xbox 360 3D Silver Sphere & Glowing Ring-of-Light Orb (Right-of-center)
        val orbRadius = (h * 0.22f).coerceAtMost(w * 0.19f)
        val orbCenter = Offset(cx + towerW * 0.46f, cy + towerH * 0.08f)

        // Outer Ring-of-Light 4 quadrants
        val ringRadius = orbRadius * 1.20f
        val ringStroke = 2.6.dp.toPx()
        listOf(-165f, -75f, 15f, 105f).forEach { startDeg ->
            drawArc(
                color = ringGreen.copy(alpha = 0.88f),
                startAngle = startDeg,
                sweepAngle = 62f,
                useCenter = false,
                topLeft = Offset(orbCenter.x - ringRadius, orbCenter.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = ringStroke, cap = StrokeCap.Round),
            )
        }

        // Brushed 3D metallic silver Xbox 360 sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF64748B)),
                center = Offset(orbCenter.x - orbRadius * 0.28f, orbCenter.y - orbRadius * 0.28f),
                radius = orbRadius * 1.35f,
            ),
            radius = orbRadius,
            center = orbCenter,
        )

        // Iconic Xbox 'X' ribbon cuts across the sphere
        val cutOffset = orbRadius * 0.52f
        drawLine(
            color = deepGreen,
            start = Offset(orbCenter.x - cutOffset, orbCenter.y - cutOffset),
            end = Offset(orbCenter.x + cutOffset, orbCenter.y + cutOffset),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = deepGreen,
            start = Offset(orbCenter.x + cutOffset, orbCenter.y - cutOffset),
            end = Offset(orbCenter.x - cutOffset, orbCenter.y + cutOffset),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ringGreen,
            start = Offset(orbCenter.x - cutOffset * 0.88f, orbCenter.y - cutOffset * 0.88f),
            end = Offset(orbCenter.x + cutOffset * 0.88f, orbCenter.y + cutOffset * 0.88f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ringGreen,
            start = Offset(orbCenter.x + cutOffset * 0.88f, orbCenter.y - cutOffset * 0.88f),
            end = Offset(orbCenter.x - cutOffset * 0.88f, orbCenter.y + cutOffset * 0.88f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.70f),
            radius = orbRadius,
            center = orbCenter,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/**
 * Slot 1 Artwork: Matte-Carbon Game Pass Media Card + Analytical 3D Brushed-Silver Cloud (`PathOperation.Union`).
 */
@Composable
private fun NxeControllerStageArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.54f

        val skyAccent = Color(0xFF60A5FA)
        val deepCobalt = Color(0xFF1D4ED8)

        // Grounded studio shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(cx - w * 0.28f, cy + h * 0.23f),
            size = Size(w * 0.56f, h * 0.11f),
        )

        // Subtle sky-blue studio glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(skyAccent.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(cx, cy),
                radius = h * 0.46f,
            ),
            radius = h * 0.46f,
            center = Offset(cx, cy),
        )

        // Back Layer: Matte-Carbon Game Pass Media Slate (Upper-left)
        val cardW = (h * 0.48f).coerceAtMost(w * 0.42f)
        val cardH = cardW * 0.68f
        val cardX = cx - cardW * 0.66f
        val cardY = cy - cardH * 0.78f

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(deepCobalt, skyAccent, deepCobalt),
            ),
            topLeft = Offset(cardX + 2.dp.toPx(), cardY - 3.dp.toPx()),
            size = Size(cardW - 4.dp.toPx(), 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF313D4F), Color(0xFF151D29), Color(0xFF0B1018)),
                start = Offset(cardX, cardY),
                end = Offset(cardX + cardW, cardY + cardH),
            ),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx()),
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(cardX + cardW * 0.14f, cardY + cardH * 0.24f),
        )
        drawLine(
            color = skyAccent.copy(alpha = 0.55f),
            start = Offset(cardX + cardW * 0.26f, cardY + cardH * 0.24f),
            end = Offset(cardX + cardW * 0.62f, cardY + cardH * 0.24f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Foreground Layer: Mathematically Exact 3D Brushed-Silver Cloud (`PathOperation.Union`)
        val cloudW = (h * 0.56f).coerceAtMost(w * 0.48f)
        val ccx = cx + cloudW * 0.12f
        val baseY = cy + h * 0.18f

        val rLeft = cloudW * 0.20f
        val rMain = cloudW * 0.27f
        val rUpperRight = cloudW * 0.21f
        val rRight = cloudW * 0.17f

        var cloudPath = Path().apply {
            addRect(
                Rect(
                    left = ccx - cloudW * 0.28f,
                    top = baseY - rRight * 1.45f,
                    right = ccx + cloudW * 0.31f,
                    bottom = baseY,
                ),
            )
        }
        val lobes = listOf(
            Rect(
                left = ccx - cloudW * 0.28f - rLeft,
                top = baseY - rLeft * 2f,
                right = ccx - cloudW * 0.28f + rLeft,
                bottom = baseY,
            ),
            Rect(
                left = ccx - cloudW * 0.06f - rMain,
                top = baseY - cloudW * 0.31f - rMain,
                right = ccx - cloudW * 0.06f + rMain,
                bottom = baseY - cloudW * 0.31f + rMain,
            ),
            Rect(
                left = ccx + cloudW * 0.17f - rUpperRight,
                top = baseY - cloudW * 0.24f - rUpperRight,
                right = ccx + cloudW * 0.17f + rUpperRight,
                bottom = baseY - cloudW * 0.24f + rUpperRight,
            ),
            Rect(
                left = ccx + cloudW * 0.31f - rRight,
                top = baseY - rRight * 2f,
                right = ccx + cloudW * 0.31f + rRight,
                bottom = baseY,
            ),
        )
        lobes.forEach { ovalRect ->
            val lobePath = Path().apply { addOval(ovalRect) }
            cloudPath = Path.combine(PathOperation.Union, cloudPath, lobePath)
        }

        // Subtle drop shadow behind cloud over carbon card
        val shadowPath = Path().apply { addPath(cloudPath, Offset(0f, 3.dp.toPx())) }
        drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.34f))

        drawPath(
            path = cloudPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                startY = baseY - cloudW * 0.58f,
                endY = baseY,
            ),
        )
        drawPath(
            path = cloudPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round),
        )

        // Crisp Solid Beveled Sapphire Play Emblem inside Cloud
        val playCx = ccx + cloudW * 0.03f
        val playCy = baseY - cloudW * 0.24f
        val playR = cloudW * 0.135f
        val playPath = Path().apply {
            moveTo(playCx - playR * 0.62f, playCy - playR * 0.84f)
            lineTo(playCx + playR * 0.88f, playCy)
            lineTo(playCx - playR * 0.62f, playCy + playR * 0.84f)
            close()
        }
        drawPath(
            path = playPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3B82F6), deepCobalt),
                startY = playCy - playR,
                endY = playCy + playR,
            ),
        )
        drawPath(
            path = playPath,
            color = Color(0xFF93C5FD),
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round),
        )
    }
}

/**
 * Slot 2 Artwork: 3 Stacked Matte-Carbon Xbox Achievement Unlock Rows.
 */
@Composable
private fun NxeTrophyStageArtwork(gamerscore: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f
            val cy = h * 0.54f

            val xboxGreen = Color(0xFF4ADE80)
            val deepXboxGreen = Color(0xFF15803D)
            val skyProgress = Color(0xFF60A5FA)

            val baseRowW = (w * 0.68f).coerceAtMost(h * 0.92f)
            val listH = h * 0.52f
            val rowGap = 5.dp.toPx()
            val rowH = (listH - rowGap * 2f) / 3f
            val topY = cy - listH * 0.50f

            // Studio pedestal shadow beneath the 3-row achievement stack
            drawOval(
                color = Color.Black.copy(alpha = 0.40f),
                topLeft = Offset(cx - baseRowW * 0.46f, topY + listH + 4.dp.toPx()),
                size = Size(baseRowW * 0.92f, h * 0.09f),
            )

            // Soft studio glow behind achievement rows
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(xboxGreen.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = h * 0.44f,
                ),
                radius = h * 0.44f,
                center = Offset(cx, cy),
            )

            for (idx in 0..2) {
                val isTopFeatured = idx == 0
                val isUnlocked = idx <= 1
                val rowW = if (isTopFeatured) baseRowW * 1.04f else baseRowW
                val rx = cx - rowW * 0.50f
                val ry = topY + idx * (rowH + rowGap)
                val rowCr = CornerRadius(3.dp.toPx(), 3.dp.toPx())

                // Matte-carbon achievement row card
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = when (idx) {
                            0 -> listOf(Color(0xFF334257), Color(0xFF172130))
                            1 -> listOf(Color(0xFF273446), Color(0xFF121A26))
                            else -> listOf(Color(0xFF1E293B), Color(0xFF0E1520))
                        },
                        startY = ry,
                        endY = ry + rowH,
                    ),
                    topLeft = Offset(rx, ry),
                    size = Size(rowW, rowH),
                    cornerRadius = rowCr,
                )
                drawRoundRect(
                    color = Color.White.copy(
                        alpha = when (idx) {
                            0 -> 0.50f
                            1 -> 0.32f
                            else -> 0.20f
                        },
                    ),
                    topLeft = Offset(rx, ry),
                    size = Size(rowW, rowH),
                    cornerRadius = rowCr,
                    style = Stroke(width = (if (isTopFeatured) 1.2.dp else 1.dp).toPx()),
                )

                // Left Square Achievement Icon Tile
                val iconPad = (rowH * 0.18f).coerceAtLeast(3.dp.toPx())
                val iconSize = (rowH - iconPad * 2f).coerceAtLeast(8.dp.toPx())
                val ix = rx + iconPad
                val iy = ry + iconPad
                val iconCr = CornerRadius(2.dp.toPx(), 2.dp.toPx())

                drawRoundRect(
                    brush = if (isUnlocked) {
                        Brush.linearGradient(
                            colors = listOf(xboxGreen, deepXboxGreen),
                            start = Offset(ix, iy),
                            end = Offset(ix + iconSize, iy + iconSize),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF475569), Color(0xFF1E293B)),
                            start = Offset(ix, iy),
                            end = Offset(ix + iconSize, iy + iconSize),
                        )
                    },
                    topLeft = Offset(ix, iy),
                    size = Size(iconSize, iconSize),
                    cornerRadius = iconCr,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isUnlocked) 0.58f else 0.25f),
                    topLeft = Offset(ix, iy),
                    size = Size(iconSize, iconSize),
                    cornerRadius = iconCr,
                    style = Stroke(width = 1.dp.toPx()),
                )

                if (isUnlocked) {
                    // Crisp white checkmark inside unlocked green tile
                    val checkPath = Path().apply {
                        moveTo(ix + iconSize * 0.26f, iy + iconSize * 0.52f)
                        lineTo(ix + iconSize * 0.44f, iy + iconSize * 0.70f)
                        lineTo(ix + iconSize * 0.76f, iy + iconSize * 0.32f)
                    }
                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Stroke(
                            width = 1.7.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                } else {
                    // Subtle locked ring dot
                    drawCircle(
                        color = Color(0xFF94A3B8),
                        radius = (iconSize * 0.16f).coerceAtLeast(1.5.dp.toPx()),
                        center = Offset(ix + iconSize * 0.50f, iy + iconSize * 0.50f),
                    )
                }

                // Middle Title & Unlock/Progress Bars
                val textStartX = ix + iconSize + iconPad * 1.15f
                val titleEndFrac = when (idx) {
                    0 -> 0.72f
                    1 -> 0.64f
                    else -> 0.67f
                }
                val titleY = ry + rowH * 0.36f
                val subY = ry + rowH * 0.67f

                drawLine(
                    color = when (idx) {
                        0 -> Color(0xFFF8FAFC)
                        1 -> Color(0xFFCBD5E1)
                        else -> Color(0xFF94A3B8)
                    },
                    start = Offset(textStartX, titleY),
                    end = Offset((rx + rowW * titleEndFrac).coerceAtLeast(textStartX + 6f), titleY),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                if (idx < 2) {
                    val subEndFrac = if (idx == 0) 0.54f else 0.48f
                    drawLine(
                        color = if (idx == 0) Color(0xFF7CE84A) else Color(0xFF64748B),
                        start = Offset(textStartX, subY),
                        end = Offset((rx + rowW * subEndFrac).coerceAtLeast(textStartX + 4f), subY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                } else {
                    // Partial sky-blue achievement progress track on row 3
                    val trackEndX = (rx + rowW * 0.62f).coerceAtLeast(textStartX + 8f)
                    val fillEndX = textStartX + (trackEndX - textStartX) * 0.58f
                    drawLine(
                        color = Color(0xFF090E17),
                        start = Offset(textStartX, subY),
                        end = Offset(trackEndX, subY),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = skyProgress,
                        start = Offset(textStartX, subY),
                        end = Offset(fillEndX, subY),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Right-Aligned Gamerscore Pill (`G` pip + score bar)
                val pillW = rowW * 0.17f
                val pillH = rowH * 0.44f
                val px = rx + rowW - pillW - iconPad
                val py = ry + (rowH - pillH) * 0.50f
                if (px > rx + rowW * 0.75f) {
                    drawRoundRect(
                        color = Color(0xFF09101B),
                        topLeft = Offset(px, py),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    )
                    drawCircle(
                        color = if (isUnlocked) Color(0xFF6CC644) else Color(0xFF64748B),
                        radius = 1.8.dp.toPx(),
                        center = Offset(px + pillW * 0.26f, py + pillH * 0.50f),
                    )
                    drawLine(
                        color = if (isUnlocked) Color(0xFFE2E8F0) else Color(0xFF64748B),
                        start = Offset(px + pillW * 0.48f, py + pillH * 0.50f),
                        end = Offset(px + pillW * 0.78f, py + pillH * 0.50f),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        if (gamerscore > 999999) {
            // Kept purely for signature parity; Gamerscore total is already displayed cleanly in the card subtitle
            NxeArtworkCornerBadge(text = "$gamerscore G")
        }
    }
}

/**
 * Slot 3 Artwork: Centered 16:9 Matte-Carbon Studio Reference Monitor with On-Screen Precision Sliders.
 */
@Composable
private fun NxeSettingsStageArtwork(badgeText: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f
            val cy = h * 0.52f

            val ringIce = Color(0xFF60A5FA)
            val deepIce = Color(0xFF2563EB)

            val monW = (h * 0.68f).coerceAtMost(w * 0.58f)
            val monH = monW * 0.62f
            val monX = cx - monW * 0.50f
            val monY = cy - monH * 0.54f
            val standH = h * 0.10f
            val baseY = monY + monH + standH

            // Studio pedestal shadow under monitor stand
            drawOval(
                color = Color.Black.copy(alpha = 0.42f),
                topLeft = Offset(cx - monW * 0.42f, baseY - 2.dp.toPx()),
                size = Size(monW * 0.84f, h * 0.09f),
            )

            // Subtle ambient ice-blue glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ringIce.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = h * 0.45f,
                ),
                radius = h * 0.45f,
                center = Offset(cx, cy),
            )

            // Brushed-silver monitor stand neck & matte-carbon foot
            val neckW = (monW * 0.10f).coerceAtLeast(6.dp.toPx())
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF64748B), Color(0xFFE2E8F0), Color(0xFF64748B)),
                    startX = cx - neckW * 0.5f,
                    endX = cx + neckW * 0.5f,
                ),
                topLeft = Offset(cx - neckW * 0.5f, monY + monH),
                size = Size(neckW, standH),
            )
            val footW = monW * 0.44f
            val footH = 4.5.dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF475569), Color(0xFF0F172A)),
                ),
                topLeft = Offset(cx - footW * 0.5f, baseY - footH * 0.4f),
                size = Size(footW, footH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(cx - footW * 0.5f, baseY - footH * 0.4f),
                size = Size(footW, footH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )

            // Top ice-blue calibration glow strip
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(deepIce, ringIce, deepIce),
                ),
                topLeft = Offset(monX + 4.dp.toPx(), monY - 3.dp.toPx()),
                size = Size(monW - 8.dp.toPx(), 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )

            // Matte-carbon 16:9 widescreen display panel
            val monCr = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2D3A4E), Color(0xFF131B28), Color(0xFF090E17)),
                    start = Offset(monX, monY),
                    end = Offset(monX + monW, monY + monH),
                ),
                topLeft = Offset(monX, monY),
                size = Size(monW, monH),
                cornerRadius = monCr,
            )

            // Diagonal glass reflection across left half of screen
            val screenSheen = Path().apply {
                moveTo(monX, monY)
                lineTo(monX + monW * 0.52f, monY)
                lineTo(monX + monW * 0.24f, monY + monH)
                lineTo(monX, monY + monH)
                close()
            }
            drawPath(path = screenSheen, color = Color.White.copy(alpha = 0.06f))

            // Crisp brushed-silver display bezel
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)),
                    startY = monY,
                    endY = monY + monH,
                ),
                topLeft = Offset(monX, monY),
                size = Size(monW, monH),
                cornerRadius = monCr,
                style = Stroke(width = 1.4.dp.toPx()),
            )

            // 3 Crisp Horizontal Precision Calibration Sliders Inside Screen
            val trackStartX = monX + monW * 0.16f
            val trackEndX = monX + monW * 0.84f
            val rowFractions = listOf(0.28f, 0.50f, 0.72f)
            val knobFractions = listOf(0.72f, 0.36f, 0.58f)

            rowFractions.forEachIndexed { idx, yf ->
                val y = monY + monH * yf
                val xKnob = trackStartX + (trackEndX - trackStartX) * knobFractions[idx]

                // Recessed dark track
                drawLine(
                    color = Color(0xFF060A12),
                    start = Offset(trackStartX, y),
                    end = Offset(trackEndX, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                // Active sky-blue fill
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(deepIce, ringIce),
                        startX = trackStartX,
                        endX = xKnob,
                    ),
                    start = Offset(trackStartX, y),
                    end = Offset(xKnob, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                // Brushed-silver 3D slider thumb cap
                val knobR = (monH * 0.052f).coerceIn(2.6.dp.toPx(), 4.5.dp.toPx())
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = knobR + 0.8.dp.toPx(),
                    center = Offset(xKnob, y + 1.dp.toPx()),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1)),
                        center = Offset(xKnob - knobR * 0.25f, y - knobR * 0.25f),
                        radius = knobR * 1.4f,
                    ),
                    radius = knobR,
                    center = Offset(xKnob, y),
                )
                drawCircle(
                    color = deepIce,
                    radius = knobR * 0.38f,
                    center = Offset(xKnob, y),
                )
            }
        }

        if (badgeText.isNotBlank()) {
            NxeArtworkCornerBadge(
                text = badgeText,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun NxeArtworkCornerBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.28f))
            .padding(1.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF061022).copy(alpha = 0.78f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

private object NxeGamerpicCache {
    private val memoryCache = LruCache<String, ImageBitmap>(16)

    fun getMemory(url: String): ImageBitmap? {
        if (url.isBlank()) return null
        return synchronized(memoryCache) { memoryCache.get(url) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun loadOrFetch(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://", ignoreCase = true)) return@withContext null
        getMemory(url)?.let { return@withContext it }

        val dir = File(context.cacheDir, "gamerpics").apply { mkdirs() }
        val diskFile = File(dir, "${sha256Hex(url)}.png")

        if (diskFile.exists() && diskFile.length() > 0L) {
            runCatching {
                BitmapFactory.decodeFile(diskFile.absolutePath)?.asImageBitmap()
            }.getOrNull()?.let { cachedBmp ->
                synchronized(memoryCache) { memoryCache.put(url, cachedBmp) }
                // Refresh silently only if older than 24h; otherwise return immediately
                if (System.currentTimeMillis() - diskFile.lastModified() < 86_400_000L) {
                    return@withContext cachedBmp
                }
            }
        }

        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/*")
            }
            try {
                if (conn.responseCode in 200..299) {
                    val rawBitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                    val scaled = if (rawBitmap.width > 256 || rawBitmap.height > 256) {
                        Bitmap.createScaledBitmap(rawBitmap, 192, 192, true)
                    } else {
                        rawBitmap
                    }
                    val tmpFile = File(dir, "${sha256Hex(url)}.tmp")
                    FileOutputStream(tmpFile).use { out ->
                        scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    tmpFile.renameTo(diskFile)
                    val imgBitmap = scaled.asImageBitmap()
                    synchronized(memoryCache) { memoryCache.put(url, imgBitmap) }
                    imgBitmap
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: getMemory(url)
    }
}

@Composable
internal fun NxeGamerpicAvatarBox(
    gamerpicUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tokens = LocalNxeGlassTokens.current
    var bitmap by remember(gamerpicUrl) {
        mutableStateOf(NxeGamerpicCache.getMemory(gamerpicUrl))
    }

    LaunchedEffect(gamerpicUrl) {
        if (gamerpicUrl.isNotBlank() && bitmap == null) {
            bitmap = NxeGamerpicCache.loadOrFetch(context, gamerpicUrl)
        } else if (gamerpicUrl.isBlank()) {
            bitmap = null
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    tokens.bgStageMid.copy(alpha = 0.65f),
                    tokens.bgApp.copy(alpha = 0.90f),
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap,
                contentDescription = "Xbox Profile Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            NxeGamerpicAvatarCanvas(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NxeGamerpicAvatarCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        // Head
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = size.minDimension * 0.22f,
            center = Offset(cx, size.height * 0.36f),
        )
        // Shoulders arc
        drawArc(
            color = Color(0xFFCBD5E1),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.16f, size.height * 0.62f),
            size = Size(size.width * 0.68f, size.height * 0.56f),
        )
    }
}

@Composable
private fun NxeRefreshIconCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pad = 2.dp.toPx()
        val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
        drawArc(
            color = Color.White.copy(alpha = 0.90f),
            startAngle = -60f,
            sweepAngle = 285f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = arcSize,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )
        // Arrowhead
        val tipX = size.width * 0.78f
        val tipY = size.height * 0.22f
        val arrowPath = Path().apply {
            moveTo(tipX, tipY - 3.dp.toPx())
            lineTo(tipX + 3.5.dp.toPx(), tipY + 2.dp.toPx())
            lineTo(tipX - 2.5.dp.toPx(), tipY + 2.5.dp.toPx())
            close()
        }
        drawPath(path = arrowPath, color = Color.White.copy(alpha = 0.92f))
    }
}

// ============================================================================
// System Settings Category Card Artworks (Clean Geometric NXE Illustrations)
// ============================================================================

@Composable
private fun NxeAppearanceStageArtwork(
    primaryColor: Color,
    secondaryColor: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f

        val frameW = (h * 0.72f).coerceAtMost(w * 0.66f)
        val frameH = frameW * 0.72f
        val fx = cx - frameW * 0.50f
        val fy = cy - frameH * 0.50f
        val cr = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        // Studio pedestal shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.40f),
            topLeft = Offset(cx - frameW * 0.44f, fy + frameH + 3.dp.toPx()),
            size = Size(frameW * 0.88f, h * 0.09f),
        )

        // Subtle theme glow behind frame
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(cx, cy),
                radius = h * 0.42f,
            ),
            radius = h * 0.42f,
            center = Offset(cx, cy),
        )

        // Matte-carbon preview window plaque
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1F2B3D), Color(0xFF0D1420)),
                startY = fy,
                endY = fy + frameH,
            ),
            topLeft = Offset(fx, fy),
            size = Size(frameW, frameH),
            cornerRadius = cr,
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1).copy(alpha = 0.75f),
            topLeft = Offset(fx, fy),
            size = Size(frameW, frameH),
            cornerRadius = cr,
            style = Stroke(width = 1.2.dp.toPx()),
        )

        // Upper mini NXE preview stage banner inside window
        val pad = frameW * 0.09f
        val stageH = frameH * 0.46f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.85f), secondaryColor.copy(alpha = 0.55f)),
                startX = fx + pad,
                endX = fx + frameW - pad,
            ),
            topLeft = Offset(fx + pad, fy + pad),
            size = Size(frameW - pad * 2f, stageH),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
        )

        // Two crisp mini card silhouettes inside the preview stage
        val miniGap = frameW * 0.04f
        val miniW = (frameW - pad * 2f - miniGap * 3f) * 0.36f
        val miniH = stageH * 0.62f
        val miniY = fy + pad + stageH - miniH - stageH * 0.12f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.88f),
            topLeft = Offset(fx + pad + miniGap, miniY),
            size = Size(miniW, miniH),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.48f),
            topLeft = Offset(fx + pad + miniGap * 2f + miniW, miniY + miniH * 0.12f),
            size = Size(miniW, miniH * 0.88f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
        )

        // Bottom row of 4 crisp non-overlapping color swatch tiles
        val swatchColors = listOf(
            primaryColor,
            Color(0xFF22C55E),
            Color(0xFF6366F1),
            Color(0xFFEF4444),
        )
        val innerW = frameW - pad * 2f
        val swatchGap = frameW * 0.05f
        val swatchSize = ((innerW - swatchGap * 3f) / 4f).coerceAtMost(frameH * 0.24f)
        val totalSwatchesW = swatchSize * 4f + swatchGap * 3f
        val startX = cx - totalSwatchesW * 0.50f
        val swatchY = fy + frameH - pad - swatchSize

        swatchColors.forEachIndexed { idx, swatchColor ->
            val sx = startX + idx * (swatchSize + swatchGap)
            val scr = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            drawRoundRect(
                color = swatchColor,
                topLeft = Offset(sx, swatchY),
                size = Size(swatchSize, swatchSize),
                cornerRadius = scr,
            )
            drawRoundRect(
                color = if (idx == 0) Color.White else Color.White.copy(alpha = 0.35f),
                topLeft = Offset(sx, swatchY),
                size = Size(swatchSize, swatchSize),
                cornerRadius = scr,
                style = Stroke(width = (if (idx == 0) 1.4.dp else 0.8.dp).toPx()),
            )
        }
    }
}

@Composable
private fun NxeAudioStageArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f

        val span = (h * 0.60f).coerceAtMost(w * 0.60f)
        val speakerCenterX = cx - span * 0.16f

        // Studio pedestal shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.38f),
            topLeft = Offset(cx - span * 0.48f, cy + span * 0.44f),
            size = Size(span * 0.96f, h * 0.09f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF60A5FA).copy(alpha = 0.16f), Color.Transparent),
                center = Offset(cx, cy),
                radius = h * 0.40f,
            ),
            radius = h * 0.40f,
            center = Offset(cx, cy),
        )

        // Brushed-silver geometric speaker driver cone
        val boxW = span * 0.20f
        val boxH = span * 0.30f
        val coneW = span * 0.26f
        val coneH = span * 0.62f
        val leftX = speakerCenterX - (boxW + coneW) * 0.50f

        val speakerPath = Path().apply {
            moveTo(leftX, cy - boxH * 0.50f)
            lineTo(leftX + boxW, cy - boxH * 0.50f)
            lineTo(leftX + boxW + coneW, cy - coneH * 0.50f)
            lineTo(leftX + boxW + coneW, cy + coneH * 0.50f)
            lineTo(leftX + boxW, cy + boxH * 0.50f)
            lineTo(leftX, cy + boxH * 0.50f)
            close()
        }

        drawPath(
            path = speakerPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF64748B)),
                startY = cy - coneH * 0.50f,
                endY = cy + coneH * 0.50f,
            ),
        )
        drawPath(
            path = speakerPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round),
        )

        // 3 Concentric Rounded Acoustic Wave Arcs
        val arcOriginX = leftX + boxW + coneW * 0.72f
        val radii = listOf(span * 0.24f, span * 0.40f, span * 0.56f)
        val alphas = listOf(0.95f, 0.72f, 0.46f)
        radii.forEachIndexed { idx, r ->
            drawArc(
                color = Color(0xFF60A5FA).copy(alpha = alphas[idx]),
                startAngle = -38f,
                sweepAngle = 76f,
                useCenter = false,
                topLeft = Offset(arcOriginX - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun NxeGamepadSettingsStageArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f

        val deckW = (h * 0.74f).coerceAtMost(w * 0.66f)
        val deckH = deckW * 0.58f
        val dx = cx - deckW * 0.50f
        val dy = cy - deckH * 0.50f
        val cr = CornerRadius(6.dp.toPx(), 6.dp.toPx())

        // Studio pedestal shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.40f),
            topLeft = Offset(cx - deckW * 0.44f, dy + deckH + 3.dp.toPx()),
            size = Size(deckW * 0.88f, h * 0.09f),
        )

        // Matte-carbon controller face plaque
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF222E40), Color(0xFF0E1522)),
                startY = dy,
                endY = dy + deckH,
            ),
            topLeft = Offset(dx, dy),
            size = Size(deckW, deckH),
            cornerRadius = cr,
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1).copy(alpha = 0.75f),
            topLeft = Offset(dx, dy),
            size = Size(deckW, deckH),
            cornerRadius = cr,
            style = Stroke(width = 1.2.dp.toPx()),
        )

        // Left Half: Crisp Brushed-Silver D-Pad Cross
        val dpadCx = dx + deckW * 0.30f
        val dpadCy = cy
        val armLen = deckH * 0.46f
        val armThick = armLen * 0.34f
        val vPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = dpadCx - armThick * 0.50f,
                    top = dpadCy - armLen * 0.50f,
                    right = dpadCx + armThick * 0.50f,
                    bottom = dpadCy + armLen * 0.50f,
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                ),
            )
        }
        val hPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = dpadCx - armLen * 0.50f,
                    top = dpadCy - armThick * 0.50f,
                    right = dpadCx + armLen * 0.50f,
                    bottom = dpadCy + armThick * 0.50f,
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                ),
            )
        }
        val dpadPath = Path.combine(PathOperation.Union, vPath, hPath)
        drawPath(
            path = dpadPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)),
                startY = dpadCy - armLen * 0.50f,
                endY = dpadCy + armLen * 0.50f,
            ),
        )

        // Right Half: 4 Xbox 360 Colored Face Button Pips (Y, X, B, A)
        val abxyCx = dx + deckW * 0.71f
        val abxyCy = cy
        val spread = deckH * 0.19f
        val pipR = (deckH * 0.088f).coerceAtLeast(2.2.dp.toPx())

        val buttons = listOf(
            Offset(abxyCx, abxyCy - spread) to Color(0xFFFBBF24), // Y (Amber)
            Offset(abxyCx - spread, abxyCy) to Color(0xFF3B82F6), // X (Blue)
            Offset(abxyCx + spread, abxyCy) to Color(0xFFEF4444), // B (Red)
            Offset(abxyCx, abxyCy + spread) to Color(0xFF22C55E), // A (Green)
        )
        buttons.forEach { (center, btnColor) ->
            drawCircle(color = btnColor, radius = pipR, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = pipR,
                center = center,
                style = Stroke(width = 0.8.dp.toPx()),
            )
        }
    }
}

@Composable
private fun NxeNetworkStageArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f

        val totalW = (h * 0.62f).coerceAtMost(w * 0.58f)
        val maxBarH = totalW * 0.78f
        val barGap = totalW * 0.08f
        val barW = (totalW - barGap * 3f) / 4f
        val baseX = cx - totalW * 0.50f
        val baseY = cy + maxBarH * 0.50f

        // Studio pedestal shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.38f),
            topLeft = Offset(cx - totalW * 0.48f, baseY + 3.dp.toPx()),
            size = Size(totalW * 0.96f, h * 0.09f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF60A5FA).copy(alpha = 0.16f), Color.Transparent),
                center = Offset(cx, cy),
                radius = h * 0.40f,
            ),
            radius = h * 0.40f,
            center = Offset(cx, cy),
        )

        val fractions = listOf(0.32f, 0.54f, 0.77f, 1.00f)
        val cr = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
        fractions.forEachIndexed { idx, frac ->
            val bh = maxBarH * frac
            val bx = baseX + idx * (barW + barGap)
            val by = baseY - bh
            val isPeak = idx == 3
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isPeak) {
                        listOf(Color(0xFFFFFFFF), Color(0xFF93C5FD), Color(0xFF3B82F6))
                    } else {
                        listOf(Color(0xFF93C5FD), Color(0xFF2563EB))
                    },
                    startY = by,
                    endY = baseY,
                ),
                topLeft = Offset(bx, by),
                size = Size(barW, bh),
                cornerRadius = cr,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = if (isPeak) 0.85f else 0.45f),
                topLeft = Offset(bx, by),
                size = Size(barW, bh),
                cornerRadius = cr,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun NxeAccountStageArtwork(
    gamerpicUrl: String,
    isSignedIn: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val cardW = (maxWidth * 0.72f).coerceIn(68.dp, 152.dp)
        val cardH = (cardW * 0.40f).coerceIn(28.dp, 54.dp)

        Box(contentAlignment = Alignment.Center) {
            // Pedestal shadow below the mini Gamercard
            Canvas(
                modifier = Modifier
                    .width(cardW * 0.92f)
                    .height(cardH + 16.dp),
            ) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.40f),
                    topLeft = Offset(0f, size.height - 9.dp.toPx()),
                    size = Size(size.width, 9.dp.toPx()),
                )
            }

            Row(
                modifier = Modifier
                    .width(cardW)
                    .height(cardH)
                    .clip(RoundedCornerShape(4.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NxeGamerpicAvatarBox(
                    gamerpicUrl = gamerpicUrl,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                )

                val rightShape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1F2C3F), Color(0xFF0E1624)),
                            ),
                            shape = rightShape,
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFCBD5E1).copy(alpha = 0.65f),
                            shape = rightShape,
                        ),
                ) {
                    val padX = size.width * 0.14f
                    val titleY = size.height * 0.38f
                    val subY = size.height * 0.66f
                    val pipR = (size.height * 0.085f).coerceAtLeast(2.dp.toPx())

                    drawCircle(
                        color = if (isSignedIn) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                        radius = pipR,
                        center = Offset(padX + pipR, titleY),
                    )
                    drawLine(
                        color = Color(0xFFF8FAFC),
                        start = Offset(padX + pipR * 3.2f, titleY),
                        end = Offset(size.width * 0.82f, titleY),
                        strokeWidth = (size.height * 0.10f).coerceAtLeast(2.dp.toPx()),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(padX, subY),
                        end = Offset(size.width * 0.58f, subY),
                        strokeWidth = (size.height * 0.075f).coerceAtLeast(1.6.dp.toPx()),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun NxeAboutStageArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f

        val boxS = (h * 0.54f).coerceAtMost(w * 0.50f)
        val bx = cx - boxS * 0.50f
        val by = cy - boxS * 0.50f
        val cr = CornerRadius(6.dp.toPx(), 6.dp.toPx())

        drawOval(
            color = Color.Black.copy(alpha = 0.40f),
            topLeft = Offset(cx - boxS * 0.44f, by + boxS + 3.dp.toPx()),
            size = Size(boxS * 0.88f, h * 0.09f),
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF243247), Color(0xFF0E1624)),
                startY = by,
                endY = by + boxS,
            ),
            topLeft = Offset(bx, by),
            size = Size(boxS, boxS),
            cornerRadius = cr,
        )
        drawRoundRect(
            color = Color(0xFFE2E8F0).copy(alpha = 0.78f),
            topLeft = Offset(bx, by),
            size = Size(boxS, boxS),
            cornerRadius = cr,
            style = Stroke(width = 1.3.dp.toPx()),
        )

        // Crisp geometric 'i' emblem
        val dotR = (boxS * 0.068f).coerceAtLeast(2.dp.toPx())
        drawCircle(
            color = Color(0xFF60A5FA),
            radius = dotR,
            center = Offset(cx, by + boxS * 0.30f),
        )
        val stemW = (boxS * 0.12f).coerceAtLeast(2.8.dp.toPx())
        val stemTop = by + boxS * 0.45f
        val stemH = boxS * 0.31f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, Color(0xFFCBD5E1)),
                startY = stemTop,
                endY = stemTop + stemH,
            ),
            topLeft = Offset(cx - stemW * 0.50f, stemTop),
            size = Size(stemW, stemH),
            cornerRadius = CornerRadius(stemW * 0.5f, stemW * 0.5f),
        )
    }
}

@Composable
private fun NxeDeveloperStageArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f

        val frameW = (h * 0.70f).coerceAtMost(w * 0.64f)
        val frameH = frameW * 0.64f
        val fx = cx - frameW * 0.50f
        val fy = cy - frameH * 0.50f
        val cr = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        drawOval(
            color = Color.Black.copy(alpha = 0.40f),
            topLeft = Offset(cx - frameW * 0.44f, fy + frameH + 3.dp.toPx()),
            size = Size(frameW * 0.88f, h * 0.09f),
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2B1233), Color(0xFF14081A)),
                startY = fy,
                endY = fy + frameH,
            ),
            topLeft = Offset(fx, fy),
            size = Size(frameW, frameH),
            cornerRadius = cr,
        )
        drawRoundRect(
            color = Color(0xFFF0ABFC).copy(alpha = 0.72f),
            topLeft = Offset(fx, fy),
            size = Size(frameW, frameH),
            cornerRadius = cr,
            style = Stroke(width = 1.2.dp.toPx()),
        )

        // Geometric `>_` terminal prompt in Fuchsia
        val chevronPath = Path().apply {
            moveTo(fx + frameW * 0.22f, fy + frameH * 0.34f)
            lineTo(fx + frameW * 0.38f, fy + frameH * 0.50f)
            lineTo(fx + frameW * 0.22f, fy + frameH * 0.66f)
        }
        drawPath(
            path = chevronPath,
            color = Color(0xFFF0ABFC),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
            color = Color(0xFFD946EF),
            start = Offset(fx + frameW * 0.46f, fy + frameH * 0.66f),
            end = Offset(fx + frameW * 0.72f, fy + frameH * 0.66f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

// ============================================================================
// Sub-Channel Stages (Cloud Gaming & Achievements - Cohesive NXE Styling)
// ============================================================================

@Composable
private fun CloudGamingStageBlade(
    state: XStreamingUiState,
    isWideTablet: Boolean,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val titles = state.filteredCloudTitles

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NxeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            isHero = true,
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Xbox Game Pass Cloud Library",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.isLoadingCloud) {
                            "Syncing Game Pass catalog..."
                        } else {
                            "${titles.size} cloud-playable titles available"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }

                NxeActionRectButton(
                    label = if (state.isLoadingCloud) "Syncing..." else "Refresh Catalog",
                    isPrimary = true,
                    onClick = {
                        haptics?.clickConfirm()
                        XStreamingStateStore.emitCommandToJs("refreshCloudTitles")
                    },
                )
            }
        }

        if (titles.isEmpty()) {
            NxeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (state.isLoadingCloud) "Loading Xbox Cloud Gaming catalog..." else "No cached Cloud titles yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Select Refresh Catalog above or press (Y) to load your Xbox Game Pass Ultimate library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                }
            }
        } else {
            val columns = if (isWideTablet) 2 else 1
            titles.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        NxeGlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 3.dp,
                            contentPadding = PaddingValues(14.dp),
                            onClick = {
                                haptics?.clickConfirm()
                                XStreamingStateStore.emitCommandToJs(
                                    "startCloudStream",
                                    """{"titleId":"${item.titleId}","name":"${item.name.replace("\"", "\\\"")}"}""",
                                )
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${item.category} · ${item.subtitle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                NxeXboxSphereBadge(
                                    letter = "A",
                                    baseColor = Color(0xFF107C10),
                                    highlightColor = Color(0xFF7CE84A),
                                    sizeDp = 26.dp,
                                )
                            }
                        }
                    }
                    if (rowItems.size < columns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsAndTelemetryStageBlade(state: XStreamingUiState) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NxeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            isHero = true,
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Gamerscore & Session Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.achievements.isNotEmpty()) {
                            "${state.totalCurrentGamerscore} / ${state.totalMaxGamerscore} G across recent titles"
                        } else {
                            "Xbox Live achievement history and stream network telemetry"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.lastSessionReport != null) {
                        NxeActionRectButton(
                            label = "Session Report",
                            isPrimary = false,
                            onClick = {
                                haptics?.clickConfirm()
                                XStreamingStateStore.openModal(NxeActiveModal.SESSION_REPORT)
                            },
                        )
                    }
                    NxeActionRectButton(
                        label = "Refresh",
                        isPrimary = true,
                        onClick = {
                            haptics?.clickConfirm()
                            XStreamingStateStore.emitCommandToJs("refreshAchievements")
                        },
                    )
                }
            }
        }

        val report = state.lastSessionReport
        val onReportClick: (() -> Unit)? = if (report != null) {
            { XStreamingStateStore.openModal(NxeActiveModal.SESSION_REPORT) }
        } else {
            null
        }
        NxeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            onClick = onReportClick,
        ) {
            if (report != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Latest Session · ${report.targetName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${report.avgFps} FPS · ${String.format("%.1f", report.avgBitrateMbps)} Mbps · ${report.avgRttMs} ms RTT",
                            style = NxeTypography.TelemetryValue,
                            color = tokens.textSecondary,
                        )
                    }
                    Text(
                        text = report.durationText,
                        style = NxeTypography.TelemetryValue,
                        color = Color.White,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Latest Stream Session",
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "After disconnecting from Remote Play or Cloud Gaming, your frame rate, decode latency, and RTT summary appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                }
            }
        }

        if (state.achievements.isEmpty()) {
            NxeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (state.isLoadingAchievements) "Loading Xbox Live Achievements..." else "No recent Xbox Achievements loaded",
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Select Refresh above or press (Y) to load your recent Xbox title Gamerscore progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                }
            }
        } else {
            state.achievements.forEach { item ->
                NxeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 3.dp,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                            )
                            Text(
                                text = "${item.currentGamerscore} / ${item.maxGamerscore} G (${item.progressPercent}%)",
                                style = NxeTypography.TelemetryValue,
                                color = tokens.textSecondary,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.progressFraction)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color(0xFF6CC644)),
                            )
                        }

                        if (item.lastUnlock.isNotBlank()) {
                            Text(
                                text = "Last unlock: ${item.lastUnlock}",
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

@Composable
private fun NxeActionRectButton(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (isPrimary) {
                    Brush.verticalGradient(
                        colors = listOf(tokens.accentSecondary.copy(alpha = 0.35f), tokens.accentPrimary),
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.30f)),
                    )
                },
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (isPrimary) 0.36f else 0.18f),
                shape = RoundedCornerShape(3.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
