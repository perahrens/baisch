package com.mygdx.game.net;

/**
 * Platform-agnostic socket abstraction.
 * Desktop implementation: SocketIoClient (io.socket under the hood).
 * GWT/HTML implementation: WebSocketClient (browser socket.io JS via JSNI).
 */
public interface SocketClient {
  void on(String event, SocketListener listener);
  /** Remove all listeners for the given event. Safe to call from dispose(). */
  void off(String event);
  void emit(String event, Object data);
  void connect();
  /** Cleanly disconnect and prevent auto-reconnect. */
  void disconnect();
  /** Returns the current socket ID, or an empty string if not yet connected. */
  String getSocketId();
}
