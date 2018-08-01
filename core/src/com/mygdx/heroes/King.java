package com.mygdx.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class King extends Hero {
	
	public King() {
		heroName = "King";
		heroID = "King";
		sprite = atlas.createSprite("bk", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);

	}
}
