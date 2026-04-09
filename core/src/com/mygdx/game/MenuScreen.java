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

  private SocketClient socket;

  private Stage menuStage;
  private MenuState menuState;

  private Label loggedInCount;
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
  private boolean sessionAllowHeroSelection = true;

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
    if (currentSelected != null && !reservedByOthers.contains(currentSelected)) {
      heroSelectBox.setSelected(currentSelected);
    } else {
      heroSelectBox.setSelected("None");
      menuState.setStartingHero("None");
    }
    updatingDropdown = false;
  }

  @Override
  public void show() {
    heroSelectBox.hideList();
    menuStage.clear();

    menuStage.addActor(group);

    if (!nameConfirmed) {
      showNameEntryScreen();
    } else if (!lobbyJoined && inSessionCreate) {
      showSessionCreateScreen();
    } else if (!lobbyJoined) {
      showSessionListScreen();
    } else {
      showLobbyScreen();
    }
  }

  private void showNameEntryScreen() {
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
            nameConfirmed = true;
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
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showSessionListScreen() {
    float cx = MyGdxGame.WIDTH / 2f;

    Label title = new Label("Games", MyGdxGame.skin);
    title.setPosition(cx - title.getWidth() / 2f, 0.75f * MyGdxGame.HEIGHT);
    menuStage.addActor(title);

    Table sessTable = new Table(MyGdxGame.skin);
    Label h1 = new Label("Name", MyGdxGame.skin);
    Label h2 = new Label("Players", MyGdxGame.skin);
    Label h3 = new Label("", MyGdxGame.skin);
    sessTable.add(h1).padRight(20);
    sessTable.add(h2).padRight(20);
    sessTable.add(h3);
    sessTable.row();

    final java.util.List<SessionInfo> list = new java.util.ArrayList<SessionInfo>(sessionList);
    for (final SessionInfo s : list) {
      Label nameL = new Label(s.name, MyGdxGame.skin);
      Label countL = new Label(s.playerCount + "/4", MyGdxGame.skin);
      if (s.running) {
        Label runL = new Label("Playing", MyGdxGame.skin);
        runL.setColor(Color.YELLOW);
        TextButton watchBtn = new TextButton("Watch", MyGdxGame.skin);
        watchBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            socket.emit("joinSessionSpectator", buildJoinData(s.id));
          }
        });
        sessTable.add(nameL).padRight(20);
        sessTable.add(countL).padRight(20);
        sessTable.add(watchBtn);
      } else {
        TextButton joinBtn = new TextButton("Join", MyGdxGame.skin);
        joinBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            socket.emit("joinSession", buildJoinData(s.id));
          }
        });
        sessTable.add(nameL).padRight(20);
        sessTable.add(countL).padRight(20);
        sessTable.add(joinBtn);
      }
      sessTable.row();
    }

    sessTable.pack();
    sessTable.setPosition(cx - sessTable.getWidth() / 2f, 0.35f * MyGdxGame.HEIGHT);
    menuStage.addActor(sessTable);

    TextButton createBtn = new TextButton("Create game", MyGdxGame.skin);
    createBtn.setSize(button.getWidth() * 1.5f, button.getHeight());
    createBtn.setPosition(cx - createBtn.getWidth() / 2f, 0.15f * MyGdxGame.HEIGHT);
    createBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        inSessionCreate = true;
        show();
      }
    });

    menuStage.addActor(createBtn);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showSessionCreateScreen() {
    float cx = MyGdxGame.WIDTH / 2f;

    Label title = new Label("New game", MyGdxGame.skin);
    title.setPosition(cx - title.getWidth() / 2f, 0.75f * MyGdxGame.HEIGHT);
    menuStage.addActor(title);

    // Button that shows the current game name and opens a native dialog to edit it
    final String nameDisplay = pendingSessionName.isEmpty() ? "Set name (optional)" : pendingSessionName;
    final TextButton gameNameBtn = new TextButton(nameDisplay, MyGdxGame.skin);
    gameNameBtn.setSize(button.getWidth() * 2, button.getHeight());
    gameNameBtn.setPosition(cx - gameNameBtn.getWidth() / 2f, 0.55f * MyGdxGame.HEIGHT);
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
    menuStage.addActor(gameNameBtn);

    // Checkbox: allow starting hero selection
    final CheckBox heroCheckbox = new CheckBox(" Allow starting hero", MyGdxGame.skin);
    heroCheckbox.setChecked(sessionAllowHeroSelection);
    heroCheckbox.pack();
    heroCheckbox.setPosition(cx - heroCheckbox.getWidth() / 2f, 0.42f * MyGdxGame.HEIGHT);
    menuStage.addActor(heroCheckbox);

    // Create button
    TextButton confirmCreateBtn = new TextButton("Create", MyGdxGame.skin);
    confirmCreateBtn.setSize(button.getWidth(), button.getHeight());
    confirmCreateBtn.setPosition(cx - confirmCreateBtn.getWidth() / 2f, 0.28f * MyGdxGame.HEIGHT);
    confirmCreateBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        String sessionName = pendingSessionName.isEmpty()
            ? menuState.getMyName() + "'s game" : pendingSessionName;
        sessionAllowHeroSelection = heroCheckbox.isChecked();
        JSONObject data = new JSONObject();
        try {
          data.put("name", menuState.getMyName());
          data.put("sessionName", sessionName);
          data.put("allowHeroSelection", sessionAllowHeroSelection);
        } catch (JSONException e) { /* ignore */ }
        socket.emit("createSession", data);
        pendingSessionName = "";
        inSessionCreate = false;
      }
    });
    menuStage.addActor(confirmCreateBtn);

    // Back button
    TextButton backBtn = new TextButton("Back", MyGdxGame.skin);
    backBtn.setPosition(10, MyGdxGame.HEIGHT - backBtn.getHeight() - 10);
    backBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        inSessionCreate = false;
        show();
      }
    });
    menuStage.addActor(backBtn);

    Gdx.input.setInputProcessor(menuStage);
  }

  private JSONObject buildJoinData(String sessionId) {
    JSONObject data = new JSONObject();
    try {
      data.put("sessionId", sessionId);
      data.put("name", menuState.getMyName());
    } catch (JSONException e) { /* ignore */ }
    return data;
  }

  private void showLobbyScreen() {
    // logged in count
    loggedInCount = new Label("Players in lobby: " + currentUsersCount, MyGdxGame.skin);
    loggedInCount.setPosition(0, 0);

    // table with all logged in users
    Table loggedInUserTable = new Table(MyGdxGame.skin);
    ArrayList<User> loggedInUsers = menuState.getUsers();

    Label headLine1 = new Label("Name", MyGdxGame.skin);
    Label headLine2 = new Label("Status", MyGdxGame.skin);

    loggedInUserTable.add(headLine1);
    loggedInUserTable.add(headLine2);
    loggedInUserTable.row();

    for (int i = 0; i < loggedInUsers.size(); i++) {
      User user = loggedInUsers.get(i);
      Label nameLabel = new Label(user.getName() + "      ", MyGdxGame.skin);

      if (user.getUserID().equals(menuState.getMyUserID())) {
        nameLabel.setColor(Color.GOLD);
      }

      Label isReady;
      if (user.isReady()) {
        isReady = new Label("READY", MyGdxGame.skin);
        isReady.setColor(Color.GREEN);
      } else {
        isReady = new Label("WAIT", MyGdxGame.skin);
        isReady.setColor(Color.RED);
      }

      loggedInUserTable.add(nameLabel);
      loggedInUserTable.add(isReady);
      loggedInUserTable.row();
    }

    loggedInUserTable.pack();
    loggedInUserTable.setPosition((MyGdxGame.WIDTH - loggedInUserTable.getWidth()) / 2f, 300);

    // Notification permission status — shown in all lobby states
    if (MyGdxGame.turnNotifier.isPermissionGranted()) {
      Label notifLabel = new Label("\uD83D\uDD14 Notifications: ON", MyGdxGame.skin);
      notifLabel.setColor(Color.GREEN);
      notifLabel.setPosition(0, 50);
      menuStage.addActor(notifLabel);
    } else {
      TextButton notifButton = new TextButton("\uD83D\uDD14 Enable notifications", MyGdxGame.skin);
      notifButton.setPosition(0, 50);
      notifButton.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          MyGdxGame.turnNotifier.requestPermission(new Runnable() {
            @Override public void run() { show(); }
          });
        }
      });
      menuStage.addActor(notifButton);
    }

    if (gameRunning) {
      // A game is already in progress — show status and offer spectating
      Label gameRunningLabel = new Label("Game in progress", MyGdxGame.skin);
      gameRunningLabel.setColor(Color.YELLOW);
      gameRunningLabel.setPosition(200, 50);
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
    } else {
      // No game running — show normal ready/start controls
      // check if all players are ready (requires >= 2)
      if (menuState.allReady() && !timerStarted) {
        System.out.println("All players ready...");
        socket.emit("startTimer", 5);
        menuState.setTimeToStart(5);
        timerStarted = true;
      }

      if (!menuState.allReady() && timerStarted) {
        timerStarted = false;
        menuState.setTimeToStart(5);
      }

      Label timerLabel = new Label("Waiting for players ... ", MyGdxGame.skin);
      if (timerStarted) {
        timerLabel = new Label("Time to start ... " + menuState.getTimeToStart(), MyGdxGame.skin);
      }
      timerLabel.setPosition(200, 0);
      menuStage.addActor(timerLabel);

      // Rebuild hero dropdown excluding heroes reserved by other lobby players.
      if (sessionAllowHeroSelection) {
        refreshHeroDropdown();

        // Add hero selector directly to stage so popup coordinates work correctly
        Label heroLabel = new Label("Starting hero:", MyGdxGame.skin);
        heroLabel.setPosition(heroSelectBox.getX(), heroSelectBox.getY() + heroSelectBox.getHeight() + 4);
        menuStage.addActor(heroLabel);
        menuStage.addActor(heroSelectBox);
      }
      menuStage.addActor(button);
    }

    menuStage.addActor(loggedInUserTable);
    menuStage.addActor(loggedInCount);

    // Leave session — returns to session list
    TextButton leaveBtn = new TextButton("Leave", MyGdxGame.skin);
    leaveBtn.setPosition(10, MyGdxGame.HEIGHT - leaveBtn.getHeight() - 10);
    leaveBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        socket.emit("leaveSession", "");
        lobbyJoined = false;
        timerStarted = false;
        gameRunning = false;
        menuState.clearUsers();
        reservedByOthers.clear();
        show();
      }
    });
    menuStage.addActor(leaveBtn);

    Gdx.input.setInputProcessor(menuStage);
  }

  @Override
  public void render(float delta) {
    // System.out.println("render menu screen");
    Gdx.gl.glClearColor(0.55f, 0.73f, 0.55f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    if (currentUsersCount != menuState.getUsers().size()) {
      currentUsersCount = menuState.getUsers().size();
      show();
    }

    if (updateScreen) {
      updateScreen = false;
      show();
    }

    if (menuState.getTimeToStart() <= 0 && timerStarted) {
      Timer.instance().clear();
      socket.emit("checkTimer", 0);
      timerStarted = false;
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
          Gdx.app.log("SocketIO", "Error parsing centralized game state!");
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
          Gdx.app.log("SocketIO", "Seconds to start game: " + timeToStart);
          show();
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error in timer!");
        }
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

    socket.on("returnToLobby", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            timerStarted = false;
            gameRunning = false;
            lobbyJoined = false;
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

    socket.on("sessionJoined", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              sessionAllowHeroSelection = data.optBoolean("allowHeroSelection", true);
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
            // Session was removed between list refresh and join — just refresh
            updateScreen = true;
          }
        });
      }
    });

    // Connect only after all listeners are registered so no events are missed
    socket.connect();
  }

}
