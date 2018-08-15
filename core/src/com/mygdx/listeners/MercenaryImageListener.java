package com.mygdx.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.heroes.Mercenaries;

public class MercenaryImageListener extends ClickListener {

  GameState gameState;
  Card defCard; // sabotaged def card
  Player player;

  public MercenaryImageListener(GameState gameState, Card defCard, Player player) {
    this.gameState = gameState;
    this.defCard = defCard;
    this.player = player;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    // is player owner of hero
    if (player.hasHero("Mercenaries")) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Mercenaries") {
          Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
          mercenaries.callback();
          defCard.addBoosted(-1);
          break;
        }
      }
    } else {
      // if not, nothing to be done
    }

    gameState.setUpdateState(true);
  };

}
