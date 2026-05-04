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
    int w = Gdx.graphics.getWidth();
    int h = Gdx.graphics.getHeight();
    // Reset viewport to full physical canvas — FitViewport from the previous
    // frame may have left glViewport set to the letterbox area, which would
    // clip both glClear and this draw to that smaller rectangle.
    Gdx.gl.glViewport(0, 0, w, h);
    if (fullScreenBatch == null) fullScreenBatch = new SpriteBatch();
    if (fullScreenCamera == null) fullScreenCamera = new OrthographicCamera();
    fullScreenCamera.setToOrtho(false, w, h);
    fullScreenBatch.setProjectionMatrix(fullScreenCamera.combined);
    fullScreenBatch.begin();
    fullScreenBatch.draw(texture, 0f, 0f, w, h);
    fullScreenBatch.end();
  }

}
