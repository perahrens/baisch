package com.mygdx.game;

public class PlayerTurn {
	int pickingDeckAttacks;
	int attackCounter;
	int takeDefCard;
	int putDefCard;
	String attackingSymbol;
	
	public PlayerTurn () {
		pickingDeckAttacks = 1;
		attackCounter = 0;
		takeDefCard = 1;
		putDefCard = 1;
		attackingSymbol = "none";
	}
	
	public void decreasePickingDeckAttacks () {
		pickingDeckAttacks -= 1;
	}
	
	public int getPickingDeckAttacks() {
		return pickingDeckAttacks;
	}
	
	public void increaseAttackCounter () {
		attackCounter += 1;
	}
	
	public int getAttackCounter () {
		return attackCounter;
	}
	
	public void decreaseTakeDefCard () {
		takeDefCard -= 1;
	}
	
	public int getTakeDefCard () {
		return takeDefCard;
	}
	
	public void decreasePutDefCard () {
		putDefCard -= 1;
	}
	
	public int getPutDefCard () {
		return putDefCard;
	}
	
	public void setAttackingSymbol(String symbol) {
		if (symbol != "joker") { 
			attackingSymbol = symbol;
		}
	}
	
	public String getAttackingSymbol () {
		return attackingSymbol;
	}

}
