package com.mygdx.heroes;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Saboteurs extends Hero {

  // (0) ready (1) active (2) destroyed (3) repaired
  private int[] saboteurStates = { 0, 0 };

  public Saboteurs() {
    heroName = "Saboteurs";
    heroID = "Sabot.";
    sprite = atlas.createSprite("bb", -1);

    isSelected = false;
    isSelectable = true;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }

  public void recover() {
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 2)
        saboteurStates[0] = 3; // repair saboteur
      else if (saboteurStates[i] == 3)
        saboteurStates[0] = 0; // provide repaired saboteur
    }
    isSelectable = true;
  }

  public boolean isReady() {
    boolean isReady = false;
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 0) {
        isReady = true;
        break;
      }
    }
    return isReady;
  }
  
  public void callback() {
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 1) {
        saboteurStates[i] = 0;
        break;
      }
    }
  }
  
  public void sabotage() {
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 0) {
        saboteurStates[i] = 1;
        break;
      }
    }
    if (!isReady()) {
      isSelected = false;
      isSelectable = false;
    }
  }

}
