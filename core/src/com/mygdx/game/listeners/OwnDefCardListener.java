package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.FortifiedTower;
import com.mygdx.game.heroes.Merchant;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.net.SocketClient;

public class OwnDefCardListener extends ClickListener {

  // cards of current player
  GameState gameState;
  Card selectedCard;
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  Map<Integer, Card> topDefCards;
  ArrayList<Player> players;
  Player player;
  SocketClient socket;
  int playerIdx;

  public OwnDefCardListener() {
  }

  public OwnDefCardListener(GameState gameState, Card selectedCard, Card kingCard, Map<Integer, Card> defCards,
      Map<Integer, Card> topDefCards, ArrayList<Card> handCards, Player player, ArrayList<Player> players) {
    this.gameState = gameState;
    this.selectedCard = selectedCard;
    this.kingCard = kingCard;
    this.defCards = defCards;
    this.topDefCards = topDefCards;
    this.handCards = handCards;
    this.player = player;
    this.players = players;
  }

  public OwnDefCardListener(GameState gameState, Card selectedCard, Card kingCard, Map<Integer, Card> defCards,
      Map<Integer, Card> topDefCards, ArrayList<Card> handCards, Player player, ArrayList<Player> players,
      SocketClient socket, int playerIdx) {
    this(gameState, selectedCard, kingCard, defCards, topDefCards, handCards, player, players);
    this.socket = socket;
    this.playerIdx = playerIdx;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (com.mygdx.game.GameScreen.getInstance() != null && (com.mygdx.game.GameScreen.getInstance().isSpectator() || com.mygdx.game.GameScreen.getInstance().isZoomModeActive())) return;
    // Defense cards must not be interacted with when it is not the player's turn.
    if (gameState.getCurrentPlayerIndex() != playerIdx) return;

    // Recurring "select card to expose, nothing happens" bug: the user expects the
    // covered own defense card itself to be the expose target, not the slot button
    // in the hand area. If we are in pendingExposeCard state and this card is
    // covered, treat the click as the expose choice and finish the turn.
    com.mygdx.game.GameScreen gs = com.mygdx.game.GameScreen.getInstance();
    if (gs != null && gs.isPendingExpose() && selectedCard.isCovered()) {
      gs.submitExposeAndFinishTurn(selectedCard.getPositionId());
      return;
    }

    if (!player.isSlotSabotaged(selectedCard.getPositionId())) {
      // Issue #174: Fortified Tower auto-stack — if the player has the Fortified Tower
      // hero with charges and exactly one hand card of the matching symbol is selected,
      // stack it on this defense slot without requiring the hero to be pre-selected.
      // The hero blinks (like the Marshal blink in HandImageListener) to confirm.
      if (selectedCard.getLevel() == 0
          && !player.getTopDefCards().containsKey(selectedCard.getPositionId())
          && player.getSelectedHandCards().size() == 1) {
        FortifiedTower ft = null;
        for (int i = 0; i < player.getHeroes().size(); i++) {
          if ("Fortified Tower".equals(player.getHeroes().get(i).getHeroName())) {
            ft = (FortifiedTower) player.getHeroes().get(i);
            break;
          }
        }
        if (ft != null && ft.getDefenseExpands() > 0) {
          Card handCard = player.getSelectedHandCards().get(0);
          if (handCard.getSymbol().equals(selectedCard.getSymbol())) {
            int handCardId = handCard.getCardId();
            int slot = selectedCard.getPositionId();
            ft.defenseExpand();
            handCard.setLevel(1);
            player.putDefCard(slot, 1);
            ft.setSelected(false);
            // Blink the Fortified Tower hero icon green (issue #174)
            ft.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.color(com.badlogic.gdx.graphics.Color.GREEN, 0f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(0.3f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.color(com.badlogic.gdx.graphics.Color.WHITE, 0.2f)
            ));
            if (socket != null) {
              try {
                JSONObject data = new JSONObject();
                data.put("playerIdx", playerIdx);
                data.put("slot", slot);
                data.put("cardId", handCardId);
                socket.emit("fortifiedTowerStack", data);
              } catch (JSONException e) { e.printStackTrace(); }
            }
            gameState.setUpdateState(true);
            return;
          }
        }
      }

      // if F.Tower and hand card is selected, put hand card on top
      if (player.getSelectedHeroes().size() > 0) {
        for (int i = 0; i < player.getHeroes().size(); i++) {
          if ("Fortified Tower".equals(player.getHeroes().get(i).getHeroName()) && player.getHeroes().get(i).isSelected()
              && player.getSelectedHandCards().size() == 1 && selectedCard.getLevel() == 0
              && !player.getTopDefCards().containsKey(selectedCard.getPositionId())) {
            Card handCard = player.getSelectedHandCards().get(0);
            FortifiedTower fortifiedTower = (FortifiedTower) player.getHeroes().get(i);
            if (fortifiedTower.getDefenseExpands() > 0 && handCard.getSymbol().equals(selectedCard.getSymbol())) {
              int handCardId = handCard.getCardId();
              int slot = selectedCard.getPositionId();
              fortifiedTower.defenseExpand();
              handCard.setLevel(1);
              player.putDefCard(slot, 1);
              fortifiedTower.setSelected(false);
              if (socket != null) {
                try {
                  JSONObject data = new JSONObject();
                  data.put("playerIdx", playerIdx);
                  data.put("slot", slot);
                  data.put("cardId", handCardId);
                  socket.emit("fortifiedTowerStack", data);
                } catch (JSONException e) { e.printStackTrace(); }
              }
              gameState.setUpdateState(true);
            }
          } else if (player.getHeroes().get(i).getHeroName() == "Mercenaries"
              && player.getHeroes().get(i).isSelected()) {
            Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
            // Issue #167: top half of the def card adds a mercenary, bottom half removes one.
            // y is in the actor's local coordinate space (0 = bottom edge).
            boolean topHalf = y >= selectedCard.getHeight() / 2f;
            if (topHalf) {
              if (mercenaries.isAvailable()) {
                mercenaries.operate();
                selectedCard.addBoosted(1);
                emitBoost(selectedCard.getPositionId(), selectedCard.getBoosted());
                gameState.setUpdateState(true);
              }
            } else {
              if (selectedCard.getBoosted() > 0) {
                mercenaries.callback();
                selectedCard.addBoosted(-1);
                emitBoost(selectedCard.getPositionId(), selectedCard.getBoosted());
                gameState.setUpdateState(true);
              }
            }
          } else if (player.getHeroes().get(i).getHeroName() == "Spy"
              && player.getHeroes().get(i).isSelected()) {
            Spy spy = (Spy) player.getHeroes().get(i);
            if (spy.getSpyExtends() > 0) {
              // Sacrifice this own defense card for +2 spy actions
              int posId = selectedCard.getPositionId();
              gameState.getCemeteryDeck().addCard(selectedCard);
              if (topDefCards.containsValue(selectedCard)) {
                topDefCards.remove(posId);
              } else {
                defCards.remove(posId);
              }
              final int sacrificedCardId = selectedCard.getCardId();
              spy.spyExtend();
              if (socket != null) {
                try {
                  JSONObject extendData = new JSONObject();
                  extendData.put("cardId", sacrificedCardId);
                  socket.emit("spyExtend", extendData);
                } catch (JSONException e) { e.printStackTrace(); }
              }
              gameState.setUpdateState(true);
            }
          } else if (player.getHeroes().get(i).getHeroName() == "Merchant"
              && player.getHeroes().get(i).isSelected()) {
            Merchant merchant = (Merchant) player.getHeroes().get(i);
            if (merchant.getTrades() > 0) {
              int posId = selectedCard.getPositionId();
              int discardedCardId = selectedCard.getCardId();
              if (topDefCards.containsValue(selectedCard)) {
                topDefCards.remove(posId);
              } else {
                defCards.remove(posId);
              }
              gameState.getCemeteryDeck().addCard(selectedCard);
              merchant.trade();
              Card newCard = gameState.getCardDeck().getCard(gameState.getCemeteryDeck());
              player.addHandCard(newCard);
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
              gameState.setUpdateState(true);
            }
          }
        }
      } else {
        // For stacked slots, always select/deselect both cards together
        int slot = selectedCard.getPositionId();
        Card pairedCard = null;
        if (defCards.containsKey(slot) && defCards.get(slot) != selectedCard) {
          pairedCard = defCards.get(slot);
        } else if (topDefCards.containsKey(slot) && topDefCards.get(slot) != selectedCard) {
          pairedCard = topDefCards.get(slot);
        }

        // Issue #177: Banneret no longer enables defense-as-attack — exclusive
        // selection always applies. Deselect hand cards and all other def cards.
        for (int i = 0; i < handCards.size(); i++) {
          handCards.get(i).setSelected(false);
        }

        // select defense card
        if (selectedCard.isSelected()) {
          selectedCard.setSelected(false);
          if (pairedCard != null) pairedCard.setSelected(false);
        } else {
          kingCard.setSelected(false);
          for (int i = 1; i <= 3; i++) {
            if (defCards.containsKey(i)) {
              defCards.get(i).setSelected(false);
            }
            if (topDefCards.containsKey(i)) {
              topDefCards.get(i).setSelected(false);
            }
          }
          selectedCard.setSelected(true);
          if (pairedCard != null) pairedCard.setSelected(true);
        }

      }
    }

    // gameState.setUpdateState(true);

  };

  private void emitBoost(int slot, int boostedCount) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIdx", playerIdx);
      data.put("slot", slot);
      // level: check if this card is a topDefCard (level 1) or defCard (level 0)
      boolean isTop = topDefCards.containsValue(selectedCard);
      data.put("level", isTop ? 1 : 0);
      data.put("boosted", boostedCount);
      socket.emit("mercDefBoost", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

}
