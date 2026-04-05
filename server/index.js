var path = require('path');
var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server, { origins: '*:*' });

app.use(require('express').static(path.join(__dirname, 'public')));


var users = [];
var timeToStart;
var timer;
var GameState = require('./gameState');
var gameState = null;
var winnerHandled = false;

function checkAndHandleWinner(io) {
  if (!gameState || winnerHandled) return;
  const winner = gameState.checkWinner();
  if (winner >= 0) {
    winnerHandled = true;
    console.log("Winner found: player " + winner + " — restarting in 5 seconds");
    // stateUpdate with winnerIndex already broadcast by the caller; schedule restart
    setTimeout(function() {
      winnerHandled = false;
      gameState = new GameState(users);
      users.forEach(function(user, idx) {
        io.to(user.id).emit('gameState', {
          playerIndex: idx,
          gameState: gameState.serialize()
        });
      });
    }, 5000);
  }
}

var PORT = process.env.PORT || 8082;
server.listen(PORT, function() {
  console.log("Server is now running on port " + PORT);
});

io.on('connection', function(socket) {
  console.log("User Connected");
  socket.emit('socketID', { id: socket.id });
  socket.broadcast.emit('newUser', { id: socket.id });

  socket.on('disconnect', function() {
    console.log("User Disconnected");
    socket.broadcast.emit('userDisconnected', { id:socket.id } );
    for (var i = 0; i < users.length; i++) {
      if (users[i].id == socket.id) {
        users.splice(i, 1);
      }
    }
    socket.broadcast.emit('getUsers', users);
  });

  socket.on('setUserReady', function(id) {
    console.log("Set User Ready" + id);
    //socket.emit('userReady', { id:socket.id } );
    //socket.broadcast.emit('userReady', { id:socket.id } );
    for (var i = 0; i < users.length; i++) {
      if (users[i].id == id) {
        if (users[i].isReady == false) {
          users[i].isReady = true;
        } else {
          users[i].isReady = false;
          clearInterval(timer);
        }
      }
    }
    socket.emit('getUsers', users);
    socket.broadcast.emit('getUsers', users);
  });
  
  socket.on('startTimer', function(seconds) {
    console.log("Start Timer");
    timeToStart = seconds;
    clearInterval(timer);
    timer = setInterval(function() {
      timeToStart--;
      socket.emit('updateTimer', { seconds: timeToStart });
      socket.broadcast.emit('updateTimer', { seconds: timeToStart });
      console.log("Seconds left ... " + timeToStart)
      if (timeToStart == 0) {
        console.log("Timer finished, broadcast to users");
        winnerHandled = false;
        gameState = new GameState(users);
        users.forEach((user, idx) => {
          io.to(user.id).emit('gameState', {
            playerIndex: idx,
            gameState: gameState.serialize()
          });
        });
        clearInterval(timer);
      }
    }, 1000);
  });
  
  socket.on('stopTimer', function(none) {
    if (timeToStart <= 0) {
      console.log("Timer stopped!");
      clearInterval(timer);
    }
  });

  socket.on('finishTurn', function(data) {
    console.log("Turn finished by player index: " + data.currentPlayerIndex);
    gameState.finishTurn(data.currentPlayerIndex);
    io.emit('stateUpdate', gameState.serialize());
  });

  socket.on('putDefCard', function(data) {
    console.log("putDefCard: playerIdx=" + data.playerIdx + " pos=" + data.positionId + " cardId=" + data.cardId);
    gameState.putDefCard(data.playerIdx, data.positionId, data.cardId);
    io.emit('stateUpdate', gameState.serialize());
  });

  socket.on('takeDefCard', function(data) {
    console.log("takeDefCard: playerIdx=" + data.playerIdx + " pos=" + data.positionId);
    gameState.takeDefCard(data.playerIdx, data.positionId);
    io.emit('stateUpdate', gameState.serialize());
  });

  socket.on('addToCemetery', function(data) {
    console.log("addToCemetery: playerIdx=" + data.playerIdx + " cardIds=" + data.cardIds + " draw=" + data.drawFromDeck);
    gameState.addToCemetery(data.playerIdx, data.cardIds || [], data.drawFromDeck || 0);
    io.emit('stateUpdate', gameState.serialize());
  });

  socket.on('discardDefCards', function(data) {
    console.log("discardDefCards: playerIdx=" + data.playerIdx);
    gameState.discardDefCards(data.playerIdx, data.slots || []);
    io.emit('stateUpdate', gameState.serialize());
  });

  socket.on('plunderResolved', function(data) {
    console.log("plunderResolved: attackerIdx=" + data.attackerIdx + " deckIndex=" + data.deckIndex + " success=" + data.success);
    gameState.plunderResolved(data.attackerIdx, data.deckIndex, data.success, data.attackCardIds || [], data.kingUsed || false, data.attackerOwnDefCardIds || []);
    io.emit('stateUpdate', gameState.serialize());
    checkAndHandleWinner(io);
  });

  socket.on('defAttackResolved', function(data) {
    console.log("defAttackResolved: attackerIdx=" + data.attackerIdx + " targetPlayerIdx=" + data.targetPlayerIdx + " success=" + data.success);
    gameState.defAttackResolved(data.attackerIdx, data.targetPlayerIdx, data.positionId, data.level, data.success, data.attackCardIds || [], data.kingUsed || false, data.attackerOwnDefCardIds || []);
    io.emit('stateUpdate', gameState.serialize());
    checkAndHandleWinner(io);
  });

  socket.on('kingAttackResolved', function(data) {
    console.log("kingAttackResolved: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " success=" + data.success);
    gameState.kingAttackResolved(data.attackerIdx, data.defenderIdx, data.success, data.attackCardIds || [], data.kingUsed || false);
    io.emit('stateUpdate', gameState.serialize());
    checkAndHandleWinner(io);
  });

  socket.on('jokerSacrifice', function(data) {
    console.log("jokerSacrifice: playerIdx=" + data.playerIdx);
    gameState.jokerSacrifice(data.playerIdx, data.jokerCardId, data.drawnCardId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Relay hero acquisition to all OTHER clients so they can update their local state.
  socket.on('heroAcquired', function(data) {
    console.log("heroAcquired: playerIndex=" + data.playerIndex + " heroName=" + data.heroName);
    socket.broadcast.emit('heroAcquired', data);
  });

  // Relay mercenary defense boost to all OTHER clients (boost is client-side only, not in server state).
  socket.on('mercDefBoost', function(data) {
    socket.broadcast.emit('mercDefBoost', data);
  });

  // Relay Reservists ready count to all OTHER clients.
  socket.on('reservistsKingBoost', function(data) {
    socket.broadcast.emit('reservistsKingBoost', data);
  });

  // Warlord: swap king card with a hand card (costs 1 take + 1 put).
  socket.on('warlordKingSwap', function(data) {
    gameState.warlordKingSwap(data.playerIdx, data.oldKingCardId, data.newKingCardId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Merchant: player discards a card and draws a replacement (1st draw, face-down to others).
  socket.on('merchantTrade', function(data) {
    gameState.merchantTrade(data.playerIdx, data.discardedCardId, data.drawnCardId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Merchant 2nd try: player discards 1st drawn card, draws once more (2nd draw revealed to all).
  socket.on('merchantSecondTry', function(data) {
    gameState.merchantSecondTry(data.playerIdx, data.firstCardId, data.secondCardId, data.isJoker);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Fortified Tower: stack a hand card on top of an existing defense card.
  socket.on('fortifiedTowerStack', function(data) {
    gameState.putTopDefCard(data.playerIdx, data.slot, data.cardId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Magician: discard enemy defense card(s) and replace with a new deck card.
  socket.on('magicianSwap', function(data) {
    gameState.magicianSwap(data.playerIdx, data.targetPlayerIdx, data.positionId,
        data.bottomCardId, data.bottomCovered, data.topCardId, data.topCovered);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Relay spy flip to all OTHER clients.
  socket.on('spyFlip', function(data) {
    socket.broadcast.emit('spyFlip', data);
  });

  // Battery Tower: relay attack intercept and responses between attacker and defender.
  socket.on('batteryDefenseCheck', function(data) {
    socket.broadcast.emit('batteryDefenseCheck', data);
  });
  socket.on('batteryAllowAttack', function(data) {
    socket.broadcast.emit('batteryAllowAttack', data);
  });
  socket.on('batteryDenyAttack', function(data) {
    socket.broadcast.emit('batteryDenyAttack', data);
  });

  // Priest: attacker takes a matching card from an enemy's hand.
  socket.on('priestConvert', function(data) {
    console.log("priestConvert: attackerIdx=" + data.attackerIdx + " targetIdx=" + data.targetPlayerIdx + " cardId=" + data.cardId);
    gameState.priestConvert(data.attackerIdx, data.targetPlayerIdx, data.cardId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Saboteurs: place a saboteur on an enemy defense slot (card or empty field).
  socket.on('sabotage', function(data) {
    console.log("sabotage: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " pos=" + data.positionId);
    gameState.sabotage(data.attackerIdx, data.defenderIdx, data.positionId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Saboteurs: owner recalls a saboteur from an enemy slot.
  socket.on('sabotageCallback', function(data) {
    console.log("sabotageCallback: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " pos=" + data.positionId);
    gameState.sabotageCallback(data.attackerIdx, data.defenderIdx, data.positionId);
    io.emit('stateUpdate', gameState.serialize());
  });

  // Saboteurs: defender sacrifices their defense card to destroy the saboteur.
  socket.on('sabotageSacrifice', function(data) {
    console.log("sabotageSacrifice: defenderIdx=" + data.defenderIdx + " pos=" + data.positionId);
    const attackerIdx = gameState.sabotageSacrifice(data.defenderIdx, data.positionId);
    if (attackerIdx !== undefined && users[attackerIdx]) {
      io.to(users[attackerIdx].id).emit('saboteurDestroyed', { attackerIdx: attackerIdx });
    }
    io.emit('stateUpdate', gameState.serialize());
  });

  // Saboteurs: defender sacrifices a hand card to destroy a saboteur on an empty slot.
  socket.on('sabotageEmptySlotSacrifice', function(data) {
    console.log("sabotageEmptySlotSacrifice: defenderIdx=" + data.defenderIdx + " pos=" + data.positionId + " card=" + data.handCardId);
    const attackerIdx = gameState.sabotageEmptySlotSacrifice(data.defenderIdx, data.positionId, data.handCardId);
    if (attackerIdx !== undefined && users[attackerIdx]) {
      io.to(users[attackerIdx].id).emit('saboteurDestroyed', { attackerIdx: attackerIdx });
    }
    io.emit('stateUpdate', gameState.serialize());
  });

  users.push(new user(socket.id));
  socket.emit('getUsers', users);
  socket.broadcast.emit('getUsers', users);
});

function user(id) {
  this.id = id;
  this.isReady = false;
}
