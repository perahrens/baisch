package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.heroes.Mercenaries;

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

  public OwnKingCardListener(GameState gameState, Player player, Card kingCard, Map<Integer, Card> defCards, Map<Integer, Card> topDefCards,
      ArrayList<Card> handCards) {
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
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Mercenaries"
            && player.getHeroes().get(i).isSelected()) {
          Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
          if (mercenaries.isAvailable()) {
            mercenaries.operate();
            kingCard.addBoosted(1);
          }
        }
      }
    } else {
      // unselect all handcards
      for (int i = 0; i < handCards.size(); i++) {
        handCards.get(i).setSelected(false);
      }

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

}
