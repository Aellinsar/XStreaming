# XStreaming — Engineering, Upstream Sync & Agent Guide (`AGENTS.md`)

This repository ([`Aellinsar/XStreaming`](https://github.com/Aellinsar/XStreaming)) is an engineering-focused fork of [`Geocld/XStreaming`](https://github.com/Geocld/XStreaming) licensed under **GNU AGPLv3** ([`LICENSE`](./LICENSE)).

Our core engineering goals are:
1. **Clean Upstream Interoperability:** Keep changes modular, minimal-diff, and well-isolated so we can both **pull/rebase upstream changes from `Geocld/XStreaming` (`upstream/main`) effortlessly** and **extract standalone, single-topic Pull Requests back to `upstream/main`**.
2. **Engineering Rigor & Testability:** Every bugfix or behavioral change must be backed by automated unit tests in [`__tests__/`](./__tests__), follow React Native / TypeScript best practices, and keep `corepack yarn test --watchAll=false` 100% green.
3. **Sleek, Reliable User Experience:** Eliminate clunky UI patterns (such as embedded WebView password entry, dead/placebo buttons, and stale React closure hangs) in favor of clean, responsive native flows.
4. **Strict License & Privacy Compliance:** Maintain full compliance with **AGPL-3.0** and keep the repository 100% free of personal network data, device identifiers, or secrets.

---

## 1. License Compliance (GNU AGPL-3.0 + Section 7 Terms & MPL-2.0)

Upstream `Geocld/XStreaming` is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)** ([`LICENSE`](./LICENSE)), which is already the **most restrictive OSI-approved copyleft license** (stronger than MPL-2.0 alone, as AGPL-3.0 enforces whole-program copyleft, forbids proprietary linking, and closes the network/SaaS loophole).

To maximize protection while remaining 100% compliant with upstream and ready for upstream PR contributions:

1. **Combined Work under AGPL-3.0 + Section 7 Additional Terms:**
   - Under AGPL-3.0 § 5(c), the combined repository and all modifications to existing upstream files remain licensed under **AGPL-3.0** ([`LICENSE`](./LICENSE)).
   - We additionally apply **AGPL-3.0 Section 7(b), 7(c), and 7(e) supplemental restrictions** in [`NOTICE.md`](./NOTICE.md) (mandatory preservation of attribution notices, prohibition of origin misrepresentation / mandatory marking of downstream derivatives, and no trademark grant).
2. **Standalone Original Files under MPL-2.0 (Secondary License: AGPL-3.0):**
   - Newly authored standalone files, unit tests, and skills in this fork are licensed under the **Mozilla Public License 2.0 (MPL-2.0)** with **GNU AGPL-3.0** compatibility per MPL-2.0 § 3.3:
     ```ts
     // SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
     ```
   - This gives our standalone code Mozilla's file-level copyleft when extracted independently, while allowing seamless combination and upstream PR submission into AGPL-3.0 `Geocld/XStreaming`.
3. **Preserve Upstream Attribution & Document Modifications (`NOTICE.md` — AGPL-3.0 § 5a):**
   - Never remove or alter [`LICENSE`](./LICENSE) or upstream copyright/author attributions (`Geocld` and contributors) in [`README.md`](./README.md) or [`src/pages/About.tsx`](./src/pages/About.tsx).
   - Whenever adding or modifying features/fixes, update [`NOTICE.md`](./NOTICE.md) with a dated entry under either **Tier 1 (Upstreamable Bugfixes & UX Improvements)** or **Tier 2 (Fork-Specific Build & Ergonomic Defaults)**.

---

## 2. Public Repository & Privacy Guardrails

1. **Zero Personal or Network Data in Git:**
   - Never commit personal IP addresses, hostnames, MAC addresses, ADB device serials, console device names/serials, account tokens, or private keystores (`*.keystore`, `*.jks`, `local.properties`).
   - Always use generic placeholders (`<device_ip>:<port>`, `<tablet_ip>:<port>`, `<phone_ip>:<port>`, `<tv_ip>:5555`, `<esedre_host>:<port>`, `%LOCALAPPDATA%\Android\Sdk`) in docs, skills, tests, and comments.
   - Keep `.esedre/` gitignored ([`.gitignore`](./.gitignore)) so local Esedre CLI wrappers, remote daemon URLs (`esedre.json`), and ticket snapshots (`snapshot.json`) are never committed to the public repository.
2. **Pre-Commit Privacy Check:**
   - Before committing, verify `git status` and `git diff` (and scan tracked/untracked files) to ensure no local environment artifacts, hostnames, private IPs, or credentials are staged.

---

## 3. Architecture for Upstream Sync & Contributing Back (`upstream/main` ↔ `origin/main`)

### Remote Topology
- `origin` → `https://github.com/Aellinsar/XStreaming.git` (our public fork)
- `upstream` → `https://github.com/Geocld/XStreaming.git` (original upstream repository)

### Two-Tier Change Separation
To make upstream PRs easy to propose in isolation and upstream merges conflict-free, we classify every change into one of two tiers (tracked in [`NOTICE.md`](./NOTICE.md)):

#### Tier 1: Upstreamable Engineering Fixes & UX Improvements
Self-contained bugfixes, protocol compliance fixes, and UX improvements that benefit all `XStreaming` users and should be proposed to `Geocld/XStreaming` via isolated PR branches (`fix/<topic>` or `upstream-pr/<topic>`):
- **MSAL Token Caching & Polling Cancellation** ([`src/MsalAuthentication.ts`](./src/MsalAuthentication.ts), [`src/xal/msal.ts`](./src/xal/msal.ts))
- **HomeScreen Auth Callback Stale-Closure Fix** ([`src/pages/Home.tsx`](./src/pages/Home.tsx))
- **Streamlined Device Code Auth UI** ([`src/components/MsalAuth.tsx`](./src/components/MsalAuth.tsx))
- **Native WebRTC Custom Bitrate SDP Injection (`b=AS:<kbps>`) & Custom Codec `setLocalDescription` Sync** ([`src/webrtc/index.ts`](./src/webrtc/index.ts), [`src/pages/NativeStream.tsx`](./src/pages/NativeStream.tsx))
- **Cold-Start Native Decoder/Audio Settings Sync** ([`src/store/settingStore.ts`](./src/store/settingStore.ts))
- **RFC1918 Private LAN IPv4 ICE Candidate Prioritization** ([`src/xCloud/teredoCandidates.ts`](./src/xCloud/teredoCandidates.ts))
- **Console Remote Play (`xHome`) SCTP DataChannel Shutdown Detection & Clean Exit Without Forced Wake** ([`src/webrtc/index.ts`](./src/webrtc/index.ts), [`src/utils/streamReconnect.ts`](./src/utils/streamReconnect.ts), [`src/pages/NativeStream.tsx`](./src/pages/NativeStream.tsx))
- **Windows `patch-package` Line-Ending & Hunk Fix** ([`patches/react-native-paper+5.12.5.patch`](./patches/react-native-paper+5.12.5.patch), [`.gitattributes`](./.gitattributes))

#### Tier 2: Fork-Specific Build & Ergonomic Defaults
Opinionated defaults and local developer ergonomics maintained on our fork's `main` branch:
- Enforcing OAuth 2.0 Device Code (`MsalAuthentication`) as the sole login flow and removing the embedded `<WebView>` login route ([`src/App.tsx`](./src/App.tsx), [`src/pages/Home.tsx`](./src/pages/Home.tsx)).
- Default settings tuned for high-DPI tablet & console Remote Play (`resolution: 1081`, `native_low_latency_decoder: true`, `fsr: true`, `power_on: true`, `check_update: false`).
- Sleek Translucent Frosted Glass UI & **Deep Royal Sapphire (`#1D4ED8`) + Electric Cyan (`#00E5FF`)** theme architecture ([`src/theme/presets.ts`](./src/theme/presets.ts), [`src/utils/themeColor.ts`](./src/utils/themeColor.ts), [`src/components/XboxSymbolBackground.tsx`](./src/components/XboxSymbolBackground.tsx), [`src/components/ConsoleItem.tsx`](./src/components/ConsoleItem.tsx), [`src/components/HomeItem.tsx`](./src/components/HomeItem.tsx)) and Ring-of-Light Connection HUD ([`src/components/Spinner.tsx`](./src/components/Spinner.tsx), [`src/utils/connectionStatus.ts`](./src/utils/connectionStatus.ts)).
- Portable Gradle/NDK build fallbacks (Windows NDK host detection, optional `nano-rs` skip when `cargo` is absent, debug signing fallback when release keystore is absent).
- Repository agent configuration ([`AGENTS.md`](./AGENTS.md), [`NOTICE.md`](./NOTICE.md), [`.agents/skills/`](./.agents/skills)).

### Critical Protocol & UI Design Guardrails
1. **Never Auto-Call `startSession()` on `xHome` Disconnect/Shutdown (`src/utils/streamReconnect.ts`, `src/pages/NativeStream.tsx`):**
   - Unlike `xCloud` (`streamType === 'cloud'`), which caches `this.sessionPath` to renegotiate against an active cloud server blade, `XcloudApi.startSession()` on Console Remote Play (`xHome`, `streamType !== 'cloud'`) always sends a new `POST /v5/sessions/home/play` request.
   - Because `POST /v5/sessions/home/play` acts as the **Xbox Live Wake-on-LAN / Remote Wake command**, auto-reconnecting `xHome` after the user selects *"Turn off console"* forces a powered-off Xbox back on.
   - Always keep `canScheduleStreamReconnect()` restricted to `streamType === 'cloud'`. On `xHome`, detect console-initiated shutdown immediately (<50ms) via SCTP DataChannel (`control`, `input`, `message`) `closing`/`close` events (`_notifyRemoteChannelClosed` in `src/webrtc/index.ts`), and exit cleanly to `Home` (`My Xbox`) via non-blocking `remoteCloseExit()`.
2. **Sleek Frosted Glass & Royal Sapphire + Electric Cyan UI (`src/theme/presets.ts`, `src/components/*`):**
   - Never introduce 2005 "Blades"-era concentric wireframe rings, target circles, curved horizon bezier arcs, or metallic pie-wedge discs.
   - Maintain translucent multi-stop SVG glass gradients (`stopOpacity` `0.36–0.64`) with diagonal glass pane reflections, crisp `1px` **Electric Cyan (`#00E5FF` / `#22D3EE`)** borders and top specular light bars, and **Deep Royal Sapphire (`#1D4ED8` / `#1E3A8A`)** glass bodies—avoiding 100%-opaque dark-navy card fills or washed-out white pastel radial overlays.
3. **Surgical Diffs & Backward-Compatible Signatures:**
   - Never run bulk auto-formatters across untouched lines in upstream files (`Home.tsx`, `NativeStream.tsx`, `App.tsx`, `xCloud/index.ts`).
   - Extract new decision logic into standalone pure TypeScript modules (`src/utils/streamReconnect.ts`, `src/utils/connectionStatus.ts`, `src/xCloud/teredoCandidates.ts`) with full Jest unit tests.

---

## 4. Engineering & Testing Standards

1. **Mandatory Unit Tests (`__tests__/`):**
   - Every bugfix or logic change must have corresponding Jest unit tests in [`__tests__/`](./__tests__):
     - [`__tests__/teredoCandidates.test.ts`](./__tests__/teredoCandidates.test.ts): LAN IPv4 vs IPv6 Teredo candidate ordering and deduplication.
     - [`__tests__/webrtcBitrate.test.ts`](./__tests__/webrtcBitrate.test.ts): RFC 4566 `b=AS:<kbps>` SDP bandwidth injection and replacement across video/audio media sections.
     - [`__tests__/msalAuthentication.test.ts`](./__tests__/msalAuthentication.test.ts): MSAL token validation, legacy XAL token deprecation, MMKV token caching, and device-code polling cancellation.
     - [`__tests__/streamReconnect.test.ts`](./__tests__/streamReconnect.test.ts): `xCloud` auto-reconnect vs `xHome` clean exit (`exit_stream`) on DataChannel close, ICE failure, and `AppState` resume without waking powered-off consoles.
     - [`__tests__/connectionStatus.test.ts`](./__tests__/connectionStatus.test.ts): Context-aware connection HUD copy distinguishing `xHome` (`Waking Xbox Console`) from `xCloud` (`Preparing Cloud Console`).
     - [`__tests__/themePresets.test.ts`](./__tests__/themePresets.test.ts): Deep Royal Sapphire (`#1D4ED8`) & Electric Cyan (`#00E5FF`) default glass tokens and preset resolution.
2. **Run Tests Before Building or Committing:**
   ```powershell
   corepack yarn test --watchAll=false
   ```
   All test suites must pass (`0 failed`) before building a release APK or opening a PR.

---

## 5. Build & Multi-Device ADB Deployment Quick Reference

### Build Signed Production Release APK (Windows)
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-17"
Set-Location android
.\gradlew.bat assembleProdRelease
```
- **Output APK:** `android/app/build/outputs/apk/prod/release/app-prod-release.apk`

### Deploy via ADB Wireless Debugging
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb start-server

# One-time per device: suppress Play Protect "Unknown to Google Play" prompt on ADB installs
& $adb -s <device_ip>:<connect_port> shell settings put global verifier_verify_adb_installs 0

# Install / upgrade APK preserving app data
& $adb -s <device_ip>:<connect_port> install -r "android\app\build\outputs\apk\prod\release\app-prod-release.apk"
```

---

## 6. Available Agent Skills ([`.agents/skills/`](./.agents/skills))

- [`.agents/skills/upstream-sync-and-pr/SKILL.md`](./.agents/skills/upstream-sync-and-pr/SKILL.md): Integrating upstream commits from `Geocld/XStreaming` (`upstream/main`), resolving conflicts cleanly, and extracting isolated `upstream-pr/<topic>` branches to contribute fixes back upstream.
- [`.agents/skills/xstreaming-build/SKILL.md`](./.agents/skills/xstreaming-build/SKILL.md): Running Jest unit tests, applying `patch-package` patches, and building the Android release APK across Windows, macOS, and Linux.
- [`.agents/skills/adb-multi-device-deploy/SKILL.md`](./.agents/skills/adb-multi-device-deploy/SKILL.md): Pairing, connecting, suppressing Play Protect USB/ADB verification prompts, and deploying APKs across phones, tablets, and Android TV devices.
