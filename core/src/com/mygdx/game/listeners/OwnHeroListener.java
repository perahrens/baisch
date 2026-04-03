package com.mygdx.game.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Mercenaries;

public class OwnHeroListener extends ClickListener {

  Hero hero;
  Player player;
  GameState gameState;

  public OwnHeroListener(Hero hero, Player player) {
    this.hero = hero;
    this.player = player;
  }

  public OwnHeroListener(Hero hero, Player player, GameState gameState) {
    this.hero = hero;
    this.player = player;
    this.gameState = gameState;
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
        // Mercenaries: cancel pending attack bonus and return those mercenaries to available
        if (hero.getHeroName() == "Mercenaries") {
          Mercenaries merc = (Mercenaries) hero;
          int atkBonus = player.getPlayerTurn().getMercenaryAttackBonus();
          for (int b = 0; b < atkBonus; b++) merc.callback();
          player.getPlayerTurn().resetMercenaryAttackBonus();
          for (Card c : player.getHandCards()) {
            while (c.getBoosted() > 0) c.addBoosted(-1);
          }
          if (gameState != null) gameState.setUpdateState(true);
        }
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
