package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Priest;

public class EnemyHandCardListener extends ClickListener {

  Card handCard;
  ArrayList<Player> players;
  Player player;

  public EnemyHandCardListener() {

  }

  public EnemyHandCardListener(Card handCard, Player player, ArrayList<Player> players) {
    this.handCard = handCard;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    // check if current player has priest and is selected
    for (int i = 0; i < player.getHeroes().size(); i++) {
      // if spy is selected, cast card away
      if (player.getHeroes().get(i).getHeroName() == "Priest" && player.getHeroes().get(i).isSelected()) {
        Priest priest = (Priest) player.getHeroes().get(i);
        if (priest.getConversionAttempts() > 0) {
          // convert hand card of enemy
          priest.conversionAttempt();
          if (player.getPlayerTurn().getAttackingSymbol()[0] == handCard.getSymbol()
              || handCard.getSymbol() == "joker") {
            System.out.println("Success: Symbol is " + handCard.getSymbol());
            priest.conversion();

            // loops over hand cards of all players and remove hand card from player
            for (int p = 0; p < players.size(); p++) {
              Iterator<Card> handCardsIt = players.get(p).getHandCards().iterator();
              while (handCardsIt.hasNext()) {
                Card cHandCard = handCardsIt.next();
                if (handCard == cHandCard) {
                  handCardsIt.remove();
                }
              }
            }

            player.addHandCard(handCard);
          } else {
            System.out.println("Failed: Symbol is " + handCard.getSymbol());
          }
        }
      }
    }
  }

}
