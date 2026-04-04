package com.mygdx.game.heroes;

public class Reservists extends Hero {

  /*
   * The Reservists hero provides up to 4 reservist figures.
   * - Defense: all ready reservists contribute +1 each to king card defense strength automatically.
   * - Attack: after initiating an attack, click the Reservists button in the overlay to spend
   *   one reservist and add +1 to attack strength. Repeatable until none remain.
   * Max 4 reservists. Starts with 2/4 ready; recovers 2 per turn (capped at 4).
   */

  // 0 = ready, 2 = spent (locked until next recover)
  private int[] reservistStates = { 0, 0, 2, 2 };

  public Reservists() {
    heroName = "Reservists";
    heroID = "Res.";
    sprite = atlas.createSprite("bp", -1);

    this.isSelected = false;
    this.isSelectable = true;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }

  @Override
  public void recover() {
    int heals = 2;
    for (int i = 0; i < reservistStates.length; i++) {
      if (reservistStates[i] == 2) {
        reservistStates[i] = 0;
        heals--;
        if (heals == 0) break;
      }
    }
    if (isAvailable()) isReady = true;
  }

  public int countReady() {
    int count = 0;
    for (int s : reservistStates) if (s == 0) count++;
    return count;
  }

  public boolean isAvailable() {
    for (int s : reservistStates) if (s == 0) return true;
    return false;
  }

  /** Spend one reservist (marks as spent, recovered next turn). */
  public void spend() {
    for (int i = 0; i < reservistStates.length; i++) {
      if (reservistStates[i] == 0) {
        reservistStates[i] = 2;
        break;
      }
    }
    if (!isAvailable()) isReady = false;
  }

}
