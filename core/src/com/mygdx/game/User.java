package com.mygdx.game;

public class User {

  String userID;
  boolean isReady;
  
  public User(String userID) {
    this.userID = userID;
    this.isReady = false;
  }
  
  public String getUserID() {
    return userID;
  }
  
  public boolean isReady() {
    return isReady;
  }
  
  public void setReady(boolean isReady) {
    this.isReady = isReady;
  }
  
  
}
