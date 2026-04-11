package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

// IO removed (platform-specific)
import com.mygdx.game.net.SocketClient;

public class MyGdxGame extends Game implements InputProcessor {
  private SocketClient socket;
  public static SocketClient socketInstance;
  /** Platform-supplied turn notifier. Defaults to no-op; overridden by HtmlLauncher. */
  public static TurnNotifier turnNotifier = TurnNotifier.NOOP;
  /** Platform-supplied player identity storage. Defaults to no-op; overridden by HtmlLauncher. */
  public static PlayerStorage playerStorage = PlayerStorage.NOOP;
  public final static float HEIGHT = 800;
  public final static float WIDTH = 450;

  static OrthographicCamera camera;
  private SpriteBatch batch;

  static Skin skin;
  static Stage stage;

  /** Background music instance. Null if the audio file is not found. */
  static Music bgMusic = null;
  /** True once bgMusic.play() has been called (guards against double-start). */
  static boolean musicStarted = false;

  /** Load the background music file without starting playback. */
  static void loadMusic() {
    try {
      bgMusic = Gdx.audio.newMusic(Gdx.files.internal("data/sounds/music.mp3"));
      bgMusic.setLooping(true);
    } catch (Exception e) {
      Gdx.app.log("Audio", "Background music not found — place data/sounds/music.mp3 to enable it");
      bgMusic = null;
    }
  }

  /**
   * Start the music if it is enabled and not yet playing.
   * Must be called from inside a user-gesture handler (click/touch) so that
   * browsers allow audio playback.
   */
  static void ensureMusicStarted() {
    if (bgMusic != null && !musicStarted && playerStorage.getMusicEnabled()) {
      bgMusic.play();
      musicStarted = true;
    }
  }

  /** Toggle music on/off, persist the preference, and update playback immediately. */
  static void setMusicEnabled(boolean enabled) {
    playerStorage.saveMusicEnabled(enabled);
    if (bgMusic == null) return;
    if (enabled) {
      bgMusic.play();
      musicStarted = true;
    } else {
      bgMusic.pause();
    }
  }

  @Override
  public void create() {

    // camera = new OrthographicCamera(Gdx.graphics.getWidth(),
    // Gdx.graphics.getHeight());
    // batch = new SpriteBatch();
    stage = new Stage();
    Gdx.input.setInputProcessor(stage);

    skin = new Skin(Gdx.files.internal("data/skins/uiskin.json"));

    loadMusic();

    connectSocket();

    setScreen(new MenuScreen(this, socket));

    // configSocketEvents();

  }

  @Override
  public void dispose() {
    if (bgMusic != null) bgMusic.dispose();
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
    socket = socketInstance;
  }

}
