package com.mygdx.game;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.net.SocketListener;
import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONObject;

/**
 * Android implementation of SocketClient wrapping the socket.io-client Java library.
 * Identical to the desktop implementation — the same jar works on Android.
 */
public class SocketIoClient implements SocketClient {

  private final Socket socket;

  public SocketIoClient(String url) throws Exception {
    socket = IO.socket(url);
  }

  public void connect() {
    socket.connect();
  }

  @Override
  public void disconnect() {
    socket.disconnect();
  }

  @Override
  public void on(String event, final SocketListener listener) {
    socket.on(event, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
          if (args[i] instanceof org.json.JSONObject) {
            try {
              converted[i] = JSONObject.parse(args[i].toString());
            } catch (Exception e) {
              converted[i] = new JSONObject();
            }
          } else if (args[i] instanceof org.json.JSONArray) {
            try {
              converted[i] = JSONArray.parse(args[i].toString());
            } catch (Exception e) {
              converted[i] = new JSONArray();
            }
          } else {
            converted[i] = args[i];
          }
        }
        listener.call(converted);
      }
    });
  }

  @Override
  public void off(String event) {
    socket.off(event);
  }

  @Override
  public String getSocketId() {
    String id = socket.id();
    return id != null ? id : "";
  }

  @Override
  public void emit(String event, Object data) {
    if (data instanceof JSONObject) {
      try {
        socket.emit(event, new org.json.JSONObject(data.toString()));
      } catch (Exception e) {
        socket.emit(event, data.toString());
      }
    } else {
      socket.emit(event, data);
    }
  }
}
