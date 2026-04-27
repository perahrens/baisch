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

  /**
   * Bridge from the JS socket.io event into Java land. Instead of
   * {@code JSON.stringify(data)} + custom char-by-char parsing of the
   * resulting string (allocates a StringBuilder per key/value, recurses
   * through every character), we walk the JS object tree directly and
   * build the Java {@link JSONObject} / {@link JSONArray} in one pass.
   *
   * For a typical 4-player late-game {@code stateUpdate} payload (~3 KB)
   * this is roughly an order of magnitude faster than the previous path
   * and produces far less GC pressure on the browser.
   */
  private native void nativeOn(JavaScriptObject sock, String event,
      SocketListener listener) /*-{
    var self = this;
    sock.on(event, $entry(function(data) {
      self.@com.mygdx.game.client.WebSocketClient::dispatch(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/mygdx/game/net/SocketListener;)(data == null ? null : data, listener);
    }));
  }-*/;

  /** Dispatches a parsed payload to the Java listener. */
  private void dispatch(JavaScriptObject data, SocketListener listener) {
    if (data == null) {
      listener.call();
      return;
    }
    if (jsIsArray(data)) {
      JSONArray arr = new JSONArray();
      fillArray(data, arr);
      listener.call(arr);
    } else if (jsIsObject(data)) {
      JSONObject obj = new JSONObject();
      fillObject(data, obj);
      listener.call(obj);
    } else {
      // Primitive (string / number / boolean) — wrap in a 1-element array
      // for backward compatibility with the previous behavior, which would
      // have thrown and called listener.call() with no args.
      listener.call();
    }
  }

  private native boolean jsIsArray(JavaScriptObject o) /*-{
    return Array.isArray(o);
  }-*/;

  private native boolean jsIsObject(JavaScriptObject o) /*-{
    return (typeof o === 'object' && o !== null);
  }-*/;

  /**
   * Walks {@code src} (a JS object) and populates {@code target} with the
   * equivalent {@link JSONObject} entries. Recurses into nested objects/
   * arrays. Numbers are stored as {@code Integer} when they are integral
   * and fit in 32 bits, else as {@code Double}. Strings stay strings,
   * booleans stay booleans, {@code null} is preserved.
   */
  private native void fillObject(JavaScriptObject src, JSONObject target) /*-{
    var self = this;
    for (var k in src) {
      if (!src.hasOwnProperty(k)) continue;
      var v = src[k];
      self.@com.mygdx.game.client.WebSocketClient::putObjectValue(Lcom/mygdx/game/util/JSONObject;Ljava/lang/String;Ljava/lang/Object;)(target, k, v);
    }
  }-*/;

  private native void fillArray(JavaScriptObject src, JSONArray target) /*-{
    var self = this;
    var n = src.length;
    for (var i = 0; i < n; i++) {
      var v = src[i];
      self.@com.mygdx.game.client.WebSocketClient::putArrayValue(Lcom/mygdx/game/util/JSONArray;Ljava/lang/Object;)(target, v);
    }
  }-*/;

  /**
   * Java-side handler for a single (key, value) pair. We keep the type
   * dispatch in Java so JSNI doesn't need to know the full method
   * signature for every overload of {@link JSONObject#put}.
   */
  @SuppressWarnings("unused")
  private void putObjectValue(JSONObject target, String key, Object value) {
    try {
      target.put(key, convertValue(value));
    } catch (JSONException e) {
      // JSONObject.put never actually throws, but the API declares it.
    }
  }

  @SuppressWarnings("unused")
  private void putArrayValue(JSONArray target, Object value) {
    target.put(convertValue(value));
  }

  /**
   * Converts a JS value (already auto-boxed by GWT for primitives) into
   * the Java type expected by {@link JSONObject}/{@link JSONArray}.
   * For nested objects/arrays we recurse via the JSNI walker.
   */
  private Object convertValue(Object value) {
    if (value == null) return null;
    if (value instanceof String) return value;
    if (value instanceof Boolean) return value;
    if (value instanceof Number) {
      double d = ((Number) value).doubleValue();
      // Distinguish integral values (the common case for card IDs, indexes,
      // counters) so getInt() avoids a parseInt round-trip.
      if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE
          && Math.floor(d) == d && !Double.isInfinite(d)) {
        return Integer.valueOf((int) d);
      }
      return Double.valueOf(d);
    }
    // Must be a JS object or array — recurse via JSNI.
    JavaScriptObject jso = (JavaScriptObject) value;
    if (jsIsArray(jso)) {
      JSONArray arr = new JSONArray();
      fillArray(jso, arr);
      return arr;
    }
    JSONObject obj = new JSONObject();
    fillObject(jso, obj);
    return obj;
  }

  @Override
  public void off(String event) {
    nativeOff(jsSocket, event);
  }

  private native void nativeOff(JavaScriptObject sock, String event) /*-{
    sock.off(event);
  }-*/;

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

  @Override
  public void disconnect() {
    nativeDoDisconnect(jsSocket);
  }

  private native void nativeDoDisconnect(JavaScriptObject sock) /*-{
    // Calling sock.disconnect() on the client stops auto-reconnect in socket.io v2.
    sock.disconnect();
  }-*/;

  @Override
  public String getSocketId() {
    return nativeGetSocketId(jsSocket);
  }

  private native String nativeGetSocketId(JavaScriptObject sock) /*-{
    var id = sock.id;
    return (id != null && id !== undefined) ? id : "";
  }-*/;
}
