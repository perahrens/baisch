package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class AbstractScreen implements Screen {

  private static SpriteBatch fullScreenBatch;
  private static OrthographicCamera fullScreenCamera;
  protected Game game;

  public AbstractScreen(Game game) {
    this.game = game;
  }

  protected void drawFullScreenTexture(Texture texture) {
    if (texture == null) return;
    if (fullScreenBatch == null) fullScreenBatch = new SpriteBatch();
    if (fullScreenCamera == null) fullScreenCamera = new OrthographicCamera();

    fullScreenCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    fullScreenBatch.setProjectionMatrix(fullScreenCamera.combined);
    fullScreenBatch.begin();
    fullScreenBatch.draw(texture, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    fullScreenBatch.end();
  }

}
