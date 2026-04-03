#!/usr/bin/env python3
"""
VS Code Copilot Chat Session Restoration Script
================================================

When you copy your VS Code data (%APPDATA%/Code or ~/.config/Code) to a new
machine, Copilot chat sessions may not appear because VS Code's workspace
storage hash includes the folder's creation time (birthtime). When folders are
copied, their birthtime changes, producing a different hash — so VS Code creates
a fresh, empty workspace storage instead of finding the old one with your chats.

This script:
  1. Scans all workspace storage folders for orphaned chat sessions
  2. Computes the correct (current) workspace hash for each repo folder
  3. Copies the chat session files and merges the session index into the
     correct workspace storage so VS Code can find them

Usage:
  python restore_vscode_chat_sessions.py              # preview (dry run)
  python restore_vscode_chat_sessions.py --apply      # actually restore

Requirements:
  - Python 3.6+ (uses only stdlib: sqlite3, hashlib, json, os, etc.)
  - IMPORTANT: Close VS Code before running with --apply

Supported platforms: Windows, macOS, Linux
"""

import argparse
import hashlib
import json
import math
import os
import platform
import shutil
import sqlite3
import sys
from pathlib import Path
from typing import Dict, List, Optional
from urllib.parse import unquote


# ---------------------------------------------------------------------------
# Platform-specific helpers
# ---------------------------------------------------------------------------

def get_vscode_storage_root() -> Path:
    """Return the path to VS Code's workspaceStorage directory."""
    system = platform.system()
    if system == "Windows":
        appdata = os.environ.get("APPDATA", "")
        return Path(appdata) / "Code" / "User" / "workspaceStorage"
    elif system == "Darwin":
        return Path.home() / "Library" / "Application Support" / "Code" / "User" / "workspaceStorage"
    else:  # Linux / WSL
        # Native Linux VS Code
        config = os.environ.get("XDG_CONFIG_HOME", str(Path.home() / ".config"))
        native = Path(config) / "Code" / "User" / "workspaceStorage"
        if native.exists():
            return native
        # When running inside WSL but VS Code is on the Windows host, try the
        # Windows AppData path via the /mnt/c mount point.
        win_user = os.environ.get("WINDOWS_USER") or Path.home().name
        win_path = Path(f"/mnt/c/Users/{win_user}/AppData/Roaming/Code/User/workspaceStorage")
        if win_path.exists():
            return win_path
        return native  # return default even if missing (will error with clear message)


def get_folder_birthtime_ms(folder: Path) -> int:
    """
    Get the folder's birth/creation time in milliseconds since epoch,
    matching VS Code's computation per platform.

    On Windows: Math.floor(stat.birthtimeMs)
    On macOS:   stat.birthtime (st_birthtime)
    On Linux:   stat.ino (inode number, since Linux ctime != birthtime)
    """
    stat = folder.stat()
    system = platform.system()
    if system == "Windows":
        # st_ctime on Windows is the creation time
        return math.floor(stat.st_ctime * 1000)
    elif system == "Darwin":
        # st_birthtime is available on macOS
        return math.floor(getattr(stat, "st_birthtime", stat.st_ctime) * 1000)
    else:
        # Linux: VS Code uses the inode number
        return stat.st_ino


def uri_to_fspath(uri: str) -> str:
    """
    Convert a file:// URI (as stored in workspace.json) to an OS filesystem path.
    Handles both:
      file:///path        (local paths)
      file://host/path    (remote/WSL paths, e.g. file://wsl.localhost/Ubuntu/...)
    """
    if uri.startswith("file:///"):
        # Standard local file URI
        path = unquote(uri[len("file:///"):])
        if platform.system() == "Windows":
            return path.replace("/", "\\")
        return "/" + path

    if uri.startswith("file://"):
        # Authority-bearing URI: file://hostname/path
        rest = unquote(uri[len("file://"):])
        slash_pos = rest.find("/")
        if slash_pos == -1:
            return rest
        hostname = rest[:slash_pos]
        path = rest[slash_pos:]  # leading /
        if platform.system() == "Windows":
            # Produce a UNC path: \\hostname\path
            return "\\\\" + hostname + path.replace("/", "\\")
        else:
            # On Linux/WSL: file://wsl.localhost/Ubuntu/home/foo -> /home/foo
            # Strip the WSL distro name (first path component after host)
            if "wsl" in hostname.lower():
                parts = path.lstrip("/").split("/", 1)
                if len(parts) == 2:
                    return "/" + parts[1]
            return path

    return uri


def normalize_fspath_for_grouping(fspath: str) -> str:
    """
    Normalize the fspath for grouping purposes (matching old workspaces
    that point to the same folder). Case-insensitive on Windows/macOS.
    NOT used for hash computation.
    """
    if platform.system() == "Linux":
        return fspath
    return fspath.lower()


def compute_workspace_hash(fspath: str, birthtime_ms: int) -> str:
    """
    Compute the workspace storage hash the same way VS Code does:
      MD5( fsPath + birthtimeStr )

    IMPORTANT: VS Code's getSingleFolderWorkspaceIdentifier uses
    folderUri.fsPath as-is (no lowercasing) for single-folder workspaces.

    See: https://github.com/microsoft/vscode/blob/main/src/vs/platform/workspaces/node/workspaces.ts
    """
    hash_input = fspath + str(birthtime_ms)
    return hashlib.md5(hash_input.encode("utf-8")).hexdigest()


# ---------------------------------------------------------------------------
# SQLite helpers (state.vscdb)
# ---------------------------------------------------------------------------

def read_chat_index(db_path: Path) -> Optional[dict]:
    """Read the chat session index from a state.vscdb file."""
    try:
        conn = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
        cursor = conn.execute(
            "SELECT value FROM ItemTable WHERE key = 'chat.ChatSessionStore.index'"
        )
        row = cursor.fetchone()
        conn.close()
        if row is None:
            return None
        data = json.loads(row[0])
        return data.get("entries", {})
    except Exception:
        return None


def write_chat_index(db_path: Path, entries: dict):
    """Write/update the chat session index in a state.vscdb file."""
    index_json = json.dumps({"version": 1, "entries": entries})
    conn = sqlite3.connect(str(db_path))
    conn.execute(
        "CREATE TABLE IF NOT EXISTS ItemTable "
        "(key TEXT UNIQUE ON CONFLICT REPLACE, value BLOB)"
    )
    conn.execute(
        "INSERT OR REPLACE INTO ItemTable (key, value) VALUES (?, ?)",
        ("chat.ChatSessionStore.index", index_json),
    )
    conn.commit()
    conn.close()


# ---------------------------------------------------------------------------
# Main logic
# ---------------------------------------------------------------------------

def scan_workspaces(storage_root: Path):
    """
    Scan all workspace storage folders and return a list of dicts:
      { hash, folder_uri, fspath, ws_dir, session_files: [Path, ...] }
    Only includes workspaces that have at least one chat session file.
    """
    results = []
    for entry in sorted(storage_root.iterdir()):
        if not entry.is_dir():
            continue
        ws_json = entry / "workspace.json"
        chat_dir = entry / "chatSessions"
        if not ws_json.exists() or not chat_dir.exists():
            continue

        session_files = list(chat_dir.glob("*.json")) + list(chat_dir.glob("*.jsonl"))
        if not session_files:
            continue

        try:
            with open(ws_json, "r", encoding="utf-8") as f:
                ws_data = json.load(f)
        except (json.JSONDecodeError, OSError):
            continue

        folder_uri = ws_data.get("folder", "")
        if not folder_uri.startswith("file://"):
            continue

        fspath = uri_to_fspath(folder_uri)
        results.append({
            "hash": entry.name,
            "folder_uri": folder_uri,
            "fspath": fspath,
            "ws_dir": entry,
            "session_files": session_files,
        })
    return results


def find_target_workspace(group: List[dict]) -> Optional[dict]:
    """
    Return the workspace in the group whose state.vscdb was most recently
    written — that is VS Code's currently active storage for this project.
    Falls back to the first entry with a chatSessions directory.
    """
    best = None
    best_mtime = -1.0
    for ws in group:
        db = ws["ws_dir"] / "state.vscdb"
        if db.exists():
            t = db.stat().st_mtime
            if t > best_mtime:
                best_mtime = t
                best = ws
    if best is None:
        # No state.vscdb anywhere — just pick the first
        best = group[0]
    return best


def main():
    parser = argparse.ArgumentParser(
        description="Restore orphaned VS Code Copilot chat sessions after migrating to a new machine.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Actually restore sessions. Without this flag, runs in preview/dry-run mode.",
    )
    parser.add_argument(
        "--storage-root",
        type=Path,
        default=None,
        help="Override the workspace storage root path (auto-detected by default).",
    )
    args = parser.parse_args()

    dry_run = not args.apply
    storage_root = args.storage_root or get_vscode_storage_root()

    print(f"Workspace storage root: {storage_root}")
    print()

    if not storage_root.exists():
        print(f"ERROR: Storage root does not exist: {storage_root}")
        sys.exit(1)

    # Phase 1: Scan
    workspaces = scan_workspaces(storage_root)
    print(f"Found {len(workspaces)} workspace storage folder(s) with chat sessions:\n")
    for ws in workspaces:
        exists = os.path.isdir(ws["fspath"])
        status = "EXISTS" if exists else "MISSING"
        print(f"  [{status:7s}] {ws['hash']}  ({len(ws['session_files'])} sessions)")
        print(f"            {ws['fspath']}")
    print()

    # Phase 2: Group by folder path (case-insensitive on Windows/macOS)
    groups = {}  # type: Dict[str, List]
    for ws in workspaces:
        key = normalize_fspath_for_grouping(ws["fspath"])
        groups.setdefault(key, []).append(ws)

    # Separate existing vs missing
    existing_groups = {}
    missing_sessions_total = 0
    for key, group in groups.items():
        fspath = group[0]["fspath"]
        if os.path.isdir(fspath):
            existing_groups[key] = group
        else:
            total = sum(len(ws["session_files"]) for ws in group)
            missing_sessions_total += total

    if missing_sessions_total > 0:
        print(f"NOTE: {missing_sessions_total} session(s) belong to folders not found on disk:")
        for key, group in groups.items():
            fspath = group[0]["fspath"]
            if not os.path.isdir(fspath):
                total = sum(len(ws["session_files"]) for ws in group)
                print(f"  - {fspath}  ({total} session(s))")
        print("  These cannot be restored until the folders exist at the original paths.")
        print()

    restorable = len(existing_groups)
    print(f"Repos on disk with sessions to process: {restorable}")
    print()

    if dry_run:
        print("=" * 60)
        print("  DRY RUN — no changes will be made.")
        print("  Re-run with --apply to execute the restoration.")
        print("=" * 60)
        print()

    total_restored = 0
    total_skipped = 0

    for key, group in sorted(existing_groups.items()):
        fspath = group[0]["fspath"]
        print(f"{'─' * 60}")
        print(f"Repo: {fspath}")

        # Identify the current (most-recently-used) workspace as the merge target.
        # Using mtime of state.vscdb avoids the need to recompute VS Code's hash
        # (which is not portable for WSL/remote paths).
        target_ws = find_target_workspace(group)
        current_hash = target_ws["hash"]
        target_dir = target_ws["ws_dir"]
        target_chat_dir = target_dir / "chatSessions"
        target_state_db = target_dir / "state.vscdb"

        print(f"  Target workspace (most recently used): {current_hash}")
        target_chat_dir.mkdir(exist_ok=True)

        # Collect sessions from all other workspaces not yet present in target
        sessions_to_restore = {}  # type: Dict[str, Path]  # session_id -> source_path
        for ws in group:
            if ws["hash"] == current_hash:
                continue
            for sf in ws["session_files"]:
                sid = sf.stem
                dst_file = target_chat_dir / sf.name
                if sid not in sessions_to_restore and not dst_file.exists():
                    sessions_to_restore[sid] = sf

        other_hashes = [ws["hash"] for ws in group if ws["hash"] != current_hash]

        if not sessions_to_restore:
            print(f"  No new sessions to restore — already up to date.")
            print()
            continue

        print(f"  Other workspace(s): {', '.join(other_hashes)}")
        print(f"  Sessions to restore: {len(sessions_to_restore)}")

        if dry_run:
            print(f"  [DRY RUN] Would copy {len(sessions_to_restore)} session(s) -> {current_hash}")
            print()
            continue

        # Copy session files
        copied = 0
        skipped = 0
        for sid, src_file in sessions_to_restore.items():
            dst_file = target_chat_dir / src_file.name
            if not dst_file.exists():
                shutil.copy2(src_file, dst_file)
                copied += 1
            else:
                skipped += 1

        print(f"  Copied {copied} session file(s), skipped {skipped} (already present)")
        total_restored += copied
        total_skipped += skipped

        # Merge chat session index in state.vscdb
        merged_entries = read_chat_index(target_state_db) or {}

        for ws in group:
            if ws["hash"] == current_hash:
                continue
            old_db = ws["ws_dir"] / "state.vscdb"
            if not old_db.exists():
                continue
            old_entries = read_chat_index(old_db) or {}
            for sid, entry in old_entries.items():
                if sid not in merged_entries:
                    merged_entries[sid] = entry

        write_chat_index(target_state_db, merged_entries)
        print(f"  Updated session index ({len(merged_entries)} total entries)")
        print()

    print(f"{'─' * 60}")
    print()
    if dry_run:
        print("Dry run complete — no files were modified.")
        print("Run again with --apply to execute the restoration.")
    else:
        print(f"Restoration complete!")
        print(f"  Sessions restored:  {total_restored}")
        print(f"  Sessions skipped:   {total_skipped} (already present)")
        print()
        print("Next steps:")
        print("  1. Open VS Code")
        print("  2. Open a repo folder")
        print("  3. Your chat history should appear in the Chat panel")


if __name__ == "__main__":
    main()
