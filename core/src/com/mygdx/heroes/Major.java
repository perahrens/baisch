package com.mygdx.heroes;

public class Major extends Hero {

  /*
   * Major increases the number of put/take defense card actions to takeDefCard +
   * setDefCard = 3.
   */

  private int mobilizations;

  public Major() {
    heroName = "Major";
    heroID = "Major";
    sprite = atlas.createSprite("wn", -1);

    mobilizations = 3;

    isSelectable = false;
    isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  public void mobilize() {
    mobilizations--;
  }

  public int getMobilizations() {
    return mobilizations;
  }

  public void recover() {
    isReady = true;
    mobilizations = 3;
  }

}
