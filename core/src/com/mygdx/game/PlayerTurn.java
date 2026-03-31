package com.mygdx.game;

import java.util.ArrayList;

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

  // --- Plunder preview state ---
  private boolean plunderPending = false;
  private boolean plunderSuccess = false;
  private ArrayList<Card> pendingAttackCards = new ArrayList<Card>();
  private int pendingPickingDeckIndex = -1;

  public boolean isPlunderPending() {
    return plunderPending;
  }

  public void setPlunderPending(boolean pending) {
    this.plunderPending = pending;
  }

  public boolean isPlunderSuccess() {
    return plunderSuccess;
  }

  public void setPlunderSuccess(boolean success) {
    this.plunderSuccess = success;
  }

  public ArrayList<Card> getPendingAttackCards() {
    return pendingAttackCards;
  }

  public void setPendingAttackCards(ArrayList<Card> cards) {
    this.pendingAttackCards = cards;
  }

  public int getPendingPickingDeckIndex() {
    return pendingPickingDeckIndex;
  }

  public void setPendingPickingDeckIndex(int index) {
    this.pendingPickingDeckIndex = index;
  }

  // --- Defense attack preview state ---
  private boolean attackPending = false;
  private boolean attackSuccess = false;
  private ArrayList<Card> pendingAttackDefCards = new ArrayList<Card>(); // [defCard] or [defCard, topDefCard]
  private int attackTargetPlayerIdx = -1;
  private int attackTargetPositionId = -1;
  private int attackTargetLevel = -1;

  public boolean isAttackPending() { return attackPending; }
  public void setAttackPending(boolean v) { this.attackPending = v; }
  public boolean isAttackSuccess() { return attackSuccess; }
  public void setAttackSuccess(boolean v) { this.attackSuccess = v; }
  public ArrayList<Card> getPendingAttackDefCards() { return pendingAttackDefCards; }
  public void setPendingAttackDefCards(ArrayList<Card> cards) { this.pendingAttackDefCards = cards; }
  public int getAttackTargetPlayerIdx() { return attackTargetPlayerIdx; }
  public void setAttackTargetPlayerIdx(int v) { this.attackTargetPlayerIdx = v; }
  public int getAttackTargetPositionId() { return attackTargetPositionId; }
  public void setAttackTargetPositionId(int v) { this.attackTargetPositionId = v; }
  public int getAttackTargetLevel() { return attackTargetLevel; }
  public void setAttackTargetLevel(int v) { this.attackTargetLevel = v; }

  // --- King attack flag ---
  private boolean kingUsed = false;
  public boolean isKingUsed() { return kingUsed; }
  public void setKingUsed(boolean v) { this.kingUsed = v; }

  // Tracks whether king was spent at any point this turn (never reset mid-turn)
  private boolean kingUsedThisTurn = false;
  public boolean isKingUsedThisTurn() { return kingUsedThisTurn; }
  public void setKingUsedThisTurn(boolean v) { this.kingUsedThisTurn = v; }

}
