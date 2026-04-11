package com.mygdx.game.listeners;

import java.util.ArrayList;

import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.PickingDeck;
import com.mygdx.game.Player;
import com.mygdx.game.PlayerTurn;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Mercenaries;

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
      boolean hasDefCards = currentPlayer.hasHero("Banneret") && !currentPlayer.getSelectedDefCards().isEmpty();

      if (kingSelected || hasHandCards || hasDefCards) {
        // King can only be used once per turn
        if (kingSelected && pt.isKingUsedThisTurn()) return;
        // King can only be used when the player has no defense cards
        if (kingSelected && (!currentPlayer.getDefCards().isEmpty() || !currentPlayer.getTopDefCards().isEmpty())) return;

        // Symbol constraint — joker can always attack regardless of symbol
        String attackSymbol;
        if (kingSelected) {
          attackSymbol = currentPlayer.getKingCard().getSymbol();
        } else if (hasHandCards) {
          attackSymbol = currentPlayer.getSelectedHandCards().get(0).getSymbol();
        } else {
          // Banneret: only own def cards selected
          attackSymbol = currentPlayer.getSelectedDefCards().get(0).getSymbol();
        }
        if (!"joker".equals(attackSymbol)) {
          if (pt.getAttackingSymbol()[0] != "none"
              && pt.getAttackingSymbol()[0] != attackSymbol
              && pt.getAttackingSymbol()[1] != attackSymbol) return;
        }

        int attackSum;
        ArrayList<Card> snapshot;
        ArrayList<Card> ownDefSnapshot;

        if (kingSelected) {
          attackSum = currentPlayer.getKingCard().getStrength();
          snapshot = new ArrayList<Card>();
          ownDefSnapshot = new ArrayList<Card>();
          pt.setKingUsed(true);
          pt.setAttackingSymbol(attackSymbol, currentPlayer.hasHero("Banneret"));
        } else {
          snapshot = new ArrayList<Card>(currentPlayer.getSelectedHandCards());
          ownDefSnapshot = new ArrayList<Card>(currentPlayer.getSelectedDefCards());
          attackSum = 0;
          for (Card c : snapshot) {
            attackSum += c.getStrength();
          }
          for (Card c : ownDefSnapshot) {
            attackSum += c.getStrength();
          }
          pt.setKingUsed(false);
          pt.setAttackingSymbol(attackSymbol, currentPlayer.hasHero("Banneret"));
        }

        // Add and consume mercenary attack bonus
        int mercBonus = pt.getMercenaryAttackBonus();
        if (mercBonus > 0) {
          attackSum += mercBonus;
          for (Hero h : currentPlayer.getHeroes()) {
            if (h.getHeroName() == "Mercenaries") {
              Mercenaries merc = (Mercenaries) h;
              for (int mi = 0; mi < mercBonus; mi++) merc.destroy();
              break;
            }
          }
          pt.resetMercenaryAttackBonus();
        }

        pt.setPendingAttackCards(snapshot);
        pt.setPendingAttackOwnDefCards(ownDefSnapshot);
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
        pt.setPendingPlunderAttackSum(attackSum);
        pt.setPendingPlunderDefStrength(defStrength);
        pt.setPlunderSuccess(attackSum > defStrength);

        // Broadcast plunder preview to all players so watchers can see what's happening
        if (gameState.getSocket() != null) {
          try {
            int attackerIdx = gameState.getCurrentPlayerIndex();
            JSONObject preview = new JSONObject();
            preview.put("attackerIdx", attackerIdx);
            preview.put("deckIndex", deckIndex);
            preview.put("defCardId", topCard.getCardId());
            preview.put("attackSum", attackSum);
            preview.put("defStrength", defStrength);
            preview.put("success", attackSum > defStrength);
            preview.put("kingUsed", kingSelected);
            preview.put("kingCardId", kingSelected && currentPlayer.getKingCard() != null ? currentPlayer.getKingCard().getCardId() : -1);
            preview.put("mercenaryBonus", mercBonus);
            preview.put("reservistBonus", 0);
            JSONArray atkIds = new JSONArray();
            for (Card c : snapshot) atkIds.put(c.getCardId());
            preview.put("attackCardIds", atkIds);
            JSONArray ownDefIds = new JSONArray();
            for (Card c : ownDefSnapshot) ownDefIds.put(c.getCardId());
            preview.put("ownDefCardIds", ownDefIds);
            preview.put("attackingSymbol", pt.getAttackingSymbol()[0]);
            preview.put("attackingSymbol2", pt.getAttackingSymbol()[1]);
            gameState.getSocket().emit("plunderPreview", preview);
          } catch (JSONException e) { e.printStackTrace(); }
        }

        gameState.setUpdateState(true);
      }
    } else {
      System.out.println("No more picking attacks allowed");
    }
  }
}

