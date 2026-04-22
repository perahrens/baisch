package com.mygdx.game;

import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * End-of-game statistics screen shown immediately after a winner is declared.
 * Two tabs: General (round count) and Players (per-player breakdown).
 * A "Return to Lobby" button navigates back to the session list.
 */
public class StatsScreen extends AbstractScreen {

  private static final Color ACTIVE_COLOR   = Color.WHITE;
  private static final Color INACTIVE_COLOR = new Color(1f, 1f, 1f, 0.35f);
  private static final Color UNDERLINE_COLOR = new Color(0.98f, 0.80f, 0.25f, 1f);
  private static final Color HEADER_COLOR    = new Color(1f, 1f, 1f, 0.9f);
  private static final Color ROW_BG_COLOR    = new Color(0f, 0f, 0f, 0.35f);
  private static final Color SEP_COLOR       = new Color(1f, 1f, 1f, 0.25f);
  private static final Color DIM_SEP_COLOR   = new Color(1f, 1f, 1f, 0.14f);

  private final SocketClient socket;
  private final JSONObject stats;
  private Stage stage;
  private boolean showPlayersTab = false;

  public StatsScreen(Game game, SocketClient socket, JSONObject stats) {
    super(game);
    this.socket = socket;
    this.stats  = stats;
  }

  @Override
  public void show() {
    stage = new Stage(new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT));
    Gdx.input.setInputProcessor(stage);
    buildUI();
  }

  // ── UI construction ─────────────────────────────────────────────────────────

  private void buildUI() {
    final float cx = MyGdxGame.WIDTH / 2f;

    // Title
    Label title = new Label("Game Statistics", MyGdxGame.skin);
    title.pack();
    title.setPosition(Math.round(cx - title.getWidth() / 2f),
        Math.round(0.91f * MyGdxGame.HEIGHT));
    stage.addActor(title);

    // ── Tab bar ──────────────────────────────────────────────────────────────
    Label generalTab = new Label("General", MyGdxGame.skin);
    Label playersTab = new Label("Players", MyGdxGame.skin);
    generalTab.pack();
    playersTab.pack();

    float tabGap    = 32f;
    float tabsWidth = generalTab.getWidth() + tabGap + playersTab.getWidth();
    float tabY      = 0.855f * MyGdxGame.HEIGHT;
    float underlineH = 3f;

    generalTab.setPosition(Math.round(cx - tabsWidth / 2f), tabY);
    playersTab.setPosition(Math.round(cx - tabsWidth / 2f + generalTab.getWidth() + tabGap), tabY);

    generalTab.setColor(!showPlayersTab ? ACTIVE_COLOR : INACTIVE_COLOR);
    playersTab.setColor( showPlayersTab ? ACTIVE_COLOR : INACTIVE_COLOR);
    generalTab.setTouchable(Touchable.disabled);
    playersTab.setTouchable(Touchable.disabled);

    Label activeTab = !showPlayersTab ? generalTab : playersTab;
    Image underline = new Image(MyGdxGame.skin.newDrawable("white", UNDERLINE_COLOR));
    underline.setSize(activeTab.getWidth(), underlineH);
    underline.setPosition(activeTab.getX(), activeTab.getY() - underlineH - 2f);

    // Invisible hit actors for generous tap targets
    com.badlogic.gdx.scenes.scene2d.Actor generalHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    generalHit.setBounds(generalTab.getX() - 8f, tabY - 8f,
        generalTab.getWidth() + 16f, generalTab.getHeight() + 16f);
    generalHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        showPlayersTab = false; show();
      }
    });

    com.badlogic.gdx.scenes.scene2d.Actor playersHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    playersHit.setBounds(playersTab.getX() - 8f, tabY - 8f,
        playersTab.getWidth() + 16f, playersTab.getHeight() + 16f);
    playersHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        showPlayersTab = true; show();
      }
    });

    stage.addActor(generalHit);
    stage.addActor(playersHit);
    stage.addActor(underline);
    stage.addActor(generalTab);
    stage.addActor(playersTab);

    // ── Tab content ──────────────────────────────────────────────────────────
    if (!showPlayersTab) {
      buildGeneralTab(cx);
    } else {
      buildPlayersTab(cx);
    }

    // ── Return to Lobby button ───────────────────────────────────────────────
    TextButton returnBtn = new TextButton("Return to Lobby", MyGdxGame.skin);
    returnBtn.pack();
    returnBtn.setPosition(Math.round(cx - returnBtn.getWidth() / 2f),
        Math.round(0.055f * MyGdxGame.HEIGHT));
    returnBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        game.setScreen(new MenuScreen(game, socket));
      }
    });
    stage.addActor(returnBtn);
  }

  private void buildGeneralTab(float cx) {
    int rounds = 0;
    try { rounds = stats.getInt("rounds"); } catch (JSONException e) { /* ignore */ }

    Table table = new Table(MyGdxGame.skin);
    table.setBackground(MyGdxGame.skin.newDrawable("white", ROW_BG_COLOR));
    table.pad(18f, 22f, 18f, 22f);

    Label roundsLabel = new Label("Rounds played", MyGdxGame.skin);
    Label roundsValue = new Label(String.valueOf(rounds), MyGdxGame.skin);
    roundsLabel.setColor(HEADER_COLOR);
    roundsValue.setColor(HEADER_COLOR);

    table.add(roundsLabel).left().padRight(32f);
    table.add(roundsValue).right();

    table.pack();
    table.setPosition(Math.round(cx - table.getWidth() / 2f),
        Math.round(0.5f * MyGdxGame.HEIGHT - table.getHeight() / 2f));
    stage.addActor(table);
  }

  private void buildPlayersTab(float cx) {
    JSONArray players = null;
    try { players = stats.getJSONArray("players"); } catch (JSONException e) { /* fall through */ }

    Table table = new Table(MyGdxGame.skin);
    table.setBackground(MyGdxGame.skin.newDrawable("white", ROW_BG_COLOR));
    table.pad(14f, 12f, 14f, 12f);

    // Column widths
    float colPlace  = 28f;
    float colName   = 90f;
    float colRounds = 44f;
    float colPlund  = 52f;
    float colAtk    = 52f;
    float colHero   = 46f;

    // Header row
    Color hc = HEADER_COLOR;
    addCell(table, "#",        colPlace,  hc, false);
    addCell(table, "Name",     colName,   hc, true);
    addCell(table, "Rounds",   colRounds, hc, false);
    addCell(table, "Plunders", colPlund,  hc, false);
    addCell(table, "Attacks",  colAtk,    hc, false);
    addCell(table, "Heroes",   colHero,   hc, false);
    table.row();

    // Header separator
    Image hSep = new Image(MyGdxGame.skin.newDrawable("white", SEP_COLOR));
    table.add(hSep).colspan(6).growX().height(1f).padBottom(5f).padTop(3f);
    table.row();

    // Sub-header "(S/F)" hint row
    addCell(table, "",      colPlace,  INACTIVE_COLOR, false);
    addCell(table, "",      colName,   INACTIVE_COLOR, true);
    addCell(table, "",      colRounds, INACTIVE_COLOR, false);
    addCell(table, "(S/F)", colPlund,  INACTIVE_COLOR, false);
    addCell(table, "(S/F)", colAtk,    INACTIVE_COLOR, false);
    addCell(table, "",      colHero,   INACTIVE_COLOR, false);
    table.row();

    if (players == null || players.length() == 0) {
      Label empty = new Label("No data available", MyGdxGame.skin);
      empty.setColor(INACTIVE_COLOR);
      table.add(empty).colspan(6).padTop(8f);
      table.row();
    } else {
      for (int i = 0; i < players.length(); i++) {
        try {
          JSONObject p = players.getJSONObject(i);
          int placement   = p.optInt("placement", i + 1);
          String name     = p.optString("name", "?");
          int roundsOut   = p.optInt("roundsUntilOut", 0);
          int plundOk     = p.optInt("plundersSuccess", 0);
          int plundFail   = p.optInt("plundersFailed", 0);
          int atkOk       = p.optInt("attacksSuccess", 0);
          int atkFail     = p.optInt("attacksFailed", 0);
          int heroes      = p.optInt("heroesReceived", 0);

          Color rowColor = (placement == 1)
              ? new Color(1f, 0.90f, 0.30f, 1f)  // gold for winner
              : Color.WHITE;

          addCell(table, ordinal(placement), colPlace,  rowColor, false);
          addCell(table, truncate(name, 10), colName,   rowColor, true);
          addCell(table, String.valueOf(roundsOut),     colRounds, rowColor, false);
          addCell(table, plundOk + "/" + plundFail,     colPlund,  rowColor, false);
          addCell(table, atkOk   + "/" + atkFail,       colAtk,    rowColor, false);
          addCell(table, String.valueOf(heroes),         colHero,   rowColor, false);
          table.row();

          if (i < players.length() - 1) {
            Image rowSep = new Image(MyGdxGame.skin.newDrawable("white", DIM_SEP_COLOR));
            table.add(rowSep).colspan(6).growX().height(1f).padTop(2f).padBottom(4f);
            table.row();
          }
        } catch (JSONException e) { /* skip malformed row */ }
      }
    }

    table.pack();

    // Wrap in a ScrollPane in case many players don't fit
    ScrollPane scroll = new ScrollPane(table, MyGdxGame.skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);

    float maxH = 0.70f * MyGdxGame.HEIGHT;
    float scrollH = Math.min(table.getPrefHeight(), maxH);
    float scrollW = table.getPrefWidth();

    scroll.setSize(scrollW, scrollH);
    scroll.setPosition(Math.round(cx - scrollW / 2f),
        Math.round(0.14f * MyGdxGame.HEIGHT + (maxH - scrollH) / 2f));
    stage.addActor(scroll);
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private static void addCell(Table table, String text, float minWidth, Color color, boolean leftAlign) {
    Label lbl = new Label(text, MyGdxGame.skin);
    lbl.setColor(color);
    com.badlogic.gdx.scenes.scene2d.ui.Cell<?> cell = table.add(lbl).minWidth(minWidth).padRight(6f);
    if (leftAlign) cell.left();
    else cell.center();
  }

  private static String ordinal(int n) {
    switch (n) {
      case 1: return "1st";
      case 2: return "2nd";
      case 3: return "3rd";
      default: return n + "th";
    }
  }

  /** Truncate a name that's too long for the column. */
  private static String truncate(String s, int maxChars) {
    if (s == null) return "";
    if (s.length() <= maxChars) return s;
    return s.substring(0, maxChars - 1) + "\u2026"; // ellipsis
  }

  // ── Screen lifecycle ─────────────────────────────────────────────────────────

  @Override
  public void render(float delta) {
    Gdx.gl.glClearColor(0.55f, 0.73f, 0.55f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    stage.act(delta);
    stage.draw();
  }

  @Override
  public void resize(int width, int height) {
    if (stage != null) stage.getViewport().update(width, height, true);
  }

  @Override public void pause()  {}
  @Override public void resume() {}

  @Override
  public void hide() {
    if (stage != null) { stage.dispose(); stage = null; }
  }

  @Override
  public void dispose() {
    if (stage != null) { stage.dispose(); stage = null; }
  }
}
