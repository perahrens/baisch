package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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

  // --- Music tracks ---
  /** Menu screens (name-entry, session list, create-game). */
  static Music musicShimmer  = null;
  /** Game lobby while waiting (no countdown). */
  static Music musicDrums    = null;
  /** Lobby countdown. */
  static Music musicIntrigue = null;
  /** Currently selected track (null = silence). */
  static Music activeMusic   = null;
  /** True once a user gesture has occurred — required to unblock browser autoplay. */
  static boolean musicStarted = false;

  private static Music loadTrack(String path) {
    try {
      Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
      m.setLooping(true);
      return m;
    } catch (Exception e) {
      Gdx.app.log("Audio", "Track not found: " + path);
      return null;
    }
  }

  static void loadMusic() {
    musicShimmer  = loadTrack("data/sounds/freesound_community-desert-shimmer-24684.mp3");
    musicDrums    = loadTrack("data/sounds/tatamusic-battle-warrior-fighting-drums-478210.mp3");
    musicIntrigue = loadTrack("data/sounds/34724807-castle-of-intrigue-288660.mp3");
  }

  /** Switch to a new track (pass null for silence). */
  static void setMusicTrack(Music newTrack) {
    if (activeMusic != newTrack) {
      if (activeMusic != null) activeMusic.pause();
      activeMusic = newTrack;
    }
    if (activeMusic != null && musicStarted && playerStorage.getMusicEnabled()) {
      activeMusic.play();
    }
  }

  /**
   * Called on the first user touch to unblock browser autoplay.
   * Starts the currently active track if music is enabled.
   */
  static void ensureMusicStarted() {
    if (!musicStarted) {
      musicStarted = true;
      if (activeMusic != null && playerStorage.getMusicEnabled()) {
        activeMusic.play();
      }
    }
  }

  /**
   * Called from the DOM touchend/click handler to start music inside the
   * browser's user-activation context.  We call stop() before play() because
   * an earlier rejected play() may have left SoundManager2's playState stale.
   */
  public void resumeMusicIfEnabled() {
    if (playerStorage.getMusicEnabled() && activeMusic != null) {
      musicStarted = true;
      activeMusic.stop();
      activeMusic.play();
    }
  }

  /** Toggle music on/off, persist the preference, and update playback immediately. */
  static void setMusicEnabled(boolean enabled) {
    musicStarted = true; // any music button interaction = valid user gesture
    playerStorage.saveMusicEnabled(enabled);
    if (enabled) {
      if (activeMusic != null) activeMusic.play();
    } else {
      if (activeMusic != null) activeMusic.pause();
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

    // Apply Linear filter to the atlas for sharper rendering on HiDPI screens.
    // gameBck/handBck in GameScreen use a standalone Pixmap texture (not the
    // atlas "white" region) so they are unaffected by this filter.
    for (Texture t : skin.getAtlas().getTextures()) {
      t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    loadMusic();

    connectSocket();

    setScreen(new MenuScreen(this, socket));

    // configSocketEvents();

  }

  @Override
  public void dispose() {
    if (musicShimmer  != null) musicShimmer.dispose();
    if (musicDrums    != null) musicDrums.dispose();
    if (musicIntrigue != null) musicIntrigue.dispose();
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
