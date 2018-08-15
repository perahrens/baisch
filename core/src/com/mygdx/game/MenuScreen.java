package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class MenuScreen extends AbstractScreen {

  private Label welcome;
  private Label baisch;
  private TextArea userName;
  private TextButton button;

  private Texture logoTexture;
  private TextureRegion logoRegion;
  private Image logoImage;

  public MenuScreen(final Game game) {
    super(game);

    // create menu screen
    Group group = new Group();
    group.setBounds(0, 0, MyGdxGame.WIDTH, MyGdxGame.HEIGHT);

    // baisch logo
    logoTexture = new Texture(Gdx.files.internal("data/graphics/Logo.png"));
    logoRegion = new TextureRegion(logoTexture, 0, 0, 394, 271);
    logoImage = new Image(logoRegion);

    // welcome text
    welcome = new Label("Welcome to\nBAISCH", MyGdxGame.skin);
    userName = new TextArea("Enter your name", MyGdxGame.skin);
    button = new TextButton("Start game", MyGdxGame.skin);

    button.setSize(button.getWidth() * 2, button.getHeight() * 2);

    logoImage.setPosition((MyGdxGame.WIDTH - logoImage.getWidth()) / 2f,
        0.9f * MyGdxGame.HEIGHT - logoImage.getHeight());
    // welcome.setPosition((MyGdxGame.WIDTH-welcome.getWidth())/2f,0.9f*MyGdxGame.HEIGHT);
    // userName.setPosition((MyGdxGame.WIDTH-userName.getWidth())/2f,
    // 0.7f*MyGdxGame.HEIGHT);
    button.setPosition((MyGdxGame.WIDTH - button.getWidth()) / 2f, 0.1f * MyGdxGame.HEIGHT);

    button.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        game.setScreen(new GameScreen(game));
      };
    });

    group.addActor(logoImage);
    // group.addActor(welcome);
    // group.addActor(userName);
    group.addActor(button);

    MyGdxGame.stage.addActor(group);
    MyGdxGame.stage.getCamera().position.set(MyGdxGame.WIDTH / 2, MyGdxGame.HEIGHT / 2, 0);

    Gdx.input.setInputProcessor(MyGdxGame.stage);

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

    SpriteBatch batch = new SpriteBatch();

    batch.begin();
    MyGdxGame.stage.draw();
    batch.end();

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
