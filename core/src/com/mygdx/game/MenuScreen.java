package com.mygdx.game;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MenuScreen extends AbstractScreen {

  private Socket socket;

  private Stage menuStage;
  private MenuState menuState;

  private Label loggedInCount;
  private TextArea userName;
  private TextButton button;

  private Group group;

  private Texture logoTexture;
  private TextureRegion logoRegion;
  private Image logoImage;

  private int currentUsersCount;
  private boolean updateScreen = false;
  boolean timerStarted = false;

  public MenuScreen(final Game game, final Socket socket) {
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

    // welcome text --> deprecated
    userName = new TextArea("Enter your name", MyGdxGame.skin);
    button = new TextButton("Start game", MyGdxGame.skin);

    button.setSize(button.getWidth() * 2, button.getHeight() * 2);

    logoImage.setPosition((MyGdxGame.WIDTH - logoImage.getWidth()) / 2f,
        0.9f * MyGdxGame.HEIGHT - logoImage.getHeight());
    // userName.setPosition((MyGdxGame.WIDTH-userName.getWidth())/2f,
    // 0.7f*MyGdxGame.HEIGHT);
    button.setPosition((MyGdxGame.WIDTH - button.getWidth()) / 2f, 0.1f * MyGdxGame.HEIGHT);

    button.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        // game.setScreen(new GameScreen(game));
        socket.emit("setUserReady", menuState.getMyUserID());
        System.out.println("test");
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

  @Override
  public void show() {
    menuStage.clear();

    // logged in count
    loggedInCount = new Label("Logged in users: " + currentUsersCount, MyGdxGame.skin);
    loggedInCount.setPosition(0, 0);

    // table with all logged in users
    Table loggedInUserTable = new Table(MyGdxGame.skin);
    ArrayList<User> loggedInUsers = menuState.getUsers();

    Label headLine1 = new Label("User ID", MyGdxGame.skin);
    headLine1.setFontScale(1.2f);
    Label headLine2 = new Label("Status", MyGdxGame.skin);
    headLine2.setFontScale(1.2f);

    loggedInUserTable.add(headLine1);
    loggedInUserTable.add(headLine2);
    loggedInUserTable.row();

    for (int i = 0; i < loggedInUsers.size(); i++) {
      User user = loggedInUsers.get(i);
      Label userIDLabel = new Label(user.getUserID() + "      ", MyGdxGame.skin);

      if (user.getUserID().equals(menuState.getMyUserID())) {
        userIDLabel.setColor(Color.GOLD);
      }

      Label isReady;
      if (user.isReady()) {
        isReady = new Label("GO", MyGdxGame.skin);
        isReady.setColor(Color.GREEN);
      } else {
        isReady = new Label("WAIT", MyGdxGame.skin);
        isReady.setColor(Color.RED);
      }

      loggedInUserTable.add(userIDLabel);
      loggedInUserTable.add(isReady);
      loggedInUserTable.row();
    }

    loggedInUserTable.setPosition(200, 300);

    // check if all players are ready
    if (menuState.allReady() && !timerStarted) {
      System.out.println("All players ready...");
      socket.emit("startTimer", 5);
      menuState.setTimeToStart(5);
      Timer.schedule(new Task() {
        @Override
        public void run() {
          show();
          System.out.println("timeToStart= " + menuState.getTimeToStart());
        }
      }, 0, 1);
      timerStarted = true;
    }

    Label timerLabel = new Label("Waiting for players ... ", MyGdxGame.skin);
    if (timerStarted) {
      timerLabel = new Label("Time to start ... " + menuState.getTimeToStart(), MyGdxGame.skin);
    }
    timerLabel.setPosition(200, 0);

    menuStage.addActor(group);
    menuStage.addActor(timerLabel);
    menuStage.addActor(loggedInUserTable);
    menuStage.addActor(loggedInCount);
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

  public void configSocketEvents(Socket socket) {
    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        Gdx.app.log("SocketIO", "Connected");
      }
    }).on("socketID", new Emitter.Listener() {

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
    }).on("newUser", new Emitter.Listener() {

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
    }).on("userDisconnected", new Emitter.Listener() {

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
    }).on("getUsers", new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        JSONArray objects = (JSONArray) args[0];
        try {
          menuState.clearUsers();
          for (int i = 0; i < objects.length(); i++) {
            String userID = ((String) objects.getJSONObject(i).getString("id"));
            boolean isReady = ((boolean) objects.getJSONObject(i).getBoolean("isReady"));
            User user = new User(userID);
            user.setReady(isReady);
            menuState.addUser(user);
            updateScreen = true;
            Gdx.app.log("SocketIO", "Get users " + userID + " " + isReady);
          }
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error getting new user ID ");
        }
        Gdx.app.log("SocketIO", "Number of users = " + menuState.getUsers().size());
      }
    }).on("userReady", new Emitter.Listener() {

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
  }

}
