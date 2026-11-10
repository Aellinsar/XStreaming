---
name: xstreaming-build
description: Install dependencies, apply patches, run Jest and JUnit unit tests, and build the XStreaming Android APK on Windows, macOS, or Linux.
---

# XStreaming Build & Test Skill (`xstreaming-build`)

Use this skill whenever modifying JavaScript/TypeScript (`src/`) or Android native (`android/`) code in `XStreaming` and verifying tests or compiling a debug/release APK.

## 1. Prerequisites & SDK Baseline

- **Node.js & Yarn**: Node.js `>=16` with Corepack enabled (`corepack yarn`).
- **JDK**: **JDK 17 LTS** (`%USERPROFILE%\.jdks\jdk-17` on Windows, required by Gradle `8.0.1`, AGP `7.4.2`, and Kotlin `1.9.24`).
- **Android SDK & NDK (`android/build.gradle`)**:
  - `ANDROID_HOME` set to the Android SDK directory (e.g., `%LOCALAPPDATA%\Android\Sdk` on Windows).
  - **`minSdkVersion = 33`** (Android 13 - Google Pixel Tablet `tangorpro` factory baseline).
  - **`compileSdkVersion = 34` / `targetSdkVersion = 34`**, Build-Tools `34.0.0`, and NDK `25.1.8937393` (or `23.1.7779620`).
  - **Kotlin & Java Target**: `kotlinVersion = "1.9.24"`, Compose Compiler `1.5.14`, and `JavaVersion.VERSION_17` (with `react.internal.disableJavaVersionAlignment=true` in `android/gradle.properties` so `@react-native/gradle-plugin` does not downgrade `compileOptions` to Java 11).

## 2. Install JS Dependencies & Apply Patches

From the repository root (`XStreaming`):

```powershell
corepack yarn install
```

This automatically runs `postinstall` (`patch-package`), which applies:
- `patches/react-native-paper+5.12.5.patch`
- `patches/react-native-webrtc+124.0.5.patch` (adds FSR 1.0 upscaling, Smart Frame Generation, and `LowLatencyDecoderConfig` hooks to `libjingle_peerconnection_so.so`).

## 3. Run Automated Unit Tests (Mandatory Before Build/Commit)

Every bugfix or architectural change must have unit test coverage in `__tests__/` (and `android/app/src/test/` for native Kotlin/Compose modules) and pass 100%:

```powershell
# Run TypeScript / Jest unit tests
corepack yarn test --watchAll=false

# Run Android Native JUnit tests (when modifying android/app/src/main/java/com/xstreaming/nativeui/)
# Note: JAVA_TOOL_OPTIONS forces IPv4 socket initialization at JVM bootstrap when downloading Maven dependencies over VPN
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-17"
$env:JAVA_TOOL_OPTIONS = "-Djava.net.preferIPv4Stack=true"
Set-Location android
.\gradlew.bat testProdReleaseUnitTest --console=plain
```

Current Jest test suites:
- `__tests__/teredoCandidates.test.ts` - Direct LAN IPv4 (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`) vs IPv6 Teredo candidate prioritization and deduplication.
- `__tests__/webrtcBitrate.test.ts` - RFC 4566 `b=AS:<kbps>` SDP bandwidth injection and update logic in `src/webrtc/index.ts`.
- `__tests__/msalAuthentication.test.ts` - `MsalAuthentication` token validation, legacy XAL token deprecation, MMKV token caching, and device-code polling cancellation.
- `__tests__/streamReconnect.test.ts` - `xCloud` automatic WebRTC reconnect vs `xHome` clean stream exit (`exit_stream`) on DataChannel close, ICE failure, and `AppState` resume without waking powered-off consoles.
- `__tests__/connectionStatus.test.ts` - Context-aware connection HUD copy distinguishing `xHome` (`Waking Xbox Console`) from `xCloud` (`Preparing Cloud Console`).
- `__tests__/themePresets.test.ts` - Deep Royal Sapphire (`#1D4ED8`) & Electric Cyan (`#00E5FF`) frosted glass tokens and multi-accent preset resolution.

## 4. Build Production Release APK (`assembleProdRelease`)

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
Remove-Item Env:\JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue

Set-Location android
.\gradlew.bat assembleProdRelease
```

### Critical Windows Build & CMake Guardrails
- **IPv4 Stack for Gradle Maven Downloads**: Keep `-Djava.net.preferIPv4Stack=true` inside `org.gradle.jvmargs` in `android/gradle.properties` to prevent Windows IPv6 socket `Permission denied: getsockopt` errors when downloading Gradle distributions or Maven artifacts.
- **Never Set `JAVA_TOOL_OPTIONS` During CMake Configuration**: Do **not** set `$env:JAVA_TOOL_OPTIONS` when running `assembleProdRelease`. When `JAVA_TOOL_OPTIONS` is set in the environment, the JVM prints `Picked up JAVA_TOOL_OPTIONS: ...` to `stderr` when invoking Google's `prefab` CLI during `:react-native-mmkv:configureCMakeRelWithDebInfo`, which Android Gradle Plugin's CMake runner treats as a fatal error.
- **Signing**: If a release keystore (`xstreaming-release-key.keystore`) is not configured in `gradle.properties`, `android/app/build.gradle` automatically falls back to `signingConfigs.debug` so the release APK is still signed and installable via `adb install`.
- **Optional Rust (`nano-rs`) Module**: If `cargo` is not installed on the build machine, `syncNanoRustJniLibs` logs a notice and skips compiling `libnano_rs.so` (only needed for the experimental `nano` console engine; the default `native` WebRTC engine does not use it).
- **Output Path**: `android/app/build/outputs/apk/prod/release/app-prod-release.apk`
