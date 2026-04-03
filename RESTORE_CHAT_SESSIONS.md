# Restoring VS Code Copilot Chat Sessions

When VS Code workspace storage hashes get out of sync (e.g. after copying files to a new machine), chat sessions may disappear. The script `restore_vscode_chat_sessions.py` consolidates orphaned sessions back into the workspace VS Code is currently using.

## Background

VS Code stores chat sessions in `%APPDATA%\Code\User\workspaceStorage\<hash>\chatSessions\` on Windows. For WSL-remote workspaces (opened via Remote - WSL), the sessions are still stored on the Windows side — the `workspace.json` just uses a `file://wsl.localhost/Ubuntu/...` URI. There is no separate `.vscode-server` storage for chat sessions.

Multiple hash folders can accumulate for the same repo when birthtimes change (e.g. after copy/restore). The script detects this and merges them.

## Steps

### 1. Run a dry-run preview (no changes made)

From inside WSL:

```bash
cd ~/source/repos/baisch
python3 restore_vscode_chat_sessions.py
```

This will list all workspace storage folders with chat sessions, resolve which ones map to the same repo, and show what would be merged — without touching anything.

### 2. Close VS Code

**Important:** VS Code must be closed before applying changes, otherwise it may overwrite the merged session index.

### 3. Apply the restoration

```bash
python3 restore_vscode_chat_sessions.py --apply
```

This will:
- Copy session `.json`/`.jsonl` files from orphaned workspace hashes into the most-recently-used hash folder
- Merge the `chat.ChatSessionStore.index` entries in `state.vscdb` so VS Code can find the sessions

### 4. Reopen VS Code

Reopen the workspace — the previously missing chat sessions should now appear in the Copilot Chat history.

## Options

| Flag | Description |
|------|-------------|
| *(none)* | Dry-run: preview changes without modifying anything |
| `--apply` | Execute the restoration |
| `--storage-root <path>` | Override the auto-detected workspace storage root |

## Storage location (WSL)

The script auto-detects the storage root. When run from inside WSL it resolves to:

```
/mnt/c/Users/<username>/AppData/Roaming/Code/User/workspaceStorage/
```

which is the same as `%APPDATA%\Code\User\workspaceStorage\` on the Windows host.
