package com.mygdx.game.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class BatteryTower extends Hero {
	
	public BatteryTower() {
		heroName = "Battery Tower";
		heroID = "B. Tow.";
		sprite = atlas.createSprite("wr", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
	}
}
