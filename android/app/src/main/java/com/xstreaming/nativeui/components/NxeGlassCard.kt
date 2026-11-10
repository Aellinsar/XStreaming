// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.LocalNxeHaptics

/**
 * 2009 Xbox 360 NXE ("Xbox Glass") Refracted Blade Card.
 *
 * Features:
 * - API 33 AGSL [NxeGlassShaders.GLASS_BLADE_AGSL] background refraction (leaving foreground
 *   text razor-sharp without self-RenderNode blur).
 * - 120Hz Material 3 spring scale animation on gamepad focus or touch press.
 * - 1.5dp Electric Cyan / semantic channel specular rim border + 2dp top light bar.
 * - Automatic API 33 LRA tactile detent tick ([com.xstreaming.nativeui.core.NxeHapticsEngine.tickBladeFocus])
 *   when focus enters via D-Pad or left analog stick.
 */
@Composable
fun NxeGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 3.dp,
    accentOverride: Color? = null,
    isHero: Boolean = false,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNxeGlassTokens.current
    val haptics = LocalNxeHaptics.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    var wasFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (isFocused && !wasFocused) {
            haptics?.tickBladeFocus()
        }
        wasFocused = isFocused
    }

    val activeHighlight = isFocused || isSelected || isHero
    val focusProgress by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            isFocused || isSelected -> 1.0f
            isHero -> 0.45f
            else -> 0.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nxeBladeFocusProgress",
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.985f
            isFocused -> 1.025f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nxeBladeScale",
    )

    val effectiveAccent = accentOverride ?: tokens.accentPrimary
    val topBodyColor = if (isHero || isFocused) tokens.glassHeroTop else tokens.glassBladeTop
    val bottomBodyColor = if (isHero || isFocused) tokens.glassHeroBottom else tokens.glassBladeBottom
    val rimColor = when {
        accentOverride != null && activeHighlight -> accentOverride.copy(alpha = 0.78f)
        activeHighlight -> tokens.borderFocusedCyan
        else -> tokens.borderGlassRest
    }

    val glassShader = remember { NxeGlassShaders.createGlassBladeShaderOrNull() }
    val shape = RoundedCornerShape(cornerRadius)

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                haptics?.clickConfirm()
                onClick()
            },
        )
    } else {
        Modifier.focusable(
            enabled = enabled,
            interactionSource = interactionSource,
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (activeHighlight) 18.dp.toPx() else 8.dp.toPx()
                this.shape = shape
                clip = false
            }
            .clip(shape)
            .then(clickableModifier)
            .drawBehind {
                val cornerPx = cornerRadius.toPx()
                val cr = CornerRadius(cornerPx, cornerPx)

                // 1. Glass Body Layer (AGSL RuntimeShader with Compose gradient fallback)
                if (glassShader != null) {
                    glassShader.setFloatUniform(
                        "resolution",
                        size.width.coerceAtLeast(1f),
                        size.height.coerceAtLeast(1f),
                    )
                    glassShader.setComposeColorUniform("topColor", topBodyColor)
                    glassShader.setComposeColorUniform("bottomColor", bottomBodyColor)
                    glassShader.setComposeColorUniform("accentColor", effectiveAccent)
                    glassShader.setFloatUniform("focusAmount", focusProgress)
                    drawRoundRect(
                        brush = ShaderBrush(glassShader),
                        cornerRadius = cr,
                    )
                } else {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(topBodyColor, bottomBodyColor),
                        ),
                        cornerRadius = cr,
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tokens.glassSheenStart,
                                tokens.glassSheenMid,
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.65f, size.height * 0.65f),
                        ),
                        cornerRadius = cr,
                    )
                }

                // 2. Subtle Top Specular Catchlight
                val strokeWidth = 1.dp.toPx()
                val halfStroke = strokeWidth / 2f
                val topBarHeight = 1.dp.toPx()
                val topEdgeTint = accentOverride ?: Color.White
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to topEdgeTint.copy(alpha = if (activeHighlight) 0.14f else 0.05f),
                        0.50f to Color.White.copy(alpha = if (activeHighlight) 0.26f else 0.10f),
                        1.0f to topEdgeTint.copy(alpha = if (activeHighlight) 0.14f else 0.05f),
                    ),
                    topLeft = Offset(strokeWidth, 0f),
                    size = Size((size.width - strokeWidth * 2f).coerceAtLeast(0f), topBarHeight),
                )

                // 3. Softened 1dp Pixel-Aligned Glass Rim
                val topRimColor = when {
                    accentOverride != null && activeHighlight -> accentOverride.copy(alpha = 0.46f)
                    activeHighlight -> Color.White.copy(alpha = 0.32f)
                    else -> Color.White.copy(alpha = 0.15f)
                }
                val bottomRimColor = when {
                    accentOverride != null && activeHighlight -> accentOverride.copy(alpha = 0.22f)
                    activeHighlight -> rimColor.copy(alpha = 0.24f)
                    else -> rimColor.copy(alpha = 0.10f)
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(topRimColor, bottomRimColor),
                    ),
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidth).coerceAtLeast(0f),
                        (size.height - strokeWidth).coerceAtLeast(0f),
                    ),
                    cornerRadius = CornerRadius(
                        (cornerPx - halfStroke).coerceAtLeast(0f),
                        (cornerPx - halfStroke).coerceAtLeast(0f),
                    ),
                    style = Stroke(width = strokeWidth),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}
