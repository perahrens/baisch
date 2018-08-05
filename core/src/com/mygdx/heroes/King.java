package com.mygdx.heroes;

public class King extends Hero {
	private int royalAttacks = 1;
	
	public King() {
		heroName = "King";
		heroID = "King";
		sprite = atlas.createSprite("bk", -1);

		isSelected = false;
		isSelectable = true;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
	}
	
	public void recover() {
		royalAttacks = 1;
		isSelectable = true;
	}
	
	public int getRoyalAttacks() {
		return royalAttacks;
	}
	
	public void royalAttack() {
		royalAttacks--;
		if (royalAttacks == 0) {
			isSelectable = false;
			isSelected = false;
		}
	}
}
