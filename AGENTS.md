# XStreaming - Engineering, Upstream Sync & Agent Guide (`AGENTS.md`)

This repository ([`Aellinsar/XStreaming`](https://github.com/Aellinsar/XStreaming)) is an engineering-focused fork of [`Geocld/XStreaming`](https://github.com/Geocld/XStreaming) licensed under **GNU AGPLv3** ([`LICENSE`](./LICENSE)).

## Skills Quick Reference & Knowledge Domain Directory

| Knowledge Domain | Specialized Skills | Focus Area |
|---|---|---|
| **Architecture, Design & Tests** | [engineering-design-and-test-standards](.agents/skills/engineering-design-and-test-standards/SKILL.md) | Unidirectional `StateFlow` architecture, 3rd-party dependency license vetting, `56dp` toolbars, 6 Modal Invariants, Fuchsia Developer Mode, feature flags, dual Jest/JUnit testing, git/commit standards. |
| **Native Android Compose UI** | [native-compose-nxe-ui](.agents/skills/native-compose-nxe-ui/SKILL.md) | `minSdkVersion = 33` (Android 13 - Pixel Tablet baseline), JDK 17 LTS, Kotlin `1.9.24`, Jetpack Compose, Material 3, API 33 AGSL `RuntimeShader` 2009 Xbox Glass refraction, LRA tactile haptics, `UiEngineBootConfig`. |
| **Build, Test & Packaging** | [xstreaming-build](.agents/skills/xstreaming-build/SKILL.md) | Jest (`corepack yarn test --watchAll=false`) and JUnit (`testProdReleaseUnitTest`) execution, `patch-package`, Windows CMake/Prefab guardrails, `assembleProdRelease`. |
| **Multi-Device ADB Deploy** | [adb-multi-device-deploy](.agents/skills/adb-multi-device-deploy/SKILL.md) | ADB Wireless Debugging pairing, Play Protect prompt suppression, multi-device deployment (Pixel 9 Pro `caiman`, Pixel Tablet `tangorpro`), VPN split tunneling. |
| **Upstream Sync & PRs** | [upstream-sync-and-pr](.agents/skills/upstream-sync-and-pr/SKILL.md) | Syncing upstream commits from `Geocld/XStreaming` (`upstream/main`) and extracting isolated `upstream-pr/<topic>` branches under AGPL-3.0. |
| **Esedre Roadmap & Tickets** | [esedre](.agents/skills/esedre/SKILL.md) | Esedre MCP/CLI developer ticketing, milestone management, and roadmap tracking (`Xbox` project). |

---

## 1. License Compliance & Vetted 3rd-Party Dependencies (GNU AGPL-3.0 + MPL-2.0)

Upstream `Geocld/XStreaming` is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)** ([`LICENSE`](./LICENSE)).

1. **Combined Work under AGPL-3.0 + Section 7 Additional Terms**:
   - Under AGPL-3.0 Section 5(c), the combined repository and all modifications to existing upstream files remain licensed under **AGPL-3.0** ([`LICENSE`](./LICENSE)).
   - We additionally apply **AGPL-3.0 Section 7(b), 7(c), and 7(e) supplemental restrictions** in [`NOTICE.md`](./NOTICE.md) (mandatory preservation of attribution notices, prohibition of origin misrepresentation / mandatory marking of downstream derivatives, and no trademark grant).
2. **Standalone Original Files under MPL-2.0 (Secondary License: AGPL-3.0)**:
   - Newly authored standalone files, unit tests, and skills in this fork (`android/app/src/main/java/com/xstreaming/nativeui/`, `android/app/src/test/java/com/xstreaming/nativeui/`, and standalone TypeScript helpers) are licensed under the **Mozilla Public License 2.0 (MPL-2.0)** with **GNU AGPL-3.0** compatibility per MPL-2.0 Section 3.3:
     ```kotlin
     // SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
     ```
3. **Strict 3rd-Party Dependency Vetting**:
   - Prefer official first-party Google AndroidX / Jetpack (`androidx.compose.*`, `androidx.activity.*`, `androidx.lifecycle.*`, `androidx.navigation.*` - all `Apache-2.0`) and official JetBrains Kotlin (`org.jetbrains.kotlin.*`, `org.jetbrains.kotlinx.*` - `Apache-2.0`) libraries.
   - Never add proprietary/closed-source SDKs, `GPL-2.0-only` dependencies, or unmaintained 3rd-party blur/haptic/UI wrappers when Android API 33 (`RuntimeShader`, `RenderEffect`, `VibrationEffect.Composition`) provides native hardware-accelerated APIs.
4. **Preserve Upstream Attribution & Document Modifications (`NOTICE.md` - AGPL-3.0 Section 5a)**:
   - Never remove or alter [`LICENSE`](./LICENSE) or upstream copyright/author attributions (`Geocld` and contributors) in [`README.md`](./README.md) or [`src/pages/About.tsx`](./src/pages/About.tsx).
   - Update [`NOTICE.md`](./NOTICE.md) whenever adding or modifying features/fixes.

---

## 2. Developer Planner, Esedre Dogfooding & Ticket Invariants

- **Primary Read-Only Inspection**: Always read `.esedre/snapshot.json` in the workspace root first for zero-latency ticket context (`Xbox` project).
- **Strict Maximum Title Length (48 Characters)**: All ticket titles MUST be strictly capped at **48 characters** max (`MAX_TICKET_TITLE_LENGTH = 48`).
- **Canonical Enums**: `status` must strictly be `'Planned'`, `'In Development'`, `'Completed'`, or `'Rejected'`. `type` must strictly be `'Feature'`, `'Platform'`, `'Tools'`, `'Idea'`, or `'Bug'`.

<!-- ESEDRE_DOGFOODING_START -->
- **ESEDRE DOGFOODING & DEVELOPER FEEDBACK MANDATE**:
  - **Tooling Dogfooding Priority & MCP Preference**: The developer is also the author/creator of Esedre. Always prioritize using official Esedre tooling rigorously and correctly on all planning tasks, actively dogfooding the toolchain in practice. **Crucially, PREFER MCP tools (`esedre_*`) over CLI execution for ticket inspection, planning, comments, and lease claims whenever MCP is available in the session**, using the CLI wrapper (`.esedre/ese.cmd`) purely as a secondary fallback.
  - **Proactive Issue & Feature Gap Escalation**: Whenever you encounter any friction, bug, formatting limitation, concurrency quirk, missing CLI flag/option/tool, output anomaly, or feature gap in Esedre, you MUST prominently and explicitly surface it to the developer.
  - **Persistent Reminder Directive**: If you raise an Esedre tooling issue, gap, or question and the developer does not reply or acknowledge it, you MUST persistently follow up and remind them in subsequent turns until confirmed. Never let an Esedre defect, friction point, or improvement opportunity slip by unaddressed.
<!-- ESEDRE_DOGFOODING_END -->

---

## 3. Public Repository, Privacy & Git Workflow Guardrails

1. **Zero Personal or Network Data in Git**:
   - Never commit personal IP addresses, hostnames, MAC addresses, ADB device serials, console device names/serials, account tokens, or private keystores (`*.keystore`, `*.jks`, `local.properties`).
   - Always use generic placeholders (`<device_ip>:<port>`, `<tablet_ip>:<port>`, `<phone_ip>:<port>`, `<tv_ip>:5555`, `<esedre_host>:<port>`, `%LOCALAPPDATA%\Android\Sdk`) in docs, skills, tests, and comments.
   - Keep `.esedre/` gitignored ([`.gitignore`](./.gitignore)).
2. **Strict Git Commit & Push Restriction**:
   - NO commits or pushes unless the user explicitly wrote `"commit"` or `"push"` in their **most recent prompt** (zero carry-over permission across turns).
3. **Clean Commit Format & Zero Em Dashes**:
   - Never use conventional commit prefixes (`feat:`, `fix:`, `chore:`, etc.). Use `[#Xbox-<id>] <Imperative Action Summary>` or `[Docs]` / `[Infra]` / `[Release] <Summary>`.
   - Never use em dashes (`\u2014`) or en dashes (`\u2013`) anywhere in code, comments, commit messages, tickets, or documentation. Never hardcode `C:\Users\...` paths in tracked files.
4. **Planning Mode Guardrail**:
   - Never modify production code during Planning Mode without explicit user sign-off.

---

## 4. Architecture, Protocol & UI Design Guardrails

### Two-Tier Change Separation (`upstream/main` <-> `origin/main`)
1. **Tier 1 (Upstreamable Engineering Fixes in `src/`)**:
   - MSAL token caching & polling cancellation ([`src/MsalAuthentication.ts`](./src/MsalAuthentication.ts), [`src/xal/msal.ts`](./src/xal/msal.ts))
   - HomeScreen auth callback stale-closure fix ([`src/pages/Home.tsx`](./src/pages/Home.tsx))
   - Native WebRTC custom bitrate SDP injection (`b=AS:<kbps>`) ([`src/webrtc/index.ts`](./src/webrtc/index.ts))
   - Cold-start native decoder/audio settings sync ([`src/store/settingStore.ts`](./src/store/settingStore.ts))
   - RFC1918 private LAN IPv4 ICE candidate prioritization ([`src/xCloud/teredoCandidates.ts`](./src/xCloud/teredoCandidates.ts))
   - Console Remote Play (`xHome`) `<50ms` SCTP DataChannel shutdown detection & clean exit without waking powered-off consoles ([`src/webrtc/index.ts`](./src/webrtc/index.ts), [`src/utils/streamReconnect.ts`](./src/utils/streamReconnect.ts))
2. **Tier 2 (Fork-Specific Native Android UI & High-DPI Ergonomics)**:
   - **Platform Target**: `minSdkVersion = 33` (Android 13 - Google Pixel Tablet `tangorpro` factory baseline), `compileSdkVersion = 34`, **JDK 17 LTS (`JavaVersion.VERSION_17`)**, **Kotlin `1.9.24`**, and **Jetpack Compose (`1.5.14` / BOM `2024.06.00`)**.
   - **Full Native Android UI (`com.xstreaming.nativeui.*`)**: 2008-2009 Xbox 360 NXE ("New Xbox Experience") spatial stage featuring the receding 3D vertical channel hierarchy (`System Settings` -> `Achievements` -> `Cloud Gaming` -> `My Xbox`), top-right rectangular Xbox Live Gamercard, a single horizontal row of 4 crisp `3.dp`-corner rectangular beveled cards (`HomeDashboardScreen.kt`) standing pixel-flush on an API 33 AGSL `RuntimeShader` lit studio floor (`NxeGlassShader.kt`) with aligned vertical mirror reflections (`NxeCardFloorReflection`), custom vector Compose `Canvas` hardware/icon illustrations, cohesive brushed silver-white bevels and secondary typography (`#D6E2F0` - never neon cyan or rainbow sibling borders), root RenderNode backdrop blur, LRA tactile haptics (`NxeHapticsEngine`), dynamic `FLAG_KEEP_SCREEN_ON` lifecycle (`MainActivity.updateKeepScreenOn` enabled only when `currentRouteScreen == "stream"` so dashboard screens time out normally), and conditional startup boot flag (`native_android_ui` in `UiEngineBootConfig`, requiring app restart to switch between Native Android UI and Legacy React Native UI via the top-level Settings toggle). `NativeUiBridgeModule.isNativeUiEnabled()` must remain `@ReactMethod(isBlockingSynchronousMethod = true): Boolean` so React Native `<Modal>` overlays (`Spinner.tsx`, `Home.tsx`, `NativeStream.tsx`) are synchronously suppressed when Native UI is active.
   - **1-Tap Browser OAuth 2.0 PKCE & Dual-Persisted Token Vault (`#Xbox-18`)**: Supports both 1-tap Chrome Custom Tabs OAuth 2.0 PKCE (`000000004c20a908`, `ms-xal-000000004c20a908://auth` parsed via pure RFC 3986 `parseQueryParams()` without relying on React Native's incomplete `URLSearchParams.get()` stub) and 2nd-Screen Device Code (`1f907974-e22b-4810-a9de-d9647380c97e`), backed by dual-persisted MMKV + Native `SharedPreferences("xstreaming_auth_vault")` storage and cached `xstsToken`/`gssvToken`.
   - **Native Stream Overlay HUD & Touch Controller (`StreamOverlayHud.kt`)**: Animated 2009 Xbox 360 Ring-of-Light connection card (`NxeRingOfLightOrb` + 4-quadrant progress bar), compact `(X) Guide` handle at `26%` opacity (expanding to live 1Hz telemetry only when toggled), ghost-translucent (`22%-26%`) multi-touch controller (`NxeTouchGamepadOverlay`) with un-inverted screen `dy` (since `src/webrtc/channels/input.ts` negates `LeftThumbYAxis`/`RightThumbYAxis`), and automatic physical gamepad detection (`InputManager.InputDeviceListener`).
   - **Two-Tier Developer Mode & Fuchsia Palette**: `isDeveloperModeAvailable` (unlocked via 7 taps on version badge) vs `isDeveloperModeEnabled` (master runtime toggle), styled with the standardized Fuchsia palette (`#D946EF` / `#F0ABFC`).
   - **Universal Modal & `56dp` Modal Frame Invariants**: Unified `56.dp` modal header/footer bars (`NxeModalFrame`), single-line ellipsis titles, root backdrop blur, click-outside/Back/`(B)` dismissal, and zero redundant footer "Close"/"Done" buttons (never put a web navbar on `HomeDashboardScreen`).

### Critical Protocol Invariant: Never Auto-Call `startSession()` on `xHome` Shutdown
- Unlike `xCloud` (`streamType === 'cloud'`), `XcloudApi.startSession()` on Console Remote Play (`xHome`) sends a new `POST /v5/sessions/home/play` request, which acts as the **Xbox Live Wake-on-LAN / Remote Wake command**.
- Always keep `canScheduleStreamReconnect()` restricted to `streamType === 'cloud'`. On `xHome`, detect console-initiated shutdown immediately (`<50ms`) via SCTP DataChannel (`control`, `input`, `message`) `closing`/`close` events and exit cleanly to the Home dashboard without waking a powered-off Xbox.

---

## 5. Automated Testing, Build & Deployment Quick Reference

1. **Mandatory Unit Tests (Zero Failures & Zero Warnings)**:
   ```powershell
   # TypeScript / Jest unit tests (__tests__/*.test.ts)
   corepack yarn test --watchAll=false

   # Android Native Kotlin / JUnit 4 tests (android/app/src/test/java/com/xstreaming/nativeui/)
   $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
   $env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-17"
   Set-Location android
   .\gradlew.bat testProdReleaseUnitTest
   ```
2. **Build Signed Production Release APK (`assembleProdRelease`)**:
   ```powershell
   $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
   $env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-17"
   Remove-Item Env:\JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
   Set-Location android
   .\gradlew.bat assembleProdRelease
   ```
   - **Output APK**: `android/app/build/outputs/apk/prod/release/app-prod-release.apk`
3. **Deploy via ADB Wireless Debugging**:
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb start-server
   & $adb -s <device_ip>:<connect_port> shell settings put global verifier_verify_adb_installs 0
   & $adb -s <device_ip>:<connect_port> install -r "android\app\build\outputs\apk\prod\release\app-prod-release.apk"
   ```
