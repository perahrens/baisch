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
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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

  // True while the session-creation sub-screen is shown
  private boolean inSessionCreate = false;
  // Pending game name typed on the create screen (cleared after creation)
  private String pendingSessionName = "";
  // Whether hero selection is allowed in the current session
  private boolean sessionAllowHeroSelection = false;
  // Pending create-screen settings
  private boolean pendingManualSetup = false;
  private int pendingStartingCards = 8;

  // The session list received from the server
  private java.util.List<SessionInfo> sessionList = new java.util.ArrayList<SessionInfo>();

  private static class SessionInfo {
    String id;
    String name;
    int playerCount;
    boolean running;
    SessionInfo(String id, String name, int playerCount, boolean running) {
      this.id = id; this.name = name; this.playerCount = playerCount; this.running = running;
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

  private static class OnlinePlayerInfo {
    String id;
    String name;
    String status;
    OnlinePlayerInfo(String id, String name, String status) {
      this.id = id; this.name = name; this.status = status;
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

  public MenuScreen(final Game game, final SocketClient socket) {
    super(game);

    this.socket = socket;

    menuStage = new Stage();

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
    }
    showPlayersTab = MyGdxGame.playerStorage.getSavedShowPlayersTab();
    // If the player was mid-game when they refreshed, suppress the lobby flash by
    // entering reconnecting mode.  show() will display a spinner until gameState
    // arrives (or sessionNotFound clears the flag and falls back to the lobby).
    if (nameConfirmed && !MyGdxGame.playerStorage.getSavedSessionId().isEmpty()) {
      reconnecting = true;
      reconnectElapsed = 0f;
    }

    // create menu screen
    group = new Group();
    group.setBounds(0, 0, MyGdxGame.WIDTH, MyGdxGame.HEIGHT);

    // baisch logo
    logoTexture = new Texture(Gdx.files.internal("data/graphics/Logo.png"));
    logoRegion = new TextureRegion(logoTexture, 0, 0, 394, 271);
    logoImage = new Image(logoRegion);

    button = new TextButton("Ready", MyGdxGame.skin);

    button.setSize(button.getWidth() * 2, button.getHeight() * 2);

    logoImage.setPosition((MyGdxGame.WIDTH - logoImage.getWidth()) / 2f,
        0.9f * MyGdxGame.HEIGHT - logoImage.getHeight());
    button.setPosition((MyGdxGame.WIDTH - button.getWidth()) / 2f, 0.1f * MyGdxGame.HEIGHT);

    // Starting hero selector (for testing)
    Array<String> heroNames = new Array<String>();
    heroNames.add("None");
    heroNames.add("Mercenaries");
    heroNames.add("Marshal");
    heroNames.add("Spy");
    heroNames.add("Battery Tower");
    heroNames.add("Merchant");
    heroNames.add("Priest");
    heroNames.add("Reservists");
    heroNames.add("Banneret");
    heroNames.add("Saboteurs");
    heroNames.add("Fortified Tower");
    heroNames.add("Magician");
    heroNames.add("Warlord");

    heroSelectBox = new SelectBox<String>(MyGdxGame.skin);
    heroSelectBox.setItems(heroNames);
    heroSelectBox.setSelected("None");
    heroSelectBox.setSize(button.getWidth(), button.getHeight());
    heroSelectBox.setPosition((MyGdxGame.WIDTH - heroSelectBox.getWidth()) / 2f, 0.21f * MyGdxGame.HEIGHT);
    heroSelectBox.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
        if (updatingDropdown) return;
        String selected = heroSelectBox.getSelected();
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

    group.addActor(logoImage);
    // button is NOT in the group so it only appears on the lobby screen

    menuStage.addActor(group);
    menuStage.getCamera().position.set(MyGdxGame.WIDTH / 2, MyGdxGame.HEIGHT / 2, 0);

    currentUsersCount = menuState.getUsers().size();

    Gdx.input.setInputProcessor(menuStage);

  }

  public void create() {

  }

  /**
   * Rebuild the hero dropdown items, excluding heroes that have been reserved by other lobby
   * players. The current player's own selection is preserved when possible; if their hero was
   * taken by someone else it is reset to "None".
   */
  private void refreshHeroDropdown() {
    String currentSelected = heroSelectBox.getSelected();
    // Treat null (empty SelectBox) the same as "None" so we don't wipe startingHero.
    if (currentSelected == null) currentSelected = "None";
    Array<String> items = new Array<String>();
    items.add("None");
    for (int i = 0; i < ALL_HERO_NAMES.length; i++) {
      if (!reservedByOthers.contains(ALL_HERO_NAMES[i])) {
        items.add(ALL_HERO_NAMES[i]);
      }
    }
    updatingDropdown = true;
    heroSelectBox.setItems(items);
    // Keep the previous selection if it is still available; otherwise fall back to "None".
    if (!currentSelected.equals("None") && !reservedByOthers.contains(currentSelected)) {
      heroSelectBox.setSelected(currentSelected);
    } else if (!currentSelected.equals("None")) {
      // Hero was reserved by another player — reset.
      heroSelectBox.setSelected("None");
      menuState.setStartingHero("None");
    } else {
      heroSelectBox.setSelected("None");
    }
    updatingDropdown = false;
  }

  @Override
  public void show() {
    heroSelectBox.hideList();
    menuStage.clear();

    if (disconnectedByDuplicateTab) {
      showDuplicateTabScreen();
    } else if (reconnecting) {
      showReconnectingScreen();
    } else if (!nameConfirmed) {
      // Logo only shown on the name-entry screen.
      menuStage.addActor(group);
      showNameEntryScreen();
    } else if (!lobbyJoined && inSessionCreate) {
      showSessionCreateScreen();
    } else if (!lobbyJoined) {
      showSessionListScreen();
    } else {
      showLobbyScreen();
    }
  }

  private void showDuplicateTabScreen() {
    Label msg = new Label(
        "This game was opened in another browser tab.\nThis tab is no longer active.",
        MyGdxGame.skin);
    msg.pack();
    msg.setPosition(
        MyGdxGame.WIDTH  / 2f - msg.getPrefWidth()  / 2f,
        MyGdxGame.HEIGHT / 2f - msg.getPrefHeight() / 2f);
    menuStage.addActor(msg);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showReconnectingScreen() {
    Label msg = new Label("Reconnecting...", MyGdxGame.skin);
    msg.pack();
    msg.setPosition(
        MyGdxGame.WIDTH  / 2f - msg.getPrefWidth()  / 2f,
        MyGdxGame.HEIGHT / 2f - msg.getPrefHeight() / 2f + 30f);
    menuStage.addActor(msg);

    TextButton returnBtn = new TextButton("Return to Lobby", MyGdxGame.skin);
    returnBtn.pack();
    returnBtn.setPosition(
        MyGdxGame.WIDTH  / 2f - returnBtn.getPrefWidth()  / 2f,
        MyGdxGame.HEIGHT / 2f - returnBtn.getPrefHeight() / 2f - 20f);
    returnBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        clearReconnectState();
        show();
      }
    });
    menuStage.addActor(returnBtn);

    Gdx.input.setInputProcessor(menuStage);
  }

  /** Clears the reconnect state and saved session so the lobby is shown cleanly. */
  private void clearReconnectState() {
    MyGdxGame.playerStorage.clearSessionId();
    reconnecting = false;
    reconnectElapsed = 0f;
  }

  private void showNameEntryScreen() {
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    // A button-shaped area that opens the native text dialog on click/tap.
    // getTextInput() is called synchronously from the DOM click event inside GWT,
    // so the mobile keyboard always opens.
    String label = menuState.getMyName().isEmpty() ? "Enter your name" : menuState.getMyName();
    TextButton enterNameButton = new TextButton(label, MyGdxGame.skin);
    enterNameButton.setSize(button.getWidth() * 2, button.getHeight());
    enterNameButton.setPosition(cx - enterNameButton.getWidth() / 2f, 0.3f * MyGdxGame.HEIGHT);
    enterNameButton.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
          @Override
          public void input(String text) {
            String name = text.trim();
            if (name.isEmpty()) return;
            menuState.setMyName(name);
            MyGdxGame.playerStorage.saveName(name);
            nameConfirmed = true;
            try {
              JSONObject reg = new JSONObject();
              reg.put("name", name);
              reg.put("token", MyGdxGame.playerStorage.getToken());
              socket.emit("registerPlayer", reg);
            } catch (JSONException e) { /* ignore */ }
            Gdx.app.postRunnable(new Runnable() {
              @Override public void run() { show(); }
            });
          }
          @Override
          public void canceled() { /* stay on name entry screen */ }
        }, "Baisch", menuState.getMyName(), "Enter your name");
      }
    });

    menuStage.addActor(enterNameButton);
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

    Label gamesTab   = new Label("Games",   MyGdxGame.skin);
    Label playersTab = new Label("Players", MyGdxGame.skin);
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
      sessTable.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.14f)));
      sessTable.pad(14f, 18f, 14f, 18f);

      Label h1 = new Label("Name", MyGdxGame.skin);
      Label h2 = new Label("Players", MyGdxGame.skin);
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
        Label countL = new Label(s.playerCount + "/4", MyGdxGame.skin);
        if (s.running) {
          TextButton watchBtn = new TextButton("Watch", MyGdxGame.skin);
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
          TextButton joinBtn = new TextButton("Join", MyGdxGame.skin);
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
        Label empty = new Label("No games available", MyGdxGame.skin);
        empty.setColor(0.6f, 0.6f, 0.6f, 1f);
        sessTable.add(empty).colspan(3);
        sessTable.row();
      }

      sessTable.pack();
      sessTable.setPosition(Math.round(cx - sessTable.getWidth() / 2f), Math.round(0.45f * MyGdxGame.HEIGHT));
      menuStage.addActor(sessTable);

      TextButton tutorialBtn = new TextButton("Tutorial", MyGdxGame.skin);
      tutorialBtn.setSize(button.getWidth(), button.getHeight());
      tutorialBtn.setPosition(cx - tutorialBtn.getWidth() / 2f, 0.08f * MyGdxGame.HEIGHT);
      tutorialBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          socket.emit("createTutorial", new JSONObject());
        }
      });
      menuStage.addActor(tutorialBtn);

      TextButton createBtn = new TextButton("Create game", MyGdxGame.skin);
      createBtn.setSize(button.getWidth(), button.getHeight());
      createBtn.setPosition(MyGdxGame.WIDTH - createBtn.getWidth() - 20f, 0.08f * MyGdxGame.HEIGHT);
      createBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          inSessionCreate = true;
          show();
        }
      });
      menuStage.addActor(createBtn);
    } else {
      // ── Players tab ─────────────────────────────────────────────────────────
      Table playersTable = new Table(MyGdxGame.skin);
      playersTable.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.14f)));
      playersTable.pad(14f, 18f, 14f, 18f);

      Label ph1 = new Label("Name", MyGdxGame.skin);
      Label ph2 = new Label("Status", MyGdxGame.skin);
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
        playersTable.add(nameL).padRight(40).padBottom(6f).left();
        playersTable.add(statusL).padBottom(6f);
        playersTable.row();
        if (pi < snapshot.size() - 1) {
          Image sep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.14f)));
          playersTable.add(sep).colspan(2).growX().height(1f).padTop(2f).padBottom(5f);
          playersTable.row();
        }
      }

      if (snapshot.isEmpty()) {
        Label empty = new Label("No players online", MyGdxGame.skin);
        empty.setColor(0.6f, 0.6f, 0.6f, 1f);
        playersTable.add(empty).colspan(2);
        playersTable.row();
      }

      playersTable.pack();
      playersTable.setPosition(Math.round(cx - playersTable.getWidth() / 2f), Math.round(0.45f * MyGdxGame.HEIGHT));
      menuStage.addActor(playersTable);
    }

    TextButton rulesBtn = new TextButton("Rules", MyGdxGame.skin);
    rulesBtn.setSize(button.getWidth(), button.getHeight());
    rulesBtn.setPosition(20f, 0.08f * MyGdxGame.HEIGHT);
    rulesBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        Gdx.net.openURI(RULES_URL);
      }
    });
    menuStage.addActor(rulesBtn);

    addMusicToggleButton(menuStage);
    addLogoutButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showSessionCreateScreen() {
    MyGdxGame.setMusicTrack(MyGdxGame.musicShimmer);
    float cx = MyGdxGame.WIDTH / 2f;

    // ── Back button (top-left) ───────────────────────────────────────────────
    TextButton backBtn = new TextButton("Back", MyGdxGame.skin);
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
    Label title = new Label("New game", MyGdxGame.skin);
    title.setFontScale(1.3f);
    title.pack();

    // ── Game name button ─────────────────────────────────────────────────────
    final String nameDisplay = pendingSessionName.isEmpty() ? "Set name (optional)" : pendingSessionName;
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
        }, "New game", pendingSessionName, "Enter game name (optional)");
      }
    });

    // ── Starting cards selector ──────────────────────────────────────────────
    Label cardsLabel = new Label("Starting cards:", MyGdxGame.skin);
    final SelectBox<String> cardsBox = new SelectBox<String>(MyGdxGame.skin);
    Array<String> cardOptions = new Array<String>();
    for (int n = 6; n <= 10; n++) cardOptions.add(String.valueOf(n));
    cardsBox.setItems(cardOptions);
    cardsBox.setSelected(String.valueOf(pendingStartingCards));

    // ── Bot count selector ───────────────────────────────────────────────────
    Label botLabel = new Label("Bots:", MyGdxGame.skin);
    final SelectBox<String> botBox = new SelectBox<String>(MyGdxGame.skin);
    Array<String> botOptions = new Array<String>();
    for (int n = 0; n <= 3; n++) botOptions.add(String.valueOf(n));
    botBox.setItems(botOptions);
    botBox.setSelected("0");

    // ── Checkboxes ───────────────────────────────────────────────────────────
    final CheckBox manualSetupCheckbox = new CheckBox(" Manual setup", MyGdxGame.skin);
    manualSetupCheckbox.setChecked(pendingManualSetup);

    final CheckBox heroCheckbox = new CheckBox(" Allow starting hero", MyGdxGame.skin);
    heroCheckbox.setChecked(sessionAllowHeroSelection);

    // ── Create button ────────────────────────────────────────────────────────
    final TextButton confirmCreateBtn = new TextButton("Create", MyGdxGame.skin);
    confirmCreateBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        String sessionName = pendingSessionName.isEmpty()
            ? menuState.getMyName() + "'s game" : pendingSessionName;
        sessionAllowHeroSelection = heroCheckbox.isChecked();
        pendingManualSetup = manualSetupCheckbox.isChecked();
        try {
          pendingStartingCards = Integer.parseInt(cardsBox.getSelected());
        } catch (NumberFormatException ex) { pendingStartingCards = 8; }
        JSONObject data = new JSONObject();
        try {
          data.put("name", menuState.getMyName());
          data.put("sessionName", sessionName);
          data.put("allowHeroSelection", sessionAllowHeroSelection);
          data.put("startingCards", pendingStartingCards);
          data.put("manualSetup", pendingManualSetup);
          data.put("botCount", Integer.parseInt(botBox.getSelected()));
          data.put("token", MyGdxGame.playerStorage.getToken());
        } catch (JSONException e) { /* ignore */ }
        socket.emit("createSession", data);
        pendingSessionName = "";
        pendingManualSetup = false;
        pendingStartingCards = 8;
        inSessionCreate = false;
      }
    });

    // ── Table layout (no overlap guaranteed) ────────────────────────────────
    Table form = new Table(MyGdxGame.skin);
    form.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.18f)));
    form.pad(20f, 24f, 20f, 24f);
    float colW = MyGdxGame.WIDTH * 0.72f;

    form.add(title).colspan(2).center().padBottom(18f);
    form.row();
    form.add(gameNameBtn).colspan(2).fillX().padBottom(14f);
    form.row();
    form.add(cardsLabel).left().padRight(12f).padBottom(14f);
    form.add(cardsBox).width(colW * 0.38f).left().padBottom(14f);
    form.row();
    form.add(botLabel).left().padRight(12f).padBottom(14f);
    form.add(botBox).width(colW * 0.38f).left().padBottom(14f);
    form.row();
    form.add(manualSetupCheckbox).colspan(2).left().padBottom(10f);
    form.row();
    form.add(heroCheckbox).colspan(2).left().padBottom(18f);
    form.row();
    form.add(confirmCreateBtn).colspan(2).center();

    form.pack();
    // Centre the form vertically in the lower 55% of the screen (below the logo area)
    float formTop = 0.82f * MyGdxGame.HEIGHT;
    form.setPosition(
        Math.round(cx - form.getWidth() / 2f),
        Math.round(formTop - form.getHeight()));
    menuStage.addActor(form);

    addMusicToggleButton(menuStage);
    addLogoutButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  private JSONObject buildJoinData(String sessionId) {
    JSONObject data = new JSONObject();
    try {
      data.put("sessionId", sessionId);
      data.put("name", menuState.getMyName());
      data.put("token", MyGdxGame.playerStorage.getToken());
    } catch (JSONException e) { /* ignore */ }
    return data;
  }

  /** Logs the player out: clears saved name, leaves any session, returns to name-entry. */
  private void logout() {
    if (lobbyJoined) {
      socket.emit("leaveSession", "");
    }
    MyGdxGame.playerStorage.clearName();
    MyGdxGame.playerStorage.clearSessionId();
    menuState.setMyName("");
    nameConfirmed = false;
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

  /** Adds a small "Log out" button to the bottom-right of the given stage. */
  private void addLogoutButton(final Stage stage) {
    TextButton logoutBtn = new TextButton("Log out", MyGdxGame.skin);
    logoutBtn.pack();
    logoutBtn.setSize(logoutBtn.getPrefWidth() + 10, logoutBtn.getPrefHeight() + 6);
    logoutBtn.setPosition(MyGdxGame.WIDTH - logoutBtn.getWidth() - 10, 10);
    logoutBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        logout();
      }
    });
    stage.addActor(logoutBtn);
  }

  /**
   * Adds a small music on/off toggle button to the top-right corner of the given stage.
   * Also installs a one-shot capture listener so the first touch anywhere starts music
   * (works around browser autoplay restrictions).
   */
  private void addMusicToggleButton(final Stage stage) {
    final boolean enabled = MyGdxGame.playerStorage.getMusicEnabled();
    final TextButton musicBtn = new TextButton(enabled ? "Music ON" : "Music OFF", MyGdxGame.skin);
    musicBtn.pack();
    musicBtn.setSize(musicBtn.getPrefWidth() + 20, musicBtn.getPrefHeight() + 10);
    musicBtn.setPosition(MyGdxGame.WIDTH - musicBtn.getWidth() - 10,
        MyGdxGame.HEIGHT - musicBtn.getHeight() - 10);
    musicBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        // If musicStarted is true BUT music is not actually playing, the DOM handler
        // just started it; don't toggle off. Otherwise, normal toggle.
        boolean actuallyPlaying = MyGdxGame.activeMusic != null
            && MyGdxGame.activeMusic.isPlaying();
        if (MyGdxGame.musicStarted && MyGdxGame.playerStorage.getMusicEnabled() && !actuallyPlaying) {
          // DOM handler set musicStarted but play() may have failed; retry.
          MyGdxGame.setMusicEnabled(true);
        } else {
          MyGdxGame.setMusicEnabled(!MyGdxGame.playerStorage.getMusicEnabled());
        }
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
    float buttonY = 0.08f * MyGdxGame.HEIGHT;

    Image actionBar = new Image(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.12f)));
    actionBar.setSize(0.86f * MyGdxGame.WIDTH, button.getHeight() + 24f);
    actionBar.setPosition(cx - actionBar.getWidth() / 2f, buttonY - 10f);
    actionBar.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

    Label lobbyTitle = new Label("Game lobby", MyGdxGame.skin);
    float lobbyTitleScale = 1.35f;
    lobbyTitle.setFontScale(lobbyTitleScale);
    lobbyTitle.setColor(1f, 1f, 1f, 0.98f);
    lobbyTitle.setPosition(Math.round(cx - lobbyTitle.getPrefWidth() / 2f), Math.round(0.835f * MyGdxGame.HEIGHT));

    // table with all logged in users
    Table loggedInUserTable = new Table(MyGdxGame.skin);
    loggedInUserTable.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.14f)));
    loggedInUserTable.pad(14f, 18f, 14f, 18f);
    ArrayList<User> loggedInUsers = menuState.getUsers();

    Label headLine1 = new Label("Name", MyGdxGame.skin);
    Label headLine2 = new Label("Status", MyGdxGame.skin);
    headLine1.setColor(1f, 1f, 1f, 0.9f);
    headLine2.setColor(1f, 1f, 1f, 0.9f);

    loggedInUserTable.add(headLine1).padRight(60).padBottom(8f);
    loggedInUserTable.add(headLine2).padBottom(8f);
    loggedInUserTable.row();

    for (int i = 0; i < loggedInUsers.size(); i++) {
      User user = loggedInUsers.get(i);
      Label nameLabel = new Label(user.getName(), MyGdxGame.skin);

      if (user.getUserID().equals(menuState.getMyUserID())) {
        nameLabel.setColor(Color.GOLD);
      }

      Table statusBadge;
      if (user.isReady()) {
        statusBadge = createStatusBadge("READY", new Color(0.14f, 0.56f, 0.24f, 1f), Color.WHITE);
      } else {
        statusBadge = createStatusBadge("WAIT", new Color(0.64f, 0.14f, 0.14f, 1f), new Color(1f, 0.94f, 0.94f, 1f));
      }

      loggedInUserTable.add(nameLabel).padRight(60).padBottom(6f);
      loggedInUserTable.add(statusBadge).left().padBottom(6f);
      loggedInUserTable.row();
      if (i < loggedInUsers.size() - 1) {
        Image sep = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 1f, 1f, 0.14f)));
        loggedInUserTable.add(sep).colspan(2).growX().height(1f).padTop(2f).padBottom(5f);
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
      Label gameRunningLabel = new Label("Game in progress", MyGdxGame.skin);
      gameRunningLabel.setColor(Color.YELLOW);
      gameRunningLabel.setPosition(cx - gameRunningLabel.getWidth() / 2f, 0.11f * MyGdxGame.HEIGHT + 46f);
      menuStage.addActor(gameRunningLabel);

      TextButton watchButton = new TextButton("Watch game", MyGdxGame.skin);
      watchButton.setSize(button.getWidth(), button.getHeight());
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
    Label lobbyStatus = new Label("Ready players: " + readyCount + " / " + loggedInUsers.size(), MyGdxGame.skin);
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
      boolean isHost = !loggedInUsers.isEmpty()
          && loggedInUsers.get(0).getUserID().equals(menuState.getMyUserID());
      boolean canHostStart = isHost && amReady && readyCount >= 2 && !timerStarted;

      if (isHost) {
        TextButton startGameButton = new TextButton("Start game", MyGdxGame.skin);
        startGameButton.setSize(button.getWidth(), button.getHeight());
        float buttonGap = 20f;
        float readyButtonX = (MyGdxGame.WIDTH / 2f) - button.getWidth() - (buttonGap / 2f);
        float startButtonX = (MyGdxGame.WIDTH / 2f) + (buttonGap / 2f);
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

        button.setPosition(readyButtonX, buttonY);
      } else {
        button.setPosition((MyGdxGame.WIDTH - button.getWidth()) / 2f, buttonY);
      }

      if (timerStarted) {
        Label countdownLabel = new Label("Starting in " + menuState.getTimeToStart() + "...", MyGdxGame.skin);
        countdownLabel.setColor(Color.YELLOW);
        countdownLabel.setPosition(cx - countdownLabel.getWidth() / 2f, buttonY + button.getHeight() + 14f);
        menuStage.addActor(countdownLabel);
      }

      // Rebuild hero dropdown excluding heroes reserved by other lobby players.
      if (sessionAllowHeroSelection) {
        refreshHeroDropdown();

        // Add hero selector directly to stage so popup coordinates work correctly
        Label heroLabel = new Label("Starting hero:", MyGdxGame.skin);
        heroLabel.setPosition(heroSelectBox.getX(), heroSelectBox.getY() + heroSelectBox.getHeight() + 4);
        menuStage.addActor(heroLabel);
        menuStage.addActor(heroSelectBox);
      } else {
        // No hero selection in this session — clear any stale hero from a previous session.
        menuState.setStartingHero("None");
      }
      menuStage.addActor(button);
    }

    menuStage.addActor(actionBar);
    menuStage.addActor(lobbyTitle);
    menuStage.addActor(loggedInUserTable);

    // Leave session — returns to session list
    TextButton leaveBtn = new TextButton("Leave", MyGdxGame.skin);
    leaveBtn.setPosition(10, MyGdxGame.HEIGHT - leaveBtn.getHeight() - 10);
    leaveBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        socket.emit("leaveSession", "");
        MyGdxGame.playerStorage.clearSessionId();
        lobbyJoined = false;
        timerStarted = false;
        gameRunning = false;
        menuState.clearUsers();
        reservedByOthers.clear();
        show();
      }
    });
    menuStage.addActor(leaveBtn);

    addMusicToggleButton(menuStage);
    addLogoutButton(menuStage);
    Gdx.input.setInputProcessor(menuStage);
  }

  @Override
  public void render(float delta) {
    // System.out.println("render menu screen");
    Gdx.gl.glClearColor(0.55f, 0.73f, 0.55f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
    // TODO Auto-generated method stub

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
            User user = new User(userID, name);
            user.setReady(isReady);
            menuState.addUser(user);
            updateScreen = true;
            Gdx.app.log("SocketIO", "Get users " + name + " (" + userID + ") ready=" + isReady);
          }
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
          Gdx.app.log("SocketIO", "Received centralized game state, playerIndex: " + playerIndex);
          Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
              game.setScreen(new GameScreen(game, gameState, playerIndex, socket, menuState.getStartingHero()));
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
                  p.getString("status")
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
            } catch (Exception e) { /* keep default */ }
            lobbyJoined = true;
            updateScreen = true;
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
    socket.connect();
  }

}
