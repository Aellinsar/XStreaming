// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui.bridge

import com.xstreaming.nativeui.core.UiEngineBootConfig
import com.xstreaming.nativeui.theme.NxeThemePresets
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Unidirectional reactive state holder (`StateFlow<XStreamingUiState>`) for the XStreaming
 * Native Android UI (`com.xstreaming.nativeui`).
 *
 * Designed for deterministic JUnit 4 unit testing with zero dependency on Android View hierarchies.
 */
object XStreamingStateStore {

    private val _state = MutableStateFlow(XStreamingUiState())
    val state: StateFlow<XStreamingUiState> = _state.asStateFlow()

    @Volatile
    var commandEmitter: ((command: String, payloadJson: String) -> Unit)? = null

    fun resetForTest(initialState: XStreamingUiState = XStreamingUiState()) {
        _state.value = initialState
    }

    @JvmStatic
    fun emitCommandToJs(command: String, payloadJson: String = "{}") {
        if (command == "startConsoleStream" || command == "startCloudStream") {
            _state.update { current ->
                val isCloud = command == "startCloudStream"
                val targetLabel = if (isCloud) {
                    "Xbox Cloud Gaming"
                } else {
                    current.selectedConsole?.name ?: "Xbox Console"
                }
                val phaseLabel = if (isCloud) {
                    "Preparing Cloud Console"
                } else if (payloadJson.contains("\"powerOnFirst\":true")) {
                    "Waking Xbox Console"
                } else {
                    "Connecting to Xbox Console"
                }
                val autoShowTouch = current.settings.showVirtualGamepad || !current.isGamepadConnected
                current.copy(
                    currentRouteScreen = "stream",
                    streamState = current.streamState.copy(
                        isStreaming = true,
                        streamType = if (isCloud) "cloud" else "home",
                        targetName = targetLabel,
                        connectionPhase = phaseLabel,
                        isHudDrawerOpen = false,
                        isPerformanceOverlayVisible = current.settings.showPerformanceOverlay,
                        isTouchGamepadVisible = autoShowTouch,
                        hasUserToggledTouchGamepad = false,
                    ),
                )
            }
        } else if (command == "exitStream") {
            _state.update { current ->
                current.copy(
                    currentRouteScreen = "home",
                    streamState = current.streamState.copy(
                        isStreaming = false,
                        connectionPhase = "",
                        isHudDrawerOpen = false,
                    ),
                )
            }
        }
        commandEmitter?.invoke(command, payloadJson)
    }

    @JvmStatic
    fun seedCachedUserProfile(gamertag: String, gamerpicUrl: String) {
        if (gamertag.isBlank() && gamerpicUrl.isBlank()) return
        _state.update { current ->
            current.copy(
                gamertag = if (current.gamertag.isBlank()) gamertag.trim() else current.gamertag,
                gamerpicUrl = if (current.gamerpicUrl.isBlank()) gamerpicUrl.trim() else current.gamerpicUrl,
            )
        }
    }

    @JvmStatic
    fun syncAuthState(
        status: String,
        message: String = "",
        deviceCode: String = "",
        verificationUri: String = "https://microsoft.com/link",
        gamertag: String = "",
        gamerpicUrl: String = "",
    ) {
        val mappedStatus = when (status.lowercase()) {
            "authenticated", "logged_in" -> AuthStatus.AUTHENTICATED
            "device_code", "device_code_pending" -> AuthStatus.DEVICE_CODE_PENDING
            "unauthenticated", "logged_out" -> AuthStatus.UNAUTHENTICATED
            else -> AuthStatus.LOADING
        }
        _state.update { current ->
            val nextModal = when {
                mappedStatus == AuthStatus.DEVICE_CODE_PENDING -> NxeActiveModal.DEVICE_CODE
                current.activeModal == NxeActiveModal.DEVICE_CODE -> null
                else -> current.activeModal
            }
            val nextGamertag = when {
                gamertag.isNotBlank() -> gamertag.trim()
                mappedStatus == AuthStatus.UNAUTHENTICATED -> ""
                else -> current.gamertag
            }
            val nextGamerpicUrl = when {
                gamerpicUrl.isNotBlank() -> gamerpicUrl.trim()
                mappedStatus == AuthStatus.UNAUTHENTICATED -> ""
                else -> current.gamerpicUrl
            }
            current.copy(
                authStatus = mappedStatus,
                authStatusMessage = message.ifBlank {
                    when (mappedStatus) {
                        AuthStatus.AUTHENTICATED -> "Signed in to Xbox Network"
                        AuthStatus.DEVICE_CODE_PENDING -> "Enter code at $verificationUri"
                        AuthStatus.UNAUTHENTICATED -> "Sign in to stream from your Xbox or Cloud"
                        AuthStatus.LOADING -> "Checking Xbox Live tokens..."
                    }
                },
                deviceCode = deviceCode,
                deviceVerificationUri = verificationUri.ifBlank { "https://microsoft.com/link" },
                gamertag = nextGamertag,
                gamerpicUrl = nextGamerpicUrl,
                activeModal = nextModal,
            )
        }
    }

    @JvmStatic
    fun updateConsoles(consoles: List<XboxConsoleItem>) {
        _state.update { current ->
            val safeIndex = current.selectedConsoleIndex.coerceIn(
                0,
                (consoles.size - 1).coerceAtLeast(0),
            )
            current.copy(
                consoles = consoles,
                selectedConsoleIndex = safeIndex,
                isRefreshingConsoles = false,
                authStatus = if (consoles.isNotEmpty() && current.authStatus == AuthStatus.LOADING) {
                    AuthStatus.AUTHENTICATED
                } else {
                    current.authStatus
                },
            )
        }
    }

    @JvmStatic
    fun selectConsoleIndex(index: Int) {
        _state.update { current ->
            if (current.consoles.isEmpty()) {
                current.copy(selectedConsoleIndex = 0)
            } else {
                current.copy(
                    selectedConsoleIndex = index.coerceIn(0, current.consoles.size - 1),
                )
            }
        }
    }

    @JvmStatic
    fun selectHomeSlot(index: Int) {
        _state.update { current ->
            current.copy(selectedHomeSlotIndex = index.coerceIn(0, 3))
        }
    }

    @JvmStatic
    fun moveHomeSlot(delta: Int): Int {
        var nextSlot = 0
        _state.update { current ->
            nextSlot = (current.selectedHomeSlotIndex + delta).coerceIn(0, 3)
            current.copy(selectedHomeSlotIndex = nextSlot)
        }
        return nextSlot
    }

    @JvmStatic
    fun settingsSlotCount(isDeveloperModeAvailable: Boolean): Int =
        if (isDeveloperModeAvailable) 8 else 7

    @JvmStatic
    fun modalForSettingsSlot(slotIndex: Int, isDeveloperModeAvailable: Boolean): NxeActiveModal =
        when (slotIndex) {
            0 -> NxeActiveModal.SETTINGS_APPEARANCE
            1 -> NxeActiveModal.SETTINGS_DISPLAY
            2 -> NxeActiveModal.SETTINGS_AUDIO
            3 -> NxeActiveModal.SETTINGS_CONTROLLER
            4 -> NxeActiveModal.SETTINGS_NETWORK
            5 -> NxeActiveModal.SETTINGS_ACCOUNT
            6 -> NxeActiveModal.ABOUT
            else -> if (isDeveloperModeAvailable) NxeActiveModal.SETTINGS_DEVELOPER else NxeActiveModal.ABOUT
        }

    @JvmStatic
    fun selectSettingsSlot(index: Int): Int {
        var clamped = 0
        _state.update { current ->
            val maxIndex = settingsSlotCount(current.settings.isDeveloperModeAvailable) - 1
            clamped = index.coerceIn(0, maxIndex)
            current.copy(selectedSettingsSlotIndex = clamped)
        }
        return clamped
    }

    @JvmStatic
    fun moveSettingsSlot(delta: Int): Int {
        var nextSlot = 0
        _state.update { current ->
            val maxIndex = settingsSlotCount(current.settings.isDeveloperModeAvailable) - 1
            nextSlot = (current.selectedSettingsSlotIndex + delta).coerceIn(0, maxIndex)
            current.copy(selectedSettingsSlotIndex = nextSlot)
        }
        return nextSlot
    }

    /**
     * Maps horizontal carousel scroll position (`0..maxScrollPx`) smoothly across all uniform-width
     * cards (`0..totalItemsCount - 1`) so scrolling left-to-right naturally steps emphasis from the
     * leftmost card (`0`) at scroll start to the rightmost card (`lastIndex`) at scroll end.
     */
    @JvmStatic
    fun computeHorizontalCarouselFocusIndex(
        totalItemsCount: Int,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffsetPx: Int,
        itemSizePx: Int,
        itemSpacingPx: Int,
        viewportWidthPx: Int,
        horizontalContentPaddingPx: Int,
    ): Int {
        if (totalItemsCount <= 1 || itemSizePx <= 0 || viewportWidthPx <= 0) return 0
        val stridePx = (itemSizePx + itemSpacingPx).coerceAtLeast(1)
        val totalContentPx = totalItemsCount * itemSizePx +
            (totalItemsCount - 1).coerceAtLeast(0) * itemSpacingPx +
            horizontalContentPaddingPx * 2
        val maxScrollPx = totalContentPx - viewportWidthPx
        if (maxScrollPx <= 0) return 0
        val currentScrollPx = (firstVisibleItemIndex * stridePx + firstVisibleItemScrollOffsetPx)
            .coerceIn(0, maxScrollPx)
        val scrollFraction = currentScrollPx.toFloat() / maxScrollPx.toFloat()
        return (scrollFraction * (totalItemsCount - 1).toFloat())
            .roundToInt()
            .coerceIn(0, totalItemsCount - 1)
    }

    @JvmStatic
    fun openAccountSettings() {
        _state.update { current ->
            current.copy(
                activeChannel = NxeDashboardChannel.SETTINGS,
                selectedSettingsSlotIndex = 5,
                activeModal = NxeActiveModal.SETTINGS_ACCOUNT,
            )
        }
    }

    @JvmStatic
    fun selectChannel(channel: NxeDashboardChannel) {
        _state.update { current ->
            current.copy(activeChannel = channel)
        }
    }

    /**
     * Cycles the active NXE channel blade left (`LB`, delta = -1) or right (`RB`, delta = +1).
     */
    @JvmStatic
    fun cycleChannelBumper(delta: Int): NxeDashboardChannel {
        val channels = NxeDashboardChannel.values()
        var nextChannel = NxeDashboardChannel.HOME
        _state.update { current ->
            val currentIdx = channels.indexOf(current.activeChannel).coerceAtLeast(0)
            val nextIdx = (currentIdx + delta).mod(channels.size)
            nextChannel = channels[nextIdx]
            current.copy(activeChannel = nextChannel)
        }
        return nextChannel
    }

    @JvmStatic
    fun openModal(modal: NxeActiveModal) {
        _state.update { current ->
            current.copy(activeModal = modal)
        }
    }

    @JvmStatic
    fun dismissModal() {
        _state.update { current ->
            current.copy(activeModal = null)
        }
    }

    @JvmStatic
    fun setGamepadConnected(connected: Boolean) {
        _state.update { current ->
            val updatedTouchVisible = if (!current.streamState.hasUserToggledTouchGamepad) {
                current.settings.showVirtualGamepad || !connected
            } else {
                current.streamState.isTouchGamepadVisible
            }
            current.copy(
                isGamepadConnected = connected,
                streamState = current.streamState.copy(
                    isTouchGamepadVisible = updatedTouchVisible,
                ),
            )
        }
    }

    @JvmStatic
    fun setCurrentRouteScreen(screenName: String) {
        val isStreamingNow = screenName.contains("stream", ignoreCase = true)
        com.xstreaming.MainActivity.updateKeepScreenOn(isStreamingNow)
        _state.update { current ->
            val autoShowTouch = current.settings.showVirtualGamepad || !current.isGamepadConnected
            current.copy(
                currentRouteScreen = screenName,
                streamState = current.streamState.copy(
                    isStreaming = isStreamingNow,
                    isHudDrawerOpen = if (!isStreamingNow) false else current.streamState.isHudDrawerOpen,
                    isTouchGamepadVisible = if (isStreamingNow && !current.streamState.isStreaming) {
                        autoShowTouch
                    } else {
                        current.streamState.isTouchGamepadVisible
                    },
                    hasUserToggledTouchGamepad = if (!isStreamingNow) false else current.streamState.hasUserToggledTouchGamepad,
                ),
            )
        }
    }

    @JvmStatic
    fun syncStreamTelemetry(
        isStreaming: Boolean,
        streamType: String,
        targetName: String,
        connectionPhase: String,
        resolutionText: String,
        fps: Int,
        bitrateMbps: Double,
        rttMs: Int,
        jitterMs: Double,
        decodeMs: Double,
        packetLossPct: Double,
    ) {
        _state.update { current ->
            current.copy(
                streamState = current.streamState.copy(
                    isStreaming = isStreaming,
                    streamType = streamType,
                    targetName = targetName.ifBlank { current.streamState.targetName },
                    connectionPhase = connectionPhase,
                    resolutionText = resolutionText.ifBlank { current.streamState.resolutionText },
                    fps = fps,
                    bitrateMbps = bitrateMbps,
                    rttMs = rttMs,
                    jitterMs = jitterMs,
                    decodeMs = decodeMs,
                    packetLossPct = packetLossPct,
                ),
            )
        }
    }

    @JvmStatic
    fun toggleStreamHudDrawer() {
        _state.update { current ->
            current.copy(
                streamState = current.streamState.copy(
                    isHudDrawerOpen = !current.streamState.isHudDrawerOpen,
                ),
            )
        }
    }

    @JvmStatic
    fun toggleStreamPerformanceOverlay() {
        _state.update { current ->
            val nextVisible = !current.streamState.isPerformanceOverlayVisible
            current.copy(
                streamState = current.streamState.copy(
                    isPerformanceOverlayVisible = nextVisible,
                ),
                settings = current.settings.copy(
                    showPerformanceOverlay = nextVisible,
                ),
            )
        }
    }

    @JvmStatic
    fun toggleStreamTouchGamepad() {
        _state.update { current ->
            val nextVisible = !current.streamState.isTouchGamepadVisible
            current.copy(
                streamState = current.streamState.copy(
                    isTouchGamepadVisible = nextVisible,
                    hasUserToggledTouchGamepad = true,
                ),
            )
        }
    }

    @JvmStatic
    fun sendVirtualGamepadInput(payloadJson: String) {
        commandEmitter?.invoke("virtualGamepadInput", payloadJson)
    }

    @JvmStatic
    fun toggleStreamMicMute() {
        _state.update { current ->
            val nextMuted = !current.streamState.isMicMuted
            emitCommandToJs("toggleStreamMic", """{"muted":$nextMuted}""")
            current.copy(
                streamState = current.streamState.copy(isMicMuted = nextMuted),
            )
        }
    }

    @JvmStatic
    fun presentSessionReport(report: SessionReportSummary) {
        _state.update { current ->
            val shouldOpenModal = current.settings.showSessionReport
            current.copy(
                lastSessionReport = report,
                activeModal = if (shouldOpenModal) NxeActiveModal.SESSION_REPORT else current.activeModal,
            )
        }
    }

    @JvmStatic
    fun applyThemePreset(presetId: String, primaryHex: String? = null, accentHex: String? = null) {
        val spec = NxeThemePresets.findPresetSpec(presetId)
        val resolvedPrimary = primaryHex ?: spec.primaryHex
        val resolvedAccent = accentHex ?: spec.accentHex
        _state.update { current ->
            current.copy(
                settings = current.settings.copy(
                    themePresetId = spec.id,
                    primaryColorHex = resolvedPrimary,
                    accentColorHex = resolvedAccent,
                ),
            )
        }
        emitCommandToJs(
            "updateTheme",
            """{"theme_preset":"${spec.id}","primary_color":"$resolvedPrimary","accent_color":"$resolvedAccent"}""",
        )
    }

    @JvmStatic
    fun recordVersionBadgeTap(): Boolean {
        var unlockedNow = false
        _state.update { current ->
            val nextCount = current.settings.versionTapCount + 1
            val unlocked = current.settings.isDeveloperModeAvailable ||
                UiEngineBootConfig.shouldUnlockDeveloperMode(nextCount)
            unlockedNow = unlocked && !current.settings.isDeveloperModeAvailable
            current.copy(
                settings = current.settings.copy(
                    versionTapCount = nextCount,
                    isDeveloperModeAvailable = unlocked,
                    isDeveloperModeEnabled = if (unlockedNow) true else current.settings.isDeveloperModeEnabled,
                ),
            )
        }
        return unlockedNow
    }

    @JvmStatic
    fun setDeveloperModeEnabled(enabled: Boolean) {
        _state.update { current ->
            current.copy(
                settings = current.settings.copy(
                    isDeveloperModeAvailable = true,
                    isDeveloperModeEnabled = enabled,
                ),
            )
        }
    }

    @JvmStatic
    fun updateCloudTitles(titles: List<CloudCatalogItem>, isLoading: Boolean = false) {
        _state.update { current ->
            current.copy(
                cloudTitles = titles,
                isLoadingCloud = isLoading,
            )
        }
    }

    @JvmStatic
    fun setCloudSearchQuery(query: String) {
        _state.update { current ->
            current.copy(cloudSearchQuery = query)
        }
    }

    @JvmStatic
    fun updateAchievements(items: List<XboxAchievementItem>, isLoading: Boolean = false) {
        _state.update { current ->
            current.copy(
                achievements = items,
                isLoadingAchievements = isLoading,
            )
        }
    }

    @JvmStatic
    fun updateSettings(transform: (NativeSettingsState) -> NativeSettingsState) {
        _state.update { current ->
            current.copy(settings = transform(current.settings))
        }
    }
}
