package com.mygdx.game;

import java.util.ArrayList;

public class MenuState {

  private String myUserID;
  private ArrayList<User> users;
  private int timeToStart;

  public MenuState() {
    users = new ArrayList<User>();
  }

  public void clearUsers() {
    users = new ArrayList<User>();
  }

  public void addUser(User user) {
    users.add(user);
  }

  public ArrayList<User> getUsers() {
    return users;
  }

  public void setMyUserID(String myUserID) {
    this.myUserID = myUserID;
  }

  public String getMyUserID() {
    return myUserID;
  }

  public boolean allReady() {
    boolean allReady = true;
    if (users.size() == 0) allReady = false;
    for (int i = 0; i < users.size(); i++) {
      if (!users.get(i).isReady()) {
        allReady = false;
        break;
      }
    }
    return allReady;
  }

  public void setTimeToStart(int timeToStart) {
    this.timeToStart = timeToStart;
  }

  public int getTimeToStart() {
    return timeToStart;
  }

  public void runTimer() {
    timeToStart--;
  }
  
}
