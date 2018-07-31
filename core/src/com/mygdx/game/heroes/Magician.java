package com.mygdx.game.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.MyGdxGame;

public class Magician extends Hero {
	
	public Magician() {
		heroName = "Magician";
		heroID = "Magic.";
		sprite = atlas.createSprite("bq", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);

	}

}
