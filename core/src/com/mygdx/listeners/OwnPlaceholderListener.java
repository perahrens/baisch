package com.mygdx.listeners;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.Player;
import com.mygdx.game.PlayerTurn;

public class OwnPlaceholderListener extends ClickListener {

  //objects of current player
  Card placeholderCard;
  Player player;
  
  public OwnPlaceholderListener() {
  }

  public OwnPlaceholderListener(Card placeholderCard, Player player) {
    this.placeholderCard = placeholderCard;
    this.player = player;
  }
  
  @Override
  public void clicked(InputEvent event, float x, float y) {
    // get selected hand cards
    if (player.getSelectedHandCards().size() == 1) {
      if (player.getPlayerTurn().getPutDefCard() > 0) {
        player.putDefCard(placeholderCard.getPositionId(), 0);
        //show();
      } else {
        System.out.println("no more put allowed");
      }
    } else {
      System.out.println("Select only one handcard");
    }
  }

}
