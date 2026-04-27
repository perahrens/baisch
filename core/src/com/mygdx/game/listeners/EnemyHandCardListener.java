package com.mygdx.game.listeners;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Priest;

public class EnemyHandCardListener extends ClickListener {

  Card handCard;
  ArrayList<Player> players;
  Player player;
  int targetPlayerIdx;
  GameState gameState;

  public EnemyHandCardListener() {
  }

  public EnemyHandCardListener(Card handCard, Player player, ArrayList<Player> players,
      int targetPlayerIdx, GameState gameState) {
    this.handCard = handCard;
    this.player = player;
    this.players = players;
    this.targetPlayerIdx = targetPlayerIdx;
    this.gameState = gameState;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (com.mygdx.game.GameScreen.getInstance() != null && com.mygdx.game.GameScreen.getInstance().isZoomModeActive()) return;
    for (int i = 0; i < player.getHeroes().size(); i++) {
      if (player.getHeroes().get(i).getHeroName() == "Priest" && player.getHeroes().get(i).isSelected()) {
        Priest priest = (Priest) player.getHeroes().get(i);
        if (priest.getConversionAttempts() > 0) {
          // Open the Priest overlay for this enemy's hand
          gameState.setPriestTargetPlayerIdx(targetPlayerIdx);
          gameState.setPriestRevealedCardId(-1);
          gameState.setUpdateState(true);
        }
        return;
      }
    }
  }

}
