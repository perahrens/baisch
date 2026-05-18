package com.mygdx.game;

import java.util.ArrayList;

import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import com.mygdx.game.net.SocketClient;
import com.mygdx.game.net.SocketListener;

public class MenuScreen extends AbstractScreen {

  private static final String RULES_URL = "https://perahrens.github.io/baisch/";

  private SocketClient socket;

  private Stage menuStage;
  private MenuState menuState;

  private TextButton button;
  private SelectBox<String> heroSelectBox;

  private Group group;

  private Texture logoTexture;
  private Texture menuBgTexture;
  private TextureRegion logoRegion;
  private Image logoImage;

  private int currentUsersCount;
  private boolean updateScreen = false;
  boolean timerStarted = false;
  private boolean gameRunning = false;

  // Whether the player has entered a name and joined the lobby.
  private boolean lobbyJoined = false;

  // Whether the player has entered a name and reached the session-list screen
  private boolean nameConfirmed = false;

  // True once the player has made an auth choice (guest / register / login).
  // Set to true at startup when a saved name or username is already present.
  private boolean authChoiceShown = false;

  // Error message to display on the register / login screens; empty string = no error.
  private String authErrorMessage = "";

  // True while waiting for a registerResult / loginResult response from the server.
  private boolean authPending = false;

  // True while the register sub-screen is shown (vs. login sub-screen).
  private boolean inRegisterScreen = false;
  // True while the login sub-screen is shown.
  private boolean inLoginScreen = false;

  // True while the session-creation sub-screen is shown
  private boolean inSessionCreate = false;
  // Pending game name typed on the create screen (cleared after creation)
  private String pendingSessionName = "";
  // Whether hero selection is allowed in the current session
  private boolean sessionAllowHeroSelection = false;
  // Pending create-screen settings
  private boolean pendingManualSetup = false;
  private int pendingStartingCards = 8;
  // How heroes are assigned when a joker is sacrificed: "value_mapping" | "color_mapping" | "free_selector"
  private String pendingHeroAssignMode = "value_mapping";

  // Lobby slot configuration received from the server (4 slots: slot 0 = host).
  // Each entry is: "player", "open", "closed", or "bot:<mode>" (e.g. "bot:balanced").
  private String[] lobbySlotTypes  = {"player", "open", "open", "open"};
  private String[] lobbySlotUserIds = {"", "", "", ""};
  private String[] lobbySlotBotUserIds = {"", "", "", ""};
  private String[] lobbySlotBotModes  = {"", "", "", ""};
  // Whether the local player created this session (persists even if they move to spectator slot)
  private boolean isSessionHost = false;

  // The session list received from the server
  private java.util.List<SessionInfo> sessionList = new java.util.ArrayList<SessionInfo>();

  private static class SessionInfo {
    String id;
    String name;
    int playerCount;
    int maxSlots;
    boolean running;
    SessionInfo(String id, String name, int playerCount, int maxSlots, boolean running) {
      this.id = id; this.name = name; this.playerCount = playerCount; this.maxSlots = maxSlots; this.running = running;
    }
  }

  // Live list of all named online players, broadcast by the server.
  private java.util.List<OnlinePlayerInfo> onlinePlayers = new java.util.ArrayList<OnlinePlayerInfo>();
  private boolean showPlayersTab = false;
  // True while waiting for the server to confirm reconnect to a running game.
  // Suppresses the lobby flash that would otherwise appear before gameState arrives.
  private boolean reconnecting = false;
  // Seconds elapsed since reconnecting started; triggers automatic fallback after the timeout.
  private float reconnectElapsed = 0f;
  private static final float RECONNECT_TIMEOUT_SECONDS = 10f;
  // True when the server kicked this tab because the same token opened a new tab.
  private boolean disconnectedByDuplicateTab = false;

  // Avatar icon chosen on the name-entry screen, persisted across refreshes.
  private String selectedIcon = "";
  // Cache of loaded avatar textures keyed by icon name.
  private final java.util.Map<String, Texture> avatarTextures = new java.util.HashMap<String, Texture>();
  // Known avatar names (file names without extension inside data/avatars/).
  private static final String[] AVATAR_NAMES = {"alien1","alien2","cat","dolphin","fishnugget","knight","lion","monkey","parrot","rat","rooster","stegosauros"};
  // Size of the top-right account/avatar button (pixels in logical coords).
  private static final float ACCOUNT_BTN_SIZE = 42f;
  // True during show() calls where addTopRightButtons() has been called.
  // Used by addLanguageButtons() to shift the language picker further left.
  private boolean accountBtnAdded = false;

  private static class OnlinePlayerInfo {
    String id;
    String name;
    String status;
    String icon;
    OnlinePlayerInfo(String id, String name, String status, String icon) {
      this.id = id; this.name = name; this.status = status; this.icon = icon != null ? icon : "";
    }
  }

  // Hero names in display order — used to rebuild the dropdown while preserving order.
  private static final String[] ALL_HERO_NAMES = {
    "Mercenaries", "Marshal", "Spy", "Battery Tower", "Merchant", "Priest",
    "Reservists", "Banneret", "Saboteurs", "Fortified Tower", "Magician", "Warlord"
  };

  // Heroes currently reserved by OTHER lobby players (not this client).
  private final java.util.HashSet<String> reservedByOthers = new java.util.HashSet<String>();

  // Set to true while refreshing dropdown items programmatically to suppress spurious heroSelected emits.
  private boolean updatingDropdown = false;
  private static final String HERO_RANDOM = "Random";
  private static final String HERO_NONE = "None";
  private Texture languageEnIcon;
  private Texture languageDeIcon;
  private Texture languageRuIcon;
  private Texture languageNoIcon;
  private Texture languageItIcon;

  private String t(String key) {
    return Localization.tr(key);
  }

  private String t(String key, Object... args) {
    return Localization.tr(key, args);
  }

  private String heroLabel(String canonicalHeroName) {
    if (HERO_NONE.equals(canonicalHeroName)) return "-";
    return Localization.heroName(canonicalHeroName);
  }

  private String heroOptionLabel(String canonical) {
    if (HERO_RANDOM.equals(canonical)) return t("menu.hero.random");
    if (HERO_NONE.equals(canonical)) return t("menu.hero.none");
    return heroLabel(canonical);
  }

  private String canonicalHeroFromOption(String optionLabel) {
    if (optionLabel == null) return HERO_RANDOM;
    if (optionLabel.equals(t("menu.hero.random"))) return HERO_RANDOM;
    if (optionLabel.equals(t("menu.hero.none"))) return HERO_NONE;
    for (int i = 0; i < ALL_HERO_NAMES.length; i++) {
      String canonical = ALL_HERO_NAMES[i];
      if (optionLabel.equals(heroLabel(canonical))) return canonical;
    }
    return HERO_RANDOM;
  }

  public MenuScreen(final Game game, final SocketClient socket) {
    super(game);

    this.socket = socket;

    menuStage = new Stage(new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT));

    // init game
    menuState = new MenuState();
    configSocketEvents(socket);

    // If the socket is already connected (e.g. returning from GameScreen), grab the
    // socket ID immediately so the lobby is functional without waiting for socketID.
    String existingSocketId = socket.getSocketId();
    if (existingSocketId != null && !existingSocketId.isEmpty()) {
      menuState.setMyUserID(existingSocketId);
    }

    // Pre-populate name and UI state from local storage so returning players skip the name-entry screen.
    String savedName = MyGdxGame.playerStorage.getSavedName();
    if (!savedName.isEmpty()) {
      menuState.setMyName(savedName);
      nameConfirmed = true;
      authChoiceShown = true;
    }
    // Returning registered-account players also skip the auth-choice and name-entry screens.
    String savedUsername = MyGdxGame.playerStorage.getSavedUsername();
    if (!savedUsername.isEmpty() && !nameConfirmed) {
      menuState.setMyName(savedUsername);
      // Ensure baisch_player_name is set so the DOM account button appears.
      MyGdxGame.playerStorage.saveName(savedUsername);
      nameConfirmed = true;
      authChoiceShown = true;
    } else if (!savedUsername.isEmpty()) {
      authChoiceShown = true;
    }
    showPlayersTab = MyGdxGame.playerStorage.getSavedShowPlayersTab();
    selectedIcon = MyGdxGame.playerStorage.getSavedIcon();
    // If the player was mid-game when they refreshed, suppress the lobby flash by
    // entering reconnecting mode.  show() will display a spinner until gameState
    // arrives (or sessionNotFound clears the flag and falls back to the lobby).
    if (nameConfirmed && !MyGdxGame.playerStorage.getSavedSessionId().isEmpty()) {
      reconnecting = true;
      reconnectElapsed = 0f;
    }

    // If returning from a game (socket already connected, name already confirmed), request a
    // fresh session list — the server only broadcasts it on socket connect or session events,
    // so a newly created MenuScreen would otherwise show a stale/empty lobby.
    if (nameConfirmed && existingSocketId != null && !existingSocketId.isEmpty()) {
      socket.emit("requestSessionList", "");
    }

    // create menu screen
    group = new Group();
    group.setBounds(0, 0, MyGdxGame.WIDTH, MyGdxGame.HEIGHT);

    // baisch logo
    logoTexture = new Texture(Gdx.files.internal("data/graphics/Logo.png"));
    logoRegion = new TextureRegion(logoTexture, 0, 0, 394, 271);
    logoImage = new Image(logoRegion);

    button = new TextButton(t("menu.ready"), MyGdxGame.skin);

    button.setSize(button.getPrefWidth() + 20, button.getPrefHeight());

    // logoImage kept for the small session-list logo only — not shown on name-entry screen
    logoImage.setPosition((MyGdxGame.WIDTH - logoImage.getWidth()) / 2f,
        0.9f * MyGdxGame.HEIGHT - logoImage.getHeight());
    button.setPosition((MyGdxGame.WIDTH - button.getWidth()) / 2f, 0.1f * MyGdxGame.HEIGHT);

    // The Joker face, BAISCH title, and suit symbols are all rendered via the
    // #name-entry-logo DOM overlay (see mobile.html / HtmlLauncher.java).
    // Nothing is added to the LibGDX group for the name-entry logo here.

    // Starting hero selector (for testing)
    Array<String> heroNames = new Array<String>();
    heroNames.add(heroOptionLabel(HERO_RANDOM));
    heroNames.add(heroOptionLabel(HERO_NONE));
    heroNames.add(heroOptionLabel("Mercenaries"));
    heroNames.add(heroOptionLabel("Marshal"));
    heroNames.add(heroOptionLabel("Spy"));
    heroNames.add(heroOptionLabel("Battery Tower"));
    heroNames.add(heroOptionLabel("Merchant"));
    heroNames.add(heroOptionLabel("Priest"));
    heroNames.add(heroOptionLabel("Reservists"));
    heroNames.add(heroOptionLabel("Banneret"));
    heroNames.add(heroOptionLabel("Saboteurs"));
    heroNames.add(heroOptionLabel("Fortified Tower"));
    heroNames.add(heroOptionLabel("Magician"));
    heroNames.add(heroOptionLabel("Warlord"));

    heroSelectBox = new SelectBox<String>(MyGdxGame.skin);
    heroSelectBox.setItems(heroNames);
    heroSelectBox.setSelected(heroOptionLabel(HERO_RANDOM));
    menuState.setStartingHero(HERO_RANDOM);
    heroSelectBox.setSize(140f, 44f);
    heroSelectBox.setPosition((MyGdxGame.WIDTH - heroSelectBox.getWidth()) / 2f, 0.21f * MyGdxGame.HEIGHT);
    heroSelectBox.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
        if (updatingDropdown) return;
        String selected = canonicalHeroFromOption(heroSelectBox.getSelected());
        menuState.setStartingHero(selected);
        socket.emit("heroSelected", selected);
      }
    });

    button.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        MyGdxGame.turnNotifier.requestPermission(new Runnable() {
          @Override public void run() { show(); }
        });
        socket.emit("setUserReady", menuState.getMyUserID());
      };
    });

    // button is NOT in the group so it only appears on the lobby screen
    // logoImage is intentionally NOT added to the group — only used for the small session-list logo

    menuStage.addActor(group);

    currentUsersCount = menuState.getUsers().size();

    Gdx.input.setInputProcessor(menuStage);

  }

  public void create() {

  }

  /**
   * Build a dropdown item list containing "Random", "None", plus all heroes not reserved by
   * others, always including {@code currentSelection} so the current value stays visible.
   */
  private Array<String> buildHeroDropdownItems(String currentSelection) {
    Array<String> items = new Array<String>();
    items.add(heroOptionLabel(HERO_RANDOM));
    items.add(heroOptionLabel(HERO_NONE));
    for (int i = 0; i < ALL_HERO_NAMES.length; i++) {
      String h = ALL_HERO_NAMES[i];
      if (!reservedByOthers.contains(h) || h.equals(currentSelection)) {
        items.add(heroOptionLabel(h));
      }
    }
    return items;
  }

  /**
   * Rebuild the hero dropdown items, excluding heroes that have been reserved by other lobby
   * players. The current player's own selection is preserved when possible; if their hero was
   * taken by someone else it is reset to "None".
   */
  private void refreshHeroDropdown() {
    String currentSelected = canonicalHeroFromOption(heroSelectBox.getSelected());
    // Treat null (empty SelectBox) the same as "Random" so we don't wipe startingHero.
    if (currentSelected == null) currentSelected = HERO_RANDOM;
    Array<String> items = new Array<String>();
    items.add(heroOptionLabel(HERO_RANDOM));
    items.add(heroOptionLabel(HERO_NONE));
    for (int i = 0; i < ALL_HERO_NAMES.length; i++) {
      if (!reservedByOthers.contains(ALL_HERO_NAMES[i])) {
        items.add(heroOptionLabel(ALL_HERO_NAMES[i]));
      }
    }
    updatingDropdown = true;
    heroSelectBox.setItems(items);
    // Keep the previous selection if it is still available; otherwise fall back to "Random".
    if (currentSelected.equals(HERO_RANDOM) || currentSelected.equals(HERO_NONE)) {
      heroSelectBox.setSelected(heroOptionLabel(currentSelected));
    } else if (!reservedByOthers.contains(currentSelected)) {
      heroSelectBox.setSelected(heroOptionLabel(currentSelected));
    } else {
      // Hero was reserved by another player — reset to Random.
      heroSelectBox.setSelected(heroOptionLabel(HERO_RANDOM));
      menuState.setStartingHero(HERO_RANDOM);
    }
    updatingDropdown = false;
  }

  @Override
  public void show() {
    if (MyGdxGame.onMenuScreenActive != null) MyGdxGame.onMenuScreenActive.run();
    heroSelectBox.hideList();
    menuStage.clear();
    accountBtnAdded = false;
    if (menuBgTexture == null) menuBgTexture = new Texture(Gdx.files.internal("data/graphics/bg_darkmoon.jpg"));
    Image menuBg = new Image(menuBgTexture);
    menuBg.setFillParent(true);
    menuStage.addActor(menuBg);

    if (disconnectedByDuplicateTab) {
      if (MyGdxGame.onNameEntryScreenDone != null) MyGdxGame.onNameEntryScreenDone.run();
      showDuplicateTabScreen();
    } else if (reconnecting) {
      if (MyGdxGame.onNameEntryScreenDone != null) MyGdxGame.onNameEntryScreenDone.run();
      showReconnectingScreen();
    } else if (!authChoiceShown) {
      // No prior identity (first visit) — show the auth-choice screen.
      menuStage.addActor(group);
      showAuthChoiceScreen();
    } else if (!nameConfirmed && inRegisterScreen) {
      menuStage.addActor(group);
      showRegisterScreen();
    } else if (!nameConfirmed && inLoginScreen) {
      menuStage.addActor(group);
      showLoginScreen();
    } else if (!nameConfirmed) {
      // Logo only shown on the name-entry screen.
      menuStage.addActor(group);
      showNameEntryScreen();
    } else if (!lobbyJoined && inSessionCreate) {
      if (MyGdxGame.onNameEntryScreenDone != null) MyGdxGame.onNameEntryScreenDone.run();
      showSessionCreateScreen();
    } else if (!lobbyJoined) {
      if (MyGdxGame.onNameEntryScreenDone != null) MyGdxGame.onNameEntryScreenDone.run();
      showSessionListScreen();
    } else {
      if (MyGdxGame.onNameEntryScreenDone != null) MyGdxGame.onNameEntryScreenDone.run();
      showLobbyScreen();
    }
    if (MyGdxGame.nativeMusicButton) {
      if (MyGdxGame.onLanguageUiUpdate != null) MyGdxGame.onLanguageUiUpdate.run();
      if (MyGdxGame.onAccountUiUpdate  != null) MyGdxGame.onAccountUiUpdate.run();
    } else {
      addLanguageButtons(menuStage);
    }
  }

  /** Returns the icon texture for the given language code, or null if unavailable. */
  private Texture getLanguageIcon(String lang) {
    if (Localization.EN.equals(lang)) {
      if (languageEnIcon == null) {
        try { languageEnIcon = new Texture(Gdx.files.internal("data/graphics/ui/lang_en.png")); }
        catch (Exception ignored) {}
      }
      return languageEnIcon;
    } else if (Localization.DE.equals(lang)) {
      if (languageDeIcon == null) {
        try { languageDeIcon = new Texture(Gdx.files.internal("data/graphics/ui/lang_de.png")); }
        catch (Exception ignored) {}
      }
      return languageDeIcon;
    } else if (Localization.RU.equals(lang)) {
      if (languageRuIcon == null) {
        try { languageRuIcon = new Texture(Gdx.files.internal("data/graphics/ui/lang_ru.png")); }
        catch (Exception ignored) {}
      }
      return languageRuIcon;
    } else if (Localization.NO.equals(lang)) {
      if (languageNoIcon == null) {
        try { languageNoIcon = new Texture(Gdx.files.internal("data/graphics/ui/lang_no.png")); }
        catch (Exception ignored) {}
      }
      return languageNoIcon;
    } else if (Localization.IT.equals(lang)) {
      if (languageItIcon == null) {
        try { languageItIcon = new Texture(Gdx.files.internal("data/graphics/ui/lang_it.png")); }
        catch (Exception ignored) {}
      }
      return languageItIcon;
    }
    return null;
  }

  private void addLanguageButtons(final Stage stage) {
    // Supported languages in display order — extend this list to add more languages.
    final String[] langs = { Localization.EN, Localization.DE, Localization.RU, Localization.NO, Localization.IT };
    final String[] langLabels = { "EN", "DE", "RU", "NO", "IT" };

    float iconW = 48f;
    float iconH = 32f;
    float gap = 6f;
    float rightMargin = 10f;
    float musicW = new TextButton(MyGdxGame.playerStorage.getMusicEnabled() ? t("menu.musicOn") : t("menu.musicOff"), MyGdxGame.skin).getPrefWidth() + 20f;
    // When the account button is present (logged-in screens), the music button has been
    // shifted left by (ACCOUNT_BTN_SIZE + gap). Match that offset here.
    float accountOffset = accountBtnAdded ? (ACCOUNT_BTN_SIZE + gap) : 0f;
    float musicX = MyGdxGame.WIDTH - musicW - rightMargin - accountOffset;
    float btnX   = musicX - iconW - gap;
    float btnY   = MyGdxGame.HEIGHT - iconH - 10f;

    // ── Build the current-language button ──────────────────────────────────────
    final String currentLang = Localization.getLanguage();
    Texture curTex = getLanguageIcon(currentLang);
    final Actor langBtn;
    if (curTex != null) {
      langBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(curTex)));
    } else {
      // Fallback: text label showing the current language code
      langBtn = new TextButton(currentLang.toUpperCase(), MyGdxGame.skin);
    }
    langBtn.setSize(iconW, iconH);
    langBtn.setPosition(btnX, btnY);

    // ── Popup picker (shown when the button is tapped) ─────────────────────────
    final Table popup = new Table(MyGdxGame.skin);
    popup.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 0.95f)));
    popup.pad(4f);
    popup.setVisible(false);

    for (int i = 0; i < langs.length; i++) {
      final String lang = langs[i];
      final String label = langLabels[i];
      if (lang.equals(currentLang)) continue; // skip currently active language

      Texture tex = getLanguageIcon(lang);
      final Actor option;
      if (tex != null) {
        option = new ImageButton(new TextureRegionDrawable(new TextureRegion(tex)));
      } else {
        option = new TextButton(label, MyGdxGame.skin);
      }
      option.setSize(iconW, iconH);
      popup.add(option).size(iconW, iconH).pad(2f).row();
      option.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          Localization.setLanguage(lang);
          show(); // rebuild the screen with the new language
        }
      });
    }

    popup.pack();
    // Position the popup below the language button
    popup.setPosition(btnX, btnY - popup.getHeight() - 4f);

    langBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        popup.setVisible(!popup.isVisible());
      }
    });

    // Clicking anywhere outside the popup or the button closes it
    stage.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (!popup.isVisible()) return false;
        boolean inPopup = x >= popup.getX() && x <= popup.getX() + popup.getWidth()
                       && y >= popup.getY() && y <= popup.getY() + popup.getHeight();
        boolean inBtn   = x >= langBtn.getX() && x <= langBtn.getX() + langBtn.getWidth()
                       && y >= langBtn.getY() && y <= langBtn.getY() + langBtn.getHeight();
        if (!inPopup && !inBtn) popup.setVisible(false);
        return false;
      }
    });

    stage.addActor(langBtn);
    stage.addActor(popup);
  }

  private void showDuplicateTabScreen() {
    float cx = MyGdxGame.WIDTH / 2f;
    Table panel = new Table(MyGdxGame.skin);
    panel.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.38f)));
    panel.pad(28f, 36f, 28f, 36f);
    Label msg = new Label(
      t("menu.duplicateTab.line1") + "\n" + t("menu.duplicateTab.line2"),
      MyGdxGame.skin);
    panel.add(msg);
    panel.pack();
    panel.setPosition(
        Math.round(cx - panel.getWidth() / 2f),
        Math.round(MyGdxGame.HEIGHT / 2f - panel.getHeight() / 2f));
    menuStage.addActor(panel);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showReconnectingScreen() {
    float cx = MyGdxGame.WIDTH / 2f;
    Table panel = new Table(MyGdxGame.skin);
    panel.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.38f)));
    panel.pad(28f, 36f, 28f, 36f);
    Label msg = new Label(t("menu.reconnecting"), MyGdxGame.skin);
    TextButton returnBtn = new TextButton(t("menu.returnLobby"), MyGdxGame.skin);
    returnBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        clearReconnectState();
        show();
      }
    });
    panel.add(msg).padBottom(20f);
    panel.row();
    panel.add(returnBtn);
    panel.pack();
    panel.setPosition(
        Math.round(cx - panel.getWidth() / 2f),
        Math.round(MyGdxGame.HEIGHT / 2f - panel.getHeight() / 2f));
    menuStage.addActor(panel);
    Gdx.input.setInputProcessor(menuStage);
  }

  /** Clears the reconnect state and saved session so the lobby is shown cleanly. */
  private void clearReconnectState() {
    MyGdxGame.playerStorage.clearSessionId();
    reconnecting = false;
    reconnectElapsed = 0f;
  }

  /** Lazy-loads an avatar texture from data/avatars/<name>.png; returns null if unavailable. */
  private Texture getAvatarTexture(String name) {
    Texture tex = avatarTextures.get(name);
    if (tex == null) {
      try {
        tex = new Texture(Gdx.files.internal("data/avatars/" + name + ".png"));
        avatarTextures.put(name, tex);
      } catch (Exception e) {
        // avatar file not present — skip silently
      }
    }
    return tex;
  }

  /** Creates a small avatar Image (size x size) for the given icon name, or null if unavailable. */
  private Image createAvatarImage(String iconName, float size) {
    if (iconName == null || iconName.isEmpty()) return null;
    Texture tex = getAvatarTexture(iconName);
    if (tex == null) return null;
    Image img = new Image(tex);
    img.setSize(size, size);
    return img;
  }

  /** Wraps a Label with an optional leading avatar icon inside a horizontal Table cell. */
  private Table buildNameCell(Label nameLabel, String iconName) {
    Table cell = new Table();
    Image avatar = createAvatarImage(iconName, 22f);
    if (avatar != null) {
      cell.add(avatar).size(22f, 22f).padRight(5f);
    }
    cell.add(nameLabel);
    return cell;
  }

  /**
   * Auth-choice screen — shown on first visit (no saved name and no saved username).
   * Three buttons: Join as guest, Register, Log in with account.
   * Layout mirrors the name-entry screen (buttons stacked from bottom).
   */
  private void showAuthChoiceScreen() {
    if (MyGdxGame.onNameEntryScreenActive != null) MyGdxGame.onNameEntryScreenActive.run();
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;
    float gap = 12f;
    float bMargin = 24f;

    TextButton guestBtn = new TextButton(t("menu.auth.joinGuest"), MyGdxGame.skin);
    TextButton registerBtn = new TextButton(t("menu.auth.register"), MyGdxGame.skin);
    TextButton loginBtn = new TextButton(t("menu.auth.loginWithAccount"), MyGdxGame.skin);

    // Use the skin's preferred dimensions like all other screens do
    guestBtn.pack();
    registerBtn.pack();
    loginBtn.pack();
    float btnH = guestBtn.getPrefHeight() + 20f;
    float btnW = Math.max(
        Math.max(guestBtn.getPrefWidth(), registerBtn.getPrefWidth()),
        loginBtn.getPrefWidth()) + 60f;

    guestBtn.setSize(btnW, btnH);
    registerBtn.setSize(btnW, btnH);
    loginBtn.setSize(btnW, btnH);

    float loginY    = bMargin;
    float registerY = loginY + btnH + gap;
    float guestY    = registerY + btnH + gap;

    guestBtn.setPosition(Math.round(cx - btnW / 2f), Math.round(guestY));
    registerBtn.setPosition(Math.round(cx - btnW / 2f), Math.round(registerY));
    loginBtn.setPosition(Math.round(cx - btnW / 2f), Math.round(loginY));

    guestBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        authChoiceShown = true;
        authErrorMessage = "";
        inRegisterScreen = false;
        inLoginScreen = false;
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { show(); } });
      }
    });

    registerBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        authChoiceShown = true;
        authErrorMessage = "";
        inRegisterScreen = true;
        inLoginScreen = false;
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { show(); } });
      }
    });

    loginBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        authChoiceShown = true;
        authErrorMessage = "";
        inRegisterScreen = false;
        inLoginScreen = true;
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { show(); } });
      }
    });

    menuStage.addActor(guestBtn);
    menuStage.addActor(registerBtn);
    menuStage.addActor(loginBtn);

    Label subtitle = new Label(t("menu.subtitle"), MyGdxGame.skin);
    subtitle.setColor(1f, 1f, 1f, 0.65f);
    subtitle.pack();
    subtitle.setPosition(
        Math.round(cx - subtitle.getWidth() / 2f),
        Math.round(guestY + btnH + 12f));
    menuStage.addActor(subtitle);

    addMusicToggleButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  /**
   * Register screen — username, password and avatar fields.
   * On submit emits registerAccount to the server.
   */
  private void showRegisterScreen() {
    if (MyGdxGame.onNameEntryScreenActive != null) MyGdxGame.onNameEntryScreenActive.run();
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    final com.badlogic.gdx.scenes.scene2d.ui.TextField usernameField =
        new com.badlogic.gdx.scenes.scene2d.ui.TextField("", MyGdxGame.skin);
    usernameField.setMessageText(t("menu.auth.usernamePlaceholder"));
    usernameField.setWidth(250f);
    usernameField.setHeight(50f);

    final com.badlogic.gdx.scenes.scene2d.ui.TextField passwordField =
        new com.badlogic.gdx.scenes.scene2d.ui.TextField("", MyGdxGame.skin);
    passwordField.setMessageText(t("menu.auth.passwordPlaceholder"));
    passwordField.setPasswordMode(true);
    passwordField.setPasswordCharacter('*');
    passwordField.setWidth(250f);
    passwordField.setHeight(50f);

    final TextButton submitBtn = new TextButton(t("menu.auth.register.submit"), MyGdxGame.skin) {
      @Override public void act(float delta) {
        super.act(delta);
        boolean ok = !usernameField.getText().trim().isEmpty()
                  && !passwordField.getText().isEmpty()
                  && !selectedIcon.isEmpty()
                  && !authPending;
        setDisabled(!ok);
        getLabel().setColor(ok ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 1f));
      }
    };
    submitBtn.pack();
    submitBtn.setSize(submitBtn.getPrefWidth() + 40f, submitBtn.getPrefHeight() + 20f);

    final Runnable doRegister = new Runnable() {
      @Override public void run() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty()) { authErrorMessage = t("menu.auth.error.usernameRequired"); show(); return; }
        if (password.isEmpty()) { authErrorMessage = t("menu.auth.error.passwordRequired"); show(); return; }
        if (password.length() < 4) { authErrorMessage = t("menu.auth.error.passwordTooShort"); show(); return; }
        if (selectedIcon.isEmpty()) return;
        MyGdxGame.keyboardHelper.hideKeyboard();
        authPending = true;
        try {
          JSONObject data = new JSONObject();
          data.put("username", username);
          data.put("password", password);
          data.put("icon", selectedIcon);
          data.put("token", MyGdxGame.playerStorage.getToken());
          socket.emit("registerAccount", data);
        } catch (JSONException e) { authPending = false; }
      }
    };

    submitBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        if (submitBtn.isDisabled()) return;
        doRegister.run();
      }
    });

    usernameField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
      @Override
      public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int btn) {
        MyGdxGame.keyboardHelper.showKeyboard(usernameField, doRegister);
        return false;
      }
    });

    passwordField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
      @Override
      public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int btn) {
        MyGdxGame.keyboardHelper.showKeyboard(passwordField, doRegister);
        return false;
      }
    });

    // Back button
    TextButton backBtn = new TextButton(t("common.back"), MyGdxGame.skin);
    backBtn.pack();
    backBtn.setSize(backBtn.getPrefWidth() + 20f, backBtn.getPrefHeight() + 10f);
    backBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        authChoiceShown = false;
        inRegisterScreen = false;
        authErrorMessage = "";
        authPending = false;
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { show(); } });
      }
    });

    // Avatar selector (reuse same logic as name-entry screen)
    float selectorMaxW = MyGdxGame.WIDTH - 24f;
    Label avatarLabel = new Label(t("menu.avatarChoose"), MyGdxGame.skin);
    avatarLabel.setColor(1f, 1f, 1f, 0.70f);
    final Table[] avatarWrappers = new Table[AVATAR_NAMES.length];
    Table avatarRow = new Table();
    avatarRow.pad(4f, 0f, 0f, 0f);
    for (int ai = 0; ai < AVATAR_NAMES.length; ai++) {
      final String avName = AVATAR_NAMES[ai];
      final int avIdx = ai;
      Texture avTex = getAvatarTexture(avName);
      if (avTex == null) continue;
      Image avImg = new Image(avTex);
      boolean selected = avName.equals(selectedIcon);
      Color borderCol = selected ? new Color(0.98f, 0.80f, 0.25f, 1f) : new Color(1f, 1f, 1f, 0.18f);
      final Table wrapper = new Table();
      wrapper.setBackground(MyGdxGame.skin.newDrawable("white", borderCol));
      wrapper.add(avImg).size(88f, 88f).pad(4f);
      avatarWrappers[avIdx] = wrapper;
      wrapper.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          selectedIcon = avName;
          MyGdxGame.playerStorage.saveIcon(avName);
          for (int j = 0; j < AVATAR_NAMES.length; j++) {
            if (avatarWrappers[j] == null) continue;
            Color c = AVATAR_NAMES[j].equals(selectedIcon)
                ? new Color(0.98f, 0.80f, 0.25f, 1f)
                : new Color(1f, 1f, 1f, 0.18f);
            avatarWrappers[j].setBackground(MyGdxGame.skin.newDrawable("white", c));
          }
        }
      });
      avatarRow.add(wrapper).padRight(avIdx < AVATAR_NAMES.length - 1 ? 6f : 0f);
    }
    ScrollPane avatarScroll = new ScrollPane(avatarRow, MyGdxGame.skin);
    avatarScroll.setScrollingDisabled(false, true);
    avatarScroll.setFadeScrollBars(false);
    avatarScroll.setOverscroll(false, false);
    if (!selectedIcon.isEmpty()) {
      for (int ai = 0; ai < AVATAR_NAMES.length; ai++) {
        if (AVATAR_NAMES[ai].equals(selectedIcon)) {
          final float targetX = ai * (88f + 4f + 4f + 6f);
          avatarScroll.layout();
          avatarScroll.setScrollX(Math.max(0f, targetX - selectorMaxW / 2f));
          break;
        }
      }
    }
    Table avatarSelector = new Table(MyGdxGame.skin);
    avatarSelector.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.28f)));
    avatarSelector.pad(8f, 12f, 8f, 12f);
    avatarSelector.add(avatarLabel).padBottom(6f).row();
    avatarSelector.add(avatarScroll).width(selectorMaxW - 24f).height(120f);
    avatarSelector.pack();

    // Bottom-up layout
    float bMargin = 16f;
    float elemGap = 10f;
    float backY     = bMargin;
    float submitY   = backY + backBtn.getHeight() + elemGap;
    float avatarY   = submitY + submitBtn.getHeight() + elemGap;
    float passY     = avatarY + avatarSelector.getHeight() + elemGap;
    float userY     = passY + passwordField.getHeight() + elemGap;

    backBtn.setPosition(Math.round(cx - backBtn.getWidth() / 2f), Math.round(backY));
    submitBtn.setPosition(Math.round(cx - submitBtn.getWidth() / 2f), Math.round(submitY));
    avatarSelector.setPosition(Math.round(cx - avatarSelector.getWidth() / 2f), Math.round(avatarY));
    passwordField.setPosition(Math.round(cx - passwordField.getWidth() / 2f), Math.round(passY));
    usernameField.setPosition(Math.round(cx - usernameField.getWidth() / 2f), Math.round(userY));

    // Title
    Label titleLabel = new Label(t("menu.auth.registerTitle"), MyGdxGame.skin);
    titleLabel.setColor(1f, 1f, 1f, 0.85f);
    titleLabel.pack();
    titleLabel.setPosition(Math.round(cx - titleLabel.getWidth() / 2f), Math.round(userY + usernameField.getHeight() + 12f));

    menuStage.addActor(usernameField);
    menuStage.addActor(passwordField);
    menuStage.addActor(avatarSelector);
    menuStage.addActor(submitBtn);
    menuStage.addActor(backBtn);
    menuStage.addActor(titleLabel);

    if (!authErrorMessage.isEmpty()) {
      Label errLabel = new Label(authErrorMessage, MyGdxGame.skin);
      errLabel.setColor(1f, 0.4f, 0.4f, 1f);
      errLabel.pack();
      errLabel.setPosition(Math.round(cx - errLabel.getWidth() / 2f), Math.round(submitY + submitBtn.getHeight() + 6f));
      menuStage.addActor(errLabel);
    }

    addMusicToggleButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  /**
   * Login screen — username and password fields.
   * On submit emits loginAccount to the server.
   */
  private void showLoginScreen() {
    if (MyGdxGame.onNameEntryScreenActive != null) MyGdxGame.onNameEntryScreenActive.run();
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    final com.badlogic.gdx.scenes.scene2d.ui.TextField usernameField =
        new com.badlogic.gdx.scenes.scene2d.ui.TextField("", MyGdxGame.skin);
    usernameField.setMessageText(t("menu.auth.usernamePlaceholder"));
    usernameField.setWidth(250f);
    usernameField.setHeight(50f);

    final com.badlogic.gdx.scenes.scene2d.ui.TextField passwordField =
        new com.badlogic.gdx.scenes.scene2d.ui.TextField("", MyGdxGame.skin);
    passwordField.setMessageText(t("menu.auth.passwordPlaceholder"));
    passwordField.setPasswordMode(true);
    passwordField.setPasswordCharacter('*');
    passwordField.setWidth(250f);
    passwordField.setHeight(50f);

    final TextButton submitBtn = new TextButton(t("menu.auth.login.submit"), MyGdxGame.skin) {
      @Override public void act(float delta) {
        super.act(delta);
        boolean ok = !usernameField.getText().trim().isEmpty()
                  && !passwordField.getText().isEmpty()
                  && !authPending;
        setDisabled(!ok);
        getLabel().setColor(ok ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 1f));
      }
    };
    submitBtn.pack();
    submitBtn.setSize(submitBtn.getPrefWidth() + 40f, submitBtn.getPrefHeight() + 20f);

    final Runnable doLogin = new Runnable() {
      @Override public void run() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty()) { authErrorMessage = t("menu.auth.error.usernameRequired"); show(); return; }
        if (password.isEmpty()) { authErrorMessage = t("menu.auth.error.passwordRequired"); show(); return; }
        MyGdxGame.keyboardHelper.hideKeyboard();
        authPending = true;
        try {
          JSONObject data = new JSONObject();
          data.put("username", username);
          data.put("password", password);
          data.put("token", MyGdxGame.playerStorage.getToken());
          socket.emit("loginAccount", data);
        } catch (JSONException e) { authPending = false; }
      }
    };

    submitBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        if (submitBtn.isDisabled()) return;
        doLogin.run();
      }
    });

    usernameField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
      @Override
      public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int btn) {
        MyGdxGame.keyboardHelper.showKeyboard(usernameField, doLogin);
        return false;
      }
    });

    passwordField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
      @Override
      public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int btn) {
        MyGdxGame.keyboardHelper.showKeyboard(passwordField, doLogin);
        return false;
      }
    });

    // Back button
    TextButton backBtn = new TextButton(t("common.back"), MyGdxGame.skin);
    backBtn.pack();
    backBtn.setSize(backBtn.getPrefWidth() + 20f, backBtn.getPrefHeight() + 10f);
    backBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        authChoiceShown = false;
        inLoginScreen = false;
        authErrorMessage = "";
        authPending = false;
        Gdx.app.postRunnable(new Runnable() { @Override public void run() { show(); } });
      }
    });

    // Bottom-up layout
    float bMargin = 16f;
    float elemGap = 12f;
    float backY   = bMargin;
    float submitY = backY + backBtn.getHeight() + elemGap;
    float passY   = submitY + submitBtn.getHeight() + elemGap;
    float userY   = passY + passwordField.getHeight() + elemGap;

    backBtn.setPosition(Math.round(cx - backBtn.getWidth() / 2f), Math.round(backY));
    submitBtn.setPosition(Math.round(cx - submitBtn.getWidth() / 2f), Math.round(submitY));
    passwordField.setPosition(Math.round(cx - passwordField.getWidth() / 2f), Math.round(passY));
    usernameField.setPosition(Math.round(cx - usernameField.getWidth() / 2f), Math.round(userY));

    Label titleLabel = new Label(t("menu.auth.loginTitle"), MyGdxGame.skin);
    titleLabel.setColor(1f, 1f, 1f, 0.85f);
    titleLabel.pack();
    titleLabel.setPosition(Math.round(cx - titleLabel.getWidth() / 2f), Math.round(userY + usernameField.getHeight() + 12f));

    menuStage.addActor(usernameField);
    menuStage.addActor(passwordField);
    menuStage.addActor(submitBtn);
    menuStage.addActor(backBtn);
    menuStage.addActor(titleLabel);

    if (!authErrorMessage.isEmpty()) {
      Label errLabel = new Label(authErrorMessage, MyGdxGame.skin);
      errLabel.setColor(1f, 0.4f, 0.4f, 1f);
      errLabel.pack();
      errLabel.setPosition(Math.round(cx - errLabel.getWidth() / 2f), Math.round(submitY + submitBtn.getHeight() + 6f));
      menuStage.addActor(errLabel);
    }

    addMusicToggleButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showNameEntryScreen() {    if (MyGdxGame.onNameEntryScreenActive != null) MyGdxGame.onNameEntryScreenActive.run();
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    // Name entry: TextField + OK button using the JSNI keyboard helper so the
    // mobile browser opens its native keyboard without a browser dialog popup.
    final com.badlogic.gdx.scenes.scene2d.ui.TextField nameField =
        new com.badlogic.gdx.scenes.scene2d.ui.TextField(menuState.getMyName(), MyGdxGame.skin);
    nameField.setMessageText(t("menu.namePlaceholder"));
    final float btnH = button.getPrefHeight();

    final Runnable doConfirm = new Runnable() {
      @Override public void run() {
        String name = nameField.getText().trim();
        if (name.isEmpty() || selectedIcon.isEmpty()) return;
        MyGdxGame.keyboardHelper.hideKeyboard();
        menuState.setMyName(name);
        MyGdxGame.playerStorage.saveName(name);
        nameConfirmed = true;
        try {
          JSONObject reg = new JSONObject();
          reg.put("name", name);
          reg.put("token", MyGdxGame.playerStorage.getToken());
          reg.put("icon", selectedIcon);
          socket.emit("registerPlayer", reg);
        } catch (JSONException e) { /* ignore */ }
        Gdx.app.postRunnable(new Runnable() {
          @Override public void run() { show(); }
        });
      }
    };

    nameField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
      @Override
      public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                               float x, float y, int pointer, int btn) {
        MyGdxGame.keyboardHelper.showKeyboard(nameField, doConfirm);
        return false;
      }
    });

    // ── Login button — enabled state driven by act() every frame so it reacts
    // correctly regardless of whether text arrives via keyTyped or setText().
    final TextButton loginBtn = new TextButton(t("menu.login"), MyGdxGame.skin) {
      @Override public void act(float delta) {
        super.act(delta);
        boolean ok = !nameField.getText().trim().isEmpty() && !selectedIcon.isEmpty();
        setDisabled(!ok);
        getLabel().setColor(ok ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 1f));
      }
    };
    loginBtn.pack();
    loginBtn.setSize(loginBtn.getPrefWidth() + 40f, loginBtn.getPrefHeight() + 20f);
    loginBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (loginBtn.isDisabled()) return;
        doConfirm.run();
      }
    });

    nameField.setWidth(250f);
    nameField.setHeight(50f);
    // Position is set after avatarSelector is packed (bottom-up layout below).

    // Avatar selector — shown below the name field.
    // The icons row is placed inside a horizontal ScrollPane so it fits any screen width.
    // Clicking an avatar updates the selection in-place WITHOUT rebuilding the screen,
    // so any text the player has typed into the name field is preserved.
    float selectorMaxW = MyGdxGame.WIDTH - 24f;

    Label avatarLabel = new Label(t("menu.avatarChoose"), MyGdxGame.skin);
    avatarLabel.setColor(1f, 1f, 1f, 0.70f);

    final Table[] avatarWrappers = new Table[AVATAR_NAMES.length];
    Table avatarRow = new Table();
    avatarRow.pad(4f, 0f, 0f, 0f);
    for (int ai = 0; ai < AVATAR_NAMES.length; ai++) {
      final String avName = AVATAR_NAMES[ai];
      final int avIdx = ai;
      Texture avTex = getAvatarTexture(avName);
      if (avTex == null) continue;
      Image avImg = new Image(avTex);
      boolean selected = avName.equals(selectedIcon);
      Color borderCol = selected ? new Color(0.98f, 0.80f, 0.25f, 1f) : new Color(1f, 1f, 1f, 0.18f);
      final Table wrapper = new Table();
      wrapper.setBackground(MyGdxGame.skin.newDrawable("white", borderCol));
      wrapper.add(avImg).size(88f, 88f).pad(4f);
      avatarWrappers[avIdx] = wrapper;
      wrapper.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          selectedIcon = avName;
          MyGdxGame.playerStorage.saveIcon(avName);
          // Update avatar border highlights without rebuilding the whole screen.
          for (int j = 0; j < AVATAR_NAMES.length; j++) {
            if (avatarWrappers[j] == null) continue;
            boolean sel = AVATAR_NAMES[j].equals(selectedIcon);
            Color c = sel ? new Color(0.98f, 0.80f, 0.25f, 1f) : new Color(1f, 1f, 1f, 0.18f);
            avatarWrappers[j].setBackground(MyGdxGame.skin.newDrawable("white", c));
          }
          // loginBtn state is updated automatically via act() override.
        }
      });
      avatarRow.add(wrapper).padRight(avIdx < AVATAR_NAMES.length - 1 ? 6f : 0f);
    }

    ScrollPane avatarScroll = new ScrollPane(avatarRow, MyGdxGame.skin);
    avatarScroll.setScrollingDisabled(false, true); // horizontal only
    avatarScroll.setFadeScrollBars(false);
    avatarScroll.setOverscroll(false, false);

    // Scroll to show the selected avatar
    if (!selectedIcon.isEmpty()) {
      for (int ai = 0; ai < AVATAR_NAMES.length; ai++) {
        if (AVATAR_NAMES[ai].equals(selectedIcon)) {
          final float targetX = ai * (88f + 4f + 4f + 6f);
          avatarScroll.layout();
          avatarScroll.setScrollX(Math.max(0f, targetX - selectorMaxW / 2f));
          break;
        }
      }
    }

    Table avatarSelector = new Table(MyGdxGame.skin);
    avatarSelector.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.28f)));
    avatarSelector.pad(8f, 12f, 8f, 12f);
    avatarSelector.add(avatarLabel).padBottom(6f).row();
    avatarSelector.add(avatarScroll).width(selectorMaxW - 24f).height(120f);
    avatarSelector.pack();

    // ── Bottom-up layout: loginBtn → avatarSelector → nameField ────────────
    // Building upward from a fixed bottom margin guarantees no overlap.
    final float bMargin = 16f;
    final float elemGap = 10f;
    float loginY   = bMargin;
    float avatarY  = loginY  + loginBtn.getHeight()     + elemGap;
    float nameY    = avatarY + avatarSelector.getHeight() + elemGap;

    loginBtn.setPosition(
        Math.round(cx - loginBtn.getWidth() / 2f), Math.round(loginY));
    avatarSelector.setPosition(
        Math.round(cx - avatarSelector.getWidth() / 2f), Math.round(avatarY));
    nameField.setPosition(
        Math.round(cx - nameField.getWidth() / 2f), Math.round(nameY));

    menuStage.addActor(nameField);
    menuStage.addActor(avatarSelector);
    menuStage.addActor(loginBtn);

    // Subtitle — the DOM overlay (BAISCH + suits) occupies the upper half;
    // position this label just above the name field.
    Label subtitle = new Label(t("menu.subtitle"), MyGdxGame.skin);
    subtitle.setColor(1f, 1f, 1f, 0.65f);
    subtitle.pack();
    subtitle.setPosition(
        Math.round(cx - subtitle.getWidth() / 2f),
        Math.round(nameY + nameField.getHeight() + 12f));
    menuStage.addActor(subtitle);



    addMusicToggleButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showSessionListScreen() {
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    // ── Tab bar ──────────────────────────────────────────────────────────────
    // Plain labels (no button box) with a colored underline on the active tab.
    final Color ACTIVE_COLOR   = Color.WHITE;
    final Color INACTIVE_COLOR = new Color(1f, 1f, 1f, 0.35f);
    final Color UNDERLINE_COLOR = new Color(0.98f, 0.80f, 0.25f, 1f); // warm gold

    Label gamesTab   = new Label(t("menu.tab.games"),   MyGdxGame.skin, "title");
    Label playersTab = new Label(t("menu.tab.players"), MyGdxGame.skin, "title");
    // Use linear filter on title font for smooth downscale rendering
    MyGdxGame.skin.getFont("title").getRegion().getTexture()
        .setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                   com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
    gamesTab.setFontScale(0.55f);
    playersTab.setFontScale(0.55f);
    gamesTab.pack();
    playersTab.pack();

    float tabGap    = 32f;
    float tabsWidth = gamesTab.getWidth() + tabGap + playersTab.getWidth();
    float tabY      = 0.88f * MyGdxGame.HEIGHT;
    float underlineH = 3f;
    float underlinePad = 0f; // underline extends full label width

    gamesTab.setPosition(cx - tabsWidth / 2f, tabY);
    playersTab.setPosition(cx - tabsWidth / 2f + gamesTab.getWidth() + tabGap, tabY);

    gamesTab.setColor(!showPlayersTab ? ACTIVE_COLOR : INACTIVE_COLOR);
    playersTab.setColor(showPlayersTab  ? ACTIVE_COLOR : INACTIVE_COLOR);
    // Labels are visual only; click handling comes from the larger invisible hit actors.
    gamesTab.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    playersTab.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

    // Underline under the active tab
    Label activeTab = !showPlayersTab ? gamesTab : playersTab;
    Image underline = new Image(MyGdxGame.skin.newDrawable("white", UNDERLINE_COLOR));
    underline.setSize(activeTab.getWidth() - underlinePad * 2, underlineH);
    underline.setPosition(activeTab.getX() + underlinePad, activeTab.getY() - underlineH - 2f);

    // Hit areas: invisible touch actors behind each label so tap targets are generous
    com.badlogic.gdx.scenes.scene2d.Actor gamesHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    gamesHit.setBounds(gamesTab.getX() - 8f, tabY - 8f,
        gamesTab.getWidth() + 16f, gamesTab.getHeight() + 16f);
    gamesHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        showPlayersTab = false; MyGdxGame.playerStorage.saveShowPlayersTab(false); show();
      }
    });

    com.badlogic.gdx.scenes.scene2d.Actor playersHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    playersHit.setBounds(playersTab.getX() - 8f, tabY - 8f,
        playersTab.getWidth() + 16f, playersTab.getHeight() + 16f);
    playersHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        showPlayersTab = true; MyGdxGame.playerStorage.saveShowPlayersTab(true); show();
      }
    });

    menuStage.addActor(gamesHit);
    menuStage.addActor(playersHit);
    menuStage.addActor(underline);
    menuStage.addActor(gamesTab);
    menuStage.addActor(playersTab);

    if (!showPlayersTab) {
      // ── Games tab ───────────────────────────────────────────────────────────
      Table sessTable = new Table(MyGdxGame.skin);
      sessTable.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.35f)));
      sessTable.pad(14f, 18f, 14f, 18f);

      Label h1 = new Label(t("common.name"), MyGdxGame.skin);
      Label h2 = new Label(t("common.players"), MyGdxGame.skin);
      Label h3 = new Label("", MyGdxGame.skin);
      h1.setColor(1f, 1f, 1f, 0.9f);
      h2.setColor(1f, 1f, 1f, 0.9f);
      sessTable.add(h1).padRight(40).padBottom(8f).left();
      sessTable.add(h2).padRight(40).padBottom(8f);
      sessTable.add(h3).padBottom(8f);
      sessTable.row();
      Image hSep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.25f)));
      sessTable.add(hSep).colspan(3).growX().height(1f).padBottom(6f);
      sessTable.row();

      final java.util.List<SessionInfo> list = new java.util.ArrayList<SessionInfo>(sessionList);
      for (int si = 0; si < list.size(); si++) {
        final SessionInfo s = list.get(si);
        Label nameL = new Label(s.name, MyGdxGame.skin);
        Label countL = new Label(s.playerCount + "/" + s.maxSlots, MyGdxGame.skin);
        if (s.running) {
          TextButton watchBtn = new TextButton(t("menu.watch"), MyGdxGame.skin);
          watchBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              socket.emit("joinSessionSpectator", buildJoinData(s.id));
            }
          });
          sessTable.add(nameL).padRight(40).padBottom(6f).left();
          sessTable.add(countL).padRight(40).padBottom(6f);
          sessTable.add(watchBtn).padBottom(6f);
        } else {
          TextButton joinBtn = new TextButton(t("menu.join"), MyGdxGame.skin);
          joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              socket.emit("joinSession", buildJoinData(s.id));
            }
          });
          sessTable.add(nameL).padRight(40).padBottom(6f).left();
          sessTable.add(countL).padRight(40).padBottom(6f);
          sessTable.add(joinBtn).padBottom(6f);
        }
        sessTable.row();
        if (si < list.size() - 1) {
          Image sep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.14f)));
          sessTable.add(sep).colspan(3).growX().height(1f).padTop(2f).padBottom(5f);
          sessTable.row();
        }
      }

      if (list.isEmpty()) {
        Label empty = new Label(t("menu.noGames"), MyGdxGame.skin);
        empty.setColor(0.6f, 0.6f, 0.6f, 1f);
        sessTable.add(empty).colspan(3);
        sessTable.row();
      }

      ScrollPane sessScrollPane = new ScrollPane(sessTable, MyGdxGame.skin);
      sessScrollPane.setScrollingDisabled(true, false);
      sessScrollPane.setFadeScrollBars(false);
      float spW = MyGdxGame.WIDTH - 32f;
      // Position the scroll pane to sit between the tab bar and the button row,
      // with a safe gap above the buttons to avoid overlap.
      float spBtnH = button.getPrefHeight();
      float spBtnY = Math.round(0.15f * MyGdxGame.HEIGHT);
      float spBottom = spBtnY + spBtnH + 8f;
      float spH = tabY - 8f - spBottom;
      sessScrollPane.setSize(spW, spH);
      sessScrollPane.setPosition(Math.round(cx - spW / 2f), Math.round(spBottom));
      menuStage.addActor(sessScrollPane);

      // Evenly-spaced button row: Rules | Tutorial | New game
      float btnH = spBtnH;
      float gap = 8f;
      float margin = 16f;
      float btnW = (MyGdxGame.WIDTH - 2 * margin - 2 * gap) / 3f;
      float btnY = spBtnY;

      TextButton rulesBtn = new TextButton(t("menu.rules"), MyGdxGame.skin);
      rulesBtn.setSize(btnW, btnH);
      rulesBtn.setPosition(margin, btnY);
      rulesBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          Gdx.net.openURI(RULES_URL);
        }
      });

      TextButton tutorialBtn = new TextButton(t("menu.tutorial"), MyGdxGame.skin);
      tutorialBtn.setSize(btnW, btnH);
      tutorialBtn.setPosition(margin + btnW + gap, btnY);
      tutorialBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          game.setScreen(new TutorialSelectScreen(game, socket));
        }
      });

      TextButton createBtn = new TextButton(t("menu.newGame"), MyGdxGame.skin);
      createBtn.setSize(btnW, btnH);
      createBtn.setPosition(margin + 2 * (btnW + gap), btnY);
      createBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          inSessionCreate = true;
          show();
        }
      });

      menuStage.addActor(rulesBtn);
      menuStage.addActor(tutorialBtn);
      menuStage.addActor(createBtn);
    } else {
      // ── Players tab ─────────────────────────────────────────────────────────
      Table playersTable = new Table(MyGdxGame.skin);
      playersTable.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.35f)));
      playersTable.pad(14f, 18f, 14f, 18f);

      Label ph1 = new Label(t("common.name"), MyGdxGame.skin);
      Label ph2 = new Label(t("common.status"), MyGdxGame.skin);
      ph1.setColor(1f, 1f, 1f, 0.9f);
      ph2.setColor(1f, 1f, 1f, 0.9f);
      playersTable.add(ph1).padRight(40).padBottom(8f).left();
      playersTable.add(ph2).padBottom(8f);
      playersTable.row();
      Image phSep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.25f)));
      playersTable.add(phSep).colspan(2).growX().height(1f).padBottom(6f);
      playersTable.row();

      final java.util.List<OnlinePlayerInfo> snapshot =
          new java.util.ArrayList<OnlinePlayerInfo>(onlinePlayers);
      for (int pi = 0; pi < snapshot.size(); pi++) {
        OnlinePlayerInfo p = snapshot.get(pi);
        Label nameL = new Label(p.name, MyGdxGame.skin);
        if (p.id.equals(menuState.getMyUserID())) nameL.setColor(Color.GOLD);
        Label statusL = new Label(p.status, MyGdxGame.skin);
        if (p.status.startsWith("In game")) statusL.setColor(Color.GREEN);
        else if (p.status.startsWith("In lobby")) statusL.setColor(Color.YELLOW);
        else if (p.status.startsWith("Watching")) statusL.setColor(Color.CYAN);
        playersTable.add(buildNameCell(nameL, p.icon)).padRight(40).padBottom(6f).left();
        playersTable.add(statusL).padBottom(6f);
        playersTable.row();
        if (pi < snapshot.size() - 1) {
          Image sep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.14f)));
          playersTable.add(sep).colspan(2).growX().height(1f).padTop(2f).padBottom(5f);
          playersTable.row();
        }
      }

      if (snapshot.isEmpty()) {
        Label empty = new Label(t("menu.noPlayers"), MyGdxGame.skin);
        empty.setColor(0.6f, 0.6f, 0.6f, 1f);
        playersTable.add(empty).colspan(2);
        playersTable.row();
      }

      playersTable.pack();
      playersTable.setPosition(Math.round(cx - playersTable.getWidth() / 2f), Math.round(0.45f * MyGdxGame.HEIGHT));
      menuStage.addActor(playersTable);

    }

    addTopRightButtons(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showSessionCreateScreen() {
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    // ── Back button (top-left) ───────────────────────────────────────────────
    TextButton backBtn = new TextButton(t("common.back"), MyGdxGame.skin);
    backBtn.pack();
    backBtn.setPosition(10, MyGdxGame.HEIGHT - backBtn.getHeight() - 10);
    backBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        inSessionCreate = false;
        show();
      }
    });
    menuStage.addActor(backBtn);

    // ── Title ────────────────────────────────────────────────────────────────
    Label title = new Label(t("menu.newGame"), MyGdxGame.skin);
    title.setFontScale(1.3f);
    title.pack();

    // ── Game name button ─────────────────────────────────────────────────────
    final String nameDisplay = pendingSessionName.isEmpty() ? t("menu.setup.setNameOptional") : pendingSessionName;
    final TextButton gameNameBtn = new TextButton(nameDisplay, MyGdxGame.skin);
    gameNameBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
          @Override
          public void input(String text) {
            pendingSessionName = text.trim();
            Gdx.app.postRunnable(new Runnable() {
              @Override public void run() { show(); }
            });
          }
          @Override public void canceled() { /* keep current name */ }
        }, t("menu.setup.enterGameNameTitle"), pendingSessionName, t("menu.setup.enterGameNamePrompt"));
      }
    });

    // ── Starting cards selector ──────────────────────────────────────────────
    Label cardsLabel = new Label(t("menu.setup.startingCards"), MyGdxGame.skin);
    final SelectBox<String> cardsBox = new SelectBox<String>(MyGdxGame.skin);
    Array<String> cardOptions = new Array<String>();
    for (int n = 6; n <= 10; n++) cardOptions.add(String.valueOf(n));
    cardsBox.setItems(cardOptions);
    cardsBox.setSelected(String.valueOf(pendingStartingCards));

    // ── Hero assign mode selector ────────────────────────────────────────────
    Label heroModeLabel = new Label(t("menu.setup.heroAssignment"), MyGdxGame.skin);
    final SelectBox<String> heroModeBox = new SelectBox<String>(MyGdxGame.skin);
    final String[] HERO_MODE_DISPLAY = {
      t("menu.setup.heroAssign.cardValue"),
      t("menu.setup.heroAssign.cardColor"),
      t("menu.setup.heroAssign.freeChoice")
    };
    final String[] HERO_MODE_KEYS    = {"value_mapping", "color_mapping", "free_selector"};
    Array<String> heroModeOptions = new Array<String>();
    for (String d : HERO_MODE_DISPLAY) heroModeOptions.add(d);
    heroModeBox.setItems(heroModeOptions);
    // Restore from pending state
    String heroModeDisplay = HERO_MODE_DISPLAY[0];
    for (int ki = 0; ki < HERO_MODE_KEYS.length; ki++) {
      if (HERO_MODE_KEYS[ki].equals(pendingHeroAssignMode)) { heroModeDisplay = HERO_MODE_DISPLAY[ki]; break; }
    }
    heroModeBox.setSelected(heroModeDisplay);
    heroModeBox.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
      @Override
      public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
        String sel = heroModeBox.getSelected();
        for (int ki = 0; ki < HERO_MODE_DISPLAY.length; ki++) {
          if (HERO_MODE_DISPLAY[ki].equals(sel)) { pendingHeroAssignMode = HERO_MODE_KEYS[ki]; return; }
        }
        pendingHeroAssignMode = "value_mapping";
      }
    });

    // ── Checkboxes ───────────────────────────────────────────────────────────
    final CheckBox manualSetupCheckbox = new CheckBox(" " + t("menu.setup.manualSetup"), MyGdxGame.skin);
    manualSetupCheckbox.setChecked(pendingManualSetup);

    final CheckBox heroCheckbox = new CheckBox(" " + t("menu.setup.allowStartingHero"), MyGdxGame.skin);
    heroCheckbox.setChecked(sessionAllowHeroSelection);

    // ── Create button ────────────────────────────────────────────────────────
    final TextButton confirmCreateBtn = new TextButton(t("common.create"), MyGdxGame.skin);
    confirmCreateBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        String sessionName = pendingSessionName.isEmpty()
          ? t("menu.setup.defaultSessionName", menuState.getMyName()) : pendingSessionName;
        sessionAllowHeroSelection = heroCheckbox.isChecked();
        pendingManualSetup = manualSetupCheckbox.isChecked();
        try {
          pendingStartingCards = Integer.parseInt(cardsBox.getSelected());
        } catch (NumberFormatException ex) { pendingStartingCards = 8; }
        // Read hero assign mode from selector
        String selHeroMode = heroModeBox.getSelected();
        for (int ki = 0; ki < HERO_MODE_DISPLAY.length; ki++) {
          if (HERO_MODE_DISPLAY[ki].equals(selHeroMode)) { pendingHeroAssignMode = HERO_MODE_KEYS[ki]; break; }
        }
        JSONObject data = new JSONObject();
        try {
          data.put("name", menuState.getMyName());
          data.put("sessionName", sessionName);
          data.put("allowHeroSelection", sessionAllowHeroSelection);
          data.put("startingCards", pendingStartingCards);
          data.put("manualSetup", pendingManualSetup);
          data.put("heroAssignMode", pendingHeroAssignMode);
          data.put("token", MyGdxGame.playerStorage.getToken());
          data.put("icon", selectedIcon);
        } catch (JSONException e) { /* ignore */ }
        socket.emit("createSession", data);
        pendingSessionName = "";
        pendingManualSetup = false;
        pendingStartingCards = 8;
        pendingHeroAssignMode = "value_mapping";
        inSessionCreate = false;
      }
    });

    // ── Table layout (no overlap guaranteed) ────────────────────────────────
    Table form = new Table(MyGdxGame.skin);
    form.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.35f)));
    form.pad(10f, 20f, 10f, 20f);
    float colW = MyGdxGame.WIDTH * 0.72f;

    form.add(title).colspan(2).center().padBottom(10f);
    form.row();
    form.add(gameNameBtn).colspan(2).fillX().padBottom(8f);
    form.row();
    form.add(cardsLabel).left().padRight(12f).padBottom(6f);
    form.add(cardsBox).width(colW * 0.38f).left().padBottom(6f);
    form.row();
    form.add(heroModeLabel).left().padRight(12f).padBottom(6f);
    form.add(heroModeBox).width(colW * 0.55f).left().padBottom(6f);
    form.row();
    form.add(manualSetupCheckbox).colspan(2).left().padBottom(4f);
    form.row();
    form.add(heroCheckbox).colspan(2).left().padBottom(8f);
    form.row();
    form.add(confirmCreateBtn).colspan(2).center();

    form.pack();
    // Position form from just below the back button; clamp bottom away from logout button
    float formTop = backBtn.getY() - 6f;
    float formY = Math.max(60f, formTop - form.getHeight());
    form.setPosition(
        Math.round(cx - form.getWidth() / 2f),
        Math.round(formY));
    menuStage.addActor(form);

    addTopRightButtons(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  private JSONObject buildJoinData(String sessionId) {
    JSONObject data = new JSONObject();
    try {
      data.put("sessionId", sessionId);
      data.put("name", menuState.getMyName());
      data.put("token", MyGdxGame.playerStorage.getToken());
      data.put("icon", selectedIcon);
    } catch (JSONException e) { /* ignore */ }
    return data;
  }

  /** Logs the player out: clears saved name, leaves any session, returns to auth-choice. */
  private void logout() {
    if (lobbyJoined) {
      socket.emit("leaveSession", "");
    }
    MyGdxGame.playerStorage.clearName();
    MyGdxGame.playerStorage.clearSessionId();
    MyGdxGame.playerStorage.clearIcon();
    MyGdxGame.playerStorage.clearUsername();
    selectedIcon = "";
    menuState.setMyName("");
    nameConfirmed = false;
    authChoiceShown = false;
    authErrorMessage = "";
    authPending = false;
    inRegisterScreen = false;
    inLoginScreen = false;
    lobbyJoined = false;
    timerStarted = false;
    gameRunning = false;
    inSessionCreate = false;
    reconnecting = false;
    pendingSessionName = "";
    menuState.clearUsers();
    reservedByOthers.clear();
    show();
  }

  /** Called from the DOM account-button dropdown on the web platform. */
  public void triggerLogout() {
    Gdx.app.postRunnable(new Runnable() {
      @Override public void run() { logout(); }
    });
  }

  /** Adds a small "Log out" button to the bottom-right of the given stage. */
  private void addLogoutButton(final Stage stage) {
    TextButton logoutBtn = new TextButton(t("menu.logOut"), MyGdxGame.skin);
    logoutBtn.pack();
    logoutBtn.setSize(logoutBtn.getPrefWidth() + 10, logoutBtn.getPrefHeight() + 6);
    logoutBtn.setPosition(MyGdxGame.WIDTH - logoutBtn.getWidth(), 0);
    logoutBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        logout();
      }
    });
    stage.addActor(logoutBtn);
  }

  /**
   * Adds the account/avatar button (top-right corner) plus the music toggle button
   * (shifted left to make room). The account button opens a small dropdown that
   * contains at least a Logout option. Call this instead of addMusicToggleButton +
   * addLogoutButton on screens where the user is logged in.
   */
  private void addTopRightButtons(final Stage stage) {
    // On web the DOM overlay handles the account button; skip LibGDX drawing.
    if (MyGdxGame.nativeMusicButton) return;
    accountBtnAdded = true;
    final float btnSize = ACCOUNT_BTN_SIZE;
    final float rMargin = 10f;
    final float gap     = 6f;
    final float topY    = MyGdxGame.HEIGHT - btnSize - 10f;

    // ── Account / avatar button ──────────────────────────────────────────────
    final Table accountBtn = new Table();
    accountBtn.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0.85f, 0.85f, 0.85f, 0.30f)));
    accountBtn.pad(2f);
    Texture avTex = (selectedIcon == null || selectedIcon.isEmpty()) ? null : getAvatarTexture(selectedIcon);
    if (avTex != null) {
      Image avImg = new Image(avTex);
      accountBtn.add(avImg).size(btnSize - 4f, btnSize - 4f);
    } else {
      // Fallback: first letter of the player name
      String initial = menuState.getMyName().isEmpty() ? "?" : String.valueOf(menuState.getMyName().charAt(0)).toUpperCase();
      Label initLbl = new Label(initial, MyGdxGame.skin);
      accountBtn.add(initLbl).size(btnSize - 4f, btnSize - 4f);
    }
    accountBtn.setSize(btnSize, btnSize);
    accountBtn.setPosition(MyGdxGame.WIDTH - btnSize - rMargin, topY);

    // ── Dropdown ─────────────────────────────────────────────────────────────
    final Table dropdown = new Table(MyGdxGame.skin);
    dropdown.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0.12f, 0.12f, 0.12f, 0.96f)));
    dropdown.pad(4f);
    TextButton logoutItem = new TextButton(t("menu.logOut"), MyGdxGame.skin);
    logoutItem.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        dropdown.setVisible(false);
        logout();
      }
    });
    dropdown.add(logoutItem).fillX().pad(2f);
    dropdown.pack();
    dropdown.setPosition(MyGdxGame.WIDTH - dropdown.getWidth() - rMargin, topY - dropdown.getHeight() - 4f);
    dropdown.setVisible(false);

    accountBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        dropdown.setVisible(!dropdown.isVisible());
      }
    });

    // Close dropdown when tapping outside it
    stage.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (!dropdown.isVisible()) return false;
        boolean inDrop = x >= dropdown.getX() && x <= dropdown.getX() + dropdown.getWidth()
                      && y >= dropdown.getY() && y <= dropdown.getY() + dropdown.getHeight();
        boolean inBtn  = x >= accountBtn.getX() && x <= accountBtn.getX() + accountBtn.getWidth()
                      && y >= accountBtn.getY() && y <= accountBtn.getY() + accountBtn.getHeight();
        if (!inDrop && !inBtn) dropdown.setVisible(false);
        return false;
      }
    });

    stage.addActor(accountBtn);
    stage.addActor(dropdown);

    // ── Music toggle (shifted left to make room for the account button) ───────
    if (!MyGdxGame.nativeMusicButton) {
      final boolean enabled = MyGdxGame.playerStorage.getMusicEnabled();
      final TextButton musicBtn = new TextButton(enabled ? t("menu.musicOn") : t("menu.musicOff"), MyGdxGame.skin);
      musicBtn.pack();
      float musicW = musicBtn.getPrefWidth() + 20f;
      float musicH = musicBtn.getPrefHeight() + 10f;
      musicBtn.setSize(musicW, musicH);
      musicBtn.setPosition(MyGdxGame.WIDTH - btnSize - rMargin - gap - musicW,
                           MyGdxGame.HEIGHT - musicH - 10f);
      musicBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          MyGdxGame.setMusicEnabled(!MyGdxGame.playerStorage.getMusicEnabled());
          show();
        }
      });
      stage.addActor(musicBtn);

      if (!MyGdxGame.musicStarted) {
        stage.addCaptureListener(new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (!isChildOf(event.getTarget(), musicBtn)) {
              MyGdxGame.ensureMusicStarted();
            }
            stage.removeCaptureListener(this);
            return false;
          }
        });
      }
    }
  }

  /**
   * Adds a small music on/off toggle button to the top-right corner of the given stage.
   * Also installs a one-shot capture listener so the first touch anywhere starts music
   * (works around browser autoplay restrictions).
   */
  private void addMusicToggleButton(final Stage stage) {
    // On the web platform the HTML/GWT layer injects an animated GIF button instead.
    if (MyGdxGame.nativeMusicButton) return;
    final boolean enabled = MyGdxGame.playerStorage.getMusicEnabled();
    final TextButton musicBtn = new TextButton(enabled ? t("menu.musicOn") : t("menu.musicOff"), MyGdxGame.skin);
    musicBtn.pack();
    musicBtn.setSize(musicBtn.getPrefWidth() + 20, musicBtn.getPrefHeight() + 10);
    musicBtn.setPosition(MyGdxGame.WIDTH - musicBtn.getWidth() - 10,
        MyGdxGame.HEIGHT - musicBtn.getHeight() - 10);
    musicBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        MyGdxGame.setMusicEnabled(!MyGdxGame.playerStorage.getMusicEnabled());
        show();
      }
    });
    stage.addActor(musicBtn);

    // On the first touch on any actor OTHER than the music button, start playback.
    // Skipping the music button avoids the race where touchDown starts the track
    // and then clicked() immediately sees isPlaying=true and toggles it off.
    // The AudioContext DOM unlocker in HtmlLauncher ensures play() works from rAF.
    if (!MyGdxGame.musicStarted) {
      stage.addCaptureListener(new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
          if (!isChildOf(event.getTarget(), musicBtn)) {
            MyGdxGame.ensureMusicStarted();
          }
          stage.removeCaptureListener(this);
          return false;
        }
      });
    }
  }

  private static boolean isChildOf(com.badlogic.gdx.scenes.scene2d.Actor actor,
      com.badlogic.gdx.scenes.scene2d.Actor parent) {
    com.badlogic.gdx.scenes.scene2d.Actor a = actor;
    while (a != null) {
      if (a == parent) return true;
      a = a.getParent();
    }
    return false;
  }

  private Table createStatusBadge(String text, Color bgColor, Color textColor) {
    Table badge = new Table();
    badge.setBackground(MyGdxGame.skin.newDrawable("white", bgColor));
    Label label = new Label(text, MyGdxGame.skin);
    label.setColor(textColor);
    badge.add(label).pad(2f, 8f, 2f, 8f);
    return badge;
  }

  private void showLobbyScreen() {
    MyGdxGame.setMusicTrack(timerStarted ? MyGdxGame.musicIntrigue : MyGdxGame.musicDrums);
    float cx = MyGdxGame.WIDTH / 2f;
    float buttonY = 0.16f * MyGdxGame.HEIGHT;

    Image actionBar = new Image(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.12f)));
    actionBar.setSize(0.86f * MyGdxGame.WIDTH, button.getPrefHeight() + 24f);
    actionBar.setPosition(cx - actionBar.getWidth() / 2f, buttonY - 10f);
    actionBar.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

    Label lobbyTitle = new Label(t("menu.lobbyTitle"), MyGdxGame.skin);
    float lobbyTitleScale = 1.35f;
    lobbyTitle.setFontScale(lobbyTitleScale);
    lobbyTitle.setColor(1f, 1f, 1f, 0.98f);
    lobbyTitle.setPosition(Math.round(cx - lobbyTitle.getPrefWidth() / 2f), Math.round(0.835f * MyGdxGame.HEIGHT));

    // table showing all 4 lobby slots
    Table loggedInUserTable = new Table(MyGdxGame.skin);
    loggedInUserTable.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.35f)));
    loggedInUserTable.pad(14f, 18f, 14f, 18f);
    ArrayList<User> loggedInUsers = menuState.getUsers();

    boolean isHost = isSessionHost;
    boolean hostIsSpectator = isHost && "bot".equals(lobbySlotTypes != null && lobbySlotTypes.length > 0 ? lobbySlotTypes[0] : "player");
    int tableColumns = sessionAllowHeroSelection ? 3 : 2;

    Label headLine1 = new Label(t("common.name"), MyGdxGame.skin);
    Label headLine2 = new Label(t("menu.slot"), MyGdxGame.skin);
    headLine1.setColor(1f, 1f, 1f, 0.9f);
    headLine2.setColor(1f, 1f, 1f, 0.9f);

    loggedInUserTable.add(headLine1).padRight(20).padBottom(8f);
    if (sessionAllowHeroSelection) {
      Label headHero = new Label(t("common.hero"), MyGdxGame.skin);
      headHero.setColor(1f, 1f, 1f, 0.9f);
      loggedInUserTable.add(headHero).padRight(20).padBottom(8f);
    }
    loggedInUserTable.add(headLine2).padBottom(8f);
    loggedInUserTable.row();

    final String[] SLOT_DISPLAY = {
      t("menu.slot.open"),
      t("menu.slot.closed"),
      t("menu.bot.passive"),
      t("menu.bot.balanced"),
      t("menu.bot.aggressive"),
      t("menu.bot.tactician"),
      t("menu.bot.mcts")
    };
    final String[] SLOT_TYPES   = {"open",  "closed",  "bot",         "bot",          "bot",            "bot",           "bot"};
    final String[] SLOT_MODES   = {"",      "",         "passive",     "balanced",     "aggressive",     "tactician",     "mcts"};
    // Slot-0 options: player-self + bots (cannot be open/closed)
    final String[] SLOT0_DISPLAY = {
      t("menu.slot.playerYou"),
      t("menu.bot.passive"),
      t("menu.bot.balanced"),
      t("menu.bot.aggressive"),
      t("menu.bot.tactician"),
      t("menu.bot.mcts")
    };
    final String[] SLOT0_TYPES   = {"player",       "bot",         "bot",          "bot",            "bot",           "bot"};
    final String[] SLOT0_MODES   = {"",             "passive",     "balanced",     "aggressive",     "tactician",     "mcts"};

    for (int slotIdx = 0; slotIdx < 4; slotIdx++) {
      final int si = slotIdx;
      String slotType = (lobbySlotTypes != null && lobbySlotTypes.length > slotIdx) ? lobbySlotTypes[slotIdx] : "open";
      String slotUserId = (lobbySlotUserIds != null && lobbySlotUserIds.length > slotIdx) ? lobbySlotUserIds[slotIdx] : "";
      String slotBotUserId = (lobbySlotBotUserIds != null && lobbySlotBotUserIds.length > slotIdx) ? lobbySlotBotUserIds[slotIdx] : "";
      String slotBotMode = (lobbySlotBotModes != null && lobbySlotBotModes.length > slotIdx) ? lobbySlotBotModes[slotIdx] : "";

      // Find the matching user in menuState
      User rowUser = null;
      if ("player".equals(slotType) && !slotUserId.isEmpty()) {
        for (int ui = 0; ui < loggedInUsers.size(); ui++) {
          if (loggedInUsers.get(ui).getUserID().equals(slotUserId)) { rowUser = loggedInUsers.get(ui); break; }
        }
      } else if ("bot".equals(slotType) && !slotBotUserId.isEmpty()) {
        for (int ui = 0; ui < loggedInUsers.size(); ui++) {
          if (loggedInUsers.get(ui).getUserID().equals(slotBotUserId)) { rowUser = loggedInUsers.get(ui); break; }
        }
      }

      // --- Determine whether to show a config dropdown for this slot ---
      // Host can reconfigure: slot 0 (always) and slots 1-3 when not occupied by another human.
      boolean slotOccupiedByOtherHuman = "player".equals(slotType)
          && !slotUserId.isEmpty()
          && !slotUserId.equals(menuState.getMyUserID());
      boolean showHostDropdown = isHost && !slotOccupiedByOtherHuman;

      if (si == 0 && showHostDropdown) {
        // Slot 0: always a row (host playing or bot), with dropdown for host
        // Name / hero columns
        if ("player".equals(slotType) && rowUser != null) {
          Label nameLabel = new Label(rowUser.getName(), MyGdxGame.skin);
          nameLabel.setColor(Color.GOLD);
          loggedInUserTable.add(buildNameCell(nameLabel, rowUser.getIcon())).padRight(10).padBottom(6f).left();
          if (sessionAllowHeroSelection) {
            refreshHeroDropdown();
            loggedInUserTable.add(heroSelectBox).padRight(10).padBottom(6f)
                .width(heroSelectBox.getWidth()).height(heroSelectBox.getHeight());
          }
        } else if ("bot".equals(slotType) && rowUser != null) {
          Label nameLabel = new Label(rowUser.getName(), MyGdxGame.skin);
          nameLabel.setColor(0.6f, 0.9f, 1f, 1f);
          loggedInUserTable.add(buildNameCell(nameLabel, rowUser.getIcon())).padRight(10).padBottom(6f).left();
          if (sessionAllowHeroSelection) {
            final String botUserId0 = rowUser.getUserID();
            final String botCurrentHero0 = rowUser.getSelectedHero();
            final SelectBox<String> botHeroBox0 = new SelectBox<String>(MyGdxGame.skin);
            botHeroBox0.setItems(buildHeroDropdownItems(botCurrentHero0));
            botHeroBox0.setSelected(heroOptionLabel(botCurrentHero0));
            botHeroBox0.setSize(100f, heroSelectBox.getHeight());
            botHeroBox0.addListener(new ChangeListener() {
              @Override
              public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                String selected = canonicalHeroFromOption(botHeroBox0.getSelected());
                try {
                  JSONObject data = new JSONObject();
                  data.put("botUserId", botUserId0);
                  data.put("heroName", selected);
                  socket.emit("setBotHeroSelection", data);
                } catch (JSONException e) { /* ignore */ }
              }
            });
            loggedInUserTable.add(botHeroBox0).padRight(10).padBottom(6f)
                .width(100f).height(heroSelectBox.getHeight());
          }
        } else {
          // No user found yet — show placeholder in name (and hero) columns
          Label placeholder = new Label(t("menu.slot.playerYou"), MyGdxGame.skin);
          placeholder.setColor(Color.GOLD);
          loggedInUserTable.add(placeholder).padRight(10).padBottom(6f).left();
          if (sessionAllowHeroSelection) {
            Label heroPlaceholder = new Label("-", MyGdxGame.skin);
            loggedInUserTable.add(heroPlaceholder).padRight(10).padBottom(6f);
          }
        }
        // Status column — dropdown to configure slot 0
        final SelectBox<String> slot0Box = new SelectBox<String>(MyGdxGame.skin);
        Array<String> slot0Opts = new Array<String>();
        for (String d : SLOT0_DISPLAY) slot0Opts.add(d);
        slot0Box.setItems(slot0Opts);
        // Pre-select current state
        if ("bot".equals(slotType)) {
          String modeDisplay = t("menu.bot.balanced");
          for (int ki = 1; ki < SLOT0_MODES.length; ki++) {
            if (SLOT0_MODES[ki].equals(slotBotMode)) { modeDisplay = SLOT0_DISPLAY[ki]; break; }
          }
          slot0Box.setSelected(modeDisplay);
        } else {
          slot0Box.setSelected(t("menu.slot.playerYou"));
        }
        slot0Box.addListener(new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            String sel = slot0Box.getSelected();
            for (int ki = 0; ki < SLOT0_DISPLAY.length; ki++) {
              if (SLOT0_DISPLAY[ki].equals(sel)) {
                try {
                  JSONObject slotData = new JSONObject();
                  slotData.put("slotIndex", 0);
                  slotData.put("slotType", SLOT0_TYPES[ki]);
                  if (!SLOT0_MODES[ki].isEmpty()) slotData.put("botMode", SLOT0_MODES[ki]);
                  socket.emit("setLobbySlot", slotData);
                } catch (JSONException e) { /* ignore */ }
                return;
              }
            }
          }
        });
        loggedInUserTable.add(slot0Box).padBottom(6f).width(120f).left();

      } else if ("player".equals(slotType) && rowUser != null) {
        // Human player row (non-host-configurable)
        Label nameLabel = new Label(rowUser.getName(), MyGdxGame.skin);
        if (rowUser.getUserID().equals(menuState.getMyUserID())) nameLabel.setColor(Color.GOLD);
        loggedInUserTable.add(buildNameCell(nameLabel, rowUser.getIcon())).padRight(10).padBottom(6f).left();
        if (sessionAllowHeroSelection) {
          boolean isOwnRow = rowUser.getUserID().equals(menuState.getMyUserID());
          if (isOwnRow) {
            refreshHeroDropdown();
            loggedInUserTable.add(heroSelectBox).padRight(10).padBottom(6f)
                .width(heroSelectBox.getWidth()).height(heroSelectBox.getHeight());
          } else {
            String heroName = rowUser.getSelectedHero();
            Label heroLbl = new Label(heroLabel(heroName), MyGdxGame.skin);
            loggedInUserTable.add(heroLbl).padRight(10).padBottom(6f);
          }
        }
        Table statusBadge = rowUser.isReady()
            ? createStatusBadge(t("menu.status.ready"), new Color(0.14f, 0.56f, 0.24f, 1f), Color.WHITE)
            : createStatusBadge(t("menu.status.wait"), new Color(0.64f, 0.14f, 0.14f, 1f), new Color(1f, 0.94f, 0.94f, 1f));
        loggedInUserTable.add(statusBadge).left().padBottom(6f);

      } else if ("bot".equals(slotType) && rowUser != null && !showHostDropdown) {
        // Bot row, non-host view
        Label nameLabel = new Label(rowUser.getName(), MyGdxGame.skin);
        nameLabel.setColor(0.6f, 0.9f, 1f, 1f);
        loggedInUserTable.add(buildNameCell(nameLabel, rowUser.getIcon())).padRight(10).padBottom(6f).left();
        if (sessionAllowHeroSelection) {
          String heroName = rowUser.getSelectedHero();
          Label heroLbl = new Label(heroLabel(heroName), MyGdxGame.skin);
          loggedInUserTable.add(heroLbl).padRight(10).padBottom(6f);
        }
        loggedInUserTable.add(createStatusBadge(t("menu.status.ready"), new Color(0.14f, 0.56f, 0.24f, 1f), Color.WHITE)).left().padBottom(6f);

      } else if (showHostDropdown && si > 0) {
        // Slots 1-3 that the host can configure (open, closed, or bot)
        // Build pre-select string
        String currentDisplay = t("menu.slot.open");
        if ("closed".equals(slotType)) {
          currentDisplay = t("menu.slot.closed");
        } else if ("bot".equals(slotType)) {
          // Find bot name row for display if available, but pre-select bot mode in dropdown
          currentDisplay = t("menu.bot.balanced");
          for (int ki = 2; ki < SLOT_MODES.length; ki++) {
            if (SLOT_MODES[ki].equals(slotBotMode)) { currentDisplay = SLOT_DISPLAY[ki]; break; }
          }
          // Show bot name / hero in name+hero columns before the dropdown col
          if (rowUser != null) {
            Label nameLabel = new Label(rowUser.getName(), MyGdxGame.skin);
            nameLabel.setColor(0.6f, 0.9f, 1f, 1f);
            loggedInUserTable.add(buildNameCell(nameLabel, rowUser.getIcon())).padRight(10).padBottom(6f).left();
            if (sessionAllowHeroSelection) {
              final String botUserIdN = rowUser.getUserID();
              final String botCurrentHeroN = rowUser.getSelectedHero();
              final SelectBox<String> botHeroBoxN = new SelectBox<String>(MyGdxGame.skin);
              botHeroBoxN.setItems(buildHeroDropdownItems(botCurrentHeroN));
              botHeroBoxN.setSelected(heroOptionLabel(botCurrentHeroN));
              botHeroBoxN.setSize(100f, heroSelectBox.getHeight());
              botHeroBoxN.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                  String selected = canonicalHeroFromOption(botHeroBoxN.getSelected());
                  try {
                    JSONObject data = new JSONObject();
                    data.put("botUserId", botUserIdN);
                    data.put("heroName", selected);
                    socket.emit("setBotHeroSelection", data);
                  } catch (JSONException e) { /* ignore */ }
                }
              });
              loggedInUserTable.add(botHeroBoxN).padRight(10).padBottom(6f)
                  .width(100f).height(heroSelectBox.getHeight());
            }
            // Show config dropdown in status column only
            final String fCurrentDisplay = currentDisplay;
            final SelectBox<String> slotBox = new SelectBox<String>(MyGdxGame.skin);
            Array<String> slotOpts = new Array<String>();
            for (String d : SLOT_DISPLAY) slotOpts.add(d);
            slotBox.setItems(slotOpts);
            slotBox.setSelected(fCurrentDisplay);
            slotBox.addListener(new ChangeListener() {
              @Override
              public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                String sel = slotBox.getSelected();
                for (int ki = 0; ki < SLOT_DISPLAY.length; ki++) {
                  if (SLOT_DISPLAY[ki].equals(sel)) {
                    try {
                      JSONObject slotData = new JSONObject();
                      slotData.put("slotIndex", si);
                      slotData.put("slotType", SLOT_TYPES[ki]);
                      if (!SLOT_MODES[ki].isEmpty()) slotData.put("botMode", SLOT_MODES[ki]);
                      socket.emit("setLobbySlot", slotData);
                    } catch (JSONException e) { /* ignore */ }
                    return;
                  }
                }
              }
            });
            loggedInUserTable.add(slotBox).padBottom(6f).width(120f).left();
            loggedInUserTable.row();
            if (si < 3) {
              Image sep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.14f)));
              loggedInUserTable.add(sep).colspan(tableColumns).growX().height(1f).padTop(2f).padBottom(5f);
              loggedInUserTable.row();
            }
            continue; // row already closed above
          }
        }
        // open or closed slot (or bot with no rowUser): span all columns with the dropdown
        final String fCurrentDisplay = currentDisplay;
        final SelectBox<String> slotBox = new SelectBox<String>(MyGdxGame.skin);
        Array<String> slotOpts = new Array<String>();
        for (String d : SLOT_DISPLAY) slotOpts.add(d);
        slotBox.setItems(slotOpts);
        slotBox.setSelected(fCurrentDisplay);
        slotBox.addListener(new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            String sel = slotBox.getSelected();
            for (int ki = 0; ki < SLOT_DISPLAY.length; ki++) {
              if (SLOT_DISPLAY[ki].equals(sel)) {
                try {
                  JSONObject slotData = new JSONObject();
                  slotData.put("slotIndex", si);
                  slotData.put("slotType", SLOT_TYPES[ki]);
                  if (!SLOT_MODES[ki].isEmpty()) slotData.put("botMode", SLOT_MODES[ki]);
                  socket.emit("setLobbySlot", slotData);
                } catch (JSONException e) { /* ignore */ }
                return;
              }
            }
          }
        });
        loggedInUserTable.add(slotBox).colspan(tableColumns).fillX().padBottom(6f);

      } else {
        // Non-host view of an empty/closed slot, or slot 0 placeholder not yet claimed
        if (si == 0) {
          Label placeholder = new Label(t("menu.slot.waiting"), MyGdxGame.skin);
          placeholder.setColor(1f, 1f, 1f, 0.4f);
          loggedInUserTable.add(placeholder).colspan(tableColumns).left().padBottom(6f);
        } else {
          Label slotLabel = new Label("closed".equals(slotType) ? t("menu.slot.closedBracket") : t("menu.slot.openBracket"), MyGdxGame.skin);
          slotLabel.setColor(1f, 1f, 1f, 0.45f);
          loggedInUserTable.add(slotLabel).colspan(tableColumns).left().padBottom(6f);
        }
      }
      loggedInUserTable.row();
      if (slotIdx < 3) {
        Image sep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.14f)));
        loggedInUserTable.add(sep).colspan(tableColumns).growX().height(1f).padTop(2f).padBottom(5f);
        loggedInUserTable.row();
      }
    }

    loggedInUserTable.pack();
    loggedInUserTable.setPosition(cx - loggedInUserTable.getWidth() / 2f,
        0.47f * MyGdxGame.HEIGHT - loggedInUserTable.getHeight() / 2f);

    // Notification permission status — temporarily hidden to avoid overlap with lobby buttons
    // if (MyGdxGame.turnNotifier.isPermissionGranted()) {
    //   Label notifLabel = new Label("\uD83D\uDD14 Notifications: ON", MyGdxGame.skin);
    //   notifLabel.setColor(Color.GREEN);
    //   notifLabel.setPosition(0, 50);
    //   menuStage.addActor(notifLabel);
    // } else {
    //   TextButton notifButton = new TextButton("\uD83D\uDD14 Enable notifications", MyGdxGame.skin);
    //   notifButton.setPosition(0, 50);
    //   notifButton.addListener(new ClickListener() {
    //     @Override
    //     public void clicked(InputEvent event, float x, float y) {
    //       MyGdxGame.turnNotifier.requestPermission(new Runnable() {
    //         @Override public void run() { show(); }
    //       });
    //     }
    //   });
    //   menuStage.addActor(notifButton);
    // }

    if (gameRunning) {
      // A game is already in progress — show status and offer spectating
      Label gameRunningLabel = new Label(t("menu.gameInProgress"), MyGdxGame.skin);
      gameRunningLabel.setColor(Color.YELLOW);
      gameRunningLabel.setPosition(cx - gameRunningLabel.getWidth() / 2f, 0.11f * MyGdxGame.HEIGHT + 46f);
      menuStage.addActor(gameRunningLabel);

      TextButton watchButton = new TextButton(t("menu.watchGame"), MyGdxGame.skin);
      watchButton.setSize(button.getPrefWidth(), button.getPrefHeight());
      watchButton.setPosition((MyGdxGame.WIDTH - watchButton.getWidth()) / 2f, 0.1f * MyGdxGame.HEIGHT);
      watchButton.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          socket.emit("joinSpectator", "");
        }
      });
      menuStage.addActor(watchButton);
    }

    // Ready player count — always visible
    int readyCount = 0;
    for (int i = 0; i < loggedInUsers.size(); i++) {
      if (loggedInUsers.get(i).isReady()) readyCount++;
    }
    Label lobbyStatus = new Label(t("menu.readyPlayers", readyCount, loggedInUsers.size()), MyGdxGame.skin);
    lobbyStatus.setPosition(0.05f * MyGdxGame.WIDTH, 0.01f * MyGdxGame.HEIGHT);
    menuStage.addActor(lobbyStatus);

    if (!gameRunning) {
      boolean amReady = false;
      for (int i = 0; i < loggedInUsers.size(); i++) {
        if (loggedInUsers.get(i).getUserID().equals(menuState.getMyUserID())) {
          amReady = loggedInUsers.get(i).isReady();
          break;
        }
      }
      boolean canHostStart = isHost && readyCount >= 2 && !timerStarted
          && (hostIsSpectator || amReady);

      if (isHost) {
        TextButton startGameButton = new TextButton(t("menu.startGame"), MyGdxGame.skin);
        startGameButton.setSize(startGameButton.getPrefWidth() + 20, startGameButton.getPrefHeight());
        float buttonGap = 20f;
        float startButtonX;
        if (hostIsSpectator) {
          // No Ready button — center the Start button
          startButtonX = (MyGdxGame.WIDTH - startGameButton.getWidth()) / 2f;
        } else {
          float readyButtonX = (MyGdxGame.WIDTH / 2f) - button.getWidth() - (buttonGap / 2f);
          startButtonX = (MyGdxGame.WIDTH / 2f) + (buttonGap / 2f);
          button.setPosition(readyButtonX, buttonY);
          menuStage.addActor(button);
        }
        startGameButton.setPosition(startButtonX, buttonY);
        startGameButton.setDisabled(!canHostStart);
        startGameButton.setTouchable(!canHostStart
            ? com.badlogic.gdx.scenes.scene2d.Touchable.disabled
            : com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        if (canHostStart) {
          startGameButton.setColor(0.2f, 0.8f, 0.2f, 1f);
        } else {
          startGameButton.setColor(0.6f, 0.6f, 0.6f, 1f);
        }
        startGameButton.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            socket.emit("startGame", new JSONObject());
          }
        });
        menuStage.addActor(startGameButton);
      } else {
        button.setPosition((MyGdxGame.WIDTH - button.getWidth()) / 2f, buttonY);
      }

      if (timerStarted) {
        Label countdownLabel = new Label(t("menu.startingIn", menuState.getTimeToStart()), MyGdxGame.skin);
        countdownLabel.setColor(Color.YELLOW);
        countdownLabel.setPosition(cx - countdownLabel.getWidth() / 2f, buttonY + button.getHeight() + 14f);
        menuStage.addActor(countdownLabel);
      }

      if (!sessionAllowHeroSelection) {
        // No hero selection in this session — clear any stale hero from a previous session.
        menuState.setStartingHero("None");
      }
      // Only show the Ready button when not a spectating host
      if (!hostIsSpectator) {
        menuStage.addActor(button);
      }
    }

    menuStage.addActor(actionBar);
    menuStage.addActor(lobbyTitle);
    menuStage.addActor(loggedInUserTable);

    // Leave session — returns to session list
    TextButton leaveBtn = new TextButton(t("menu.leave"), MyGdxGame.skin);
    leaveBtn.setPosition(10, MyGdxGame.HEIGHT - leaveBtn.getHeight() - 10);
    leaveBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        socket.emit("leaveSession", "");
        MyGdxGame.playerStorage.clearSessionId();
        isSessionHost = false;
        lobbyJoined = false;
        timerStarted = false;
        gameRunning = false;
        menuState.clearUsers();
        reservedByOthers.clear();
        show();
      }
    });
    menuStage.addActor(leaveBtn);

    addTopRightButtons(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  @Override
  public void render(float delta) {
    // System.out.println("render menu screen");
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    drawFullScreenTexture(menuBgTexture);

    // Auto-escape from stuck reconnect state after timeout.
    if (reconnecting) {
      reconnectElapsed += delta;
      if (reconnectElapsed >= RECONNECT_TIMEOUT_SECONDS) {
        clearReconnectState();
        show();
      }
    }

    if (currentUsersCount != menuState.getUsers().size()) {
      currentUsersCount = menuState.getUsers().size();
      show();
    }

    if (updateScreen) {
      updateScreen = false;
      show();
    }

    menuStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    menuStage.act(delta);
    menuStage.draw();
  }

  @Override
  public void resize(int width, int height) {
    // TODO Auto-generated method stub

  }

  @Override
  public void pause() {
    // TODO Auto-generated method stub

  }

  @Override
  public void resume() {
    // TODO Auto-generated method stub

  }

  @Override
  public void hide() {
    // TODO Auto-generated method stub

  }

  @Override
  public void dispose() {
    menuStage.dispose();
    logoTexture.dispose();
    if (menuBgTexture != null) { menuBgTexture.dispose(); menuBgTexture = null; }
    if (languageEnIcon != null) { languageEnIcon.dispose(); languageEnIcon = null; }
    if (languageDeIcon != null) { languageDeIcon.dispose(); languageDeIcon = null; }
    if (languageRuIcon != null) { languageRuIcon.dispose(); languageRuIcon = null; }
    if (languageNoIcon != null) { languageNoIcon.dispose(); languageNoIcon = null; }
    if (languageItIcon != null) { languageItIcon.dispose(); languageItIcon = null; }
    for (Texture t : avatarTextures.values()) { if (t != null) t.dispose(); }
    avatarTextures.clear();
  }

  public void configSocketEvents(final SocketClient socket) {
    socket.on("connect", new SocketListener() {

      @Override
      public void call(Object... args) {
        Gdx.app.log("SocketIO", "Connected");
      }
    });
    socket.on("socketID", new SocketListener() {

      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          String myUserID = data.getString("id");
          menuState.setMyUserID(myUserID);
          Gdx.app.log("SocketIO", "My ID: " + myUserID);
          // If we already have a saved name (restored from storage), register immediately.
          if (nameConfirmed && !menuState.getMyName().isEmpty()) {
            try {
              JSONObject autoReg = new JSONObject();
              autoReg.put("name", menuState.getMyName());
              autoReg.put("token", MyGdxGame.playerStorage.getToken());
              autoReg.put("icon", selectedIcon);
              socket.emit("registerPlayer", autoReg);
              // Try to rejoin the session the player was in before the refresh.
              String savedSessId = MyGdxGame.playerStorage.getSavedSessionId();
              if (!savedSessId.isEmpty()) {
                socket.emit("joinSession", buildJoinData(savedSessId));
              }
            } catch (JSONException ex) { /* ignore */ }
          }
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error getting ID");
        }
      }
    });
    socket.on("newUser", new SocketListener() {

      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          String id = data.getString("id");
          Gdx.app.log("SocketIO", "New User connected: " + id);
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error getting new user ID ");
        }
      }
    });
    socket.on("userDisconnected", new SocketListener() {

      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          String id = data.getString("id");
          Gdx.app.log("SocketIO", "User disconnected: " + id);
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error disconnecting user ID ");
        }
      }
    });
    socket.on("getUsers", new SocketListener() {

      @Override
      public void call(Object... args) {
        JSONArray objects = (JSONArray) args[0];
        try {
          menuState.clearUsers();
          for (int i = 0; i < objects.length(); i++) {
            String userID = objects.getJSONObject(i).getString("id");
            String name   = objects.getJSONObject(i).getString("name");
            boolean isReady = objects.getJSONObject(i).getBoolean("isReady");
            String heroSel = objects.getJSONObject(i).optString("heroSelection", "None");
            String userIcon = objects.getJSONObject(i).optString("icon", "");
            User user = new User(userID, name);
            user.setReady(isReady);
            user.setSelectedHero(heroSel);
            user.setIcon(userIcon);
            menuState.addUser(user);
            Gdx.app.log("SocketIO", "Get users " + name + " (" + userID + ") ready=" + isReady + " hero=" + heroSel);
          }
          // Rebuild reservedByOthers from received hero selections
          reservedByOthers.clear();
          for (int j = 0; j < menuState.getUsers().size(); j++) {
            User u = menuState.getUsers().get(j);
            if (!u.getUserID().equals(menuState.getMyUserID())) {
              String h = u.getSelectedHero();
              if (!h.equals("None") && !h.equals("Random")) reservedByOthers.add(h);
            }
          }
          updateScreen = true;
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error parsing getUsers");
        }
        Gdx.app.log("SocketIO", "Number of users = " + menuState.getUsers().size());
      }
    });
    socket.on("userReady", new SocketListener() {

      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          String id = data.getString("id");
          ArrayList<User> users = menuState.getUsers();
          for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserID().equals(id)) {
              users.get(i).setReady(true);
              System.out.println(users.get(i).isReady());
            }
          }
          Gdx.app.log("SocketIO", "User ready: " + id);
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error ready user ID ");
        }
      }
    });
    socket.on("gameState", new SocketListener() {
      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          final int playerIndex = data.getInt("playerIndex");
          final JSONObject gameState = data.getJSONObject("gameState");
          final String heroAssignMode = data.optString("heroAssignMode", "value_mapping");
          Gdx.app.log("SocketIO", "Received centralized game state, playerIndex: " + playerIndex);
          Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
              // Guard: only create a GameScreen from MenuScreen when the current screen is
              // NOT already a GameScreen. On reconnect the server sends a gameState event,
              // but the active GameScreen has its own handler that handles the transition.
              // Without this guard, both MenuScreen and GameScreen would create a new
              // GameScreen simultaneously, stripping the new screen's stateUpdate listener.
              if (!(game.getScreen() instanceof GameScreen)) {
                game.setScreen(new GameScreen(game, gameState, playerIndex, socket, menuState.getStartingHero(), heroAssignMode));
              }
            }
          });
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error parsing centralized game state: " + e.getMessage());
        }
      }
    });
    socket.on("updateTimer", new SocketListener() {

      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          int timeToStart = data.getInt("seconds");
          menuState.setTimeToStart(timeToStart);
          timerStarted = timeToStart > 0;
          Gdx.app.log("SocketIO", "Seconds to start game: " + timeToStart);
          show();
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error in timer!");
        }
      }
    });

    socket.on("startCountdownCanceled", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            timerStarted = false;
            menuState.setTimeToStart(5);
            updateScreen = true;
          }
        });
      }
    });

    socket.on("heroReserved", new SocketListener() {
      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          final String heroName = data.getString("heroName");
          Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
              reservedByOthers.add(heroName);
              updateScreen = true;
            }
          });
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error parsing heroReserved");
        }
      }
    });

    socket.on("heroReleased", new SocketListener() {
      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          final String heroName = data.getString("heroName");
          Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
              reservedByOthers.remove(heroName);
              updateScreen = true;
            }
          });
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error parsing heroReleased");
        }
      }
    });

    socket.on("gameStatus", new SocketListener() {
      @Override
      public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        try {
          final boolean running = data.getBoolean("running");
          Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
              gameRunning = running;
              updateScreen = true;
            }
          });
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error parsing gameStatus");
        }
      }
    });

    socket.on("gameAlreadyRunning", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            timerStarted = false;
            gameRunning = true;
            updateScreen = true;
          }
        });
      }
    });

    socket.on("notEnoughReadyPlayers", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            updateScreen = true;
          }
        });
      }
    });

    socket.on("leftSessionNotReady", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            MyGdxGame.playerStorage.clearSessionId();
            reconnecting = false;
            reconnectElapsed = 0f;
            isSessionHost = false;
            lobbyJoined = false;
            timerStarted = false;
            gameRunning = false;
            showPlayersTab = false;
            menuState.clearUsers();
            reservedByOthers.clear();
            show();
          }
        });
      }
    });

    socket.on("returnToLobby", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            MyGdxGame.playerStorage.clearSessionId();
            reconnecting = false;
            reconnectElapsed = 0f;
            timerStarted = false;
            gameRunning = false;
            isSessionHost = false;
            lobbyJoined = false;
            showPlayersTab = false;
            menuState.clearUsers();
            reservedByOthers.clear();
            game.setScreen(MenuScreen.this);
          }
        });
      }
    });

    socket.on("sessionList", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONArray arr = (JSONArray) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            sessionList.clear();
            try {
              for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                sessionList.add(new SessionInfo(
                  o.getString("id"),
                  o.getString("name"),
                  o.getInt("playerCount"),
                  o.optInt("maxSlots", 4),
                  o.getBoolean("running")
                ));
              }
            } catch (JSONException e) {
              Gdx.app.log("SocketIO", "Error parsing sessionList");
            }
            if (!lobbyJoined) updateScreen = true;
          }
        });
      }
    });

    socket.on("playerList", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONArray arr = (JSONArray) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            onlinePlayers.clear();
            try {
              for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                onlinePlayers.add(new OnlinePlayerInfo(
                  p.getString("id"),
                  p.getString("name"),
                  p.getString("status"),
                  p.optString("icon", "")
                ));
              }
            } catch (JSONException e) {
              Gdx.app.log("SocketIO", "Error parsing playerList");
            }
            if (!lobbyJoined) updateScreen = true;
          }
        });
      }
    });

    socket.on("sessionJoined", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              sessionAllowHeroSelection = data.optBoolean("allowHeroSelection", true);
              String sessId = data.optString("sessionId", "");
              if (!sessId.isEmpty()) MyGdxGame.playerStorage.saveSessionId(sessId);
              if (data.optBoolean("isHost", false)) isSessionHost = true;
            } catch (Exception e) { /* keep default */ }
            // Clear the reconnecting spinner if we were reconnecting to a lobby
            // (not a running game — those go via gameState, not sessionJoined).
            reconnecting = false;
            reconnectElapsed = 0f;
            lobbyJoined = true;
            // Notify server of our initial hero selection only when hero selection is enabled.
            if (sessionAllowHeroSelection) {
              socket.emit("heroSelected", menuState.getStartingHero());
            }
            updateScreen = true;
          }
        });
      }
    });

    socket.on("sessionClosed", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            MyGdxGame.playerStorage.clearSessionId();
            reconnecting = false;
            reconnectElapsed = 0f;
            isSessionHost = false;
            lobbyJoined = false;
            timerStarted = false;
            gameRunning = false;
            showPlayersTab = false;
            menuState.clearUsers();
            reservedByOthers.clear();
            show();
          }
        });
      }
    });

    socket.on("sessionNotFound", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            // The session we tried to rejoin no longer exists — clear the stale id and
            // drop back to the lobby so the player can start or join a fresh game.
            MyGdxGame.playerStorage.clearSessionId();
            reconnecting = false;
            reconnectElapsed = 0f;
            updateScreen = true;
          }
        });
      }
    });

    socket.on("duplicateTab", new SocketListener() {
      @Override
      public void call(Object... args) {
        // Disconnect first so socket.io does not auto-reconnect and ping-pong with the new tab.
        socket.disconnect();
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            // Set the flag before setScreen so that show() renders the right message.
            // This works whether the old tab is on MenuScreen or GameScreen.
            disconnectedByDuplicateTab = true;
            game.setScreen(MenuScreen.this);
          }
        });
      }
    });

    // Connect only after all listeners are registered so no events are missed
    socket.on("lobbySlots", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONArray slots = (JSONArray) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              for (int i = 0; i < Math.min(4, slots.length()); i++) {
                JSONObject s = slots.getJSONObject(i);
                String type = s.optString("type", "open");
                lobbySlotTypes[i]      = type;
                lobbySlotUserIds[i]    = s.optString("userId", "");
                lobbySlotBotUserIds[i] = s.optString("botUserId", "");
                lobbySlotBotModes[i]   = s.optString("botMode", "");
              }
            } catch (JSONException e) {
              Gdx.app.log("SocketIO", "Error parsing lobbySlots: " + e.getMessage());
            }
            updateScreen = true;
          }
        });
      }
    });

    socket.on("sessionFull", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            updateScreen = true;
          }
        });
      }
    });

    socket.on("registerResult", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject result = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            authPending = false;
            try {
              boolean success = result.optBoolean("success", false);
              if (success) {
                String username = result.optString("name", "");
                String icon = result.optString("icon", "");
                MyGdxGame.playerStorage.saveUsername(username);
                MyGdxGame.playerStorage.saveName(username);
                menuState.setMyName(username);
                if (!icon.isEmpty()) {
                  selectedIcon = icon;
                  MyGdxGame.playerStorage.saveIcon(icon);
                }
                nameConfirmed = true;
                inRegisterScreen = false;
                authErrorMessage = "";
                try {
                  JSONObject reg = new JSONObject();
                  reg.put("name", username);
                  reg.put("token", MyGdxGame.playerStorage.getToken());
                  reg.put("icon", selectedIcon);
                  socket.emit("registerPlayer", reg);
                } catch (JSONException e) { /* ignore */ }
                show();
              } else {
                authErrorMessage = mapAuthError(result.optString("error", ""));
                show();
              }
            } catch (Exception e) {
              authErrorMessage = "";
              show();
            }
          }
        });
      }
    });

    socket.on("loginResult", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject result = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            authPending = false;
            try {
              boolean success = result.optBoolean("success", false);
              if (success) {
                String username = result.optString("name", "");
                String icon = result.optString("icon", "");
                MyGdxGame.playerStorage.saveUsername(username);
                MyGdxGame.playerStorage.saveName(username);
                menuState.setMyName(username);
                if (!icon.isEmpty()) {
                  selectedIcon = icon;
                  MyGdxGame.playerStorage.saveIcon(icon);
                }
                nameConfirmed = true;
                inLoginScreen = false;
                authErrorMessage = "";
                try {
                  JSONObject reg = new JSONObject();
                  reg.put("name", username);
                  reg.put("token", MyGdxGame.playerStorage.getToken());
                  reg.put("icon", selectedIcon);
                  socket.emit("registerPlayer", reg);
                } catch (JSONException e) { /* ignore */ }
                show();
              } else {
                authErrorMessage = mapAuthError(result.optString("error", ""));
                show();
              }
            } catch (Exception e) {
              authErrorMessage = "";
              show();
            }
          }
        });
      }
    });

    socket.connect();
  }

  /** Maps a server-side error code to a localized message string. */
  private String mapAuthError(String errorCode) {
    if ("username_taken".equals(errorCode)) return t("menu.auth.error.usernameTaken");
    if ("invalid_credentials".equals(errorCode)) return t("menu.auth.error.invalidCredentials");
    return errorCode.isEmpty() ? "" : errorCode;
  }

}
