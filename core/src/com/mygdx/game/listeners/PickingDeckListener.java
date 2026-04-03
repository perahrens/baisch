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

      boolean kingSelected = currentPlayer.getKingCard() != null && currentPlayer.getKingCard().isSelected();
      boolean hasHandCards = currentPlayer.getSelectedHandCards().size() > 0;

      if (kingSelected || hasHandCards) {
        // King can only be used once per turn
        if (kingSelected && pt.isKingUsedThisTurn()) return;
        // King can only be used when the player has no defense cards
        if (kingSelected && (!currentPlayer.getDefCards().isEmpty() || !currentPlayer.getTopDefCards().isEmpty())) return;

        // Symbol constraint — joker can always attack regardless of symbol
        String attackSymbol = kingSelected ? currentPlayer.getKingCard().getSymbol()
            : currentPlayer.getSelectedHandCards().get(0).getSymbol();
        if (!"joker".equals(attackSymbol)) {
          if (pt.getAttackingSymbol()[0] != "none"
              && pt.getAttackingSymbol()[0] != attackSymbol
              && pt.getAttackingSymbol()[1] != attackSymbol) return;
        }

        int attackSum;
        ArrayList<Card> snapshot;

        if (kingSelected) {
          attackSum = currentPlayer.getKingCard().getStrength();
          snapshot = new ArrayList<Card>();
          pt.setKingUsed(true);
          pt.setAttackingSymbol(attackSymbol, currentPlayer.hasHero("Lieutenant"));
        } else {
          snapshot = new ArrayList<Card>(currentPlayer.getSelectedHandCards());
          attackSum = 0;
          for (Card c : snapshot) {
            attackSum += c.getStrength();
          }
          pt.setKingUsed(false);
          pt.setAttackingSymbol(attackSymbol, currentPlayer.hasHero("Lieutenant"));
        }

        pt.setPendingAttackCards(snapshot);
        pt.setPendingPickingDeckIndex(deckIndex);

        // Reveal the top card of this harvest deck
        ArrayList<Card> pickingCards = thisPickingDeck.getCards();
        Card topCard = pickingCards.get(pickingCards.size() - 1);
        topCard.setCovered(false);

        System.out.println("Attack with " + attackSum + " defense is " + topCard.getStrength());

        pt.decreasePickingDeckAttacks();
        pt.setPlunderPending(true);
        // Joker on top of harvest deck defends with infinite+1 (1000) — unbeatable
        int defStrength = "joker".equals(topCard.getSymbol()) ? 1000 : topCard.getStrength();
        pt.setPlunderSuccess(attackSum > defStrength);

        gameState.setUpdateState(true);
      }
    } else {
      System.out.println("No more picking attacks allowed");
    }
  }
}

