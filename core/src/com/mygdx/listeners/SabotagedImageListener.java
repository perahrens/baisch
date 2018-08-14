package com.mygdx.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.heroes.Saboteurs;

public class SabotagedImageListener extends ClickListener {

  GameState gameState;
  Card defCard; // sabotaged def card
  Player player;

  public SabotagedImageListener(GameState gameState, Card defCard, Player player) {
    this.gameState = gameState;
    this.defCard = defCard;
    this.player = player;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {

    // is player owner of hero
    if (player.hasHero("Saboteurs")) {
      for (int i = 0; i < player.getHeroes().size(); i++) {
        if (player.getHeroes().get(i).getHeroName() == "Saboteurs") {
          Saboteurs saboteurs = (Saboteurs) player.getHeroes().get(i);
          saboteurs.callback();
          defCard.setSabotaged(false);
          break;
        }
      }
    } else {
      // if not, blow up card with saboteur
      ArrayList<Player> players = gameState.getPlayers();
      CardDeck cemeteryDeck = gameState.getCemeteryDeck();
      for (int i = 0; i < players.size(); i++) {
        if (players.get(i).hasHero("Saboteurs")) {
          for (int j = 0; j < players.get(i).getHeroes().size(); j++) {
            if (players.get(i).getHeroes().get(j).getHeroName() == "Saboteurs") {
              Saboteurs saboteurs = (Saboteurs) players.get(i).getHeroes().get(j);
              saboteurs.destroy();
              cemeteryDeck.addCard(defCard);
              defCard.setRemoved(true);
              break;
            }
          }
        }
      }
    }

    gameState.setUpdateState(true);
  };

}
