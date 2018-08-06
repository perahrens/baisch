package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;

public class OwnDefCardListener extends ClickListener {

  //cards of current player
  Card selectedCard;
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;

  public OwnDefCardListener() {
    System.out.println("created OwnDefCardListener");
  }

  public OwnDefCardListener(Card selectedCard, Card kingCard, Map<Integer, Card> defCards, ArrayList<Card> handCards) {
    System.out.println("created OwnDefCardListener");
    this.selectedCard = selectedCard;
    this.kingCard = kingCard;
    this.defCards = defCards;
    this.handCards = handCards;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    System.out.println("clicked OwnDefCardListener");

    // unselect all handcards
    for (int i = 0; i < handCards.size(); i++) {
      handCards.get(i).setSelected(false);
    }

    // select defense card
    if (selectedCard.isSelected()) {
      selectedCard.setSelected(false);
    } else {
      kingCard.setSelected(false);
      for (int i = 1; i <= 3; i++) {
        if (defCards.containsKey(i)) {
          defCards.get(i).setSelected(false);
        }
      }
      selectedCard.setSelected(true);
    }
  };

}
