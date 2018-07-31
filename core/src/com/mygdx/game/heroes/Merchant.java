package com.mygdx.game.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.MyGdxGame;

public class Merchant extends Hero {
	private int trades;

	public Merchant() {
		heroName = "Merchant";
		heroID = "Merch.";
		sprite = atlas.createSprite("wq", -1);

		this.isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
	
	/* if hero is selected and handcard is selected
	 *  cast away handcard and get new handcard from deck
	 *  decrement number of trades
	 *  if last trade, then make card visible for all players 
	 */ 
	public void trade() {
		
	}

}
