var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);
var players = [];

server.listen(8082, function() {
  console.log("Server is now running... ");
});

io.on('connection', function(socket) {
  console.log("Player Connected");
  socket.emit('socketID', { id: socket.id });
  socket.broadcast.emit('newPlayer', { id: socket.id });
  socket.on('disconnect', function() {
    console.log("Player Disconnected");
    socket.broadcast.emit('playerDisconnected', { id:socket.id } );
    for (var i = 0; i < players.length; i++) {
      if (players[i].id == socket.id) {
        players.splice(i);
      }
    }
    socket.broadcast.emit('getPlayers', players);
  });
  players.push(new player(socket.id));
  socket.emit('getPlayers', players);
  socket.broadcast.emit('getPlayers', players);
});

function player(id) {
  this.id = id;
}