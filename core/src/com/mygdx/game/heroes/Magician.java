package com.mygdx.game.heroes;

public class Magician extends Hero {
  private int spells = 1;

  public Magician() {
    heroName = "Magician";
    heroID = "Magic.";
    sprite = atlas.createSprite("bq", -1);

    isSelected = false;
    isSelectable = true;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  @Override
  public void recover() {
    spells = 1;
    isSelectable = true;
  }

  public void castSpell() {
    spells--;
    if (spells == 0) {
      isSelectable = false;
      isSelected = false;
    }
  }

  public int getSpells() {
    return spells;
  }

  public void setSpells(int spells) {
    this.spells = Math.max(0, spells);
    if (this.spells > 0) {
      isSelectable = true;
    } else {
      isSelectable = false;
      isSelected = false;
    }
  }

}
