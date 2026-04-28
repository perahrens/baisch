package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

public class OwnKingCardListener extends ClickListener {

  // cards of current player
  GameState gameState;
  Player player;
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  Map<Integer, Card> topDefCards;
  SocketClient socket;
  int playerIdx;

  public OwnKingCardListener() {
  }

  public OwnKingCardListener(GameState gameState, Player player, Card kingCard, Map<Integer, Card> defCards,
      Map<Integer, Card> topDefCards, ArrayList<Card> handCards) {
    this.gameState = gameState;
    this.player = player;
    this.kingCard = kingCard;
    this.defCards = defCards;
    this.topDefCards = topDefCards;
    this.handCards = handCards;
  }

  public OwnKingCardListener(GameState gameState, Player player, Card kingCard, Map<Integer, Card> defCards,
      Map<Integer, Card> topDefCards, ArrayList<Card> handCards, SocketClient socket, int playerIdx) {
    this(gameState, player, kingCard, defCards, topDefCards, handCards);
    this.socket = socket;
    this.playerIdx = playerIdx;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (com.mygdx.game.GameScreen.getInstance() != null && (com.mygdx.game.GameScreen.getInstance().isSpectator() || com.mygdx.game.GameScreen.getInstance().isZoomModeActive())) return;

    if (player.getSelectedHeroes().size() > 0) {
      // Mercenaries in defense mode: top half of king adds a mercenary, bottom half removes one.
      // (Issue #167: same UX as for own defense cards.)
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Mercenaries" && player.getHeroes().get(i).isSelected()) {
          Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
          boolean topHalf = y >= kingCard.getHeight() / 2f;
          if (topHalf) {
            if (mercenaries.isAvailable()) {
              mercenaries.operate();
              kingCard.addBoosted(1);
              emitKingBoost(kingCard.getBoosted());
              if (gameState != null) gameState.setUpdateState(true);
            }
          } else {
            if (kingCard.getBoosted() > 0) {
              mercenaries.callback();
              kingCard.addBoosted(-1);
              emitKingBoost(kingCard.getBoosted());
              if (gameState != null) gameState.setUpdateState(true);
            }
          }
          return;
        }
      }
      // Any other hero selected (e.g. Warlord after using its attack): deselect it
      // and fall through to normal king selection below.
      for (int i = 0; i < player.getHeroes().size(); i++) {
        player.getHeroes().get(i).setSelected(false);
      }
    }

    {
      // unselect all handcards
      for (int i = 0; i < handCards.size(); i++) {
        handCards.get(i).setSelected(false);
      }
      // Selecting own king cancels any pending coup-swap auto-select
      player.getPlayerTurn().setCoupSwapPendingCardId(-1);

      // select king card
      if (kingCard.isSelected()) {
        kingCard.setSelected(false);
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
        kingCard.setSelected(true);
      }
    }
    ;

  }

  /** Emit mercDefBoost with level=-1 to indicate the king card. */
  private void emitKingBoost(int boostedCount) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIdx", playerIdx);
      data.put("slot", -1);
      data.put("level", -1);
      data.put("boosted", boostedCount);
      socket.emit("mercDefBoost", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

}
