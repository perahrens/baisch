package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Iterator;

import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.HeroesSquare;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Hero;

public class CemeteryDeckListener extends ClickListener {

  GameState gameState;

  public CemeteryDeckListener(GameState gameState) {
    this.gameState = gameState;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    System.out.println("Cemetery");
    Player currentPlayer = gameState.getCurrentPlayer();
    CardDeck cardDeck = gameState.getCardDeck();
    CardDeck cemeteryDeck = gameState.getCemeteryDeck();
    HeroesSquare heroesSquare = gameState.getHeroesSquare();
    if (currentPlayer.getSelectedHandCards().size() > 0) {
      Iterator<Card> handCardIt = currentPlayer.getHandCards().iterator();
      while (handCardIt.hasNext()) {
        Card currCard = handCardIt.next();
        if (currCard.isSelected()) {
          System.out.println("Remove handcard " + currCard.getStrength());
          cemeteryDeck.addCard(currCard);
          boolean isJoker = "joker".equals(currCard.getSymbol());
          int drawFromDeck = 0;
          if (isJoker) {
            System.out.println("Get hero");
            Card heroCard = cardDeck.getCard(cemeteryDeck);
            System.out.println("Hero card is " + heroCard.getStrength());
            Hero hero = heroesSquare.getHero(heroCard.getStrength());
            if (hero != null) {
              currentPlayer.addHero(hero);
            }
            cemeteryDeck.addCard(heroCard);
            drawFromDeck = 1;
          }
          handCardIt.remove();
          if (gameState.getSocket() != null) {
            try {
              JSONObject payload = new JSONObject();
              payload.put("playerIdx", gameState.getPlayers().indexOf(currentPlayer));
              JSONArray cardIds = new JSONArray();
              cardIds.put(currCard.getCardId());
              payload.put("cardIds", cardIds);
              payload.put("drawFromDeck", drawFromDeck);
              gameState.getSocket().emit("addToCemetery", payload);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          gameState.setUpdateState(true);
        }
      }
    } else if (currentPlayer.getSelectedDefCards().size() > 0) {
      ArrayList<Card> selDef = currentPlayer.getSelectedDefCards();
      JSONArray slotsJson = new JSONArray();
      for (int i = 0; i < selDef.size(); i++) {
        Card c = selDef.get(i);
        int slot = c.getPositionId();
        boolean isTop = c.getLevel() == 1;
        cemeteryDeck.addCard(c);
        if (isTop) {
          currentPlayer.getTopDefCards().remove(slot);
        } else {
          currentPlayer.getDefCards().remove(slot);
        }
        try {
          JSONObject slotObj = new JSONObject();
          slotObj.put("slot", slot);
          slotObj.put("isTop", isTop);
          slotsJson.put(slotObj);
        } catch (Exception ex) { ex.printStackTrace(); }
      }
      if (gameState.getSocket() != null) {
        try {
          JSONObject payload = new JSONObject();
          payload.put("playerIdx", gameState.getPlayers().indexOf(currentPlayer));
          payload.put("slots", slotsJson);
          gameState.getSocket().emit("discardDefCards", payload);
        } catch (Exception e) { e.printStackTrace(); }
      }
      gameState.setUpdateState(true);
    }
  }

}
