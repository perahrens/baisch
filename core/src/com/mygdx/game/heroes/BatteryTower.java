package com.mygdx.game.heroes;

public class BatteryTower extends Hero {

  /*
   * When a defense card or the king of the owning player is attacked, the owner
   * can spend 1 charge to deny the attack: the attacker's hand cards are locked
   * for the rest of the turn and their attack cards are revealed.
   * Charges refill to 1 at the start of each new turn.
   */

  private int charges;

  public BatteryTower() {
    heroName = "Battery Tower";
    heroID = "B. Tow.";
    sprite = atlas.createSprite("wr", -1);

    this.isSelected = false;
    this.isSelectable = false;
    charges = 1;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }

  public int getCharges() {
    return charges;
  }

  /** Spend one charge to deny an attack. */
  public void fire() {
    if (charges > 0) charges--;
  }

  @Override
  public void recover() {
    isReady = true;
    charges = 1;
  }
}
