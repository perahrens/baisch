package com.mygdx.game.listeners;

import java.util.ArrayList;

import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.net.SocketClient;

/**
 * Click handler for the saboteur icon overlaid on a defense card or empty slot.
 *
 * <ul>
 *   <li>Owner of the saboteur clicks → recalls the saboteur back.</li>
 *   <li>Defender clicks on a real card → sacrifices (discards) the card to destroy the saboteur.</li>
 *   <li>Defender clicks on an empty slot with one hand card selected → sacrifices that hand card
 *       to destroy the saboteur.</li>
 * </ul>
 */
public class SabotagedImageListener extends ClickListener {

  GameState gameState;
  Card defCard;            // the card beneath the saboteur (may be a placeholder)
  Player currentPlayer;    // the player who is clicking
  Player sabotagedPlayer;  // the player whose slot is sabotaged
  int slot;                // defense slot (1-3)
  int currentPlayerIdx;
  SocketClient socket;

  public SabotagedImageListener(GameState gameState, Card defCard, Player currentPlayer,
      Player sabotagedPlayer, int slot, int currentPlayerIdx, SocketClient socket) {
    this.gameState = gameState;
    this.defCard = defCard;
    this.currentPlayer = currentPlayer;
    this.sabotagedPlayer = sabotagedPlayer;
    this.slot = slot;
    this.currentPlayerIdx = currentPlayerIdx;
    this.socket = socket;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    int ownerIdx = sabotagedPlayer.getSlotSaboteurOwnerIdx(slot);
    if (ownerIdx < 0) return; // no saboteur here

    if (ownerIdx == currentPlayerIdx) {
      // ---- Owner recalls saboteur ----
      sabotagedPlayer.clearSlotSabotaged(slot);
      Saboteurs saboteurs = findSaboteurs(gameState.getPlayers().get(ownerIdx));
      if (saboteurs != null) saboteurs.callback();
      emitSabotageCallback(ownerIdx, sabotagedPlayerIdx(), slot);

    } else if (!defCard.isPlaceholder()) {
      // ---- Defender sacrifices their real defense card ----
      gameState.getCemeteryDeck().addCard(defCard);
      defCard.setRemoved(true);
      sabotagedPlayer.clearSlotSabotaged(slot);
      Saboteurs saboteurs = findSaboteurs(gameState.getPlayers().get(ownerIdx));
      if (saboteurs != null) saboteurs.destroy();
      emitSabotageSacrifice(sabotagedPlayerIdx(), slot);

    } else {
      // ---- Defender sacrifices a hand card to clear saboteur on empty slot ----
      ArrayList<Card> selected = currentPlayer.getSelectedHandCards();
      if (selected.size() == 1) {
        Card handCard = selected.get(0);
        int handCardId = handCard.getCardId();
        currentPlayer.getHandCards().remove(handCard);
        sabotagedPlayer.clearSlotSabotaged(slot);
        Saboteurs saboteurs = findSaboteurs(gameState.getPlayers().get(ownerIdx));
        if (saboteurs != null) saboteurs.destroy();
        emitSabotageEmptySlotSacrifice(sabotagedPlayerIdx(), slot, handCardId);
      }
    }

    gameState.setUpdateState(true);
  }

  // ---- helpers ----

  private int sabotagedPlayerIdx() {
    return gameState.getPlayers().indexOf(sabotagedPlayer);
  }

  private Saboteurs findSaboteurs(Player p) {
    if (p == null) return null;
    for (int i = 0; i < p.getHeroes().size(); i++) {
      if ("Saboteurs".equals(p.getHeroes().get(i).getHeroName()))
        return (Saboteurs) p.getHeroes().get(i);
    }
    return null;
  }

  private void emitSabotageCallback(int attackerIdx, int defenderIdx, int positionId) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", attackerIdx);
      data.put("defenderIdx", defenderIdx);
      data.put("positionId", positionId);
      socket.emit("sabotageCallback", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitSabotageSacrifice(int defenderIdx, int positionId) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("defenderIdx", defenderIdx);
      data.put("positionId", positionId);
      socket.emit("sabotageSacrifice", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitSabotageEmptySlotSacrifice(int defenderIdx, int positionId, int handCardId) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("defenderIdx", defenderIdx);
      data.put("positionId", positionId);
      data.put("handCardId", handCardId);
      socket.emit("sabotageEmptySlotSacrifice", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }
}

