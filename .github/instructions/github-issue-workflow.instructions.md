---
description: "Use when given a GitHub issue to work on. Covers improving issue titles and descriptions, implementing changes, creating branches and PRs, and deploying to Fly.io after each code change."
---
# GitHub Issue Workflow

## When Given a GitHub Issue

### 1. Improve the Issue

Before starting implementation, update the issue on GitHub:
- Rewrite the title to be clear and action-oriented
- Improve the description: add reproduction steps, acceptance criteria, and technical context
- Use `mcp_github_issue_write` to update the issue

### 2. Create a Branch

Create a branch named `feat/<topic>-issue-<number>` (e.g., `feat/bot-overlays-issue-47`):
- Use `mcp_github_create_branch` or run `git checkout -b feat/<topic>-issue-<number>` in the WSL terminal

### 3. Implement the Change

Make all code changes using VS Code file editing tools (not terminal text manipulation like `sed`/`awk`):
- Use `read_file`, `replace_string_in_file`, `multi_replace_string_in_file`, `create_file`
- Read files before modifying them
- Understand existing code before suggesting changes

### 4. Deploy After Every Code Change

After **each** change to the code, deploy to Fly.io immediately:

```bash
wsl git -C /home/per-ahrens/source/repos/baisch add -A
wsl git -C /home/per-ahrens/source/repos/baisch commit -m "<type>: <description>"
wsl git -C /home/per-ahrens/source/repos/baisch push origin <branch-name>
wsl bash -c "cd /home/per-ahrens/source/repos/baisch && ~/.fly/bin/fly deploy"
```

Wait for deploy to complete and confirm both machines reach "good state" before proceeding.

### 5. Create a Pull Request

Once the feature is complete and deployed:
- Use `mcp_github_create_pull_request` to open a PR against the main branch
- Reference the issue in the PR body (e.g., "Closes #47")
- Title should match the improved issue title
- Only add changes related to the issue in the PR

## Key Constraints

- **Always use WSL commands** for git and deployment (prefix with `wsl git ...` or `wsl bash -c "..."`)
- **Never use destructive git commands** (no `--force`, no `reset --hard`) without confirmation
- **Deploy app name**: `baisch-game` (from `fly.toml` in repo root)
- **Fly CLI path**: `~/.fly/bin/fly` (within WSL)
- **Repository path in WSL**: `/home/per-ahrens/source/repos/baisch`

## Target Environments — Test Every Change Against All of Them

This codebase runs on multiple platforms. Any change to core game logic, UI layout, input
handling, or audio **must be verified not to break any of these environments**:

| Environment | Entry point | Notes |
|---|---|---|
| **Web — mobile phone (Chrome/Safari)** | `html/webapp/mobile.html` | Touch forwarding, `width=device-width` viewport, letterbox |
| **Web — desktop browser** | `html/webapp/index.html` | Mouse input, wide/landscape screens, letterbox |
| **Android app** | `android/` | `AndroidLauncher`, `AndroidPlayerStorage`, `AndroidManifest.xml` |
| **Future: iOS app** | `ios/` (stubbed) | Will use same core; avoid platform assumptions |
| **Desktop (dev/testing)** | `desktop/` | Uses `PlayerStorage.NOOP` — no persistence |

### Platform-Specific Anti-Patterns to Avoid

- **Never use Java `==`/`!=` for String comparison** — strings from JSON parsing are non-interned on Android and will not compare equal by reference to literals.
- **Never use `PlayerStorage.NOOP` in production launchers** — it silently discards all preferences. Create a platform implementation (`AndroidPlayerStorage`, `BrowserPlayerStorage`).
- **Never hard-code physical pixel sizes** — use `MyGdxGame.WIDTH` / `MyGdxGame.HEIGHT` (logical coordinates) everywhere. `Gdx.graphics.getWidth/Height()` returns physical pixels.
- **Never assume a fixed viewport width in HTML** — tablets and desktops have varying viewport sizes. The Java letterbox (`FitViewport` + `GameScreen` scale/offset) handles aspect ratio; the HTML only needs to give the canvas the actual viewport dimensions.
- **`Gdx.net.openURI()` on Android 11+** requires a `<queries>` block in `AndroidManifest.xml` for the target URL scheme (`https`), or the call is silently ignored.
- **Browser autoplay guards** (e.g., `musicStarted` flag) must not be applied to Android — they prevent the music toggle from working. Keep audio unlock logic inside `HtmlLauncher` / `BrowserPlayerStorage` only.
