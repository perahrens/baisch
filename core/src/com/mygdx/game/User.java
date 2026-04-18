package com.mygdx.game;

public class User {

  String userID;
  String name;
  boolean isReady;
  
  public User(String userID, String name) {
    this.userID = userID;
    this.name = name;
    this.isReady = false;
  }
  
  public String getUserID() {
    return userID;
  }

  public String getName() {
    return name;
  }
  
  public boolean isReady() {
    return isReady;
  }
  
  public void setReady(boolean isReady) {
    this.isReady = isReady;
  }

  String selectedHero;

  public String getSelectedHero() {
    return selectedHero != null ? selectedHero : "None";
  }

  public void setSelectedHero(String selectedHero) {
    this.selectedHero = selectedHero;
  }


}
