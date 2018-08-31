package com.mygdx.game.listeners;

import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.HeroesSquare;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.Hero;

public class CemeteryDeckListener extends ClickListener {

  GameState gameState;

  public CemeteryDeckListener(GameState gameState) {
    this.gameState = gameState;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    System.out.println("Cemetery");
    Player currentPlayer = gameState.getCurrentPlayer();
    CardDeck cardDeck = gameState.getCardDeck();
    CardDeck cemeteryDeck = gameState.getCemeteryDeck();
    HeroesSquare heroesSquare = gameState.getHeroesSquare();
    if (currentPlayer.getSelectedHandCards().size() > 0) {
      Iterator<Card> handCardIt = currentPlayer.getHandCards().iterator();
      while (handCardIt.hasNext()) {
        Card currCard = handCardIt.next();
        if (currCard.isSelected()) {
          System.out.println("Remove handcard " + currCard.getStrength());
          cemeteryDeck.addCard(currCard);
          // if joker, get hero
          if (currCard.getSymbol() == "joker") {
            System.out.println("Get hero");
            Card heroCard = cardDeck.getCard(cemeteryDeck);
            System.out.println("Hero card is " + heroCard.getStrength());
            Hero hero = heroesSquare.getHero(heroCard.getStrength());
            if (hero != null) {
              currentPlayer.addHero(hero);
            }
            cemeteryDeck.addCard(heroCard);
          }
          handCardIt.remove();
          gameState.setUpdateState(true);
        }
      }
    }
  }

}
