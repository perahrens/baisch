package com.mygdx.game;

public class PlayerTurn {
  int pickingDeckAttacks;
  int attackCounter;
  int takeDefCard;
  int putDefCard;
  String[] attackingSymbol = {"none","none"};

  public PlayerTurn() {
    pickingDeckAttacks = 1;
    attackCounter = 0;
    takeDefCard = 1;
    putDefCard = 1;
  }

  public void decreasePickingDeckAttacks() {
    pickingDeckAttacks -= 1;
  }

  public int getPickingDeckAttacks() {
    return pickingDeckAttacks;
  }

  public void increaseAttackCounter() {
    attackCounter += 1;
  }

  public int getAttackCounter() {
    return attackCounter;
  }

  public void decreaseTakeDefCard() {
    takeDefCard -= 1;
  }

  public int getTakeDefCard() {
    return takeDefCard;
  }

  public void decreasePutDefCard() {
    putDefCard -= 1;
  }

  public int getPutDefCard() {
    return putDefCard;
  }

  public void setAttackingSymbol(String symbol, boolean extended) {
    if (symbol != "joker") {
      attackingSymbol[0] = symbol;
      if (extended) {
        System.out.println("extended");
        if (symbol == "hearts") attackingSymbol[1] = "diamonds";
        if (symbol == "diamonds") attackingSymbol[1] = "hearts";
        if (symbol == "spades") attackingSymbol[1] = "clubs";
        if (symbol == "clubs") attackingSymbol[1] = "spades";
      }
    }
  }

  public String[] getAttackingSymbol() {
    return attackingSymbol;
  }

}
