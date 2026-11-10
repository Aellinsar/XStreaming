// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.core

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Android 13+ (API 33) Linear Resonant Actuator (LRA) tactile haptics engine for the
 * 2009 Xbox 360 NXE ("Xbox Glass") Native UI.
 *
 * Uses [VibrationAttributes.USAGE_HARDWARE_FEEDBACK] and [VibrationEffect.Composition]
 * primitives tuned for the LRA motors in the Google Pixel 9 Pro (`caiman`) and
 * Pixel Tablet (`tangorpro`).
 */
class NxeHapticsEngine(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    }

    private val hardwareFeedbackAttributes: VibrationAttributes by lazy {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
            .build()
    }

    /**
     * Crisp mechanical detent tick fired when D-pad, thumbstick, or touch focus moves
     * across 2009 NXE glass blades or swatch selectors.
     */
    fun tickBladeFocus() {
        performPrimitiveOrPredefined(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_TICK,
            scale = 0.65f,
            fallbackEffectId = VibrationEffect.EFFECT_TICK,
        )
    }

    /**
     * Soft low-frequency mechanical thud fired on `LB`/`RB` bumper channel switches
     * and modal sheet transitions.
     */
    fun thudBumper() {
        performPrimitiveOrPredefined(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            scale = 0.80f,
            fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
        )
    }

    /**
     * Tactile confirmation click fired on `Start stream`, `(A)` confirm, or clipboard copy.
     */
    fun clickConfirm() {
        performPrimitiveOrPredefined(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_CLICK,
            scale = 0.90f,
            fallbackEffectId = VibrationEffect.EFFECT_CLICK,
        )
    }

    /**
     * Light release tick fired on `(B)` back or modal dismissal.
     */
    fun tickBack() {
        performPrimitiveOrPredefined(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_TICK,
            scale = 0.40f,
            fallbackEffectId = VibrationEffect.EFFECT_TICK,
        )
    }

    private fun performPrimitiveOrPredefined(
        primitiveId: Int,
        scale: Float,
        fallbackEffectId: Int,
    ) {
        if (!UiEngineBootConfig.isUiHapticsEnabled(context)) {
            return
        }
        val activeVibrator = vibrator ?: return
        if (!activeVibrator.hasVibrator()) {
            return
        }
        try {
            val effect = if (activeVibrator.areAllPrimitivesSupported(primitiveId)) {
                VibrationEffect.startComposition()
                    .addPrimitive(primitiveId, scale.coerceIn(0.1f, 1.0f))
                    .compose()
            } else {
                VibrationEffect.createPredefined(fallbackEffectId)
            }
            activeVibrator.vibrate(effect, hardwareFeedbackAttributes)
        } catch (_: Throwable) {
            // Ignore hardware vibrator security/service exceptions on headless/TV devices
        }
    }
}
