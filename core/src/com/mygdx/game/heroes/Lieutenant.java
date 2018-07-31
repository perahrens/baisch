package com.mygdx.game.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Lieutenant extends Hero {
	
	public Lieutenant() {
		heroName = "Lieutenant";
		heroID = "Lieut.";
		sprite = atlas.createSprite("bk", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
}
