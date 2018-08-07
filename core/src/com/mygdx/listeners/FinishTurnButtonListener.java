package com.mygdx.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;

public class FinishTurnButtonListener extends ClickListener {

  GameState gameState;

  public FinishTurnButtonListener(GameState gameState) {
    this.gameState = gameState;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    Player currentPlayer = gameState.getNextPlayer();
    System.out.println("Next player " + currentPlayer.getPlayerName());
    // show();
  };
}
