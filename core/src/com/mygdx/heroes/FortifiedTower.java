package com.mygdx.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class FortifiedTower extends Hero {
	
	public FortifiedTower() {
		heroName = "Fortified Tower";
		heroID = "F. Tow.";
		sprite = atlas.createSprite("br", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
}
