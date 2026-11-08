---
name: xstreaming-build
description: Install dependencies, apply patches, run Jest unit tests, and build the XStreaming Android APK on Windows, macOS, or Linux.
---

# XStreaming Build & Test Skill (`xstreaming-build`)

Use this skill whenever modifying JavaScript/TypeScript (`src/`) or Android native (`android/`) code in `XStreaming` and verifying tests or compiling a debug/release APK.

## 1. Prerequisites

- **Node.js & Yarn:** Node.js `>=16` with Corepack enabled (`corepack yarn`).
- **JDK:** JDK 17 (`%USERPROFILE%\.jdks\jdk-17` on Windows, required by Gradle `8.0.1` and `@react-native/gradle-plugin` `0.72`).
- **Android SDK & NDK:**
  - `ANDROID_HOME` set to the Android SDK directory (e.g., `%LOCALAPPDATA%\Android\Sdk` on Windows).
  - Android SDK Platform 34, Build-Tools `34.0.0`, and NDK `25.1.8937393`.

## 2. Install JS Dependencies & Apply Patches

From the repository root (`XStreaming`):

```powershell
corepack yarn install
```

This automatically runs `postinstall` (`patch-package`), which applies:
- `patches/react-native-paper+5.12.5.patch`
- `patches/react-native-webrtc+124.0.5.patch` (adds FSR 1.0 upscaling, Smart Frame Generation, and `LowLatencyDecoderConfig` hooks to `libjingle_peerconnection_so.so`).

## 3. Run Automated Unit Tests (Mandatory Before Build/Commit)

Every bugfix or architectural change must have unit test coverage in `__tests__/` and pass the full Jest suite:

```powershell
corepack yarn test --watchAll=false
```

Current test suites:
- `__tests__/teredoCandidates.test.ts` — Direct LAN IPv4 (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`) vs IPv6 Teredo candidate prioritization and deduplication.
- `__tests__/webrtcBitrate.test.ts` — RFC 4566 `b=AS:<kbps>` SDP bandwidth injection and update logic in `src/webrtc/index.ts`.
- `__tests__/msalAuthentication.test.ts` — `MsalAuthentication` token validation, legacy XAL token deprecation, MMKV token caching, and device-code polling cancellation.
- `__tests__/streamReconnect.test.ts` — `xCloud` automatic WebRTC reconnect vs `xHome` clean stream exit (`exit_stream`) on DataChannel close, ICE failure, and `AppState` resume without waking powered-off consoles.
- `__tests__/connectionStatus.test.ts` — Context-aware connection HUD copy distinguishing `xHome` (`Waking Xbox Console`) from `xCloud` (`Preparing Cloud Console`).
- `__tests__/themePresets.test.ts` — Deep Royal Sapphire (`#1D4ED8`) & Electric Cyan (`#00E5FF`) frosted glass tokens and preset resolution.

## 4. Build Production Release APK (`assembleProdRelease`)

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Set-Location android
.\gradlew.bat assembleProdRelease
```

### Build Notes
- **Signing:** If a release keystore (`xstreaming-release-key.keystore`) is not configured in `gradle.properties`, `android/app/build.gradle` automatically falls back to `signingConfigs.debug` so the release APK is still signed and installable via `adb install`.
- **Optional Rust (`nano-rs`) Module:** If `cargo` is not installed on the build machine, `buildNanoRustAndroid` logs a notice and skips compiling `libxstreaming_nano.so` (only needed for the experimental `nano` console engine; the default `native` WebRTC engine does not use it).
- **Output Path:** `android/app/build/outputs/apk/prod/release/app-prod-release.apk`
