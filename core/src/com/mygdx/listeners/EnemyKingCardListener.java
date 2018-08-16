package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.heroes.Hero;
import com.mygdx.heroes.Spy;

public class EnemyKingCardListener extends ClickListener {

  GameState gameState;
  Card kingCard;
  Player player;
  ArrayList<Player> players;

  public EnemyKingCardListener() {
  }

  public EnemyKingCardListener(GameState gameState, Card kingCard, Player player, ArrayList<Player> players) {
    this.gameState = gameState;
    this.kingCard = kingCard;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    CardDeck cemeteryDeck = gameState.getCemeteryDeck();

    if (player.getSelectedHeroes().size() > 0) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Spy" && player.getHeroes().get(i).isSelected()) {
          Spy spy = (Spy) player.getHeroes().get(i);
          // check if all def cards are uncovered
          // first find player
          Player targetPlayer = players.get(0);

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
    } else {
      // attack enemy defense
      if (player.getSelectedHandCards().size() > 0) {
        if (player.getPlayerTurn().getAttackingSymbol()[0] == "none"
            || player.getPlayerTurn().getAttackingSymbol()[0] == player.getSelectedHandCards().get(0).getSymbol()
            || player.getPlayerTurn().getAttackingSymbol()[1] == player.getSelectedHandCards().get(0).getSymbol()) {
          player.getPlayerTurn().setAttackingSymbol(player.getSelectedHandCards().get(0).getSymbol(),
              player.hasHero("Lieutenant"));

          // check if defense card is fortified
          boolean attackSuccess;
          kingCard.setCovered(false);
          attackSuccess = player.attackEnemyDefense(kingCard);

          // selected hand cards to cemetery deck
          Iterator<Card> handCardIt = player.getHandCards().iterator();
          while (handCardIt.hasNext()) {
            Card currCard = handCardIt.next();
            if (currCard.isSelected()) {
              cemeteryDeck.addCard(currCard);
              handCardIt.remove();
            }
          }

          // if attack successul, other player is dead
          if (attackSuccess) {
            // find the owning player
            Player deadPlayer = gameState.getPlayers().get(0);
            for (int i = 0; i < gameState.getPlayers().size(); i++) {
              if (gameState.getPlayers().get(i).getKingCard() == kingCard ) {
                deadPlayer = gameState.getPlayers().get(i);
                break;
              }
            }

            // player gets king card
            player.addHandCard(kingCard);
            kingCard.setRemoved(true);

            // player gets all hand cards
            Iterator<Card> deadHandCardIt = deadPlayer.getHandCards().iterator();
            while (deadHandCardIt.hasNext()) {
              Card currCard = deadHandCardIt.next();
              player.addHandCard(currCard);
              deadHandCardIt.remove();
            }

            //TODO should be free hero choice
            // player gets one hero
            Iterator<Hero> deadHeroIt = deadPlayer.getHeroes().iterator();
            while(deadHeroIt.hasNext()) {
              Hero currHero = deadHeroIt.next();
              player.addHero(currHero);
              deadHeroIt.remove();
              break;
            }
            
            deadPlayer.setDead();

          }

          // gameState.setUpdateState(true);
        }
      }
    }
  }

}
