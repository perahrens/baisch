package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Card extends Actor {
  Sprite sprite;
  TextureAtlas atlas = new TextureAtlas("data/skins/carddeck.atlas");

  private final String symbol;
  private final Integer index;

  private int positionId;

  private boolean isSelected = false;
  private boolean isCovered;
  private boolean isActive;
  private boolean isPlaceholder = false;

  private boolean isRemoved = false; // temporary information that card should be removed

  private boolean isSuspended = false; // suspended card can only be used next round
  private boolean isTradable = false; // information that card can be traded again and should be overlaid with
                                      // keep/cast button

  private float defWidth;
  private float defHeight;

  private Color defColor;

  private final float scaleFactor = 0.22f;
  private float rotate = 0;

  // placeholder card
  public Card() {
    sprite = atlas.createSprite("back", 3);

    symbol = "back";
    index = 3;

    positionId = -1;

    isCovered = false;
    isActive = false;
    isPlaceholder = true;

    defWidth = sprite.getWidth() * scaleFactor;
    defHeight = sprite.getHeight() * scaleFactor;

    defColor = getColor();

    setWidth(defWidth);
    setHeight(defHeight);
    setBounds(getX(), getY(), getWidth(), getHeight());
  }

  // standard playing card
  public Card(String symbol, int index) {
    sprite = atlas.createSprite(symbol, index);

    this.symbol = symbol;
    this.index = index;

    positionId = -1;

    isCovered = false;
    isActive = false;
    isPlaceholder = false;

    defWidth = sprite.getWidth() * scaleFactor;
    defHeight = sprite.getHeight() * scaleFactor;

    defColor = getColor();

    setWidth(defWidth);
    setHeight(defHeight);
    setBounds(getX(), getY(), getWidth(), getHeight());
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    sprite = atlas.createSprite(symbol, index);
    Color color = defColor;
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
    if (isPlaceholder) {
      batch.setColor(color.r, color.g, color.b, color.a * parentAlpha * 0.5f);
    } else {
      if (isCovered) {
        if (isActive) {
          // set dark
          batch.setColor(color.r / 1.5f, color.g * 1.5f, color.b / 1.5f, color.a * parentAlpha);
          if (isSelected) {
            // set dark green
            batch.setColor(color.r / 2.0f, color.g, color.b / 2.0f, color.a * parentAlpha);
          } else {
            // set dark
          }
        } else {
          // set red
          sprite = atlas.createSprite("back", 1);
          if (isSelected) {
            // not selectable --> attack
          } else {
            // set red back
          }
        }
      } else {
        if (isActive) {
          if (isSelected) {
            // set green
            batch.setColor(color.r / 2.0f, color.g, color.b / 2.0f, color.a * parentAlpha);
          } else {
            // set white
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
          }
        } else {
          // set white
          batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
          if (isSelected) {
            // not selectable --> attack
          } else {
            // set white
          }
        }
      }
    }

    batch.draw(sprite, getX(), getY(), getWidth() / 2f, getHeight() / 2f, getWidth(), getHeight(), 1, 1, rotate);
  }

  public boolean isPlaceholder() {
    return isPlaceholder;
  }

  public void setPlaceholder(boolean isPlaceholder) {
    this.isPlaceholder = isPlaceholder;
  }

  public boolean isRemoved() {
    return isRemoved;
  }

  public void setRemoved(boolean isRemoved) {
    this.isRemoved = isRemoved;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public boolean isSelected() {
    return isSelected;
  }

  public String getSymbol() {
    return this.symbol;
  }

  public int getIndex() {
    return this.index;
  }

  public int getStrength() {
    int strength = index;
    if (index == 1)
      strength = 14;
    if (symbol == "joker")
      strength = 999;
    return strength;
  }

  public void setCovered(boolean isCovered) {
    this.isCovered = isCovered;
  }

  public boolean isCovered() {
    return isCovered;
  }

  public void setTradable(boolean isTradeable) {
    this.isTradable = isTradeable;
  }

  public boolean isTradeable() {
    return isTradable;
  }

  public void setRotation(int rotation) {
    this.rotate = rotation;
  }

  public float getDefWidth() {
    return defWidth;
  }

  public float getDefHeight() {
    return defHeight;
  }

  public void dispose() {
    this.dispose();
  }

  public void setDeckPosition() {
    setWidth(defWidth);
    setHeight(defHeight);
    setX(MyGdxGame.WIDTH / 2 - getWidth() / 2 + getHeight() / 2);
    setY(MyGdxGame.WIDTH / 2 - getHeight() / 2 - getHeight() / 2);
    rotate = 45;
    isCovered = true;
    isSelected = false;
  }

  public void setCemeteryPosition() {
    setWidth(defWidth);
    setHeight(defHeight);
    setX(MyGdxGame.WIDTH / 2 - defWidth / 2 - defHeight / 2);
    setY(MyGdxGame.WIDTH / 2);
    rotate = 45;
    isCovered = false;
    isSelected = false;
  }

  public int getPositionId() {
    return positionId;
  }

  public void setMapPosition(int player, int position) {
    setWidth(defWidth);
    setHeight(defHeight);
    isSelected = false;
    positionId = position;
    switch (player) {
    case 0:
      switch (position) {
      case 0:
        setX((MyGdxGame.WIDTH - getWidth()) / 2);
        setY(0);
        break;
      default:
        setX((MyGdxGame.WIDTH - getWidth()) / 2 + (position - 2) * getWidth());
        setY(getHeight());
        break;
      }
      break;
    case 1:
      switch (position) {
      case 0:
        setX((getHeight() - getWidth()) / 2);
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2);
        rotate = -90;
        break;
      default:
        setX((getHeight() - getWidth()) / 2 + getHeight());
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2 + (2 - position) * getWidth());
        rotate = -90;
        break;
      }
      break;
    case 2:
      switch (position) {
      case 0:
        setX((MyGdxGame.WIDTH - getWidth()) / 2);
        setY(MyGdxGame.WIDTH - getHeight());
        rotate = 180;
        break;
      default:
        setX((MyGdxGame.WIDTH - getWidth()) / 2 + (2 - position) * getWidth());
        setY(MyGdxGame.WIDTH - 2f * getHeight());
        rotate = 180;
        break;
      }
      break;
    case 3:
      switch (position) {
      case 0:
        setX((getHeight() - getWidth()) / 2 + MyGdxGame.WIDTH - getHeight());
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2);
        rotate = 90;
        break;
      default:
        setX((getHeight() - getWidth()) / 2 + MyGdxGame.WIDTH - 2f * getHeight());
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2 + (position - 2) * getWidth());
        rotate = 90;
        break;
      }
      break;
    }
  }

}
