package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;

public class OwnKingCardListener extends ClickListener {
  
  //cards of current player
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  
  public OwnKingCardListener() {
  }

  public OwnKingCardListener(Card kingCard, Map<Integer, Card> defCards, ArrayList<Card> handCards) {
    this.kingCard = kingCard;
    this.defCards = defCards;
    this.handCards = handCards;
  }
  
  @Override
  public void clicked(InputEvent event, float x, float y) {
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
      }
      kingCard.setSelected(true);
    }
  };
  
}
