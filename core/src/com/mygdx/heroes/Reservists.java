package com.mygdx.heroes;

public class Reservists extends Hero {
  public Reservists() {
    heroName = "Reservists";
    heroID = "Reser.";
    sprite = atlas.createSprite("bp", -1);

    this.isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }
}
