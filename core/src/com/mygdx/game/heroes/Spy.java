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
      isReady = false;
      isSelected = false; // auto-deselect when out of actions
    }
  }

}
