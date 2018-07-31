package com.mygdx.game.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Saboteurs extends Hero {
	
	public Saboteurs() {
		heroName = "Saboteurs";
		heroID = "Sabot.";
		sprite = atlas.createSprite("bb", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
}
