package com.mygdx.game;

public class User {

  String userID;
  String name;
  boolean isReady;
  boolean isHost;
  
  public User(String userID, String name) {
    this.userID = userID;
    this.name = name;
    this.isReady = false;
    this.isHost = false;
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

  public boolean isHost() {
    return isHost;
  }

  public void setHost(boolean isHost) {
    this.isHost = isHost;
  }
  
  
}
