package com.mygdx.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Priest extends Hero {
	private int conversionAttempts = 2;
	
	public Priest() {
		heroName = "Priest";
		heroID = "Priest";
		sprite = atlas.createSprite("wk", -1);

		isSelected = false;
		isSelectable = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
	
	public void recover () {
		conversionAttempts = 2;
	}
	
	public void conversionAttempt () {
		conversionAttempts--;
		if (conversionAttempts == 0) {
			isSelectable = false;
			isSelected = false;
		}
	}
	
	public int getConversionAttempts() {
		return conversionAttempts;
	}
	
	public void conversion() {
		conversionAttempts = 0;
	}
	
}
