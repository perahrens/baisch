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
    // Issue #167: replenish up to 4 ready mercenaries each round, capped so that
    // ready + in-use never exceeds 8. Killed (dead) mercenaries are resurrected
    // as needed to reach the target ready count.
    int inUse = 0;
    int ready = 0;
    for (int i = 0; i < mercenaryStates.length; i++) {
      if (mercenaryStates[i] == 1) inUse++;
      else if (mercenaryStates[i] == 0) ready++;
    }
    int targetReady = Math.min(4, 8 - inUse);
    int needed = targetReady - ready;
    for (int i = 0; i < mercenaryStates.length && needed > 0; i++) {
      if (mercenaryStates[i] == 2) {
        mercenaryStates[i] = 0;
        needed--;
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
      isReady = false;
    }
  }

}
