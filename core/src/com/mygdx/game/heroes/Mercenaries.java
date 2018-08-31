package com.mygdx.game.heroes;

public class Mercenaries extends Hero {

  // (0) ready (1) in use (2) dead
  private int[] mercenaryStates = { 0, 0, 0, 0, 2, 2, 2, 2 };

  public Mercenaries() {
    heroName = "Mercenaries";
    heroID = "Merc.";
    sprite = atlas.createSprite("wp", -1);

    isSelected = false;
    isSelectable = true;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  @Override
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
    if (isAvailable()) {
      isReady = true;
    }
  }

  public int countReady() {
    int readyCount = 0;
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 0) {
        readyCount++;
      }
    }
    return readyCount;
  }

  public boolean isAvailable() {
    boolean isAvailable = false;
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 0) {
        isAvailable = true;
        break;
      }
    }
    return isAvailable;
  }

  public void callback() {
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 1) {
        mercenaryStates[i] = 0;
        break;
      }
    }
    if (isAvailable()) {
      isReady = true;
    }
  }

  public void destroy() {
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 1) {
        mercenaryStates[i] = 2;
        break;
      }
    }
  }

  public void operate() {
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 0) {
        mercenaryStates[i] = 1;
        break;
      }
    }
    if (!isAvailable()) {
      isSelected = false;
      isReady = false;
    }
  }

}
