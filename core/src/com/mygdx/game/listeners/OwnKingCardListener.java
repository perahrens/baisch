package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Mercenaries;

public class OwnKingCardListener extends ClickListener {

  // cards of current player
  GameState gameState;
  Player player;
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  Map<Integer, Card> topDefCards;

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

  @Override
  public void clicked(InputEvent event, float x, float y) {

    if (player.getSelectedHeroes().size() > 0) {
      // Mercenaries in defense mode: clicking king adds a boost to it.
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Mercenaries" && player.getHeroes().get(i).isSelected()) {
          Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
          if (mercenaries.isAvailable()) {
            mercenaries.operate();
            kingCard.addBoosted(1);
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
      // Unselect all hand cards via player.getHandCards() so we always use the
      // current list — sortHandCards() and the post-swap re-add can replace the
      // underlying ArrayList, so the stored handCards field becomes stale and
      // would miss cards added after the last show() call (e.g. the old king
      // pushed back into hand by a coup swap).
      for (Card c : player.getHandCards()) {
        c.setSelected(false);
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
    // Trigger a screen rebuild so a fresh OwnKingCardListener is attached to
    // the current board king — after a coup/Warlord swap the old listener
    // still references the previous king Card object, so without a rebuild the
    // first click on the new king goes to a stale listener and has no visible
    // effect.
    gameState.setUpdateState(true);
  }

}
