// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.components

import android.content.res.Configuration
import android.view.WindowInsets as AndroidWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.LocalNxeHaptics

/**
 * Lab 151 Ecosystem Invariant:
 * All top header toolbars and bottom modal action bars lock to 56.dp content height.
 */
val NXE_TOOLBAR_HEIGHT_DP: Dp = 56.dp

/**
 * Reads unconsumed system bar insets directly from `LocalView.current.rootWindowInsets`
 * (`WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()`, and
 * `WindowInsets.Type.navigationBars()`), preventing sibling `ReactRootView` /
 * `SafeAreaProvider` view hierarchies from swallowing insets before `ComposeView`
 * receives them. Includes a guaranteed minimum safe floor (`48.dp` top on portrait
 * phones / `28.dp` on tablets & landscape, `24.dp` bottom) so top chrome never
 * collides with the status bar or camera punch-hole even on the first layout frame.
 */
@Composable
fun rememberNxeSystemBarInsets(): Pair<Dp, Dp> {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val rootInsets = view.rootWindowInsets
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPortraitPhone = configuration.screenWidthDp < 600 && !isLandscape

    val statusBarPx = rootInsets
        ?.getInsetsIgnoringVisibility(
            AndroidWindowInsets.Type.statusBars() or
                AndroidWindowInsets.Type.displayCutout(),
        )?.top ?: 0
    val navBarPx = rootInsets
        ?.getInsetsIgnoringVisibility(
            AndroidWindowInsets.Type.navigationBars(),
        )?.bottom ?: 0

    val rawTopDp = with(density) { statusBarPx.toDp() }
    val rawBottomDp = with(density) { navBarPx.toDp() }

    val minTopDp = when {
        isPortraitPhone -> 48.dp
        isLandscape -> 14.dp
        else -> 28.dp
    }
    val minBottomDp = if (isLandscape) 8.dp else 24.dp

    return Pair(
        maxOf(rawTopDp, minTopDp),
        maxOf(rawBottomDp, minBottomDp),
    )
}

/**
 * Applies API 33 hardware-accelerated RenderNode blur to the background dashboard container
 * whenever a modal dialog or bottom sheet is open, keeping modal foreground typography sharp.
 */
fun Modifier.nxeRootBackdropBlur(isModalOpen: Boolean, radiusPx: Float = 28f): Modifier =
    if (isModalOpen) {
        this.graphicsLayer {
            renderEffect = BlurEffect(
                radiusX = radiusPx,
                radiusY = radiusPx,
                edgeTreatment = TileMode.Decal,
            )
        }
    } else {
        this
    }

/**
 * Unified 56.dp Top Frame Bar (`NxeTopBar`).
 * Automatically clears the system status bar / camera cutout via [rememberNxeSystemBarInsets]
 * and enforces single-line ellipsis truncation on all titles and action labels.
 */
@Composable
fun NxeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    centerContent: (@Composable RowScope.() -> Unit)? = null,
    trailingActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = LocalNxeGlassTokens.current
    val (topInsetDp, _) = rememberNxeSystemBarInsets()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.glassChromeTop)
            .padding(top = topInsetDp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NXE_TOOLBAR_HEIGHT_DP)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = if (centerContent != null) {
                    Modifier.padding(end = 10.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingContent != null) {
                    leadingContent()
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (centerContent != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    content = centerContent,
                )
            }

            if (trailingActions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = trailingActions,
                )
            }
        }

        // Subtle 1dp bottom specular divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tokens.borderSubtle),
        )
    }
}

/**
 * Standardized `<ModalBreadcrumb>` component for cross-modal navigation
 * (e.g., About Dialog <-> Release Notes Dialog).
 */
@Composable
fun NxeModalBreadcrumb(
    parentLabel: String,
    currentLabel: String,
    onNavigateParent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = parentLabel,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.accentSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    haptics?.tickBack()
                    onNavigateParent()
                }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textMuted,
        )
        Text(
            text = currentLabel,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Universal Modal Frame (`NxeModalFrame`) enforcing all 6 Lab 151 Modal Invariants:
 * 1. Scrim click-outside dismissal (`rgba(2, 6, 18, 0.84)`).
 * 2. Inner card click propagation isolation (`clickable(indication = null) {}`).
 * 3. Hardware Back / Controller `(B)` dismissal via [BackHandler].
 * 4. Minimalist 56.dp header with single-line title on the left and circular `X` close button on the right.
 * 5. Zero redundant footer "Close" or "Done" buttons (optional 56.dp footer is strictly for
 *    meaningful actions like "Copy Telemetry" or "Don't show this again").
 * 6. High-contrast Refracted Glass surface over the blurred root dashboard.
 */
@Composable
fun NxeModalFrame(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 620.dp,
    headerBreadcrumb: (@Composable () -> Unit)? = null,
    footerContent: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    BackHandler(enabled = true) {
        haptics?.tickBack()
        onDismissRequest()
    }

    // 1. Full-screen translucent scrim with click-outside dismissal & safe-drawing insets
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.glassModalScrim)
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = {
                    haptics?.tickBack()
                    onDismissRequest()
                },
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 2. Inner Glass Card with click propagation isolation
        NxeGlassCard(
            modifier = modifier
                .widthIn(max = maxWidth)
                .heightIn(max = 580.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = { /* Stop propagation to backdrop scrim */ },
                ),
            cornerRadius = 4.dp,
            isHero = true,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 4. Unified 56.dp Modal Header (Title/Breadcrumb left, X Close button right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NXE_TOOLBAR_HEIGHT_DP)
                        .background(tokens.glassChromeTop)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (headerBreadcrumb != null) {
                            headerBreadcrumb()
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, tokens.borderGlassRest, RoundedCornerShape(3.dp))
                            .clickable {
                                haptics?.tickBack()
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✕",
                            color = tokens.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Modal Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    content = content,
                )

                // 5. Optional 56.dp Footer Bar (Only for meaningful actions, never redundant Close/Done)
                if (footerContent != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NXE_TOOLBAR_HEIGHT_DP)
                            .background(tokens.glassChromeBottom)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        content = footerContent,
                    )
                }
            }
        }
    }
}
