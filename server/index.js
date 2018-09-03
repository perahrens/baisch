var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);

var users = [];
var timeToStart;
var timer;

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
      console.log("Seconds left ... " + timeToStart)
      if (timeToStart == 0) {
        console.log("Timer finished, broadcast to users");
        socket.emit('startGame', {});
        socket.broadcast.emit('startGame', {});
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
  
  users.push(new user(socket.id));
  socket.emit('getUsers', users);
  socket.broadcast.emit('getUsers', users);
});

function user(id) {
  this.id = id;
  this.isReady = false;
}
