package com.mygdx.listeners;

import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.Player;
import com.mygdx.heroes.Merchant;
import com.mygdx.heroes.Spy;

public class OwnHandCardListener extends ClickListener {

  Card handCard;
  Player player;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;

  public OwnHandCardListener() {

  }

  public OwnHandCardListener(Card handCard, Player player, CardDeck cardDeck, CardDeck cemeteryDeck) {
    this.handCard = handCard;
    this.player = player;
    this.cardDeck = cardDeck;
    this.cemeteryDeck = cemeteryDeck;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    // unselect all defense and king cards
    player.getKingCard().setSelected(false);
    for (int i = 1; i <= 3; i++) {
      if (player.getDefCards().containsKey(i)) {
        player.getDefCards().get(i).setSelected(false);
      }
      if (player.getTopDefCards().containsKey(i)) {
        player.getTopDefCards().get(i).setSelected(false);
      }
    }

    // select hand card
    if (handCard.isSelected()) {
      handCard.setSelected(false);
    } else {
      if (handCard.getSymbol() == player.getSelectedSymbol()) {
        handCard.setSelected(true);
      } else {
        for (int i = 0; i < player.getHandCards().size(); i++) {
          player.getHandCards().get(i).setSelected(false);
        }
        handCard.setSelected(true);
        player.setSelectedSymbol(handCard.getSymbol());
      }
    }

    // check hero functions on hand cards
    if (player.getSelectedHeroes().size() > 0) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        // if spy is selected, cast card away
        if (player.getHeroes().get(i).getHeroName() == "Spy" && player.getHeroes().get(i).isSelected()) {
          Spy spy = (Spy) player.getHeroes().get(i);
          if (player.getSelectedHandCards().size() == 1 && spy.getSpyExtends() > 0) {
            // cast away selected card
            Iterator<Card> handCardIt = player.getHandCards().iterator();
            while (handCardIt.hasNext()) {
              Card currCard = handCardIt.next();
              if (currCard.isSelected()) {
                System.out.println("Remove handcard " + currCard.getStrength());
                cemeteryDeck.addCard(currCard);
                handCardIt.remove();
              }
            }

            // extends spy attacks
            spy.spyExtend();
          }
        } else if (player.getHeroes().get(i).getHeroName() == "Merchant" && player.getHeroes().get(i).isSelected()) {
          Merchant merchant = (Merchant) player.getHeroes().get(i);
          if (player.getSelectedHandCards().size() == 1 && merchant.getTrades() > 0) {
            Iterator<Card> handCardIt = player.getHandCards().iterator();
            while (handCardIt.hasNext()) {
              Card currCard = handCardIt.next();
              if (currCard.isSelected()) {
                System.out.println("Remove handcard " + currCard.getStrength());
                cemeteryDeck.addCard(currCard);
                handCardIt.remove();
              }
            }

            // get new card from deck
            merchant.trade();
            Card newCard = cardDeck.getCard(cemeteryDeck);
            player.addHandCard(newCard);

            newCard.setTradable(true);
          }
        }
      }
      // gameState.setUpdateState(true);
    }
  };

}
