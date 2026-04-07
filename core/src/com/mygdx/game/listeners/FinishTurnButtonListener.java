package com.mygdx.game.listeners;

import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameState;
import com.mygdx.game.net.SocketClient;

public class FinishTurnButtonListener extends ClickListener {

  GameState gameState;
  SocketClient socket;

  public FinishTurnButtonListener(GameState gameState, SocketClient socket) {
    this.gameState = gameState;
    this.socket = socket;
  }

  private boolean fired = false;

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (fired) return;
    fired = true;
    try {
      JSONObject data = new JSONObject();
      data.put("currentPlayerIndex", gameState.getCurrentPlayerIndex());
      socket.emit("finishTurn", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    // Do NOT call setUpdateState(true) here. The server will respond with a stateUpdate
    // that triggers the re-render. Calling it prematurely rebuilds the button with a
    // fresh fired=false listener before the server responds, creating a second-tap window.
  };
}
