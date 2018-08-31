package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.King;
import com.mygdx.game.heroes.Magician;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.heroes.Spy;

public class EnemyDefCardListener extends ClickListener {

  Card defCard;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;
  ArrayList<Player> players;
  Player player;

  public EnemyDefCardListener() {
  }

  public EnemyDefCardListener(Card defCard, CardDeck cardDeck, CardDeck cemeteryDeck, Player player,
      ArrayList<Player> players) {
    this.defCard = defCard;
    this.cardDeck = cardDeck;
    this.cemeteryDeck = cemeteryDeck;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    Map<Integer, Card> defCards = players.get(0).getDefCards();
    Map<Integer, Card> topDefCards = players.get(0).getTopDefCards();
    for (int p = 0; p < players.size(); p++) {
      for (int c = 1; c <= 3; c++) {
        if (players.get(p).getDefCards().containsKey(c)) { // || players.get(p).getTopDefCards().containsKey(c)) {
          if (players.get(p).getDefCards().get(c) == defCard) {
            defCards = players.get(p).getDefCards();
            if (players.get(p).getTopDefCards().containsKey(c)) {
              topDefCards = players.get(p).getTopDefCards();
            }
          } else if (players.get(p).getTopDefCards().containsKey(c)) {
            if (players.get(p).getTopDefCards().get(c) == defCard) {
              defCards = players.get(p).getDefCards();
              topDefCards = players.get(p).getTopDefCards();
            }
          }
        }
      }
    }

    if (player.getSelectedHeroes().size() > 0) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Spy" && player.getHeroes().get(i).isSelected()) {
          Spy spy = (Spy) player.getHeroes().get(i);
          if (spy.getSpyAttacks() > 0) {
            spy.spyAttack();
            System.out.println("Number spy attacks left = " + spy.getSpyAttacks());
            defCard.setCovered(false);
          }
        } else if (player.getHeroes().get(i).getHeroName() == "Saboteurs" && player.getHeroes().get(i).isSelected()) {
          Saboteurs saboteurs = (Saboteurs) player.getHeroes().get(i);
          if (saboteurs.isAvailable() && !defCard.isSabotaged()) {
            saboteurs.sabotage();
            defCard.setSabotaged(true);
          }
        } else if (player.getHeroes().get(i).getHeroName() == "Magician" && player.getHeroes().get(i).isSelected()) {
          Magician magician = (Magician) player.getHeroes().get(i);
          if (magician.getSpells() > 0) {
            magician.castSpell();

            // loop over all def cards of player
            for (int p = 0; p < players.size(); p++) {
              for (int c = 1; c <= 3; c++) {
                if (players.get(p).getDefCards().containsKey(c)) {
                  Card cDefCard = players.get(p).getDefCards().get(c);
                  Card cTopDefCard = players.get(p).getTopDefCards().get(c);
                  if (cDefCard == defCard) {
                    Card newDefCard = cardDeck.getCard(cemeteryDeck);
                    newDefCard.setCovered(!defCard.isCovered());
                    cemeteryDeck.addCard(defCard);
                    players.get(p).getDefCards().remove(c);
                    players.get(p).addDefCard(c, newDefCard, 0);
                    // gameState.setUpdateState(true);
                  }
                  if (cTopDefCard == defCard) {
                    Card newDefCard = cardDeck.getCard(cemeteryDeck);
                    newDefCard.setCovered(!defCard.isCovered());
                    cemeteryDeck.addCard(defCard);
                    players.get(p).getTopDefCards().remove(c);
                    players.get(p).addDefCard(c, newDefCard, 1);
                    // gameState.setUpdateState(true);
                  }
                }
              }
            }
          }
        } else if (player.getHeroes().get(i).getHeroName() == "King" && player.getKingCard().isSelected()
            && (player.getKingCard().getSymbol() == player.getPlayerTurn().getAttackingSymbol()[0]
                || player.getKingCard().getSymbol() == player.getPlayerTurn().getAttackingSymbol()[1]
                || player.getPlayerTurn().getAttackingSymbol()[0] == "none")) {
          System.out.println("king attack");
          King king = (King) player.getHeroes().get(i);
          king.royalAttack();
          player.getPlayerTurn().setAttackingSymbol(player.getKingCard().getSymbol(), player.hasHero("Lieutenant"));
          // cover up hero and enemy defense card
          defCard.setCovered(false);
          player.getKingCard().setCovered(false);

          // check if defense card is fortified
          int attackResult;
          int positionId = defCard.getPositionId();
          Card topDefCard = null;
          if (topDefCards.containsKey(positionId)) {
            defCard = defCards.get(positionId);
            topDefCard = topDefCards.get(positionId);

            defCard.setCovered(false);
            topDefCard.setCovered(false);
            attackResult = player.royalAttack(defCard, topDefCard);
          } else {
            defCard.setCovered(false);
            attackResult = player.royalAttack(defCard);
          }

          if (attackResult == 2) {
            player.addHandCard(defCard);
            defCard.setRemoved(true);
            if (topDefCard != null) {
              player.addHandCard(topDefCard);
              topDefCard.setRemoved(true);
            }
          } else if (attackResult == 1) {
            // nothing happens
          } else {
            // TODO player lost
          }

          // gameState.setUpdateState(true);
        }
      }
    }

    // attack enemy defense
    if (player.getSelectedHandCards().size() > 0) {
      if (player.getPlayerTurn().getAttackingSymbol()[0] == "none"
          || player.getPlayerTurn().getAttackingSymbol()[0] == player.getSelectedHandCards().get(0).getSymbol()
          || player.getPlayerTurn().getAttackingSymbol()[1] == player.getSelectedHandCards().get(0).getSymbol()) {
        player.getPlayerTurn().setAttackingSymbol(player.getSelectedHandCards().get(0).getSymbol(),
            player.hasHero("Lieutenant"));

        // check if defense card is fortified
        boolean attackSuccess;
        int positionId = defCard.getPositionId();
        Card topDefCard = null;
        if (topDefCards.containsKey(positionId)) {
          defCard = defCards.get(positionId);
          topDefCard = topDefCards.get(positionId);

          defCard.setCovered(false);
          topDefCard.setCovered(false);
          attackSuccess = player.attackEnemyDefense(defCard, topDefCard);
        } else {
          defCard.setCovered(false);
          attackSuccess = player.attackEnemyDefense(defCard);
        }

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
          if (topDefCard != null) {
            player.addHandCard(topDefCard);
            topDefCard.setRemoved(true);
          }
        }

        // gameState.setUpdateState(true);
      }
    }
  }

}
