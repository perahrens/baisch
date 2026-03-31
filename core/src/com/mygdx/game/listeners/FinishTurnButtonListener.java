package com.mygdx.game.listeners;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameState;
import io.socket.client.Socket;

public class FinishTurnButtonListener extends ClickListener {

  GameState gameState;
  Socket socket;

  public FinishTurnButtonListener(GameState gameState, Socket socket) {
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
    gameState.setUpdateState(true);
  };
}
