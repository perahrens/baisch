package com.mygdx.game.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Merchant;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.heroes.Warlord;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

public class OwnHandCardListener extends ClickListener {

  Card handCard;
  Player player;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;
  GameState gameState;
  SocketClient socket;
  int playerIdx;

  public OwnHandCardListener() {}

  public OwnHandCardListener(Card handCard, Player player, CardDeck cardDeck, CardDeck cemeteryDeck) {
    this.handCard = handCard;
    this.player = player;
    this.cardDeck = cardDeck;
    this.cemeteryDeck = cemeteryDeck;
  }

  public OwnHandCardListener(Card handCard, Player player, CardDeck cardDeck, CardDeck cemeteryDeck, GameState gameState) {
    this(handCard, player, cardDeck, cemeteryDeck);
    this.gameState = gameState;
  }

  public OwnHandCardListener(Card handCard, Player player, CardDeck cardDeck, CardDeck cemeteryDeck, GameState gameState,
      SocketClient socket, int playerIdx) {
    this(handCard, player, cardDeck, cemeteryDeck, gameState);
    this.socket = socket;
    this.playerIdx = playerIdx;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    // Warlord king swap: if own king is selected, swap it with this hand card
    // Costs 1 take + 1 put action.
    // Allowed only when: player has Warlord hero, OR player has no defense cards (coup).
    if (player.getKingCard() != null && player.getKingCard().isSelected()) {
      boolean hasWarlord = player.hasHero("Warlord");
      boolean hasDefCards = !player.getDefCards().isEmpty() || !player.getTopDefCards().isEmpty();
      if ((hasWarlord || !hasDefCards) && player.getPlayerTurn().getTakeDefCard() > 0 && player.getPlayerTurn().getPutDefCard() > 0) {
        Card oldKing = player.getKingCard();
        Card newKing = handCard;
        // Swap locally — new king is always placed face-down
        newKing.setCovered(true);
        player.setKingCard(newKing);
        player.getHandCards().remove(newKing);
        player.addHandCard(oldKing);
        // Non-warlord coup: keep old king selected so it can immediately attack.
        // coupSwapPendingCardId survives stateUpdate hand rebuilds, keeping the card auto-selected.
        if (!hasWarlord) {
          player.getPlayerTurn().setCoupSwapPendingCardId(oldKing.getCardId());
          player.setSelectedSymbol(oldKing.getSymbol());
          // Fresh king on board: reset king-used flag so the new board king can attack this turn
          player.getPlayerTurn().setKingUsedThisTurn(false);
        }
        // Deselect king
        player.getKingCard().setSelected(false);
        // Consume actions
        player.getPlayerTurn().decreaseTakeDefCard();
        player.getPlayerTurn().decreasePutDefCard();
        // Sync to server
        if (socket != null) {
          try {
            JSONObject data = new JSONObject();
            data.put("playerIdx", playerIdx);
            data.put("oldKingCardId", oldKing.getCardId());
            data.put("newKingCardId", newKing.getCardId());
            socket.emit("warlordKingSwap", data);
          } catch (JSONException e) { e.printStackTrace(); }
        }
        if (gameState != null) gameState.setUpdateState(true);
      }
      return;
    }

    // check hero functions on hand cards (Spy, Merchant — but NOT Mercenaries here;
    // Mercenaries attack bonus is added by clicking the hero while hand cards are selected)
    boolean spyOrMerchantSelected = false;
    for (int i = 0; i < player.getHeroes().size(); i++) {
      String hn = player.getHeroes().get(i).getHeroName();
      if ((hn == "Spy" || hn == "Merchant") && player.getHeroes().get(i).isSelected()) {
        spyOrMerchantSelected = true;
        break;
      }
    }
    if (spyOrMerchantSelected) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        // if spy is selected, cast card away
        if (player.getHeroes().get(i).getHeroName() == "Spy" && player.getHeroes().get(i).isSelected()) {
          Spy spy = (Spy) player.getHeroes().get(i);
          if (spy.getSpyExtends() > 0) {
            // Sacrifice the clicked hand card directly for +2 spy actions
            System.out.println("Spy sacrifice handcard " + handCard.getStrength());
            cemeteryDeck.addCard(handCard);
            player.getHandCards().remove(handCard);
            final int sacrificedCardId = handCard.getCardId();
            spy.spyExtend();
            if (socket != null) {
              try {
                JSONObject extendData = new JSONObject();
                extendData.put("cardId", sacrificedCardId);
                socket.emit("spyExtend", extendData);
              } catch (JSONException e) { e.printStackTrace(); }
            }
            if (gameState != null) gameState.setUpdateState(true);
          }
          return;
        } else if (player.getHeroes().get(i).getHeroName() == "Merchant" && player.getHeroes().get(i).isSelected()) {
          Merchant merchant = (Merchant) player.getHeroes().get(i);
          if (merchant.getTrades() > 0) {
            // Trade the clicked hand card directly
            int discardedCardId = handCard.getCardId();
            cemeteryDeck.addCard(handCard);
            player.getHandCards().remove(handCard);

            // draw replacement card
            merchant.trade();
            Card newCard = cardDeck.getCard(cemeteryDeck);
            boolean isJoker = "joker".equals(newCard.getSymbol());
            if (isJoker) {
              // Joker on 1st draw: keep it (no 2nd try required for first draw joker)
              player.addHandCard(newCard);
            } else {
              player.addHandCard(newCard);
            }
            newCard.setTradable(true);

            if (socket != null) {
              try {
                JSONObject data = new JSONObject();
                data.put("playerIdx", playerIdx);
                data.put("discardedCardId", discardedCardId);
                data.put("drawnCardId", newCard.getCardId());
                socket.emit("merchantTrade", data);
              } catch (JSONException e) { e.printStackTrace(); }
            }
            if (gameState != null) gameState.setUpdateState(true);
          }
          return;
        }
      }
    } else {
      // If Mercenaries hero was selected (defense mode), deselect it to allow hand card selection
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Mercenaries" && player.getHeroes().get(i).isSelected()) {
          player.getHeroes().get(i).setSelected(false);
          break;
        }
      }
      // unselect all defense and king cards
      player.getKingCard().setSelected(false);
      for (int i = 1; i <= 3; i++) {
        if (player.getDefCards().containsKey(i)) {
          player.getDefCards().get(i).setSelected(false);
        }
        if (player.getTopDefCards().containsKey(i)) {
          player.getTopDefCards().get(i).setSelected(false);
        }
      }

      // select hand card
      if (handCard.isSelected()) {
        handCard.setSelected(false);
      } else {
        if (handCard.getSymbol() == player.getSelectedSymbol()) {
          handCard.setSelected(true);
        } else {
          for (int i = 0; i < player.getHandCards().size(); i++) {
            player.getHandCards().get(i).setSelected(false);
          }
          handCard.setSelected(true);
          player.setSelectedSymbol(handCard.getSymbol());
          // Player switched to different symbol — cancel coup-swap auto-select
          player.getPlayerTurn().setCoupSwapPendingCardId(-1);
        }
      }

      // If all hand cards are now deselected, clear any pending mercenary attack bonus
      if (player.getSelectedHandCards().size() == 0 && player.getPlayerTurn().getMercenaryAttackBonus() > 0) {
        for (int i = 0; i < player.getHeroes().size(); i++) {
          if (player.getHeroes().get(i).getHeroName() == "Mercenaries") {
            Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
            int bonus = player.getPlayerTurn().getMercenaryAttackBonus();
            for (int b = 0; b < bonus; b++) mercenaries.callback();
            player.getPlayerTurn().resetMercenaryAttackBonus();
            if (gameState != null) gameState.setUpdateState(true);
            break;
          }
        }
      }
    }
  };

}
