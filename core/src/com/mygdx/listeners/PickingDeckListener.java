package com.mygdx.listeners;

import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.PickingDeck;
import com.mygdx.game.Player;

public class PickingDeckListener extends ClickListener {

  GameState gameState;
  PickingDeck thisPickingDeck;
  PickingDeck otherPickingDeck;

  public PickingDeckListener(GameState gameState, PickingDeck thisPickingDeck, PickingDeck otherPickingDeck) {
    this.gameState = gameState;
    this.thisPickingDeck = thisPickingDeck;
    this.otherPickingDeck = otherPickingDeck;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    Player currentPlayer = gameState.getCurrentPlayer();

    if (currentPlayer.getPlayerTurn().getPickingDeckAttacks() > 0) {
      System.out.println("Selected handcards " + currentPlayer.getSelectedHandCards().size());
      if (currentPlayer.getSelectedHandCards().size() > 0) {
        if (currentPlayer.getPlayerTurn().getAttackingSymbol() == "none" || currentPlayer.getPlayerTurn()
            .getAttackingSymbol() == currentPlayer.getSelectedHandCards().get(0).getSymbol()) {
          currentPlayer.getPlayerTurn().decreasePickingDeckAttacks();
          currentPlayer.attackPickingDeck(thisPickingDeck, otherPickingDeck, gameState.getCardDeck(),
              gameState.getCemeteryDeck());
          Iterator<Card> handCardIt = currentPlayer.getHandCards().iterator();
          while (handCardIt.hasNext()) {
            Card currCard = handCardIt.next();
            if (currCard.isSelected()) {
              System.out.println("Remove handcard " + currCard.getStrength());
              currentPlayer.getPlayerTurn().setAttackingSymbol(currCard.getSymbol());
              gameState.getCemeteryDeck().addCard(currCard);
              handCardIt.remove();
            }
          }
        }
      }
    } else {
      System.out.println("No more picking attacks allowed");
    }
    //gameState.setUpdateState(true);
  }
}
