# Modification, Attribution & Licensing Notice (AGPL-3.0 Sections 5a & 7 / MPL-2.0)

This repository ([`Aellinsar/XStreaming`](https://github.com/Aellinsar/XStreaming)) is a modified fork of [`Geocld/XStreaming`](https://github.com/Geocld/XStreaming).

- **Original Work & Copyright:** Copyright (C) `Geocld` and original XStreaming contributors (`https://github.com/Geocld/XStreaming`).
- **Fork Modifications & Additions Copyright:** Copyright (C) 2026 `Aellinsar` (`https://github.com/Aellinsar/XStreaming`).
- **Combined Work License:** **GNU Affero General Public License v3.0 (AGPL-3.0)** - see [`LICENSE`](./LICENSE) for the full verbatim license text.
- **Standalone Original Additions License:** Newly authored standalone files, tests, and agent skills created in this fork (`android/app/src/main/java/com/xstreaming/nativeui/`, `android/app/src/test/java/com/xstreaming/nativeui/`, `__tests__/`, and `.agents/skills/`) are licensed under the **Mozilla Public License 2.0 (MPL-2.0)** with **GNU AGPL-3.0** as a compatible Secondary License (per MPL-2.0 Section 3.3), so they remain 100% compatible with upstream `Geocld/XStreaming` while carrying MPL-2.0 file-level copyleft when used independently.

---

## Additional Terms Pursuant to AGPL-3.0 Section 7

In accordance with **Section 7 of the GNU Affero General Public License v3.0**, the following additional terms apply to the modifications and additions authored in [`Aellinsar/XStreaming`](https://github.com/Aellinsar/XStreaming):

1. **Preservation of Legal Notices & Attribution (AGPL-3.0 Section 7b):**
   All copies, redistributions, or derivative works must retain both the upstream copyright notices (`Geocld` and contributors) and the fork modification notices and copyright attributions in this [`NOTICE.md`](./NOTICE.md) file.
2. **Prohibition of Misrepresentation of Origin (AGPL-3.0 Section 7c):**
   The origin of these modifications must not be misrepresented; modified versions distributed by third parties must be clearly marked as changed from [`Aellinsar/XStreaming`](https://github.com/Aellinsar/XStreaming).
3. **No Grant of Trademark Rights (AGPL-3.0 Section 7e):**
   Nothing in this License grants permission to use the trade names, trademarks, service marks, or contributor handles (`Aellinsar`, `Geocld`, or `XStreaming`) for endorsement or promotion of derivative distributions without prior written permission.

---

## Dated Summary of Modifications (AGPL-3.0 Section 5a)

### 2026-11-08 - Full Native Android UI (Jetpack Compose, Material 3, API 33 AGSL 2009 Xbox Glass & LRA Haptics), Authentication, WebRTC Native Bitrate & LAN ICE Prioritization

#### Tier 1: Upstreamable Bugfixes & UX Improvements (Candidate PRs for `Geocld/XStreaming`)
1. **MSAL OAuth 2.0 Device Code Token Caching & Polling Cancellation (`src/MsalAuthentication.ts`, `src/xal/msal.ts`)**
   - Added MMKV token persistence (`saveStreamToken`, `saveWebToken`) and fast-path cached token validation (`isStreamTokenValid`, `isWebTokenValid`) to `MsalAuthentication.getTokens()`, matching `Authentication.ts` and ensuring downstream consumers (`Cloud.tsx`, `Transfer.tsx`, `utils/xcloud.ts`) receive valid cached tokens.
   - Added `cancelDeviceCodeAuth()` to stop background OAuth token polling immediately when the user cancels the Device Code dialog.
   - Purged deprecated XAL tokens regardless of expiration state during `MsalAuthentication.checkAuthentication()`.
2. **Stale Closure Fix on Authentication Completion (`src/pages/Home.tsx`)**
   - Rebound `_authentication.current._authenticationCompleted`, `_authenticationFailed`, and `_authenticationProgress` on every `HomeScreen` effect pass so remounts or app-focus transitions while authenticating in an external browser never invoke stale state setters on an unmounted component instance.
3. **Streamlined Device Code Authentication UI (`src/components/MsalAuth.tsx`)**
   - Removed the non-functional `Completed` button (`onPress={() => setMsalBtnLoading(true)}`), added automatic clipboard copy of `user_code` on mount, and added a single-action **"Copy Code & Open Browser"** button alongside a live background-polling status indicator.
4. **Native WebRTC Custom Video/Audio Bitrate SDP Injection & Custom Codec Offer Sync (`src/webrtc/index.ts`, `src/pages/NativeStream.tsx`)**
   - Ported `setVideoBitrate()`, `setAudioBitrate()`, and RFC 4566-compliant `_setBitrate()` (`b=AS:<kbps>` after `c=` lines and before `a=` lines) to the native WebRTC client (`webRTCClient`).
   - Added `await webrtcClient.setLocalDescription(offer)` in `NativeStream.tsx` when custom codec selection mutates the SDP offer prior to `sendSDPOffer(offer)`.
5. **Cold-Start Persistence for Native Low-Latency Decoder & Stereo Audio (`src/store/settingStore.ts`)**
   - Called `syncNativeSettings()` inside `getSettings()` on initial load so `AudioSettingModule.setLowLatencyDecoderEnabled` and `AudioSettingModule.setStereoEnabled` are applied on cold start rather than only after re-saving Settings.
6. **RFC1918 Private LAN IPv4 ICE Candidate Prioritization (`src/xCloud/teredoCandidates.ts`, `__tests__/teredoCandidates.test.ts`)**
   - Prioritized direct RFC1918 private LAN IPv4 candidates (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) ahead of Teredo (`2001:0000:...`) and public STUN candidates when `ipv6` is disabled (`prioritizeIpv6 = false`).
7. **Cross-Platform Patch & Build Fixes (`patches/react-native-paper+5.12.5.patch`, `.gitattributes`)**
   - Corrected the diff hunk line count in `patches/react-native-paper+5.12.5.patch` (`@@ -262,6 +262,6 @@`) and enforced LF line endings for `*.patch` files via `.gitattributes` so `patch-package` succeeds on Windows.
8. **Console Remote Play (`xHome`) SCTP DataChannel Shutdown Detection & Clean Exit (`src/webrtc/index.ts`, `src/utils/streamReconnect.ts`, `src/pages/NativeStream.tsx`, `__tests__/streamReconnect.test.ts`)**
   - Hooked remote `closing`/`close` events on critical WebRTC SCTP DataChannels (`control`, `input`, `message`) via `_notifyRemoteChannelClosed()` so console-initiated shutdowns (*"Turn off console"* / *"Stop streaming"*) immediately transition to `CLOSED` (<50ms) instead of freezing on the last video frame for 5-15 seconds waiting for UDP ICE timeout.
   - Preserved the `streamType === 'cloud'` requirement in `canScheduleStreamReconnect()` so `xHome` never automatically calls `startSession()` (`POST /v5/sessions/home/play`, which acts as Wake-on-LAN and would force a powered-off Xbox back on), and instead exits cleanly to `Home` (`My Xbox`) via non-blocking `remoteCloseExit()` on `CLOSED`, `FAILED`, 3-second `DISCONNECTED`, or `AppState` `'active'` resume.
9. **1-Tap Browser OAuth 2.0 PKCE Sign-In, Dual-Persisted Native Token Vault & Persistent XSTS/GSSV Disk Cache (`src/MsalAuthentication.ts`, `src/xal/msal.ts`, `src/xal/tokenstore.ts`, `src/store/authVaultStore.ts`, `src/tokens/streamingtoken.ts`, `android/app/src/main/AndroidManifest.xml`, `UiEngineBootConfig.kt`, `NativeUiBridgeModule.kt`)**
   - Added 1-Tap OAuth 2.0 Authorization Code with PKCE (`S256`) using public native client `000000004c20a908` and custom redirect `ms-xal-000000004c20a908://auth` on `/consumers/oauth2/v2.0/authorize` and `/token`, plus `verification_uri_complete` (`?otc=<code>`) auto-fill for Device Code on Android TV.
   - Added dual-persistence (`authVaultStore.ts` + Android `SharedPreferences` `xstreaming_auth_vault`) and removed destructive `_tokenStore.clear()` calls on transient network/XSTS errors so 90-day rolling `refresh_token` credentials survive transient errors, MMKV resets, or UI engine switches.
   - Persisted intermediate `xstsToken` and `gssvToken` inside `TokenStore` (`user.tokenstore`) and preserved `_objectCreateTime` across `StreamingToken` rehydration.

#### Tier 2: Fork-Specific Native Android UI, Build Portability & Ergonomic Defaults
1. **Primary OAuth 2.0 Device Code Login (`src/pages/Home.tsx`, `src/App.tsx`, `src/common/settings/bases.ts`, `src/pages/SettingDetail.tsx`)**
   - Removed the embedded `<WebView>` credential-entry login path (`src/pages/Login.tsx`, `src/Authentication.ts`) to enforce system-browser OAuth 2.0 Device Code sign-in as the primary authentication path.
2. **Default Streaming & Display Settings (`src/store/settingStore.ts`, `LowLatencyDecoderConfig.java`)**
   - Set defaults tuned for local Console Remote Play (`resolution: 1081` / `1080p(HQ)`, `native_low_latency_decoder: true`, `fsr: true`, `power_on: true`, `check_update: false`, `use_msal_login: true`).
3. **Slested Frosted Glass Theme Engine & Deep Royal Sapphire + Electric Cyan Palette (`src/theme/presets.ts`, `src/utils/themeColor.ts`, `src/components/XboxSymbolBackground.tsx`, `src/components/ConsoleItem.tsx`, `src/components/HomeItem.tsx`, `src/pages/Home.tsx`, `src/components/SessionReportModal.tsx`, `__tests__/themePresets.test.ts`)**
   - Built a translucent frosted glass visual system defaulting to **Royal Sapphire & Cyan Glass (`#1D4ED8` primary with `#00E5FF` / `#22D3EE` Electric Cyan accents)** with 18 selectable main colors (`THEME_PRIMARY_COLOR_OPTIONS`) and 6 glass presets.
4. **Xbox Ring-of-Light Connection HUD & Context-Aware Status Copy (`src/components/Spinner.tsx`, `src/utils/connectionStatus.ts`, `src/pages/NativeStream.tsx`, `src/pages/Stream.tsx`, `__tests__/connectionStatus.test.ts`)**
   - Replaced the generic loading spinner with a theme-aware Xbox Ring-of-Light & 3D Guide Sphere HUD featuring a 4-quadrant connection progress bar and friendly status copy that distinguishes Console Remote Play (`Waking Xbox Console`) from Cloud Gaming (`Preparing Cloud Console`).
5. **Portable Android Gradle / NDK Configuration (`android/app/build.gradle`, `android/build.gradle`, `android/gradle.properties`)**
   - Added Windows/Linux NDK host detection (`windows-x86_64`, `linux-x86_64`), graceful skip of optional `nano-rs` Rust build when `cargo` is absent, and automatic fallback to `signingConfigs.debug` when a private release keystore is not configured.
6. **Full Native Android Jetpack Compose UI: Material 3 & 2009 Xbox 360 "Xbox Glass" (`com.xstreaming.nativeui.*`, `MainActivity.java`, `MainApplication.java`, `GamepadManager.java`, `android/app/src/test/java/com/xstreaming/nativeui/*`)**
   - Upgraded the Android platform baseline to `minSdkVersion = 33` (Android 13 - Google Pixel Tablet `tangorpro` factory baseline), `JavaVersion.VERSION_17` (JDK 17 LTS with `react.internal.disableJavaVersionAlignment=true`), Kotlin `1.9.24`, and official `Apache-2.0` Jetpack Compose (`BOM 2024.06.00`, Compose Compiler `1.5.14`, Material 3).
   - Authored the Native Android Compose UI (`NativeUiHost`, `HomeDashboardScreen`, `StreamOverlayHud`, `SettingsScreen`, `NxeModals`, `NxeGlassShader`, `NxeGlassCard`, `NxeToolbarAndModal`, `NxeHapticsEngine`, `UiEngineBootConfig`, `XStreamingStateStore`, `NativeUiBridgeModule`) featuring API 33 AGSL `RuntimeShader` studio horizon and 135-degree frosted glass refraction, horizontal 2008-2009 NXE `My Xbox` and `System Settings` card carousels (`Appearance`, `Display`, `Audio`, `Controller`, `Network & Console`, `Xbox Account`, `About XStreaming`), root RenderNode GPU backdrop blur, API 33 LRA hardware tactile haptics, real Xbox Live Gamertag (`#12345` suffix) and disk-cached Gamerpic profile avatar resolution, unified `56.dp` modal frames, 6 Universal Modal Invariants, Two-Tier Fuchsia Developer Mode (`#D946EF` / `#F0ABFC`), and conditional `native_android_ui` cold-start UI engine switching with app restart.
