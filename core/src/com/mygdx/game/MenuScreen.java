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
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.Input;
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

  // Whether the player has entered a name and joined the lobby.
  private boolean lobbyJoined = false;

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
        socket.emit("setUserReady", menuState.getMyUserID());
      };
    });

    group.addActor(logoImage);
    group.addActor(button);

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

    if (!lobbyJoined) {
      showNameEntryScreen();
    } else {
      showLobbyScreen();
    }
  }

  private void showNameEntryScreen() {
    TextButton enterNameButton = new TextButton("Enter your name", MyGdxGame.skin);
    enterNameButton.setSize(button.getWidth(), button.getHeight());
    enterNameButton.setPosition((MyGdxGame.WIDTH - enterNameButton.getWidth()) / 2f, 0.45f * MyGdxGame.HEIGHT);
    enterNameButton.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        Gdx.input.getTextInput(new Input.TextInputListener() {
          @Override
          public void input(String text) {
            String name = text.trim();
            if (name.isEmpty()) {
              showNameEntryScreen();
              return;
            }
            menuState.setMyName(name);
            lobbyJoined = true;
            socket.emit("joinLobby", name);
            show();
          }
          @Override
          public void canceled() {
            // Stay on name entry screen
          }
        }, "Baisch", "", "Enter your name");
      }
    });

    menuStage.addActor(enterNameButton);
    Gdx.input.setInputProcessor(menuStage);
  }

  private void showLobbyScreen() {
    // logged in count
    loggedInCount = new Label("Players in lobby: " + currentUsersCount, MyGdxGame.skin);
    loggedInCount.setPosition(0, 0);

    // table with all logged in users
    Table loggedInUserTable = new Table(MyGdxGame.skin);
    ArrayList<User> loggedInUsers = menuState.getUsers();

    Label headLine1 = new Label("Name", MyGdxGame.skin);
    headLine1.setFontScale(1.2f);
    Label headLine2 = new Label("Status", MyGdxGame.skin);
    headLine2.setFontScale(1.2f);

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

    loggedInUserTable.setPosition(200, 300);

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
    menuStage.addActor(loggedInUserTable);
    menuStage.addActor(loggedInCount);

    // Rebuild hero dropdown excluding heroes reserved by other lobby players.
    refreshHeroDropdown();

    // Add hero selector directly to stage so popup coordinates work correctly
    Label heroLabel = new Label("Starting hero:", MyGdxGame.skin);
    heroLabel.setPosition(heroSelectBox.getX(), heroSelectBox.getY() + heroSelectBox.getHeight() + 4);
    menuStage.addActor(heroLabel);
    menuStage.addActor(heroSelectBox);
    menuStage.addActor(button);
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

    // Connect only after all listeners are registered so no events are missed
    socket.connect();
  }

}
