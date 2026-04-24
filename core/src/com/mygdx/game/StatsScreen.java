package com.mygdx.game;

import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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
  private final JSONArray log;
  private Stage stage;
  private Texture bgTexture;
  private int activeTab = 0; // 0=General, 1=Players, 2=History

  public StatsScreen(Game game, SocketClient socket, JSONObject stats, JSONArray log) {
    super(game);
    this.socket = socket;
    this.stats  = stats;
    this.log    = (log != null) ? log : new JSONArray();
  }

  @Override
  public void show() {
    if (MyGdxGame.onMenuScreenActive != null) MyGdxGame.onMenuScreenActive.run();
    stage = new Stage(new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT));
    Gdx.input.setInputProcessor(stage);
    if (bgTexture == null) bgTexture = new Texture(Gdx.files.internal("data/graphics/bg_darkmoon.jpg"));
    Image bg = new Image(bgTexture);
    bg.setFillParent(true);
    stage.addActor(bg);
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
    Label historyTab = new Label("History", MyGdxGame.skin);
    generalTab.pack();
    playersTab.pack();
    historyTab.pack();

    float tabGap    = 28f;
    float tabsWidth = generalTab.getWidth() + tabGap + playersTab.getWidth() + tabGap + historyTab.getWidth();
    float tabY      = 0.855f * MyGdxGame.HEIGHT;
    float underlineH = 3f;

    generalTab.setPosition(Math.round(cx - tabsWidth / 2f), tabY);
    playersTab.setPosition(Math.round(cx - tabsWidth / 2f + generalTab.getWidth() + tabGap), tabY);
    historyTab.setPosition(Math.round(cx - tabsWidth / 2f + generalTab.getWidth() + tabGap + playersTab.getWidth() + tabGap), tabY);

    generalTab.setColor(activeTab == 0 ? ACTIVE_COLOR : INACTIVE_COLOR);
    playersTab.setColor(activeTab == 1 ? ACTIVE_COLOR : INACTIVE_COLOR);
    historyTab.setColor(activeTab == 2 ? ACTIVE_COLOR : INACTIVE_COLOR);
    generalTab.setTouchable(Touchable.disabled);
    playersTab.setTouchable(Touchable.disabled);
    historyTab.setTouchable(Touchable.disabled);

    Label activeTabLabel = (activeTab == 0) ? generalTab : (activeTab == 1) ? playersTab : historyTab;
    Image underline = new Image(MyGdxGame.skin.newDrawable("white", UNDERLINE_COLOR));
    underline.setSize(activeTabLabel.getWidth(), underlineH);
    underline.setPosition(activeTabLabel.getX(), activeTabLabel.getY() - underlineH - 2f);

    // Invisible hit actors for generous tap targets
    com.badlogic.gdx.scenes.scene2d.Actor generalHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    generalHit.setBounds(generalTab.getX() - 8f, tabY - 8f,
        generalTab.getWidth() + 16f, generalTab.getHeight() + 16f);
    generalHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        activeTab = 0; show();
      }
    });

    com.badlogic.gdx.scenes.scene2d.Actor playersHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    playersHit.setBounds(playersTab.getX() - 8f, tabY - 8f,
        playersTab.getWidth() + 16f, playersTab.getHeight() + 16f);
    playersHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        activeTab = 1; show();
      }
    });

    com.badlogic.gdx.scenes.scene2d.Actor historyHit = new com.badlogic.gdx.scenes.scene2d.Actor();
    historyHit.setBounds(historyTab.getX() - 8f, tabY - 8f,
        historyTab.getWidth() + 16f, historyTab.getHeight() + 16f);
    historyHit.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        activeTab = 2; show();
      }
    });

    stage.addActor(generalHit);
    stage.addActor(playersHit);
    stage.addActor(historyHit);
    stage.addActor(underline);
    stage.addActor(generalTab);
    stage.addActor(playersTab);
    stage.addActor(historyTab);

    // ── Tab content ──────────────────────────────────────────────────────────
    if (activeTab == 0) {
      buildGeneralTab(cx);
    } else if (activeTab == 1) {
      buildPlayersTab(cx);
    } else {
      buildHistoryTab(cx);
    }

    // ── Return to Lobby button ───────────────────────────────────────────────
    TextButton returnBtn = new TextButton("Return to Lobby", MyGdxGame.skin);
    returnBtn.setSize(returnBtn.getPrefWidth() + 20, returnBtn.getPrefHeight());
    float btnRowY = Math.round(0.055f * MyGdxGame.HEIGHT);
    if (activeTab == 2) {
      // Side-by-side with Copy log: return on the right of centre
      returnBtn.setPosition(Math.round(cx + 6f), btnRowY);
    } else {
      returnBtn.setPosition(Math.round(cx - returnBtn.getWidth() / 2f), btnRowY);
    }
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
    float colPlace    = 28f;
    float colName     = 90f;
    float colRounds   = 44f;
    float colPlund    = 52f;
    float colAtk      = 52f;
    float colDefeated = 52f;
    float colKing     = 44f;
    float colMobilise = 52f;
    float colHero     = 46f;

    // Header row
    Color hc = HEADER_COLOR;
    addCell(table, "#",        colPlace,    hc, false);
    addCell(table, "Name",     colName,     hc, true);
    addCell(table, "Rounds",   colRounds,   hc, false);
    addCell(table, "Loots", colPlund,    hc, false);
    addCell(table, "Attacks",  colAtk,      hc, false);
    addCell(table, "Defeated", colDefeated, hc, false);
    addCell(table, "King",     colKing,     hc, false);
    addCell(table, "T/P",      colMobilise, hc, false);
    addCell(table, "Heroes",   colHero,     hc, false);
    table.row();

    // Header separator
    Image hSep = new Image(MyGdxGame.skin.newDrawable("white", SEP_COLOR));
    table.add(hSep).colspan(9).growX().height(1f).padBottom(5f).padTop(3f);
    table.row();

    // Sub-header hint row
    addCell(table, "",      colPlace,    INACTIVE_COLOR, false);
    addCell(table, "",      colName,     INACTIVE_COLOR, true);
    addCell(table, "",      colRounds,   INACTIVE_COLOR, false);
    addCell(table, "(S/F)", colPlund,    INACTIVE_COLOR, false);
    addCell(table, "(S/F)", colAtk,      INACTIVE_COLOR, false);
    addCell(table, "",      colDefeated, INACTIVE_COLOR, false);
    addCell(table, "",      colKing,     INACTIVE_COLOR, false);
    addCell(table, "(T/P)", colMobilise, INACTIVE_COLOR, false);
    addCell(table, "",      colHero,     INACTIVE_COLOR, false);
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
          int plundOk     = p.optInt("lootsSuccess", 0);
          int plundFail   = p.optInt("lootsFailed", 0);
          int atkOk       = p.optInt("attacksSuccess", 0);
          int atkFail     = p.optInt("attacksFailed", 0);
          int defeated    = p.optInt("defeated", 0);
          int kingUsed    = p.optInt("kingUsed", 0);
          int takeActs    = p.optInt("takeActions", 0);
          int putActs     = p.optInt("putActions", 0);
          int heroes      = p.optInt("heroesReceived", 0);

          Color rowColor = (placement == 1)
              ? new Color(1f, 0.90f, 0.30f, 1f)  // gold for winner
              : Color.WHITE;

          addCell(table, ordinal(placement),        colPlace,    rowColor, false);
          addCell(table, truncate(name, 10),         colName,     rowColor, true);
          addCell(table, String.valueOf(roundsOut),  colRounds,   rowColor, false);
          addCell(table, plundOk + "/" + plundFail,  colPlund,    rowColor, false);
          addCell(table, atkOk   + "/" + atkFail,    colAtk,      rowColor, false);
          addCell(table, String.valueOf(defeated),   colDefeated, rowColor, false);
          addCell(table, String.valueOf(kingUsed),   colKing,     rowColor, false);
          addCell(table, takeActs + "/" + putActs,   colMobilise, rowColor, false);
          addCell(table, String.valueOf(heroes),     colHero,     rowColor, false);
          table.row();

          if (i < players.length() - 1) {
            Image rowSep = new Image(MyGdxGame.skin.newDrawable("white", DIM_SEP_COLOR));
            table.add(rowSep).colspan(9).growX().height(1f).padTop(2f).padBottom(4f);
            table.row();
          }
        } catch (JSONException e) { /* skip malformed row */ }
      }
    }

    table.pack();

    // Wrap in a ScrollPane — horizontal only (table always fits vertically)
    ScrollPane scroll = new ScrollPane(table, MyGdxGame.skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(false, true);

    float maxW    = 0.96f * MyGdxGame.WIDTH;
    float scrollH = table.getPrefHeight();
    float scrollW = Math.min(table.getPrefWidth(), maxW);

    scroll.setSize(scrollW, scrollH);
    scroll.setPosition(Math.round(cx - scrollW / 2f),
        Math.round(0.4875f * MyGdxGame.HEIGHT - scrollH / 2f));
    stage.addActor(scroll);
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────
  private void buildHistoryTab(float cx) {
    float contentTop    = 0.825f * MyGdxGame.HEIGHT;
    float contentBottom = 0.19f  * MyGdxGame.HEIGHT;
    float contentH = contentTop - contentBottom;
    float contentW = 0.92f * MyGdxGame.WIDTH;

    final StringBuilder logText = new StringBuilder();

    Table inner = new Table();
    inner.top().left().pad(6f);

    if (log.length() == 0) {
      Label empty = new Label("No history yet.", MyGdxGame.skin);
      empty.setColor(INACTIVE_COLOR);
      inner.add(empty).row();
    } else {
      try {
        for (int i = 0; i < log.length(); i++) {
          JSONObject entry = log.getJSONObject(i);
          String text    = entry.optString("text", "");
          boolean neutral = entry.optBoolean("neutral", false);
          boolean success = entry.optBoolean("success", true);
          if (logText.length() > 0) logText.append("\n");
          logText.append(text);
          Label lbl = new Label(text, MyGdxGame.skin);
          lbl.setWrap(true);
          Color lc = neutral
              ? new Color(0.85f, 0.85f, 0.85f, 1f)
              : (success ? new Color(0.3f, 0.95f, 0.3f, 1f) : new Color(0.95f, 0.3f, 0.25f, 1f));
          lbl.setColor(lc);
          inner.add(lbl).left().padBottom(4f).width(contentW - 24f).row();
        }
      } catch (JSONException e) { e.printStackTrace(); }
    }

    // Dark background behind history list for readability
    Image historyBg = new Image(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.55f)));
    historyBg.setSize(contentW, contentH);
    historyBg.setPosition(Math.round(cx - contentW / 2f), Math.round(contentBottom));
    stage.addActor(historyBg);

    ScrollPane scroll = new ScrollPane(inner, MyGdxGame.skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    scroll.setSize(contentW, contentH);
    scroll.setPosition(Math.round(cx - contentW / 2f), Math.round(contentBottom));
    scroll.layout();
    scroll.setScrollPercentY(1f);
    stage.addActor(scroll);

    // "Copy log" button — copies all history entries to the system clipboard
    // Placed to the left of the centred Return to Lobby button (which sits right of centre)
    TextButton copyBtn = new TextButton("Copy log", MyGdxGame.skin);
    copyBtn.setSize(copyBtn.getPrefWidth() + 20, copyBtn.getPrefHeight());
    copyBtn.setPosition(Math.round(cx - copyBtn.getWidth() - 6f),
        Math.round(0.055f * MyGdxGame.HEIGHT));
    copyBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        Gdx.app.getClipboard().setContents(logText.toString());
      }
    });
    stage.addActor(copyBtn);
  }

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
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
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
    if (bgTexture != null) { bgTexture.dispose(); bgTexture = null; }
  }
}
