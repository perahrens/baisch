package com.mygdx.game.heroes;

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

  @Override
  public void recover() {
    for (int i = 0; i < saboteurStates.length; i++) {
      if (saboteurStates[i] == 2)
        saboteurStates[i] = 3; // repair saboteur (was using index 0 — bug fixed)
      else if (saboteurStates[i] == 3)
        saboteurStates[i] = 0; // provide repaired saboteur
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

  /** How many saboteurs are currently ready (state 0). Used for the x/2 indicator. */
  public int countReady() {
    int count = 0;
    for (int state : saboteurStates) {
      if (state == 0) count++;
    }
    return count;
  }

  /**
   * Sync hero state from server-authoritative active count.
   * Called during applyStateUpdate to reconcile deployed saboteurs.
   */
  public void syncFromActiveCount(int active) {
    for (int i = 0; i < saboteurStates.length; i++) saboteurStates[i] = 0;
    int marked = 0;
    for (int i = 0; i < saboteurStates.length && marked < active; i++) {
      saboteurStates[i] = 1;
      marked++;
    }
    isReady = isAvailable();
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
