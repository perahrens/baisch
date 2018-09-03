package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import io.socket.client.IO;
import io.socket.client.Socket;

public class MyGdxGame extends Game implements InputProcessor {
  private Socket socket;
  public final static float HEIGHT = 800;
  public final static float WIDTH = 450;

  static OrthographicCamera camera;
  private SpriteBatch batch;

  static Skin skin;
  static Stage stage;

  @Override
  public void create() {

    // camera = new OrthographicCamera(Gdx.graphics.getWidth(),
    // Gdx.graphics.getHeight());
    // batch = new SpriteBatch();
    stage = new Stage();
    Gdx.input.setInputProcessor(stage);

    skin = new Skin(Gdx.files.internal("data/skins/uiskin.json"));

    connectSocket();

    setScreen(new MenuScreen(this, socket));

    // configSocketEvents();

  }

  @Override
  public void dispose() {

  }

  @Override
  public boolean keyDown(int keycode) {
    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    return false;
  }

  @Override
  public boolean keyTyped(char character) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean scrolled(int amount) {
    // TODO Auto-generated method stub
    return false;
  }

  public void connectSocket() {
    try {
      socket = IO.socket("http://localhost:8082");
      socket.connect();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

}
