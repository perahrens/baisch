package com.mygdx.game.heroes;

public class FortifiedTower extends Hero {
  private int defenseExpands = 1;

  public FortifiedTower() {
    heroName = "Fortified Tower";
    heroID = "F. Tow.";
    sprite = atlas.createSprite("br", -1);

    isSelected = false;
    isSelectable = true;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  @Override
  public void recover() {
    defenseExpands = 1;
    isSelectable = true;
  }

  public int getDefenseExpands() {
    return defenseExpands;
  }

  public void defenseExpand() {
    defenseExpands--;
    if (defenseExpands == 0) {
      isSelectable = false;
      isSelected = false;
    }
  }

}
