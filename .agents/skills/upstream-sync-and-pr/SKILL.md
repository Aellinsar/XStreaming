---
name: upstream-sync-and-pr
description: Sync upstream changes from Geocld/XStreaming into Aellinsar/XStreaming and extract clean, isolated single-topic branches (upstream-pr/<topic>) for contributing bugfixes and improvements back to upstream/main under AGPL-3.0.
---

# Upstream Sync & Isolated PR Extraction Skill (`upstream-sync-and-pr`)

Use this skill whenever:
1. Pulling or rebasing new changes from `upstream/main` (`Geocld/XStreaming`) into our fork (`origin/main` / `Aellinsar/XStreaming`).
2. Preparing a clean, isolated Pull Request branch (`upstream-pr/<topic>`) to propose a bugfix or UX improvement back to `Geocld/XStreaming` without including fork-specific defaults or unrelated modifications.
3. Auditing changes for **GNU AGPL-3.0** license compliance, test coverage, and privacy guardrails.

---

## 1. Verifying Remote Configuration

Always run commands from the repository root (`XStreaming`):

```powershell
git remote -v
# Expected:
# origin    https://github.com/Aellinsar/XStreaming.git (fetch/push)
# upstream  https://github.com/Geocld/XStreaming.git (fetch/push)
```

Fetch the latest upstream state before any sync or PR extraction:

```powershell
git fetch upstream --prune
git fetch origin --prune
```

---

## 2. Organizing Commits on `main` (Atomic & Tiered)

To keep `origin/main` easy to maintain and cherry-pick from, structure commits atomically by topic and tier:

- **Tier 1 Commits (Upstreamable Bugfixes & UX Improvements):**
  - `fix(auth): cache MSAL streaming/web tokens and support cancelling device code polling`
  - `fix(auth): rebind HomeScreen auth callbacks on remount and streamline MsalAuth UI`
  - `fix(webrtc): implement setVideoBitrate/setAudioBitrate SDP b=AS injection in native engine`
  - `fix(ice): prioritize RFC1918 private LAN IPv4 candidates when IPv6 is disabled`
  - `fix(stream): exit console stream cleanly on remote close or failure without waking console`
  - `fix(settings): sync native low-latency decoder and stereo audio settings on cold start`
  - `fix(patches): correct react-native-paper patch hunk header and enforce LF eol`
- **Tier 2 Commits (Fork-Specific Defaults & Build Ergonomics):**
  - `feat(auth): make MSAL OAuth device code the sole login path and remove legacy WebView login`
  - `chore(settings): tune default resolution, FSR, low-latency decoder, and power-on settings`
  - `build(android): support Windows NDK host toolchain, optional nano-rs skip, and debug signing fallback`
  - `docs(agents): add AGENTS.md, NOTICE.md, and .agents/skills workflows`

---

## 3. Integrating New Upstream Changes (`upstream/main` → `main`)

When `Geocld/XStreaming` pushes new commits to `upstream/main`:

```powershell
# 1. Ensure working tree is clean
git status

# 2. Fetch latest upstream
git fetch upstream

# 3. Inspect incoming upstream commits
git log --oneline HEAD..upstream/main

# 4. Rebase or merge upstream/main into main
# Prefer rebase if local commits haven't been shared, or merge --no-ff to preserve public history:
git merge upstream/main --no-edit
```

### Conflict Resolution Guidelines
- **`src/pages/Home.tsx` / `src/App.tsx`**: Preserve our MSAL-primary flow and callback rebinding (`_authentication.current._authenticationCompleted = authenticationCompleted`) while adopting any new upstream console/cloud state handling.
- **`src/store/settingStore.ts`**: Adopt any new setting keys added by upstream in both the `Settings` type and `defaultSettings`, while preserving our `syncNativeSettings(mergedSettings)` call inside `getSettings()` and our fork's preferred defaults.
- **Post-Merge Verification**:
  ```powershell
  corepack yarn test --watchAll=false
  ```

---

## 4. Extracting an Isolated PR Branch for Upstream (`upstream-pr/<topic>` or `fix/<topic>`)

When proposing a Tier 1 fix back to `Geocld/XStreaming`, **never** open a PR from our fork's `main` branch (which contains Tier 2 fork defaults and agent configs). Instead, branch directly off `upstream/main`.

If you have uncommitted work on `main`, use a temporary `git worktree` so `main` is never disturbed:

```powershell
# 1. Create an isolated worktree branched from upstream/main
$wt = Join-Path $env:TEMP "xstreaming-pr-wt"
git fetch upstream
git worktree add -B fix/<topic-name> $wt upstream/main

# 2. Apply or cherry-pick ONLY the relevant Tier 1 changes & unit tests inside $wt
Set-Location $wt
corepack yarn test --watchAll=false
git add <files>
git commit -m "fix(<scope>): <concise description>"
git push -u origin fix/<topic-name>

# 3. Clean up the temporary worktree
Set-Location "<repo-root>"
git worktree remove --force $wt
```

### Important Adjustment When Proposing Auth Improvements Upstream
If proposing our MSAL improvements (`src/MsalAuthentication.ts`, `src/xal/msal.ts`, `src/components/MsalAuth.tsx`, and the stale-closure fix in `src/pages/Home.tsx`) to `upstream/main`:
- Keep `src/Authentication.ts` and `src/pages/Login.tsx` intact on the `upstream-pr/*` branch unless upstream explicitly wants to deprecate XAL WebView login, so the PR is strictly additive and non-breaking for upstream maintainers.

### Pre-PR Checklist
Before pushing `upstream-pr/<topic-name>` to `origin`:
1. **Diff Inspection:**
   ```powershell
   git diff upstream/main...HEAD
   ```
   Verify that ONLY files relevant to `<topic-name>` are touched, with zero unrelated formatting changes and zero fork-only files (`AGENTS.md`, `.agents/`).
2. **Unit Tests:**
   ```powershell
   corepack yarn test --watchAll=false
   ```
3. **Privacy & License Audit:**
   - Confirm no personal IPs, hostnames, device serials, or credentials exist in the diff.
   - Confirm [`LICENSE`](../../../LICENSE) (AGPL-3.0) compliance.
4. **Push Branch to Origin for PR Creation:**
   ```powershell
   git push -u origin upstream-pr/<topic-name>
   ```
