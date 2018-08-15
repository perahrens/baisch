package com.mygdx.heroes;

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
    if (isAvailable()) {
      isReady = true;
    }
  }

  // is one saboteur ready to use
  public boolean isAvailable() {
    boolean isAvailable = false;
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 0) {
        isAvailable = true;
        break;
      }
    }
    return isAvailable;
  }

  public void callback() {
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 1) {
        saboteurStates[i] = 0;
        break;
      }
    }
    if (isAvailable()) {
      isReady = true;
    }
  }

  public void destroy() {
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 1) {
        saboteurStates[i] = 2;
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
    if (!isAvailable()) {
      isSelected = false;
      isReady = false;
    }
  }

}
