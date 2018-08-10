package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.Player;
import com.mygdx.heroes.FortifiedTower;

public class OwnDefCardListener extends ClickListener {

  // cards of current player
  Card selectedCard;
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  ArrayList<Player> players;
  Player player;

  public OwnDefCardListener() {
  }

  public OwnDefCardListener(Card selectedCard, Card kingCard, Map<Integer, Card> defCards, ArrayList<Card> handCards,
      Player player, ArrayList<Player> players) {
    this.selectedCard = selectedCard;
    this.kingCard = kingCard;
    this.defCards = defCards;
    this.handCards = handCards;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    // if F.Tower and hand card is selected, put hand card on top
    if (player.getSelectedHeroes().size() > 0) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Fortified Tower" && player.getHeroes().get(i).isSelected()
            && player.getSelectedHandCards().size() == 1 && selectedCard.getLevel() == 0) {
          System.out.println("Do F.Tower defend");
          Card handCard = player.getSelectedHandCards().get(0);
          FortifiedTower fortifiedTower = (FortifiedTower) player.getHeroes().get(i);
          if (fortifiedTower.getDefenseExpands() > 0 && handCard.getSymbol() == selectedCard.getSymbol()) {
            fortifiedTower.defenseExpand();
            handCard.setLevel(1);
            player.putDefCard(selectedCard.getPositionId(), 1);
          }
        }
      }
    } else {
      // unselect all handcards
      for (int i = 0; i < handCards.size(); i++) {
        handCards.get(i).setSelected(false);
      }

      // select defense card
      if (selectedCard.isSelected()) {
        selectedCard.setSelected(false);
      } else {
        kingCard.setSelected(false);
        for (int i = 1; i <= 3; i++) {
          if (defCards.containsKey(i)) {
            defCards.get(i).setSelected(false);
          }
        }
        selectedCard.setSelected(true);
      }

    }
  };

}
