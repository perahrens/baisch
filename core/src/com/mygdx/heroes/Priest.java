package com.mygdx.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Priest extends Hero {
	
	public Priest() {
		heroName = "Priest";
		heroID = "Priest";
		sprite = atlas.createSprite("wk", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
}
