package com.mygdx.game.heroes;

public class Priest extends Hero {
  private int conversionAttempts = 2;

  public Priest() {
    heroName = "Priest";
    heroID = "Priest";
    sprite = atlas.createSprite("wk", -1);

    isSelected = false;
    // priest becomes selectable when attacking symbol is defined
    isSelectable = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  @Override
  public void recover() {
    conversionAttempts = 2;
  }

  public void conversionAttempt() {
    conversionAttempts--;
    if (conversionAttempts == 0) {
      isSelectable = false;
      isSelected = false;
    }
  }

  public int getConversionAttempts() {
    return conversionAttempts;
  }

  public void conversion() {
    conversionAttempts = 0;
  }

}
