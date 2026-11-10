---
name: engineering-design-and-test-standards
description: Core engineering architecture, 3rd-party dependency vetting and licensing, UI/modal design invariants, developer mode, feature flagging, automated testing, and git workflow standards aligned with the Lab 151 ecosystem.
---

# Engineering, Design, Dependency & Testing Standards (`engineering-design-and-test-standards`)

This skill defines the cross-cutting engineering, architectural, UI/UX, dependency licensing, testing, and git workflow principles for `XStreaming`, harmonized with the sibling `lab151` and `lab151-sleep` repositories.

---

## 1. Architectural Principles & Reactive State Mental Models

1. **Deep Mechanical Rationale**:
   - Whenever designing or refactoring components, document and reason about the concrete mechanics: render/composition lifecycles, recomposition scopes, thread confinement, memory ownership, and hardware pipeline boundaries.
2. **Unidirectional Reactive State (`StateFlow` / Observable Stores)**:
   - **Android Native UI (`com.xstreaming.nativeui.*`)**: ViewModels expose immutable point-in-time state snapshots via Kotlin `StateFlow<UiState>`. Composables observe state via `collectAsStateWithLifecycle()` and emit user intent upward via explicit lambda callbacks or sealed `UiEvent` actions.
   - **Decoupled Persistence (Zero UI-Effect Storage Writes)**: Never trigger `SharedPreferences` or MMKV disk writes inside Compose `LaunchedEffect` or React `useEffect` render loops. Persistence side-effects belong strictly inside Repository / Store action methods (`UiEngineBootConfig`, `NxeThemeRepository`, `settingStore`).
3. **Anti-Monolith & Single-Responsibility Files**:
   - Keep screen-level files focused on layout orchestration (`< 250` lines per file). Extract reusable cards, shaders, modals, and state reducers into dedicated, independently testable modules.
4. **Defensive Render & Schema Migration Guards**:
   - Anticipate nullish, missing, or legacy preference values on cold start.
   - All persisted settings and profile payloads must pass through deterministic, idempotent schema normalization (`NxeThemePresets.resolve()`, `UiEngineBootConfig.readConfig()`) with canonical defaults before UI mounting.
5. **Draft State Buffering in Editable Dialogs**:
   - Complex configuration dialogs that edit multiple interdependent fields must buffer edits in local draft state and track `isDirty`. If `isDirty` is true, intercept backdrop tap, hardware Back, or `X` close to confirm discarding unsaved changes.

---

## 2. Vetted 3rd-Party Dependencies & License Governance

1. **Strict License Compatibility with GNU AGPL-3.0 & MPL-2.0**:
   - The combined `XStreaming` work is licensed under **GNU AGPL-3.0** (`LICENSE`), and standalone original files in this fork are dual-licensed under **`MPL-2.0 OR AGPL-3.0-or-later`** (`NOTICE.md`).
   - Every 3rd-party Gradle (`android/app/build.gradle`) or NPM (`package.json`) dependency MUST be vetted for license compatibility before addition:
     - **Approved Licenses**: `Apache-2.0`, `MIT`, `BSD-2-Clause`, `BSD-3-Clause`, `ISC`, `MPL-2.0`, `EPL-2.0`, `LGPL-2.1-or-later`, `AGPL-3.0-or-later`.
     - **Strictly Prohibited**: Proprietary/closed-source binary SDKs without source, `GPL-2.0-only` (incompatible with AGPL-3.0), or unlicensed GitHub tarballs.
2. **First-Party Jetpack & Kotlin Standard Library Preference**:
   - Prefer official Google Jetpack / AndroidX (`androidx.compose.*`, `androidx.activity.*`, `androidx.lifecycle.*`, `androidx.navigation.*`, `androidx.window.*` - all `Apache-2.0`) and official JetBrains Kotlin (`org.jetbrains.kotlin.*`, `org.jetbrains.kotlinx:kotlinx-coroutines-android` - `Apache-2.0`) libraries.
   - **Zero Redundant UI Wrapper Libraries**: Never add 3rd-party blur libraries (e.g., `Blurry`, `Haze`), 3rd-party haptic wrappers, or unmaintained theme libraries when Android API 33 (`android.graphics.RuntimeShader`, `android.graphics.RenderEffect`, `android.os.VibrationEffect.Composition`) and Jetpack Material 3 provide first-party hardware-accelerated implementations.
3. **Mandatory SPDX License Headers on Standalone Files**:
   - Every newly authored Kotlin, Java, or TypeScript file created for this fork must begin with:
     ```kotlin
     // SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
     ```

---

## 3. UI Design Tokens, Toolbars & Universal Modal Invariants

### A. Semantic Design Tokens Over Hardcoded Colors
- All components must consume semantic tokens from `LocalNxeGlassTokens` (`bgApp`, `bgSurface`, `bgSurfaceElevated`, `bgInput`, `textPrimary`, `textSecondary`, `textMuted`, `borderSubtle`, `borderStrong`, `borderAccent`, `accentPrimary`, `accentSecondary`).
- **Permitted Semantic Color Exceptions**:
  1. **Channel Domain Accents**: Console Hero Emerald (`#16A34A` / `#4ADE80`), Cloud Gaming Cyan (`#00E5FF`), Achievements Trophy Gold (`#F59E0B`), and Settings Luminous Amethyst (`#A78BFA`).
  2. **Telemetry & Power Status**: Online/Excellent Emerald (`#22C55E`), Standby/Moderate Amber (`#F59E0B`), Error/Critical Red (`#EF4444`).
  3. **Developer Mode Controls (Standardized Fuchsia Palette)**: All developer-gated badges, toggles, and diagnostic tools MUST use the canonical Lab 151 Fuchsia palette:
     - Background: `Color(0x1AD946EF)` (`fuchsia-500` at 10% alpha) or `Color(0x4D4A044E)` (`fuchsia-950` at 30% alpha)
     - Border: `Color(0x4DD946EF)` (`fuchsia-500` at 30% alpha)
     - Text / Icon: `Color(0xFFF0ABFC)` (`fuchsia-300`) / `Color(0xFFE879F9)` (`fuchsia-400`)
     - Active / Primary Button: `Color(0xFFC026D3)` (`fuchsia-600`) with white text.

### B. Unified Frame Toolbar Geometry (`56dp`) & No-Wrap Headers
- **Unified Height (`56dp`)**: Top application headers (`NxeTopBar`), bottom navigation docks, and modal footer toolbars must lock to a `56.dp` height.
- **Strict No-Wrap Title Policy**: View and modal titles must use `maxLines = 1` and `overflow = TextOverflow.Ellipsis` inside a weighted container (`Modifier.weight(1f)`) so titles never wrap onto a second line or collide with trailing action buttons.

### C. Universal Modal & Dialog Invariants
Every modal dialog (`SessionReportDialog`, `ThemeStudioSheet`, `ConfirmationDialog`, `AboutDialog`) MUST satisfy all 6 invariants:
1. **Click-Outside-To-Close Scrim**: Tapping the outer darkened backdrop (`rgba(2, 6, 18, 0.84)`) dismisses the modal.
2. **Event Propagation Isolation**: Tapping inside the modal card surface never bubbles up to trigger backdrop dismissal.
3. **Hardware Back / Escape / Controller `(B)` Dismissal**: Wired to Android's `BackHandler` / `OnBackInvokedDispatcher` and controller `KEYCODE_BUTTON_B`.
4. **Root Window / Portal Mounting with Background RenderNode Blur**: Modals mount at the root window level while applying `RenderEffect.createBlurEffect(28f, 28f, TileMode.Decal)` to the underlying dashboard container (never self-blurring the modal's own text).
5. **Minimalist Clean Headers**: Leading edge shows only the concise Title (plus optional semantic icon badge); trailing edge shows the `X` close icon button. No decorative fluff paragraphs in headers.
6. **Strict Ban on Redundant Footer "Close" / "Done" Buttons**:
   - Because every modal already provides a top-right `X` close button, backdrop tap-to-close, and `(B)` / Back gesture dismissal, **never place a redundant full-width "Close" or "Done" button in the modal footer**.
   - Modal footers (`56.dp` height) are reserved strictly for meaningful workflow actions (e.g., `Don't show this again` checkbox + `Copy Telemetry` action in `SessionReportDialog`, `Reset Defaults` in `ThemeStudioSheet`, or `Save` / `Restart App` in confirmation dialogs) or symmetrical `<ModalBreadcrumb>` cross-navigation (e.g., `About` <-> `Release Notes`).
7. **Strict Ban on Native OS Alert Dialogs**: Never use raw `android.app.AlertDialog` or JS `Alert.alert()`. Always render custom themed Compose Material 3 dialogs matching `LocalNxeGlassTokens`.

---

## 4. Two-Tier Developer Mode & Feature Flagging

1. **Two-Tier Developer Mode (`isDeveloperModeAvailable` vs `isDeveloperModeEnabled`)**:
   - **`isDeveloperModeAvailable`**: True in `dev` builds or unlocked by tapping the app version badge 7 times in Settings / About. Reveals the **Developer Options** blade in Settings.
   - **`isDeveloperModeEnabled`**: The master runtime toggle inside Developer Options. All developer-gated UI badges, diagnostic overlays, and experimental toggles must check `isDeveloperModeEnabled` and render using the Fuchsia developer palette.
2. **Feature Flagging (`native_android_ui`)**:
   - Feature flag keys use `snake_case` (`native_android_ui`) and map 1:1 to Esedre ticket `featureFlag` metadata.
   - Support `GLOBAL_FEATURE_FLAG_OVERRIDES` in `UiEngineBootConfig.kt` so a flag can be centrally graduated to `true` (default ON) while keeping a developer override toggle in Settings (`Switch to Legacy React Native UI (Requires Restart)`).
   - Never delete a feature flag without explicit user instruction.

---

## 5. Automated Testing & Verification Standards

1. **Mandatory Dual-Layer Unit Testing**:
   - **TypeScript / Jest (`__tests__/*.test.ts`)**: Run via `corepack yarn test --watchAll=false`. Covers protocol helpers (`teredoCandidates`, `webrtcBitrate`, `msalAuthentication`, `streamReconnect`, `connectionStatus`, `themePresets`).
   - **Kotlin / JUnit 4 (`android/app/src/test/java/com/xstreaming/nativeui/**/*Test.kt`)**: Run via `.\gradlew.bat testProdReleaseUnitTest`. Covers `NxeThemePresets`, `UiEngineBootConfig`, `XStreamingViewModel` state transitions, and schema migrations.
2. **Zero Weakened Assertions & Zero Build Warnings**:
   - Never weaken or delete test assertions to make a failing test pass; fix the underlying implementation.
   - Every change must pass both `corepack yarn test --watchAll=false` and `.\gradlew.bat testProdReleaseUnitTest` with `0` failures before building `assembleProdRelease`.

---

## 6. Git Workflow, Commit Naming & Copywriting Invariants

1. **Strict Turn-by-Turn Commit & Push Authorization**:
   - NEVER run `git add`, `git commit`, or `git push` unless the user explicitly wrote `"commit"` or `"push"` in their **most recent prompt** (zero carry-over permission across turns).
2. **Branch Hierarchy**:
   - Create isolated ticket branches (`ticket/Xbox-<id>-<slug>`) off `release/upcoming` (or `upstream-pr/<topic>` off `upstream/main` when extracting upstream PRs).
3. **Clean Commit Format (`[#Xbox-<id>] <Imperative Action Summary>`)**:
   - **Strict Ban on Conventional Prefixes**: Never use `feat:`, `fix:`, `chore:`, `refactor:`, or `style:` in commit messages or PR titles.
   - Use `[#Xbox-<id>] <Imperative Action Summary>` (e.g., `[#Xbox-13] Add Compose Material 3 NXE glass theme and LRA haptics engine`) or `[Docs]` / `[Infra]` / `[Release] <Summary>`.
4. **Zero Em Dashes or En Dashes Anywhere**:
   - Never use em dashes (`\u2014`) or en dashes (`\u2013`) in code, UI strings, comments, commit messages, ticket specs, or markdown documentation. Use hyphens (`-`), colons (`:`), or parentheses instead.
5. **Natural Gamer Copywriting & Ban on Pretentious Buzzwords**:
   - Write UI copy and release notes in direct, concrete gamer-facing language.
   - Never use decorative buzzwords like `"authentic"`, `"premium"`, `"elegant"`, `"high-fidelity"`, or `"state-of-the-art"`.
