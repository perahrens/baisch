package com.mygdx.game.heroes;

public class Lieutenant extends Hero {

  /*
   * Lieutenant allows two attacking symbols of the same color (hearts + diamonds
   * or spades + clubs)
   */

  public Lieutenant() {
    heroName = "Lieutenant";
    heroID = "Lieut.";
    sprite = atlas.createSprite("bn", -1);

    isSelectable = false;
    isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }
}
