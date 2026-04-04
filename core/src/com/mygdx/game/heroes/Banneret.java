package com.mygdx.game.heroes;

public class Banneret extends Hero {

  /*
   * Banneret allows two attacking symbols of the same color (hearts + diamonds
   * or spades + clubs)
   */

  public Banneret() {
    heroName = "Banneret";
    heroID = "Bann.";
    sprite = atlas.createSprite("bn", -1);

    isSelectable = false;
    isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }
}
