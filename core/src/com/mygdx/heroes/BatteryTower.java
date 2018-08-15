package com.mygdx.heroes;

public class BatteryTower extends Hero {

  public BatteryTower() {
    heroName = "Battery Tower";
    heroID = "B. Tow.";
    sprite = atlas.createSprite("wr", -1);

    this.isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);
  }
}
