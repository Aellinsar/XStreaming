// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
package com.xstreaming.nativeui

import com.xstreaming.nativeui.bridge.AuthStatus
import com.xstreaming.nativeui.bridge.CloudCatalogItem
import com.xstreaming.nativeui.bridge.NxeActiveModal
import com.xstreaming.nativeui.bridge.NxeDashboardChannel
import com.xstreaming.nativeui.bridge.SessionReportSummary
import com.xstreaming.nativeui.bridge.XStreamingStateStore
import com.xstreaming.nativeui.bridge.XStreamingUiState
import com.xstreaming.nativeui.bridge.XboxAchievementItem
import com.xstreaming.nativeui.bridge.XboxConsoleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class XStreamingStateStoreTest {

    @Before
    fun setUp() {
        XStreamingStateStore.resetForTest(XStreamingUiState())
    }

    @Test
    fun `syncAuthState opens DEVICE_CODE modal when pending and dismisses on authenticated`() {
        XStreamingStateStore.syncAuthState(
            status = "device_code_pending",
            message = "Enter code",
            deviceCode = "ABCD-EFGH",
            verificationUri = "https://microsoft.com/link",
        )

        val pendingState = XStreamingStateStore.state.value
        assertEquals(AuthStatus.DEVICE_CODE_PENDING, pendingState.authStatus)
        assertEquals("ABCD-EFGH", pendingState.deviceCode)
        assertEquals(NxeActiveModal.DEVICE_CODE, pendingState.activeModal)

        XStreamingStateStore.syncAuthState(
            status = "authenticated",
            gamertag = "MasterChief117",
        )

        val authedState = XStreamingStateStore.state.value
        assertEquals(AuthStatus.AUTHENTICATED, authedState.authStatus)
        assertEquals("MasterChief117", authedState.gamertag)
        assertNull(authedState.activeModal)
    }

    @Test
    fun `cycleChannelBumper wraps cleanly across LB and RB bumpers`() {
        assertEquals(NxeDashboardChannel.HOME, XStreamingStateStore.state.value.activeChannel)

        assertEquals(NxeDashboardChannel.CLOUD, XStreamingStateStore.cycleChannelBumper(1))
        assertEquals(NxeDashboardChannel.ACHIEVEMENTS, XStreamingStateStore.cycleChannelBumper(1))
        assertEquals(NxeDashboardChannel.SETTINGS, XStreamingStateStore.cycleChannelBumper(1))
        assertEquals(NxeDashboardChannel.HOME, XStreamingStateStore.cycleChannelBumper(1))

        // Wrap backwards via LB (-1)
        assertEquals(NxeDashboardChannel.SETTINGS, XStreamingStateStore.cycleChannelBumper(-1))
    }

    @Test
    fun `updateConsoles and selectConsoleIndex clamp selection within bounds`() {
        val consoles = listOf(
            XboxConsoleItem(serverId = "XBOX-1", name = "Living Room Series X", powerState = "On"),
            XboxConsoleItem(serverId = "XBOX-2", name = "Office Series S", powerState = "ConnectedStandby"),
        )
        XStreamingStateStore.updateConsoles(consoles)
        XStreamingStateStore.selectConsoleIndex(99)

        assertEquals(1, XStreamingStateStore.state.value.selectedConsoleIndex)
        assertEquals("XBOX-2", XStreamingStateStore.state.value.selectedConsole?.serverId)
        assertTrue(XStreamingStateStore.state.value.selectedConsole?.isStandbyReady == true)
    }

    @Test
    fun `recordVersionBadgeTap unlocks two-tier developer mode on 7th tap`() {
        repeat(6) {
            assertFalse(XStreamingStateStore.recordVersionBadgeTap())
        }
        assertFalse(XStreamingStateStore.state.value.settings.isDeveloperUiActive)

        val unlockedOnSeventh = XStreamingStateStore.recordVersionBadgeTap()
        assertTrue(unlockedOnSeventh)
        assertTrue(XStreamingStateStore.state.value.settings.isDeveloperModeAvailable)
        assertTrue(XStreamingStateStore.state.value.settings.isDeveloperModeEnabled)
        assertTrue(XStreamingStateStore.state.value.settings.isDeveloperUiActive)
    }

    @Test
    fun `presentSessionReport respects showSessionReport preference and formats telemetry`() {
        val report = SessionReportSummary(
            targetName = "Living Room Series X",
            streamType = "home",
            durationText = "42m 15s",
            avgFps = 60,
            avgBitrateMbps = 34.5,
            avgRttMs = 4,
            avgDecodeMs = 1.9,
            packetLossPct = 0.0,
            exitReason = "Console Shutdown Detected (<50ms)",
        )

        XStreamingStateStore.presentSessionReport(report)
        assertEquals(NxeActiveModal.SESSION_REPORT, XStreamingStateStore.state.value.activeModal)
        assertTrue(report.toFormattedTelemetryReport().contains("Living Room Series X"))
    }

    @Test
    fun `updateCloudTitles and setCloudSearchQuery filter cloud catalog case-insensitively`() {
        val titles = listOf(
            CloudCatalogItem(titleId = "HALO-INF", name = "Halo Infinite", category = "Game Pass"),
            CloudCatalogItem(titleId = "FORZA-5", name = "Forza Horizon 5", category = "Touch + Controller"),
        )
        XStreamingStateStore.updateCloudTitles(titles, isLoading = false)
        assertEquals(2, XStreamingStateStore.state.value.filteredCloudTitles.size)

        XStreamingStateStore.setCloudSearchQuery("forza")
        assertEquals(1, XStreamingStateStore.state.value.filteredCloudTitles.size)
        assertEquals("FORZA-5", XStreamingStateStore.state.value.filteredCloudTitles.first().titleId)
    }

    @Test
    fun `updateAchievements computes Gamerscore totals and progress percentages`() {
        val achievements = listOf(
            XboxAchievementItem(
                titleId = "1",
                name = "Halo 3",
                currentGamerscore = 750,
                maxGamerscore = 1000,
            ),
            XboxAchievementItem(
                titleId = "2",
                name = "Gears of War 2",
                currentGamerscore = 500,
                maxGamerscore = 1000,
            ),
        )
        XStreamingStateStore.updateAchievements(achievements, isLoading = false)
        val state = XStreamingStateStore.state.value
        assertEquals(1250, state.totalCurrentGamerscore)
        assertEquals(2000, state.totalMaxGamerscore)
        assertEquals(75, state.achievements.first().progressPercent)
    }

    @Test
    fun `startConsoleStream auto-enables touch gamepad when no controller is paired and hides stats bar by default`() {
        XStreamingStateStore.setGamepadConnected(false)
        XStreamingStateStore.emitCommandToJs(
            "startConsoleStream",
            """{"serverId":"XBOX-1","powerOnFirst":true}""",
        )
        val streamState = XStreamingStateStore.state.value.streamState
        assertTrue(streamState.isStreaming)
        assertTrue(streamState.isTouchGamepadVisible)
        assertFalse(streamState.isPerformanceOverlayVisible)

        // User can manually hide touch controls from the stream drawer (e.g. when controller is linked directly to console)
        XStreamingStateStore.toggleStreamTouchGamepad()
        assertFalse(XStreamingStateStore.state.value.streamState.isTouchGamepadVisible)
        assertTrue(XStreamingStateStore.state.value.streamState.hasUserToggledTouchGamepad)

        // User can toggle the performance stats bar on and off from the stream drawer
        XStreamingStateStore.toggleStreamPerformanceOverlay()
        assertTrue(XStreamingStateStore.state.value.streamState.isPerformanceOverlayVisible)
        XStreamingStateStore.toggleStreamPerformanceOverlay()
        assertFalse(XStreamingStateStore.state.value.streamState.isPerformanceOverlayVisible)
    }

    @Test
    fun `moveHomeSlot navigates across 4 horizontal NXE My Xbox cards and clamps at edges`() {
        assertEquals(0, XStreamingStateStore.state.value.selectedHomeSlotIndex)
        assertEquals(0, XStreamingStateStore.moveHomeSlot(-1))
        assertEquals(1, XStreamingStateStore.moveHomeSlot(1))
        assertEquals(2, XStreamingStateStore.moveHomeSlot(1))
        assertEquals(3, XStreamingStateStore.moveHomeSlot(1))
        assertEquals(3, XStreamingStateStore.moveHomeSlot(1))
    }

    @Test
    fun `gamertagName and gamertagSuffix split modern #12345 tags and preserve classic names`() {
        XStreamingStateStore.syncAuthState(
            status = "authenticated",
            gamertag = "SpartanJohn#11701",
            gamerpicUrl = "https://images-eds-ssl.xboxlive.com/image?url=avatar117",
        )
        assertEquals("SpartanJohn", XStreamingStateStore.state.value.gamertagName)
        assertEquals("#11701", XStreamingStateStore.state.value.gamertagSuffix)
        assertEquals(
            "https://images-eds-ssl.xboxlive.com/image?url=avatar117",
            XStreamingStateStore.state.value.gamerpicUrl,
        )

        XStreamingStateStore.syncAuthState(
            status = "authenticated",
            gamertag = "ClassicGamertag",
        )
        assertEquals("ClassicGamertag", XStreamingStateStore.state.value.gamertagName)
        assertEquals("", XStreamingStateStore.state.value.gamertagSuffix)
        assertEquals(
            "https://images-eds-ssl.xboxlive.com/image?url=avatar117",
            XStreamingStateStore.state.value.gamerpicUrl,
        )

        XStreamingStateStore.syncAuthState(status = "unauthenticated")
        assertEquals("", XStreamingStateStore.state.value.gamertagName)
        assertEquals("", XStreamingStateStore.state.value.gamertagSuffix)
        assertEquals("", XStreamingStateStore.state.value.gamerpicUrl)
    }

    @Test
    fun `openAccountSettings jumps to System Settings slot 5 and opens Xbox Account modal`() {
        assertEquals(NxeDashboardChannel.HOME, XStreamingStateStore.state.value.activeChannel)
        XStreamingStateStore.openAccountSettings()
        assertEquals(NxeDashboardChannel.SETTINGS, XStreamingStateStore.state.value.activeChannel)
        assertEquals(5, XStreamingStateStore.state.value.selectedSettingsSlotIndex)
        assertEquals(NxeActiveModal.SETTINGS_ACCOUNT, XStreamingStateStore.state.value.activeModal)
    }

    @Test
    fun `moveSettingsSlot navigates across distinct System Settings cards and expands when developer mode unlocks`() {
        assertEquals(7, XStreamingStateStore.settingsSlotCount(false))
        assertEquals(0, XStreamingStateStore.state.value.selectedSettingsSlotIndex)
        assertEquals(0, XStreamingStateStore.moveSettingsSlot(-1))
        assertEquals(6, XStreamingStateStore.selectSettingsSlot(99))
        assertEquals(NxeActiveModal.ABOUT, XStreamingStateStore.modalForSettingsSlot(6, false))

        // Unlock developer mode via 7 taps and verify 8th Developer Options card becomes reachable
        repeat(7) { XStreamingStateStore.recordVersionBadgeTap() }
        assertTrue(XStreamingStateStore.state.value.settings.isDeveloperModeAvailable)
        assertEquals(8, XStreamingStateStore.settingsSlotCount(true))
        assertEquals(7, XStreamingStateStore.moveSettingsSlot(1))
        assertEquals(NxeActiveModal.SETTINGS_DEVELOPER, XStreamingStateStore.modalForSettingsSlot(7, true))
    }

    @Test
    fun `computeHorizontalCarouselFocusIndex steps smoothly across all System Settings cards during scroll`() {
        // 1280dp tablet @ 2x density = 2560px viewport, 542px cards, 32px spacing, 88px side padding
        val itemSizePx = 542
        val itemSpacingPx = 32
        val stridePx = itemSizePx + itemSpacingPx
        val viewportPx = 2560
        val padPx = 88
        val totalCards = 7
        val totalContentPx = totalCards * itemSizePx + (totalCards - 1) * itemSpacingPx + padPx * 2
        val maxScrollPx = totalContentPx - viewportPx

        val visited = (0..6).map { slot ->
            val scrollPx = ((slot.toFloat() / 6f) * maxScrollPx).toInt()
            val firstIdx = scrollPx / stridePx
            val firstOffset = scrollPx % stridePx
            XStreamingStateStore.computeHorizontalCarouselFocusIndex(
                totalItemsCount = totalCards,
                firstVisibleItemIndex = firstIdx,
                firstVisibleItemScrollOffsetPx = firstOffset,
                itemSizePx = itemSizePx,
                itemSpacingPx = itemSpacingPx,
                viewportWidthPx = viewportPx,
                horizontalContentPaddingPx = padPx,
            )
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), visited)
    }
}
