#!/usr/bin/env python3
"""Patch server/index.js to add configurable bot players with AI (issue #47)."""
import pathlib, sys

f = pathlib.Path("/home/per-ahrens/source/repos/baisch/server/index.js")
src = f.read_text()

# Detect line ending
NL = "\r\n" if "\r\n" in src else "\n"

def nl(s):
    """Convert \n to the file's native line ending."""
    return s.replace("\n", NL) if NL != "\n" else s

# ─────────────────────────────────────────────────────────────────────────────
# 1. Replace autoFinishBotTurnIfNeeded with full bot AI
# ─────────────────────────────────────────────────────────────────────────────

old_auto = nl(
    "function autoFinishBotTurnIfNeeded(sess) {\n"
    "  if (!sess || !sess.gameState || !sess.isTutorial) return;\n"
    "  var currentIdx = sess.gameState.currentPlayerIndex;\n"
    "  var currentPlayer = sess.gameState.players[currentIdx];\n"
    "  if (currentPlayer && currentPlayer.id.indexOf('bot_') === 0) {\n"
    "    setTimeout(function() {\n"
    "      if (!sess.gameState) return;\n"
    "      sess.gameState.finishTurn();\n"
    "      io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());\n"
    "      autoFinishBotTurnIfNeeded(sess);\n"
    "    }, 1500);\n"
    "  }\n"
    "}\n"
)

new_bot_ai = nl(
    "// ── Bot AI ─────────────────────────────────────────────────────────────────\n"
    "\n"
    "function isBot(player) {\n"
    "  return player && player.id.indexOf('bot_') === 0;\n"
    "}\n"
    "\n"
    "function playBotTurnIfNeeded(sess) {\n"
    "  if (!sess || !sess.gameState) return;\n"
    "  var gs = sess.gameState;\n"
    "  var player = gs.players[gs.currentPlayerIndex];\n"
    "  if (!isBot(player) || player.isOut) return;\n"
    "  setTimeout(function() {\n"
    "    if (!sess.gameState) return;\n"
    "    executeBotTurn(sess);\n"
    "  }, 1500);\n"
    "}\n"
    "\n"
    "function executeBotTurn(sess) {\n"
    "  var gs = sess.gameState;\n"
    "  var idx = gs.currentPlayerIndex;\n"
    "  var p = gs.players[idx];\n"
    "  if (!p || p.isOut) return;\n"
    "\n"
    "  // 1. Fill empty defense slots (keep at least 1 card in hand)\n"
    "  botFillDefense(gs, idx);\n"
    "\n"
    "  // 2. Try to attack\n"
    "  if (p.hand.length > 0) {\n"
    "    if (!botTryKingAttack(gs, idx)) {\n"
    "      if (!botTryDefAttack(gs, idx)) {\n"
    "        botTryPlunder(gs, idx);\n"
    "      }\n"
    "    }\n"
    "  }\n"
    "\n"
    "  // 3. Finish turn\n"
    "  gs.finishTurn();\n"
    "  io.to(sess.id).emit('stateUpdate', gs.serialize());\n"
    "  checkAndHandleWinner(sess);\n"
    "\n"
    "  // Next player might be a bot too\n"
    "  playBotTurnIfNeeded(sess);\n"
    "}\n"
    "\n"
    "function botFillDefense(gs, playerIdx) {\n"
    "  var p = gs.players[playerIdx];\n"
    "  for (var slot = 1; slot <= 3; slot++) {\n"
    "    if (p.defCards[slot] == null && p.hand.length > 1) {\n"
    "      var cardId = botWeakestCard(gs, p.hand);\n"
    "      if (cardId !== null) gs.putDefCard(playerIdx, slot, cardId);\n"
    "    }\n"
    "  }\n"
    "}\n"
    "\n"
    "function botTryKingAttack(gs, attackerIdx) {\n"
    "  var attacker = gs.players[attackerIdx];\n"
    "  for (var di = 0; di < gs.players.length; di++) {\n"
    "    if (di === attackerIdx) continue;\n"
    "    var defender = gs.players[di];\n"
    "    if (defender.isOut) continue;\n"
    "    // Check if all 3 defense slots are empty (king is exposed)\n"
    "    var allEmpty = true;\n"
    "    for (var s = 1; s <= 3; s++) {\n"
    "      if (defender.defCards[s] != null || defender.topDefCards[s] != null) { allEmpty = false; break; }\n"
    "    }\n"
    "    if (!allEmpty) continue;\n"
    "    // Attack with strongest card\n"
    "    var atkCard = botStrongestCard(gs, attacker.hand);\n"
    "    if (atkCard === null) continue;\n"
    "    var atkStr = gs.cardStrength(atkCard);\n"
    "    var defStr = gs.cardStrength(defender.kingCard);\n"
    "    var success = (atkStr >= defStr);\n"
    "    gs.kingAttackResolved(attackerIdx, di, success, [atkCard], false);\n"
    "    return true;\n"
    "  }\n"
    "  return false;\n"
    "}\n"
    "\n"
    "function botTryDefAttack(gs, attackerIdx) {\n"
    "  var attacker = gs.players[attackerIdx];\n"
    "  if (attacker.hand.length < 1) return false;\n"
    "  for (var di = 0; di < gs.players.length; di++) {\n"
    "    if (di === attackerIdx) continue;\n"
    "    var defender = gs.players[di];\n"
    "    if (defender.isOut) continue;\n"
    "    for (var slot = 1; slot <= 3; slot++) {\n"
    "      var defCardId = defender.defCards[slot];\n"
    "      if (defCardId == null) continue;\n"
    "      var defStr = gs.cardStrength(defCardId);\n"
    "      // Find a hand card strong enough to beat it\n"
    "      var atkCard = botStrongestCard(gs, attacker.hand);\n"
    "      if (atkCard === null) continue;\n"
    "      var atkStr = gs.cardStrength(atkCard);\n"
    "      if (atkStr >= defStr) {\n"
    "        gs.defAttackResolved(attackerIdx, di, slot, 0, true, [atkCard], false, []);\n"
    "        return true;\n"
    "      }\n"
    "    }\n"
    "  }\n"
    "  return false;\n"
    "}\n"
    "\n"
    "function botTryPlunder(gs, attackerIdx) {\n"
    "  var attacker = gs.players[attackerIdx];\n"
    "  if (attacker.hand.length < 1 || (attacker.pickingDeckAttacks || 0) <= 0) return false;\n"
    "  // Pick the deck with the weaker top card\n"
    "  var bestDeck = -1;\n"
    "  var bestDefStr = 9999;\n"
    "  for (var d = 0; d < gs.pickingDecks.length; d++) {\n"
    "    var deck = gs.pickingDecks[d];\n"
    "    if (deck.length === 0) continue;\n"
    "    var topCard = deck[deck.length - 1];\n"
    "    var str = gs.cardStrength(topCard.id);\n"
    "    if (str < bestDefStr) { bestDefStr = str; bestDeck = d; }\n"
    "  }\n"
    "  if (bestDeck === -1) return false;\n"
    "  var atkCard = botStrongestCard(gs, attacker.hand);\n"
    "  if (atkCard === null) return false;\n"
    "  var atkStr = gs.cardStrength(atkCard);\n"
    "  var success = (atkStr >= bestDefStr);\n"
    "  // Call preview first (locks cards), then resolve\n"
    "  gs.setPlunderPreview({ attackerIdx: attackerIdx, deckIndex: bestDeck, attackCardIds: [atkCard], attackingSymbol: 'none', attackingSymbol2: 'none' });\n"
    "  gs.plunderResolved(attackerIdx, bestDeck, success, [atkCard], false, []);\n"
    "  return true;\n"
    "}\n"
    "\n"
    "function botStrongestCard(gs, hand) {\n"
    "  if (!hand || hand.length === 0) return null;\n"
    "  var best = hand[0];\n"
    "  var bestStr = gs.cardStrength(hand[0]);\n"
    "  for (var i = 1; i < hand.length; i++) {\n"
    "    var s = gs.cardStrength(hand[i]);\n"
    "    if (s > bestStr) { bestStr = s; best = hand[i]; }\n"
    "  }\n"
    "  return best;\n"
    "}\n"
    "\n"
    "function botWeakestCard(gs, hand) {\n"
    "  if (!hand || hand.length === 0) return null;\n"
    "  var best = hand[0];\n"
    "  var bestStr = gs.cardStrength(hand[0]);\n"
    "  for (var i = 1; i < hand.length; i++) {\n"
    "    var s = gs.cardStrength(hand[i]);\n"
    "    if (s < bestStr) { bestStr = s; best = hand[i]; }\n"
    "  }\n"
    "  return best;\n"
    "}\n"
)

if old_auto not in src:
    print("ERROR: autoFinishBotTurnIfNeeded not found")
    sys.exit(1)
src = src.replace(old_auto, new_bot_ai)
print("OK 1/6: replaced autoFinishBotTurnIfNeeded with full bot AI")

# ─────────────────────────────────────────────────────────────────────────────
# 2. In finishTurn handler: replace isTutorial guard
# ─────────────────────────────────────────────────────────────────────────────

old_fin = nl("    if (sess.isTutorial) autoFinishBotTurnIfNeeded(sess);\n")
new_fin = nl("    playBotTurnIfNeeded(sess);\n")

if old_fin not in src:
    print("ERROR: isTutorial autoFinishBotTurnIfNeeded not found in finishTurn")
    sys.exit(1)
src = src.replace(old_fin, new_fin)
print("OK 2/6: finishTurn now calls playBotTurnIfNeeded unconditionally")

# ─────────────────────────────────────────────────────────────────────────────
# 3. In createSession handler: accept botCount and add bots
# ─────────────────────────────────────────────────────────────────────────────

old_create = nl(
    "    var sess = createSession(sessionName, allowHeroSelection, startingCards, manualSetup);\n"
    "    var cToken = (data && data.token) ? String(data.token).slice(0, 64) : null;\n"
    "    sess.users.push(makeUser(socket.id, name, cToken));\n"
)

new_create = nl(
    "    var sess = createSession(sessionName, allowHeroSelection, startingCards, manualSetup);\n"
    "    var botCount = Math.min(3, Math.max(0, parseInt(data && data.botCount) || 0));\n"
    "    var cToken = (data && data.token) ? String(data.token).slice(0, 64) : null;\n"
    "    sess.users.push(makeUser(socket.id, name, cToken));\n"
    "    for (var bi = 1; bi <= botCount; bi++) {\n"
    "      var botUser = makeUser('bot_' + sess.id + '_' + bi, 'Bot ' + bi);\n"
    "      botUser.isReady = true;\n"
    "      sess.users.push(botUser);\n"
    "    }\n"
)

if old_create not in src:
    print("ERROR: createSession handler not found")
    sys.exit(1)
src = src.replace(old_create, new_create)
print("OK 3/6: createSession accepts botCount and adds bots")

# ─────────────────────────────────────────────────────────────────────────────
# 4. In createTutorial handler: replace autoFinishBotTurnIfNeeded → playBotTurnIfNeeded
# ─────────────────────────────────────────────────────────────────────────────

old_tut = nl("    autoFinishBotTurnIfNeeded(sess);\n  });\n\n  socket.on('giveUp'")
new_tut = nl("    playBotTurnIfNeeded(sess);\n  });\n\n  socket.on('giveUp'")

if old_tut not in src:
    print("ERROR: tutorial autoFinishBotTurnIfNeeded not found")
    sys.exit(1)
src = src.replace(old_tut, new_tut)
print("OK 4/6: createTutorial now calls playBotTurnIfNeeded")

# ─────────────────────────────────────────────────────────────────────────────
# 5. In startGameForSession: skip bot emits + hook playBotTurnIfNeeded
# ─────────────────────────────────────────────────────────────────────────────

old_emit = nl(
    "  sess.users.forEach(function(u, idx) {\n"
    "    io.to(u.id).emit('gameState', {\n"
    "      playerIndex: idx,\n"
    "      gameState: sess.gameState.serialize()\n"
    "    });\n"
    "    console.log('gameState emitted to ' + u.name + ' (' + u.id + ') as player ' + idx);\n"
    "    // Persist player index in token map so they can reconnect to the right slot.\n"
    "    if (u.token && tokenMap[u.token]) {\n"
    "      tokenMap[u.token].playerIdx = idx;\n"
    "      tokenMap[u.token].sessionId = sess.id;\n"
    "    }\n"
    "  });\n"
    "  broadcastSessionList();\n"
    "  broadcastPlayerList();\n"
    "  return true;\n"
    "}\n"
)

new_emit = nl(
    "  sess.users.forEach(function(u, idx) {\n"
    "    if (!isBot(u)) {\n"
    "      io.to(u.id).emit('gameState', {\n"
    "        playerIndex: idx,\n"
    "        gameState: sess.gameState.serialize()\n"
    "      });\n"
    "      console.log('gameState emitted to ' + u.name + ' (' + u.id + ') as player ' + idx);\n"
    "    }\n"
    "    // Persist player index in token map so they can reconnect to the right slot.\n"
    "    if (u.token && tokenMap[u.token]) {\n"
    "      tokenMap[u.token].playerIdx = idx;\n"
    "      tokenMap[u.token].sessionId = sess.id;\n"
    "    }\n"
    "  });\n"
    "  // Auto-submit manual setup for bots\n"
    "  if (sess.manualSetup && sess.gameState.setupPhase) {\n"
    "    sess.users.forEach(function(u, idx) {\n"
    "      if (isBot(u)) {\n"
    "        var bp = sess.gameState.players[idx];\n"
    "        if (bp && bp.hand.length >= 4) {\n"
    "          var kingId = bp.hand[0];\n"
    "          var defIds = [bp.hand[1], bp.hand[2], bp.hand[3]];\n"
    "          var discardIds = bp.hand.length > 6 ? [bp.hand[4], bp.hand[5]] : [];\n"
    "          sess.gameState.applyManualSetup(idx, kingId, defIds, discardIds);\n"
    "        }\n"
    "      }\n"
    "    });\n"
    "  }\n"
    "  broadcastSessionList();\n"
    "  broadcastPlayerList();\n"
    "  // If the first player is a bot, start the bot turn chain\n"
    "  playBotTurnIfNeeded(sess);\n"
    "  return true;\n"
    "}\n"
)

if old_emit not in src:
    print("ERROR: startGameForSession emit loop not found")
    sys.exit(1)
src = src.replace(old_emit, new_emit)
print("OK 5/6: startGameForSession skips bot emits and hooks playBotTurnIfNeeded")

# ─────────────────────────────────────────────────────────────────────────────
# 6. Include botCount in the createSession console.log for debugging
# ─────────────────────────────────────────────────────────────────────────────

old_log = nl('    console.log("Session created: " + sess.id + " \'" + sess.name + "\' by " + name + " (heroes: " + allowHeroSelection + ", startingCards: " + sess.startingCards + ", manualSetup: " + manualSetup + ")");\n')
new_log = nl('    console.log("Session created: " + sess.id + " \'" + sess.name + "\' by " + name + " (heroes: " + allowHeroSelection + ", startingCards: " + sess.startingCards + ", manualSetup: " + manualSetup + ", bots: " + botCount + ")");\n')

if old_log not in src:
    print("WARN: console.log not found — skipping (non-critical)")
else:
    src = src.replace(old_log, new_log)
    print("OK 6/6: updated console.log to include botCount")

f.write_text(src)
print("\nAll patches applied successfully to server/index.js")
