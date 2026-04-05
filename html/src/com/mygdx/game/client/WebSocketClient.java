package com.mygdx.game.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.net.SocketListener;
import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

/**
 * GWT/browser implementation of SocketClient using JSNI to call the
 * socket.io JavaScript client loaded via a <script> tag in index.html.
 */
public class WebSocketClient implements SocketClient {

  private JavaScriptObject jsSocket;

  public WebSocketClient(String url) {
    // autoConnect:false so the socket only connects after all listeners are
    // registered (connect() is called at end of configSocketEvents).
    jsSocket = nativeCreate(url);
  }

  private native JavaScriptObject nativeCreate(String url) /*-{
    return $wnd.io(url, { autoConnect: false });
  }-*/;

  @Override
  public void on(String event, final SocketListener listener) {
    nativeOn(jsSocket, event, listener);
  }

  private native void nativeOn(JavaScriptObject sock, String event,
      SocketListener listener) /*-{
    var self = this;
    sock.on(event, $entry(function(data) {
      var jsonStr = (data !== undefined && data !== null)
          ? JSON.stringify(data) : null;
      self.@com.mygdx.game.client.WebSocketClient::callListener(
          Lcom/mygdx/game/net/SocketListener;Ljava/lang/String;)(listener, jsonStr);
    }));
  }-*/;

  /** Bridge method so JSNI can invoke the listener with parsed JSON. */
  private void callListener(SocketListener listener, String jsonStr) {
    try {
      if (jsonStr == null || jsonStr.isEmpty()) {
        listener.call();
        return;
      }
      if (jsonStr.charAt(0) == '[') {
        listener.call(JSONArray.parse(jsonStr));
      } else {
        listener.call(JSONObject.parse(jsonStr));
      }
    } catch (JSONException e) {
      listener.call();
    }
  }

  @Override
  public void emit(String event, Object data) {
    String jsonStr = (data != null) ? data.toString() : "null";
    nativeEmit(jsSocket, event, jsonStr);
  }

  private native void nativeEmit(JavaScriptObject sock, String event,
      String jsonStr) /*-{
    var obj;
    if (jsonStr === 'null') {
      obj = null;
    } else {
      try { obj = JSON.parse(jsonStr); } catch (e) { obj = jsonStr; }
    }
    sock.emit(event, obj);
  }-*/;

  @Override
  public void connect() {
    nativeDoConnect(jsSocket);
  }

  private native void nativeDoConnect(JavaScriptObject sock) /*-{
    sock.connect();
  }-*/;
}
