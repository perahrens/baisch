package com.mygdx.listeners;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.Player;
import com.mygdx.heroes.Spy;

public class EnemyKingCardListener extends ClickListener {

  Card kingCard;
  Player player;
  ArrayList<Player> players;

  public EnemyKingCardListener() {
  }

  public EnemyKingCardListener(Card kingCard, Player player, ArrayList<Player> players) {
    this.kingCard = kingCard;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (player.getSelectedHeroes().size() > 0) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Spy" && player.getHeroes().get(i).isSelected()) {
          Spy spy = (Spy) player.getHeroes().get(i);
          // check if all def cards are uncovered
          // first find player
          System.out.println("this player will be ");
          Player targetPlayer = players.get(0);
          ;
          for (int j = 0; i < players.size(); i++) {
            if (players.get(j).getKingCard() == kingCard) {
              System.out.println("this player " + players.get(j).getPlayerName());
              targetPlayer = players.get(j);
            }
          }

          boolean allUncovered = true;
          for (int j = 1; j <= 3; j++) {
            if (targetPlayer.getDefCards().containsKey(j)) {
              if (targetPlayer.getDefCards().get(j).isCovered()) {
                allUncovered = false;
              }
            }
          }

          if (spy.getSpyAttacks() > 0 && allUncovered) {
            spy.spyAttack();
            System.out.println("Number spy attacks left = " + spy.getSpyAttacks());
            kingCard.setCovered(false);
          }
        }
      }
    }
  }

}
