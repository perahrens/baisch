package com.mygdx.game.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;

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
    // Deselect everything so the player can interact normally
    if (gameState != null) {
      Player p = gameState.getCurrentPlayer();
      if (p.getKingCard() != null) p.getKingCard().setSelected(false);
      for (Card hc : p.getHandCards()) hc.setSelected(false);
      for (int i = 0; i < p.getHeroes().size(); i++) p.getHeroes().get(i).setSelected(false);
      gameState.setUpdateState(true);
    }
  }

}
