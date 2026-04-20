package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.heroes.HeroDescriptions;
import com.mygdx.game.net.SocketClient;

/**
 * Displays a hero's description in a paginated step-by-step format.
 * Each page corresponds to a paragraph from HeroDescriptions.
 */
public class HeroTutorialScreen extends AbstractScreen {

  private final SocketClient socket;
  private final String heroName;
  private final String[] pages;
  private int page = 0;
  private Stage stage;

  public HeroTutorialScreen(Game game, SocketClient socket, String heroName) {
    super(game);
    this.socket = socket;
    this.heroName = heroName;

    // Split description into pages on double-newline
    String desc = HeroDescriptions.get(heroName);
    this.pages = desc.split("\n\n");

    stage = new Stage(new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT));
    buildPage();
  }

  private void buildPage() {
    stage.clear();

    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(20f);

    // Hero name
    Label title = new Label(heroName, MyGdxGame.skin, "default");
    title.setColor(Color.GOLD);
    root.add(title).padBottom(4f).row();

    // Page indicator
    Label pageIndicator = new Label("Step " + (page + 1) + " / " + pages.length, MyGdxGame.skin, "default");
    pageIndicator.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
    root.add(pageIndicator).padBottom(18f).row();

    // Content text
    Label content = new Label(pages[page].trim(), MyGdxGame.skin, "default");
    content.setWrap(true);
    root.add(content).width(400f).padBottom(28f).row();

    // Navigation buttons row
    Table nav = new Table();
    if (page > 0) {
      TextButton prevBtn = new TextButton("◄ Prev", MyGdxGame.skin);
      prevBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          page--;
          buildPage();
        }
      });
      nav.add(prevBtn).width(130f).height(46f).padRight(10f);
    }
    if (page < pages.length - 1) {
      TextButton nextBtn = new TextButton("Next ►", MyGdxGame.skin);
      nextBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          page++;
          buildPage();
        }
      });
      nav.add(nextBtn).width(130f).height(46f);
    } else {
      TextButton doneBtn = new TextButton("Done ✓", MyGdxGame.skin);
      doneBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          game.setScreen(new TutorialSelectScreen(game, socket));
        }
      });
      nav.add(doneBtn).width(130f).height(46f);
    }
    root.add(nav).padBottom(16f).row();

    // Back to tutorial list
    TextButton backBtn = new TextButton("Back to Tutorials", MyGdxGame.skin);
    backBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        game.setScreen(new TutorialSelectScreen(game, socket));
      }
    });
    root.add(backBtn).width(240f).height(44f).row();

    stage.addActor(root);
  }

  @Override
  public void show() {
    Gdx.input.setInputProcessor(stage);
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClearColor(0.08f, 0.08f, 0.18f, 1f);
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
  }
}
