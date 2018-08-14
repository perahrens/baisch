package com.mygdx.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Mercenaries extends Hero {

  // (0) ready (1) defending (2) dead
  private int[] mercenaryStates = { 0, 0, 0, 0, 2, 2, 2, 2 };

  public Mercenaries() {
    heroName = "Mercenaries";
    heroID = "Merc.";
    sprite = atlas.createSprite("wp", -1);

    this.isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  public void recover() {
    int heals = 4;
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 2) {
        mercenaryStates[i] = 0;
        heals--;
      }
      if (heals == 0) {
        break;
      }
    }
  }

}
