package com.mygdx.game.listeners;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
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
    Player nextPlayer = gameState.getNextPlayer();
    System.out.println("Next player " + nextPlayer.getPlayerName());
    int nextPlayerIndex = gameState.getCurrentPlayerIndex();
    try {
      JSONObject data = new JSONObject();
      data.put("nextPlayerIndex", nextPlayerIndex);
      socket.emit("finishTurn", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    gameState.setUpdateState(true);
  };
}
