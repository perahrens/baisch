package com.mygdx.game;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Dice extends Actor {
  Sprite sprite;
  TextureAtlas atlas = new TextureAtlas("data/skins/dice.atlas");

  private int number;

  final float scaleFactor = 0.1f;

  public Dice() {
    //number = ThreadLocalRandom.current().nextInt(1, 6 + 1);
    number = 1;
    sprite = atlas.createSprite("side", number);

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

    /*
     * addListener(new ClickListener() {
     * 
     * @Override public void clicked(InputEvent event, float x, float y) { number =
     * ThreadLocalRandom.current().nextInt(1, 6 + 1);
     * 
     * sprite = atlas.createSprite("side", number);
     * 
     * setWidth(sprite.getWidth()*scaleFactor);
     * setHeight(sprite.getHeight()*scaleFactor);
     * 
     * }; });
     */
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    com.badlogic.gdx.graphics.Color color = getColor();
    // color.
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
    batch.draw(sprite, getX(), getY(), getWidth() / 2f, getHeight() / 2f, getWidth(), getHeight(), 1, 1, 0);
  }

  public void roll() {
    //number = ThreadLocalRandom.current().nextInt(1, 6 + 1);
    number = 1;
    sprite = atlas.createSprite("side", number);
  }

  public int getNumber() {
    return this.number;
  }

  public void setMapPosition(int player) {
    switch (player) {
    case 0:
      setX((MyGdxGame.WIDTH - getWidth()) / 2 + 50);
      setY(20);
      break;
    case 1:
      setX(20);
      setY((MyGdxGame.WIDTH - getWidth()) / 2 - 50);
      break;
    case 2:
      setX((MyGdxGame.WIDTH - getWidth()) / 2 - 50);
      setY(MyGdxGame.WIDTH - 50);
      break;
    case 3:
      setX(MyGdxGame.WIDTH - 50);
      setY((MyGdxGame.WIDTH - getWidth()) / 2 + 50);
      break;
    default:
      break;
    }
  }
}
