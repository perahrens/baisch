package com.mygdx.game;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MenuScreen extends AbstractScreen {

  private Stage menuStage;
  private MenuState menuState;

  private Label loggedInCount;
  private TextArea userName;
  private TextButton button;
  
  private Group group;

  private Texture logoTexture;
  private TextureRegion logoRegion;
  private Image logoImage;

  public MenuScreen(final Game game, Socket socket) {
    super(game);
    
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
        System.out.println("test");
      };
    });

    group.addActor(logoImage);
    group.addActor(button);

    menuStage.addActor(group);
    menuStage.getCamera().position.set(MyGdxGame.WIDTH / 2, MyGdxGame.HEIGHT / 2, 0);

    Gdx.input.setInputProcessor(menuStage);

  }

  public void configSocketEvents(Socket socket) {
    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        // TODO Auto-generated method stub
        Gdx.app.log("SocketIO", "Connected");
      }
    }).on("socketID", new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        // TODO Auto-generated method stub
        JSONObject data = (JSONObject) args[0];
        try {
          String id = data.getString("id");
          Gdx.app.log("SocketIO", "My ID: " + id);
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error getting ID");
        }
      }
    }).on("newPlayer", new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        // TODO Auto-generated method stub
        JSONObject data = (JSONObject) args[0];
        try {
          String id = data.getString("id");
          Gdx.app.log("SocketIO", "New Player connected: " + id);
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error getting new player ID ");
        }
      }
    }).on("playerDisconnected", new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        // TODO Auto-generated method stub
        JSONObject data = (JSONObject) args[0];
        try {
          String id = data.getString("id");
          Gdx.app.log("SocketIO", "Player disconnected: " + id);
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error disconnecting player ID ");
        }
      }
    }).on("getPlayers", new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        // TODO Auto-generated method stub
        JSONArray objects = (JSONArray) args[0];
        try {
          for (int i = 0; i < objects.length(); i++) {
            String playerName = ((String) objects.getJSONObject(i).getString("id"));
            Player player = new Player(playerName);
            menuState.addPlayer(player);
            Gdx.app.log("SocketIO", "Get players " + playerName);
          }
        } catch (JSONException e) {
          Gdx.app.log("SocketIO", "Error getting new player ID ");
        }
        Gdx.app.log("SocketIO", "Numger of players = " + menuState.getPlayers().size());
      }
    });
  }

  public void create() {

  }

  @Override
  public void show() {
    // TODO Auto-generated method stub

  }

  @Override
  public void render(float delta) {
    // System.out.println("render menu screen");
    Gdx.gl.glClearColor(0.55f, 0.73f, 0.55f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    
    menuStage.clear();

    // logged in count
    loggedInCount = new Label("Logged in users: " + menuState.getPlayers().size(), MyGdxGame.skin);
    loggedInCount.setPosition(0, 0);

    menuStage.addActor(group);
    menuStage.addActor(loggedInCount);

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

}
