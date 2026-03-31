var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);


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

server.listen(8082, function() {
  console.log("Server is now running... ");
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

  socket.on('plunderResolved', function(data) {
    console.log("plunderResolved: attackerIdx=" + data.attackerIdx + " deckIndex=" + data.deckIndex + " success=" + data.success);
    gameState.plunderResolved(data.attackerIdx, data.deckIndex, data.success, data.attackCardIds || [], data.kingUsed || false);
    io.emit('stateUpdate', gameState.serialize());
    checkAndHandleWinner(io);
  });

  socket.on('defAttackResolved', function(data) {
    console.log("defAttackResolved: attackerIdx=" + data.attackerIdx + " targetPlayerIdx=" + data.targetPlayerIdx + " success=" + data.success);
    gameState.defAttackResolved(data.attackerIdx, data.targetPlayerIdx, data.positionId, data.level, data.success, data.attackCardIds || [], data.kingUsed || false);
    io.emit('stateUpdate', gameState.serialize());
    checkAndHandleWinner(io);
  });

  socket.on('kingAttackResolved', function(data) {
    console.log("kingAttackResolved: attackerIdx=" + data.attackerIdx + " defenderIdx=" + data.defenderIdx + " success=" + data.success);
    gameState.kingAttackResolved(data.attackerIdx, data.defenderIdx, data.success, data.attackCardIds || [], data.kingUsed || false);
    io.emit('stateUpdate', gameState.serialize());
    checkAndHandleWinner(io);
  });
  
  users.push(new user(socket.id));
  socket.emit('getUsers', users);
  socket.broadcast.emit('getUsers', users);
});

function user(id) {
  this.id = id;
  this.isReady = false;
}
