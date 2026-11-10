// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.xstreaming.nativeui.bridge.AuthStatus
import com.xstreaming.nativeui.bridge.NativeUiInputBus
import com.xstreaming.nativeui.bridge.NxeDashboardChannel
import com.xstreaming.nativeui.bridge.XStreamingStateStore
import com.xstreaming.nativeui.core.UiEngineBootConfig
import com.xstreaming.nativeui.screens.HomeDashboardScreen
import com.xstreaming.nativeui.screens.StreamOverlayHud
import com.xstreaming.nativeui.theme.LocalNxeHaptics
import com.xstreaming.nativeui.theme.NxeGlassTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Host entry point that conditionally mounts the Jetpack Compose 2009 Xbox Glass UI
 * (`com.xstreaming.nativeui`) inside [com.xstreaming.MainActivity] when
 * [UiEngineBootConfig.isNativeUiEnabled] is `true`.
 */
object NativeUiHost {

    @JvmStatic
    fun attachIfEnabled(activity: Activity) {
        if (!UiEngineBootConfig.isNativeUiEnabled(activity)) {
            return
        }

        // Hydrate fast-boot SharedPreferences into XStreamingStateStore in <1ms
        val prefs = activity.getSharedPreferences(UiEngineBootConfig.PREFS_NAME, Activity.MODE_PRIVATE)
        val presetId = prefs.getString(
            UiEngineBootConfig.KEY_THEME_PRESET,
            UiEngineBootConfig.DEFAULT_THEME_PRESET,
        ) ?: UiEngineBootConfig.DEFAULT_THEME_PRESET
        val primaryHex = prefs.getString(
            UiEngineBootConfig.KEY_PRIMARY_COLOR,
            UiEngineBootConfig.DEFAULT_PRIMARY_COLOR,
        ) ?: UiEngineBootConfig.DEFAULT_PRIMARY_COLOR
        val accentHex = prefs.getString(
            UiEngineBootConfig.KEY_ACCENT_COLOR,
            UiEngineBootConfig.DEFAULT_ACCENT_COLOR,
        ) ?: UiEngineBootConfig.DEFAULT_ACCENT_COLOR
        val devAvailable = UiEngineBootConfig.isDeveloperModeAvailable(activity)
        val devEnabled = UiEngineBootConfig.isDeveloperModeEnabled(activity)
        val hapticsEnabled = UiEngineBootConfig.isUiHapticsEnabled(activity)
        val cachedGamertag = prefs.getString(UiEngineBootConfig.KEY_CACHED_GAMERTAG, "") ?: ""
        val cachedGamerpicUrl = prefs.getString(UiEngineBootConfig.KEY_CACHED_GAMERPIC_URL, "") ?: ""

        XStreamingStateStore.seedCachedUserProfile(
            gamertag = cachedGamertag,
            gamerpicUrl = cachedGamerpicUrl,
        )
        XStreamingStateStore.updateSettings { current ->
            current.copy(
                themePresetId = presetId,
                primaryColorHex = primaryHex,
                accentColorHex = accentHex,
                isDeveloperModeAvailable = devAvailable,
                isDeveloperModeEnabled = devEnabled,
                uiHapticsEnabled = hapticsEnabled,
                nativeAndroidUiEnabled = true,
            )
        }

        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NativeUiRoot()
            }
        }

        activity.addContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
}

@Composable
fun NativeUiRoot() {
    val state by XStreamingStateStore.state.collectAsState()

    NxeGlassTheme(
        presetId = state.settings.themePresetId,
        customPrimaryHex = state.settings.primaryColorHex,
        customAccentHex = state.settings.accentColorHex,
    ) {
        val focusManager = LocalFocusManager.current
        val haptics = LocalNxeHaptics.current

        // Route hardware gamepad, D-pad, and LB/RB bumper actions from MainActivity
        LaunchedEffect(Unit) {
            NativeUiInputBus.actions.collectLatest { action ->
                when (action) {
                    "up" -> {
                        haptics?.tickBladeFocus()
                        runCatching { focusManager.moveFocus(FocusDirection.Up) }
                    }
                    "down" -> {
                        haptics?.tickBladeFocus()
                        runCatching { focusManager.moveFocus(FocusDirection.Down) }
                    }
                    "left" -> {
                        haptics?.tickBladeFocus()
                        if (state.activeModal == null && state.activeChannel == NxeDashboardChannel.HOME) {
                            XStreamingStateStore.moveHomeSlot(-1)
                        } else if (state.activeModal == null && state.activeChannel == NxeDashboardChannel.SETTINGS) {
                            XStreamingStateStore.moveSettingsSlot(-1)
                        } else {
                            runCatching { focusManager.moveFocus(FocusDirection.Left) }
                        }
                    }
                    "right" -> {
                        haptics?.tickBladeFocus()
                        if (state.activeModal == null && state.activeChannel == NxeDashboardChannel.HOME) {
                            XStreamingStateStore.moveHomeSlot(1)
                        } else if (state.activeModal == null && state.activeChannel == NxeDashboardChannel.SETTINGS) {
                            XStreamingStateStore.moveSettingsSlot(1)
                        } else {
                            runCatching { focusManager.moveFocus(FocusDirection.Right) }
                        }
                    }
                    "lb" -> {
                        haptics?.thudBumper()
                        XStreamingStateStore.cycleChannelBumper(-1)
                    }
                    "rb" -> {
                        haptics?.thudBumper()
                        XStreamingStateStore.cycleChannelBumper(1)
                    }
                    "select" -> {
                        haptics?.clickConfirm()
                        if (state.activeModal == null && state.currentRouteScreen == "home") {
                            when {
                                state.activeChannel == NxeDashboardChannel.HOME -> {
                                    when (state.selectedHomeSlotIndex) {
                                        0 -> {
                                            if (state.authStatus == AuthStatus.UNAUTHENTICATED && state.consoles.isEmpty()) {
                                                XStreamingStateStore.emitCommandToJs("startWebLogin")
                                            } else if (state.selectedConsole != null) {
                                                val console = state.selectedConsole
                                                val serverId = console?.serverId.orEmpty()
                                                val shouldPowerOn = console != null &&
                                                    !console.isPoweredOn &&
                                                    state.settings.autoWakeConsole
                                                XStreamingStateStore.emitCommandToJs(
                                                    "startConsoleStream",
                                                    """{"serverId":"$serverId","powerOnFirst":$shouldPowerOn}""",
                                                )
                                            } else {
                                                XStreamingStateStore.emitCommandToJs("refreshConsoles")
                                            }
                                        }
                                        1 -> XStreamingStateStore.selectChannel(NxeDashboardChannel.CLOUD)
                                        2 -> XStreamingStateStore.selectChannel(NxeDashboardChannel.ACHIEVEMENTS)
                                        3 -> XStreamingStateStore.selectChannel(NxeDashboardChannel.SETTINGS)
                                    }
                                }
                                state.activeChannel == NxeDashboardChannel.SETTINGS -> {
                                    val modal = XStreamingStateStore.modalForSettingsSlot(
                                        slotIndex = state.selectedSettingsSlotIndex,
                                        isDeveloperModeAvailable = state.settings.isDeveloperModeAvailable,
                                    )
                                    XStreamingStateStore.openModal(modal)
                                }
                                state.activeChannel == NxeDashboardChannel.CLOUD -> {
                                    XStreamingStateStore.emitCommandToJs("refreshCloudTitles")
                                }
                            }
                        }
                    }
                    "y" -> {
                        if (state.activeModal == null && state.currentRouteScreen == "home") {
                            haptics?.tickBladeFocus()
                            XStreamingStateStore.emitCommandToJs("refreshConsoles")
                        }
                    }
                    "back" -> {
                        haptics?.tickBack()
                        when {
                            state.activeModal != null -> XStreamingStateStore.dismissModal()
                            state.streamState.isHudDrawerOpen -> XStreamingStateStore.toggleStreamHudDrawer()
                            state.currentRouteScreen != "home" && state.currentRouteScreen != "stream" -> {
                                XStreamingStateStore.setCurrentRouteScreen("home")
                                XStreamingStateStore.emitCommandToJs("navigateHome")
                            }
                            state.activeChannel != NxeDashboardChannel.HOME -> {
                                XStreamingStateStore.selectChannel(NxeDashboardChannel.HOME)
                            }
                        }
                    }
                    "nexus" -> {
                        haptics?.clickConfirm()
                        if (state.currentRouteScreen == "stream") {
                            XStreamingStateStore.toggleStreamHudDrawer()
                        }
                    }
                }
            }
        }

        if (state.currentRouteScreen.equals("stream", ignoreCase = true)) {
            StreamOverlayHud(streamState = state.streamState)
        } else {
            HomeDashboardScreen(state = state)
        }
    }
}
