// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.bridge

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Hardware gamepad, D-pad, thumbstick, and bumper (`LB`/`RB`) navigation bus for the
 * Native Android Jetpack Compose UI (`com.xstreaming.nativeui`).
 *
 * Receives debounced menu navigation actions from [com.xstreaming.MainActivity]:
 * - Directional: `"up"`, `"down"`, `"left"`, `"right"`
 * - Channel bumpers: `"lb"`, `"rb"`
 * - Face buttons: `"select"` (`A`), `"back"` (`B`), `"x"` (`X`), `"y"` (`Y`), `"nexus"` (`Guide`)
 */
object NativeUiInputBus {

    private val _actions = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val actions: SharedFlow<String> = _actions.asSharedFlow()

    @JvmStatic
    fun dispatchAction(action: String) {
        if (action.isNotBlank()) {
            _actions.tryEmit(action)
        }
    }
}
