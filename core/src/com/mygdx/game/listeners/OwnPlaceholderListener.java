package com.mygdx.game.listeners;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Hero;

public class OwnPlaceholderListener extends ClickListener {

  Card placeholderCard;
  Player player;
  GameState gameState;

  public OwnPlaceholderListener() {
  }

  public OwnPlaceholderListener(Card placeholderCard, Player player, GameState gameState) {
    this.placeholderCard = placeholderCard;
    this.player = player;
    this.gameState = gameState;
  }

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

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (player.getSelectedHandCards().size() == 1) {
      if (player.canMobilize()) {
        Card cardToPlace = player.getSelectedHandCards().get(0);
        int cardId = cardToPlace.getCardId();
        int positionId = placeholderCard.getPositionId();
        if (player.isSlotSabotaged(positionId)) {
          System.out.println("[OwnPlaceholderListener] slot " + positionId + " is blocked by saboteur");
          return;
        }
        player.putDefCard(positionId, 0);
        if (gameState.getSocket() != null && cardId > 0 && positionId >= 1 && positionId <= 3) {
          try {
            JSONObject payload = new JSONObject();
            payload.put("playerIdx", gameState.getPlayers().indexOf(player));
            payload.put("positionId", positionId);
            payload.put("cardId", cardId);
            gameState.getSocket().emit("putDefCard", payload);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        blinkMajor();
        gameState.setUpdateState(true);
      } else {
        System.out.println("no more put allowed");
      }
    } else {
      System.out.println("Select only one handcard");
    }
  }

}
