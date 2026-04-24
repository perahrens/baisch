package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

/**
 * Tutorial selection screen: lets the player choose between the Basic Rules
 * tutorial (which starts an actual bot game) or a hero-specific info tutorial.
 */
public class TutorialSelectScreen extends AbstractScreen {

  private static final String[] HERO_NAMES = {
    "Mercenaries", "Marshal", "Spy", "Battery Tower", "Merchant", "Priest",
    "Reservists", "Banneret", "Saboteurs", "Fortified Tower", "Magician", "Warlord"
  };

  private final SocketClient socket;
  private Stage stage;
  private Texture bgTexture;

  public TutorialSelectScreen(Game game, SocketClient socket) {
    super(game);
    this.socket = socket;
    stage = new Stage(new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT));
    bgTexture = new Texture(Gdx.files.internal("data/graphics/bg_darkmoon.jpg"));
    build();
  }

  private void build() {
    if (bgTexture != null) {
      Image bg = new Image(bgTexture);
      bg.setFillParent(true);
      stage.addActor(bg);
    }
    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(20f);

    // Title
    Label title = new Label("Choose a Tutorial", MyGdxGame.skin, "default");
    title.setColor(Color.GOLD);
    root.add(title).padBottom(16f).row();

    // Basic tutorial button
    TextButton basicBtn = new TextButton("► Basic Rules", MyGdxGame.skin);
    basicBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        socket.emit("createTutorial", new JSONObject());
      }
    });
    root.add(basicBtn).width(340f).height(90f).padBottom(24f).row();

    // Section label
    Label heroLabel = new Label("Hero Tutorials", MyGdxGame.skin, "default");
    heroLabel.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
    root.add(heroLabel).padBottom(10f).row();

    // Scrollable list of hero buttons
    Table heroTable = new Table();
    heroTable.top();
    for (final String heroName : HERO_NAMES) {
      TextButton btn = new TextButton(heroName, MyGdxGame.skin);
      btn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          // Issue #171: launch an interactive bot game scenario for this hero
          // (replaces the prior text-only HeroTutorialScreen).
          try {
            JSONObject payload = new JSONObject();
            payload.put("heroName", heroName);
            socket.emit("createHeroTutorial", payload);
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        }
      });
      heroTable.add(btn).width(320f).padBottom(4f).row();
    }

    ScrollPane scroll = new ScrollPane(heroTable, MyGdxGame.skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    root.add(scroll).width(360f).height(420f).row();

    // Back button
    TextButton backBtn = new TextButton("Back", MyGdxGame.skin);
    backBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        game.setScreen(new MenuScreen(game, socket));
      }
    });
    root.add(backBtn).width(200f).height(90f).padTop(14f).row();

    stage.addActor(root);
  }

  @Override
  public void show() {
    Gdx.input.setInputProcessor(stage);
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    stage.act(delta);
    stage.draw();
  }

  @Override
  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override public void pause() {}
  @Override public void resume() {}
  @Override public void hide() {}

  @Override
  public void dispose() {
    stage.dispose();
    if (bgTexture != null) { bgTexture.dispose(); bgTexture = null; }
  }
}
