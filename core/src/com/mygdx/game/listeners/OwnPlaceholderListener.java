package com.mygdx.game.listeners;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;

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

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (player.getSelectedHandCards().size() == 1) {
      if (player.getPlayerTurn().getPutDefCard() > 0) {
        Card cardToPlace = player.getSelectedHandCards().get(0);
        int cardId = cardToPlace.getCardId();
        int positionId = placeholderCard.getPositionId();
        System.out.println("[OwnPlaceholderListener] placing cardId=" + cardId + " at pos=" + positionId);
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
        gameState.setUpdateState(true);
      } else {
        System.out.println("no more put allowed");
      }
    } else {
      System.out.println("Select only one handcard");
    }
  }

}
