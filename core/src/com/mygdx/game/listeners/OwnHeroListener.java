package com.mygdx.game.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Reservists;
import com.mygdx.game.heroes.Spy;

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
    // Spy can be clicked even when isReady==false, as long as it has extends left (sacrifice mode)
    boolean spyExtendMode = hero.getHeroName() == "Spy" && !hero.isReady()
        && ((Spy) hero).getSpyExtends() > 0 && hero.isSelectable();

    if ((hero.isReady() && hero.isSelectable()) || spyExtendMode) {

      // ---- Mercenaries attack mode: hand cards selected + click hero = +1 bonus ----
      if (hero.getHeroName() == "Mercenaries" && player.getSelectedHandCards().size() > 0) {
        Mercenaries mercenaries = (Mercenaries) hero;
        if (mercenaries.isAvailable()) {
          mercenaries.operate();
          player.getPlayerTurn().incrementMercenaryAttackBonus();
          if (gameState != null) gameState.setUpdateState(true);
        }
        return; // never toggle selection in attack mode
      }

      // ---- Reservists: attack boost is handled only in the attack overlay, not in hand stage ----
      if ("Reservists".equals(hero.getHeroName()) && player.getSelectedHandCards().size() > 0) {
        return;
      }

      if (hero.isSelected()) {
        // Deselect: if Mercenaries was in defense mode, just deselect
        hero.setSelected(false);
        // Deselect hero also resets any accumulated hand-card boost (defense mode cancel)
        if (hero.getHeroName() == "Mercenaries") {
          if (gameState != null) gameState.setUpdateState(true);
        }
      } else {
        // Select: unselect everything else first
        player.getKingCard().setSelected(false);
        for (int i = 1; i <= 3; i++) {
          if (player.getDefCards().containsKey(i)) {
            player.getDefCards().get(i).setSelected(false);
          }
        }
        // For Mercenaries/Reservists: don't deselect hand cards — attack mode relies on them staying selected
        if (hero.getHeroName() != "Mercenaries" && hero.getHeroName() != "Reservists") {
          for (int i = 0; i < player.getHandCards().size(); i++) {
            player.getHandCards().get(i).setSelected(false);
          }
        }
        for (int i = 0; i < player.getHeroes().size(); i++) {
          player.getHeroes().get(i).setSelected(false);
        }
        hero.setSelected(true);
      }
    }
  }

}
