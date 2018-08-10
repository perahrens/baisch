package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.Player;
import com.mygdx.heroes.King;
import com.mygdx.heroes.Magician;
import com.mygdx.heroes.Priest;
import com.mygdx.heroes.Spy;

public class EnemyDefCardListener extends ClickListener {
  
  Card defCard;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;
  ArrayList<Player> players;
  Player player;

  public EnemyDefCardListener () {
  }
  
  public EnemyDefCardListener (Card defCard, CardDeck cardDeck, CardDeck cemeteryDeck, Player player, ArrayList<Player> players) {
    this.defCard = defCard;
    this.cardDeck = cardDeck;
    this.cemeteryDeck = cemeteryDeck;
    this.player = player;
    this.players = players;
  }
  
  @Override
  public void clicked(InputEvent event, float x, float y) {
    if (player.getSelectedHeroes().size() > 0) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Spy"
            && player.getHeroes().get(i).isSelected()) {
          Spy spy = (Spy) player.getHeroes().get(i);
          if (spy.getSpyAttacks() > 0) {
            spy.spyAttack();
            System.out.println("Number spy attacks left = " + spy.getSpyAttacks());
            defCard.setCovered(false);
          }
        } else if (player.getHeroes().get(i).getHeroName() == "Magician"
            && player.getHeroes().get(i).isSelected()) {
          Magician magician = (Magician) player.getHeroes().get(i);
          if (magician.getSpells() > 0) {
            magician.castSpell();

            // loop over all def cards of player
            for (int p = 0; p < players.size(); p++) {
              for (int c = 1; c <= 3; c++) {
                if (players.get(p).getDefCards().containsKey(c)) {
                  Card cDefCard = players.get(p).getDefCards().get(c);
                  Card newDefCard = cardDeck.getCard(cemeteryDeck);
                  if (cDefCard == defCard) {
                    newDefCard.setCovered(!defCard.isCovered());
                    cemeteryDeck.addCard(defCard);
                    players.get(p).getDefCards().remove(c);
                    players.get(p).addDefCard(c, newDefCard, 0);
                    //gameState.setUpdateState(true);
                  }
                }
              }
            }
          }
        } else if (player.getHeroes().get(i).getHeroName() == "King"
            && player.getKingCard().isSelected()
            && (player.getKingCard().getSymbol() == player
                .getPlayerTurn().getAttackingSymbol()
                || player.getPlayerTurn().getAttackingSymbol() == "none")) {
          System.out.println("king attack");
          King king = (King) player.getHeroes().get(i);
          king.royalAttack();
          player.getPlayerTurn()
              .setAttackingSymbol(player.getKingCard().getSymbol());
          // cover up hero and enemy defense card
          defCard.setCovered(false);
          player.getKingCard().setCovered(false);

          int attackResult = player.royalAttack(defCard);

          if (attackResult == 2) {
            player.addHandCard(defCard);
            defCard.setRemoved(true);
          } else if (attackResult == 1) {
            // nothing happens
          } else {
            // TODO player lost
          }

          //gameState.setUpdateState(true);
        }
      }
    }

    if (player.getSelectedHandCards().size() > 0) {
      if (player.getPlayerTurn().getAttackingSymbol() == "none"
          || player.getPlayerTurn().getAttackingSymbol() == player.getSelectedHandCards().get(0).getSymbol()) {
        player.getPlayerTurn()
            .setAttackingSymbol(player.getSelectedHandCards().get(0).getSymbol());
        defCard.setCovered(false);
        boolean attackSuccess = player.attackEnemyDefense(defCard);

        // selected hand cards to cemetery deck
        Iterator<Card> handCardIt = player.getHandCards().iterator();
        while (handCardIt.hasNext()) {
          Card currCard = handCardIt.next();
          if (currCard.isSelected()) {
            cemeteryDeck.addCard(currCard);
            handCardIt.remove();
          }
        }

        if (attackSuccess) {
          player.addHandCard(defCard);
          defCard.setRemoved(true);
        }

        //gameState.setUpdateState(true);
      }
    }
  }

}
