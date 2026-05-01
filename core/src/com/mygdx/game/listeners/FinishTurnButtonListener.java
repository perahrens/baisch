package com.mygdx.game.listeners;

import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameScreen;
import com.mygdx.game.GameState;
import com.mygdx.game.net.SocketClient;

public class FinishTurnButtonListener extends ClickListener {

  GameState gameState;
  SocketClient socket;

  public FinishTurnButtonListener(GameState gameState, SocketClient socket) {
    this.gameState = gameState;
    this.socket = socket;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    // Guard against double-emit: flag lives on PlayerTurn so it survives show() rebuilds.
    if (gameState.getCurrentPlayer().getPlayerTurn().isFinishTurnEmitted()) return;
    gameState.getCurrentPlayer().getPlayerTurn().setFinishTurnEmitted(true);
    GameScreen.finishTurnSentAt = System.currentTimeMillis();
    try {
      JSONObject data = new JSONObject();
      data.put("currentPlayerIndex", gameState.getCurrentPlayerIndex());
      data.put("clientSentAt", System.currentTimeMillis());
      socket.emit("finishTurn", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    // Do NOT call setUpdateState(true) here. The server will respond with a stateUpdate
    // that triggers the re-render.
  };
}
