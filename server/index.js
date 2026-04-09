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

function createSession(name) {
  var id = 's' + (_nextSessionId++);
  sessions[id] = {
    id: id,
    name: name,
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

function makeUser(id, name) {
  return { id: id, name: name || 'Player', isReady: false };
}

function checkAndHandleWinner(sess) {
  if (!sess.gameState || sess.winnerHandled) return;
  const winner = sess.gameState.checkWinner();
  if (winner >= 0) {
    sess.winnerHandled = true;
    console.log("Session " + sess.id + " winner: player " + winner + " — returning to lobby in 5 seconds");
    setTimeout(function() {
      sess.winnerHandled = false;
      sess.gameState = null;
      sess.heroSelections = {};
      sess.users.forEach(function(u) { u.isReady = false; });
      io.to(sess.id).emit('returnToLobby');
      sess.spectators = [];
      io.to(sess.id).emit('getUsers', sess.users);
      broadcastSessionList();
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
}

var PORT = process.env.PORT || 8082;
server.listen(PORT, function() {
  console.log("Server is now running on port " + PORT);
});

io.on('connection', function(socket) {
  console.log("User Connected: " + socket.id);
  socket.emit('socketID', { id: socket.id });
  socket.emit('sessionList', getSessionList());

  socket.on('disconnect', function() {
    console.log("User Disconnected: " + socket.id);
    leaveCurrentSession(socket);
  });

  // ── Session management events ────────────────────────────────────────────

  socket.on('leaveSession', function() {
    console.log('User ' + socket.id + ' left session');
    leaveCurrentSession(socket);
  });

  socket.on('createSession', function(data) {
    leaveCurrentSession(socket); // clean up any previous session first
    var name = (data && data.name) ? String(data.name).slice(0, 30) : 'Player';
    var sessionName = (data && data.sessionName) ? String(data.sessionName).slice(0, 50) : name + "'s game";
    var sess = createSession(sessionName);
    sess.users.push(makeUser(socket.id, name));
    socketToSession[socket.id] = sess.id;
    socket.join(sess.id);
    console.log("Session created: " + sess.id + " '" + sess.name + "' by " + name);
    socket.emit('sessionJoined', { sessionId: sess.id });
    io.to(sess.id).emit('getUsers', sess.users);
    socket.emit('gameStatus', { running: false });
    broadcastSessionList();
  });

  socket.on('joinSession', function(data) {
    if (!data || !data.sessionId) return;
    var sess = sessions[data.sessionId];
    if (!sess) { socket.emit('sessionNotFound'); return; }
    if (sess.gameState !== null) { socket.emit('gameStatus', { running: true }); return; }
    leaveCurrentSession(socket); // leave any previous session first
    sess = sessions[data.sessionId]; // re-fetch — leaveCurrentSession may have deleted a different session
    var name = (data.name) ? String(data.name).slice(0, 30) : 'Player';
    var existing = sess.users.find(function(u) { return u.id === socket.id; });
    if (!existing) sess.users.push(makeUser(socket.id, name));
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
    socket.emit('sessionJoined', { sessionId: sess.id });
    io.to(sess.id).emit('getUsers', sess.users);
    socket.emit('gameStatus', { running: false });
    broadcastSessionList();
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
          clearInterval(sess.timer);
        }
      }
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
    if (sess.users.length < 2) {
      console.log("Not enough players to start (need at least 2)");
      return;
    }
    sess.timeToStart = seconds;
    clearInterval(sess.timer);
    sess.timer = setInterval(function() {
      sess.timeToStart--;
      io.to(sess.id).emit('updateTimer', { seconds: sess.timeToStart });
      console.log("Session " + sess.id + " seconds left: " + sess.timeToStart);
      if (sess.timeToStart === 0) {
        console.log("Timer finished for session " + sess.id + ", starting game");
        sess.winnerHandled = false;
        sess.gameState = new GameState(sess.users);
        sess.users.forEach(function(u, idx) {
          io.to(u.id).emit('gameState', {
            playerIndex: idx,
            gameState: sess.gameState.serialize()
          });
        });
        clearInterval(sess.timer);
        broadcastSessionList();
      }
    }, 1000);
  });

  socket.on('stopTimer', function() {
    var sess = getSession(socket.id);
    if (!sess) return;
    if (sess.timeToStart <= 0) {
      console.log("Timer stopped for session " + sess.id);
      clearInterval(sess.timer);
    }
  });

  // Client requests full state resync
  socket.on('requestStateSync', function() {
    var sess = getSession(socket.id);
    if (sess && sess.gameState) {
      socket.emit('stateUpdate', sess.gameState.serialize());
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

  socket.on('jokerSacrifice', function(data) {
    var sess = getSession(socket.id);
    if (!sess || !sess.gameState) return;
    console.log("jokerSacrifice: playerIdx=" + data.playerIdx);
    sess.gameState.jokerSacrifice(data.playerIdx, data.jokerCardId, data.drawnCardId);
    io.to(sess.id).emit('stateUpdate', sess.gameState.serialize());
  });

  socket.on('heroAcquired', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    console.log("heroAcquired: playerIndex=" + data.playerIndex + " heroName=" + data.heroName);
    socket.to(sess.id).emit('heroAcquired', data);
  });

  socket.on('heroLost', function(data) {
    var sess = getSession(socket.id);
    if (!sess) return;
    console.log("heroLost: playerIndex=" + data.playerIndex + " heroName=" + data.heroName);
    socket.to(sess.id).emit('heroLost', data);
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
    if (!sess) return;
    socket.to(sess.id).emit('spyFlip', data);
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
