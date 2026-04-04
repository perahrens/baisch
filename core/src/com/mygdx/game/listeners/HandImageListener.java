package com.mygdx.game.listeners;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Hero;

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
      boolean shouldTake = (defCards.containsKey(j) && defCards.get(j).isSelected())
          || (topDefCards.containsKey(j) && topDefCards.get(j).isSelected());
      if (shouldTake && player.canMobilize()) {
        emitTakeDefCard(j);
        player.takeDefCard(j);
        blinkMajor();
      }
    }
    gameState.setUpdateState(true);
  };

  private void blinkMajor() {
    for (int i = 0; i < player.getHeroes().size(); i++) {
      if (player.getHeroes().get(i).getHeroName() == "Marshal") {
        Hero h = player.getHeroes().get(i);
        h.addAction(Actions.sequence(
            Actions.color(Color.GREEN, 0f),
            Actions.delay(0.3f),
            Actions.color(Color.WHITE, 0.2f)
        ));
        break;
      }
    }
  }

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
