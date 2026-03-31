package com.mygdx.game.listeners;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;

public class HandImageListener extends ClickListener {

  GameState gameState;
  Player player;

  public HandImageListener(GameState gameState, Player player) {
    this.gameState = gameState;
    this.player = player;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    Map<Integer, Card> defCards = player.getDefCards();
    Map<Integer, Card> topDefCards = player.getTopDefCards();
    for (int j = 1; j <= 3; j++) {
      if (defCards.containsKey(j) && defCards.get(j).isSelected()) {
        emitTakeDefCard(j);
        player.takeDefCard(j);
      }
      if (topDefCards.containsKey(j) && topDefCards.get(j).isSelected()) {
        emitTakeDefCard(j);
        player.takeDefCard(j);
      }
    }
    gameState.setUpdateState(true);
  };

  private void emitTakeDefCard(int positionId) {
    if (gameState.getSocket() == null) return;
    try {
      JSONObject payload = new JSONObject();
      payload.put("playerIdx", gameState.getCurrentPlayerIndex());
      payload.put("positionId", positionId);
      gameState.getSocket().emit("takeDefCard", payload);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

}
