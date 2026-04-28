package com.mygdx.game.listeners;

import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.net.SocketClient;

/**
 * Listener for empty enemy defense-card slots.
 * Enables the Saboteurs hero to place a saboteur on an empty field.
 */
public class EnemyPlaceholderListener extends ClickListener {

  Player player;        // current (attacking) player
  Player targetPlayer;  // enemy player who owns this empty slot
  int slot;             // position 1-3
  int playerIdx;        // current player's index
  int targetPlayerIdx;
  GameState gameState;
  SocketClient socket;

  public EnemyPlaceholderListener(Player player, Player targetPlayer, int slot,
      int playerIdx, int targetPlayerIdx, GameState gameState, SocketClient socket) {
    this.player = player;
    this.targetPlayer = targetPlayer;
    this.slot = slot;
    this.playerIdx = playerIdx;
    this.targetPlayerIdx = targetPlayerIdx;
    this.gameState = gameState;
    this.socket = socket;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (com.mygdx.game.GameScreen.getInstance() != null && (com.mygdx.game.GameScreen.getInstance().isSpectator() || com.mygdx.game.GameScreen.getInstance().isZoomModeActive())) return;
    // Only act when Saboteurs hero is selected
    for (int si = 0; si < player.getHeroes().size(); si++) {
      if ("Saboteurs".equals(player.getHeroes().get(si).getHeroName())
          && player.getHeroes().get(si).isSelected()) {
        Saboteurs saboteurs = (Saboteurs) player.getHeroes().get(si);
        if (saboteurs.isAvailable() && !targetPlayer.isSlotSabotaged(slot)) {
          targetPlayer.setSlotSabotaged(slot, playerIdx);
          saboteurs.sabotage();
          emitSabotage();
          if (gameState != null) gameState.setUpdateState(true);
        }
        return;
      }
    }
  }

  private void emitSabotage() {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", playerIdx);
      data.put("defenderIdx", targetPlayerIdx);
      data.put("positionId", slot);
      socket.emit("sabotage", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }
}
