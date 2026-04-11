package com.mygdx.game.net;

/**
 * Platform-agnostic socket abstraction.
 * Desktop implementation: SocketIoClient (io.socket under the hood).
 * GWT/HTML implementation: WebSocketClient (browser socket.io JS via JSNI).
 */
public interface SocketClient {
  void on(String event, SocketListener listener);
  void emit(String event, Object data);
  void connect();
  /** Cleanly disconnect and prevent auto-reconnect. */
  void disconnect();
}
