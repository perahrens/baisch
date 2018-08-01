package com.mygdx.game.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.MyGdxGame;

public class Spy extends Hero {
	private int spyAttacks;
	private int spyExtends;
	
	public Spy() {
		heroName = "Spy";
		heroID = "Spy";
		sprite = atlas.createSprite("wb", -1);

		spyAttacks = 1;
		spyExtends = 1;
		
		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
	}
	
	public void recover() {
		spyAttacks = 1;
		spyExtends = 1;
		isReady = true;
	}
	
	public int getSpyAttacks() {
		return spyAttacks;
	}
	
	public int getSpyExtends() {
		return spyExtends;
	}
	
	public void spyExtend() {
		//when hand card is casted away
		spyExtends--;
		spyAttacks++;
		spyAttacks++;
	}
	
	public void spyAttack() {
		spyAttacks--;
		if (spyAttacks == 0 && spyExtends == 0) {
			isReady = false;
			isSelected = false;
		}
	}

}
