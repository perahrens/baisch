package com.mygdx.heroes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

public class Hero extends Actor {
  protected String heroName = "Hero";
  protected String heroID = "H.";
  protected Sprite sprite;
  protected TextureAtlas atlas = new TextureAtlas("data/skins/pieces.atlas");

  protected boolean isSelectable = false;
  protected boolean isSelected;

  protected boolean isHand = false;
  final float scaleFactor = 1f;

  protected boolean isReady = true; // can hero still be used this turn

  @Override
  public void draw(Batch batch, float parentAlpha) {
    com.badlogic.gdx.graphics.Color color = getColor();
    if (this.isSelected) {
      batch.setColor(color.r / 2.0f, color.g, color.b / 2.0f, color.a * parentAlpha);
    } else {
      batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
    }
    if (isHand) {
      batch.draw(sprite, getX(), getY(), getWidth() / 2f, getHeight() / 2f, getWidth(), getHeight(), 1, 1, 0);
    } else {
      batch.draw(sprite, getX(), getY(), getWidth() / 2f, getHeight() / 2f, getWidth(), getHeight(), 0.5f, 0.5f, 0);
    }
  }

  public String getHeroID() {
    return heroID;
  }

  public String getHeroName() {
    return heroName;
  }

  public void setHand(boolean isHand) {
    this.isHand = isHand;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public boolean isSelected() {
    return isSelected;
  }

  public void setSelectable(boolean isSelectable) {
    this.isSelectable = isSelectable;
  }

  public boolean isSelectable() {
    return isSelectable;
  }

  public void removeAllListeners() {
    Array<EventListener> listeners = getListeners();
    for (EventListener listener : listeners) {
      removeListener(listener);
    }
  }

  public void recover() {

  }

  public boolean isReady() {
    return isReady;
  }

}
