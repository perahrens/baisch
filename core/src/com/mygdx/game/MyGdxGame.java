package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

// IO removed (platform-specific)
import com.mygdx.game.net.DiagListener;
import com.mygdx.game.net.SocketClient;

public class MyGdxGame extends Game implements InputProcessor {
  private SocketClient socket;
  public static SocketClient socketInstance;
  /** Platform-supplied turn notifier. Defaults to no-op; overridden by HtmlLauncher. */
  public static TurnNotifier turnNotifier = TurnNotifier.NOOP;
  /** Platform-supplied player identity storage. Defaults to no-op; overridden by HtmlLauncher. */
  public static PlayerStorage playerStorage = PlayerStorage.NOOP;
  /** Platform-supplied keyboard helper for mobile browsers. Defaults to no-op; overridden by HtmlLauncher. */
  public static KeyboardHelper keyboardHelper = KeyboardHelper.NOOP;
  public final static float HEIGHT = 800;
  public final static float WIDTH = 450;

  static OrthographicCamera camera;
  private SpriteBatch batch;

  static Skin skin;
  private static BitmapFont defaultUiFont;
  private static BitmapFont ruUiFont;
  /** Old plain uiskin — used for compact banner buttons (Skip / Next ►). */
  static Skin plainSkin;
  static Stage stage;

  // --- Sound effects ---
  public static Sound soundCardDrop    = null;
  public static Sound soundCardShuffle = null;
  public static Sound soundKingAttack  = null;
  public static Sound soundSlurp       = null;
  public static Sound soundJokerLaugh  = null;
  // Attack outcome (active player only)
  public static Sound soundAttackSuccess   = null;
  public static Sound soundAttackFail      = null;
  // Hero sounds (all players)
  public static Sound soundHeroSpy         = null;
  public static Sound soundHeroMercenaries = null;
  public static Sound soundHeroMarshal     = null;
  public static Sound soundHeroPriest      = null;
  public static Sound soundHeroBanneret    = null;
  public static Sound soundHeroMagician    = null;
  public static Sound soundHeroBatteryTower = null;

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

  /**
   * When true, the HTML/GWT layer provides its own animated GIF music button and
   * {@link MenuScreen#addMusicToggleButton} skips adding the LibGDX TextButton.
   * Set to {@code true} by HtmlLauncher before the first screen is shown.
   */
  public static boolean nativeMusicButton = false;

  /**
   * Called whenever the music enabled-state changes so the platform layer can
   * update its visual music indicator (e.g. start/stop the animated GIF).
   * Set by HtmlLauncher; null on non-web platforms.
   */
  public static Runnable onMusicUiUpdate = null;
  /** Called whenever language button visuals should refresh (browser DOM). */
  public static Runnable onLanguageUiUpdate = null;
  /** Called when the game-play screen becomes active; hides the native music button. */
  public static Runnable onGameScreenActive = null;
  /** Called when a menu/stats screen becomes active; shows the native music button again. */
  public static Runnable onMenuScreenActive = null;
  /** Called when the name-entry screen is shown; reveals the DOM logo overlay. */
  public static Runnable onNameEntryScreenActive = null;
  /** Called when leaving the name-entry screen; hides the DOM logo overlay. */
  public static Runnable onNameEntryScreenDone = null;
  /** Called after each finishTurn roundtrip is measured; pushes to window.baischDiag. Null on non-browser platforms. */
  public static DiagListener onRoundtripRecorded = null;

  /** Singleton reference — set in {@link #create()} for JSNI callbacks. */
  public static MyGdxGame INSTANCE;

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

  private static Sound loadSoundEffect(String path) {
    try {
      return Gdx.audio.newSound(Gdx.files.internal(path));
    } catch (Exception e) {
      Gdx.app.log("Audio", "Sound not found: " + path);
      return null;
    }
  }

  static void loadSounds() {
    soundCardDrop    = loadSoundEffect("data/sounds/freesound_community-carddrop2-92718.mp3");
    soundCardShuffle = loadSoundEffect("data/sounds/freesound_community-riffle-card-shuffle-104313.mp3");
    soundKingAttack  = loadSoundEffect("data/sounds/freesound_community-middle-ages-war-crywav-14780.mp3");
    soundSlurp       = loadSoundEffect("data/sounds/freesound_community-cartoon-slurp-37066.mp3");
    soundJokerLaugh  = loadSoundEffect("data/sounds/freesound_community-joker-laugh-2-98829.mp3");
    soundAttackSuccess    = loadSoundEffect("data/sounds/ksjsbwuil-apple-pay-success-sound-effect-481188.mp3");
    soundAttackFail       = loadSoundEffect("data/sounds/universfield-fail-trumpet-02-383962.mp3");
    soundHeroSpy          = loadSoundEffect("data/sounds/bentomusic-comical-style-music-of-silliness-and-goofing-around-loop-439124.mp3");
    soundHeroMercenaries  = loadSoundEffect("data/sounds/freesound_community-medieval-fanfare-6826.mp3");
    soundHeroMarshal      = loadSoundEffect("data/sounds/dragon-studio-horse-neigh-515279.mp3");
    soundHeroPriest       = loadSoundEffect("data/sounds/freesound_community-sfx_spirit_gain-95855.mp3");
    soundHeroBanneret     = loadSoundEffect("data/sounds/freesound_community-fx-comedy-marching-78276.mp3");
    soundHeroMagician     = loadSoundEffect("data/sounds/fxprosound-magic-glitter-wand-5-248607.mp3");
    soundHeroBatteryTower = loadSoundEffect("data/sounds/dragon-studio-loud-explosion-425457.mp3");
  }

  /** Play a one-shot sound effect (no-op if sound effects are disabled or sound is null). */
  public static void playGameSound(Sound s) {
    if (s == null) return;
    if (!playerStorage.getSoundEnabled()) return;
    s.play();
  }

  /** Play a sound and auto-stop it after 10 seconds (for hero sounds that may be long). */
  public static void playGameSoundCapped(final Sound s) {
    if (s == null) return;
    if (!playerStorage.getSoundEnabled()) return;
    final long soundId = s.play();
    com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
      @Override
      public void run() {
        s.stop(soundId);
      }
    }, 10f);
  }

  /** Play the hero-won sound for the given hero name (capped at 10 s). Called for all players. */
  public static void playHeroWonSound(String heroName) {
    Sound s = null;
    if ("Spy".equals(heroName))           s = soundHeroSpy;
    else if ("Mercenaries".equals(heroName)) s = soundHeroMercenaries;
    else if ("Marshal".equals(heroName))     s = soundHeroMarshal;
    else if ("Priest".equals(heroName))      s = soundHeroPriest;
    else if ("Banneret".equals(heroName))    s = soundHeroBanneret;
    else if ("Magician".equals(heroName))    s = soundHeroMagician;
    else if ("Battery Tower".equals(heroName)) s = soundHeroBatteryTower;
    playGameSoundCapped(s);
  }

  /** Toggle sound effects on/off and persist the preference. */
  static void setSoundEnabled(boolean enabled) {
    playerStorage.saveSoundEnabled(enabled);
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
   * Guards against re-triggering if music was already started.
   */
  public void resumeMusicIfEnabled() {
    if (playerStorage.getMusicEnabled() && activeMusic != null && !musicStarted) {
      musicStarted = true;
      activeMusic.stop();
      activeMusic.play();
    }
  }

  /**
   * Returns true once the music track has been loaded (activeMusic != null).
   * Used by the DOM audio unlocker to decide whether to remove its listener.
   */
  public boolean isMusicActive() {
    return activeMusic != null;
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
    if (onMusicUiUpdate != null) onMusicUiUpdate.run();
  }

  /**
   * Called from the HTML music GIF button (via JSNI) when the user clicks it.
   * Toggles the music state and refreshes the current menu screen if needed.
   */
  public static void handleMusicButtonClick() {
    boolean newEnabled = !playerStorage.getMusicEnabled();
    setMusicEnabled(newEnabled);
    if (INSTANCE != null) {
      com.badlogic.gdx.Screen scr = INSTANCE.getScreen();
      if (scr instanceof MenuScreen) ((MenuScreen) scr).show();
    }
  }

  /** Called from the HTML language button (via JSNI) when the user picks a language. */
  public static void handleLanguageButtonClick(String languageCode) {
    Localization.setLanguage(languageCode);
    if (INSTANCE != null) {
      com.badlogic.gdx.Screen scr = INSTANCE.getScreen();
      if (scr != null) scr.show();
    }
  }

  /**
   * Swap UI fonts when RU is active so only Russian uses the Cyrillic bitmap font.
   */
  public static void applyLanguageFont() {
    if (skin == null) return;

    if (defaultUiFont == null) {
      defaultUiFont = skin.getFont("font");
    }

    if (ruUiFont == null) {
      try {
        ruUiFont = new BitmapFont(Gdx.files.internal("data/skins/rusty-robot/font-export-ru.fnt"));
      } catch (Exception ex) {
        Gdx.app.log("Font", "RU font unavailable, falling back to default: " + ex.getMessage());
        ruUiFont = null;
      }
    }

    boolean useRuFont = Localization.RU.equals(Localization.getLanguage()) && ruUiFont != null;
    BitmapFont activeFont = useRuFont ? ruUiFont : defaultUiFont;

    Label.LabelStyle labelDefault = skin.get("default", Label.LabelStyle.class);
    labelDefault.font = activeFont;
    Label.LabelStyle labelBg = skin.get("bg", Label.LabelStyle.class);
    labelBg.font = activeFont;
    Label.LabelStyle labelTitle = skin.get("title", Label.LabelStyle.class);
    labelTitle.font = activeFont;

    TextButton.TextButtonStyle textButton = skin.get("default", TextButton.TextButtonStyle.class);
    textButton.font = activeFont;
    ImageTextButton.ImageTextButtonStyle imageTextButton = skin.get("default", ImageTextButton.ImageTextButtonStyle.class);
    imageTextButton.font = activeFont;

    CheckBox.CheckBoxStyle checkbox = skin.get("default", CheckBox.CheckBoxStyle.class);
    checkbox.font = activeFont;
    CheckBox.CheckBoxStyle radio = skin.get("radio", CheckBox.CheckBoxStyle.class);
    radio.font = activeFont;

    List.ListStyle list = skin.get("default", List.ListStyle.class);
    list.font = activeFont;
    SelectBox.SelectBoxStyle selectBox = skin.get("default", SelectBox.SelectBoxStyle.class);
    selectBox.font = activeFont;
    TextField.TextFieldStyle textField = skin.get("default", TextField.TextFieldStyle.class);
    textField.font = activeFont;

    Window.WindowStyle windowDefault = skin.get("default", Window.WindowStyle.class);
    windowDefault.titleFont = activeFont;
    Window.WindowStyle windowEmpty = skin.get("empty", Window.WindowStyle.class);
    windowEmpty.titleFont = activeFont;
    Window.WindowStyle windowDialog = skin.get("dialog", Window.WindowStyle.class);
    windowDialog.titleFont = activeFont;
    Window.WindowStyle windowEmptyBg = skin.get("empty-bg", Window.WindowStyle.class);
    windowEmptyBg.titleFont = activeFont;
  }

  @Override
  public void create() {
    INSTANCE = this;

    // camera = new OrthographicCamera(Gdx.graphics.getWidth(),
    // Gdx.graphics.getHeight());
    // batch = new SpriteBatch();
    stage = new Stage();
    Gdx.input.setInputProcessor(stage);

    skin = new Skin(Gdx.files.internal("data/skins/rusty-robot/rusty-robot-ui.json"));
    plainSkin = new Skin(Gdx.files.internal("data/skins/uiskin.json"));

    Localization.init(playerStorage.getLanguage());
    applyLanguageFont();

    // Apply Linear filter to the atlas for sharper rendering on HiDPI screens.
    // gameBck/handBck in GameScreen use a standalone Pixmap texture (not the
    // atlas "white" region) so they are unaffected by this filter.
    for (Texture t : skin.getAtlas().getTextures()) {
      t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    loadMusic();
    loadSounds();

    connectSocket();

    setScreen(new MenuScreen(this, socket));

    // configSocketEvents();

  }

  @Override
  public void dispose() {
    if (musicShimmer  != null) musicShimmer.dispose();
    if (musicDrums    != null) musicDrums.dispose();
    if (musicIntrigue != null) musicIntrigue.dispose();
    if (soundCardDrop    != null) soundCardDrop.dispose();
    if (soundCardShuffle != null) soundCardShuffle.dispose();
    if (soundKingAttack  != null) soundKingAttack.dispose();
    if (soundSlurp       != null) soundSlurp.dispose();
    if (soundJokerLaugh  != null) soundJokerLaugh.dispose();
    if (soundAttackSuccess    != null) soundAttackSuccess.dispose();
    if (soundAttackFail       != null) soundAttackFail.dispose();
    if (soundHeroSpy          != null) soundHeroSpy.dispose();
    if (soundHeroMercenaries  != null) soundHeroMercenaries.dispose();
    if (soundHeroMarshal      != null) soundHeroMarshal.dispose();
    if (soundHeroPriest       != null) soundHeroPriest.dispose();
    if (soundHeroBanneret     != null) soundHeroBanneret.dispose();
    if (soundHeroMagician     != null) soundHeroMagician.dispose();
    if (soundHeroBatteryTower != null) soundHeroBatteryTower.dispose();
    if (ruUiFont != null) ruUiFont.dispose();
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
