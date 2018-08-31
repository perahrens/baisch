package com.mygdx.game;

import java.util.ArrayList;

public class MenuState {

  private ArrayList<Player> players;
  
  public MenuState() {
    players = new ArrayList<Player>();
  }
  
  public void addPlayer(Player player) {
    players.add(player);
  }
  
  public ArrayList<Player> getPlayers() {
    return players;
  }
  
}
