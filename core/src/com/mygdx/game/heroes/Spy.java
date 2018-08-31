package com.mygdx.game.heroes;

public class Spy extends Hero {
  private int spyAttacks;
  private int spyExtends;

  public Spy() {
    heroName = "Spy";
    heroID = "Spy";
    sprite = atlas.createSprite("wb", -1);

    spyAttacks = 1;
    spyExtends = 1;

    isSelectable = true;
    isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }

  @Override
  public void recover() {
    spyAttacks = 1;
    spyExtends = 1;
    isReady = true;
  }

  public int getSpyAttacks() {
    return spyAttacks;
  }

  public int getSpyExtends() {
    return spyExtends;
  }

  public void spyExtend() {
    // when hand card is casted away
    spyExtends--;
    spyAttacks++;
    spyAttacks++;
  }

  public void spyAttack() {
    spyAttacks--;
    if (spyAttacks == 0 && spyExtends == 0) {
      isReady = false;
      isSelected = false;
    }
  }

}
