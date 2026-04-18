var path = require('path');
var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server, { origins: '*:*' });

// Version endpoint — used by Android/iOS clients to check for updates.
app.get('/version', function(req, res) {
  res.json({ version: process.env.APP_VERSION || 'unknown' });
});

// Serve the mobile-optimised page at /m (canonical URL).
app.get('/m', function(req, res) {
  res.sendFile(path.join(__dirname, 'public', 'mobile.html'));
});

// Serve the same page for all browsers at /. The mobile.html is now
// universal: it works on all browsers and window sizes.
app.get('/', function(req, res) {
  res.sendFile(path.join(__dirname, 'public', 'mobile.html'));
});

app.use(require('express').static(path.join(__dirname, 'public')));

var GameState = require('./gameState');
var bot = require('./bot')(io, checkAndHandleWinner);

// ─── Session management ───────────────────────────────────────────────────────
// sessions: { [sessionId]: { id, name, users[], spectators[], gameState,
//             heroSelections{}, winnerHandled, timeToStart, timer } }
var sessions = {};
// socketToSession: { [socketId]: sessionId } — for O(1) session lookup
var socketToSession = {};
var _nextSessionId = 1;

// All connected sockets with their display names (set via registerPlayer).
// { [socketId]: { id, name } }
var connectedPlayers = {};

// Guest-token map — survives socket reconnections.
// { [token]: { name, socketId, sessionId, playerIdx } }
// token is a UUID v4 generated client-side and stored in the browser's localStorage.
var tokenMap = {};

function findTokenBySocketId(socketId) {
  var keys = Object.keys(tokenMap);
  for (var i = 0; i < keys.length; i++) {
    if (tokenMap[keys[i]].socketId === socketId) return keys[i];
  }
  return null;
}

function getPlayerStatus(socketId) {
  var sessId = socketToSession[socketId];
  if (!sessId) return 'Online';
  var sess = sessions[sessId];
  if (!sess) return 'Online';
  if (sess.spectators.indexOf(socketId) !== -1) return 'Watching: ' + sess.name;
  if (sess.gameState !== null) return 'In game: ' + sess.name;
  return 'In lobby: ' + sess.name;
}

function broadcastPlayerList() {
  var list = Object.keys(connectedPlayers)
    .filter(function(sid) { return connectedPlayers[sid].name; })
    .map(function(sid) {
      return { id: sid, name: connectedPlayers[sid].name, status: getPlayerStatus(sid) };
    });
  io.emit('playerList', list);
}

function createSession(name, allowHeroSelection, startingCards, manualSetup) {
  var id = 's' + (_nextSessionId++);
  sessions[id] = {
    id: id,
    name: name,
    allowHeroSelection: allowHeroSelection !== false, // default true
    startingCards: Math.min(10, Math.max(6, parseInt(startingCards, 10) || 8)),
    manualSetup: !!manualSetup,
    users: [],
    spectators: [],
    gameState: null,
    heroSelections: {},
    winnerHandled: false,
    timeToStart: 0,
    timer: null
  };
  return sessions[id];
}

function getSession(socketId) {
  var sid = socketToSession[socketId];
  return sid ? sessions[sid] : null;
}

function getSessionList() {
  return Object.values(sessions).map(function(s) {
    return { id: s.id, name: s.name, playerCount: s.users.length, running: s.gameState !== null };
  });
}

function broadcastSessionList() {
  io.emit('sessionList', getSessionList());
}

function makeUser(id, name, token) {
  return { id: id, name: name || 'Player', isReady: false, token: token || null };
}

function getReadyUsers(sess) {
  return sess.users.filter(function(u) { return u.isReady; });
}

function canSessionStart(sess, requesterSocketId) {
  if (!sess || sess.gameState !== null || !sess.users.length || sess.users[0].id !== requesterSocketId) {
    return false;
  }

  var requester = sess.users.find(function(u) { return u.id === requesterSocketId; });
  return !!requester && requester.isReady && getReadyUsers(sess).length >= 2;
}

function cancelStartCountdown(sess, notifyClients) {
  if (sess.timer) {
    clearInterval(sess.timer);
    sess.timer = null;
  }
  sess.timeToStart = 0;
  if (notifyClients) {
    io.to(sess.id).emit('startCountdownCanceled');
  }
}

function startCountdownForSession(sess, requesterSocketId, seconds) {
  if (!canSessionStart(sess, requesterSocketId)) {
    return false;
  }

  cancelStartCountdown(sess, false);
  sess.timeToStart = seconds;
  io.to(sess.id).emit('updateTimer', { seconds: sess.timeToStart });

  sess.timer = setInterval(function() {
    if (!canSessionStart(sess, requesterSocketId)) {
      cancelStartCountdown(sess, true);
      return;
    }

    sess.timeToStart--;
    io.to(sess.id).emit('updateTimer', { seconds: sess.timeToStart });
    console.log("Session " + sess.id + " seconds left: " + sess.timeToStart);

    if (sess.timeToStart <= 0) {
      cancelStartCountdown(sess, false);
      startGameForSession(sess, requesterSocketId);
    }
  }, 1000);

  return true;
}

function startGameForSession(sess, requesterSocketId) {
  if (!sess || sess.gameState !== null) return false;

  // Host is the first player in lobby order.
  if (!sess.users.length || sess.users[0].id !== requesterSocketId) {
    return false;
  }

  var readyUsers = sess.users.filter(function(u) { return u.isReady; });
  if (readyUsers.length < 2) {
    return false;
  }

  // Users who are not ready are removed from the session and sent back to list.
  var removedUsers = sess.users.filter(function(u) { return !u.isReady; });
  removedUsers.forEach(function(u) {
    delete socketToSession[u.id];
    delete sess.heroSelections[u.id];
    var s = io.sockets.sockets[u.id];
    if (s) {
      s.leave(sess.id);
      s.emit('leftSessionNotReady');
      s.emit('sessionList', getSessionList());
    }
  });

  sess.users = readyUsers;
  sess.winnerHandled = false;
  sess.gameState = new GameState(sess.users, { startingCards: sess.startingCards, manualSetup: sess.manualSetup });

  // Apply lobby starting hero selections before initial gameState broadcast.
  sess.users.forEach(function(u, idx) {
    var hero = sess.heroSelections[u.id];
    if (hero && hero !== 'None') {
      sess.gameState.heroAcquired(idx, hero);
    }
  });

  io.to(sess.id).emit('getUsers', getUsersWithHeroes(sess));
  sess.users.forEach(function(u, idx) {
    if (!bot.isBot(u)) {
      io.to(u.id).emit('gameState', {
        playerIndex: idx,
        gameState: sess.gameState.serialize()
      });
      console.log('gameState emitted to ' + u.name + ' (' + u.id + ') as player ' + idx);
    }
    // Persist player index in token map so they can reconnect to the right slot.
    if (u.token && tokenMap[u.token]) {
      tokenMap[u.token].playerIdx = idx;
      tokenMap[u.token].sessionId = sess.id;
    }
  });
  // Auto-submit manual setup for bots using the smart card-selection strategy
  if (sess.manualSetup && sess.gameState.setupPhase) {
    sess.users.forEach(function(u, idx) {
      if (bot.isBot(u)) {
        var setup = bot.autoSetupBot(sess.gameState, idx);
        if (setup) {
          sess.gameState.applyManualSetup(idx, setup.kingId, setup.defIds, setup.discardIds);
        }
      }
    });
  }
  broadcastSessionList();
  broadcastPlayerList();
  // If the first player is a bot, start the bot turn chain
  bot.playBotTurnIfNeeded(sess);
  return true;
}

function checkAndHandleWinner(sess) {
  if (!sess.gameState || sess.winnerHandled) return;
  const winner = sess.gameState.checkWinner();
  if (winner >= 0) {
    sess.winnerHandled = true;
    console.log("Session " + sess.id + " winner: player " + winner + " — closing session in 5 seconds");
    setTimeout(function() {
      var sessId = sess.id;
      // Notify all participants to return to the session list
      io.to(sessId).emit('returnToLobby');
      // Remove all socket→session mappings so these sockets are session-less again
      sess.users.forEach(function(u) {
        delete socketToSession[u.id];
        // Clear token map session binding so the player is not auto-reconnected to a dead game.
        if (u.token && tokenMap[u.token]) {
          delete tokenMap[u.token].sessionId;
          delete tokenMap[u.token].playerIdx;
        }
      });
      sess.spectators.forEach(function(sid) { delete socketToSession[sid]; });
      if (sess.timer) clearInterval(sess.timer);
      // Delete the session entirely — it won't appear in the session list anymore
      delete sessions[sessId];
      broadcastSessionList();
      broadcastPlayerList();
    }, 5000);
  }
}

function leaveCurrentSession(socket) {
  var sess = getSession(socket.id);
  if (!sess) return;
  var hero = sess.heroSelections[socket.id];
  if (hero && hero !== 'None') {
    socket.to(sess.id).emit('heroReleased', { heroName: hero });
  }
  delete sess.heroSelections[socket.id];
  var userIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
  if (userIdx !== -1) sess.users.splice(userIdx, 1);
  var specIdx = sess.spectators.indexOf(socket.id);
  if (specIdx !== -1) sess.spectators.splice(specIdx, 1);
  socket.leave(sess.id);
  io.to(sess.id).emit('getUsers', getUsersWithHeroes(sess));
  if (sess.users.length === 0 && sess.spectators.length === 0) {
    if (sess.timer) clearInterval(sess.timer);
    delete sessions[sess.id];
    console.log('Session ' + sess.id + ' deleted (empty)');
  }
  delete socketToSession[socket.id];
  broadcastSessionList();
  broadcastPlayerList();
}

var PORT = process.env.PORT || 8082;
server.listen(PORT, function() {
  console.log("Server is now running on port " + PORT);
});

// ── Bot AI ─────────────────────────────────────────────────────────────────

function isBot(player) {
  return player && player.id.indexOf('bot_') === 0;
}

function playBotTurnIfNeeded(sess) {
  if (!sess || !sess.gameState) return;
  var gs = sess.gameState;
  var player = gs.players[gs.currentPlayerIndex];
  if (!isBot(player) || player.isOut) return;
  setTimeout(function() {
    if (!sess.gameState) return;
    executeBotTurn(sess);
  }, 1500);
}

// Returns the card suit name for a card ID
function botCardSuit(cardId) {
  if (cardId > 52) return 'joker';
  var si = Math.floor((cardId - 1) / 13);
  return ['clubs', 'diamonds', 'hearts', 'spades'][si];
}

// Group non-joker hand cards by suit. Returns { suitName: [cardIds] }
function botGroupBySuit(hand) {
  var groups = {};
  for (var i = 0; i < hand.length; i++) {
    var id = hand[i];
    if (id > 52) continue;
    var suit = botCardSuit(id);
    if (!groups[suit]) groups[suit] = [];
    groups[suit].push(id);
  }
  return groups;
}

// Find the minimal subset of cards (from sorted list) with combined strength >= threshold.
// Returns array of cardIds, or null if impossible even with all cards.
function botMinimalSubset(gs, cards, threshold) {
  if (!cards || cards.length === 0) return null;
  var sorted = cards.slice().sort(function(a, b) { return gs.cardStrength(a) - gs.cardStrength(b); });
  var total = 0;
  for (var i = 0; i < sorted.length; i++) total += gs.cardStrength(sorted[i]);
  if (total < threshold) return null;
  // Greedy: take strongest cards first until we meet threshold
  var chosen = [];
  var sum = 0;
  for (var j = sorted.length - 1; j >= 0 && sum < threshold; j--) {
    chosen.push(sorted[j]);
    sum += gs.cardStrength(sorted[j]);
  }
  return chosen;
}

// Find the best single-suit combination from a hand (returns {sum, cards, suit})
function botBestSuitCombo(gs, hand) {
  var groups = botGroupBySuit(hand);
  var best = { sum: 0, cards: [], suit: 'none' };
  var suits = Object.keys(groups);
  for (var i = 0; i < suits.length; i++) {
    var suit = suits[i];
    var sum = 0;
    for (var j = 0; j < groups[suit].length; j++) sum += gs.cardStrength(groups[suit][j]);
    if (sum > best.sum) best = { sum: sum, cards: groups[suit], suit: suit };
  }
  return best;
}

// Choose the best plunder: smart multi-card combo, preferring bigger decks with larger margin.
// Returns { deckIndex, cardIds, symbol, success } or null.
function botChoosePlunder(gs, attackerIdx) {
  var p = gs.players[attackerIdx];
  if (!p || !p.hand || p.hand.length === 0 || (p.pickingDeckAttacks || 0) <= 0) return null;

  var groups = botGroupBySuit(p.hand);
  var bestChoice = null;
  var bestScore = -9999;

  for (var d = 0; d < gs.pickingDecks.length; d++) {
    var deck = gs.pickingDecks[d];
    if (deck.length === 0) continue;

    // Find any face-up card in the deck — that's what the player can see as a hint.
    var visibleStrength = 0;
    for (var fi = 0; fi < deck.length; fi++) {
      if (!deck[fi].covered) { visibleStrength = gs.cardStrength(deck[fi].id); break; }
    }
    if (visibleStrength === 0) continue; // entire deck is covered, skip

    // Bot is server-side so it can read the actual threshold (top covered card).
    var topCard = deck[deck.length - 1];
    var actualThreshold = gs.cardStrength(topCard.id);
    var deckSize = deck.length;

    // Target: aim for 12-15 total, at least meeting the real threshold.
    // Larger decks deserve a safer margin; smaller decks keep it economical.
    var safeTarget = Math.max(actualThreshold, deckSize >= 4 ? 14 : (deckSize >= 3 ? 13 : 12));

    var suits = Object.keys(groups);
    for (var si = 0; si < suits.length; si++) {
      var suit = suits[si];
      // Try to reach safeTarget; if impossible, just try to meet the actual threshold.
      var combo = botMinimalSubset(gs, groups[suit], safeTarget)
               || botMinimalSubset(gs, groups[suit], actualThreshold);
      if (!combo) continue;
      var comboSum = 0;
      for (var ci = 0; ci < combo.length; ci++) comboSum += gs.cardStrength(combo[ci]);
      // Skip wildly over-spending
      if (comboSum > actualThreshold + 6 && comboSum > 15) continue;
      var success = (comboSum >= actualThreshold);
      var waste = Math.max(0, comboSum - actualThreshold);
      // Prefer success > bigger deck > less waste > fewer cards
      var score = (success ? 1000 : -500) + deckSize * 10 - waste * 2 - combo.length;
      if (score > bestScore) {
        bestScore = score;
        bestChoice = { deckIndex: d, cardIds: combo, symbol: suit, success: success };
      }
    }
  }
  // Don't attempt plunder if facing certain failure
  return (bestChoice && (bestChoice.success || bestScore > -200)) ? bestChoice : null;
}

// Choose the best defense attack.
// allowScout=false: only attack known face-up cards.
// allowScout=true: also probe covered cards with a weak card (scout).
// Returns { defenderIdx, positionId, cardIds, symbol, success } or null.
function botChooseDefAttack(gs, attackerIdx, allowScout) {
  var attacker = gs.players[attackerIdx];
  if (!attacker || !attacker.hand || attacker.hand.length === 0) return null;

  var groups = botGroupBySuit(attacker.hand);
  var bestChoice = null;
  var bestScore = -9999;

  for (var di = 0; di < gs.players.length; di++) {
    if (di === attackerIdx) continue;
    var defender = gs.players[di];
    if (defender.isOut) continue;

    for (var slot = 1; slot <= 3; slot++) {
      var defCardId = defender.defCards[slot];
      if (defCardId == null) continue;

      var isFaceUp = defender.defCardsCovered && defender.defCardsCovered[slot] === false;

      if (isFaceUp) {
        var threshold = gs.cardStrength(defCardId);
        var suits = Object.keys(groups);
        for (var si = 0; si < suits.length; si++) {
          var suit = suits[si];
          var combo = botMinimalSubset(gs, groups[suit], threshold);
          if (!combo) continue;
          var comboSum = 0;
          for (var ci = 0; ci < combo.length; ci++) comboSum += gs.cardStrength(combo[ci]);
          var success = (comboSum >= threshold);
          // Bonus if this would open the king (only shield remaining)
          var shieldsLeft = 0;
          for (var s = 1; s <= 3; s++) {
            if (defender.defCards[s] != null || defender.topDefCards[s] != null) shieldsLeft++;
          }
          var score = (success ? 1000 : -200) + (shieldsLeft === 1 ? 100 : 0) - combo.length;
          if (score > bestScore) {
            bestScore = score;
            bestChoice = { defenderIdx: di, positionId: slot, cardIds: combo, symbol: suit, success: success };
          }
        }
      } else if (allowScout && attacker.hand.length > 1) {
        // Scout: probe with weakest non-joker card to reveal the defense card
        var nonJokerHand = attacker.hand.filter(function(id) { return id <= 52; });
        var weakestId = null, weakestStr = 9999;
        for (var ci = 0; ci < nonJokerHand.length; ci++) {
          var s = gs.cardStrength(nonJokerHand[ci]);
          if (s < weakestStr) { weakestStr = s; weakestId = nonJokerHand[ci]; }
        }
        if (weakestId !== null) {
          var scoutScore = 1; // low priority vs face-up attacks
          if (scoutScore > bestScore) {
            bestScore = scoutScore;
            bestChoice = { defenderIdx: di, positionId: slot,
                           cardIds: [weakestId], symbol: botCardSuit(weakestId), success: false };
          }
        }
      }
    }
  }
  return bestChoice;
}

// Try a king attack using minimal suit combo. Returns true if attempted.
// How long overlays stay visible (ms) — matches human player experience
var BOT_ACTION_DELAY = 1500;

// Async variant: shows attack preview, waits BOT_ACTION_DELAY, resolves, then calls callback(true/false).
function botTryKingAttackAsync(sess, gs, attackerIdx, callback) {
  var attacker = gs.players[attackerIdx];
  var groups = botGroupBySuit(attacker.hand);

  for (var di = 0; di < gs.players.length; di++) {
    if (di === attackerIdx) continue;
    var defender = gs.players[di];
    if (defender.isOut) continue;

    // All 3 defense slots must be empty (king exposed)
    var allEmpty = true;
    for (var s = 1; s <= 3; s++) {
      if (defender.defCards[s] != null || defender.topDefCards[s] != null) { allEmpty = false; break; }
    }
    if (!allEmpty) continue;

    var kingStr = gs.cardStrength(defender.kingCard);
    var suits = Object.keys(groups);
    for (var si = 0; si < suits.length; si++) {
      var suit = suits[si];
      var combo = botMinimalSubset(gs, groups[suit], kingStr);
      if (!combo) continue;
      var comboSum = 0;
      for (var ci = 0; ci < combo.length; ci++) comboSum += gs.cardStrength(combo[ci]);
      if (comboSum < kingStr) continue;
      gs.setAttackPreview({ attackerIdx: attackerIdx, defenderIdx: di, positionId: 0, level: 0,
                             attackingSymbol: suit, attackingSymbol2: 'none' });
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      (function(capturedDi, capturedCombo) {
        setTimeout(function() {
          gs.kingAttackResolved(attackerIdx, capturedDi, true, capturedCombo, false);
          io.to(sess.id).emit('stateUpdate', gs.serialize());
          checkAndHandleWinner(sess);
          callback(true);
        }, BOT_ACTION_DELAY);
      })(di, combo);
      return;
    }

    // Last resort: use a joker to eliminate
    var jokers = attacker.hand.filter(function(id) { return id > 52; });
    if (jokers.length > 0) {
      gs.setAttackPreview({ attackerIdx: attackerIdx, defenderIdx: di, positionId: 0, level: 0,
                             attackingSymbol: 'joker', attackingSymbol2: 'none' });
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      (function(capturedDi, capturedJoker) {
        setTimeout(function() {
          gs.kingAttackResolved(attackerIdx, capturedDi, true, [capturedJoker], false);
          io.to(sess.id).emit('stateUpdate', gs.serialize());
          checkAndHandleWinner(sess);
          callback(true);
        }, BOT_ACTION_DELAY);
      })(di, jokers[0]);
      return;
    }
  }
  callback(false);
}

// After a plunder: optionally attack a face-up defense card, then finish the turn.
function botContinueAfterPlunder(sess, gs, idx) {
  var atkAfterPlunder = botChooseDefAttack(gs, idx, false);
  if (atkAfterPlunder) {
    gs.setAttackPreview({ attackerIdx: idx, defenderIdx: atkAfterPlunder.defenderIdx,
                           positionId: atkAfterPlunder.positionId, level: 0,
                           attackingSymbol: atkAfterPlunder.symbol, attackingSymbol2: 'none' });
    io.to(sess.id).emit('stateUpdate', gs.serialize());
    var captured = atkAfterPlunder;
    setTimeout(function() {
      gs.defAttackResolved(idx, captured.defenderIdx, captured.positionId,
                            0, captured.success, captured.cardIds, false, []);
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      checkAndHandleWinner(sess);
      botFinishTurn(sess, gs, idx, true);
    }, BOT_ACTION_DELAY);
  } else {
    botFinishTurn(sess, gs, idx, false);
  }
}

// Expose defense/king if no attack was made, then finishTurn and chain next bot.
function botFinishTurn(sess, gs, idx, attackedPlayer) {
  var p = gs.players[idx];
  if (!attackedPlayer && p) {
    var exposed = false;
    for (var slot = 1; slot <= 3; slot++) {
      if (p.defCards[slot] != null && p.defCardsCovered && p.defCardsCovered[slot] !== false) {
        gs.exposeDefCard(idx, slot);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        exposed = true;
        break;
      }
    }
    if (!exposed && p.kingCard != null && p.kingCovered !== false) {
      gs.exposeKingCard(idx);
      io.to(sess.id).emit('stateUpdate', gs.serialize());
    }
  }
  gs.finishTurn();
  io.to(sess.id).emit('stateUpdate', gs.serialize());
  checkAndHandleWinner(sess);
  playBotTurnIfNeeded(sess);
}

// Replace an exposed (face-up) defense card with a face-down card from hand.
// Returns true if performed.
function botReplaceExposedDefense(gs, playerIdx) {
  var p = gs.players[playerIdx];
  if (!p || !p.defCardsCovered) return false;

  for (var slot = 1; slot <= 3; slot++) {
    if (p.defCardsCovered[slot] === false && p.defCards[slot] != null) {
      var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
      if (nonJokerHand.length < 1) return false;

      // Find weakest non-joker card to put face-down here
      var weakestId = null, weakestStr = 9999;
      for (var i = 0; i < nonJokerHand.length; i++) {
        var s = gs.cardStrength(nonJokerHand[i]);
        if (s < weakestStr) { weakestStr = s; weakestId = nonJokerHand[i]; }
      }
      if (weakestId === null) return false;

      // Move exposed card back to hand, place new card face-down
      var exposedCardId = p.defCards[slot];
      delete p.defCards[slot];
      delete p.defCardsCovered[slot];
      p.hand.push(exposedCardId);

      var hi = p.hand.indexOf(weakestId);
      if (hi !== -1) p.hand.splice(hi, 1);
      p.defCards[slot] = weakestId;
      if (!p.defCardsCovered) p.defCardsCovered = {};
      p.defCardsCovered[slot] = true;
      return true;
    }
  }
  return false;
}

// Determine the hero name the bot should acquire from a joker sacrifice oracle card.
function botHeroNameFromOracleCard(gs, oracleCardId) {
  var HERO_MAP = {
    2: 'Mercenaries', 3: 'Spy', 4: 'Marshal', 5: 'Battery Tower',
    6: 'Merchant',    7: 'Priest', 8: 'Reservists', 9: 'Saboteurs',
    10: 'Banneret',   11: 'Fortified Tower', 12: 'Magician', 13: 'Warlord'
  };
  var ALL_HEROES = Object.values(HERO_MAP);
  var RED_HEROES  = ['Merchant', 'Priest', 'Reservists', 'Mercenaries', 'Battery Tower', 'Marshal'];
  var BLACK_HEROES = ['Warlord', 'Spy', 'Banneret', 'Fortified Tower', 'Magician', 'Saboteurs'];

  // Collect already-owned heroes
  var owned = {};
  for (var i = 0; i < gs.players.length; i++) {
    (gs.players[i].heroes || []).forEach(function(h) { owned[h] = true; });
  }
  function pickFirst(list) {
    for (var j = 0; j < list.length; j++) { if (!owned[list[j]]) return list[j]; }
    return list.length > 0 ? list[0] : null; // fallback: grab any if all owned
  }

  if (oracleCardId > 52) return pickFirst(ALL_HEROES); // another joker: free choice

  var suitIdx = Math.floor((oracleCardId - 1) / 13);
  var cardIdx = (oracleCardId - 1) % 13 + 1;
  if (cardIdx === 1) {
    // Ace: red suits (diamonds=1, hearts=2) → red heroes; black → black heroes
    return pickFirst(suitIdx === 1 || suitIdx === 2 ? RED_HEROES : BLACK_HEROES);
  }
  return HERO_MAP[cardIdx] ? HERO_MAP[cardIdx] : pickFirst(ALL_HEROES);
}

// Sacrifice a joker for a hero, unless joker is needed for a critical action.
// Returns true if performed.
function botTryJokerSacrifice(sess, gs, playerIdx) {
  var p = gs.players[playerIdx];
  var jokers = p.hand.filter(function(id) { return id > 52; });
  if (jokers.length === 0) return false;

  // Exception 1: save joker to eliminate a player whose king is exposed
  for (var di = 0; di < gs.players.length; di++) {
    if (di === playerIdx || gs.players[di].isOut) continue;
    var defender = gs.players[di];
    var allEmpty = true;
    for (var s = 1; s <= 3; s++) {
      if (defender.defCards[s] != null || defender.topDefCards[s] != null) { allEmpty = false; break; }
    }
    if (allEmpty) {
      var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
      var bestCombo = botBestSuitCombo(gs, nonJokerHand);
      if (bestCombo.sum < gs.cardStrength(defender.kingCard)) return false; // need joker for kill
    }
  }

  // Exception 2: save joker if it is the only way to plunder a known deck
  if ((p.pickingDeckAttacks || 0) > 0) {
    var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
    var hasUncoveredDeck = false;
    var canPlunderWithoutJoker = false;
    for (var d = 0; d < gs.pickingDecks.length; d++) {
      var deck = gs.pickingDecks[d];
      if (deck.length === 0) continue;
      var topCard = deck[deck.length - 1];
      if (topCard.covered) continue;
      hasUncoveredDeck = true;
      var threshold = gs.cardStrength(topCard.id);
      var best = botBestSuitCombo(gs, nonJokerHand);
      if (best.sum >= threshold) { canPlunderWithoutJoker = true; break; }
    }
    if (hasUncoveredDeck && !canPlunderWithoutJoker) return false; // save joker for plunder
  }

  // Perform sacrifice
  if (gs.deck.length === 0) return false;
  var jokerId = jokers[0];
  var oracleCardId = gs.deck[gs.deck.length - 1];
  var heroName = botHeroNameFromOracleCard(gs, oracleCardId);
  if (!heroName) return false;

  gs.jokerSacrifice(playerIdx, jokerId, oracleCardId);
  gs.heroAcquired(playerIdx, heroName);
  io.to(sess.id).emit('stateUpdate', gs.serialize());
  return true;
}

function botFillDefense(gs, playerIdx) {
  var p = gs.players[playerIdx];
  var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
  for (var slot = 1; slot <= 3; slot++) {
    if (p.defCards[slot] == null && nonJokerHand.length > 1) {
      var weakestId = null, weakestStr = 9999;
      for (var i = 0; i < nonJokerHand.length; i++) {
        var s = gs.cardStrength(nonJokerHand[i]);
        if (s < weakestStr) { weakestStr = s; weakestId = nonJokerHand[i]; }
      }
      if (weakestId !== null) {
        gs.putDefCard(playerIdx, slot, weakestId);
        nonJokerHand.splice(nonJokerHand.indexOf(weakestId), 1);
      }
    }
  }
}

function executeBotTurn(sess) {
  var gs = sess.gameState;
  var idx = gs.currentPlayerIndex;
  var p = gs.players[idx];
  if (!p || p.isOut) return;

  // 1. Sacrifice joker for hero (unless saving it for critical use)
  botTryJokerSacrifice(sess, gs, idx);

  // 2. Fill empty defense slots with weakest hand cards
  botFillDefense(gs, idx);

  // 3. Replace any face-up (exposed) defense card with a fresh face-down card
  if (botReplaceExposedDefense(gs, idx)) {
    io.to(sess.id).emit('stateUpdate', gs.serialize());
  }

  // 4. Try to eliminate a player (king attack) — highest priority; async with overlay delay
  botTryKingAttackAsync(sess, gs, idx, function(didKingAttack) {
    if (didKingAttack) {
      botFinishTurn(sess, gs, idx, true);
      return;
    }

    // 5. Smart plunder — multi-card, economical combo
    var plunderChoice = botChoosePlunder(gs, idx);
    if (plunderChoice) {
      gs.setPlunderPreview({ attackerIdx: idx, deckIndex: plunderChoice.deckIndex,
                             attackCardIds: plunderChoice.cardIds,
                             attackingSymbol: plunderChoice.symbol, attackingSymbol2: 'none' });
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      var captured = plunderChoice;
      setTimeout(function() {
        gs.plunderResolved(idx, captured.deckIndex, captured.success,
                           captured.cardIds, false, []);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        checkAndHandleWinner(sess);
        // 6. After plunder: optional follow-up defense attack, then finish
        botContinueAfterPlunder(sess, gs, idx);
      }, BOT_ACTION_DELAY);
      return;
    }

    // 7. No plunder: attack a face-up defense card
    var atkChoice = botChooseDefAttack(gs, idx, false);
    if (atkChoice) {
      gs.setAttackPreview({ attackerIdx: idx, defenderIdx: atkChoice.defenderIdx,
                             positionId: atkChoice.positionId, level: 0,
                             attackingSymbol: atkChoice.symbol, attackingSymbol2: 'none' });
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      var capturedAtk = atkChoice;
      setTimeout(function() {
        gs.defAttackResolved(idx, capturedAtk.defenderIdx, capturedAtk.positionId,
                              0, capturedAtk.success, capturedAtk.cardIds, false, []);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        checkAndHandleWinner(sess);
        botFinishTurn(sess, gs, idx, true);
      }, BOT_ACTION_DELAY);
      return;
    }

    // 8. Scout: probe a covered defense card with a weak card to reveal it for future turns
    var scoutChoice = botChooseDefAttack(gs, idx, true);
    if (scoutChoice) {
      gs.setAttackPreview({ attackerIdx: idx, defenderIdx: scoutChoice.defenderIdx,
                             positionId: scoutChoice.positionId, level: 0,
                             attackingSymbol: scoutChoice.symbol, attackingSymbol2: 'none' });
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      var capturedScout = scoutChoice;
      setTimeout(function() {
        gs.defAttackResolved(idx, capturedScout.defenderIdx, capturedScout.positionId,
                              0, false, capturedScout.cardIds, false, []);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        checkAndHandleWinner(sess);
        botFinishTurn(sess, gs, idx, true);
      }, BOT_ACTION_DELAY);
      return;
    }

    // No attack of any kind — expose defense or king, then finish
    botFinishTurn(sess, gs, idx, false);
  });
}

// Legacy helpers (still used in tutorial/other paths)
function botStrongestCard(gs, hand) {
  if (!hand || hand.length === 0) return null;
  var best = hand[0], bestStr = gs.cardStrength(hand[0]);
  for (var i = 1; i < hand.length; i++) {
    var s = gs.cardStrength(hand[i]);
    if (s > bestStr) { bestStr = s; best = hand[i]; }
  }
  return best;
}

function botWeakestCard(gs, hand) {
  if (!hand || hand.length === 0) return null;
  var best = hand[0], bestStr = gs.cardStrength(hand[0]);
  for (var i = 1; i < hand.length; i++) {
    var s = gs.cardStrength(hand[i]);
    if (s < bestStr) { bestStr = s; best = hand[i]; }
  }
  return best;
}


// Build getUsers payload augmented with each player's current hero selection.
function getUsersWithHeroes(sess) {
  return sess.users.map(function(u) {
    return { id: u.id, name: u.name, isReady: u.isReady, heroSelection: sess.heroSelections[u.id] || 'None' };
  });
}

// Auto-advance hero auction for any bot that is next to bid.
// Bots with no heroes bid if they can meet the minimum; otherwise they pass.
function botAdvanceAuction(sess) {
  if (!sess || !sess.gameState || !sess.gameState.pendingHeroAuction) return;
  var auction = sess.gameState.pendingHeroAuction;
  var bidderIdx = auction.currentBidderIdx;
  if (bidderIdx !== null && bidderIdx !== undefined &&
      sess.users[bidderIdx] && bot.isBot(sess.users[bidderIdx])) {
    var bBot = sess.gameState.players[bidderIdx];
    var botHeroCount = (bBot.heroes || []).length;
    var minBid = Math.max(auction.minBid,
      auction.currentBid ? auction.currentBid.totalStrength + 1 : auction.minBid);

    // Bot bids if it has no heroes and can cover the minimum bid with hand cards.
    if (botHeroCount === 0) {
      var nonJokers = (bBot.hand || []).filter(function(id) { return id <= 52; });
      // Sort descending by strength so we pick fewest (strongest) cards first.
      var sorted = nonJokers.slice().sort(function(a, b) {
        return sess.gameState.cardStrength(b) - sess.gameState.cardStrength(a);
      });
      var chosen = [], sum = 0;
      for (var j = 0; j < sorted.length && sum < minBid; j++) {
        chosen.push(sorted[j]);
        sum += sess.gameState.cardStrength(sorted[j]);
      }
      if (chosen.length > 0 && sum >= minBid) {
        var ok = sess.gameState.heroAuctionBid(bidderIdx, chosen, []);
        if (ok) {
          io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
          botAdvanceAuction(sess);
          return;
        }
      }
    }

    // Bot passes.
    sess.gameState.heroAuctionPass(bidderIdx);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    botAdvanceAuction(sess);
  }
}

io.on('connection', function(socket) {
  console.log("User Connected: " + socket.id);
  connectedPlayers[socket.id] = { id: socket.id, name: '' };
  socket.emit('socketID', { id: socket.id });
  socket.emit('sessionList', getSessionList());
  socket.emit('playerList', Object.keys(connectedPlayers)
    .filter(function(sid) { return connectedPlayers[sid].name; })
    .map(function(sid) {
      return { id: sid, name: connectedPlayers[sid].name, status: getPlayerStatus(sid) };
    }));

  socket.on('disconnect', function() {
    console.log("User Disconnected: " + socket.id);
    // If the player was in a running game, preserve their session slot so they
    // can reconnect transparently (same game, same player index).
    var sess = getSession(socket.id);
    if (sess && sess.gameState !== null) {
      var token = findTokenBySocketId(socket.id);
      if (token) {
        tokenMap[token].socketId = null;
        delete socketToSession[socket.id];
        delete connectedPlayers[socket.id];
        broadcastPlayerList();
        console.log('Player with token ' + token.slice(0, 8) + '... went offline — slot preserved in session ' + sess.id);
        return;
      }
    }
    delete connectedPlayers[socket.id];
    leaveCurrentSession(socket);
    broadcastPlayerList();
  });

  // ── Session management events ────────────────────────────────────────────

  socket.on('leaveSession', function() {
    console.log('User ' + socket.id + ' left session');
    leaveCurrentSession(socket);
  });

  socket.on('registerPlayer', function(data) {
    var name = (data && data.name) ? String(data.name).slice(0, 30) : '';
    var token = (data && data.token) ? String(data.token).slice(0, 64) : null;
    if (connectedPlayers[socket.id]) connectedPlayers[socket.id].name = name;

    if (token) {
      if (!tokenMap[token]) tokenMap[token] = {};

      // If another socket is still alive with this token it is a duplicate tab.
      // Kick the old connection so only the most-recent tab is active.
      var prevSocketId = tokenMap[token].socketId;
      if (prevSocketId && prevSocketId !== socket.id) {
        var prevSocket = io.sockets.sockets[prevSocketId];
        if (prevSocket) {
          console.log('Duplicate tab for token ' + token.slice(0, 8) + '... — disconnecting old socket ' + prevSocketId);
          prevSocket.emit('duplicateTab');
          prevSocket.disconnect(true);
        }
      }

      tokenMap[token].name = name;
      tokenMap[token].socketId = socket.id;

      // Reconnect player to an active game if their token still has a live session slot.
      var sessId = tokenMap[token].sessionId;
      var playerIdx = tokenMap[token].playerIdx;
      if (sessId !== undefined && playerIdx !== undefined) {
        var sess = sessions[sessId];
        if (sess && sess.gameState !== null) {
          var user = sess.users.find(function(u) { return u.token === token; });
          if (user) {
            delete socketToSession[user.id]; // remove stale old-socket entry
            user.id = socket.id;
            socketToSession[socket.id] = sessId;
            socket.join(sessId);
            socket.emit('gameState', { playerIndex: playerIdx, gameState: sess.gameState.serialize() });
            broadcastPlayerList();
            console.log('Reconnected ' + name + ' (token ' + token.slice(0, 8) + '...) to session ' + sessId + ' as player ' + playerIdx);
            return;
          }
        }
        // Session no longer exists or game not running — clear stale token session info.
        delete tokenMap[token].sessionId;
        delete tokenMap[token].playerIdx;
      }
    }

    broadcastPlayerList();
  });

  socket.on('createSession', function(data) {
    leaveCurrentSession(socket); // clean up any previous session first
    var name = (data && data.name) ? String(data.name).slice(0, 30) : 'Player';
    if (connectedPlayers[socket.id]) connectedPlayers[socket.id].name = name;
    var sessionName = (data && data.sessionName) ? String(data.sessionName).slice(0, 50) : name + "'s game";
    var allowHeroSelection = (data && data.allowHeroSelection !== false);
    var startingCards = (data && data.startingCards) ? parseInt(data.startingCards, 10) : 8;
    var manualSetup = !!(data && data.manualSetup);
    var sess = createSession(sessionName, allowHeroSelection, startingCards, manualSetup);
    var botCount = Math.min(3, Math.max(0, parseInt(data && data.botCount) || 0));
    var cToken = (data && data.token) ? String(data.token).slice(0, 64) : null;
    sess.users.push(makeUser(socket.id, name, cToken));
    for (var bi = 1; bi <= botCount; bi++) {
      var botUser = makeUser('bot_' + sess.id + '_' + bi, 'Bot ' + bi);
      botUser.isReady = true;
      sess.users.push(botUser);
    }
    if (cToken) {
      if (!tokenMap[cToken]) tokenMap[cToken] = {};
      tokenMap[cToken].name = name;
      tokenMap[cToken].socketId = socket.id;
      tokenMap[cToken].sessionId = sess.id;
    }
    socketToSession[socket.id] = sess.id;
    socket.join(sess.id);
    console.log("Session created: " + sess.id + " '" + sess.name + "' by " + name + " (heroes: " + allowHeroSelection + ", startingCards: " + sess.startingCards + ", manualSetup: " + manualSetup + ", bots: " + botCount + ")");
    socket.emit('sessionJoined', { sessionId: sess.id, allowHeroSelection: sess.allowHeroSelection, startingCards: sess.startingCards, manualSetup: sess.manualSetup });
    io.to(sess.id).emit('getUsers', getUsersWithHeroes(sess));
    socket.emit('gameStatus', { running: false });
    broadcastSessionList();
    broadcastPlayerList();
  });

  socket.on('joinSession', function(data) {
    if (!data || !data.sessionId) return;
    var sess = sessions[data.sessionId];
    if (!sess) { socket.emit('sessionNotFound'); return; }
    if (sess.gameState !== null) { socket.emit('gameStatus', { running: true }); return; }
    leaveCurrentSession(socket); // leave any previous session first
    sess = sessions[data.sessionId]; // re-fetch — leaveCurrentSession may have deleted a different session
    var name = (data.name) ? String(data.name).slice(0, 30) : 'Player';
    var jToken = (data && data.token) ? String(data.token).slice(0, 64) : null;
    if (connectedPlayers[socket.id]) connectedPlayers[socket.id].name = name;
    var existing = sess.users.find(function(u) { return u.id === socket.id; });
    if (!existing) sess.users.push(makeUser(socket.id, name, jToken));
    if (jToken) {
      if (!tokenMap[jToken]) tokenMap[jToken] = {};
      tokenMap[jToken].name = name;
      tokenMap[jToken].socketId = socket.id;
      tokenMap[jToken].sessionId = sess.id;
    }
    socketToSession[socket.id] = sess.id;
    socket.join(sess.id);
    console.log("User " + name + " joined session " + sess.id);
    // Send existing hero reservations to the new joiner
    Object.keys(sess.heroSelections).forEach(function(sid) {
      var h = sess.heroSelections[sid];
      if (h && h !== 'None' && sid !== socket.id) {
        socket.emit('heroReserved', { heroName: h });
      }
    });
    socket.emit('sessionJoined', { sessionId: sess.id, allowHeroSelection: sess.allowHeroSelection, startingCards: sess.startingCards, manualSetup: sess.manualSetup });
    io.to(sess.id).emit('getUsers', getUsersWithHeroes(sess));
    socket.emit('gameStatus', { running: false });
    broadcastSessionList();
    broadcastPlayerList();
  });

  socket.on('joinSessionSpectator', function(data) {
    if (!data || !data.sessionId) return;
    var sess = sessions[data.sessionId];
    if (!sess || !sess.gameState) { socket.emit('gameStatus', { running: false }); return; }
    console.log("Spectator joined session " + sess.id + ": " + socket.id);
    if (sess.spectators.indexOf(socket.id) === -1) sess.spectators.push(socket.id);
    socketToSession[socket.id] = sess.id;
    socket.join(sess.id);
    socket.emit('sessionJoined', { sessionId: sess.id });
    socket.emit('gameState', { playerIndex: -1, gameState: sess.gameState.serialize() });
  });

  // ── Lobby events ─────────────────────────────────────────────────────────

  socket.on('setUserReady', function(id) {
    var sess = getSession(socket.id);
    if (!sess) return;
    console.log("Set User Ready: " + id);
    for (var i = 0; i < sess.users.length; i++) {
      if (sess.users[i].id === id) {
        if (sess.users[i].isReady === false) {
          sess.users[i].isReady = true;
        } else {
          sess.users[i].isReady = false;
        }
      }
    }
    if (sess.timer && !canSessionStart(sess, sess.users[0] && sess.users[0].id)) {
      cancelStartCountdown(sess, true);
    }
    io.to(sess.id).emit('getUsers', getUsersWithHeroes(sess));
  });
  
  socket.on('startTimer', function(seconds) {
    var sess = getSession(socket.id);
    if (!sess) return;
    console.log("Start Timer for session " + sess.id);
    if (sess.gameState !== null) {
      console.log("Game already running — rejecting startTimer");
      socket.emit('gameAlreadyRunning');
      return;
    }
    if (!startCountdownForSession(sess, socket.id, seconds)) {
      socket.emit('notEnoughReadyPlayers');
    }
  });

  socket.on('startGame', function() {
    var sess = getSession(socket.id);
    if (!sess) return;
    if (sess.gameState !== null) {
      socket.emit('gameAlreadyRunning');
      return;
    }
    if (!startCountdownForSession(sess, socket.id, 5)) {
      socket.emit('notEnoughReadyPlayers');
    }
  });

  socket.on('stopTimer', function() {
    var sess = getSession(socket.id);
    if (!sess) return;
    cancelStartCountdown(sess, true);
  });

  // Client requests full state resync
  socket.on('requestStateSync', function() {
    var sess = getSession(socket.id);
    if (sess && sess.gameState) {
      socket.emit('stateUpdate', sess.gameState.serialize());
    }
  });

  // Manual setup phase: player submits their king and defense card choices
  socket.on('submitSetup', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState || !sess.gameState.setupPhase) return;
    var playerIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    if (playerIdx === -1) return;
    var kingCardId = data && data.kingCardId;
    var defCardIds = data && Array.isArray(data.defCardIds) ? data.defCardIds : [];
    if (defCardIds.length !== 3) return;
    var discardCardIds = data && Array.isArray(data.discardCardIds) ? data.discardCardIds : [];
    var ok = sess.gameState.applyManualSetup(playerIdx, kingCardId, defCardIds, discardCardIds);
    if (!ok) {
      console.log('submitSetup rejected for player ' + playerIdx + ' in session ' + sess.id);
      return;
    }
    console.log('submitSetup accepted for player ' + playerIdx + ' in session ' + sess.id + (sess.gameState.setupPhase ? ' (waiting for others)' : ' (setup complete)'));
    // Broadcast updated state to all players
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    // Persist player indices once setup is complete
    if (!sess.gameState.setupPhase) {
      sess.users.forEach(function(u, idx) {
        if (u.token && tokenMap[u.token]) {
          tokenMap[u.token].playerIdx = idx;
          tokenMap[u.token].sessionId = sess.id;
        }
      });
    }
  });

  socket.on('finishTurn', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("Turn finished by player index: " + data.currentPlayerIndex);
    if (data.currentPlayerIndex !== sess.gameState.currentPlayerIndex) {
      console.log("finishTurn rejected: server currentPlayerIndex=" + sess.gameState.currentPlayerIndex + ", client sent=" + data.currentPlayerIndex);
      return;
    }
    sess.gameState.finishTurn();
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    bot.playBotTurnIfNeeded(sess);
  });

  socket.on('putDefCard', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("putDefCard: playerIdx=" + data.playerIdx + " pos=" + data.positionId + " cardId=" + data.cardId);
    sess.gameState.putDefCard(data.playerIdx, data.positionId, data.cardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('takeDefCard', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("takeDefCard: playerIdx=" + data.playerIdx + " pos=" + data.positionId);
    sess.gameState.takeDefCard(data.playerIdx, data.positionId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('addToCemetery', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("addToCemetery: playerIdx=" + data.playerIdx + " cardIds=" + data.cardIds + " draw=" + data.drawFromDeck);
    sess.gameState.addToCemetery(data.playerIdx, data.cardIds || [], data.drawFromDeck || 0);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('discardDefCards', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("discardDefCards: playerIdx=" + data.playerIdx);
    sess.gameState.discardDefCards(data.playerIdx, data.slots || []);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('plunderPreview', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.setPlunderPreview(data);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('plunderResolved', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("plunderResolved: attackerIdx=" + data.attackerIdx + " deckIndex=" + data.deckIndex + " success=" + data.success);
    sess.gameState.plunderResolved(data.attackerIdx, data.deckIndex, data.success, data.attackCardIds || [], data.kingUsed || false, data.attackerOwnDefCardIds || []);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    checkAndHandleWinner(sess);
  });

  socket.on('attackPreview', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.setAttackPreview(data);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('defAttackResolved', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("defAttackResolved: attackerIdx=" + data.attackerIdx + " targetPlayerIdx=" + data.targetPlayerIdx + " success=" + data.success);
    sess.gameState.defAttackResolved(data.attackerIdx, data.targetPlayerIdx, data.positionId, data.level, data.success, data.attackCardIds || [], data.kingUsed || false, data.attackerOwnDefCardIds || []);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    checkAndHandleWinner(sess);
  });

  socket.on('kingAttackResolved', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("kingAttackResolved: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " success=" + data.success);
    sess.gameState.kingAttackResolved(data.attackerIdx, data.defenderIdx, data.success, data.attackCardIds || [], data.kingUsed || false);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    checkAndHandleWinner(sess);
  });

  socket.on('heroSelectedFromKingDefeat', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var pending = sess.gameState.pendingHeroSelection;
    if (!pending) return;
    var userIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    if (userIdx !== pending.attackerIdx) return;
    console.log("heroSelectedFromKingDefeat: attacker=" + userIdx + " hero=" + data.heroName);
    sess.gameState.resolveHeroSelection(data.heroName);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('exposeDefCard', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("exposeDefCard: playerIdx=" + data.playerIdx + " slot=" + data.slot);
    sess.gameState.exposeDefCard(data.playerIdx, data.slot);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('exposeKingCard', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("exposeKingCard: playerIdx=" + data.playerIdx);
    sess.gameState.exposeKingCard(data.playerIdx);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('jokerSacrifice', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("jokerSacrifice: playerIdx=" + data.playerIdx);
    sess.gameState.jokerSacrifice(data.playerIdx, data.jokerCardId, data.drawnCardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('heroAcquired', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("heroAcquired: playerIndex=" + data.playerIndex + " heroName=" + data.heroName);
    sess.gameState.heroAcquired(data.playerIndex, data.heroName);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('heroLost', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("heroLost: playerIndex=" + data.playerIndex + " heroName=" + data.heroName);
    sess.gameState.heroLost(data.playerIndex, data.heroName);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('initiateHeroSale', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var playerIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    var ok = sess.gameState.initiateHeroSale(playerIdx, String(data.heroName || ''), parseInt(data.minBid) || 0);
    if (ok) {
      io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
      botAdvanceAuction(sess);
    }
  });

  socket.on('heroAuctionBid', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var playerIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    var handIds = (data.handCardIds || []).map(Number);
    var defIds = (data.defCardIds || []).map(Number);
    var ok = sess.gameState.heroAuctionBid(playerIdx, handIds, defIds);
    if (ok) {
      io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
      botAdvanceAuction(sess);
    }
  });

  socket.on('heroAuctionPass', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var playerIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    sess.gameState.heroAuctionPass(playerIdx);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    botAdvanceAuction(sess);
  });

  socket.on('heroSelected', function(heroName) {
    var sess = getSession(socket.id);
    if (!sess) return;
    var oldHero = sess.heroSelections[socket.id] || 'None';
    sess.heroSelections[socket.id] = heroName;
    if (oldHero !== 'None') {
      socket.to(sess.id).emit('heroReleased', { heroName: oldHero });
    }
    if (heroName !== 'None') {
      socket.to(sess.id).emit('heroReserved', { heroName: heroName });
    }
  });

  // Host sets starting hero for a bot slot.
  socket.on('setBotHeroSelection', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    // Only the host (first user) may set bot heroes.
    if (!sess.users.length || sess.users[0].id !== socket.id) return;
    var botUserId = String((data && data.botUserId) || '');
    var heroName  = String((data && data.heroName)  || 'None');
    var botUser = sess.users.find(function(u) { return u.id === botUserId && bot.isBot(u); });
    if (!botUser) return;
    var oldHero = sess.heroSelections[botUserId] || 'None';
    if (oldHero !== 'None') {
      socket.to(sess.id).emit('heroReleased', { heroName: oldHero });
    }
    sess.heroSelections[botUserId] = heroName;
    if (heroName !== 'None') {
      socket.to(sess.id).emit('heroReserved', { heroName: heroName });
    }
    io.to(sess.id).emit('getUsers', getUsersWithHeroes(sess));
  });

  socket.on('mercDefBoost', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    socket.to(sess.id).emit('mercDefBoost', data);
  });

  socket.on('reservistsKingBoost', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    socket.to(sess.id).emit('reservistsKingBoost', data);
  });

  socket.on('warlordDirectAttack', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.warlordDirectAttack(data.playerIdx);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('warlordKingSwap', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.warlordKingSwap(data.playerIdx, data.oldKingCardId, data.newKingCardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('merchantTrade', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.merchantTrade(data.playerIdx, data.discardedCardId, data.drawnCardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('merchantSecondTry', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.merchantSecondTry(data.playerIdx, data.firstCardId, data.secondCardId, data.isJoker);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('fortifiedTowerStack', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.putTopDefCard(data.playerIdx, data.slot, data.cardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('magicianSwap', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.magicianSwap(data.playerIdx, data.targetPlayerIdx, data.positionId,
        data.bottomCardId, data.bottomCovered, data.topCardId, data.topCovered);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('spyFlip', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var userIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    if (userIdx === -1) return;
    if (!sess.gameState.spyFlip(userIdx, data.targetPlayerIdx, data.slot)) return;
    socket.to(sess.id).emit('spyFlip', data);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('spyExtend', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var userIdx = sess.users.findIndex(function(u) { return u.id === socket.id; });
    if (userIdx === -1) return;
    if (!sess.gameState.spyExtend(userIdx, data.cardId)) return;
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('batteryDefenseCheck', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    var targetIdx = data && data.targetPlayerIdx;
    // Auto-respond on behalf of a bot defender
    if (targetIdx !== undefined && sess.gameState && sess.users[targetIdx] && bot.isBot(sess.users[targetIdx])) {
      var defPlayer = sess.gameState.players[targetIdx];
      if (defPlayer && (defPlayer.batteryTowerCharges || 0) > 0) {
        // Bot always uses Battery Tower to deny the attack
        defPlayer.batteryTowerCharges--;
        io.to(sess.id).emit('batteryDenyAttack', {
          attackerIdx:    data.attackerIdx,
          targetPlayerIdx: targetIdx,
          positionId:     data.positionId,
          isKing:         data.isKing || false
        });
      } else {
        // No charges — auto-allow
        io.to(sess.id).emit('batteryAllowAttack', { attackerIdx: data.attackerIdx });
      }
      return;
    }
    socket.to(sess.id).emit('batteryDefenseCheck', data);
  });

  socket.on('batteryAllowAttack', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    socket.to(sess.id).emit('batteryAllowAttack', data);
  });

  socket.on('batteryDenyAttack', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    socket.to(sess.id).emit('batteryDenyAttack', data);
  });

  socket.on('priestConvert', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("priestConvert: attackerIdx=" + data.attackerIdx + " targetIdx=" + data.targetPlayerIdx + " cardId=" + data.cardId);
    sess.gameState.priestConvert(data.attackerIdx, data.targetPlayerIdx, data.cardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('priestAttemptFailed', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    sess.gameState.priestAttemptFailed(data.attackerIdx);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('sabotage', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("sabotage: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " pos=" + data.positionId);
    sess.gameState.sabotage(data.attackerIdx, data.defenderIdx, data.positionId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('sabotageCallback', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("sabotageCallback: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " pos=" + data.positionId);
    sess.gameState.sabotageCallback(data.attackerIdx, data.defenderIdx, data.positionId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('sabotageSacrifice', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("sabotageSacrifice: defenderIdx=" + data.defenderIdx + " pos=" + data.positionId);
    const attackerIdx = sess.gameState.sabotageSacrifice(data.defenderIdx, data.positionId);
    if (attackerIdx !== undefined && sess.users[attackerIdx]) {
      io.to(sess.users[attackerIdx].id).emit('saboteurDestroyed', { attackerIdx: attackerIdx });
    }
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('sabotageEmptySlotSacrifice', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("sabotageEmptySlotSacrifice: defenderIdx=" + data.defenderIdx + " pos=" + data.positionId + " card=" + data.handCardId);
    const attackerIdx = sess.gameState.sabotageEmptySlotSacrifice(data.defenderIdx, data.positionId, data.handCardId);
    if (attackerIdx !== undefined && sess.users[attackerIdx]) {
      io.to(sess.users[attackerIdx].id).emit('saboteurDestroyed', { attackerIdx: attackerIdx });
    }
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('createTutorial', function() {
    leaveCurrentSession(socket);
    var sess = createSession('Tutorial', false, 6, false);
    sess.isTutorial = true;
    var player = connectedPlayers[socket.id];
    var userName = player ? player.name : 'Player';
    var botId = 'bot_' + sess.id;
    sess.users = [
      makeUser(socket.id, userName),
      makeUser(botId, 'Tutorial Bot')
    ];
    socket.join(sess.id);
    socketToSession[socket.id] = sess.id;
    sess.gameState = new GameState(sess.users, { startingCards: 8 });
    sess.gameState.isTutorial = true;
    io.to(socket.id).emit('gameState', {
      playerIndex: 0,
      gameState: sess.gameState.serialize()
    });
    broadcastSessionList();
    broadcastPlayerList();
    bot.playBotTurnIfNeeded(sess);
  });

  socket.on('giveUp', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var playerIdx = data.playerIndex;
    if (playerIdx < 0 || playerIdx >= sess.gameState.players.length) return;
    var player = sess.gameState.players[playerIdx];
    if (player.isOut) return;
    console.log("Player " + playerIdx + " (" + player.name + ") gave up in session " + sess.id);
    player.isOut = true;
    if (sess.gameState.currentPlayerIndex === playerIdx) {
      sess.gameState.finishTurn();
    }
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
    checkAndHandleWinner(sess);
    bot.playBotTurnIfNeeded(sess);
  });

  socket.on('giveUpAndLeave', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    var playerIdx = data.playerIndex;
    if (playerIdx < 0 || playerIdx >= sess.gameState.players.length) return;
    var player = sess.gameState.players[playerIdx];
    if (!player.isOut) {
      console.log("Player " + playerIdx + " (" + player.name + ") gave up & left session " + sess.id);
      player.isOut = true;
      var wasCurrentPlayer = sess.gameState.currentPlayerIndex === playerIdx;
      if (wasCurrentPlayer) {
        sess.gameState.finishTurn();
      }
      io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
      checkAndHandleWinner(sess);
      // Only restart bot chain if we advanced the turn — if it's already a bot's turn,
      // the existing bot chain is still running and must not be double-scheduled.
      if (wasCurrentPlayer) {
        bot.playBotTurnIfNeeded(sess);
      }
    }
    // Clear token map so a page-refresh does not ghost-reconnect the player.
    var token = findTokenBySocketId(socket.id);
    if (token && tokenMap[token]) {
      delete tokenMap[token].sessionId;
      delete tokenMap[token].playerIdx;
    }
    leaveCurrentSession(socket);
  });
});
