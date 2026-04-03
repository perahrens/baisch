package com.mygdx.game.listeners;

import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Mercenaries;
import io.socket.client.Socket;

public class MercenaryImageListener extends ClickListener {

  GameState gameState;
  Card defCard;
  Player player;
  Socket socket;
  int playerIdx;
  int slot;
  int level;

  public MercenaryImageListener(GameState gameState, Card defCard, Player player) {
    this.gameState = gameState;
    this.defCard = defCard;
    this.player = player;
  }

  public MercenaryImageListener(GameState gameState, Card defCard, Player player, Socket socket, int playerIdx, int slot, int level) {
    this(gameState, defCard, player);
    this.socket = socket;
    this.playerIdx = playerIdx;
    this.slot = slot;
    this.level = level;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    // is player owner of hero
    if (player.hasHero("Mercenaries")) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Mercenaries") {
          Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
          mercenaries.callback();
          defCard.addBoosted(-1);
          emitBoost(defCard.getBoosted());
          break;
        }
      }
    }

    gameState.setUpdateState(true);
  };

  private void emitBoost(int boostedCount) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIdx", playerIdx);
      data.put("slot", slot);
      data.put("level", level);
      data.put("boosted", boostedCount);
      socket.emit("mercDefBoost", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

}
