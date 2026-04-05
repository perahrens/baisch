package com.mygdx.game.net;

/**
 * Callback interface for socket events.
 * args[0] is a com.mygdx.game.util.JSONObject (or JSONArray for getUsers events).
 */
public interface SocketListener {
  void call(Object... args);
}
