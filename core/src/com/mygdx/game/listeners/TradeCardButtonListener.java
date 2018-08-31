package com.mygdx.game.listeners;

import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.Player;

public class TradeCardButtonListener extends ClickListener {

  Card tradeableCard;
  Player player;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;

  public TradeCardButtonListener() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    Iterator<Card> handCardsIt = player.getHandCards().iterator();
    while (handCardsIt.hasNext()) {
      Card handCard = handCardsIt.next();
      if (handCard.isTradeable()) {
        System.out.println("Remove tradeable card");
        cemeteryDeck.addCard(handCard);
        handCardsIt.remove();
      }
    }

    Card newCard = cardDeck.getCard(cemeteryDeck);
    player.addHandCard(newCard);
    tradeableCard.setTradable(false);
    // gameState.setUpdateState(true);
  }

}
