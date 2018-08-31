package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.heroes.FortifiedTower;
import com.mygdx.game.heroes.Mercenaries;

public class OwnDefCardListener extends ClickListener {

  // cards of current player
  GameState gameState;
  Card selectedCard;
  Card kingCard;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  Map<Integer, Card> topDefCards;
  ArrayList<Player> players;
  Player player;

  public OwnDefCardListener() {
  }

  public OwnDefCardListener(GameState gameState, Card selectedCard, Card kingCard, Map<Integer, Card> defCards,
      Map<Integer, Card> topDefCards, ArrayList<Card> handCards, Player player, ArrayList<Player> players) {
    this.gameState = gameState;
    this.selectedCard = selectedCard;
    this.kingCard = kingCard;
    this.defCards = defCards;
    this.topDefCards = topDefCards;
    this.handCards = handCards;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    if (!selectedCard.isSabotaged()) {
      // if F.Tower and hand card is selected, put hand card on top
      if (player.getSelectedHeroes().size() > 0) {
        for (int i = 0; i < player.getHeroes().size(); i++) {
          if (player.getHeroes().get(i).getHeroName() == "Fortified Tower" && player.getHeroes().get(i).isSelected()
              && player.getSelectedHandCards().size() == 1 && selectedCard.getLevel() == 0) {
            Card handCard = player.getSelectedHandCards().get(0);
            FortifiedTower fortifiedTower = (FortifiedTower) player.getHeroes().get(i);
            if (fortifiedTower.getDefenseExpands() > 0 && handCard.getSymbol() == selectedCard.getSymbol()) {
              fortifiedTower.defenseExpand();
              handCard.setLevel(1);
              player.putDefCard(selectedCard.getPositionId(), 1);
            }
          } else if (player.getHeroes().get(i).getHeroName() == "Mercenaries"
              && player.getHeroes().get(i).isSelected()) {
            Mercenaries mercenaries = (Mercenaries) player.getHeroes().get(i);
            if (mercenaries.isAvailable()) {
              mercenaries.operate();
              selectedCard.addBoosted(1);
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
            if (topDefCards.containsKey(i)) {
              topDefCards.get(i).setSelected(false);
            }
          }
          selectedCard.setSelected(true);
        }

      }
    }

    // gameState.setUpdateState(true);

  };

}
