package com.mygdx.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Reservists extends Hero {
	public Reservists() {
		heroName = "Reservists";
		heroID = "Reser.";
		sprite = atlas.createSprite("bp", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
}
