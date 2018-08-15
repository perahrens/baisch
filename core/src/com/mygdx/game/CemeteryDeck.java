package com.mygdx.game;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class CemeteryDeck extends Actor {
  Sprite sprite;
  ArrayList<Card> cards = new ArrayList<Card>();

  public CemeteryDeck() {
    sprite = new Sprite();

    setWidth(200);
    setHeight(200);
  }

  public void addCard(Card card) {
    cards.add(card);
  }

  public ArrayList<Card> getCards() {
    return this.cards;
  }
}
