package com.mygdx.game.heroes;

public class Warlord extends Hero {

  /*
   * The Warlord (Black King figure) provides two abilities:
   *
   * 1. Direct King Attack — 1/1 charge per turn:
   *    Select Warlord, then click an enemy defense card to attack it with the king.
   *    This bypasses the normal restriction that requires the attacker to have no
   *    own defense cards. The king must still match the current attack symbol.
   *    Cannot be combined with hand cards.
   *
   * 2. King Swap — costs 1 take + 1 put action:
   *    Select the own king card, then click a hand card to swap them.
   *    The hand card becomes the new king; the old king goes to hand.
   */

  private int attacks = 1;

  public Warlord() {
    heroName = "Warlord";
    heroID = "Warl.";
    sprite = atlas.createSprite("bk", -1);

    isSelected = false;
    isSelectable = true;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }

  public int getAttacks() { return attacks; }

  public void setAttacks(int attacks) {
    this.attacks = Math.max(0, attacks);
    if (this.attacks > 0) {
      isSelectable = true;
    } else {
      isSelectable = false;
      isSelected = false;
    }
  }

  public boolean isAttackAvailable() { return attacks > 0; }

  /** Spend the Warlord's attack charge for this turn. */
  public void useAttack() {
    attacks--;
    if (attacks == 0) {
      isSelectable = false;
      isSelected = false;
    }
  }

  @Override
  public void recover() {
    attacks = 1;
    isSelectable = true;
    isReady = true;
  }
}
