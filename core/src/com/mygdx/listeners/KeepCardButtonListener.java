package com.mygdx.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;

public class KeepCardButtonListener extends ClickListener {

  Card tradeableCard;

  public KeepCardButtonListener(Card tradeableCard) {
    this.tradeableCard = tradeableCard;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    tradeableCard.setTradable(false);
    // gameState.setUpdateState(true);
  }

}
