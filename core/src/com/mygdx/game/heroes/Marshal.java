package com.mygdx.game.heroes;

public class Marshal extends Hero {

  /*
   * Marshal increases the number of put/take defense card actions to takeDefCard +
   * setDefCard = 3.
   */

  private int mobilizations;

  public Marshal() {
    heroName = "Marshal";
    heroID = "Marshal";
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

  @Override
  public void recover() {
    isReady = true;
    mobilizations = 3;
  }

}
