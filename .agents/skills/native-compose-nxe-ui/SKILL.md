---
name: native-compose-nxe-ui
description: Design, build, and test the XStreaming Native Android UI using Kotlin 1.9.24, JDK 17, API 33 (Android 13), Jetpack Compose, Material 3, AGSL RuntimeShaders (2009 Xbox 360 NXE Glass aesthetic), and LRA tactile haptics.
---

# Native Android UI Skill: Material 3 & 2009 Xbox 360 "Xbox Glass" (`native-compose-nxe-ui`)

Use this skill whenever designing, implementing, or debugging XStreaming's native Android UI (`android/app/src/main/java/com/xstreaming/nativeui/`) under Milestone 2 (`Xbox-13` through `Xbox-17`). Always pair this skill with [engineering-design-and-test-standards](../engineering-design-and-test-standards/SKILL.md).

---

## 1. Platform, Toolchain & Vetted Dependency Baseline

- **`minSdkVersion = 33` (Android 13 - Google Pixel Tablet `tangorpro` Factory Baseline)**:
  - Every supported device is guaranteed to run Android 13 (`API 33`) or newer (covering Pixel Tablet `tangorpro`, Pixel 9 Pro `caiman`, and modern gaming handhelds/phones).
  - Never write legacy `if (Build.VERSION.SDK_INT < 33)` fallback branches inside `com.xstreaming.nativeui.*`.
  - Unconditionally use API 31-33 native capabilities:
    - **AGSL Custom Pixel Shaders (`android.graphics.RuntimeShader`)** and `RenderEffect.createRuntimeShaderEffect()`
    - **GPU RenderNode Blur (`RenderEffect.createBlurEffect()` / `Modifier.blur()`)**
    - **LRA Hardware Haptics (`VibrationAttributes.USAGE_HARDWARE_FEEDBACK` & `VibrationEffect.Composition`)**
    - **Multi-Motor Controller Rumble (`VibratorManager` / `InputDevice.getVibratorManager()`)**
    - **Predictive Back (`OnBackInvokedDispatcher`)**
- **JDK 17 LTS + Kotlin `1.9.24` + Compose Compiler `1.5.14`**:
  - `XStreaming` uses `@react-native/gradle-plugin` (`0.72`), which pins AGP `7.4.2` on Gradle `8.0.1`.
  - Use **Kotlin `1.9.24`**, **Compose Compiler `1.5.14`**, and **Compose BOM `2024.06.00`** (`androidx.compose.material3:material3`).
  - **Critical RNGP Override Guardrail**: `@react-native/gradle-plugin`'s `JdkConfiguratorUtils.kt` silently resets `compileOptions` to `JavaVersion.VERSION_11` inside `finalizeDsl` unless disabled. Always keep `react.internal.disableJavaVersionAlignment=true` in `android/gradle.properties` and set both `compileOptions` (`JavaVersion.VERSION_17`) and `kotlinOptions.jvmTarget = "17"` in `android/app/build.gradle`.
- **Vetted First-Party Dependencies Only (`Apache-2.0`)**:
  - Use only official Google AndroidX / Jetpack (`androidx.compose.*`, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`, `androidx.navigation:navigation-compose`) and official JetBrains Kotlin (`org.jetbrains.kotlinx:kotlinx-coroutines-android`) artifacts (all `Apache-2.0`, 100% compatible with `MPL-2.0` and `AGPL-3.0`).
  - Never add unvetted 3rd-party blur, haptic, or UI wrapper libraries.
- **License Header Requirement (`AGENTS.md` Section 1)**:
  - Every standalone file under `android/app/src/main/java/com/xstreaming/nativeui/` and `android/app/src/test/java/com/xstreaming/nativeui/` must begin with:
    ```kotlin
    // SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
    ```

---

## 2. Critical Architectural Guardrails (Avoiding Known Pitfalls)

### Guardrail 1: Headless Protocol Engine + Native Compose UI Overlay (`native_android_ui`)
- Do **not** break or duplicate the TypeScript MSAL OAuth 2.0 Device Code, XSTS token exchange, RFC1918 LAN IPv4 Teredo sorting (`src/xCloud/teredoCandidates.ts`), SDP bitrate injection (`src/webrtc/index.ts`), or `<50ms` `xHome` SCTP DataChannel close detection (`src/utils/streamReconnect.ts`).
- `MainActivity` continues to host the React Native Hermes instance as a headless signaling/protocol engine when `UiEngineBootConfig.isNativeUiEnabled(context)` is `true`, while mounting the Jetpack Compose `NativeUiRoot` as the visible UI layer and communicating via `NativeUiBridgeModule`.

### Guardrail 2: Synchronous `SharedPreferences` for Cold-Start Boot Flag (`UiEngineBootConfig`)
- Never read `react-native-mmkv` directly in `MainActivity.onCreate()` before the JS bundle initializes; `MMKV` throws `IllegalStateException` if read before `MMKV.initialize()`.
- Always store the boot flag (`native_android_ui`, default `true`), `GLOBAL_FEATURE_FLAG_OVERRIDES`, two-tier Developer Mode flags (`is_developer_mode_available`, `is_developer_mode_enabled`), and fast-boot theme/haptic preferences (`theme_preset`, `primary_color`, `accent_color`, `ui_haptics_enabled`) in `SharedPreferences("xstreaming_native_prefs", Context.MODE_PRIVATE)` and sync changes bi-directionally with `settingStore`.
- Switching between **Android Native UI** and **Legacy React Native UI** requires an app restart triggered via `Intent.makeRestartActivityTask(componentName)` + `Runtime.getRuntime().exit(0)`.

### Guardrail 3: `MainActivity` Gamepad & D-Pad Event Routing
- In `MainActivity.java`, `dispatchKeyEvent()` and `onGenericMotionEvent()` intercept D-pad, thumbstick, and `A`/`B`/`X`/`Y` events when `GamepadManager.getCurrentScreen() != "stream"`.
- When `UiEngineBootConfig.isNativeUiEnabled(this)` is `true`:
  - Route menu navigation events (`up`, `down`, `left`, `right`, `lb`, `rb`, `select`, `back`, `x`, `y`), including **`KEYCODE_BUTTON_L1` (`lb`) and `KEYCODE_BUTTON_R1` (`rb`)** for 2009 NXE bumper channel switching, into `NativeUiInputBus` / Compose's `FocusManager` and `NxeHapticsEngine`.

### Guardrail 4: GPU Backdrop Blur vs Self-Blur (`RenderEffect`)
- Calling `Modifier.blur()` on a card blurs the card's **own text and icons**, not the background behind it.
- **Correct Pattern for Modal Scrims (`SessionReportDialog`, `ThemeStudioSheet`)**: Apply `Modifier.graphicsLayer { renderEffect = BlurEffect(28f, 28f, TileMode.Decal) }` to the **underlying `HomeDashboardScreen` root container** whenever a modal state is active, while rendering the modal dialog above a darkened `rgba(2, 6, 18, 0.84)` scrim with crisp, unblurred typography.
- **Correct Pattern for NXE Glass Blades (`NxeGlassCard`)**: Use an API 33 AGSL `RuntimeShader` (`NxeGlassShader.kt`) + `drawWithCache` Canvas layer behind the card content to render refracted studio lighting, diagonal `135deg` glass-pane sheen, a `1.5dp` Electric Cyan (`#00E5FF`) rim, and a `2dp` top specular light bar.

### Guardrail 5: `SurfaceViewRenderer` (`RTCFsrView`) Z-Order in Compose
- `org.webrtc.SurfaceViewRenderer` punches through the window surface. Keep it in standard hardware underlay mode (`setZOrderMediaOverlay(false)` / default) so Jetpack Compose overlays (`RingOfLightHud`, `PerformanceOverlayBar`) composite cleanly on top at 120Hz without frame copies.

### Guardrail 6: Strict UI Engine Isolation & Synchronous `@ReactMethod(isBlockingSynchronousMethod = true)` Bridge
- `NativeUiBridgeModule.isNativeUiEnabled()` MUST be annotated with `@ReactMethod(isBlockingSynchronousMethod = true): Boolean` so React Native components (`Spinner.tsx`, `Home.tsx`, `NativeStream.tsx`) can check `NativeModules.NativeUiBridge?.isNativeUiEnabled?.() === true` synchronously during render. Declaring it as an async `Promise` `@ReactMethod` returns a `Promise` object (`Promise !== true`) in JS, causing React Native `<Modal>` dialogs (`Spinner.tsx`) to render on top of the Native Compose UI.
- Never unmount or hide the Native Compose overlay for `login`, `cloud_catalog`, or `achievements_view`. When `native_android_ui` is enabled, 100% of screens (Sign-In, My Xbox, Cloud Gaming, Achievements, Settings, and Stream Overlay HUD) must render natively in Jetpack Compose, and both `SettingsScreen.kt` and `src/pages/Settings.tsx` must expose the top-level `"Android Native UI (2009 Xbox Glass)"` switch (`"Must restart app to switch..."`).

### Guardrail 7: React Native 0.72 Hermes `URLSearchParams` Stub Pitfall
- In React Native 0.72's Hermes runtime (`react-native/Libraries/Blob/URLSearchParams.js`), `URLSearchParams.prototype.get()` is unimplemented and throws `Error: URLSearchParams.get is not implemented` (even though Node.js in Jest implements full WHATWG `URLSearchParams`).
- Always use pure RFC 3986 helpers (`parseQueryParams()` and `encodeFormParams()` in `src/xal/msal.ts`) for OAuth 2.0 PKCE redirect URI parsing and form encoding, and guard with a Jest spy throwing on `URLSearchParams.prototype.get`.

### Guardrail 8: Stream Overlay HUD (`StreamOverlayHud.kt`), Touch Controller & Thumbstick Y-Axis Convention
- **Ghost-Translucent Touch Controller (`NxeTouchGamepadOverlay`)**: Idle touch controls must use `22%-26%` base opacity (scaling to `85%` only while actively pressed) so they never obstruct gameplay video.
- **Thumbstick Y-Axis Sign Convention**: Screen touch `dy` (`+down / -up`) must be passed **un-inverted** (`normY = (clampedDy / maxRadiusPx).coerceIn(-1f, 1f)`) into `NativeStream.tsx`, because `src/webrtc/channels/input.ts` already negates `LeftThumbYAxis` and `RightThumbYAxis` (`-GamepadState.LeftThumbYAxis`) when serializing Xbox input reports. Negating `dy` in `StreamOverlayHud.kt` causes double-negation (inverted Up/Down on both sticks).
- **Physical Gamepad Auto-Detection (`InputManager.InputDeviceListener`)**: `MainActivity.java` detects connected physical gamepads and updates `XStreamingStateStore.updatePhysicalControllerConnected(connected)` so the touch overlay auto-shows when no physical controller is attached and auto-hides when one connects, while remaining manual-overrideable via the 1-tap `Show/Hide Touch Controls` tile in the stream drawer.
- **Connection Card & Compact Top `(X) Guide` Handle**: Hide the top `(X) Guide` pill while `isConnecting == true` so it never overlaps the centered `NxeRingOfLightOrb` + 4-quadrant progress bar card. Once connected, default the top pill to a compact `(X) Guide` handle at `26%` opacity, expanding to live 1Hz telemetry (`Resolution • FPS • Mbps • RTT • Decode • Loss`) only when `Show/Hide Stats Bar` is toggled on.

### Guardrail 9: Authentic 2008-2009 Xbox 360 NXE Spatial Stage (Zero Web/SaaS Admin Layout)
- **No Web Top App Bar on Dashboard**: Never place an Android/Bootstrap navbar (`[MY XBOX | CLOUD | STATS | SETTINGS]`) or cyberpunk `//` section headers (`CONSOLE DECK // ...`) on `HomeDashboardScreen.kt`.
- **Receding 3D Channel Stack & Top-Right Gamercard**: Render the 4 channels as the authentic 2008 NXE receding vertical stack (`System Settings` -> `Achievements` -> `Cloud Gaming` -> bold white `My Xbox`) anchored right above the card row (collapsing to an inline floating text strip only when `screenHeightDp < 500`), paired with the rectangular Xbox Live Gamercard pod in the upper-right sky.
- **Single Horizontal Row of 4 Crisp Rectangular Slot Cards + Floor Reflections**: On widescreen `My Xbox`, render a single horizontal row of 4 crisp rectangular `3.dp`-corner cards (`Play Console`, `Cloud Gaming`, `Achievements`, `System Settings`) whose bottom edges sit pixel-flush on `screenHeight * floorHorizonFraction`, casting aligned vertical mirror reflections (`NxeCardFloorReflection`) onto the brushed studio floor below, with the `1 of 4` counter and glossy 3D `(A)` / `(Y)` / `(X)` sphere button legends at the bottom of the floor.

### Guardrail 10: Cohesive Brushed Silver-White & Theme Palette (No Rainbow Cards, No Neon Body Text)
- **Unified Card Material**: All normal dashboard and settings cards share ONE cohesive theme-lit glass interior and crisp silver-white bevel (`Color.White.copy(alpha = 0.36f)` at rest, `2.25.dp` pure white bezel when focused). Never paint sibling cards in 4 clashing rainbow border colors (green + cyan + orange + purple). Reserve the Fuchsia palette (`#D946EF` / `#F0ABFC`) strictly for Developer Mode.
- **Silver-White Secondary Text**: `tokens.textSecondary` MUST remain cool brushed silver (`Color(0xFFD6E2F0)`), never `accentSecondary` (`#00E5FF` neon cyan).
- **Custom Vector Stage Illustrations**: Every card slot must render real multi-layered Compose `Canvas` artwork (Series X monolith + 360 Ring-of-Light sphere, 360 controller, trophy cup, HD display + gear) - never plain-text letters (`[X]`, `[C]`, `[G]`, `[S]`) inside tiny boxes.

### Guardrail 11: Dynamic `FLAG_KEEP_SCREEN_ON` Lifecycle (`MainActivity.updateKeepScreenOn`)
- Never set `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` unconditionally in `MainActivity.onCreate()`.
- `XStreamingStateStore.setCurrentRouteScreen(screenName)` calls `MainActivity.updateKeepScreenOn(isStreamingNow)` (`screenName == "stream"`) so the display stays awake only during active streaming and times out/sleeps normally when idle on dashboard screens.

---

## 3. 2008-2009 Xbox 360 NXE Visual, Modal & Haptic Spec

1. **Modal Header/Footer `56dp` Geometry & No-Wrap Titles**:
   - Modal header/footer bars (`NxeModalFrame`) lock to `56.dp` height with single-line `TextOverflow.Ellipsis` titles.
2. **Lit Studio Wall & Brushed Horizon Floor (`NxeStudioBackdrop`)**:
   - On widescreen `My Xbox`, `showStudioFloorPlane = true` aligns the AGSL floor plane directly with `floorHorizonFraction` where the 4 rectangular cards rest; on scrollable sub-channels and portrait mode, `showStudioFloorPlane = false` renders a seamless lit studio backdrop.
3. **Cohesive NXE Card Palette & Fuchsia Developer Mode**:
   - Focused slot uses a crisp `2.25.dp` white outer bezel and steps taller onto the stage; resting slots use a cohesive `1.25.dp` brushed silver-white bezel and upper curved NXE glass highlight arc.
   - **Developer Mode Controls (`isDeveloperModeEnabled`)**: Standardized Fuchsia palette (`border = Color(0x4DD946EF)`, `bg = Color(0x1AD946EF)`, `text = Color(0xFFF0ABFC)`).
4. **Universal Modal Rules (Zero Redundant Close/Done Footer Buttons)**:
   - Every modal (`SessionReportDialog`, `ThemeStudioSheet`) has a minimalist `56.dp` header (title on left, `X` close button on right), backdrop tap-to-close, and `(B)` / Back dismissal.
   - Never place a redundant full-width "Close" or "Done" button in the modal footer; use the `56.dp` footer strictly for functional tools (`Don't show this again` checkbox + `Copy Telemetry` button in `SessionReportDialog`, `Reset Defaults` in `ThemeStudioSheet`, or `<ModalBreadcrumb>` cross-navigation).
5. **Typography (`NxeTypography`)**:
   - Strictly geometric humanist sans-serif (`FontFamily.SansSerif`), never `FontFamily.Serif`.
   - Enforce `fontFeatureSettings = "tnum"` (tabular numerals) on all network telemetry (`RTT`, `FPS`, `Mbps`, `Jitter`, `Packet Loss`) and Gamerscore counters so digits never jitter during live updates.
6. **LRA Tactile Haptics (`NxeHapticsEngine`)**:
   - Use API 33 `VibrationAttributes.USAGE_HARDWARE_FEEDBACK`:
     - **Blade Focus / D-Pad / Stick Step**: `VibrationEffect.Composition.PRIMITIVE_TICK` (`scale = 0.65f`)
     - **Bumper (`LB`/`RB`) & Sheet Snap**: `VibrationEffect.Composition.PRIMITIVE_LOW_TICK` (`scale = 0.8f`)
     - **Primary Confirm (`Start stream` / `(A)`)**: `VibrationEffect.Composition.PRIMITIVE_CLICK` (`scale = 0.9f`)
     - **Back / Dismiss (`(B)`)**: `VibrationEffect.Composition.PRIMITIVE_TICK` (`scale = 0.4f`)
7. **Adaptive Responsive Breakpoints**:
   - **Widescreen Landscape (Pixel Tablet `2560x1600` 16:10 & Phone Landscape)**: Full-bleed 2008 NXE horizontal 4-card stage standing on the reflective studio floor (`Play Console`, `Cloud Gaming`, `Achievements`, `System Settings`), navigable via D-pad Left/Right, `LB`/`RB` bumpers, and touch.
   - **Compact Portrait (Pixel 9 Pro Portrait)**: Receding channel stack + Gamercard header, full-width rectangular Console Hero Card, and 3 cohesive rectangular NXE slot rows.
