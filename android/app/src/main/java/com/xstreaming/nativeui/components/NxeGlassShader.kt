// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.components

import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.xstreaming.nativeui.theme.LocalNxeGlassTokens
import com.xstreaming.nativeui.theme.NxeGlassTokens
import org.intellij.lang.annotations.Language

/**
 * API 33 AGSL (Android Graphics Shading Language) shaders for the 2009 Xbox 360 NXE
 * ("Xbox Glass") studio horizon backdrop and diagonal glass-pane refraction.
 *
 * Baseline: Android 13 (API 33 - Google Pixel Tablet tangorpro / Pixel 9 Pro caiman).
 */
object NxeGlassShaders {

    /**
     * Studio Horizon Backdrop Shader:
     * Renders the 2008-2009 Xbox 360 NXE lit studio backdrop wall and optional
     * brushed reflective studio floor plane aligned directly beneath the horizontal card row.
     */
    @Language("AGSL")
    const val STUDIO_BACKDROP_AGSL = """
        uniform float2 resolution;
        uniform float4 skyColor;
        uniform float4 midColor;
        uniform float4 floorColor;
        uniform float floorHorizonY;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(resolution, float2(1.0, 1.0));

            if (floorHorizonY > 0.0) {
                // Upper lit NXE studio wall brightening smoothly toward the horizon behind the cards
                float wallT = clamp(uv.y / max(floorHorizonY, 0.01), 0.0, 1.0);
                float3 wallRgb = mix(skyColor.rgb, midColor.rgb, smoothstep(0.08, 0.96, wallT));

                // Soft wide left-center studio keylight behind the foreground My Xbox cards
                float dx = (uv.x - 0.36) * 1.05;
                float dy = (uv.y - (floorHorizonY - 0.12)) * 2.1;
                float keyGlow = exp(-(dx * dx + dy * dy) * 2.4) * 0.24;
                wallRgb += float3(0.78, 0.88, 1.0) * keyGlow + midColor.rgb * (keyGlow * 0.65);

                // Brushed reflective studio floor plane below floorHorizonY
                float floorT = clamp((uv.y - floorHorizonY) / max(1.0 - floorHorizonY, 0.01), 0.0, 1.0);
                float3 fRgb = mix(floorColor.rgb * 1.28 + float3(0.06, 0.08, 0.11), skyColor.rgb * 0.78, smoothstep(0.0, 0.92, floorT));

                // Subtle studio floor horizon sheen where the rectangular cards stand
                float horizonDist = (uv.y - floorHorizonY) * 26.0;
                float floorSheen = exp(-(horizonDist * horizonDist)) * exp(-((uv.x - 0.45) * (uv.x - 0.45)) * 1.4) * 0.14;

                float planeMix = smoothstep(floorHorizonY - 0.006, floorHorizonY + 0.012, uv.y);
                float3 finalRgb = mix(wallRgb, fRgb, planeMix) + float3(0.85, 0.92, 1.0) * floorSheen;
                return half4(clamp(finalRgb, 0.0, 1.0), 1.0);
            } else {
                // Continuous seamless studio backdrop for scrollable sub-channels & portrait mode
                float floorMix = smoothstep(0.12, 0.95, uv.y);
                float3 base = mix(skyColor.rgb, floorColor.rgb, floorMix);

                float dx = (uv.x - 0.45) * 1.15;
                float dy = (uv.y - 0.34) * 1.45;
                float radialGlow = exp(-(dx * dx + dy * dy) * 2.4) * 0.45;

                float3 finalRgb = base + midColor.rgb * radialGlow + float3(0.12, 0.16, 0.22) * (radialGlow * 0.4);
                return half4(clamp(finalRgb, 0.0, 1.0), 1.0);
            }
        }
    """

    /**
     * Diagonal 135-degree Frosted Glass Blade Refraction Shader:
     * Computes a multi-stop translucent glass body with a diagonal specular sheen
     * and subtle top-edge luminance lift behind sharp foreground typography.
     */
    @Language("AGSL")
    const val GLASS_BLADE_AGSL = """
        uniform float2 resolution;
        uniform float4 topColor;
        uniform float4 bottomColor;
        uniform float4 accentColor;
        uniform float focusAmount;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(resolution, float2(1.0, 1.0));

            // Vertical glass body gradient
            float3 bodyRgb = mix(topColor.rgb, bottomColor.rgb, uv.y);
            float bodyAlpha = mix(topColor.a, bottomColor.a, uv.y);

            // 135-degree diagonal glass-pane specular sheen band
            float diag = (uv.x * 0.65) + (uv.y * 0.35);
            float sheenBand = exp(-pow((diag - 0.28) * 5.5, 2.0)) * (0.09 + focusAmount * 0.08);

            // Top specular rim glow inside the glass pane
            float topRim = exp(-uv.y * 14.0) * (0.10 + focusAmount * 0.16);

            float3 finalRgb = bodyRgb
                + float3(sheenBand)
                + accentColor.rgb * (topRim + focusAmount * 0.07);

            float finalAlpha = clamp(bodyAlpha + sheenBand * 0.5 + focusAmount * 0.05, 0.0, 0.98);
            return half4(clamp(finalRgb, 0.0, 1.0) * finalAlpha, finalAlpha);
        }
    """

    fun createStudioBackdropShaderOrNull(): RuntimeShader? =
        runCatching { RuntimeShader(STUDIO_BACKDROP_AGSL) }.getOrNull()

    fun createGlassBladeShaderOrNull(): RuntimeShader? =
        runCatching { RuntimeShader(GLASS_BLADE_AGSL) }.getOrNull()
}

/**
 * Full-bleed 2009 Xbox 360 NXE Studio Horizon Backdrop.
 * Uses API 33 AGSL [RuntimeShader] on hardware-accelerated canvases with a
 * deterministic Compose gradient fallback.
 */
@Composable
fun NxeStudioBackdrop(
    modifier: Modifier = Modifier,
    tokens: NxeGlassTokens = LocalNxeGlassTokens.current,
    showStudioFloorPlane: Boolean = false,
    floorHorizonFraction: Float = 0.71f,
) {
    val runtimeShader = remember { NxeGlassShaders.createStudioBackdropShaderOrNull() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val effectiveHorizon = if (showStudioFloorPlane) floorHorizonFraction else -1.0f

        if (runtimeShader != null) {
            runtimeShader.setFloatUniform("resolution", width, height)
            runtimeShader.setComposeColorUniform("skyColor", tokens.bgSkyDome)
            runtimeShader.setComposeColorUniform("midColor", tokens.bgStageMid)
            runtimeShader.setComposeColorUniform("floorColor", tokens.bgStageFloor)
            runtimeShader.setFloatUniform("floorHorizonY", effectiveHorizon)
            drawRect(brush = ShaderBrush(runtimeShader))
        } else {
            // Deterministic fallback for headless/JVM preview rendering
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to tokens.bgSkyDome,
                    0.68f to tokens.bgStageMid,
                    1.0f to tokens.bgStageFloor,
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(tokens.bgStageMid.copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(width * 0.4f, height * 0.52f),
                    radius = width * 0.72f,
                ),
            )
        }
    }
}

internal fun RuntimeShader.setComposeColorUniform(name: String, color: Color) {
    setFloatUniform(name, color.red, color.green, color.blue, color.alpha)
}
