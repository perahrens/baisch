package com.mygdx.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Major extends Hero {
	
	/*Major increases the number of put/take defense card actions
	 *to takeDefCard + setDefCard = 3.
	 */
	
	private int mobilizations;
	
	public Major() {
		heroName = "Major";
		heroID = "Major";
		sprite = atlas.createSprite("wn", -1);

		mobilizations = 3;
		
		isSelectable = false;
		isSelected = false;

		setWidth(sprite.getWidth()*scaleFactor);
		setHeight(sprite.getHeight()*scaleFactor);
		
	}
	
	public void mobilize () {
		mobilizations--;
	}
	
	public int getMobilizations () {
		return mobilizations;
	}
	
	public void recover() {
		isReady = true;
		mobilizations = 3;
	}
	
}
