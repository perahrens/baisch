package com.mygdx.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.MyGdxGame;

public class Magician extends Hero {
	private int spells = 1;
	
	public Magician() {
		heroName = "Magician";
		heroID = "Magic.";
		sprite = atlas.createSprite("bq", -1);

		isSelected = false;
		isSelectable = true;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);

	}
	
	public void recover() {
		spells = 1;
		isSelectable = true;
	}
	
	public void castSpell() {
		spells--;
		if (spells == 0) {
			isSelectable = false;
			isSelected = false;
		}
	}
	
	public int getSpells() {
		return spells;
	}

}
