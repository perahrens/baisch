var path = require('path');
var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server, { origins: '*:*' });

// Serve the mobile-optimised page at /m.
app.get('/m', function(req, res) {
  res.sendFile(path.join(__dirname, 'public', 'mobile.html'));
});

// Auto-redirect mobile browsers visiting / to /m.
app.get('/', function(req, res, next) {
  var ua = req.headers['user-agent'] || '';
  if (/mobile|android|iphone|ipad|ipod/i.test(ua)) {
    return res.redirect('/m');
  }
  next();
});

app.use(require('express').static(path.join(__dirname, 'public')));

var GameState = require('./gameState');

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

  io.to(sess.id).emit('getUsers', sess.users);
  sess.users.forEach(function(u, idx) {
    if (!isBot(u)) {
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
  // Auto-submit manual setup for bots
  if (sess.manualSetup && sess.gameState.setupPhase) {
    sess.users.forEach(function(u, idx) {
      if (isBot(u)) {
        var bp = sess.gameState.players[idx];
        if (bp && bp.hand.length >= 4) {
          var kingId = bp.hand[0];
          var defIds = [bp.hand[1], bp.hand[2], bp.hand[3]];
          var discardIds = bp.hand.length > 6 ? [bp.hand[4], bp.hand[5]] : [];
          sess.gameState.applyManualSetup(idx, kingId, defIds, discardIds);
        }
      }
    });
  }
  broadcastSessionList();
  broadcastPlayerList();
  // If the first player is a bot, start the bot turn chain
  playBotTurnIfNeeded(sess);
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
  io.to(sess.id).emit('getUsers', sess.users);
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

function executeBotTurn(sess) {
  var gs = sess.gameState;
  var idx = gs.currentPlayerIndex;
  var p = gs.players[idx];
  if (!p || p.isOut) return;

  // 1. Fill empty defense slots (keep at least 1 card in hand)
  botFillDefense(gs, idx);

  // 2. Try to attack
  if (p.hand.length > 0) {
    if (!botTryKingAttack(gs, idx)) {
      if (!botTryDefAttack(gs, idx)) {
        botTryPlunder(gs, idx);
      }
    }
  }

  // 3. Finish turn
  gs.finishTurn();
  io.to(sess.id).emit('stateUpdate', gs.serialize());
  checkAndHandleWinner(sess);

  // Next player might be a bot too
  playBotTurnIfNeeded(sess);
}

function botFillDefense(gs, playerIdx) {
  var p = gs.players[playerIdx];
  for (var slot = 1; slot <= 3; slot++) {
    if (p.defCards[slot] == null && p.hand.length > 1) {
      var cardId = botWeakestCard(gs, p.hand);
      if (cardId !== null) gs.putDefCard(playerIdx, slot, cardId);
    }
  }
}

function botTryKingAttack(gs, attackerIdx) {
  var attacker = gs.players[attackerIdx];
  for (var di = 0; di < gs.players.length; di++) {
    if (di === attackerIdx) continue;
    var defender = gs.players[di];
    if (defender.isOut) continue;
    // Check if all 3 defense slots are empty (king is exposed)
    var allEmpty = true;
    for (var s = 1; s <= 3; s++) {
      if (defender.defCards[s] != null || defender.topDefCards[s] != null) { allEmpty = false; break; }
    }
    if (!allEmpty) continue;
    // Attack with strongest card
    var atkCard = botStrongestCard(gs, attacker.hand);
    if (atkCard === null) continue;
    var atkStr = gs.cardStrength(atkCard);
    var defStr = gs.cardStrength(defender.kingCard);
    var success = (atkStr >= defStr);
    gs.kingAttackResolved(attackerIdx, di, success, [atkCard], false);
    return true;
  }
  return false;
}

function botTryDefAttack(gs, attackerIdx) {
  var attacker = gs.players[attackerIdx];
  if (attacker.hand.length < 1) return false;
  for (var di = 0; di < gs.players.length; di++) {
    if (di === attackerIdx) continue;
    var defender = gs.players[di];
    if (defender.isOut) continue;
    for (var slot = 1; slot <= 3; slot++) {
      var defCardId = defender.defCards[slot];
      if (defCardId == null) continue;
      var defStr = gs.cardStrength(defCardId);
      // Find a hand card strong enough to beat it
      var atkCard = botStrongestCard(gs, attacker.hand);
      if (atkCard === null) continue;
      var atkStr = gs.cardStrength(atkCard);
      if (atkStr >= defStr) {
        gs.defAttackResolved(attackerIdx, di, slot, 0, true, [atkCard], false, []);
        return true;
      }
    }
  }
  return false;
}

function botTryPlunder(gs, attackerIdx) {
  var attacker = gs.players[attackerIdx];
  if (attacker.hand.length < 1 || (attacker.pickingDeckAttacks || 0) <= 0) return false;
  // Pick the deck with the weaker top card
  var bestDeck = -1;
  var bestDefStr = 9999;
  for (var d = 0; d < gs.pickingDecks.length; d++) {
    var deck = gs.pickingDecks[d];
    if (deck.length === 0) continue;
    var topCard = deck[deck.length - 1];
    var str = gs.cardStrength(topCard.id);
    if (str < bestDefStr) { bestDefStr = str; bestDeck = d; }
  }
  if (bestDeck === -1) return false;
  var atkCard = botStrongestCard(gs, attacker.hand);
  if (atkCard === null) return false;
  var atkStr = gs.cardStrength(atkCard);
  var success = (atkStr >= bestDefStr);
  // Call preview first (locks cards), then resolve
  gs.setPlunderPreview({ attackerIdx: attackerIdx, deckIndex: bestDeck, attackCardIds: [atkCard], attackingSymbol: 'none', attackingSymbol2: 'none' });
  gs.plunderResolved(attackerIdx, bestDeck, success, [atkCard], false, []);
  return true;
}

function botStrongestCard(gs, hand) {
  if (!hand || hand.length === 0) return null;
  var best = hand[0];
  var bestStr = gs.cardStrength(hand[0]);
  for (var i = 1; i < hand.length; i++) {
    var s = gs.cardStrength(hand[i]);
    if (s > bestStr) { bestStr = s; best = hand[i]; }
  }
  return best;
}

function botWeakestCard(gs, hand) {
  if (!hand || hand.length === 0) return null;
  var best = hand[0];
  var bestStr = gs.cardStrength(hand[0]);
  for (var i = 1; i < hand.length; i++) {
    var s = gs.cardStrength(hand[i]);
    if (s < bestStr) { bestStr = s; best = hand[i]; }
  }
  return best;
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
    io.to(sess.id).emit('getUsers', sess.users);
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
    io.to(sess.id).emit('getUsers', sess.users);
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
    io.to(sess.id).emit('getUsers', sess.users);
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
    playBotTurnIfNeeded(sess);
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
    playBotTurnIfNeeded(sess);
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
  });
});
