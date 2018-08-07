package com.mygdx.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Player;
import com.mygdx.heroes.Hero;

public class OwnHeroListener extends ClickListener {

  Hero hero;
  Player player;

  public OwnHeroListener(Hero hero, Player player) {
    this.hero = hero;
    this.player = player;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (hero.isReady() && hero.isSelectable()) {
      System.out.println("Hero isSelected=" + hero.isSelected());
      // unselect all defense and king cards
      player.getKingCard().setSelected(false);
      for (int i = 1; i <= 3; i++) {
        if (player.getDefCards().containsKey(i)) {
          player.getDefCards().get(i).setSelected(false);
        }
      }

      // unselect all hand cards
      for (int i = 0; i < player.getHandCards().size(); i++) {
        player.getHandCards().get(i).setSelected(false);
      }

      // select current hero
      if (hero.isSelected()) {
        // unselect selected hero
        hero.setSelected(false);
      } else {
        // unselect all other heroes and only select new one
        for (int i = 0; i < player.getHeroes().size(); i++) {
          player.getHeroes().get(i).setSelected(false);
        }
        hero.setSelected(true);
      }
      System.out.println("Hero  isSelected=" + hero.isSelected());
    }
  }

}
