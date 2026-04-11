package com.mygdx.game.heroes;

public class Spy extends Hero {
  private int spyAttacks;
  private int spyMaxAttacks;
  private int spyExtends;

  public Spy() {
    heroName = "Spy";
    heroID = "Spy";
    sprite = atlas.createSprite("wb", -1);

    spyAttacks = 1;
    spyMaxAttacks = 1;
    spyExtends = 1;

    isSelectable = true;
    isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }

  @Override
  public void recover() {
    spyAttacks = 1;
    spyMaxAttacks = 1;
    spyExtends = 1;
    isReady = true;
  }

  public int getSpyAttacks() {
    return spyAttacks;
  }

  public int getSpyMaxAttacks() {
    return spyMaxAttacks;
  }

  public int getSpyExtends() {
    return spyExtends;
  }

  public void setSpyAttacks(int spyAttacks) {
    this.spyAttacks = Math.max(0, spyAttacks);
  }

  public void setSpyMaxAttacks(int spyMaxAttacks) {
    this.spyMaxAttacks = Math.max(1, spyMaxAttacks);
  }

  public void setSpyExtends(int spyExtends) {
    this.spyExtends = Math.max(0, spyExtends);
    if (this.spyAttacks <= 0 && this.spyExtends <= 0) {
      isReady = false;
      isSelected = false;
    } else {
      isReady = true;
    }
  }

  public void spyExtend() {
    // sacrifice a card: spend the extend charge, gain +2 actions
    spyExtends--;
    spyAttacks += 2;
    spyMaxAttacks = Math.min(spyMaxAttacks + 2, 3);
  }

  public void spyAttack() {
    spyAttacks--;
    if (spyAttacks <= 0) {
      spyAttacks = 0;
      if (spyExtends == 0) {
        isReady = false;
      }
      isSelected = false; // auto-deselect when out of flip actions
    }
  }

}
