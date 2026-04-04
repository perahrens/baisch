package com.mygdx.game.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;

public class KeepCardButtonListener extends ClickListener {

  Card tradeableCard;
  GameState gameState;

  public KeepCardButtonListener(Card tradeableCard, GameState gameState) {
    this.tradeableCard = tradeableCard;
    this.gameState = gameState;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    tradeableCard.setTradable(false);
    if (gameState != null) gameState.setUpdateState(true);
  }

}
