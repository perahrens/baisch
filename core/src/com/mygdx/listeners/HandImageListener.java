package com.mygdx.listeners;

import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.Player;

public class HandImageListener extends ClickListener {
  
  Player player;
  
  public HandImageListener(Player player) { 
    this.player = player;
  }
  
  @Override
  public void clicked(InputEvent event, float x, float y) {
    Map<Integer, Card> defCards = player.getDefCards();
    for (int j = 1; j <= 3; j++) {
      if (defCards.containsKey(j)) {
        if (defCards.get(j).isSelected()) {
          player.takeDefCard(j);
        }
      }
    }
    //show();
  };

}
