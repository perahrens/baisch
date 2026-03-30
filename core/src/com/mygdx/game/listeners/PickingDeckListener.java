package com.mygdx.game.listeners;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.PickingDeck;
import com.mygdx.game.Player;
import com.mygdx.game.PlayerTurn;

public class PickingDeckListener extends ClickListener {

  GameState gameState;
  PickingDeck thisPickingDeck;
  int deckIndex;

  public PickingDeckListener(GameState gameState, PickingDeck thisPickingDeck, PickingDeck otherPickingDeck, int deckIndex) {
    this.gameState = gameState;
    this.thisPickingDeck = thisPickingDeck;
    this.deckIndex = deckIndex;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    Player currentPlayer = gameState.getCurrentPlayer();
    PlayerTurn pt = currentPlayer.getPlayerTurn();

    if (pt.isPlunderPending()) {
      // Confirmation is handled by the fullscreen overlay in GameScreen; ignore deck clicks.
      return;
    }

    if (pt.getPickingDeckAttacks() > 0) {
      System.out.println("Selected handcards " + currentPlayer.getSelectedHandCards().size());
      if (currentPlayer.getSelectedHandCards().size() > 0) {
        String selectedSymbol = currentPlayer.getSelectedHandCards().get(0).getSymbol();
        if (pt.getAttackingSymbol()[0] == "none"
            || pt.getAttackingSymbol()[0] == selectedSymbol
            || pt.getAttackingSymbol()[1] == selectedSymbol) {

          // Snapshot selected cards before the redraw deselects them
          ArrayList<Card> snapshot = new ArrayList<Card>(currentPlayer.getSelectedHandCards());
          pt.setPendingAttackCards(snapshot);
          pt.setPendingPickingDeckIndex(deckIndex);

          // Compute attack sum
          int attackSum = 0;
          for (Card c : snapshot) {
            attackSum += c.getStrength();
          }

          // Reveal the top card of this harvest deck
          ArrayList<Card> pickingCards = thisPickingDeck.getCards();
          Card topCard = pickingCards.get(pickingCards.size() - 1);
          topCard.setCovered(false);

          System.out.println("Attack with " + attackSum + " defense is " + topCard.getStrength());

          pt.decreasePickingDeckAttacks();
          pt.setAttackingSymbol(selectedSymbol, currentPlayer.hasHero("Lieutenant"));
          pt.setPlunderPending(true);
          pt.setPlunderSuccess(attackSum > topCard.getStrength());

          gameState.setUpdateState(true);
        }
      }
    } else {
      System.out.println("No more picking attacks allowed");
    }
  }
}

