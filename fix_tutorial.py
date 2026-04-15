#!/usr/bin/env python3
"""Fix tutorial starting cards: 6 -> 8 so players get hand cards after doSetup()."""
import pathlib, sys

f = pathlib.Path(__file__).resolve().parent / "server" / "index.js"
src = f.read_text()

old = "sess.gameState = new GameState(sess.users, { startingCards: 6 });"
new = "sess.gameState = new GameState(sess.users, { startingCards: 8 });"

if old not in src:
    print("ERROR: old string not found in server/index.js")
    sys.exit(1)

count = src.count(old)
if count != 1:
    print(f"ERROR: found {count} occurrences, expected 1")
    sys.exit(1)

src = src.replace(old, new)
f.write_text(src)
print("OK: startingCards changed from 6 to 8 in createTutorial handler")
