
package com.mygdx.game;
import com.mygdx.game.util.JSONObject;
import com.mygdx.game.util.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.PickingDeck;
import java.util.Iterator;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.BatteryTower;
import com.mygdx.game.heroes.FortifiedTower;
import com.mygdx.game.heroes.Magician;
import com.mygdx.game.heroes.Marshal;
import com.mygdx.game.heroes.Merchant;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Reservists;
import com.mygdx.game.heroes.Priest;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.heroes.Warlord;
import com.mygdx.game.listeners.EnemyDefCardListener;
import com.mygdx.game.listeners.EnemyHandCardListener;
import com.mygdx.game.listeners.EnemyKingCardListener;
import com.mygdx.game.listeners.FinishTurnButtonListener;
import com.mygdx.game.listeners.KeepCardButtonListener;
import com.mygdx.game.listeners.MercenaryImageListener;
import com.mygdx.game.listeners.OwnDefCardListener;
import com.mygdx.game.listeners.EnemyPlaceholderListener;
import com.mygdx.game.listeners.OwnHandCardListener;
import com.mygdx.game.listeners.OwnHeroListener;
import com.mygdx.game.listeners.PickingDeckListener;
import com.mygdx.game.listeners.OwnKingCardListener;
import com.mygdx.game.listeners.OwnPlaceholderListener;
import com.mygdx.game.listeners.SabotagedImageListener;
import com.mygdx.game.listeners.TradeCardButtonListener;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.net.SocketListener;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.heroes.HeroDescriptions;

//public class GameScreen extends AbstractScreen {
public class GameScreen extends ScreenAdapter {

  private GameState gameState;

  // screen objects
  InputMultiplexer inMulti;
  InputProcessor inProTop;
  InputProcessor inProBottom;
  private FitViewport fitVPGame;
  private FitViewport fitVPHand;
  private FitViewport fitVPOverlay;
  private Stage gameStage;
  private Stage handStage;
  private Stage overlayStage;
  private Image gameBck;
  private Image handBck;
  private Image handHighlight;
  private Texture plainWhiteTexture;
  private Texture texGameBck;
  private Texture texHandBck;
  private Label myPlayerLabel;
  private Label roundCounter;
  private TextButton finishTurnButton;
  // Cache of avatar textures loaded during gameplay, keyed by icon name.
  private final Map<String, Texture> gameAvatarTextures = new HashMap<String, Texture>();

  // game objects
  private Player currentPlayer;
  private ArrayList<Player> players;

  // all listeners
  // gameStage own objects
  private OwnDefCardListener ownDefCardListener;
  private OwnKingCardListener ownKingCardListener;
  private OwnPlaceholderListener ownPlaceholderListener;
  private SabotagedImageListener sabotagedImageListener;
  private MercenaryImageListener mercenaryImageListener;

  // gameStage enemy objects
  private EnemyDefCardListener enemyDefCardListener;
  private EnemyKingCardListener enemyKingCardListener;
  private EnemyHandCardListener enemyHandCardListener;
  private EnemyPlaceholderListener enemyPlaceholderListener;

  // handStage
  private OwnHandCardListener ownHandCardListener;
  private OwnHeroListener ownHeroListener;
  private boolean isDraggingDefCard = false;
  private boolean isDraggingHandCard = false;
  private Card dragOverlayCard = null;
  private KeepCardButtonListener keepCardButtonListener;
  private TradeCardButtonListener tradeCardButtonListener;
  private FinishTurnButtonListener finishTurnButtonListener;

  // Merchant 2nd-try reveal: card ID shown to all non-trading players
  private int merchantRevealCardId = -1;
  private int merchantRevealPlayerIdx = -1;

  private int playerIndex;
  private boolean isSpectator = false;
  // Set to true when navigating away; prevents stale socket listeners from acting.
  private boolean screenDisposed = false;
  private JSONObject centralizedState;
  private SocketClient socket;
  private Game game;
  private boolean menuOpen = false;
  private boolean logOpen = false;
  private InputMultiplexer menuAndGameMulti;
  // Battery Tower: stored when this local player is the defender and must allow/deny
  private JSONObject pendingBatteryDefCheck = null;
  // Attack preview broadcast: set from stateUpdate when another player has a pending attack
  private JSONObject pendingAttackBroadcast = null;
  // Loot preview broadcast: set from stateUpdate when another player has a pending loot
  private JSONObject pendingLootBroadcast = null;
  // Pending hero selection after a successful king defeat (attacker must choose one hero)
  private java.util.ArrayList<String> pendingKingDefeatHeroOptions = null;
  // Hero auction: server-authoritative state (non-null when auction is in progress)
  private JSONObject pendingHeroAuction = null;
  // Auction bid card selections (bidder's local toggle state)
  private java.util.Set<Integer> auctionBidHandCardIds = new java.util.HashSet<Integer>();
  private java.util.Set<Integer> auctionBidDefCardIds = new java.util.HashSet<Integer>();
  // Sell-hero setup: hero name player wants to sell (__SELECT__ = hero choice pending), null = inactive
  private String auctionSellHeroName = null;
  private int auctionSellMinBid = 1;
  // Battery Tower: card IDs revealed to the defender after they allow or deny
  private JSONArray pendingBatteryResultCards = null;
  // Battery Tower: shown briefly to the ATTACKER when the bot defender auto-responds
  // Null = no notification; non-null = message to display (auto-dismisses after 2 s)
  private String batteryBotNotification = null;
  private float batteryBotNotificationTimer = 0f;
  // Set when the current player ended their turn without attacking -- they must expose a defense card.
  private boolean pendingExposeCard = false;
  // Static reference to the live GameScreen so listeners (e.g. OwnDefCardListener)
  // can submit a covered-card-expose tap without us threading a parameter through
  // every constructor call site. Set in show(), cleared in hide().
  private static GameScreen INSTANCE = null;
  public static GameScreen getInstance() { return INSTANCE; }
  public boolean isZoomModeActive() { return zoomModeActive; }
  public boolean isSpectator() { return isSpectator; }
  // Tutorial mode: guided overlay steps for new players
  private boolean isTutorial = false;
  private int tutorialStep = 0;
  private int tutorialDefenseBaseline = -1; // defense card count when DEFENSE step started; -1 = unset
  // Issue #171: hero-specific interactive tutorial. Independent state machine
  // running alongside (but mutually exclusive with) the basic tutorial.
  private boolean isHeroTutorial = false;
  private String heroTutorialName = null;
  private int heroTutorialStep = 0;
  private TutorialStepDef[] heroTutorialSteps = null;
  // Tracks the previous frame's currentPlayerIndex so we can detect a turn flip
  // from bot back to player and fire the MY_TURN_START hook exactly once.
  private int heroTutorialPrevPlayerIdx = -1;
  private JSONArray activityLog = new JSONArray();
  // Log overlay live-update state
  private ScrollPane logScrollPane = null;
  private Table logInnerTable = null;
  private int logLastRenderedCount = 0;
  // Chat state
  private boolean chatOpen = false;
  private final java.util.ArrayList<String[]> chatMessages = new java.util.ArrayList<String[]>(); // {"name", "text"}
  private ScrollPane chatScrollPane = null;
  private Table chatInnerTable = null;
  private int unreadChatMessages = 0;
  private Label chatBadgeLabel = null;
  private Label logBadgeLabel = null;
  // Emit Reservists count to other clients once on first render (before any stateUpdate fires)
  private boolean initialReservistsBroadcastDone = false;

  // ── Manual setup phase ────────────────────────────────────────────────────
  // -1 = not yet selected; >= 1 = card ID of the chosen king
  private int setupSelectedKingId = -1;
  // IDs of the 3 selected defense cards (-1 = not yet selected)
  private final int[] setupSelectedDefIds = { -1, -1, -1 };
  // True after the player has clicked Confirm (waiting for others to finish)
  private boolean setupSubmitted = false;
  // Safety-net: while waiting for others to submit setup, periodically request a state resync
  // so the screen self-heals if the final stateUpdate (setupPhase=false) was missed.
  private float setupWaitTimer = 0f;
  // Heartbeat resync: during active gameplay, request a full state resync if no stateUpdate
  // has been received for 30 seconds. Recovers clients that silently get out of sync.
  private float syncHeartbeatTimer = 0f;
  private final ArrayList<Integer> setupKeepIds = new ArrayList<Integer>();

  // Textures cached once to avoid leaking a new Texture on every show() call
  private Texture texMercenary;
  private Texture texSabotaged;
  private Texture texHearts;
  private Texture texDiamondsRed;
  private Texture texHeartsRed;
  private Texture texDiamonds;
  private Texture texClubs;
  private Texture texSpades;
  private Texture texSomeSymbol;
  private Texture texShieldCheck;
  private Texture texArrowDownShield;
  private Texture texMenuButton;
  private Texture texChatIcon;
  private Texture texHistoryIcon;

  // Card zoom (issue #218)
  private static final float CARD_ZOOM = 1.7f;
  private Card currentlyZoomedCard = null;
  private PickingDeck currentlyZoomedDeck = null;
  private final HashSet<PickingDeck> deckZoomAttached = new HashSet<PickingDeck>();
  // Zoom-mode toggle (issue #246)
  private boolean zoomModeActive = false;
  private Texture texZoomButton;
  private Image zoomModeBtn;


  // New constructor for centralized state
  public GameScreen(Game game, JSONObject centralizedState, int playerIndex, SocketClient socket) {
    this(game, centralizedState, playerIndex, socket, "None");
  }

  private String startingHero = "None";

  public GameScreen(Game game, JSONObject centralizedState, int playerIndex, SocketClient socket, String startingHero) {
    this.game = game;
    this.startingHero = startingHero;
    this.socket = socket;
    // playerIndex == -1 means spectator — display from player 0's viewpoint, read-only
    this.isSpectator = (playerIndex < 0);
    this.playerIndex = this.isSpectator ? 0 : playerIndex;
    this.centralizedState = centralizedState;

    // Build all game state from the server-provided authoritative state
    gameState = new GameState(centralizedState);
    gameState.setSocket(socket);
    players = gameState.getPlayers();
    currentPlayer = players.get(this.playerIndex);
    this.isTutorial = centralizedState.optBoolean("isTutorial", false);
    // Issue #171: hero tutorial detection — overrides the basic tutorial flow.
    String htn = centralizedState.optString("heroTutorialName", null);
    if (htn != null && !htn.isEmpty() && !"null".equals(htn)) {
      this.heroTutorialName = htn;
      this.heroTutorialSteps = HeroTutorialSteps.forHero(htn);
      if (this.heroTutorialSteps != null) {
        this.isHeroTutorial = true;
        this.isTutorial = false; // hero tutorial owns the overlay; basic flow disabled
      }
    }

    // Single stateUpdate listener — replaces all specific sync events
    final int notifyPlayerIdx = this.playerIndex; // capture field before parameter shadows it
    socket.on("stateUpdate", new SocketListener() {
      @Override
      public void call(Object... args) {
        if (screenDisposed) return;
        final JSONObject data = (JSONObject) args[0];
        // Fire turn notification here, NOT inside applyStateUpdate/postRunnable.
        // postRunnable runs on the render thread which is paused when the tab is hidden
        // (requestAnimationFrame throttling). WebSocket callbacks always fire.
        try {
          int newIdx = data.getInt("currentPlayerIndex");
          int prevIdx = gameState.getCurrentPlayerIndex();
          if (prevIdx != newIdx) {
            if (!isSpectator && newIdx == notifyPlayerIdx) {
              MyGdxGame.turnNotifier.notifyYourTurn(currentPlayer.getPlayerName());
            } else {
              // Turn moved to someone else — clear any active title flash
              MyGdxGame.turnNotifier.clearNotification();
            }
          }
        } catch (JSONException e) { /* ignore malformed packet */ }
        syncHeartbeatTimer = 0f;
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            applyStateUpdate(data);
            gameState.setUpdateState(true);
          }
        });
      }
    });

    // Restart listener: server sends a fresh gameState when a new game begins
    final Game theGame = game;
    final SocketClient theSocket = socket;
    final String theStartingHero = startingHero;
    socket.on("gameState", new SocketListener() {
      @Override
      public void call(Object... args) {
        if (screenDisposed) return;
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int newPlayerIndex = data.getInt("playerIndex");
              JSONObject newState = data.getJSONObject("gameState");
              theGame.setScreen(new GameScreen(theGame, newState, newPlayerIndex, theSocket, theStartingHero));
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
      }
    });

    // Another player acquired a hero — apply it to the local game state.
    final int myPlayerIndex = playerIndex;
    socket.on("heroAcquired", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int pIdx = data.getInt("playerIndex");
              String heroName = data.getString("heroName");
              if (pIdx != myPlayerIndex) {
                gameState.applyHeroAcquired(pIdx, heroName);
              }
              gameState.setUpdateState(true);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });

    // A joker draw landed on a hero that was already owned — the owner loses the hero.
    // Per issue #25: the drawing player gets nothing; the previous owner loses the hero.
    socket.on("heroLost", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int pIdx = data.getInt("playerIndex");
              String heroName = data.getString("heroName");
              if (pIdx >= 0 && pIdx < players.size()) {
                players.get(pIdx).removeHeroByName(heroName);
              }
              gameState.setUpdateState(true);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });

    // Server notifies us that one of our deployed saboteurs was destroyed by the enemy.
    // Mark it as destroyed (state 2) so the recovery clock starts.
    socket.on("saboteurDestroyed", new SocketListener() {
      @Override
      public void call(Object... args) {
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            for (Hero h : currentPlayer.getHeroes()) {
              if ("Saboteurs".equals(h.getHeroName())) {
                ((Saboteurs) h).destroy();
                break;
              }
            }
            gameState.setUpdateState(true);
          }
        });
      }
    });

    // Handle incoming mercenary defense boost from another client
    // Spy flip relay: another player used spy to reveal one of our defense cards
    socket.on("spyFlip", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int tIdx = data.getInt("targetPlayerIdx");
              int slot = data.getInt("slot");
              int level = data.getInt("level");
              Player p = gameState.getPlayers().get(tIdx);
              if (slot == -1) {
                // Spy flipped the king card
                if (p.getKingCard() != null) p.getKingCard().setCovered(false);
              } else {
                Map<Integer, Card> cards = (level == 0) ? p.getDefCards() : p.getTopDefCards();
                Card c = cards.get(slot);
                if (c != null) c.setCovered(false);
              }
              gameState.setUpdateState(true);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });

    socket.on("batteryDefenseCheck", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int targetIdx = data.getInt("targetPlayerIdx");
              // Only the targeted defender shows the Allow/Deny UI
              if (targetIdx == myPlayerIndex) {
                Player me = gameState.getPlayers().get(myPlayerIndex);
                BatteryTower bt = null;
                for (int i = 0; i < me.getHeroes().size(); i++) {
                  if (me.getHeroes().get(i) instanceof BatteryTower) {
                    bt = (BatteryTower) me.getHeroes().get(i);
                    break;
                  }
                }
                if (bt != null && bt.getCharges() > 0) {
                  pendingBatteryDefCheck = data;
                  gameState.setUpdateState(true);
                } else {
                  // No charges — auto-allow
                  try {
                    JSONObject allow = new JSONObject();
                    allow.put("attackerIdx", data.getInt("attackerIdx"));
                    theSocket.emit("batteryAllowAttack", allow);
                  } catch (JSONException ex) { ex.printStackTrace(); }
                }
              }
            } catch (JSONException e) { e.printStackTrace(); }
          }
        });
      }
    });

    socket.on("batteryAllowAttack", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int attackerIdx = data.getInt("attackerIdx");
              if (attackerIdx == myPlayerIndex) {
                int defIdx2 = data.optInt("targetPlayerIdx", -1);
                String defName = (defIdx2 >= 0 && defIdx2 < gameState.getPlayers().size())
                    ? gameState.getPlayers().get(defIdx2).getPlayerName() : "Defender";
                batteryBotNotification = defName + " (Battery Tower): attack allowed";
                batteryBotNotificationTimer = 2f;
                PlayerTurn pt = gameState.getCurrentPlayer().getPlayerTurn();
                // Reveal the cards now that the attack is allowed
                if (pt.isAttackTargetIsKing()) {
                  int defIdx = pt.getAttackTargetPlayerIdx();
                  if (defIdx >= 0) {
                    Card kc = gameState.getPlayers().get(defIdx).getKingCard();
                    if (kc != null) kc.setCovered(false);
                  }
                } else {
                  for (Card dc : pt.getPendingAttackDefCards()) dc.setCovered(false);
                }
                pt.setBatteryWaiting(false);
                // Emit attack preview now that battery has allowed — the defender can see the battle
                if (!pt.isAttackTargetIsKing()) {
                  try {
                    JSONObject previewData = new JSONObject();
                    previewData.put("attackerIdx", myPlayerIndex);
                    previewData.put("defenderIdx", pt.getAttackTargetPlayerIdx());
                    previewData.put("positionId", pt.getAttackTargetPositionId());
                    previewData.put("level", pt.getAttackTargetLevel());
                    JSONArray atkPrevIds = new JSONArray();
                    for (Card c : pt.getPendingAttackCards()) atkPrevIds.put(c.getCardId());
                    previewData.put("attackCardIds", atkPrevIds);
                    JSONArray ownDefPrevIds = new JSONArray();
                    for (Card c : pt.getPendingAttackOwnDefCards()) ownDefPrevIds.put(c.getCardId());
                    previewData.put("ownDefCardIds", ownDefPrevIds);
                    JSONArray defPrevIds = new JSONArray();
                    for (Card dc : pt.getPendingAttackDefCards()) defPrevIds.put(dc.getCardId());
                    previewData.put("defCardIds", defPrevIds);
                    previewData.put("kingUsed", pt.isKingUsed());
                    previewData.put("kingCardId", pt.isKingUsed() && currentPlayer.getKingCard() != null ? currentPlayer.getKingCard().getCardId() : -1);
                    previewData.put("mercenaryBonus", pt.getPendingAttackMercenaryBonus());
                      int defMercBonusBT = 0;
                      for (Card dc : pt.getPendingAttackDefCards()) defMercBonusBT += dc.getBoosted();
                      previewData.put("defMercBonus", defMercBonusBT);
                      JSONArray defBoostsBT = new JSONArray();
                      for (Card dc : pt.getPendingAttackDefCards()) defBoostsBT.put(dc.getBoosted());
                      previewData.put("defCardBoosts", defBoostsBT);
                    previewData.put("reservistBonus", pt.getReservistAttackBonus());
                    previewData.put("success", pt.isAttackSuccess());
                    previewData.put("attackingSymbol", pt.getAttackingSymbol()[0]);
                    previewData.put("attackingSymbol2", pt.getAttackingSymbol()[1]);
                    theSocket.emit("attackPreview", previewData);
                  } catch (JSONException ex) { ex.printStackTrace(); }
                }
                gameState.setUpdateState(true);
              }
            } catch (JSONException e) { e.printStackTrace(); }
          }
        });
      }
    });

    socket.on("batteryDenyAttack", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int attackerIdx = data.getInt("attackerIdx");
              if (attackerIdx == myPlayerIndex) {
                int defIdx3 = data.optInt("targetPlayerIdx", -1);
                String defName3 = (defIdx3 >= 0 && defIdx3 < gameState.getPlayers().size())
                    ? gameState.getPlayers().get(defIdx3).getPlayerName() : "Defender";
                batteryBotNotification = defName3 + " (Battery Tower): attack BLOCKED!";
                batteryBotNotificationTimer = 2.5f;
                PlayerTurn pt = gameState.getCurrentPlayer().getPlayerTurn();
                // Cancel the attack, lock only the cards used in the attack
                pt.setAttackPending(false);
                pt.setBatteryWaiting(false);
                pt.setBatteryDenied(true);
                pt.setAttackTargetIsKing(false);
                // Save denied card IDs (by ID not reference — survives stateUpdate hand rebuild)
                ArrayList<Integer> deniedIds = new ArrayList<Integer>();
                for (Card c : pt.getPendingAttackCards()) deniedIds.add(c.getCardId());
                pt.setBatteryDeniedAttackCardIds(deniedIds);
                pt.getPendingAttackCards().clear();
                pt.getPendingAttackDefCards().clear();
                pt.getPendingAttackOwnDefCards().clear();
                // Re-cover the revealed def cards (attack was denied)
                Player targetP = gameState.getPlayers().get(data.getInt("targetPlayerIdx"));
                int posId = data.getInt("positionId");
                if (posId >= 1) {
                  Card dc = targetP.getDefCards().get(posId);
                  if (dc != null) dc.setCovered(true);
                  Card tdc = targetP.getTopDefCards().get(posId);
                  if (tdc != null) tdc.setCovered(true);
                }
                Card tkc = targetP.getKingCard();
                if (data.optBoolean("isKing", false) && tkc != null) tkc.setCovered(true);
              }
              gameState.setUpdateState(true);
            } catch (JSONException e) { e.printStackTrace(); }
          }
        });
      }
    });

    socket.on("mercDefBoost", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int pIdx = data.getInt("playerIdx");
              int slot = data.getInt("slot");
              int level = data.getInt("level");
              int boosted = data.getInt("boosted");
              Player p = gameState.getPlayers().get(pIdx);
              Card c;
              if (level == -1) {
                // Issue #167: king card boost
                c = p.getKingCard();
              } else {
                Map<Integer, Card> cards = (level == 0) ? p.getDefCards() : p.getTopDefCards();
                c = cards.get(slot);
              }
              if (c != null) {
                // Set boost to the authoritative value from the emitting client
                while (c.getBoosted() > boosted) c.addBoosted(-1);
                while (c.getBoosted() < boosted) c.addBoosted(1);
              }
              gameState.setUpdateState(true);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });

    socket.on("reservistsKingBoost", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject data = (JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int pIdx = data.getInt("playerIdx");
              int count = data.getInt("count");
              gameState.getPlayers().get(pIdx).setReservistsReadyCount(count);
              gameState.setUpdateState(true);
            } catch (JSONException e) { e.printStackTrace(); }
          }
        });
      }
    });

    // Game ended — server sends game statistics; navigate to the stats screen
    socket.on("gameStats", new SocketListener() {
      @Override
      public void call(Object... args) {
        final JSONObject statsJson = (args.length > 0 && args[0] instanceof JSONObject)
            ? (JSONObject) args[0] : new JSONObject();
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            screenDisposed = true;
            theGame.setScreen(new StatsScreen(theGame, theSocket, statsJson, activityLog));
          }
        });
      }
    });

    // Game ended — server tells all clients to return to the lobby
    socket.on("returnToLobby", new SocketListener() {
      @Override
      public void call(Object... args) {
        // MenuScreen's own returnToLobby listener will handle the screen switch;
        // nothing to do here since MenuScreen is still registered on the socket.
      }
    });

    socket.on("chatMessage", new SocketListener() {
      @Override
      public void call(Object... args) {
        if (args.length == 0) return;
        try {
          JSONObject data = (JSONObject) args[0];
          final String name = data.optString("name", "?");
          final String text = data.optString("text", "");
          Gdx.app.postRunnable(new Runnable() {
            @Override public void run() {
              chatMessages.add(new String[]{name, text});
              if (!chatOpen) {
                unreadChatMessages++;
                if (chatBadgeLabel != null) {
                  chatBadgeLabel.setText(unreadChatMessages >= 100 ? "99+" : String.valueOf(unreadChatMessages));
                  chatBadgeLabel.setVisible(true);
                }
              }
              if (chatOpen && chatInnerTable != null) {
                // append new message to the open overlay
                Label lbl = new Label("[" + name + "] " + text, MyGdxGame.skin);
                lbl.setWrap(true);
                lbl.setColor(new Color(0.85f, 0.95f, 1f, 1f));
                chatInnerTable.add(lbl).left().padBottom(4f).expandX().fillX().row();
                chatInnerTable.pack();
                if (chatScrollPane != null) {
                  chatScrollPane.layout();
                  chatScrollPane.setScrollPercentY(1f);
                }
              }
            }
          });
        } catch (Exception e) { e.printStackTrace(); }
      }
    });

    // Initialize stages
    gameStage = new Stage();
    fitVPGame = new FitViewport(MyGdxGame.WIDTH, MyGdxGame.WIDTH);
    gameStage.setViewport(fitVPGame);

    handStage = new Stage();
    fitVPHand = new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT - MyGdxGame.WIDTH);
    handStage.setViewport(fitVPHand);

    overlayStage = new Stage();
    fitVPOverlay = new FitViewport(MyGdxGame.WIDTH, MyGdxGame.HEIGHT);
    overlayStage.setViewport(fitVPOverlay);

    inMulti = new InputMultiplexer();
    inMulti.addProcessor(gameStage);
    inMulti.addProcessor(handStage);
    menuAndGameMulti = new InputMultiplexer();
    menuAndGameMulti.addProcessor(overlayStage);
    menuAndGameMulti.addProcessor(gameStage);
    menuAndGameMulti.addProcessor(handStage);
    // Initial input processor is set by render() each frame.

    // Use a standalone 1×1 white texture (not the atlas "white" region) so that
    // the atlas Linear filter cannot bleed into these full-stage backgrounds.
    Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pix.setColor(Color.WHITE);
    pix.fill();
    plainWhiteTexture = new Texture(pix);
    pix.dispose();

    // Photographic wooden backgrounds for game table and hand area.
    texGameBck = new Texture(Gdx.files.internal("data/graphics/bg_table.jpg"));
    texGameBck.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    texHandBck = new Texture(Gdx.files.internal("data/graphics/bg_hand.jpg"));
    texHandBck.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

    gameBck = new Image(new TextureRegionDrawable(new TextureRegion(texGameBck)));
    gameBck.setFillParent(true);
    // Tapping empty space in the game board unzooms any card/deck (issue #246).
    gameBck.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (currentlyZoomedCard != null) unzoomCard(currentlyZoomedCard);
        if (currentlyZoomedDeck != null) { setDeckScale(currentlyZoomedDeck, 1f); currentlyZoomedDeck = null; }
        return false;
      }
    });
    gameStage.addActor(gameBck);

    // handBck uses a tinted white overlay on top of the photo so that the
    // defence-card-selected highlight (green tint) still works.
    handBck = new Image(new TextureRegionDrawable(new TextureRegion(texHandBck)));
    handBck.setFillParent(true);
    handStage.addActor(handBck);

    // Semi-transparent green overlay; alpha driven in render() when a defence card is selected.
    TextureRegionDrawable whiteDrw = new TextureRegionDrawable(new TextureRegion(plainWhiteTexture));
    handHighlight = new Image(whiteDrw);
    handHighlight.setFillParent(true);
    handHighlight.setColor(0.3f, 0.8f, 0.3f, 0f);
    // Purely visual — must not consume input events that handBck's listener needs.
    handHighlight.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

    // Clicking anywhere in the hand area takes the currently-selected defense card.
    handBck.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (gameState.isSetupPhase()) return;
        Map<Integer, Card> defs = currentPlayer.getDefCards();
        Map<Integer, Card> topDefs = currentPlayer.getTopDefCards();
        for (int jj = 1; jj <= 3; jj++) {
          boolean shouldTake = (defs.containsKey(jj) && defs.get(jj).isSelected())
              || (topDefs.containsKey(jj) && topDefs.get(jj).isSelected());
          if (shouldTake && currentPlayer.canTakeDefCard()) {
            emitTakeDefCard(jj);
            currentPlayer.takeDefCard(jj);
          }
        }
        // After taking, deselect all hand cards so the SELECT tutorial step doesn't
        // immediately auto-advance (the moved Card object retains its selected state).
        for (Card c : currentPlayer.getHandCards()) { c.setSelected(false); }
        gameState.setUpdateState(true);
      }
    });

    texMercenary  = new Texture(Gdx.files.internal("data/skins/whitepawn.png"));
    texSabotaged  = new Texture(Gdx.files.internal("data/skins/sabotaged.png"));
    texHearts     = new Texture(Gdx.files.internal("data/skins/hearts.png"));
    texHeartsRed  = new Texture(Gdx.files.internal("data/skins/hearts_red.png"));
    texDiamonds   = new Texture(Gdx.files.internal("data/skins/diamonds.png"));
    texDiamondsRed = new Texture(Gdx.files.internal("data/skins/diamonds_red.png"));
    texClubs      = new Texture(Gdx.files.internal("data/skins/clubs.png"));
    texSpades     = new Texture(Gdx.files.internal("data/skins/spades.png"));
    texSomeSymbol = new Texture(Gdx.files.internal("data/skins/someSymbol.png"));
    texShieldCheck     = new Texture(Gdx.files.internal("data/skins/shield-check-f.png"));
    texArrowDownShield = new Texture(Gdx.files.internal("data/skins/arrow-down-shield.png"));
    texMenuButton = new Texture(Gdx.files.internal("data/graphics/options.png"));
    texChatIcon    = new Texture(Gdx.files.internal("data/graphics/chat.png"));
    texHistoryIcon = new Texture(Gdx.files.internal("data/graphics/history.png"));
    texZoomButton  = createMagnifierTexture(44);

    // Request authoritative state from server. This handles the case where the browser
    // tab was inactive during game initialization: requestAnimationFrame is paused for
    // inactive tabs, so postRunnable tasks (including this constructor) are deferred.
    // Any stateUpdate events broadcast while the constructor was queued are missed
    // because the socket listener was not yet registered. Requesting a sync here
    // ensures this client gets the current authoritative state immediately.
    socket.emit("requestStateSync", new JSONObject());
  }

  @Override
  public void show() {
    INSTANCE = this;
    MyGdxGame.setMusicTrack(null); // no music during the game
    if (MyGdxGame.onGameScreenActive != null) MyGdxGame.onGameScreenActive.run();

    players = gameState.getPlayers();
    // Spectators always follow the player whose turn it currently is.
    if (isSpectator) {
      currentPlayer = gameState.getCurrentPlayer();
    }

    gameStage.clear();
    handStage.clear();
    if (!logOpen && !chatOpen) overlayStage.clear();

    // Reset card zoom state (cards are reused between show() calls; scale persists)
    if (currentlyZoomedCard != null) {
      currentlyZoomedCard.setScale(1f);
      currentlyZoomedCard = null;
    }
    for (PickingDeck pd : gameState.getPickingDecks()) {
      for (Card pdCard : pd.getCards()) pdCard.setScale(1f);
    }

    gameStage.addActor(gameBck);
    handStage.addActor(handBck);
    handStage.addActor(handHighlight);

    // Manual setup phase: show card-selection UI instead of the normal game board
    if (gameState.isSetupPhase()) {
      showSetupPhaseScreen();
      return;
    }

    // On first render after setup phase (or on first normal game render), broadcast Reservists.
    if (!initialReservistsBroadcastDone) {
      initialReservistsBroadcastDone = true;
      for (Hero h : currentPlayer.getHeroes()) {
        if ("Reservists".equals(h.getHeroName())) {
          emitReservistsKingBoost(((Reservists) h).countReady());
          break;
        }
      }
    }

    showGameStage(players, currentPlayer);
    showHandStage(players, currentPlayer);

    // Auto-advance tutorial steps that are gated on game-state conditions rather than actions
    if (isTutorial && tutorialStep >= 0) {
      // Step 6: advance once it's the player's turn again (bot finished)
      if (tutorialStep == TUTORIAL_STEP_WAITING && gameState.getCurrentPlayerIndex() == playerIndex) {
        tutorialStep = TUTORIAL_STEP_INFO_EXPOSE;
      }
    }
    // Issue #171: hero-tutorial — fire MY_TURN_START hook when control flips
    // back from the bot to the player.
    if (isHeroTutorial && heroTutorialStep >= 0) {
      int curIdx = gameState.getCurrentPlayerIndex();
      if (heroTutorialPrevPlayerIdx != -1
          && heroTutorialPrevPlayerIdx != playerIndex
          && curIdx == playerIndex) {
        tutorialAdvanceHook("MY_TURN_START");
      }
      heroTutorialPrevPlayerIdx = curIdx;
    }

    if (menuOpen && logOpen) {
      refreshLogOverlay();
    } else if (menuOpen && chatOpen) {
      // chat overlay updates itself via postRunnable — nothing to rebuild here
    } else if (menuOpen) {
      buildMenuOverlay();
    } else {
      addMenuButtonToOverlay();
      if (isTutorial && tutorialStep >= 0) {
        buildTutorialOverlay();
      } else if (isHeroTutorial && heroTutorialStep >= 0) {
        buildHeroTutorialOverlay();
      }
    }
  }

  /**
   * Shows the interactive manual setup phase UI.
   *
   * Phase 1: player taps a card to designate it as king (highlighted in gold).
   * Phase 2: player taps up to 3 more cards as defense cards (highlighted in green).
   * Phase 3: a Confirm button appears once all 4 cards are chosen.
   * After Confirm: show "Waiting for other players…" until setup completes for everyone.
   */
  private void showSetupPhaseScreen() {
    float cx = MyGdxGame.WIDTH / 2f;
    float gameH = MyGdxGame.WIDTH;   // gameStage is square
    float handH = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;

    Card infoCard = new Card();
    final float cardW = infoCard.getDefWidth() * 1.6f;
    final float cardH = infoCard.getDefHeight() * 1.6f;

    // ── Status label ────────────────────────────────────────────────────────
    int _defCount = 0;
    for (int id : setupSelectedDefIds) if (id != -1) _defCount++;
    final int allHandSize = currentPlayer.getHandCards().size();
    // keepPhase: after king+3def chosen, player must pick exactly 2 hand cards to keep (if any extra)
    final int extraCards = allHandSize - 4; // cards remaining in hand that aren't king/def
    final boolean needKeepPhase = (extraCards > 2);
    final boolean inKeepPhase = (setupSelectedKingId != -1 && _defCount == 3 && needKeepPhase);
    String statusText;
    if (setupSubmitted) {
      Map<Integer, Boolean> ssMap = gameState.getSetupSubmittedMap();
      ArrayList<String> pending = new ArrayList<String>();
      for (int i = 0; i < players.size(); i++) {
        if (i == playerIndex) continue; // we know we submitted even before server echoes it
        if (!Boolean.TRUE.equals(ssMap.get(i))) {
          pending.add(players.get(i).getPlayerName());
        }
      }
      if (pending.isEmpty()) {
        statusText = "All ready, starting...";
      } else {
        StringBuilder sb = new StringBuilder("Waiting for: ");
        for (int wi = 0; wi < pending.size(); wi++) {
          if (wi > 0) sb.append(", ");
          sb.append(pending.get(wi));
        }
        statusText = sb.toString();
      }
    } else if (setupSelectedKingId == -1) {
      statusText = "Select your king card";
    } else if (_defCount < 3) {
      statusText = "Select defense card " + (_defCount + 1) + " of 3";
    } else if (inKeepPhase) {
      int stillNeeded = 2 - setupKeepIds.size();
      if (stillNeeded > 0) {
        statusText = "Select " + stillNeeded + " hand card" + (stillNeeded > 1 ? "s" : "") + " to keep";
      } else {
        statusText = "Tap Confirm to start";
      }
    } else {
      statusText = "Tap Confirm to start";
    }
    Label statusLabel = new Label(statusText, MyGdxGame.skin);
    statusLabel.setColor(Color.WHITE);
    statusLabel.pack();
    statusLabel.setPosition(cx - statusLabel.getPrefWidth() / 2f, gameH - statusLabel.getHeight() - 20);
    gameStage.addActor(statusLabel);

    // ── Hand cards layout ────────────────────────────────────────────────────
    if (!setupSubmitted) {
      ArrayList<Card> handCards = new ArrayList<Card>(currentPlayer.getHandCards());
      int count = handCards.size();
      float maxW = MyGdxGame.WIDTH - 10f;
      final float step = count <= 1 ? cardW : Math.min(cardW, (maxW - cardW) / (count - 1));
      float totalW = cardW + (count > 1 ? (count - 1) * step : 0);
      float startX = cx - totalW / 2f;
      final float handY = (handH - cardH) / 2f;

      for (int i = 0; i < count; i++) {
        final Card c = handCards.get(i);
        final int cardId = c.getCardId();

        // Determine highlight color
        boolean isKing = (cardId == setupSelectedKingId);
        boolean isDef = false;
        for (int id : setupSelectedDefIds) if (id == cardId) { isDef = true; break; }

        c.setCovered(false);
        c.setWidth(cardW);
        c.setHeight(cardH);
        c.setX(startX + i * step);
        c.setY(handY);

        if (isKing) {
          c.setColor(Color.GOLD);
        } else if (isDef) {
          c.setColor(new Color(0.4f, 1f, 0.4f, 1f));
        } else if (inKeepPhase && setupKeepIds.contains(cardId)) {
          c.setColor(new Color(0.4f, 1f, 1f, 1f)); // cyan = marked to keep
        } else if (inKeepPhase) {
          c.setColor(new Color(0.45f, 0.45f, 0.45f, 1f)); // grayed = will be discarded
        } else {
          c.setColor(Color.WHITE);
        }

        final boolean cardIsKing = (cardId == setupSelectedKingId);
        boolean cardIsDef = false;
        for (int id : setupSelectedDefIds) if (id == cardId) { cardIsDef = true; break; }
        final boolean cardIsKeepSelected = cardIsKing || cardIsDef;

        c.removeAllListeners();
        if (inKeepPhase && !cardIsKeepSelected) {
          // In keep phase: tap non-king/non-def cards to toggle keep
          c.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              if (setupKeepIds.contains(cardId)) {
                setupKeepIds.remove((Integer) cardId);
              } else if (setupKeepIds.size() < 2) {
                setupKeepIds.add(cardId);
              }
              gameState.setUpdateState(true);
            }
          });
        } else if (!inKeepPhase) {
          // In king/def selection phase
          c.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              // Deselect king
              if (cardId == setupSelectedKingId) {
                setupSelectedKingId = -1;
                setupKeepIds.clear();
                gameState.setUpdateState(true);
                return;
              }
              // Deselect def
              for (int slot = 0; slot < 3; slot++) {
                if (setupSelectedDefIds[slot] == cardId) {
                  setupSelectedDefIds[slot] = -1;
                  for (int s = slot; s < 2; s++) setupSelectedDefIds[s] = setupSelectedDefIds[s + 1];
                  setupSelectedDefIds[2] = -1;
                  setupKeepIds.clear();
                  gameState.setUpdateState(true);
                  return;
                }
              }
              // Select as king
              if (setupSelectedKingId == -1) {
                setupSelectedKingId = cardId;
                setupKeepIds.clear();
                gameState.setUpdateState(true);
                return;
              }
              // Select as def (up to 3)
              for (int slot = 0; slot < 3; slot++) {
                if (setupSelectedDefIds[slot] == -1) {
                  setupSelectedDefIds[slot] = cardId;
                  setupKeepIds.clear();
                  gameState.setUpdateState(true);
                  return;
                }
              }
            }
          });
        }
        handStage.addActor(c);
      }

      // ── King label ───────────────────────────────────────────────────────
      if (setupSelectedKingId != -1) {
        Label kingLabel = new Label("KING", MyGdxGame.skin);
        kingLabel.setColor(Color.GOLD);
        // Find king card x
        for (int i = 0; i < count; i++) {
          if (handCards.get(i).getCardId() == setupSelectedKingId) {
            kingLabel.pack();
            kingLabel.setPosition(handCards.get(i).getX() + cardW / 2f - kingLabel.getWidth() / 2f,
                handY + cardH + 4f);
            handStage.addActor(kingLabel);
            break;
          }
        }
      }

      // ── Defense labels ───────────────────────────────────────────────────
      for (int slot = 0; slot < 3; slot++) {
        final int defId = setupSelectedDefIds[slot];
        if (defId == -1) continue;
        for (int i = 0; i < count; i++) {
          if (handCards.get(i).getCardId() == defId) {
            Label defLabel = new Label("DEF " + (slot + 1), MyGdxGame.skin);
            defLabel.setColor(new Color(0.4f, 1f, 0.4f, 1f));
            defLabel.pack();
            defLabel.setPosition(handCards.get(i).getX() + cardW / 2f - defLabel.getWidth() / 2f,
                handY - defLabel.getHeight() - 4f);
            handStage.addActor(defLabel);
            break;
          }
        }
      }

      // ── Keep/Discard labels (when in keep phase) ────────────────────────────
      if (inKeepPhase) {
        for (int i = 0; i < count; i++) {
          final Card c2 = handCards.get(i);
          final int cid = c2.getCardId();
          if (cid == setupSelectedKingId) continue;
          boolean isd = false; for (int id : setupSelectedDefIds) if (id == cid) { isd = true; break; }
          if (isd) continue;
          // Show KEEP / DISCARD label below the card
          boolean markedKeep = setupKeepIds.contains(cid);
          Label keepLabel = new Label(markedKeep ? "KEEP" : "DISCARD", MyGdxGame.skin);
          keepLabel.setColor(markedKeep ? new Color(0.4f, 1f, 1f, 1f) : Color.RED);
          keepLabel.pack();
          keepLabel.setPosition(c2.getX() + cardW / 2f - keepLabel.getWidth() / 2f,
              handY - keepLabel.getHeight() - 4f);
          handStage.addActor(keepLabel);
        }
      }

      // ── Confirm button (shown when king + 3 def chosen AND keeps done) ─
      int defCount = 0;
      for (int id : setupSelectedDefIds) if (id != -1) defCount++;
      boolean keepsReady = !needKeepPhase || (setupKeepIds.size() == 2);
      if (setupSelectedKingId != -1 && defCount == 3 && keepsReady) {
        final ArrayList<Integer> frozenKeeps = new ArrayList<Integer>(setupKeepIds);
        TextButton confirmBtn = new TextButton("Confirm", MyGdxGame.skin);
        confirmBtn.pack();
        confirmBtn.setSize(confirmBtn.getPrefWidth() + 20, confirmBtn.getPrefHeight() + 10);
        confirmBtn.setPosition(cx - confirmBtn.getWidth() / 2f, 0.06f * MyGdxGame.HEIGHT);
        confirmBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (setupSubmitted) return;
            setupSubmitted = true;
            try {
              JSONObject data = new JSONObject();
              data.put("kingCardId", setupSelectedKingId);
              JSONArray defIds = new JSONArray();
              defIds.put(setupSelectedDefIds[0]);
              defIds.put(setupSelectedDefIds[1]);
              defIds.put(setupSelectedDefIds[2]);
              data.put("defCardIds", defIds);
              // Compute which cards to discard: all hand cards not in king/def/keep
              JSONArray discardIds = new JSONArray();
              java.util.Set<Integer> keepSet = new java.util.HashSet<Integer>(frozenKeeps);
              keepSet.add(setupSelectedKingId);
              for (int id : setupSelectedDefIds) keepSet.add(id);
              for (Card hc : currentPlayer.getHandCards()) {
                if (!keepSet.contains(hc.getCardId())) discardIds.put(hc.getCardId());
              }
              data.put("discardCardIds", discardIds);
              socket.emit("submitSetup", data);
            } catch (JSONException ex) { ex.printStackTrace(); }
            gameState.setUpdateState(true);
          }
        });
        overlayStage.addActor(confirmBtn);
      }
    }

    Gdx.input.setInputProcessor(menuAndGameMulti);
  }

  public void showGameStage(final ArrayList<Player> players, final Player currentPlayer) {
    Card infoCard = new Card();
    float cardW = infoCard.getDefWidth();
    float cardH = infoCard.getDefHeight();
    // Collect hand count labels to be added LAST (on top of all card actors)
    ArrayList<Label> handCountLabels = new ArrayList<Label>();

    // draw round number
    roundCounter = new Label("Round " + gameState.getRoundNumber(), MyGdxGame.skin);
    roundCounter.setColor(Color.WHITE);
    roundCounter.setPosition(0, MyGdxGame.WIDTH - roundCounter.getHeight());
    gameStage.addActor(roundCounter);

    // draw whose turn it is — directly below the round counter
    Label turnLabel = new Label(gameState.getCurrentPlayer().getPlayerName() + "'s turn", MyGdxGame.skin);
    turnLabel.setColor(Color.GOLD);
    turnLabel.setPosition(roundCounter.getX(),
        roundCounter.getY() - turnLabel.getHeight());
    gameStage.addActor(turnLabel);

    // draw card deck and cemetery
    ArrayList<Card> deckCards = gameState.getCardDeck().getCards();
    for (int i = 0; i < deckCards.size(); i++) {
      Card deckCard = deckCards.get(i);
      deckCard.setDeckPosition();
      deckCard.setY(deckCard.getY() + i * 0.3f);
      gameStage.addActor(deckCard);
    }

    CardDeck cemeteryDeck = gameState.getCemeteryDeck();
    ArrayList<Card> cemeteryCards = gameState.getCemeteryDeck().getCards();
    for (int i = 0; i < cemeteryCards.size(); i++) {
      Card cemeteryCard = cemeteryCards.get(i);
      if (cemeteryCard.getBoosted() > 0) {
        cemeteryCard.unboost(players);
      }
      cemeteryCard.setCemeteryPosition();
      cemeteryCard.setY(cemeteryCard.getY() + i * 0.3f);
      gameStage.addActor(cemeteryCard);
    }
    cemeteryDeck.setRotation(45);
    cemeteryDeck.setWidth(infoCard.getDefWidth());
    cemeteryDeck.setHeight(infoCard.getDefHeight());
    cemeteryDeck.setX(MyGdxGame.WIDTH / 2f - infoCard.getDefWidth() / 2f);
    cemeteryDeck.setY(MyGdxGame.WIDTH / 2f);
    gameStage.addActor(cemeteryDeck);

    // draw picking decks
    ArrayList<PickingDeck> pickingDecks = gameState.getPickingDecks();
    for (int i = 0; i < pickingDecks.size(); i++) {
      ArrayList<Card> pickingCards = pickingDecks.get(i).getCards();
      for (int j = 0; j < pickingCards.size(); j++) {
        pickingCards.get(j).setX(MyGdxGame.WIDTH / 2 - pickingCards.get(j).getDefWidth() / 2
            + (2 * i - 1) * 0.8f * pickingCards.get(j).getDefWidth());
        pickingCards.get(j).setY(MyGdxGame.WIDTH / 2 - pickingCards.get(j).getDefHeight() / 2
            + (2 * i - 1) * 0.8f * pickingCards.get(j).getDefWidth());
        pickingCards.get(j).setRotation(45);
        // shift offset
        pickingCards.get(j)
            .setX(pickingCards.get(j).getX() + 0.1f * (pickingCards.size() - 1) * pickingCards.get(j).getDefHeight());
        pickingCards.get(j)
            .setY(pickingCards.get(j).getY() - 0.1f * (pickingCards.size() - 1) * pickingCards.get(j).getDefHeight());
        // shift for each card
        pickingCards.get(j).setX(pickingCards.get(j).getX() - 0.2f * (j) * pickingCards.get(j).getDefHeight());
        pickingCards.get(j).setY(pickingCards.get(j).getY() + 0.2f * (j) * pickingCards.get(j).getDefHeight());
        gameStage.addActor(pickingCards.get(j));
      }
      if (!pickingCards.isEmpty()) {
        pickingDecks.get(i).setX(MyGdxGame.WIDTH / 2 - pickingCards.get(0).getDefWidth() / 2
            + (2 * i - 1) * 0.8f * pickingCards.get(0).getDefWidth());
        pickingDecks.get(i).setY(MyGdxGame.WIDTH / 2 - pickingCards.get(0).getDefHeight() / 2
            + (2 * i - 1) * 0.8f * pickingCards.get(0).getDefWidth());
        pickingDecks.get(i).setWidth(pickingCards.get(0).getDefWidth());
        pickingDecks.get(i).setHeight(pickingCards.get(0).getDefHeight());
        pickingDecks.get(i).setRotation(45);
        gameStage.addActor(pickingDecks.get(i));
      }
    }

    // Attach zoom listeners to picking decks (once per PickingDeck object lifetime)
    for (int i = 0; i < pickingDecks.size(); i++) {
      PickingDeck deck = pickingDecks.get(i);
      if (!deckZoomAttached.contains(deck) && !deck.getCards().isEmpty()) {
        attachPickingDeckZoom(deck);
        deckZoomAttached.add(deck);
      }
    }

    // draw game status of players
    for (int i = 0; i < players.size(); i++) {
      // visual slot 0 = bottom (own player), 1 = left, 2 = top, 3 = right
      int visualSlot = (i - playerIndex + 4) % 4;
      System.out.println("Player " + players.get(i).getPlayerName() + " hand = " + players.get(i).getHandCards().size());
      System.out.println("Player " + players.get(i).getPlayerName() + " def = " + players.get(i).getDefCards().size());

      // display dice
      Dice dice = players.get(i).getDice();
      dice.setMapPosition(visualSlot);
      gameStage.addActor(dice);

      // Skip all card rendering for eliminated players
      if (players.get(i).isOut()) {
        Label outLabel = new Label(players.get(i).getPlayerName() + " OUT", MyGdxGame.skin);
        outLabel.setColor(Color.RED);
        outLabel.setPosition(dice.getX() + dice.getWidth() + 2f, dice.getY());
        gameStage.addActor(outLabel);
        continue;
      }

      // Hand deck: to the RIGHT of the king card from the player's perspective,
      // rotated 90° relative to the player's card orientation,
      // gap = one card length (cardH) between king's near edge and deck's near edge.
      //
      // "Right" maps to screen directions:
      //   P0 (faces up)    → +X   P1 (faces right) → -Y
      //   P2 (faces down)  → -X   P3 (faces left)  → +Y
      //
      // King positions (setMapPosition position=0):
      //   P0 center: (WIDTH/2,       H/2)
      //   P1 center: (H/2,           WIDTH/2)
      //   P2 center: (WIDTH/2,       WIDTH-H/2)
      //   P3 center: (WIDTH-H/2,     WIDTH/2)
      //
      // For rot 90°: visual box centred at (anchorX+W/2, anchorY+H/2),
      //              half-extents H/2 (horizontal) and W/2 (vertical).
      // For rot 0°/180°: visual box = [anchorX, anchorX+W] × [anchorY, anchorY+H].
      ArrayList<Card> handCards = players.get(i).getHandCards();
      if (handCards.size() > 0) {
        // Gap between king's near edge and deck's near edge = cardW/2.
        // "Right" from each player's perspective maps to these screen directions:
        //   P0 (+X), P1 (-Y), P2 (-X), P3 (+Y).
        // For rot 90°: visual_left  = anchorX + W/2 - H/2
        //              visual_right = anchorX + W/2 + H/2
        // For rot 0°/180°: visual top = anchorY + H, visual bottom = anchorY
        float deckX, deckY;
        int deckRot;
        switch (visualSlot) {
        case 0: // king visual right = (WIDTH+W)/2; deck visual left = king_right + W/2
          deckX = (MyGdxGame.WIDTH + cardW) / 2f + cardW / 2f + (cardH - cardW) / 2f;
          deckY = 0f;
          deckRot = 90;
          break;
        case 1: // king visual bottom = WIDTH/2 - W/2; deck top (anchorY+H) = king_bottom - W/2
          deckX = (cardH - cardW) / 2f;
          deckY = MyGdxGame.WIDTH / 2f - cardW - cardH;
          deckRot = 0;
          break;
        case 2: // king visual left = (WIDTH-H)/2; deck visual right = king_left - W/2
          deckX = (MyGdxGame.WIDTH - cardH) / 2f - cardW / 2f - (cardH + cardW) / 2f;
          deckY = MyGdxGame.WIDTH - cardH;
          deckRot = 90;
          break;
        case 3: // king visual top = WIDTH/2 + W/2; deck visual bottom (anchorY) = king_top + W/2
          deckX = MyGdxGame.WIDTH - cardH / 2f - cardW / 2f;
          deckY = MyGdxGame.WIDTH / 2f + cardW;
          deckRot = 180;
          break;
        default:
          deckX = 0; deckY = 0; deckRot = 0; break;
        }

        for (int j = 0; j < handCards.size(); j++) {
          final Card handCard = handCards.get(j);
          handCard.setCovered(true);
          handCard.setRotation(deckRot);
          handCard.setActive(false);
          // Only deselect cards for other players — current player's cards keep their selection
          if (players.get(i) != currentPlayer) {
            handCard.setSelected(false);
          }
          handCard.setSize(cardW, cardH);
          handCard.setPosition(deckX + j * 0.3f, deckY + j * 0.3f);
          handCard.removeAllListeners();
          enemyHandCardListener = new EnemyHandCardListener(handCard, gameState.getCurrentPlayer(),
              gameState.getPlayers(), i, gameState);
          handCard.addListener(enemyHandCardListener);
          gameStage.addActor(handCard);
          // Issue #175: highlight the top hand card of an enemy deck when Priest is selected.
          if (j == handCards.size() - 1) {
            applyEnemyHandDeckHighlight(handCard, players.get(i), currentPlayer);
          }
        }

        // Count label only for other players (not the local player).
        // Centred directly on the deck visual centre: (anchorX+W/2, anchorY+H/2).
        if (i != playerIndex) {
          Label handCountLabel = new Label(String.valueOf(handCards.size()), MyGdxGame.skin);
          handCountLabel.setColor(Color.BLACK);
          handCountLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          float lw = handCountLabel.getPrefWidth();
          float lh = handCountLabel.getPrefHeight();
          handCountLabel.setPosition(deckX + cardW / 2f - lw / 2f, deckY + cardH / 2f - lh / 2f);
          handCountLabels.add(handCountLabel);
        }
      }

      // display king cards
      final Card kingCard = players.get(i).getKingCard();
      if (kingCard == null) {
        Gdx.app.error("GameScreen", "kingCard is null for player " + i);
        continue;
      }
      // setMapPosition() resets isSelected=false; preserve it for the own king so that a
      // server stateUpdate arriving while the player has their king selected does not
      // silently deselect it. Restored regardless of kingUsedThisTurn — the king may
      // still need to be selected for a swap even after it has already attacked this turn.
      // Attack listeners independently guard against a second king attack.
      boolean kingWasSelected = (players.get(i) == currentPlayer)
          && kingCard.isSelected();
      kingCard.setMapPosition(visualSlot, 0, 0);
      if (kingWasSelected) kingCard.setSelected(true);
      // Own king: active (grey tint, face visible to owner); enemy king: card back
      if (players.get(i) == currentPlayer) {
        kingCard.setActive(true);
      } else {
        kingCard.setActive(false);
      }

      kingCard.removeAllListeners();

      if (players.get(i) != currentPlayer) {
        enemyKingCardListener = new EnemyKingCardListener(gameState, kingCard, gameState.getCurrentPlayer(),
            gameState.getPlayers(), socket, playerIndex);
        kingCard.addListener(enemyKingCardListener);
      } else {
        ownKingCardListener = new OwnKingCardListener(gameState, currentPlayer,
            gameState.getCurrentPlayer().getKingCard(), gameState.getCurrentPlayer().getDefCards(),
            gameState.getCurrentPlayer().getTopDefCards(), gameState.getCurrentPlayer().getHandCards(),
            socket, playerIndex);
        kingCard.addListener(ownKingCardListener);
      }

      gameStage.addActor(kingCard);

      // Issue #218: zoom on hover/tap for face-up king cards
      // Own king uses overlay zoom so it renders above the hand strip when zoomed.
      if (players.get(i) == currentPlayer) {
        attachKingOverlayZoomListener(kingCard);
      } else if (!kingCard.isCovered()) {
        attachZoomListener(kingCard);
      }

      // Issue #167: Mercenaries selection highlight on own king (top=add, bottom=remove)
      if (players.get(i) == currentPlayer && isMercenariesSelectedBy(currentPlayer)) {
        addMercenarySelectionHighlight(kingCard);
      }

      // Issues #54, #179, #180: highlight enemy king when Magician/Warlord/Spy is selected
      // and the appropriate conditions are met.
      applyEnemyKingHighlight(kingCard, players.get(i), currentPlayer);

      if (kingCard.getBoosted() > 0) {
        TextureRegion mercenaryRegion = new TextureRegion(texMercenary, 0, 0, 512, 512);
        Image mercenaryImage = new Image(mercenaryRegion);
        mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(), mercenaryImage.getWidth() / 20f,
            mercenaryImage.getHeight() / 20f);
        mercenaryImage.setPosition(kingCard.getX(), kingCard.getY());
        mercenaryImage.setX(mercenaryImage.getX() + kingCard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
        mercenaryImage.setY(mercenaryImage.getY() + kingCard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);
        // Issue #167: don't absorb clicks — add/remove via the king card itself.
        mercenaryImage.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        gameStage.addActor(mercenaryImage);

        String boostCount = String.valueOf(kingCard.getBoosted());
        Label boostCountLabel = new Label(boostCount, MyGdxGame.skin);
        boostCountLabel.setColor(Color.GOLD);
        boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());
        boostCountLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        gameStage.addActor(boostCountLabel);
      }

      // Reservists indicator on king card — cyan pawn + "+N" visible to all players
      int resCount;
      if (players.get(i) == currentPlayer) {
        resCount = 0;
        for (Hero h : players.get(i).getHeroes()) {
          if ("Reservists".equals(h.getHeroName())) {
            resCount = ((Reservists) h).countReady();
            break;
          }
        }
      } else {
        resCount = players.get(i).getReservistsReadyCount();
      }
      if (resCount > 0) {
        TextureRegion resRegion = new TextureRegion(texMercenary, 0, 0, 512, 512);
        Image resImage = new Image(resRegion);
        resImage.setBounds(resImage.getX(), resImage.getY(), resImage.getWidth() / 20f, resImage.getHeight() / 20f);
        float resCx = kingCard.getX() + kingCard.getWidth() / 2f - resImage.getWidth() / 2f;
        float resCy = kingCard.getY() + kingCard.getHeight() / 2f - resImage.getHeight() / 2f;
        resImage.setPosition(resCx, resCy);
        resImage.setColor(Color.CYAN);
        gameStage.addActor(resImage);
        Label resCountLabel = new Label("+" + resCount, MyGdxGame.skin);
        resCountLabel.setColor(Color.CYAN);
        resCountLabel.setPosition(resImage.getX() + resImage.getWidth(), resImage.getY());
        gameStage.addActor(resCountLabel);
      }

      // display defense cards and placeholders
      Map<Integer, Card> defCards = players.get(i).getDefCards();
      for (int j = 1; j <= 3; j++) {
        final Card defCard;
        if (defCards.containsKey(j)) {
          defCard = defCards.get(j);
          defCard.setPlaceholder(false);
          defCard.removeAllListeners();
          if (players.get(i) != currentPlayer) {
            enemyDefCardListener = new EnemyDefCardListener(defCard, gameState,
                gameState.getCurrentPlayer(), gameState.getPlayers(), socket, playerIndex);
            defCard.addListener(enemyDefCardListener);
          } else {
            ownDefCardListener = new OwnDefCardListener(gameState, defCard, gameState.getCurrentPlayer().getKingCard(),
                gameState.getCurrentPlayer().getDefCards(), gameState.getCurrentPlayer().getTopDefCards(),
                gameState.getCurrentPlayer().getHandCards(), gameState.getCurrentPlayer(), gameState.getPlayers(),
                socket, playerIndex);
            defCard.addListener(ownDefCardListener);
            // Drag-to-take: drag defense card down into hand area to pick it up
            final Card dragDefCard = defCard;
            dragDefCard.addListener(new DragListener() {
              float touchOffX, touchOffY;
              @Override
              public void dragStart(InputEvent event, float x, float y, int pointer) {
                touchOffX = x; touchOffY = y;
                isDraggingDefCard = true;
                dragDefCard.setVisible(false);
                dragOverlayCard = Card.fromCardId(dragDefCard.getCardId());
                dragOverlayCard.setWidth(dragDefCard.getWidth());
                dragOverlayCard.setHeight(dragDefCard.getHeight());
                float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
                dragOverlayCard.setPosition(dragDefCard.getX(), dragDefCard.getY() + oy);
                overlayStage.addActor(dragOverlayCard);
              }
              @Override
              public void drag(InputEvent event, float x, float y, int pointer) {
                if (dragOverlayCard == null) return;
                float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
                dragOverlayCard.setPosition(event.getStageX() - touchOffX, (event.getStageY() - touchOffY) + oy);
              }
              @Override
              public void dragStop(InputEvent event, float x, float y, int pointer) {
                isDraggingDefCard = false;
                if (dragOverlayCard != null) { dragOverlayCard.remove(); dragOverlayCard = null; }
                dragDefCard.setVisible(true);
                if (event.getStageY() < 0 && currentPlayer.canTakeDefCard()) {
                  emitTakeDefCard(dragDefCard.getPositionId());
                  currentPlayer.takeDefCard(dragDefCard.getPositionId());
                }
                gameState.setUpdateState(true);
              }
            });
          }
        } else {
          defCard = new Card();
          defCard.removeAllListeners();
          if (players.get(i) == currentPlayer) {
            ownPlaceholderListener = new OwnPlaceholderListener(defCard, gameState.getCurrentPlayer(), gameState);
            defCard.addListener(ownPlaceholderListener);
          } else {
            // Enemy empty slot — add placeholder listener so Saboteurs can place here
            enemyPlaceholderListener = new EnemyPlaceholderListener(
                currentPlayer, players.get(i), j, playerIndex, i, gameState, socket);
            defCard.addListener(enemyPlaceholderListener);
          }
        }

        if (defCard.isRemoved()) {
          players.get(i).getDefCards().remove(j);
          System.out.println("Def card removed!");
        }

        defCard.setMapPosition(visualSlot, j, 0);
        // Own def card: active (grey tint, face visible to owner); enemy def card: card back
        if (players.get(i) == currentPlayer) {
          defCard.setActive(true);
        } else {
          defCard.setActive(false);
        }
        gameStage.addActor(defCard);

        // Issue #218: zoom on hover/tap for face-up def cards (non-placeholders only)
        if (!defCard.isPlaceholder()) {
          if (players.get(i) == currentPlayer || !defCard.isCovered()) {
            attachZoomListener(defCard);
          }
        }

        // Issues #54, #178, #179, #180: highlight enemy def cards (and empty enemy slots
        // for Saboteurs) when the relevant attacker hero is selected.
        applyEnemyDefCardHighlight(defCard, players.get(i), currentPlayer, j);
        // Issue #174: highlight own def cards on which the selected hand card can be stacked.
        applyOwnDefCardFortifyHighlight(defCard, players.get(i), currentPlayer, j);

        // Issue #167: when Mercenaries hero is selected, overlay translucent
        // green (top half) / red (bottom half) tint on each own def card so the
        // player sees where to click to add or remove a mercenary. Skip if a top
        // def card is stacked on this slot — the highlight then goes on the top
        // card below.
        if (players.get(i) == currentPlayer
            && !players.get(i).getTopDefCards().containsKey(j)
            && isMercenariesSelectedBy(currentPlayer)) {
          addMercenarySelectionHighlight(defCard);
        }

        if (players.get(i).isSlotSabotaged(j)) {
          TextureRegion sabotagedRegion = new TextureRegion(texSabotaged, 0, 0, 64, 64);
          Image sabotagedImage = new Image(sabotagedRegion);
          sabotagedImage.setBounds(sabotagedImage.getX(), sabotagedImage.getY(),
              sabotagedImage.getWidth() / 5f, sabotagedImage.getHeight() / 5f);
          sabotagedImage.setPosition(defCard.getX(), defCard.getY());
          sabotagedImage.setX(sabotagedImage.getX() + defCard.getWidth() / 2f - sabotagedImage.getWidth() / 2f);
          sabotagedImage.setY(sabotagedImage.getY() + defCard.getHeight() / 2f - sabotagedImage.getHeight() / 2f);
          removeAllListeners(sabotagedImage);
          sabotagedImageListener = new SabotagedImageListener(gameState, defCard, currentPlayer,
              players.get(i), j, playerIndex, socket);
          sabotagedImage.addListener(sabotagedImageListener);
          gameStage.addActor(sabotagedImage);
        }

        if (defCard.getBoosted() > 0) {
          TextureRegion mercenaryRegion = new TextureRegion(texMercenary, 0, 0, 512, 512);
          Image mercenaryImage = new Image(mercenaryRegion);
          mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(),
              mercenaryImage.getWidth() / 20f, mercenaryImage.getHeight() / 20f);
          mercenaryImage.setPosition(defCard.getX(), defCard.getY());
          mercenaryImage.setX(mercenaryImage.getX() + defCard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
          mercenaryImage.setY(mercenaryImage.getY() + defCard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);
          // Issue #167: the mercenary symbol overlay must NOT absorb clicks — the
          // player adds/removes mercenaries by clicking the def card itself
          // (top half = add, bottom half = remove) when the Mercenaries hero is
          // selected, and selects the def card normally when it isn't.
          mercenaryImage.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          gameStage.addActor(mercenaryImage);

          String boostCount = "+" + String.valueOf(defCard.getBoosted());
          Label boostCountLabel = new Label(boostCount, MyGdxGame.skin);
          boostCountLabel.setColor(Color.GOLD);
          boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());
          boostCountLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          gameStage.addActor(boostCountLabel);
        }
      }

      // display top defense cards
      Map<Integer, Card> topDefCards = players.get(i).getTopDefCards();
      for (int j = 1; j <= 3; j++) {
        final Card topDefCard;
        if (topDefCards.containsKey(j)) {
          topDefCard = topDefCards.get(j);
          topDefCard.removeAllListeners();
          if (players.get(i) != currentPlayer) {
            enemyDefCardListener = new EnemyDefCardListener(topDefCard, gameState,
                gameState.getCurrentPlayer(), gameState.getPlayers(), socket, playerIndex);
            topDefCard.addListener(enemyDefCardListener);
          } else {
            ownDefCardListener = new OwnDefCardListener(gameState, topDefCard,
                gameState.getCurrentPlayer().getKingCard(), gameState.getCurrentPlayer().getDefCards(),
                gameState.getCurrentPlayer().getTopDefCards(), gameState.getCurrentPlayer().getHandCards(),
                gameState.getCurrentPlayer(), gameState.getPlayers(),
                socket, playerIndex);
            topDefCard.addListener(ownDefCardListener);
            // Drag-to-take: drag top defense card down into hand area to pick it up
            final Card dragTopDefCard = topDefCard;
            dragTopDefCard.addListener(new DragListener() {
              float touchOffX, touchOffY;
              @Override
              public void dragStart(InputEvent event, float x, float y, int pointer) {
                touchOffX = x; touchOffY = y;
                isDraggingDefCard = true;
                dragTopDefCard.setVisible(false);
                dragOverlayCard = Card.fromCardId(dragTopDefCard.getCardId());
                dragOverlayCard.setWidth(dragTopDefCard.getWidth());
                dragOverlayCard.setHeight(dragTopDefCard.getHeight());
                float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
                dragOverlayCard.setPosition(dragTopDefCard.getX(), dragTopDefCard.getY() + oy);
                overlayStage.addActor(dragOverlayCard);
              }
              @Override
              public void drag(InputEvent event, float x, float y, int pointer) {
                if (dragOverlayCard == null) return;
                float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
                dragOverlayCard.setPosition(event.getStageX() - touchOffX, (event.getStageY() - touchOffY) + oy);
              }
              @Override
              public void dragStop(InputEvent event, float x, float y, int pointer) {
                isDraggingDefCard = false;
                if (dragOverlayCard != null) { dragOverlayCard.remove(); dragOverlayCard = null; }
                dragTopDefCard.setVisible(true);
                if (event.getStageY() < 0 && currentPlayer.canTakeDefCard()) {
                  emitTakeDefCard(dragTopDefCard.getPositionId());
                  currentPlayer.takeDefCard(dragTopDefCard.getPositionId());
                }
                gameState.setUpdateState(true);
              }
            });
          }
          topDefCard.setMapPosition(visualSlot, j, 1);
          // Own top def card: active (grey tint, face visible to owner); enemy: card back
          if (players.get(i) == currentPlayer) {
            topDefCard.setActive(true);
          } else {
            topDefCard.setActive(false);
          }
          gameStage.addActor(topDefCard);

          // Issue #218: zoom on hover/tap for top def cards
          if (players.get(i) == currentPlayer || !topDefCard.isCovered()) {
            attachZoomListener(topDefCard);
          }

          // Issue #167: stacked slot — highlight the top card (the one the
          // player actually sees and clicks) when Mercenaries is selected.
          if (players.get(i) == currentPlayer && isMercenariesSelectedBy(currentPlayer)) {
            addMercenarySelectionHighlight(topDefCard);
          }
          // Issues #54, #178, #179, #180: enemy-targeting hero highlight on top def card.
          applyEnemyDefCardHighlight(topDefCard, players.get(i), currentPlayer, j);
        }
      }

      // display player label
      // Break long names (e.g. "Bot 1 (Passive)") before the parenthesised suffix
      // so the label never overlaps adjacent defense cards.
      String rawName = players.get(i).getPlayerName();
      String displayName = rawName.contains(" (") ? rawName.replace(" (", "\n(") : rawName;
      Label playerLabel = new Label(displayName, MyGdxGame.skin);
      playerLabel.setFontScale(0.8f);
      playerLabel.pack(); // recalculate width/height after scale
      // Highlight the player whose turn it currently is
      if (players.get(i) == gameState.getCurrentPlayer()) {
        playerLabel.setColor(Color.GOLD);
      } else {
        playerLabel.setColor(Color.WHITE);
      }
      switch (visualSlot) {
      case 0:
        playerLabel.setPosition((MyGdxGame.WIDTH - playerLabel.getWidth()) / 2 - kingCard.getDefHeight(),
            kingCard.getDefWidth() / 2);
        break;
      case 1:
        playerLabel.setPosition(0, (MyGdxGame.WIDTH - playerLabel.getHeight()) / 2 + kingCard.getDefWidth());
        break;
      case 2:
        playerLabel.setPosition((MyGdxGame.WIDTH - playerLabel.getWidth()) / 2 + kingCard.getDefHeight(),
            MyGdxGame.WIDTH - playerLabel.getHeight() - kingCard.getDefWidth() / 2);
        break;
      case 3:
        playerLabel.setPosition(MyGdxGame.WIDTH - playerLabel.getWidth(),
            (MyGdxGame.WIDTH - playerLabel.getHeight()) / 2 - kingCard.getDefWidth());
        break;
      default:
        break;
      }

      // display heroes
      ArrayList<Hero> playerHeroes = players.get(i).getHeroes();
      for (int j = 0; j < playerHeroes.size(); j++) {
        // Don't reset selection state of the local player's heroes — that is
        // managed by showHandStage/listeners so it persists across refreshes.
        if (players.get(i) != currentPlayer) {
          playerHeroes.get(j).setSelected(false);
        }
        playerHeroes.get(j).setHand(false);
        playerHeroes.get(j).setPosition(playerLabel.getX(), playerLabel.getY());

        switch (visualSlot) {
        case 0:
          playerHeroes.get(j).setPosition(
              playerHeroes.get(j).getX() - playerLabel.getWidth() - j * playerHeroes.get(j).getWidth() / 3,
              playerHeroes.get(j).getY() - playerHeroes.get(j).getHeight() / 4);
          break;
        case 1:
          playerHeroes.get(j).setPosition(playerHeroes.get(j).getX(),
              playerHeroes.get(j).getY() + 2 * playerLabel.getHeight() + j * playerHeroes.get(j).getHeight() / 3);
          break;
        case 2:
          playerHeroes.get(j).setPosition(
              playerHeroes.get(j).getX() + playerLabel.getWidth() + j * playerHeroes.get(j).getWidth() / 3,
              MyGdxGame.WIDTH - playerHeroes.get(j).getHeight());
          break;
        case 3:
          playerHeroes.get(j).setPosition(MyGdxGame.WIDTH - playerHeroes.get(j).getWidth(),
              playerHeroes.get(j).getY() - 2 * playerLabel.getHeight() - j * playerHeroes.get(j).getHeight() / 3);
          break;
        default:
          break;
        }

        // All board heroes get an info overlay click listener.
        // (Local player's own heroes are moved to handStage by showHandStage, so the
        //  listener added here gets replaced there — no conflict.)
        playerHeroes.get(j).removeAllListeners();
        final String heroInfoName_gs = playerHeroes.get(j).getHeroName();
        playerHeroes.get(j).addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            showHeroInfoOverlay(heroInfoName_gs);
            event.stop();
          }
        });
        gameStage.addActor(playerHeroes.get(j));
      }

      gameStage.addActor(playerLabel);

      // Avatar icon next to the player label
      String iconName = players.get(i).getIcon();
      if (iconName != null && !iconName.isEmpty()) {
        Texture avTex = gameAvatarTextures.get(iconName);
        if (avTex == null) {
          try {
            avTex = new Texture(Gdx.files.internal("data/avatars/" + iconName + ".png"));
            gameAvatarTextures.put(iconName, avTex);
          } catch (Exception ignored) { }
        }
        if (avTex != null) {
          final float avSize = 20f;
          Image avImg = new Image(avTex);
          avImg.setSize(avSize, avSize);
          switch (visualSlot) {
          case 0: // bottom-centre — place avatar to the left of the label
            avImg.setPosition(playerLabel.getX() - avSize - 2f, playerLabel.getY() + (playerLabel.getHeight() - avSize) / 2f);
            break;
          case 1: // left-centre — place above the label
            avImg.setPosition(playerLabel.getX() + (playerLabel.getWidth() - avSize) / 2f, playerLabel.getY() + playerLabel.getHeight() + 2f);
            break;
          case 2: // top-centre — place avatar to the right of the label
            avImg.setPosition(playerLabel.getX() + playerLabel.getWidth() + 2f, playerLabel.getY() + (playerLabel.getHeight() - avSize) / 2f);
            break;
          case 3: // right-centre — place below the label
            avImg.setPosition(playerLabel.getX() + (playerLabel.getWidth() - avSize) / 2f, playerLabel.getY() - avSize - 2f);
            break;
          default: break;
          }
          gameStage.addActor(avImg);
        }
      }
    }

    // Add hand count labels AFTER all player actors so they render on top
    for (Label lbl : handCountLabels) {
      gameStage.addActor(lbl);
    }

    // Loot preview overlay — added LAST so it renders on top of everything
    if (currentPlayer.getPlayerTurn().isLootPending()) {
      final Player lootPlayer = currentPlayer;
      final PlayerTurn pt = lootPlayer.getPlayerTurn();
      final boolean lootSuccess = pt.isLootSuccess();

      // Semi-transparent black tint over the whole board; catches any tap to confirm
      Image overlay = new Image(MyGdxGame.skin, "white");
      overlay.setFillParent(true);
      overlay.setColor(0f, 0f, 0f, 0.45f);
      overlay.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          if (!pt.isLootPending()) return; // guard against double-tap before re-render
          final int deckIdx = pt.getPendingPickingDeckIndex();
          PickingDeck thisD = gameState.getPickingDecks().get(deckIdx);
          PickingDeck otherD = gameState.getPickingDecks().get(1 - deckIdx);
          if (lootSuccess) {
            Iterator<Card> it = thisD.getCards().iterator();
            while (it.hasNext()) { lootPlayer.addHandCard(it.next()); it.remove(); }
            otherD.addCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
            thisD.addCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
            thisD.getCards().get(thisD.getCards().size() - 1).setCovered(false);
            thisD.addCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
            if (pt.isKingUsed()) lootPlayer.getKingCard().setCovered(false);
          } else {
            Card newPickCard = gameState.getCardDeck().getCard(gameState.getCemeteryDeck());
            newPickCard.setCovered(true);
            thisD.addCard(newPickCard);
            if (pt.isKingUsed()) {
              lootPlayer.getKingCard().setCovered(false);
              lootPlayer.setOut(true);
            }
          }
          for (Card c : pt.getPendingAttackCards()) {
            lootPlayer.getHandCards().remove(c);
            gameState.getCemeteryDeck().addCard(c);
          }
          for (Card c : pt.getPendingAttackOwnDefCards()) {
            int slot = c.getPositionId();
            lootPlayer.getDefCards().remove(slot);
            lootPlayer.getTopDefCards().remove(slot);
            gameState.getCemeteryDeck().addCard(c);
          }
          pt.setLootPending(false);
          if (pt.isKingUsed()) pt.setKingUsedThisTurn(true);
          // Coup swap: if the old king (now a hand card) was used in this attack, mark king as spent
          int coupId = pt.getCoupSwapPendingCardId();
          if (coupId != -1) {
            for (Card c : pt.getPendingAttackCards()) {
              if (c.getCardId() == coupId) {
                pt.setKingUsedThisTurn(true);
                pt.setCoupSwapPendingCardId(-1);
                break;
              }
            }
          }
          // Broadcast to server (server applies + broadcasts stateUpdate to all)
          try {
            JSONObject emitData = new JSONObject();
            emitData.put("attackerIdx", gameState.getCurrentPlayerIndex());
            emitData.put("deckIndex", deckIdx);
            emitData.put("success", lootSuccess);
            emitData.put("kingUsed", pt.isKingUsed());
            JSONArray atkIdArr = new JSONArray();
            for (Card c : pt.getPendingAttackCards()) atkIdArr.put(c.getCardId());
            emitData.put("attackCardIds", atkIdArr);
            JSONArray ownDefIdArr = new JSONArray();
            for (Card c : pt.getPendingAttackOwnDefCards()) ownDefIdArr.put(c.getCardId());
            emitData.put("attackerOwnDefCardIds", ownDefIdArr);
            socket.emit("lootResolved", emitData);
          } catch (JSONException e) {
            e.printStackTrace();
          }
          tutorialAdvance(TUTORIAL_STEP_LOOT);
          tutorialAdvanceHook("LOOT");
          pt.getPendingAttackCards().clear();
          pt.getPendingAttackOwnDefCards().clear();
          pt.resetReservistAttackBonus();
          pt.resetPendingAttackMercenaryBonus();
          if (currentPlayer.getKingCard() != null) currentPlayer.getKingCard().setSelected(false);
          gameState.setUpdateState(true);
        }
      });

      // Battle visualization — card layout constants
      Card refCardPl = new Card();
      float plCW = refCardPl.getDefWidth() * 1.5f;
      float plCH = refCardPl.getDefHeight() * 1.5f;
      float plBotY = 265f;
      float plLeftX = 10f;
      float plRightX = MyGdxGame.WIDTH / 2f + 10f;

      int plAtkSum = pt.getPendingLootAttackSum() + pt.getReservistAttackBonus();
      int plDefStr = pt.getPendingLootDefStrength();

      Label lootResultLabel = new Label(
          lootSuccess ? "SUCCESS!  Tap to claim the cards."
                      : "FAILED.  Tap to continue.",
          MyGdxGame.skin);
      lootResultLabel.setColor(lootSuccess ? Color.GREEN : Color.RED);
      lootResultLabel.setPosition(
          MyGdxGame.WIDTH / 2f - lootResultLabel.getPrefWidth() / 2f,
          plBotY - 44f);

      gameStage.addActor(overlay);

      // Column headers
      Label plAtkHdr = new Label("ATTACK", MyGdxGame.skin);
      plAtkHdr.setColor(Color.CYAN);
      plAtkHdr.setPosition(plLeftX, plBotY + plCH + 5f);
      gameStage.addActor(plAtkHdr);
      Label plDefHdr = new Label("LOOT", MyGdxGame.skin);
      plDefHdr.setColor(Color.ORANGE);
      plDefHdr.setPosition(plRightX, plBotY + plCH + 5f);
      gameStage.addActor(plDefHdr);

      // Attack cards (left column)
      if (pt.isKingUsed() && lootPlayer.getKingCard() != null) {
        Card kd = Card.fromCardId(lootPlayer.getKingCard().getCardId());
        kd.setCovered(false); kd.setActive(true);
        kd.setSize(plCW, plCH);
        kd.setPosition(plLeftX, plBotY);
        gameStage.addActor(kd);
      } else {
        ArrayList<Card> plAtkSrc = new ArrayList<Card>(pt.getPendingAttackCards());
        plAtkSrc.addAll(pt.getPendingAttackOwnDefCards());
        int nPA = Math.max(1, plAtkSrc.size());
        float paW = Math.min(plCW, (MyGdxGame.WIDTH / 2f - 20f - (nPA - 1) * 4f) / nPA);
        float paH = plCH * (paW / plCW);
        for (int ai = 0; ai < plAtkSrc.size(); ai++) {
          Card disp = Card.fromCardId(plAtkSrc.get(ai).getCardId());
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(paW, paH);
          disp.setPosition(plLeftX + ai * (paW + 4f), plBotY + (plCH - paH) / 2f);
          gameStage.addActor(disp);
        }
      }

      // Defense card (right column) — top card of the harvest deck (already revealed)
      final int plDeckIdx = pt.getPendingPickingDeckIndex();
      ArrayList<Card> plDeckCards = gameState.getPickingDecks().get(plDeckIdx).getCards();
      if (!plDeckCards.isEmpty()) {
        Card defDisp = Card.fromCardId(plDeckCards.get(plDeckCards.size() - 1).getCardId());
        defDisp.setCovered(false); defDisp.setActive(true);
        defDisp.setSize(plCW, plCH);
        defDisp.setPosition(plRightX, plBotY);
        gameStage.addActor(defDisp);
      }

      // Sum labels
      Label plAtkSumLbl = new Label("Sum: " + plAtkSum, MyGdxGame.skin);
      plAtkSumLbl.setColor(Color.WHITE);
      plAtkSumLbl.setPosition(plLeftX, plBotY - 22f);
      gameStage.addActor(plAtkSumLbl);
      Label plDefSumLbl = new Label("Sum: " + plDefStr, MyGdxGame.skin);
      plDefSumLbl.setColor(Color.WHITE);
      plDefSumLbl.setPosition(plRightX, plBotY - 22f);
      gameStage.addActor(plDefSumLbl);

      gameStage.addActor(lootResultLabel);

      // Reservists loot boost button — only when currently failing but can be won
      for (Hero resH : lootPlayer.getHeroes()) {
        if ("Reservists".equals(resH.getHeroName())) {
          final Reservists resHero = (Reservists) resH;
          boolean canFlipLoot = !pt.isLootSuccess() && resHero.isAvailable()
              && (pt.getPendingLootAttackSum() + pt.getReservistAttackBonus() + resHero.countReady())
                  > pt.getPendingLootDefStrength();
          if (canFlipLoot) {
            TextButton resBtn = new TextButton("Reservists +1  (" + resHero.countReady() + " left)", MyGdxGame.skin);
            resBtn.pack();
            resBtn.setPosition(MyGdxGame.WIDTH / 2f - resBtn.getWidth() / 2f, MyGdxGame.WIDTH * 0.42f);
            resBtn.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                resHero.spend();
                emitReservistsKingBoost(resHero.countReady());
                pt.incrementReservistAttackBonus();
                boolean newLootSuccess =
                    pt.getPendingLootAttackSum() + pt.getReservistAttackBonus() > pt.getPendingLootDefStrength();
                pt.setLootSuccess(newLootSuccess);
                // Re-emit lootPreview so watchers see the updated sum and outcome
                if (socket != null) {
                  try {
                    JSONObject plPreview = new JSONObject();
                    plPreview.put("attackerIdx", playerIndex);
                    plPreview.put("deckIndex", plDeckIdx);
                    ArrayList<Card> plDeckCurr = gameState.getPickingDecks().get(plDeckIdx).getCards();
                    plPreview.put("defCardId", plDeckCurr.isEmpty() ? -1 : plDeckCurr.get(plDeckCurr.size() - 1).getCardId());
                    plPreview.put("attackSum", pt.getPendingLootAttackSum() + pt.getReservistAttackBonus());
                    plPreview.put("defStrength", pt.getPendingLootDefStrength());
                    plPreview.put("success", newLootSuccess);
                    plPreview.put("kingUsed", pt.isKingUsed());
                    plPreview.put("kingCardId", pt.isKingUsed() && lootPlayer.getKingCard() != null ? lootPlayer.getKingCard().getCardId() : -1);
                    plPreview.put("mercenaryBonus", pt.getPendingAttackMercenaryBonus());
                    plPreview.put("reservistBonus", pt.getReservistAttackBonus());
                    JSONArray plResAtkIds = new JSONArray();
                    for (Card c : pt.getPendingAttackCards()) plResAtkIds.put(c.getCardId());
                    plPreview.put("attackCardIds", plResAtkIds);
                    JSONArray plResOwnIds = new JSONArray();
                    for (Card c : pt.getPendingAttackOwnDefCards()) plResOwnIds.put(c.getCardId());
                    plPreview.put("ownDefCardIds", plResOwnIds);
                    socket.emit("lootPreview", plPreview);
                  } catch (JSONException ex) { ex.printStackTrace(); }
                }
                gameState.setUpdateState(true);
              }
            });
            gameStage.addActor(resBtn);
          }
          break;
        }
      }
    }

    // Loot watcher overlay — shown to non-looting players when another player is looting
    if (pendingLootBroadcast != null && !currentPlayer.getPlayerTurn().isLootPending()) {
      try {
        final int plBcAtkIdx = pendingLootBroadcast.getInt("attackerIdx");
        final boolean plBcSuccess = pendingLootBroadcast.getBoolean("success");
        final boolean plBcKingUsed = pendingLootBroadcast.optBoolean("kingUsed", false);
        final int plBcKingCardId = pendingLootBroadcast.optInt("kingCardId", -1);
        final int plBcDefCardId = pendingLootBroadcast.optInt("defCardId", -1);
        final int plBcAtkSum = pendingLootBroadcast.optInt("attackSum", 0)
            + pendingLootBroadcast.optInt("reservistBonus", 0);
        final int plBcDefStr = pendingLootBroadcast.optInt("defStrength", 0);
        final JSONArray plBcAtkIds = pendingLootBroadcast.optJSONArray("attackCardIds");
        final JSONArray plBcOwnDefIds = pendingLootBroadcast.optJSONArray("ownDefCardIds");

        Image wPlOverlay = new Image(MyGdxGame.skin, "white");
        wPlOverlay.setFillParent(true);
        wPlOverlay.setColor(0f, 0f, 0f, 0.55f);
        gameStage.addActor(wPlOverlay);

        String plAtkName = gameState.getPlayers().get(plBcAtkIdx).getPlayerName();

        Card wPlRef = new Card();
        float wPlCW = wPlRef.getDefWidth() * 1.5f;
        float wPlCH = wPlRef.getDefHeight() * 1.5f;
        float wPlBotY = 265f;
        float wPlLeftX = 10f;
        float wPlRightX = MyGdxGame.WIDTH / 2f + 10f;

        // Column headers
        Label wPlAtkHdr = new Label(plAtkName + " loots:", MyGdxGame.skin);
        wPlAtkHdr.setColor(Color.CYAN);
        wPlAtkHdr.setPosition(wPlLeftX, wPlBotY + wPlCH + 22f);
        gameStage.addActor(wPlAtkHdr);
        Label wPlDefHdr = new Label("Harvest deck:", MyGdxGame.skin);
        wPlDefHdr.setColor(Color.ORANGE);
        wPlDefHdr.setPosition(wPlRightX, wPlBotY + wPlCH + 22f);
        gameStage.addActor(wPlDefHdr);

        // Attack cards (left column)
        ArrayList<Integer> wPlAtkCardIds = new ArrayList<Integer>();
        if (plBcKingUsed && plBcKingCardId > 0) {
          wPlAtkCardIds.add(plBcKingCardId);
        } else {
          if (plBcAtkIds != null) for (int ai = 0; ai < plBcAtkIds.length(); ai++) wPlAtkCardIds.add(plBcAtkIds.getInt(ai));
          if (plBcOwnDefIds != null) for (int ai = 0; ai < plBcOwnDefIds.length(); ai++) wPlAtkCardIds.add(plBcOwnDefIds.getInt(ai));
        }
        int nWPA = Math.max(1, wPlAtkCardIds.size());
        float wPAW = Math.min(wPlCW, (MyGdxGame.WIDTH / 2f - 20f - (nWPA - 1) * 4f) / nWPA);
        float wPAH = wPlCH * (wPAW / wPlCW);
        for (int ai = 0; ai < wPlAtkCardIds.size(); ai++) {
          Card disp = Card.fromCardId(wPlAtkCardIds.get(ai));
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(wPAW, wPAH);
          disp.setPosition(wPlLeftX + ai * (wPAW + 4f), wPlBotY + (wPlCH - wPAH) / 2f);
          gameStage.addActor(disp);
        }

        // Defense card (right column)
        if (plBcDefCardId > 0) {
          Card wDefDisp = Card.fromCardId(plBcDefCardId);
          wDefDisp.setCovered(false); wDefDisp.setActive(true);
          wDefDisp.setSize(wPlCW, wPlCH);
          wDefDisp.setPosition(wPlRightX, wPlBotY);
          gameStage.addActor(wDefDisp);
        }

        // Sum labels
        Label wPlAtkSumLbl = new Label("Sum: " + plBcAtkSum, MyGdxGame.skin);
        wPlAtkSumLbl.setColor(Color.WHITE);
        wPlAtkSumLbl.setPosition(wPlLeftX, wPlBotY - 22f);
        gameStage.addActor(wPlAtkSumLbl);
        Label wPlDefSumLbl = new Label("Sum: " + plBcDefStr, MyGdxGame.skin);
        wPlDefSumLbl.setColor(Color.WHITE);
        wPlDefSumLbl.setPosition(wPlRightX, wPlBotY - 22f);
        gameStage.addActor(wPlDefSumLbl);

        Label wPlResultLbl = new Label(plBcSuccess ? plAtkName + " loots!" : plAtkName + " fails!", MyGdxGame.skin);
        wPlResultLbl.setColor(plBcSuccess ? Color.GREEN : Color.RED);
        wPlResultLbl.setPosition(MyGdxGame.WIDTH / 2f - wPlResultLbl.getPrefWidth() / 2f, wPlBotY - 44f);
        gameStage.addActor(wPlResultLbl);
        Label wPlFooter = new Label("Waiting for " + plAtkName + " to confirm...", MyGdxGame.skin);
        wPlFooter.setColor(Color.YELLOW);
        wPlFooter.setPosition(MyGdxGame.WIDTH / 2f - wPlFooter.getPrefWidth() / 2f, wPlBotY - 66f);
        gameStage.addActor(wPlFooter);
      } catch (JSONException e) { e.printStackTrace(); }
    }

    // Defense-attack preview overlay — added LAST so it renders on top
    if (currentPlayer.getPlayerTurn().isAttackPending()) {
      final Player atkPlayer = currentPlayer;
      final PlayerTurn apt = atkPlayer.getPlayerTurn();
      final boolean atkSuccess = apt.isAttackSuccess();
      final boolean targetIsKing = apt.isAttackTargetIsKing();

      final boolean batteryWaiting = apt.isBatteryWaiting();

      Image atkOverlay = new Image(MyGdxGame.skin, "white");
      atkOverlay.setFillParent(true);
      atkOverlay.setColor(0f, 0f, 0f, 0.45f);
      atkOverlay.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          // If waiting for Battery Tower defender decision, block dismiss
          if (batteryWaiting) return;
          // Discard attacking hand cards (empty for king attacks)
          for (Card c : apt.getPendingAttackCards()) {
            atkPlayer.getHandCards().remove(c);
            gameState.getCemeteryDeck().addCard(c);
          }
          // Discard own def cards used as attackers (Banneret)
          for (Card c : apt.getPendingAttackOwnDefCards()) {
            int slot = c.getPositionId();
            atkPlayer.getDefCards().remove(slot);
            atkPlayer.getTopDefCards().remove(slot);
            gameState.getCemeteryDeck().addCard(c);
          }
          if (targetIsKing) {
            // King-on-king or hand-cards-on-king attack
            if (atkSuccess) {
              // Defender is eliminated; state update from server will reflect this
              Player defender = gameState.getPlayers().get(apt.getAttackTargetPlayerIdx());
              defender.setOut(true);
              if (apt.isKingUsed()) atkPlayer.getKingCard().setCovered(false);
            } else {
              if (apt.isKingUsed()) {
                atkPlayer.getKingCard().setCovered(false);
                atkPlayer.setOut(true);
              }
            }
            try {
              JSONObject emitData = new JSONObject();
              emitData.put("attackerIdx", gameState.getCurrentPlayerIndex());
              emitData.put("defenderIdx", apt.getAttackTargetPlayerIdx());
              emitData.put("success", atkSuccess);
              emitData.put("kingUsed", apt.isKingUsed());
              JSONArray atkIds = new JSONArray();
              for (Card c : apt.getPendingAttackCards()) { atkIds.put(c.getCardId()); }
              emitData.put("attackCardIds", atkIds);
              socket.emit("kingAttackResolved", emitData);
              tutorialAdvance(TUTORIAL_STEP_KING_ATTACK);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          } else {
            // Regular defense card attack
            if (atkSuccess) {
              for (Card dc : apt.getPendingAttackDefCards()) {
                dc.setRemoved(true);
                atkPlayer.addHandCard(dc);
                // Lock captured card as a prey card immediately (before server stateUpdate arrives)
                // so the re-render does not show it as usable. Server confirms on stateUpdate.
                atkPlayer.getPlayerTurn().getPreyCardIds().add(dc.getCardId());
              }
              if (apt.isKingUsed()) atkPlayer.getKingCard().setCovered(false);
            } else {
              if (apt.isKingUsed()) {
                atkPlayer.getKingCard().setCovered(false);
                atkPlayer.setOut(true);
              }
            }
            try {
              JSONObject emitData = new JSONObject();
              emitData.put("attackerIdx", gameState.getCurrentPlayerIndex());
              emitData.put("targetPlayerIdx", apt.getAttackTargetPlayerIdx());
              emitData.put("positionId", apt.getAttackTargetPositionId());
              emitData.put("level", apt.getAttackTargetLevel());
              emitData.put("success", atkSuccess);
              emitData.put("kingUsed", apt.isKingUsed());
              JSONArray atkIds = new JSONArray();
              for (Card c : apt.getPendingAttackCards()) { atkIds.put(c.getCardId()); }
              emitData.put("attackCardIds", atkIds);
              JSONArray ownDefIds = new JSONArray();
              for (Card c : apt.getPendingAttackOwnDefCards()) { ownDefIds.put(c.getCardId()); }
              emitData.put("attackerOwnDefCardIds", ownDefIds);
              socket.emit("defAttackResolved", emitData);
              tutorialAdvance(TUTORIAL_STEP_KING_ATTACK);
            } catch (JSONException e) {
              e.printStackTrace();
            }
            apt.getPendingAttackDefCards().clear();
            apt.getPendingAttackOwnDefCards().clear();
          }
          // Coup swap: if the old king (now a hand card) was used in this attack, mark king as spent.
          // Must check BEFORE clearing pendingAttackCards.
          int coupId2 = apt.getCoupSwapPendingCardId();
          if (coupId2 != -1) {
            for (Card c : apt.getPendingAttackCards()) {
              if (c.getCardId() == coupId2) {
                apt.setKingUsedThisTurn(true);
                apt.setCoupSwapPendingCardId(-1);
                break;
              }
            }
          }
          apt.getPendingAttackCards().clear();
          apt.resetReservistAttackBonus();
          apt.resetPendingAttackMercenaryBonus();
          apt.setAttackPending(false);
          apt.setAttackTargetIsKing(false);
          // Mark king as spent only for normal king attacks. Warlord direct attacks
          // are an additional action granted by the hero and must NOT consume the
          // regular once-per-turn king attack/loot.
          if (apt.isKingUsed() && !apt.isPendingAttackIsWarlord()) apt.setKingUsedThisTurn(true);
          // Count this attack locally (same pattern as Warlord) so the expose-card penalty
          // check in FinishTurnButtonListener never falsely fires before the server stateUpdate arrives.
          if (!apt.isPendingAttackIsWarlord()) apt.increaseAttackCounter();
          apt.setPendingAttackIsWarlord(false);
          // Clear hand card attack boost visuals after attack resolves
          for (Card c : atkPlayer.getHandCards()) {
            c.setSelected(false);
            while (c.getBoosted() > 0) c.addBoosted(-1);
          }
          // Clear own def card selections
          for (Card c : atkPlayer.getDefCards().values()) c.setSelected(false);
          for (Card c : atkPlayer.getTopDefCards().values()) c.setSelected(false);
          if (atkPlayer.getKingCard() != null) atkPlayer.getKingCard().setSelected(false);
          gameState.setUpdateState(true);
        }
      });

      String normalText = targetIsKing
          ? (atkSuccess ? "KING DEFEATED!  Tap to claim." : "KING ATTACK FAILED.  Tap to continue.")
          : (atkSuccess ? "ATTACK SUCCESS!  Tap to claim the defense card." : "ATTACK FAILED.  Tap to continue.");
      String resultText = batteryWaiting ? "Waiting for defender..." : normalText;
      Label atkResultLabel = new Label(resultText, MyGdxGame.skin);
      atkResultLabel.setColor(batteryWaiting ? Color.YELLOW : (atkSuccess ? Color.GREEN : Color.RED));

      // Battle visualization — card layout constants
      Card refCardAtk = new Card();
      float bCW = refCardAtk.getDefWidth() * 1.5f;
      float bCH = refCardAtk.getDefHeight() * 1.5f;
      float cBotY = 265f;   // bottom of the card row in the overlay
      float leftX = 10f;
      float rightX = MyGdxGame.WIDTH / 2f + 10f;

      // Attack sum (for label)
      int atkVizSum;
      if (apt.isKingUsed() && atkPlayer.getKingCard() != null) {
        atkVizSum = atkPlayer.getKingCard().getStrength();
      } else {
        atkVizSum = 0;
        for (Card ac : apt.getPendingAttackCards()) atkVizSum += ac.getStrength();
        for (Card ac : apt.getPendingAttackOwnDefCards()) atkVizSum += ac.getStrength();
      }
      atkVizSum += apt.getReservistAttackBonus();
        atkVizSum += apt.getPendingAttackMercenaryBonus();
      // Defense sum (for label)
      int defVizSum = 0;
      ArrayList<Card> pendingDefViz = apt.getPendingAttackDefCards();
      if (targetIsKing) {
        Player defKingPlayer = gameState.getPlayers().get(apt.getAttackTargetPlayerIdx());
        if (defKingPlayer != null && defKingPlayer.getKingCard() != null) {
          defVizSum = defKingPlayer.getKingCard().getStrength();
        }
      } else {
        for (Card dc : pendingDefViz) defVizSum += "joker".equals(dc.getSymbol()) ? 1 : dc.getStrength();
      }

      atkResultLabel.setPosition(
          MyGdxGame.WIDTH / 2f - atkResultLabel.getPrefWidth() / 2f,
          cBotY - 44f);

      gameStage.addActor(atkOverlay);

      // Column headers
      Label atkHdrLbl = new Label("ATTACK", MyGdxGame.skin);
      atkHdrLbl.setColor(Color.CYAN);
      atkHdrLbl.setPosition(leftX, cBotY + bCH + 5f);
      gameStage.addActor(atkHdrLbl);
      Label defHdrLbl = new Label("DEFENSE", MyGdxGame.skin);
      defHdrLbl.setColor(Color.ORANGE);
      defHdrLbl.setPosition(rightX, cBotY + bCH + 5f);
      gameStage.addActor(defHdrLbl);

      // Attack cards (left column)
      if (apt.isKingUsed() && atkPlayer.getKingCard() != null) {
        Card kDisp = Card.fromCardId(atkPlayer.getKingCard().getCardId());
        kDisp.setCovered(false); kDisp.setActive(true);
        kDisp.setSize(bCW, bCH);
        kDisp.setPosition(leftX, cBotY);
        gameStage.addActor(kDisp);
      } else {
        ArrayList<Card> atkSrc = new ArrayList<Card>(apt.getPendingAttackCards());
        atkSrc.addAll(apt.getPendingAttackOwnDefCards());
        int nA = Math.max(1, atkSrc.size());
        float aW = Math.min(bCW, (MyGdxGame.WIDTH / 2f - 20f - (nA - 1) * 4f) / nA);
        float aH = bCH * (aW / bCW);
        float lastAX = leftX; float lastAY = cBotY + (bCH - aH) / 2f;
        for (int ai = 0; ai < atkSrc.size(); ai++) {
          Card disp = Card.fromCardId(atkSrc.get(ai).getCardId());
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(aW, aH);
          lastAX = leftX + ai * (aW + 4f); lastAY = cBotY + (bCH - aH) / 2f;
          disp.setPosition(lastAX, lastAY);
          gameStage.addActor(disp);
        }
        // Attacker mercenary bonus indicator — centered on the last attack card
        int atkMercViz = apt.getPendingAttackMercenaryBonus();
        if (atkMercViz > 0) {
          float iSz = aH / 3f;
          TextureRegion mReg = new TextureRegion(texMercenary, 0, 0, 512, 512);
          Image mImg = new Image(mReg);
          mImg.setSize(iSz, iSz);
          mImg.setPosition(lastAX + aW / 2f - iSz / 2f, lastAY + aH / 2f - iSz / 2f);
          mImg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          gameStage.addActor(mImg);
          Label mLbl = new Label("+" + atkMercViz, MyGdxGame.skin);
          mLbl.setColor(Color.GOLD);
          mLbl.setPosition(lastAX + aW / 2f - iSz / 2f + iSz + 2f, lastAY + aH / 2f - iSz / 2f);
          mLbl.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          gameStage.addActor(mLbl);
        }
      }

      // Defense cards (right column)
      if (targetIsKing) {
        Player defKP = gameState.getPlayers().get(apt.getAttackTargetPlayerIdx());
        if (defKP != null && defKP.getKingCard() != null) {
          Card kd = Card.fromCardId(defKP.getKingCard().getCardId());
          kd.setCovered(false); kd.setActive(true);
          kd.setSize(bCW, bCH);
          kd.setPosition(rightX, cBotY);
          gameStage.addActor(kd);
        }
      } else {
        int nD = Math.max(1, pendingDefViz.size());
        float dW = Math.min(bCW, (MyGdxGame.WIDTH / 2f - 20f - (nD - 1) * 4f) / nD);
        float dH = bCH * (dW / bCW);
        for (int di = 0; di < pendingDefViz.size(); di++) {
          Card dc = pendingDefViz.get(di);
          Card disp = Card.fromCardId(dc.getCardId());
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(dW, dH);
          float dDispX = rightX + di * (dW + 4f);
          float dDispY = cBotY + (bCH - dH) / 2f;
          disp.setPosition(dDispX, dDispY);
          gameStage.addActor(disp);
          // Per-card defender mercenary boost indicator
          int dcBoost = dc.getBoosted();
          if (dcBoost > 0) {
            float iSz = dH / 3f;
            TextureRegion mReg = new TextureRegion(texMercenary, 0, 0, 512, 512);
            Image mImg = new Image(mReg);
            mImg.setSize(iSz, iSz);
            mImg.setPosition(dDispX + dW / 2f - iSz / 2f, dDispY + dH / 2f - iSz / 2f);
            mImg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            gameStage.addActor(mImg);
            Label mLbl = new Label("+" + dcBoost, MyGdxGame.skin);
            mLbl.setColor(Color.GOLD);
            mLbl.setPosition(dDispX + dW / 2f - iSz / 2f + iSz + 2f, dDispY + dH / 2f - iSz / 2f);
            mLbl.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            gameStage.addActor(mLbl);
          }
        }
      }

      // Sum labels
      Label atkSumLbl = new Label("Sum: " + atkVizSum, MyGdxGame.skin);
      atkSumLbl.setColor(Color.WHITE);
      atkSumLbl.setPosition(leftX, cBotY - 22f);
      gameStage.addActor(atkSumLbl);
      Label defSumLbl = new Label("Sum: " + defVizSum, MyGdxGame.skin);
      defSumLbl.setColor(Color.WHITE);
      defSumLbl.setPosition(rightX, cBotY - 22f);
      gameStage.addActor(defSumLbl);

      gameStage.addActor(atkResultLabel);

      // Reservists attack boost button — shown only when not waiting for Battery Tower
      // and only when the attack is currently failing but spending reservists can flip it
      if (!batteryWaiting) {
        for (Hero resH : atkPlayer.getHeroes()) {
          if ("Reservists".equals(resH.getHeroName())) {
            final Reservists resHero = (Reservists) resH;
            // Compute effective attack base and def strength for the "can flip" check
            int atkBase;
            int defStrCheck;
            ArrayList<Card> pendingDefCards = apt.getPendingAttackDefCards();
            if (!pendingDefCards.isEmpty()) {
              // Regular def card attack: sum hand cards + own def cards used; compute defStr from stored def cards
              atkBase = 0;
              for (Card ac : apt.getPendingAttackCards()) atkBase += ac.getStrength();
              for (Card ac : apt.getPendingAttackOwnDefCards()) atkBase += ac.getStrength();
              defStrCheck = 0;
              for (Card dc : pendingDefCards) defStrCheck += "joker".equals(dc.getSymbol()) ? 1 : dc.getStrength();
            } else {
              // King attack: use stored base sums set by EnemyKingCardListener
              atkBase = apt.getPendingAttackBaseSum();
              defStrCheck = apt.getPendingAttackDefStr();
            }
            boolean canFlipAttack = !atkSuccess && resHero.isAvailable()
                && (atkBase + apt.getPendingAttackMercenaryBonus() + apt.getReservistAttackBonus() + resHero.countReady()) > defStrCheck;
            if (canFlipAttack) {
              TextButton resBtn = new TextButton("Reservists +1  (" + resHero.countReady() + " left)", MyGdxGame.skin);
              resBtn.pack();
              resBtn.setPosition(MyGdxGame.WIDTH / 2f - resBtn.getWidth() / 2f, MyGdxGame.WIDTH * 0.42f);
              resBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                  resHero.spend();
                  emitReservistsKingBoost(resHero.countReady());
                  apt.incrementReservistAttackBonus();
                  // Recalculate from frozen snapshots — do NOT call attackEnemyDefense() here
                  // because applyStateUpdate may have already rebuilt hand cards (losing isSelected).
                  int newAtkSum;
                  if (apt.isKingUsed() && atkPlayer.getKingCard() != null) {
                    newAtkSum = atkPlayer.getKingCard().getStrength();
                  } else {
                    newAtkSum = 0;
                    for (Card snapC : apt.getPendingAttackCards()) newAtkSum += snapC.getStrength();
                    for (Card snapC : apt.getPendingAttackOwnDefCards()) newAtkSum += snapC.getStrength();
                  }
                  newAtkSum += apt.getPendingAttackMercenaryBonus();
                  newAtkSum += apt.getReservistAttackBonus();
                  ArrayList<Card> defCards = apt.getPendingAttackDefCards();
                  int newDefStr = 0;
                  for (Card dc : defCards) newDefStr += "joker".equals(dc.getSymbol()) ? 1 : dc.getStrength();
                  boolean newSuccess = defCards.isEmpty()
                      ? (apt.getPendingAttackBaseSum() + apt.getPendingAttackMercenaryBonus() + apt.getReservistAttackBonus()) > apt.getPendingAttackDefStr()
                      : newAtkSum > newDefStr;
                  apt.setAttackSuccess(newSuccess);
                  // Re-emit attackPreview so defenders/watchers see the updated sum and outcome
                  if (socket != null) {
                    try {
                      JSONObject resPreview = new JSONObject();
                      resPreview.put("attackerIdx", playerIndex);
                      resPreview.put("defenderIdx", apt.getAttackTargetPlayerIdx());
                      resPreview.put("positionId", apt.getAttackTargetPositionId());
                      resPreview.put("level", apt.getAttackTargetLevel());
                      JSONArray rpAtkIds = new JSONArray();
                      for (Card c : apt.getPendingAttackCards()) rpAtkIds.put(c.getCardId());
                      resPreview.put("attackCardIds", rpAtkIds);
                      JSONArray rpOwnDefIds = new JSONArray();
                      for (Card c : apt.getPendingAttackOwnDefCards()) rpOwnDefIds.put(c.getCardId());
                      resPreview.put("ownDefCardIds", rpOwnDefIds);
                      JSONArray rpDefIds = new JSONArray();
                      for (Card c : apt.getPendingAttackDefCards()) rpDefIds.put(c.getCardId());
                      resPreview.put("defCardIds", rpDefIds);
                      resPreview.put("kingUsed", apt.isKingUsed());
                      resPreview.put("kingCardId", apt.isKingUsed() && atkPlayer.getKingCard() != null ? atkPlayer.getKingCard().getCardId() : -1);
                      resPreview.put("mercenaryBonus", apt.getPendingAttackMercenaryBonus());
                      int defMercBonusRes = 0;
                      for (Card dc : apt.getPendingAttackDefCards()) defMercBonusRes += dc.getBoosted();
                      resPreview.put("defMercBonus", defMercBonusRes);
                      JSONArray defBoostsRes = new JSONArray();
                      for (Card dc : apt.getPendingAttackDefCards()) defBoostsRes.put(dc.getBoosted());
                      resPreview.put("defCardBoosts", defBoostsRes);
                      resPreview.put("reservistBonus", apt.getReservistAttackBonus());
                      resPreview.put("success", newSuccess);
                      resPreview.put("attackingSymbol", apt.getAttackingSymbol()[0]);
                      resPreview.put("attackingSymbol2", apt.getAttackingSymbol()[1]);
                      socket.emit("attackPreview", resPreview);
                    } catch (JSONException ex) { ex.printStackTrace(); }
                  }
                  gameState.setUpdateState(true);
                }
              });
              gameStage.addActor(resBtn);
            }
            break;
          }
        }
      }
    }

    // Defender / watcher overlay — shown when another player has a pending attack
    if (pendingAttackBroadcast != null && !currentPlayer.getPlayerTurn().isAttackPending()) {
      try {
        final int bcAtkIdx = pendingAttackBroadcast.getInt("attackerIdx");
        final int bcDefIdx = pendingAttackBroadcast.getInt("defenderIdx");
        final boolean bcSuccess = pendingAttackBroadcast.getBoolean("success");
        final boolean bcKingUsed = pendingAttackBroadcast.optBoolean("kingUsed", false);
        final int bcKingCardId = pendingAttackBroadcast.optInt("kingCardId", -1);
        final int bcMercBonus = pendingAttackBroadcast.optInt("mercenaryBonus", 0);
        final int bcDefMercBonus = pendingAttackBroadcast.optInt("defMercBonus", 0);
        final int bcResBonus = pendingAttackBroadcast.optInt("reservistBonus", 0);
        final JSONArray bcAtkIds = pendingAttackBroadcast.optJSONArray("attackCardIds");
        final JSONArray bcOwnDefIds = pendingAttackBroadcast.optJSONArray("ownDefCardIds");
        final JSONArray bcDefIds = pendingAttackBroadcast.optJSONArray("defCardIds");
        final JSONArray bcDefBoosts = pendingAttackBroadcast.optJSONArray("defCardBoosts");

        Image watchOverlay = new Image(MyGdxGame.skin, "white");
        watchOverlay.setFillParent(true);
        watchOverlay.setColor(0f, 0f, 0f, 0.55f);
        gameStage.addActor(watchOverlay);

        String atkName = gameState.getPlayers().get(bcAtkIdx).getPlayerName();
        String defName = gameState.getPlayers().get(bcDefIdx).getPlayerName();

        Card wRefCard = new Card();
        float wCW = wRefCard.getDefWidth() * 1.5f;
        float wCH = wRefCard.getDefHeight() * 1.5f;
        float wBotY = 265f;
        float wLeftX = 10f;
        float wRightX = MyGdxGame.WIDTH / 2f + 10f;

        // Column headers
        Label wAtkHdr = new Label(atkName + " attacks:", MyGdxGame.skin);
        wAtkHdr.setColor(Color.CYAN);
        wAtkHdr.setPosition(wLeftX, wBotY + wCH + 22f);
        gameStage.addActor(wAtkHdr);
        Label wDefHdr = new Label(defName + " defends:", MyGdxGame.skin);
        wDefHdr.setColor(Color.ORANGE);
        wDefHdr.setPosition(wRightX, wBotY + wCH + 22f);
        gameStage.addActor(wDefHdr);

        // Attack cards (left column)
        ArrayList<Integer> wAtkCardIds = new ArrayList<Integer>();
        if (bcKingUsed && bcKingCardId > 0) {
          wAtkCardIds.add(bcKingCardId);
        } else {
          if (bcAtkIds != null) for (int ai = 0; ai < bcAtkIds.length(); ai++) wAtkCardIds.add(bcAtkIds.getInt(ai));
          if (bcOwnDefIds != null) for (int ai = 0; ai < bcOwnDefIds.length(); ai++) wAtkCardIds.add(bcOwnDefIds.getInt(ai));
        }
        int wAtkSum = 0;
        int nWA = Math.max(1, wAtkCardIds.size());
        float wAW = Math.min(wCW, (MyGdxGame.WIDTH / 2f - 20f - (nWA - 1) * 4f) / nWA);
        float wAH = wCH * (wAW / wCW);
        for (int ai = 0; ai < wAtkCardIds.size(); ai++) {
          Card disp = Card.fromCardId(wAtkCardIds.get(ai));
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(wAW, wAH);
          disp.setPosition(wLeftX + ai * (wAW + 4f), wBotY + (wCH - wAH) / 2f);
          gameStage.addActor(disp);
          wAtkSum += disp.getStrength();
        }
        wAtkSum += bcMercBonus;
        wAtkSum += bcResBonus;
        // Attacker mercenary bonus indicator — centered on the last attack card
        if (bcMercBonus > 0 && !wAtkCardIds.isEmpty()) {
          int lastWAI = wAtkCardIds.size() - 1;
          float lastWAX = wLeftX + lastWAI * (wAW + 4f);
          float lastWAY = wBotY + (wCH - wAH) / 2f;
          float iSz = wAH / 3f;
          TextureRegion mReg = new TextureRegion(texMercenary, 0, 0, 512, 512);
          Image mImg = new Image(mReg);
          mImg.setSize(iSz, iSz);
          mImg.setPosition(lastWAX + wAW / 2f - iSz / 2f, lastWAY + wAH / 2f - iSz / 2f);
          mImg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          gameStage.addActor(mImg);
          Label mLbl = new Label("+" + bcMercBonus, MyGdxGame.skin);
          mLbl.setColor(Color.GOLD);
          mLbl.setPosition(lastWAX + wAW / 2f - iSz / 2f + iSz + 2f, lastWAY + wAH / 2f - iSz / 2f);
          mLbl.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          gameStage.addActor(mLbl);
        }

        // Defense cards (right column)
        ArrayList<Integer> wDefCardIds = new ArrayList<Integer>();
        if (bcDefIds != null) for (int di = 0; di < bcDefIds.length(); di++) wDefCardIds.add(bcDefIds.getInt(di));
        int wDefSum = 0;
        int nWD = Math.max(1, wDefCardIds.size());
        float wDW = Math.min(wCW, (MyGdxGame.WIDTH / 2f - 20f - (nWD - 1) * 4f) / nWD);
        float wDH = wCH * (wDW / wCW);
        for (int di = 0; di < wDefCardIds.size(); di++) {
          Card disp = Card.fromCardId(wDefCardIds.get(di));
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(wDW, wDH);
          float dispX = wRightX + di * (wDW + 4f);
          float dispY = wBotY + (wCH - wDH) / 2f;
          disp.setPosition(dispX, dispY);
          gameStage.addActor(disp);
          wDefSum += "joker".equals(disp.getSymbol()) ? 1 : disp.getStrength();
          // Per-card mercenary boost indicator
          int cardBoost = (bcDefBoosts != null && di < bcDefBoosts.length()) ? bcDefBoosts.getInt(di) : 0;
          if (cardBoost > 0) {
            float iSz = wDH / 3f;
            TextureRegion mReg = new TextureRegion(texMercenary, 0, 0, 512, 512);
            Image mImg = new Image(mReg);
            mImg.setSize(iSz, iSz);
            mImg.setPosition(dispX + wDW / 2f - iSz / 2f, dispY + wDH / 2f - iSz / 2f);
            mImg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            gameStage.addActor(mImg);
            Label mLbl = new Label("+" + cardBoost, MyGdxGame.skin);
            mLbl.setColor(Color.GOLD);
            mLbl.setPosition(dispX + wDW / 2f - iSz / 2f + iSz + 2f, dispY + wDH / 2f - iSz / 2f);
            mLbl.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            gameStage.addActor(mLbl);
          }
        }
        // Issue #167: defender's mercenary boost (defense cards rendered via
        // Card.fromCardId have no boost, so add it explicitly).
        wDefSum += bcDefMercBonus;

        // Sum labels
        Label wAtkSum_lbl = new Label("Sum: " + wAtkSum, MyGdxGame.skin);
        wAtkSum_lbl.setColor(Color.WHITE);
        wAtkSum_lbl.setPosition(wLeftX, wBotY - 22f);
        gameStage.addActor(wAtkSum_lbl);
        Label wDefSum_lbl = new Label("Sum: " + wDefSum, MyGdxGame.skin);
        wDefSum_lbl.setColor(Color.WHITE);
        wDefSum_lbl.setPosition(wRightX, wBotY - 22f);
        gameStage.addActor(wDefSum_lbl);

        // Result and footer
        Label wResultLbl = new Label(bcSuccess ? atkName + " WINS!" : atkName + " FAILS!", MyGdxGame.skin);
        wResultLbl.setColor(bcSuccess ? Color.GREEN : Color.RED);
        wResultLbl.setPosition(MyGdxGame.WIDTH / 2f - wResultLbl.getPrefWidth() / 2f, wBotY - 44f);
        gameStage.addActor(wResultLbl);
        Label wFooter = new Label("Waiting for " + atkName + " to confirm...", MyGdxGame.skin);
        wFooter.setColor(Color.YELLOW);
        wFooter.setPosition(MyGdxGame.WIDTH / 2f - wFooter.getPrefWidth() / 2f, wBotY - 66f);
        gameStage.addActor(wFooter);
      } catch (JSONException e) { e.printStackTrace(); }
    }

    // Battery Tower bot notification — shown briefly to the attacker
    if (batteryBotNotification != null) {
      Image btNotifOverlay = new Image(MyGdxGame.skin, "white");
      btNotifOverlay.setFillParent(true);
      btNotifOverlay.setColor(0f, 0f, 0.3f, 0.75f);
      gameStage.addActor(btNotifOverlay);
      Label btNotifLabel = new Label(batteryBotNotification, MyGdxGame.skin);
      btNotifLabel.setColor(Color.YELLOW);
      btNotifLabel.setFontScale(1.15f);
      btNotifLabel.pack();
      btNotifLabel.setPosition(MyGdxGame.WIDTH / 2f - btNotifLabel.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.5f);
      gameStage.addActor(btNotifLabel);
    }

    // Battery Tower defender overlay — shown when this player must allow or deny an attack
    if (pendingBatteryDefCheck != null) {
      final JSONObject btCheck = pendingBatteryDefCheck;
      Image btOverlay = new Image(MyGdxGame.skin, "white");
      btOverlay.setFillParent(true);
      btOverlay.setColor(0f, 0f, 0.4f, 0.7f);
      gameStage.addActor(btOverlay);

      Label btTitle = new Label("Battery Tower! Incoming attack — fire?", MyGdxGame.skin);
      btTitle.setColor(Color.YELLOW);
      btTitle.setPosition(MyGdxGame.WIDTH / 2f - btTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.6f);
      gameStage.addActor(btTitle);

      TextButton allowBtn = new TextButton("Allow", MyGdxGame.skin);
      allowBtn.setWidth(MyGdxGame.WIDTH / 4f);
      allowBtn.setPosition(MyGdxGame.WIDTH / 2f - allowBtn.getWidth() - 8f, MyGdxGame.WIDTH * 0.5f);
      allowBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          try {
            pendingBatteryResultCards = btCheck.optJSONArray("attackCardIds");
            pendingBatteryDefCheck = null;
            JSONObject resp = new JSONObject();
            resp.put("attackerIdx", btCheck.getInt("attackerIdx"));
            socket.emit("batteryAllowAttack", resp);
          } catch (JSONException e) { e.printStackTrace(); }
          gameState.setUpdateState(true);
        }
      });
      gameStage.addActor(allowBtn);

      TextButton denyBtn = new TextButton("Deny (Fire!)", MyGdxGame.skin);
      denyBtn.setWidth(MyGdxGame.WIDTH / 4f);
      denyBtn.setPosition(MyGdxGame.WIDTH / 2f + 8f, MyGdxGame.WIDTH * 0.5f);
      denyBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          // Spend the charge
          Player me = gameState.getPlayers().get(playerIndex);
          for (int i = 0; i < me.getHeroes().size(); i++) {
            if (me.getHeroes().get(i).getHeroName() == "Battery Tower") {
              ((BatteryTower) me.getHeroes().get(i)).fire();
              break;
            }
          }
          try {
            JSONObject resp = new JSONObject();
            resp.put("attackerIdx", btCheck.getInt("attackerIdx"));
            resp.put("targetPlayerIdx", btCheck.getInt("targetPlayerIdx"));
            resp.put("positionId", btCheck.getInt("positionId"));
            resp.put("isKing", btCheck.optBoolean("isKing", false));
            JSONArray atkExpIds = btCheck.optJSONArray("attackCardIds");
            if (atkExpIds != null) resp.put("attackCardIds", atkExpIds);
            socket.emit("batteryDenyAttack", resp);
            pendingBatteryResultCards = atkExpIds;
            pendingBatteryDefCheck = null;
          } catch (JSONException e) { e.printStackTrace(); }
          gameState.setUpdateState(true);
        }
      });
      gameStage.addActor(denyBtn);
    }

    // Battery Tower result overlay — shown to the defender after they allow or deny
    if (pendingBatteryResultCards != null) {
      final JSONArray resultCards = pendingBatteryResultCards;
      Image btResOverlay = new Image(MyGdxGame.skin, "white");
      btResOverlay.setFillParent(true);
      btResOverlay.setColor(0f, 0f, 0f, 0.6f);
      gameStage.addActor(btResOverlay);

      Label btResTitle = new Label("Attack cards used:", MyGdxGame.skin);
      btResTitle.setColor(Color.YELLOW);
      btResTitle.setPosition(MyGdxGame.WIDTH / 2f - btResTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.62f);
      gameStage.addActor(btResTitle);

      try {
        Card sampleCard = new Card();
        float cw = sampleCard.getDefWidth() * 1.5f;
        float ch = sampleCard.getDefHeight() * 1.5f;
        float totalW = resultCards.length() * cw + (resultCards.length() - 1) * 4f;
        float startX = MyGdxGame.WIDTH / 2f - totalW / 2f;
        float cardY = MyGdxGame.WIDTH * 0.68f;
        for (int ai = 0; ai < resultCards.length(); ai++) {
          Card rc = Card.fromCardId(resultCards.getInt(ai));
          rc.setCovered(false);
          rc.setWidth(cw);
          rc.setHeight(ch);
          rc.setPosition(startX + ai * (cw + 4f), cardY);
          gameStage.addActor(rc);
        }
      } catch (JSONException ignored) {}

      TextButton btResDismiss = new TextButton("OK", MyGdxGame.skin);
      btResDismiss.setWidth(MyGdxGame.WIDTH / 4f);
      btResDismiss.setPosition(MyGdxGame.WIDTH / 2f - btResDismiss.getWidth() / 2f, MyGdxGame.WIDTH * 0.5f);
      btResDismiss.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          pendingBatteryResultCards = null;
          gameState.setUpdateState(true);
        }
      });
      gameStage.addActor(btResDismiss);
    }

    // Winner overlay — shown on top of everything when a winner is determined
    if (gameState.getWinnerIndex() >= 0) {
      Image winOverlay = new Image(MyGdxGame.skin, "white");
      winOverlay.setFillParent(true);
      winOverlay.setColor(0f, 0f, 0f, 0.75f);
      gameStage.addActor(winOverlay);

      Player winner = gameState.getPlayers().get(gameState.getWinnerIndex());
      Label winLabel = new Label(winner.getPlayerName() + " WINS!", MyGdxGame.skin);
      winLabel.setColor(Color.GOLD);
      winLabel.setPosition(
          MyGdxGame.WIDTH / 2f - winLabel.getPrefWidth() / 2f,
          MyGdxGame.WIDTH / 2f + winLabel.getPrefHeight());
      gameStage.addActor(winLabel);

      Label restartLabel = new Label("New game starting in 5 seconds...", MyGdxGame.skin);
      restartLabel.setColor(Color.WHITE);
      restartLabel.setPosition(
          MyGdxGame.WIDTH / 2f - restartLabel.getPrefWidth() / 2f,
          MyGdxGame.WIDTH / 2f - restartLabel.getPrefHeight());
      gameStage.addActor(restartLabel);
    }

    // Hero selection overlay — shown when the local player must choose a hero
    // (drawn card was an Ace or another Joker). Renders on top of the game board.
    if (currentPlayer.getPlayerTurn().isHeroSelectionPending()) {
      final PlayerTurn hspt = currentPlayer.getPlayerTurn();
      final java.util.ArrayList<Hero> choices = hspt.getHeroChoices();

      Image hsOverlay = new Image(MyGdxGame.skin, "white");
      hsOverlay.setFillParent(true);
      hsOverlay.setColor(0f, 0f, 0f, 0.78f);
      gameStage.addActor(hsOverlay);

      Label hsTitle = new Label("Choose your Hero:", MyGdxGame.skin);
      hsTitle.setColor(Color.GOLD);
      hsTitle.setPosition(MyGdxGame.WIDTH / 2f - hsTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.78f);
      gameStage.addActor(hsTitle);

      // Layout: compute button width from the widest hero name so text never clips.
      float maxBtnPrefW = 0f;
      for (Hero c : choices) {
        TextButton tb = new TextButton(c.getHeroName(), MyGdxGame.skin);
        maxBtnPrefW = Math.max(maxBtnPrefW, tb.getPrefWidth());
      }
      float btnW = maxBtnPrefW + 20f;
      int numCols = Math.max(1, (int) ((MyGdxGame.WIDTH - 4f) / (btnW + 8f)));
      numCols = Math.min(numCols, choices.size());
      float btnGapX = numCols > 1 ? (MyGdxGame.WIDTH - numCols * btnW) / (numCols - 1) : 0f;
      float startX  = numCols > 1 ? 0f : (MyGdxGame.WIDTH - btnW) / 2f;
      float startY  = MyGdxGame.WIDTH * 0.62f;
      float rowH    = 0f;

      for (int ci = 0; ci < choices.size(); ci++) {
        final Hero choice = choices.get(ci);
        TextButton heroBtn = new TextButton(choice.getHeroName(), MyGdxGame.skin);
        heroBtn.setSize(btnW, heroBtn.getPrefHeight());
        if (rowH == 0f) rowH = heroBtn.getPrefHeight() + 8f;
        int col = ci % numCols;
        int row = ci / numCols;
        heroBtn.setPosition(startX + col * (btnW + btnGapX), startY - row * rowH);
        heroBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            final String heroName = choice.getHeroName();
            Hero consumed = gameState.getHeroesSquare().consumeHeroByName(heroName);
            if (consumed != null) {
              completeHeroAcquisition(consumed);
            } else {
              // Hero is already owned — strip it from the owner and transfer it.
              int ownerIdx = gameState.findHeroOwnerIndex(heroName);
              if (ownerIdx >= 0) {
                Hero stripped = players.get(ownerIdx).removeHeroByName(heroName);
                if (stripped != null) {
                  // Emit heroAcquired BEFORE heroLost so other clients can
                  // still find the hero in the old owner's list inside
                  // applyHeroAcquired and transfer it without it disappearing.
                  completeHeroAcquisition(stripped);
                  try {
                    JSONObject emitData = new JSONObject();
                    emitData.put("playerIndex", ownerIdx);
                    emitData.put("heroName", heroName);
                    socket.emit("heroLost", emitData);
                  } catch (JSONException e) {
                    e.printStackTrace();
                  }
                }
              }
            }
          }
        });
        gameStage.addActor(heroBtn);
      }
    }

    // King-defeat hero selection overlay — shown to the attacker when they must pick one of the
    // defeated player's heroes. Only appears when pendingKingDefeatHeroOptions is populated.
    if (pendingKingDefeatHeroOptions != null && !pendingKingDefeatHeroOptions.isEmpty()) {
      final java.util.ArrayList<String> kdOptions = pendingKingDefeatHeroOptions;

      Image kdOverlay = new Image(MyGdxGame.skin, "white");
      kdOverlay.setFillParent(true);
      kdOverlay.setColor(0f, 0f, 0f, 0.78f);
      gameStage.addActor(kdOverlay);

      Label kdTitle = new Label("Choose a Hero from the defeated player:", MyGdxGame.skin);
      kdTitle.setColor(Color.GOLD);
      kdTitle.setPosition(MyGdxGame.WIDTH / 2f - kdTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.78f);
      gameStage.addActor(kdTitle);

      float maxKdPrefW = 0f;
      for (String n : kdOptions) {
        TextButton tb = new TextButton(n, MyGdxGame.skin);
        maxKdPrefW = Math.max(maxKdPrefW, tb.getPrefWidth());
      }
      float btnW = maxKdPrefW + 20f;
      int numCols = Math.max(1, (int) ((MyGdxGame.WIDTH - 4f) / (btnW + 8f)));
      numCols = Math.min(numCols, kdOptions.size());
      float btnGapX = numCols > 1 ? (MyGdxGame.WIDTH - numCols * btnW) / (numCols - 1) : 0f;
      float startX  = numCols > 1 ? 0f : (MyGdxGame.WIDTH - btnW) / 2f;
      float startY  = MyGdxGame.WIDTH * 0.62f;
      float rowH    = 0f;

      for (int ci = 0; ci < kdOptions.size(); ci++) {
        final String heroName = kdOptions.get(ci);
        TextButton heroBtn = new TextButton(heroName, MyGdxGame.skin);
        heroBtn.setSize(btnW, heroBtn.getPrefHeight());
        if (rowH == 0f) rowH = heroBtn.getPrefHeight() + 8f;
        int col = ci % numCols;
        int row = ci / numCols;
        heroBtn.setPosition(startX + col * (btnW + btnGapX), startY - row * rowH);
        heroBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            pendingKingDefeatHeroOptions = null;
            try {
              JSONObject emitData = new JSONObject();
              emitData.put("heroName", heroName);
              socket.emit("heroSelectedFromKingDefeat", emitData);
            } catch (JSONException e) {
              e.printStackTrace();
            }
            gameState.setUpdateState(true);
          }
        });
        gameStage.addActor(heroBtn);
      }
    }

    // ── Sell Hero setup overlay ─────────────────────────────────────────────
    // Shown when the player is setting up a hero sale (before sending to server).
    if (auctionSellHeroName != null && pendingHeroAuction == null) {
      Image selOv = new Image(MyGdxGame.skin, "white");
      selOv.setFillParent(true);
      selOv.setColor(0f, 0f, 0f, 0.78f);
      gameStage.addActor(selOv);

      if ("__SELECT__".equals(auctionSellHeroName)) {
        // Hero-choice phase — player owns multiple heroes, must pick one to sell
        Label selTitle = new Label("Which hero do you want to sell?", MyGdxGame.skin);
        selTitle.setColor(Color.GOLD);
        selTitle.setPosition(MyGdxGame.WIDTH / 2f - selTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.72f);
        gameStage.addActor(selTitle);

        final ArrayList<Hero> phList = currentPlayer.getHeroes();
        float maxPhPrefW = 0f;
        for (Hero ph : phList) {
          TextButton tb = new TextButton(ph.getHeroName(), MyGdxGame.skin);
          maxPhPrefW = Math.max(maxPhPrefW, tb.getPrefWidth());
        }
        float btnW = maxPhPrefW + 20f;
        int numCols = Math.max(1, (int) ((MyGdxGame.WIDTH - 4f) / (btnW + 8f)));
        numCols = Math.min(numCols, phList.size());
        float btnGapX = numCols > 1 ? (MyGdxGame.WIDTH - numCols * btnW) / (numCols - 1) : 0f;
        float startX  = numCols > 1 ? 0f : (MyGdxGame.WIDTH - btnW) / 2f;
        float startY  = MyGdxGame.WIDTH * 0.58f;
        float rowH    = 0f;
        for (int ci = 0; ci < phList.size(); ci++) {
          final String hName = phList.get(ci).getHeroName();
          TextButton hBtn = new TextButton(hName, MyGdxGame.skin);
          hBtn.setSize(btnW, hBtn.getPrefHeight());
          if (rowH == 0f) rowH = hBtn.getPrefHeight() + 8f;
          int col = ci % numCols;
          int row = ci / numCols;
          hBtn.setPosition(startX + col * (btnW + btnGapX), startY - row * rowH);
          hBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              auctionSellHeroName = hName;
              auctionSellMinBid = 1;
              gameState.setUpdateState(true);
            }
          });
          gameStage.addActor(hBtn);
        }
        TextButton selCancel = new TextButton("Cancel", MyGdxGame.skin);
        selCancel.setSize(selCancel.getPrefWidth() + 20, selCancel.getPrefHeight());
        selCancel.setPosition(MyGdxGame.WIDTH / 2f - selCancel.getWidth() / 2f, MyGdxGame.WIDTH * 0.35f);
        selCancel.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            auctionSellHeroName = null;
            gameState.setUpdateState(true);
          }
        });
        gameStage.addActor(selCancel);
      } else {
        // Min-bid phase — player has chosen a hero and is setting the minimum bid strength
        final String sHeroName = auctionSellHeroName;
        Label mbTitle = new Label("Sell " + sHeroName + " — Minimum bid strength:", MyGdxGame.skin);
        mbTitle.setColor(Color.GOLD);
        mbTitle.setPosition(MyGdxGame.WIDTH / 2f - mbTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.82f);
        gameStage.addActor(mbTitle);

        Label mbValue = new Label(String.valueOf(auctionSellMinBid), MyGdxGame.skin);
        mbValue.setFontScale(2f);
        mbValue.setColor(Color.WHITE);
        mbValue.setPosition(MyGdxGame.WIDTH / 2f - mbValue.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.65f);
        gameStage.addActor(mbValue);

        TextButton minusBtn = new TextButton("-", MyGdxGame.skin);
        minusBtn.setSize(90f, 70f);
        minusBtn.setPosition(MyGdxGame.WIDTH / 2f - 140f, MyGdxGame.WIDTH * 0.62f);
        minusBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (auctionSellMinBid > 1) { auctionSellMinBid--; gameState.setUpdateState(true); }
          }
        });
        gameStage.addActor(minusBtn);

        TextButton plusBtn = new TextButton("+", MyGdxGame.skin);
        plusBtn.setSize(90f, 70f);
        plusBtn.setPosition(MyGdxGame.WIDTH / 2f + 60f, MyGdxGame.WIDTH * 0.62f);
        plusBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            auctionSellMinBid++;
            gameState.setUpdateState(true);
          }
        });
        gameStage.addActor(plusBtn);

        TextButton confirmBtn = new TextButton("Start Auction", MyGdxGame.skin);
        float caBtnW = confirmBtn.getPrefWidth() + 20;
        confirmBtn.setSize(caBtnW, confirmBtn.getPrefHeight());
        confirmBtn.setPosition(MyGdxGame.WIDTH / 2f - caBtnW / 2f, MyGdxGame.WIDTH * 0.38f);
        confirmBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            try {
              JSONObject data = new JSONObject();
              data.put("heroName", sHeroName);
              data.put("minBid", auctionSellMinBid);
              socket.emit("initiateHeroSale", data);
            } catch (JSONException e) { e.printStackTrace(); }
            auctionSellHeroName = null;
            gameState.setUpdateState(true);
          }
        });
        gameStage.addActor(confirmBtn);

        TextButton mbCancel = new TextButton("Cancel", MyGdxGame.skin);
        mbCancel.setSize(mbCancel.getPrefWidth() + 20, mbCancel.getPrefHeight());
        mbCancel.setPosition(MyGdxGame.WIDTH / 2f - mbCancel.getWidth() / 2f, MyGdxGame.WIDTH * 0.14f);
        mbCancel.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            auctionSellHeroName = null;
            gameState.setUpdateState(true);
          }
        });
        gameStage.addActor(mbCancel);
      }
    }

    // ── Auction bidding overlay ─────────────────────────────────────────────
    // Shown to all players while a hero auction is in progress on the server.
    // Uses overlayStage (450×800 logical coords) so it covers the full screen.
    if (pendingHeroAuction != null) {
      try {
        final int sellerIdx    = pendingHeroAuction.getInt("sellerIdx");
        final String aHeroName = pendingHeroAuction.getString("heroName");
        final int minBidVal    = pendingHeroAuction.getInt("minBid");
        final int curBidderIdx = pendingHeroAuction.getInt("currentBidderIdx");
        final JSONObject curBid = pendingHeroAuction.optJSONObject("currentBid");

        Image aOv = new Image(MyGdxGame.skin, "white");
        aOv.setFillParent(true);
        aOv.setColor(0f, 0f, 0f, 0.85f);
        overlayStage.addActor(aOv);

        String sellerName = (sellerIdx >= 0 && sellerIdx < players.size())
            ? players.get(sellerIdx).getPlayerName() : "Player " + sellerIdx;
        Label aTitle = new Label(sellerName + " is selling " + aHeroName
            + "  (min bid: " + minBidVal + ")", MyGdxGame.skin);
        aTitle.setColor(Color.GOLD);
        aTitle.setPosition(MyGdxGame.WIDTH / 2f - aTitle.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.91f);
        overlayStage.addActor(aTitle);

        // Show the hero image (visible to all players)
        if (sellerIdx >= 0 && sellerIdx < players.size()) {
          for (Hero heroActor : players.get(sellerIdx).getHeroes()) {
            if (aHeroName.equals(heroActor.getHeroName())) {
              com.badlogic.gdx.graphics.g2d.Sprite hs = heroActor.getSprite();
              if (hs != null) {
                float heroW = hs.getWidth() * 0.55f;
                float heroH = hs.getHeight() * 0.55f;
                Image heroImg = new Image(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                    new com.badlogic.gdx.graphics.g2d.TextureRegion(hs))) {
                  @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
                };
                heroImg.setSize(heroW, heroH);
                heroImg.setPosition(MyGdxGame.WIDTH / 2f - heroW / 2f, MyGdxGame.HEIGHT * 0.83f - heroH / 2f);
                overlayStage.addActor(heroImg);
              }
              break;
            }
          }
        }

        if (curBid != null) {
          int cBidder = curBid.getInt("bidderIdx");
          int cTotal  = curBid.getInt("totalStrength");
          String cName = (cBidder >= 0 && cBidder < players.size())
              ? players.get(cBidder).getPlayerName() : "Player " + cBidder;
          Label bidLabel = new Label("Current bid: " + cTotal + "  by " + cName, MyGdxGame.skin);
          bidLabel.setColor(Color.CYAN);
          bidLabel.setPosition(MyGdxGame.WIDTH / 2f - bidLabel.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.75f);
          overlayStage.addActor(bidLabel);
        } else {
          Label noBid = new Label("No bids yet", MyGdxGame.skin);
          noBid.setColor(Color.LIGHT_GRAY);
          noBid.setPosition(MyGdxGame.WIDTH / 2f - noBid.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.75f);
          overlayStage.addActor(noBid);
        }

        if (!isSpectator && curBidderIdx == playerIndex) {
          // ── This player's bid turn ──────────────────────────────────────
          Label yourTurnLbl = new Label("Your turn — select cards to bid:", MyGdxGame.skin);
          yourTurnLbl.setColor(Color.GREEN);
          yourTurnLbl.setPosition(MyGdxGame.WIDTH / 2f - yourTurnLbl.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.66f);
          overlayStage.addActor(yourTurnLbl);

          // Hand cards as toggle buttons
          final Player localPlayer = players.get(playerIndex);
          final ArrayList<Card> myHand = localPlayer.getHandCards();
          float cBtnW = MyGdxGame.WIDTH / 6.5f;
          float cBtnH = cBtnW * 1.2f;
          float cGapX = MyGdxGame.WIDTH * 0.008f;
          int maxPerRow = 6;
          float handRowsY = MyGdxGame.HEIGHT * 0.53f;
          for (int ci = 0; ci < myHand.size(); ci++) {
            final Card hc = myHand.get(ci);
            final int hcId = hc.getCardId();
            boolean sel = auctionBidHandCardIds.contains(hcId);
            int row = ci / maxPerRow;
            int col = ci % maxPerRow;
            int rowCards = Math.min(myHand.size() - row * maxPerRow, maxPerRow);
            float rx = (MyGdxGame.WIDTH - rowCards * (cBtnW + cGapX)) / 2f;
            String cLbl = hc.getSymbol().substring(0, 1).toUpperCase()
                + (hc.getIndex() == 1 ? "A"
                    : (hc.getCardId() > 52 ? "J" : String.valueOf(hc.getIndex())));
            TextButton cb = new TextButton(cLbl + (sel ? "*" : ""), MyGdxGame.skin);
            if (sel) cb.setColor(0.4f, 1f, 0.4f, 1f);
            cb.setSize(cBtnW, cBtnH);
            cb.setPosition(rx + col * (cBtnW + cGapX), handRowsY - row * (cBtnH + 4f));
            cb.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                if (auctionBidHandCardIds.contains(hcId)) auctionBidHandCardIds.remove(hcId);
                else auctionBidHandCardIds.add(hcId);
                gameState.setUpdateState(true);
              }
            });
            overlayStage.addActor(cb);
          }

          // Defense cards as toggle buttons
          Map<Integer, Card> myDefs = localPlayer.getDefCards();
          Map<Integer, Card> myTopDefs = localPlayer.getTopDefCards();
          java.util.List<Integer> defIds = new java.util.ArrayList<Integer>();
          for (Card dc : myDefs.values()) defIds.add(dc.getCardId());
          for (Card dc : myTopDefs.values()) defIds.add(dc.getCardId());
          if (!defIds.isEmpty()) {
            Label defHdr = new Label("Defense:", MyGdxGame.skin);
            defHdr.setColor(Color.YELLOW);
            float defRowY = MyGdxGame.HEIGHT * 0.22f;
            defHdr.setPosition(MyGdxGame.WIDTH / 2f - defHdr.getPrefWidth() / 2f, defRowY + cBtnH + 2f);
            overlayStage.addActor(defHdr);
            float defStartX = (MyGdxGame.WIDTH - defIds.size() * (cBtnW + cGapX)) / 2f;
            for (int di = 0; di < defIds.size(); di++) {
              final int dcId = defIds.get(di);
              boolean dSel = auctionBidDefCardIds.contains(dcId);
              Card dc = Card.fromCardId(dcId);
              String dcLbl = dc.getSymbol().substring(0, 1).toUpperCase()
                  + (dc.getIndex() == 1 ? "A"
                      : (dcId > 52 ? "J" : String.valueOf(dc.getIndex())));
              TextButton dcBtn = new TextButton(dcLbl + (dSel ? "*" : ""), MyGdxGame.skin);
              if (dSel) dcBtn.setColor(1f, 1f, 0.4f, 1f);
              dcBtn.setSize(cBtnW, cBtnH);
              dcBtn.setPosition(defStartX + di * (cBtnW + cGapX), defRowY);
              dcBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                  if (auctionBidDefCardIds.contains(dcId)) auctionBidDefCardIds.remove(dcId);
                  else auctionBidDefCardIds.add(dcId);
                  gameState.setUpdateState(true);
                }
              });
              overlayStage.addActor(dcBtn);
            }
          }

          // Total strength and bid / pass buttons
          int totalBidStr = 0;
          for (int id : auctionBidHandCardIds) totalBidStr += Card.fromCardId(id).getStrength();
          for (int id : auctionBidDefCardIds)  totalBidStr += Card.fromCardId(id).getStrength();
          int requiredStr = (curBid != null ? curBid.optInt("totalStrength", 0) + 1 : minBidVal);
          boolean hasCards = (!auctionBidHandCardIds.isEmpty() || !auctionBidDefCardIds.isEmpty());
          boolean canBid   = hasCards && totalBidStr >= requiredStr;

          Label strLbl = new Label("Bid: " + totalBidStr + "  (need >= " + requiredStr + ")", MyGdxGame.skin);
          strLbl.setColor(canBid ? Color.GREEN : Color.RED);
          strLbl.setPosition(MyGdxGame.WIDTH / 2f - strLbl.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.12f);
          overlayStage.addActor(strLbl);

          final int finalTotal = totalBidStr;
          TextButton bidBtn = new TextButton("Bid", MyGdxGame.skin);
          bidBtn.setSize(bidBtn.getPrefWidth() + 20, bidBtn.getPrefHeight());
          bidBtn.setColor(canBid ? Color.WHITE : Color.DARK_GRAY);
          bidBtn.setPosition(MyGdxGame.WIDTH / 2f - bidBtn.getWidth() - 8f, MyGdxGame.HEIGHT * 0.04f);
          if (canBid) {
            final java.util.Set<Integer> snapHand = new java.util.HashSet<Integer>(auctionBidHandCardIds);
            final java.util.Set<Integer> snapDef  = new java.util.HashSet<Integer>(auctionBidDefCardIds);
            bidBtn.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                try {
                  JSONObject data = new JSONObject();
                  JSONArray handArr = new JSONArray();
                  for (int id : snapHand) handArr.put(id);
                  JSONArray defArr = new JSONArray();
                  for (int id : snapDef) defArr.put(id);
                  data.put("handCardIds", handArr);
                  data.put("defCardIds", defArr);
                  socket.emit("heroAuctionBid", data);
                } catch (JSONException e) { e.printStackTrace(); }
                auctionBidHandCardIds.clear();
                auctionBidDefCardIds.clear();
                gameState.setUpdateState(true);
              }
            });
          }
          overlayStage.addActor(bidBtn);

          TextButton passBtn = new TextButton("Pass", MyGdxGame.skin);
          passBtn.setSize(passBtn.getPrefWidth() + 20, passBtn.getPrefHeight());
          passBtn.setPosition(MyGdxGame.WIDTH / 2f + 8f, MyGdxGame.HEIGHT * 0.04f);
          passBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              socket.emit("heroAuctionPass", new JSONObject());
              auctionBidHandCardIds.clear();
              auctionBidDefCardIds.clear();
              gameState.setUpdateState(true);
            }
          });
          overlayStage.addActor(passBtn);

        } else {
          // ── Waiting for another player (or seller watching) ─────────────
          String waitName = (curBidderIdx >= 0 && curBidderIdx < players.size())
              ? players.get(curBidderIdx).getPlayerName() : "Player " + curBidderIdx;
          Label waitLbl = new Label("Waiting for " + waitName + " to bid or pass...", MyGdxGame.skin);
          waitLbl.setColor(Color.LIGHT_GRAY);
          waitLbl.setPosition(MyGdxGame.WIDTH / 2f - waitLbl.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.50f);
          overlayStage.addActor(waitLbl);
        }
      } catch (JSONException e) { e.printStackTrace(); }
    }



    // Merchant reveal overlay — shown on top while the current player decides Keep / 2nd Try
    Card merchantPendingCard = null;
    for (Card hc : currentPlayer.getHandCards()) {
      if (hc.isTradeable()) { merchantPendingCard = hc; break; }
    }
    if (merchantPendingCard != null) {
      final Card tradeableCard = merchantPendingCard;

      Image merchOverlay = new Image(MyGdxGame.skin, "white");
      merchOverlay.setFillParent(true);
      merchOverlay.setColor(0f, 0f, 0f, 0.72f);
      gameStage.addActor(merchOverlay);

      Label promptLabel = new Label("Your new card:", MyGdxGame.skin);
      promptLabel.setColor(Color.GOLD);

      // Show the card face-up in the centre
      Card displayCard = Card.fromCardId(tradeableCard.getCardId());
      float cw = displayCard.getDefWidth() * 1.5f;
      float ch = displayCard.getDefHeight() * 1.5f;
      displayCard.setWidth(cw);
      displayCard.setHeight(ch);
      float cardX = MyGdxGame.WIDTH / 2f - cw / 2f;
      float cardY = MyGdxGame.WIDTH / 2f - ch / 2f + ch * 0.15f;
      displayCard.setPosition(cardX, cardY);
      gameStage.addActor(displayCard);

      promptLabel.setPosition(
          MyGdxGame.WIDTH / 2f - promptLabel.getPrefWidth() / 2f,
          cardY + displayCard.getHeight() + 8f);
      gameStage.addActor(promptLabel);

      float btnW = MyGdxGame.WIDTH * 0.35f;
      float btnY = cardY - 60f;

      TextButton keepBtn = new TextButton("Keep", MyGdxGame.skin);
      keepBtn.setSize(btnW, 50f);
      keepBtn.setPosition(MyGdxGame.WIDTH / 2f - btnW - 8f, btnY);
      keepCardButtonListener = new KeepCardButtonListener(tradeableCard, gameState);
      keepBtn.addListener(keepCardButtonListener);
      gameStage.addActor(keepBtn);

      TextButton tryBtn = new TextButton("2nd Try", MyGdxGame.skin);
      tryBtn.setSize(btnW, 50f);
      tryBtn.setPosition(MyGdxGame.WIDTH / 2f + 8f, btnY);
      tradeCardButtonListener = new TradeCardButtonListener(tradeableCard, gameState.getCurrentPlayer(),
          gameState.getCardDeck(), gameState.getCemeteryDeck(), gameState, socket, playerIndex);
      tryBtn.addListener(tradeCardButtonListener);
      gameStage.addActor(tryBtn);
    }

    // Merchant 2nd-try reveal: display the drawn card face-up for ALL players (incl. trader),
    // and show "JOKER — lost" if the second draw was a joker. Hidden once the server clears
    // merchantReveal on finishTurn. The whole stage gets a transparent click-catcher so any
    // tap dismisses the overlay (sends dismissMerchantReveal to the server, which clears
    // lastMerchantReveal for every client on next stateUpdate).
    if (merchantRevealCardId != -1) {
      Card revealCard = Card.fromCardId(merchantRevealCardId);
      boolean isJoker = revealCard != null && "joker".equals(revealCard.getSymbol());
      // Tap-anywhere dismiss layer (must be added FIRST so it is below the card visually
      // but receives clicks meant for the empty area around the card).
      Image dismissLayer = new Image(MyGdxGame.skin, "white");
      dismissLayer.setFillParent(true);
      dismissLayer.setColor(0f, 0f, 0f, 0.55f);
      dismissLayer.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
          // Hide locally for snappy UX, then notify the server to clear for everyone.
          merchantRevealCardId = -1;
          merchantRevealPlayerIdx = -1;
          if (socket != null) {
            JSONObject d = new JSONObject();
            socket.emit("dismissMerchantReveal", d);
          }
          gameState.setUpdateState(true);
          return true;
        }
      });
      gameStage.addActor(dismissLayer);

      float rcw = revealCard.getDefWidth() * 1.5f;
      float rch = revealCard.getDefHeight() * 1.5f;
      revealCard.setWidth(rcw);
      revealCard.setHeight(rch);
      revealCard.setPosition(
          (MyGdxGame.WIDTH - rcw) / 2f,
          (MyGdxGame.WIDTH - rch) / 2f);
      // Card itself also dismisses on tap.
      revealCard.clearListeners();
      revealCard.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
          merchantRevealCardId = -1;
          merchantRevealPlayerIdx = -1;
          if (socket != null) {
            JSONObject d = new JSONObject();
            socket.emit("dismissMerchantReveal", d);
          }
          gameState.setUpdateState(true);
          return true;
        }
      });
      gameStage.addActor(revealCard);
      String revealText;
      if (merchantRevealPlayerIdx == playerIndex) {
        revealText = isJoker ? "JOKER — lost! (tap to dismiss)" : "Your 2nd-try card (tap to dismiss)";
      } else {
        revealText = isJoker
            ? "P" + merchantRevealPlayerIdx + " drew JOKER — lost! (tap to dismiss)"
            : "P" + merchantRevealPlayerIdx + " 2nd-try reveal (tap to dismiss)";
      }
      Label revealLabel = new Label(revealText, MyGdxGame.skin);
      revealLabel.setColor(isJoker ? Color.RED : Color.GREEN);
      revealLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
      revealLabel.setPosition(
          revealCard.getX() + (revealCard.getWidth() - revealLabel.getPrefWidth()) / 2f,
          revealCard.getY() + revealCard.getHeight() + 2f);
      gameStage.addActor(revealLabel);
    }

    // Priest overlay — shown when the current player opens an enemy hand to pick a card
    final int priestTarget = gameState.getPriestTargetPlayerIdx();
    if (priestTarget >= 0 && priestTarget < players.size() && priestTarget != playerIndex) {
      final Player priestCurrentPlayer = currentPlayer;
      final ArrayList<Player> priestPlayers = players;
      final Priest priest;
      Priest priestTmp = null;
      for (Hero h : currentPlayer.getHeroes()) {
        if ("Priest".equals(h.getHeroName())) { priestTmp = (Priest) h; break; }
      }
      priest = priestTmp;
      if (priest != null) {
        final ArrayList<Card> targetHand = players.get(priestTarget).getHandCards();
        final int revealedId = gameState.getPriestRevealedCardId();

        // dark overlay
        Image priestBg = new Image(MyGdxGame.skin, "white");
        priestBg.setFillParent(true);
        priestBg.setColor(0f, 0f, 0f, 0.78f);
        gameStage.addActor(priestBg);

        String prompt = revealedId < 0
            ? "Priest: pick a card (" + priest.getConversionAttempts() + " tr" + (priest.getConversionAttempts() == 1 ? "y" : "ies") + " left)"
            : "No match!";
        Label promptLbl = new Label(prompt, MyGdxGame.skin);
        promptLbl.setColor(revealedId < 0 ? Color.GOLD : Color.RED);
        promptLbl.setPosition(
            MyGdxGame.WIDTH / 2f - promptLbl.getPrefWidth() / 2f,
            MyGdxGame.WIDTH * 0.72f);
        gameStage.addActor(promptLbl);

        // Lay out cards in a row
        int n = Math.max(targetHand.size(), 1);
        float pCardW = Math.min(55f, (MyGdxGame.WIDTH - 20f) / n - 5f);
        float pCardH = pCardW * 1.4f;
        float spacing = 5f;
        float totalW = targetHand.size() * (pCardW + spacing) - spacing;
        float startX = MyGdxGame.WIDTH / 2f - totalW / 2f;
        float cardRowY = MyGdxGame.WIDTH / 2f - pCardH / 2f;

        for (int ci = 0; ci < targetHand.size(); ci++) {
          final Card tc = targetHand.get(ci);
          Card display = Card.fromCardId(tc.getCardId());
          display.setSize(pCardW, pCardH);
          display.setPosition(startX + ci * (pCardW + spacing), cardRowY);

          if (revealedId < 0) {
            // face-down, clickable
            display.setCovered(true);
            display.setActive(false);
            display.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                String atkSym = priestCurrentPlayer.getPlayerTurn().getAttackingSymbol()[0];
                priest.conversionAttempt();
                if (atkSym.equals(tc.getSymbol()) || "joker".equals(tc.getSymbol())) {
                  // Success
                  priest.conversion();
                  Iterator<Card> it = priestPlayers.get(priestTarget).getHandCards().iterator();
                  while (it.hasNext()) { if (it.next() == tc) { it.remove(); break; } }
                  priestCurrentPlayer.addHandCard(tc);
                  emitPriestConvert(priestTarget, tc.getCardId());
                  gameState.setPriestTargetPlayerIdx(-1);
                  gameState.setPriestRevealedCardId(-1);
                } else {
                  // Miss — reveal it and notify server to decrement the counter
                  gameState.setPriestRevealedCardId(tc.getCardId());
                  emitPriestAttemptFailed();
                }
                gameState.setUpdateState(true);
              }
            });
          } else {
            // show the revealed card face-up, rest face-down
            if (tc.getCardId() == revealedId) {
              display.setCovered(false);
              display.setActive(true);
            } else {
              display.setCovered(true);
              display.setActive(false);
              // Issue #175: face-down cards remain clickable while attempts remain so the
              // player can retry directly without an extra "Try again" button.
              if (priest.getConversionAttempts() > 0) {
                display.addListener(new ClickListener() {
                  @Override
                  public void clicked(InputEvent event, float x, float y) {
                    String atkSym = priestCurrentPlayer.getPlayerTurn().getAttackingSymbol()[0];
                    priest.conversionAttempt();
                    if (atkSym.equals(tc.getSymbol()) || "joker".equals(tc.getSymbol())) {
                      priest.conversion();
                      Iterator<Card> it = priestPlayers.get(priestTarget).getHandCards().iterator();
                      while (it.hasNext()) { if (it.next() == tc) { it.remove(); break; } }
                      priestCurrentPlayer.addHandCard(tc);
                      emitPriestConvert(priestTarget, tc.getCardId());
                      gameState.setPriestTargetPlayerIdx(-1);
                      gameState.setPriestRevealedCardId(-1);
                    } else {
                      gameState.setPriestRevealedCardId(tc.getCardId());
                      emitPriestAttemptFailed();
                    }
                    gameState.setUpdateState(true);
                  }
                });
              }
            }
          }
          gameStage.addActor(display);
        }

        // Buttons below the cards
        float btnY = cardRowY - 55f;
        float btnW = 120f;
        if (revealedId >= 0) {
          if (priest.getConversionAttempts() > 0) {
            // Issue #175: replace "Try again" with "Cancel" — face-down cards are now
            // directly clickable for retry.
            TextButton cancelBtn = new TextButton("Cancel", MyGdxGame.skin);
            cancelBtn.setSize(btnW, 45f);
            cancelBtn.setPosition(MyGdxGame.WIDTH / 2f - btnW / 2f, btnY);
            cancelBtn.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                gameState.setPriestRevealedCardId(-1);
                gameState.setPriestTargetPlayerIdx(-1);
                gameState.setUpdateState(true);
              }
            });
            gameStage.addActor(cancelBtn);
          } else {
            // No more attempts
            TextButton doneBtn = new TextButton("Done", MyGdxGame.skin);
            doneBtn.setSize(btnW, 45f);
            doneBtn.setPosition(MyGdxGame.WIDTH / 2f - btnW / 2f, btnY);
            doneBtn.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                priest.setSelectable(false);
                priest.setSelected(false);
                gameState.setPriestRevealedCardId(-1);
                gameState.setPriestTargetPlayerIdx(-1);
                gameState.setUpdateState(true);
              }
            });
            gameStage.addActor(doneBtn);
          }
        } else {
          // Still in selection phase — offer cancel
          TextButton cancelBtn = new TextButton("Cancel", MyGdxGame.skin);
          cancelBtn.setSize(btnW, 45f);
          cancelBtn.setPosition(MyGdxGame.WIDTH / 2f - btnW / 2f, btnY);
          cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              gameState.setPriestTargetPlayerIdx(-1);
              gameState.setUpdateState(true);
            }
          });
          gameStage.addActor(cancelBtn);
        }
      }
    }
  }

  public void showHandStage(ArrayList<Player> players, final Player currentPlayer) {
    // Set up own hand card listeners for the current turn player
    for (int i = 0; i < players.size(); i++) {
      ArrayList<Card> handCards = players.get(i).getHandCards();
      for (int j = 0; j < handCards.size(); j++) {
        if (players.get(i) == gameState.getCurrentPlayer()) {
          final Card handCard = handCards.get(j);
          handCard.removeAllListeners();
          // If battery tower denied this turn, lock only the specific cards used in the denied attack
          boolean isDeniedCard = gameState.getCurrentPlayer().getPlayerTurn().getBatteryDeniedAttackCardIds().contains(handCard.getCardId());
          boolean isPreyCard = gameState.getCurrentPlayer().getPlayerTurn().getPreyCardIds().contains(handCard.getCardId());
          if (!isDeniedCard && !isPreyCard) {
            ownHandCardListener = new OwnHandCardListener(handCard, gameState.getCurrentPlayer(), gameState.getCardDeck(),
                gameState.getCemeteryDeck(), gameState, socket, playerIndex);
            handCard.addListener(ownHandCardListener);
            // Drag hand card up into defense slot placeholder
            handCard.addListener(new DragListener() {
              float touchOffX, touchOffY;
              @Override
              public void dragStart(InputEvent event, float x, float y, int pointer) {
                touchOffX = x; touchOffY = y;
                isDraggingHandCard = true;
                handCard.setVisible(false);
                dragOverlayCard = Card.fromCardId(handCard.getCardId());
                dragOverlayCard.setWidth(handCard.getWidth());
                dragOverlayCard.setHeight(handCard.getHeight());
                dragOverlayCard.setPosition(handCard.getX(), handCard.getY());
                overlayStage.addActor(dragOverlayCard);
              }
              @Override
              public void drag(InputEvent event, float x, float y, int pointer) {
                if (dragOverlayCard == null) return;
                dragOverlayCard.setPosition(event.getStageX() - touchOffX, event.getStageY() - touchOffY);
              }
              @Override
              public void dragStop(InputEvent event, float x, float y, int pointer) {
                isDraggingHandCard = false;
                if (dragOverlayCard != null) { dragOverlayCard.remove(); dragOverlayCard = null; }
                handCard.setVisible(true);
                float handAreaHeight = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
                if (event.getStageY() > handAreaHeight) {
                  float gameStageY = event.getStageY() - handAreaHeight;
                  Actor hit = gameStage.hit(event.getStageX(), gameStageY, false);
                  if (hit instanceof Card) {
                    Card target = (Card) hit;
                    int posId = target.getPositionId();
                    if (target.isPlaceholder() && posId >= 1 && posId <= 3
                        && currentPlayer.canPutDefCard() && !currentPlayer.isSlotSabotaged(posId)) {
                      currentPlayer.putDefCard(posId, 0);
                      emitPutDefCard(posId, handCard.getCardId());
                      gameState.setUpdateState(true);
                      return;
                    }
                  }
                }
                gameState.setUpdateState(true);
              }
            });
          }
          handCards.get(j).setActive(false);
        }
      }
    }

    // Draw heroes and hand cards only for the current (own) player
    // Sort BEFORE calling getHandCards() — sortHandCards() replaces the
    // Player.handCards reference with a new ArrayList, so we must
    // capture the sorted list after the sort completes.
    currentPlayer.sortHandCards();
    final ArrayList<Card> handCards = currentPlayer.getHandCards();
    ArrayList<Hero> playerHeroes = currentPlayer.getHeroes();
    ArrayList<Integer> deniedCardIds = currentPlayer.getPlayerTurn().getBatteryDeniedAttackCardIds();
    ArrayList<Integer> preyCardIds = currentPlayer.getPlayerTurn().getPreyCardIds();

    // Single row layout for all hand cards
    int handCardCount = handCards.size();
    int upperCount = handCardCount;
    int lowerCount = 0;

    for (int j = 0; j < handCards.size(); j++) {
      Card handcard = handCards.get(j);
      handcard.setCovered(false);
      handcard.setActive(true);
      if (deniedCardIds.contains(handcard.getCardId()) || preyCardIds.contains(handcard.getCardId())) handcard.setColor(0.4f, 0.4f, 0.4f, 1f);
      else handcard.setColor(Color.WHITE);
      handcard.setRotation(0);
      handcard.setWidth(handcard.getDefWidth() * 2);
      handcard.setHeight(handcard.getDefHeight() * 2);
      // Fan step: distribute count cards evenly so the last card's right edge aligns with
      // the 5-card boundary (4 card-widths between first and last card's left edge).
      float cardW = handcard.getWidth();
      float upperStep = upperCount <= 1 ? cardW : Math.min(cardW, 4.0f * cardW / (upperCount - 1));
      float lowerStep = lowerCount <= 1 ? cardW : Math.min(cardW, 4.0f * cardW / (lowerCount - 1));
      if (j < upperCount) {
        handcard.setX(j * upperStep);
        handcard.setY(MyGdxGame.WIDTH / 2);
      } else {
        handcard.setX((j - upperCount) * lowerStep);
        handcard.setY(MyGdxGame.WIDTH / 2 - handcard.getHeight());
      }
      handStage.addActor(handcard);
      // Issues #54, #176: highlight own hand cards as discard candidates when Spy/Merchant is selected.
      applyOwnHandCardHighlight(handcard, currentPlayer);

      if (handcard.getBoosted() > 0) {
        TextureRegion mercenaryRegion = new TextureRegion(texMercenary, 0, 0, 512, 512);
        Image mercenaryImage = new Image(mercenaryRegion);
        mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(), mercenaryImage.getWidth() / 10f,
            mercenaryImage.getHeight() / 10f);
        mercenaryImage.setPosition(handcard.getX(), handcard.getY());
        mercenaryImage.setX(mercenaryImage.getX() + handcard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
        mercenaryImage.setY(mercenaryImage.getY() + handcard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);
        removeAllListeners(mercenaryImage);
        mercenaryImageListener = new MercenaryImageListener(gameState, handcard, currentPlayer);
        mercenaryImage.addListener(mercenaryImageListener);
        handStage.addActor(mercenaryImage);

        String boostCount = String.valueOf(handcard.getBoosted());
        Label boostCountLabel = new Label(boostCount, MyGdxGame.skin);
        boostCountLabel.setColor(new Color(0.1f, 0.2f, 0.8f, 1f));
        boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());
        handStage.addActor(boostCountLabel);
      }

      if (handCards.get(j).isTradeable()) {
        // Merchant dialog is handled as a modal overlay in showGameStage — skip inline buttons here.
      }
    }

    // Display all heroes of current player
    // Bottom bar height (finish-turn button) — heroes sit just above it
    final float bottomBarH = new TextButton("Finish turn", MyGdxGame.skin).getPrefHeight() + 2f;
    for (int j = 0; j < playerHeroes.size(); j++) {
      final Hero hero = playerHeroes.get(j);
      hero.setHand(true);
      hero.setPosition(j * hero.getWidth(), bottomBarH);

      if (hero.getHeroName() == "Priest") {
        Priest priestHero = (Priest) hero;
        PlayerTurn priestPt = gameState.getCurrentPlayer().getPlayerTurn();
        if (priestHero.getConversionAttempts() > 0 && !"none".equals(priestPt.getAttackingSymbol()[0])) {
          hero.setSelectable(true);
        } else {
          hero.setSelectable(false);
        }
      }

      hero.removeAllListeners();
      final String heroInfoName = hero.getHeroName();
      if (currentPlayer == gameState.getCurrentPlayer()) {
        ownHeroListener = new OwnHeroListener(hero, gameState.getCurrentPlayer(), gameState);
        hero.addListener(ownHeroListener);
      } else {
        // Not our turn — hero is not usable; clicking the image shows info instead.
        hero.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            showHeroInfoOverlay(heroInfoName);
            event.stop();
          }
        });
      }

      Label heroLabel = new Label(hero.getHeroID(), MyGdxGame.skin);
      heroLabel.setPosition(j * hero.getWidth() + (hero.getWidth() - heroLabel.getWidth()) / 2, bottomBarH + hero.getHeight());
      heroLabel.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          showHeroInfoOverlay(heroInfoName);
          event.stop();
        }
      });

      handStage.addActor(hero);
      handStage.addActor(heroLabel);

      if (hero.getHeroName() == "Spy") {
        Spy spy = (Spy) hero;
        String spyCount = spy.getSpyAttacks() + "/" + spy.getSpyMaxAttacks();
        Label spyCountLabel = new Label(spyCount, MyGdxGame.skin);
        spyCountLabel.setColor(Color.CYAN);
        spyCountLabel.setPosition(hero.getX() + hero.getWidth() - spyCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(spyCountLabel);
      }

      if (hero.getHeroName() == "Battery Tower") {
        BatteryTower bt = (BatteryTower) hero;
        String btCount = bt.getCharges() + "/1";
        Label btCountLabel = new Label(btCount, MyGdxGame.skin);
        btCountLabel.setColor(new Color(0.1f, 0.2f, 0.8f, 1f));
        btCountLabel.setPosition(hero.getX() + hero.getWidth() - btCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(btCountLabel);
      }

      if (hero.getHeroName() == "Priest") {
        Priest priest = (Priest) hero;
        Label priestCountLabel = new Label(priest.getConversionAttempts() + "/2", MyGdxGame.skin);
        priestCountLabel.setColor(Color.CYAN);
        priestCountLabel.setPosition(hero.getX() + hero.getWidth() - priestCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(priestCountLabel);
      }

      if ("Warlord".equals(hero.getHeroName())) {
        Warlord warlord = (Warlord) hero;
        String atkCount = warlord.getAttacks() + "/1";
        Label warlordCountLabel = new Label(atkCount, MyGdxGame.skin);
        warlordCountLabel.setColor(Color.ORANGE);
        warlordCountLabel.setPosition(hero.getX() + hero.getWidth() - warlordCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(warlordCountLabel);
      }

      if ("Merchant".equals(hero.getHeroName())) {
        Merchant merchant = (Merchant) hero;
        String tradeCount = merchant.getTrades() + "/1";
        Label merchantCountLabel = new Label(tradeCount, MyGdxGame.skin);
        merchantCountLabel.setColor(Color.GREEN);
        merchantCountLabel.setPosition(hero.getX() + hero.getWidth() - merchantCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(merchantCountLabel);
      }

      if ("Fortified Tower".equals(hero.getHeroName())) {
        FortifiedTower ft = (FortifiedTower) hero;
        String ftCount = ft.getDefenseExpands() + "/1";
        Label ftCountLabel = new Label(ftCount, MyGdxGame.skin);
        ftCountLabel.setColor(Color.PURPLE);
        ftCountLabel.setPosition(hero.getX() + hero.getWidth() - ftCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(ftCountLabel);
      }

      if ("Magician".equals(hero.getHeroName())) {
        Magician magician = (Magician) hero;
        String spellCount = magician.getSpells() + "/1";
        Label magCountLabel = new Label(spellCount, MyGdxGame.skin);
        magCountLabel.setColor(Color.CYAN);
        magCountLabel.setPosition(hero.getX() + hero.getWidth() - magCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(magCountLabel);
      }

      if (hero.getHeroName() == "Marshal") {
        Marshal marshal = (Marshal) hero;
        String mobCount = marshal.getMobilizations() + "/3";
        Label mobCountLabel = new Label(mobCount, MyGdxGame.skin);
        mobCountLabel.setColor(Color.ORANGE);
        mobCountLabel.setPosition(hero.getX() + hero.getWidth() - mobCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(mobCountLabel);
      }

      if (hero.getHeroName() == "Mercenaries") {
        Mercenaries mercenaries = (Mercenaries) hero;
        int atkBonus = currentPlayer.getPlayerTurn().getMercenaryAttackBonus();
        // x/8 counter label — right-aligned to the hero sprite's right edge
        String readyCount = mercenaries.countReady() + "/8";
        Label readyCountLabel = new Label(readyCount, MyGdxGame.skin);
        readyCountLabel.setColor(new Color(0.1f, 0.2f, 0.8f, 1f));
        float indicatorX = hero.getX() + hero.getWidth() - readyCountLabel.getPrefWidth();
        readyCountLabel.setPosition(indicatorX, hero.getY());
        handStage.addActor(readyCountLabel);
        // Red +x label above x/8 when there is a pending attack bonus
        if (atkBonus > 0) {
          Label atkBonusLabel = new Label("+" + atkBonus, MyGdxGame.skin);
          atkBonusLabel.setColor(Color.RED);
          atkBonusLabel.setPosition(hero.getX() + hero.getWidth() - atkBonusLabel.getPrefWidth(),
              hero.getY() + readyCountLabel.getPrefHeight() + 2f);
          handStage.addActor(atkBonusLabel);
        }
      }

      if ("Reservists".equals(hero.getHeroName())) {
        Reservists reservists = (Reservists) hero;
        int resAtkBonus = currentPlayer.getPlayerTurn().getReservistAttackBonus();
        String readyCount = reservists.countReady() + "/4";
        Label readyCountLabel = new Label(readyCount, MyGdxGame.skin);
        readyCountLabel.setColor(Color.CYAN);
        float indicatorX = hero.getX() + hero.getWidth() - readyCountLabel.getPrefWidth();
        readyCountLabel.setPosition(indicatorX, hero.getY());
        handStage.addActor(readyCountLabel);
        if (resAtkBonus > 0) {
          Label atkBonusLabel = new Label("+" + resAtkBonus, MyGdxGame.skin);
          atkBonusLabel.setColor(Color.RED);
          atkBonusLabel.setPosition(hero.getX() + hero.getWidth() - atkBonusLabel.getPrefWidth(),
              hero.getY() + readyCountLabel.getPrefHeight() + 2f);
          handStage.addActor(atkBonusLabel);
        }
      }

      if ("Saboteurs".equals(hero.getHeroName())) {
        Saboteurs saboteurs = (Saboteurs) hero;
        String sabCount = saboteurs.countReady() + "/2";
        Label sabCountLabel = new Label(sabCount, MyGdxGame.skin);
        sabCountLabel.setColor(Color.RED);
        sabCountLabel.setPosition(hero.getX() + hero.getWidth() - sabCountLabel.getPrefWidth(), hero.getY());
        handStage.addActor(sabCountLabel);
        int recovering = saboteurs.countRecovering();
        if (recovering > 0) {
          Label recLabel = new Label(String.valueOf(recovering), MyGdxGame.skin);
          recLabel.setColor(Color.ORANGE);
          recLabel.setPosition(hero.getX() + hero.getWidth() - recLabel.getPrefWidth(),
              hero.getY() + sabCountLabel.getPrefHeight() + 2f);
          handStage.addActor(recLabel);
        }
      }
    }

    // Turn info and button
    finishTurnButton = new TextButton("Finish turn", MyGdxGame.skin);
    finishTurnButton.setSize(finishTurnButton.getPrefWidth() + 10, finishTurnButton.getPrefHeight());
    finishTurnButton.setPosition(MyGdxGame.WIDTH - finishTurnButton.getWidth(), 0);
    myPlayerLabel = new Label(currentPlayer.getPlayerName(), MyGdxGame.skin);
    myPlayerLabel.setColor(Color.WHITE);
    boolean isMyTurn = !isSpectator && (gameState.getCurrentPlayer() == currentPlayer);
    TextButton getHeroActionBtn = null;
    TextButton sellHeroActionBtn = null;

    // "Sacrifice Joker" button — only on your turn, bottom-left of hand stage
    if (isMyTurn && !currentPlayer.getPlayerTurn().isHeroSelectionPending()) {
      Card jokerInHand = null;
      for (Card hc : handCards) {
        if ("joker".equals(hc.getSymbol())) { jokerInHand = hc; break; }
      }
      if (jokerInHand != null) {
        final Card theJoker = jokerInHand;
        TextButton heroBtn = new TextButton("Get Hero", MyGdxGame.skin);
        float hBtnW = heroBtn.getPrefWidth() + 4;
        float hBtnH = heroBtn.getPrefHeight();
        heroBtn.setSize(hBtnW, hBtnH);
        heroBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            performJokerSacrifice(theJoker);
          }
        });
        getHeroActionBtn = heroBtn;
      }
    }

    // "Sell Hero" button — only on your turn, no ongoing auction, at least one hero owned
    if (isMyTurn && pendingHeroAuction == null && auctionSellHeroName == null
        && !playerHeroes.isEmpty()) {
      final ArrayList<Hero> phForSell = playerHeroes;
      TextButton sellHeroBtn = new TextButton("Sell Hero", MyGdxGame.skin);
      sellHeroBtn.setSize(sellHeroBtn.getPrefWidth() + 4, sellHeroBtn.getPrefHeight());
      sellHeroBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          if (phForSell.size() == 1) {
            auctionSellHeroName = phForSell.get(0).getHeroName();
          } else {
            auctionSellHeroName = "__SELECT__";
          }
          auctionSellMinBid = 1;
          gameState.setUpdateState(true);
        }
      });
      sellHeroActionBtn = sellHeroBtn;
    }

    // Spectators: hide finish-turn button and show a spectator indicator instead.
    // Regular players: show button only on their turn.
    if (isSpectator) {
      finishTurnButton.setVisible(false);
      Label spectatorLabel = new Label("Spectator Mode", MyGdxGame.skin);
      spectatorLabel.setColor(Color.CYAN);
      spectatorLabel.setPosition(MyGdxGame.WIDTH - spectatorLabel.getPrefWidth(), 0);
      handStage.addActor(spectatorLabel);
    } else if (isMyTurn && (currentPlayer.getPlayerTurn().isAttackPending() || pendingAttackBroadcast != null)) {
      finishTurnButton.setVisible(false);
    } else if (isMyTurn && pendingExposeCard) {
      finishTurnButton.setVisible(false);
      // Defensive: also disable touch on the hidden finish-turn button so it can
      // never absorb the slot button click after being re-added on top of the overlay
      // (recurring bug: "finish turn does nothing").
      finishTurnButton.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
      // Self-heal: if there is no covered defense card to expose (e.g. state
      // changed before the overlay rebuild), drop the flag and fall through
      // so the regular finish-turn button is shown instead of a dead overlay.
      boolean stillHasCovered = false;
      for (Card c : currentPlayer.getDefCards().values()) {
        if (c.isCovered()) { stillHasCovered = true; break; }
      }
      if (!stillHasCovered) {
        for (Card c : currentPlayer.getTopDefCards().values()) {
          if (c.isCovered()) { stillHasCovered = true; break; }
        }
      }
      if (!stillHasCovered) {
        pendingExposeCard = false;
        finishTurnButton.setVisible(isMyTurn);
        finishTurnButton.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        finishTurnButtonListener = new FinishTurnButtonListener(gameState, socket);
        finishTurnButton.addListener(finishTurnButtonListener);
      }
      // addExposeCardOverlay() is called at the end of showHandStage() so it
      // renders on top of all other handStage actors (hudPanel, sellHeroBtn etc.)
    } else {
      finishTurnButton.setVisible(isMyTurn);
      finishTurnButton.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
      finishTurnButtonListener = new FinishTurnButtonListener(gameState, socket) {
        private boolean checkedPenalty = false;
        @Override
        public void clicked(InputEvent event, float x, float y) {
          if (checkedPenalty) { super.clicked(event, x, y); return; }
          checkedPenalty = true;
          if (currentPlayer.getPlayerTurn().getAttackCounter() == 0) {
            boolean hasCoveredCard = false;
            for (Card c : currentPlayer.getDefCards().values()) {
              if (c.isCovered()) { hasCoveredCard = true; break; }
            }
            if (!hasCoveredCard) {
              for (Card c : currentPlayer.getTopDefCards().values()) {
                if (c.isCovered()) { hasCoveredCard = true; break; }
              }
            }
            if (hasCoveredCard) {
              pendingExposeCard = true;
              gameState.setUpdateState(true);
              return;
            }
            Card king = currentPlayer.getKingCard();
            if (king != null && king.isCovered()) {
              try {
                JSONObject exposeData = new JSONObject();
                exposeData.put("playerIdx", playerIndex);
                socket.emit("exposeKingCard", exposeData);
              } catch (JSONException ex) { ex.printStackTrace(); }
            }
          }
          super.clicked(event, x, y);
          tutorialAdvance(TUTORIAL_STEP_ENDTURN);
          tutorialAdvanceHook("FINISH_TURN");
        }
      };
      finishTurnButton.addListener(finishTurnButtonListener);
    }

    // ----- Player HUD panel (name chip + action indicators) -----
    float hudPad = 4f;
    float iconH = 512f / 10f;

    // Shield availability
    Marshal marshalHero = null;
    for (int mi = 0; mi < currentPlayer.getHeroes().size(); mi++) {
      if (currentPlayer.getHeroes().get(mi).getHeroName() == "Marshal") {
        marshalHero = (Marshal) currentPlayer.getHeroes().get(mi);
        break;
      }
    }
    PlayerTurn ptHand = currentPlayer.getPlayerTurn();
    boolean takeShieldAvail = isMyTurn && (marshalHero != null
        ? marshalHero.getMobilizations() > 0 : ptHand.getTakeDefCard() > 0);
    boolean putShieldAvail  = isMyTurn && (marshalHero != null
        ? marshalHero.getMobilizations() > 0 : ptHand.getPutDefCard() > 0);

    // Attacking symbols — resolve source textures and native dimensions
    String attackingSymbol    = ptHand.getAttackingSymbol()[0];
    String attackingSymbolExt = ptHand.getAttackingSymbol()[1];
    boolean hasTwoSymbols = !"none".equals(attackingSymbolExt);

    Texture sym1Tex; int sym1W; int sym1H;
    if ("hearts".equals(attackingSymbol)) {
      sym1Tex = texHeartsRed;   sym1W = 512; sym1H = 512;
    } else if ("diamonds".equals(attackingSymbol)) {
      sym1Tex = texDiamondsRed; sym1W = 512; sym1H = 512;
    } else if ("clubs".equals(attackingSymbol)) {
      sym1Tex = texClubs;       sym1W = 512; sym1H = 512;
    } else if ("spades".equals(attackingSymbol)) {
      sym1Tex = texSpades;      sym1W = 512; sym1H = 512;
    } else {
      sym1Tex = texSomeSymbol;  sym1W = 342; sym1H = 512;
    }

    Texture sym2Tex = texSomeSymbol; int sym2W = 342; int sym2H = 512;
    if (hasTwoSymbols) {
      if ("hearts".equals(attackingSymbolExt)) {
        sym2Tex = texHeartsRed;   sym2W = 512; sym2H = 512;
      } else if ("diamonds".equals(attackingSymbolExt)) {
        sym2Tex = texDiamondsRed; sym2W = 512; sym2H = 512;
      } else if ("clubs".equals(attackingSymbolExt)) {
        sym2Tex = texClubs;       sym2W = 512; sym2H = 512;
      } else if ("spades".equals(attackingSymbolExt)) {
        sym2Tex = texSpades;      sym2W = 512; sym2H = 512;
      }
    }

    // Icon row: always [take-shield] [put-shield] [symbol-slot] — fixed total width
    Table iconsRow = new Table();
    final Color shieldAvailColor = new Color(0.1f, 1f, 0.1f, 1f);
    final Color shieldDimColor   = new Color(0.25f, 0.25f, 0.25f, 0.6f);

    // Take-defense shield — always visible; green highlight + bright icon when available
    Table arrowCell = new Table(MyGdxGame.skin);
    if (takeShieldAvail) {
      arrowCell.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0.45f, 0f, 0.55f)));
    }
    Image arrowShieldImg = new Image(new TextureRegion(texArrowDownShield,
        0, 0, texArrowDownShield.getWidth(), texArrowDownShield.getHeight())) {
      @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
    };
    arrowShieldImg.setColor(takeShieldAvail ? shieldAvailColor : shieldDimColor);
    arrowCell.add(arrowShieldImg).size(iconH * 0.85f, iconH * 0.85f).pad(iconH * 0.075f);
    iconsRow.add(arrowCell).size(iconH, iconH).padRight(2f);

    // Put-defense shield — always visible
    Table checkCell = new Table(MyGdxGame.skin);
    if (putShieldAvail) {
      checkCell.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0.45f, 0f, 0.55f)));
    }
    Image shieldCheckImg = new Image(new TextureRegion(texShieldCheck,
        0, 0, texShieldCheck.getWidth(), texShieldCheck.getHeight())) {
      @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
    };
    shieldCheckImg.setColor(putShieldAvail ? shieldAvailColor : shieldDimColor);
    checkCell.add(shieldCheckImg).size(iconH * 0.85f, iconH * 0.85f).pad(iconH * 0.075f);
    iconsRow.add(checkCell).size(iconH, iconH).padRight(2f);

    // Symbol slot: always iconH x iconH total.
    // One symbol: full icon.
    // Two symbols: both drawn at full size, sym1 shifted left by iconH/2, sym2 shifted right by iconH/2.
    // No tinting — textures display their natural colors.
    if (!hasTwoSymbols) {
      Image sym1Img = new Image(new TextureRegion(sym1Tex, 0, 0, sym1W, sym1H));
      iconsRow.add(sym1Img).size(iconH, iconH);
    } else {
      final float iH = iconH;
      final Texture fSym1Tex = sym1Tex; final int fSym1W = sym1W; final int fSym1H = sym1H;
      final Texture fSym2Tex = sym2Tex; final int fSym2W = sym2W; final int fSym2H = sym2H;
      com.badlogic.gdx.scenes.scene2d.Group symGroup =
          new com.badlogic.gdx.scenes.scene2d.Group();
      symGroup.setSize(iH, iH);
      Image sym1Img = new Image(new TextureRegion(fSym1Tex, 0, 0, fSym1W, fSym1H));
      sym1Img.setSize(iH, iH);
      sym1Img.setPosition(-iH / 4f, 0f);
      Image sym2Img = new Image(new TextureRegion(fSym2Tex, 0, 0, fSym2W, fSym2H));
      sym2Img.setSize(iH, iH);
      sym2Img.setPosition(iH / 4f, 0f);
      symGroup.addActor(sym1Img);
      symGroup.addActor(sym2Img);
      iconsRow.add(symGroup).size(iH, iH);
    }

    // Unified HUD panel: dark semi-transparent background, name above icons
    // Row 3 (bottom bar): HUD panel left, icon buttons centre-left, finish-turn right
    Table hudPanel = new Table(MyGdxGame.skin);
    hudPanel.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.4f)));
    hudPanel.pad(hudPad);
    hudPanel.add(myPlayerLabel).padBottom(2f).row();
    hudPanel.add(iconsRow);
    hudPanel.pack();
    hudPanel.setPosition(2f, 2f);
    handStage.addActor(hudPanel);

    // History and Chat icon buttons — same size as HUD attack/shield icons
    float iconBtnSize = iconH;
    float iconBtnX = hudPanel.getWidth() + 6f;
    float iconBtnY = 2f;

    Image historyIconImg = new Image(new TextureRegionDrawable(new TextureRegion(texHistoryIcon)));
    historyIconImg.setSize(iconBtnSize, iconBtnSize);
    historyIconImg.setPosition(iconBtnX, iconBtnY);
    historyIconImg.setColor(Color.WHITE);
    historyIconImg.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) { showLogOverlay(); }
    });
    handStage.addActor(historyIconImg);

    final float chatIconX = iconBtnX + iconBtnSize + 4f;
    Image chatIconImg = new Image(new TextureRegionDrawable(new TextureRegion(texChatIcon)));
    chatIconImg.setSize(iconBtnSize, iconBtnSize);
    chatIconImg.setPosition(chatIconX, iconBtnY);
    chatIconImg.setColor(Color.WHITE);
    chatIconImg.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) { showChatOverlay(); }
    });
    handStage.addActor(chatIconImg);

    // Unread chat badge — green number centered on chat icon (50% larger font)
    Label badge = new Label(unreadChatMessages > 0 ? String.valueOf(unreadChatMessages) : "", MyGdxGame.skin);
    badge.setFontScale(1.5f);
    badge.setColor(new Color(0.1f, 0.9f, 0.1f, 1f));
    badge.setVisible(unreadChatMessages > 0);
    badge.setPosition(chatIconX + (iconBtnSize - badge.getPrefWidth()) / 2f,
                      iconBtnY  + (iconBtnSize - badge.getPrefHeight()) / 2f);
    handStage.addActor(badge);
    chatBadgeLabel = badge;

    // Unread history-log badge — green number centered on history icon
    int unreadLog = activityLog.length() - logLastRenderedCount;
    if (logOpen) unreadLog = 0; // don't count entries the player is currently viewing
    Label logBadge = new Label(unreadLog > 0 ? (unreadLog >= 100 ? "99+" : String.valueOf(unreadLog)) : "", MyGdxGame.skin);
    logBadge.setFontScale(1.5f);
    logBadge.setColor(new Color(0.1f, 0.9f, 0.1f, 1f));
    logBadge.setVisible(unreadLog > 0);
    logBadge.setPosition(iconBtnX + (iconBtnSize - logBadge.getPrefWidth()) / 2f,
                         iconBtnY + (iconBtnSize - logBadge.getPrefHeight()) / 2f);
    handStage.addActor(logBadge);
    logBadgeLabel = logBadge;

    // Action buttons (Get Hero / Sell Hero) — hero row, right edge
    float heroActionBtnY = bottomBarH + 2f;
    if (sellHeroActionBtn != null && getHeroActionBtn != null) {
      getHeroActionBtn.setPosition(MyGdxGame.WIDTH - getHeroActionBtn.getPrefWidth() - 2f, heroActionBtnY);
      sellHeroActionBtn.setPosition(getHeroActionBtn.getX() - sellHeroActionBtn.getPrefWidth() - 4f, heroActionBtnY);
      handStage.addActor(sellHeroActionBtn);
      handStage.addActor(getHeroActionBtn);
    } else if (sellHeroActionBtn != null) {
      sellHeroActionBtn.setPosition(MyGdxGame.WIDTH - sellHeroActionBtn.getPrefWidth() - 2f, heroActionBtnY);
      handStage.addActor(sellHeroActionBtn);
    } else if (getHeroActionBtn != null) {
      getHeroActionBtn.setPosition(MyGdxGame.WIDTH - getHeroActionBtn.getPrefWidth() - 2f, heroActionBtnY);
      handStage.addActor(getHeroActionBtn);
    }

    handStage.addActor(finishTurnButton);
    // Expose-card overlay must be added last so its actors render on top of
    // hudPanel, sellHeroActionBtn and finishTurnButton.
    if (isMyTurn && pendingExposeCard) addExposeCardOverlay();
  }

  private void addExposeCardOverlay() {
    float stageW = MyGdxGame.WIDTH;
    float stageH = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;

    // Wrap all overlay actors in a Group so we can remove them instantly on the
    // first tap — eliminating the "frozen" appearance while the render loop catches up.
    final com.badlogic.gdx.scenes.scene2d.Group overlayGroup =
        new com.badlogic.gdx.scenes.scene2d.Group();
    overlayGroup.setSize(stageW, stageH);

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setSize(stageW, stageH);
    bg.setPosition(0, 0);
    bg.setColor(0f, 0f, 0f, 0.72f);
    // The veil is purely visual — keep it non-touchable so it can never absorb the
    // slot button click in any race condition (recurring bug: "finish turn does
    // nothing" when the user taps the expose slot button).
    bg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    overlayGroup.addActor(bg);

    Label prompt = new Label("No attack -- expose a defense card:", MyGdxGame.skin);
    prompt.setColor(Color.YELLOW);
    prompt.setPosition(stageW / 2f - prompt.getPrefWidth() / 2f, stageH - prompt.getPrefHeight() - 6);
    overlayGroup.addActor(prompt);

    float btnW = stageW / 4f;
    float btnX = 4;
    int buttonsAdded = 0;
    Map<Integer, Card> defCards    = currentPlayer.getDefCards();
    Map<Integer, Card> topDefCards = currentPlayer.getTopDefCards();
    for (int slot = 1; slot <= 3; slot++) {
      Card covered = null;
      if (topDefCards.containsKey(slot) && topDefCards.get(slot).isCovered()) {
        covered = topDefCards.get(slot);
      } else if (defCards.containsKey(slot) && defCards.get(slot).isCovered()) {
        covered = defCards.get(slot);
      }
      if (covered == null) continue;
      final int finalSlot = slot;
      TextButton slotBtn = new TextButton("Slot " + slot, MyGdxGame.skin);
      slotBtn.setSize(btnW, slotBtn.getPrefHeight() * 1.5f);
      slotBtn.setPosition(btnX, stageH / 2f - slotBtn.getHeight() / 2f);
      btnX += btnW + 4;
      slotBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
        // Use touchDown rather than ClickListener.clicked so the event fires even if a
        // stateUpdate destroys/recreates the slot button between touchDown and touchUp
        // (recurring bug: "select card to expose, nothing happens").
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
          overlayGroup.remove(); // immediate visual feedback — don't wait for render loop
          submitExposeAndFinishTurn(finalSlot);
          return true;
        }
      });
      overlayGroup.addActor(slotBtn);
      buttonsAdded++;
    }
    // Fallback: if no covered slots exist, automatically cancel expose and end turn.
    // This guards against an edge case where the state was already updated server-side.
    if (buttonsAdded == 0) {
      pendingExposeCard = false;
      Label noSlots = new Label("(no covered card to expose — ending turn)", MyGdxGame.skin);
      noSlots.setColor(Color.LIGHT_GRAY);
      noSlots.setPosition(stageW / 2f - noSlots.getPrefWidth() / 2f, stageH / 2f);
      handStage.addActor(noSlots);
      try {
        JSONObject ftData = new JSONObject();
        ftData.put("currentPlayerIndex", gameState.getCurrentPlayerIndex());
        socket.emit("finishTurn", ftData);
      } catch (JSONException ex) { ex.printStackTrace(); }
    } else {
      handStage.addActor(overlayGroup);
    }
  }

  /** True when this client is in the "choose a covered defense card to expose" state. */
  public boolean isPendingExpose() {
    return pendingExposeCard;
  }

  /**
   * Submit the choice of which slot to expose, then end the turn.
   * Called by the slot buttons in {@link #addExposeCardOverlay()} and by
   * OwnDefCardListener when the player taps a covered own defense card directly.
   */
  public void submitExposeAndFinishTurn(int slot) {
    if (!pendingExposeCard) return;
    pendingExposeCard = false;
    try {
      JSONObject exposeData = new JSONObject();
      exposeData.put("playerIdx", playerIndex);
      exposeData.put("slot", slot);
      socket.emit("exposeDefCard", exposeData);
      JSONObject ftData = new JSONObject();
      ftData.put("currentPlayerIndex", gameState.getCurrentPlayerIndex());
      socket.emit("finishTurn", ftData);
      tutorialAdvance(TUTORIAL_STEP_ENDTURN);
      tutorialAdvanceHook("FINISH_TURN");
    } catch (JSONException ex) { ex.printStackTrace(); }
    gameState.setUpdateState(true);
  }

  private void showHeroInfoOverlay(String heroName) {
    menuOpen = true;
    overlayStage.clear();

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0, 0, 0, 0.82f);
    overlayStage.addActor(bg);

    Table outer = new Table();
    outer.setFillParent(true);
    outer.top().pad(20f);

    Label titleLabel = new Label(heroName, MyGdxGame.skin);
    titleLabel.setColor(Color.GOLD);
    outer.add(titleLabel).padBottom(10).row();

    Table descTable = new Table();
    descTable.top().left().pad(4f);
    Label descLabel = new Label(getHeroDescription(heroName), MyGdxGame.skin);
    descLabel.setWrap(true);
    descTable.add(descLabel).left().expandX().fillX().row();
    ScrollPane descScroll = new ScrollPane(descTable, MyGdxGame.skin);
    descScroll.setFadeScrollBars(false);
    descScroll.setScrollingDisabled(true, false);
    outer.add(descScroll).expandX().fillX().expandY().fillY().padBottom(8f).row();

    TextButton closeBtn = new TextButton("Close", MyGdxGame.skin);
    closeBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        closeMenu();
      }
    });
    outer.add(closeBtn).width(300).height(60).padTop(8).row();

    overlayStage.addActor(outer);
  }

  private static String getHeroDescription(String name) {
    return HeroDescriptions.get(name);
  }

  private void showInGameMenu() {
    menuOpen = true;
    buildMenuOverlay();
    // render() will enforce overlayStage as input processor while menuOpen is true
  }

  private void buildMenuOverlay() {
    logOpen = false;
    chatOpen = false;
    overlayStage.clear();

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0, 0, 0, 0.6f);
    bg.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        closeMenu();
      }
    });
    overlayStage.addActor(bg);

    Table table = new Table();
    table.setFillParent(true);

    Label titleLabel = new Label("Game Menu", MyGdxGame.skin);
    table.add(titleLabel).padBottom(20).row();

    TextButton resumeBtn = new TextButton("Resume", MyGdxGame.skin);
    resumeBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        closeMenu();
      }
    });
    table.add(resumeBtn).width(300).height(90).padBottom(5).row();

    TextButton historyBtn = new TextButton("History", MyGdxGame.skin);
    historyBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        GameScreen.this.showLogOverlay();
      }
    });
    table.add(historyBtn).width(300).height(90).padBottom(5).row();

    if (isSpectator || (currentPlayer != null && currentPlayer.isOut())) {
      TextButton leaveBtn = new TextButton("Leave Game", MyGdxGame.skin);
      leaveBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          closeMenu();
          navigateToLobby();
        }
      });
      table.add(leaveBtn).width(300).height(90).row();
    } else {
      TextButton giveUpStayBtn = new TextButton("Give Up & Stay", MyGdxGame.skin);
      giveUpStayBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          closeMenu();
          emitGiveUp();
        }
      });
      table.add(giveUpStayBtn).width(300).height(90).padBottom(5).row();

      TextButton giveUpLeaveBtn = new TextButton("Give Up & Leave", MyGdxGame.skin);
      giveUpLeaveBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          closeMenu();
          emitGiveUpAndLeave();
        }
      });
      table.add(giveUpLeaveBtn).width(300).height(90).row();
    }

    overlayStage.addActor(table);
  }

  private void refreshLogOverlay() {
    if (logScrollPane == null || logInnerTable == null) return;
    int count = activityLog.length();
    if (count <= logLastRenderedCount) return;
    boolean wasAtBottom = logScrollPane.getScrollPercentY() >= 0.95f;
    try {
      for (int i = logLastRenderedCount; i < count; i++) {
        JSONObject entry = activityLog.getJSONObject(i);
        String text = entry.optString("text", "");
        boolean neutral = entry.optBoolean("neutral", false);
        boolean success = entry.optBoolean("success", true);
        Label lbl = new Label(text, MyGdxGame.skin);
        lbl.setWrap(true);
        Color lc = neutral
            ? new Color(0.85f, 0.85f, 0.85f, 1f)
            : (success ? new Color(0.3f, 0.95f, 0.3f, 1f) : new Color(0.95f, 0.3f, 0.25f, 1f));
        lbl.setColor(lc);
        logInnerTable.add(lbl).left().padBottom(4f).expandX().fillX().row();
      }
    } catch (JSONException e) { e.printStackTrace(); }
    logLastRenderedCount = count;
    logScrollPane.layout();
    if (wasAtBottom) logScrollPane.setScrollPercentY(1f);
  }

  private void showLogOverlay() {
    menuOpen = true;
    logOpen = true;
    if (logBadgeLabel != null) logBadgeLabel.setVisible(false);
    overlayStage.clear();

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0, 0, 0, 0.82f);
    overlayStage.addActor(bg);

    Table outer = new Table();
    outer.setFillParent(true);
    outer.top().pad(20f);

    Label titleLabel = new Label("History", MyGdxGame.skin);
    outer.add(titleLabel).padBottom(10).row();

    // Scrollable inner table holds all log entries
    final Table inner = new Table();
    inner.top().left().pad(6f);
    logInnerTable = inner;

    if (activityLog.length() == 0) {
      Label emptyLabel = new Label("No history yet.", MyGdxGame.skin);
      emptyLabel.setColor(Color.GRAY);
      inner.add(emptyLabel).row();
      logLastRenderedCount = 0;
    } else {
      try {
        for (int i = 0; i < activityLog.length(); i++) {
          JSONObject entry = activityLog.getJSONObject(i);
          String text = entry.optString("text", "");
          boolean neutral = entry.optBoolean("neutral", false);
          boolean success = entry.optBoolean("success", true);
          Label lbl = new Label(text, MyGdxGame.skin);
          lbl.setWrap(true);
          Color lc = neutral
              ? new Color(0.85f, 0.85f, 0.85f, 1f)
              : (success ? new Color(0.3f, 0.95f, 0.3f, 1f) : new Color(0.95f, 0.3f, 0.25f, 1f));
          lbl.setColor(lc);
          inner.add(lbl).left().padBottom(4f).expandX().fillX().row();
        }
      } catch (JSONException e) { e.printStackTrace(); }
      logLastRenderedCount = activityLog.length();
    }

    ScrollPane scroll = new ScrollPane(inner, MyGdxGame.skin);
    logScrollPane = scroll;
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    scroll.layout();
    scroll.setScrollPercentY(1f); // start scrolled to the bottom (most recent)
    outer.add(scroll).expandX().fillX().expandY().fillY().padBottom(8f).row();

    TextButton backBtn = new TextButton("Back", MyGdxGame.skin);
    backBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        closeMenu();
      }
    });
      outer.add(backBtn).width(300).height(90).padTop(8).row();

    overlayStage.addActor(outer);
  }

  private void showChatOverlay() {
    menuOpen = true;
    chatOpen = true;
    unreadChatMessages = 0;
    if (chatBadgeLabel != null) chatBadgeLabel.setVisible(false);
    overlayStage.clear();

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0, 0, 0, 0.82f);
    overlayStage.addActor(bg);

    final Table outer = new Table();
    outer.setFillParent(true);
    outer.top().pad(20f);

    Label titleLabel = new Label("Chat", MyGdxGame.skin);
    outer.add(titleLabel).padBottom(10).row();

    final Table inner = new Table();
    inner.top().left().pad(6f);
    chatInnerTable = inner;

    for (String[] msg : chatMessages) {
      String displayText = "[" + msg[0] + "] " + msg[1];
      Label lbl = new Label(displayText, MyGdxGame.skin);
      lbl.setWrap(true);
      lbl.setColor(new Color(0.85f, 0.95f, 1f, 1f));
      inner.add(lbl).left().padBottom(4f).expandX().fillX().row();
    }

    ScrollPane scroll = new ScrollPane(inner, MyGdxGame.skin);
    chatScrollPane = scroll;
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    scroll.layout();
    scroll.setScrollPercentY(1f);
    outer.add(scroll).expandX().fillX().expandY().fillY().padBottom(8f).row();

    // Input row
    final com.badlogic.gdx.scenes.scene2d.ui.TextField inputField =
        new com.badlogic.gdx.scenes.scene2d.ui.TextField("", MyGdxGame.skin);
    inputField.setMessageText("Type a message...");

    // Shared send action — used by the Enter key callback.
    // Use a Runnable[] wrapper so the lambda can reference itself (re-shows keyboard
    // after sending so the user can type the next message without re-tapping).
    final Runnable[] doSendRef = { null };
    doSendRef[0] = new Runnable() {
      @Override public void run() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        MyGdxGame.keyboardHelper.showKeyboard(inputField, doSendRef[0]);
        try {
          JSONObject msg = new JSONObject();
          String senderName = currentPlayer != null ? currentPlayer.getPlayerName() : "?";
          msg.put("name", senderName);
          msg.put("text", text);
          socket.emit("chatMessage", msg);
        } catch (JSONException e) { e.printStackTrace(); }
      }
    };
    final Runnable doSend = doSendRef[0];

    // On mobile browsers, focus the hidden native input to open the keyboard.
    // Pass doSend as the Enter callback so pressing Done/Return triggers a send.
    inputField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
      @Override
      public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                               float x, float y, int pointer, int btn) {
        MyGdxGame.keyboardHelper.showKeyboard(inputField, doSend);
        return false;
      }
    });

    Table inputRow = new Table();
    inputRow.add(inputField).expandX().fillX().padLeft(40f).padRight(40f);
    outer.add(inputRow).expandX().fillX().padBottom(6f).row();

    TextButton backBtn = new TextButton("Back", MyGdxGame.skin);
    backBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) { closeMenu(); }
    });
    outer.add(backBtn).width(300).height(90).padTop(4).row();

    overlayStage.addActor(outer);
  }

  // ── Card zoom helpers (issue #218) ─────────────────────────────────────────

  private boolean nothingSelectedInHand() {
    // Block hover-zoom only when the player has attack cards selected (hand, king, heroes).
    // Def-card selection must NOT block zoom — switching between own def cards would
    // otherwise leave the newly-hovered card un-zoomed (the reported bug).
    if (players == null || playerIndex < 0 || playerIndex >= players.size()) return true;
    Player local = players.get(playerIndex);
    if (local == null) return true;
    if (!local.getSelectedHandCards().isEmpty()) return false;
    Card king = local.getKingCard();
    if (king != null && king.isSelected()) return false;
    if (!local.getSelectedHeroes().isEmpty()) return false;
    return true;
  }

  private void zoomCard(Card card) {
    // Unzoom the previously zoomed card before switching to a new one
    if (currentlyZoomedCard != null && currentlyZoomedCard != card) {
      // King card may have been reparented to overlayStage for zoom — restore it first
      if (currentlyZoomedCard.getStage() == overlayStage) {
        float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
        currentlyZoomedCard.setY(currentlyZoomedCard.getY() - oy);
        gameStage.addActor(currentlyZoomedCard);
      }
      currentlyZoomedCard.setScale(1f);
      currentlyZoomedCard = null;
    }
    // Also unzoom any currently zoomed deck
    if (currentlyZoomedDeck != null) {
      setDeckScale(currentlyZoomedDeck, 1f);
      currentlyZoomedDeck = null;
    }
    float w = card.getWidth();
    float h = card.getHeight();
    float cx = card.getX();
    float cy = card.getY();
    float s = CARD_ZOOM;
    float W = MyGdxGame.WIDTH;

    // Margins keep the zoomed card a bit inside the play-area edges so it is
    // never cut off and the bottom-player king never touches the hand strip.
    float MARGIN_X      = 4f;
    float MARGIN_TOP    = 4f;
    float MARGIN_BOTTOM = 14f; // extra clearance from the hand strip below the play area

    // Default: scale around the card centre, then clamp the origin so the
    // visible (scaled) card stays within [MARGIN, W - MARGIN] on each axis.
    // Visible left   = cx + originX*(1-s)
    // Visible right  = cx + originX*(1-s) + w*s
    // Constraint     left   >= MARGIN_X  ⇔  originX <= (cx - MARGIN_X)/(s-1)
    // Constraint     right  <= W - MARGIN_X ⇔ originX >= (cx + w*s - W + MARGIN_X)/(s-1)
    float originX = w / 2f;
    float maxOriginX = (cx - MARGIN_X) / (s - 1f);
    float minOriginX = (cx + w * s - W + MARGIN_X) / (s - 1f);
    if (originX > maxOriginX) originX = maxOriginX;
    if (originX < minOriginX) originX = minOriginX;

    float originY = h / 2f;
    float maxOriginY = (cy - MARGIN_BOTTOM) / (s - 1f);
    float minOriginY = (cy + h * s - W + MARGIN_TOP) / (s - 1f);
    if (originY > maxOriginY) originY = maxOriginY;
    if (originY < minOriginY) originY = minOriginY;

    card.setOriginX(originX);
    card.setOriginY(originY);
    card.setScale(CARD_ZOOM);
    card.toFront();
    currentlyZoomedCard = card;
  }

  private void unzoomCard(Card card) {
    card.setScale(1f);
    if (currentlyZoomedCard == card) currentlyZoomedCard = null;
  }

  /** Turns off zoom mode, restores any zoomed card/deck, and resets the lens button tint. */
  public void deactivateZoomMode() {
    zoomModeActive = false;
    if (currentlyZoomedCard != null) {
      // If the king card was reparented to overlayStage for zoom, restore it first
      if (currentlyZoomedCard.getStage() == overlayStage) {
        float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
        currentlyZoomedCard.setY(currentlyZoomedCard.getY() - oy);
        gameStage.addActor(currentlyZoomedCard);
      }
      unzoomCard(currentlyZoomedCard);
    }
    if (currentlyZoomedDeck != null) {
      setDeckScale(currentlyZoomedDeck, 1f);
      currentlyZoomedDeck = null;
    }
    if (zoomModeBtn != null) zoomModeBtn.setColor(Color.WHITE);
  }

  private void attachZoomListener(final Card card) {
    card.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (!zoomModeActive) return false; // zoom only when lens is selected
        if (card == currentlyZoomedCard) {
          unzoomCard(card); // click same card = toggle off
        } else {
          zoomCard(card);
        }
        return false;
      }
    });
  }

  // Own king zoom: reparents the card to overlayStage when zoomed so it renders
  // above the hand strip. Only active in zoom mode (lens button selected).
  private void attachKingOverlayZoomListener(final Card card) {
    final float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
    card.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (!zoomModeActive) return false; // zoom only when lens is selected
        if (card == currentlyZoomedCard) {
          // Toggle off: restore from overlayStage if needed
          if (card.getStage() == overlayStage) {
            card.setY(card.getY() - oy);
            gameStage.addActor(card);
          }
          unzoomCard(card);
        } else {
          // Zoom: move to overlayStage so it renders above the hand strip
          if (card.getStage() != overlayStage) {
            card.setY(card.getY() + oy);
            overlayStage.addActor(card);
          }
          zoomCard(card);
        }
        return false;
      }
    });
  }

  private void attachPickingDeckZoom(final PickingDeck deck) {
    // Uses InputListener (not ClickListener) to avoid cancelling touch focus of
    // PickingDeckListener, which would prevent looting from firing (bots get stuck).
    // Zoom only active when lens button is selected.
    deck.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (!zoomModeActive) return false; // zoom only when lens is selected
        // Clear any previously zoomed card (restoring king from overlayStage if needed)
        if (currentlyZoomedCard != null) {
          if (currentlyZoomedCard.getStage() == overlayStage) {
            float oy = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;
            currentlyZoomedCard.setY(currentlyZoomedCard.getY() - oy);
            gameStage.addActor(currentlyZoomedCard);
          }
          currentlyZoomedCard.setScale(1f);
          currentlyZoomedCard = null;
        }
        // Toggle deck zoom
        if (currentlyZoomedDeck == deck) {
          setDeckScale(deck, 1f);
          currentlyZoomedDeck = null;
        } else {
          if (currentlyZoomedDeck != null) setDeckScale(currentlyZoomedDeck, 1f);
          currentlyZoomedDeck = deck;
          setDeckScale(deck, CARD_ZOOM);
        }
        return false; // do not consume — let PickingDeckListener still fire
      }
    });
  }

  private void setDeckScale(PickingDeck deck, float scale) {
    for (Card c : deck.getCards()) {
      c.setOriginX(c.getWidth() / 2f);
      c.setOriginY(c.getHeight() / 2f);
      c.setScale(scale);
      if (scale > 1f) {
        c.toFront(); // bring above defense cards when zoomed
      }
    }
    if (scale > 1f) {
      // Keep the PickingDeck actor on top of its own cards so it still
      // intercepts touches (required for PickingDeckListener to fire).
      deck.toFront();
    }
  }

  /** Creates a simple white magnifying-glass icon of the given pixel size using Pixmap. */
  private Texture createMagnifierTexture(int size) {
    Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    pm.setColor(0f, 0f, 0f, 0f);
    pm.fill();
    pm.setColor(Color.WHITE);
    // Glass ring: center at ~38%, radius ~23%
    int cx = Math.round(size * 0.38f);
    int cy = Math.round(size * 0.38f);
    int r  = Math.round(size * 0.23f);
    // Draw 3-pixel-wide ring
    pm.drawCircle(cx, cy, r - 1);
    pm.drawCircle(cx, cy, r);
    pm.drawCircle(cx, cy, r + 1);
    // Handle: diagonal line from lower-right of circle to corner
    int hx1 = Math.round(cx + r * 0.72f);
    int hy1 = Math.round(cy + r * 0.72f);
    int hx2 = size - 4;
    int hy2 = size - 4;
    // Draw 3-pixel-wide line by offsetting perpendicular to the diagonal
    pm.drawLine(hx1,     hy1,     hx2,     hy2);
    pm.drawLine(hx1 + 1, hy1,     hx2 + 1, hy2);
    pm.drawLine(hx1,     hy1 + 1, hx2,     hy2 + 1);
    Texture tex = new Texture(pm);
    pm.dispose();
    return tex;
  }

  private void addMenuButtonToOverlay() {
    float btnSize = 44f;
    float gap = 4f;

    // Zoom toggle button — to the left of the menu button
    zoomModeBtn = new Image(texZoomButton);
    zoomModeBtn.setSize(btnSize, btnSize);
    zoomModeBtn.setPosition(MyGdxGame.WIDTH - 2 * btnSize - gap, MyGdxGame.HEIGHT - btnSize);
    zoomModeBtn.setColor(zoomModeActive ? Color.YELLOW : Color.WHITE);
    zoomModeBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (zoomModeActive) {
          deactivateZoomMode();
        } else {
          zoomModeActive = true;
          if (zoomModeBtn != null) zoomModeBtn.setColor(Color.YELLOW);
        }
      }
    });
    overlayStage.addActor(zoomModeBtn);

    Image menuBtn = new Image(texMenuButton);
    menuBtn.setSize(btnSize, btnSize);
    menuBtn.setPosition(MyGdxGame.WIDTH - btnSize, MyGdxGame.HEIGHT - btnSize);
    menuBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        GameScreen.this.showInGameMenu();
      }
    });
    overlayStage.addActor(menuBtn);
  }

  private void closeMenu() {
    menuOpen = false;
    logOpen = false;
    chatOpen = false;
    overlayStage.clear();
    addMenuButtonToOverlay();
    // render() will set the correct input processor next frame
  }

  // ── Tutorial overlay ────────────────────────────────────────────────────────
  //
  // Steps:
  //   0  – Blocking intro
  //   1  – ACTION: take a defense card to hand    (auto-advances after takeDefCard)
  //   2  – ACTION: select a hand card             (auto-advances in render() poll)
  //   3  – Blocking info: attack symbols
  //   4  – ACTION: loot a harvest deck         (auto-advances after lootResolved)
  //   5  – Blocking info: loot mechanics
  //   6  – Blocking info: joker card
  //   7  – ACTION: place a defense card           (auto-advances in render() poll)
  //   8  – ACTION: end your turn                  (auto-advances after finishTurn)
  //   9  – Waiting for bot                        (auto-advances in show() when player's turn)
  //  10  – Blocking info: expose defense rule
  //  11  – Blocking info: king card usage
  //  12  – ACTION: discard all defense cards      (auto-advances in render() poll, cemetery)
  //  13  – ACTION: swap king with a hand card     (auto-advances in render() poll, coupSwap)
  //  14  – ACTION: attack with the king           (auto-advances after kingAttackResolved)
  //  15  – Blocking complete

  private static final int TUTORIAL_STEP_INTRO          = 0;
  private static final int TUTORIAL_STEP_TAKE_DEF_FIRST  = 1;
  private static final int TUTORIAL_STEP_SELECT          = 2;
  private static final int TUTORIAL_STEP_INFO_SYMBOLS    = 3;
  private static final int TUTORIAL_STEP_LOOT           = 4;
  private static final int TUTORIAL_STEP_INFO_LOOT      = 5;
  private static final int TUTORIAL_STEP_INFO_JOKER      = 6;
  private static final int TUTORIAL_STEP_DEFENSE         = 7;
  private static final int TUTORIAL_STEP_ENDTURN         = 8;
  private static final int TUTORIAL_STEP_WAITING         = 9;
  private static final int TUTORIAL_STEP_INFO_EXPOSE     = 10;
  private static final int TUTORIAL_STEP_INFO_KING       = 11;
  private static final int TUTORIAL_STEP_DISCARD_DEF     = 12;
  private static final int TUTORIAL_STEP_SWITCH_KING     = 13;
  private static final int TUTORIAL_STEP_KING_ATTACK     = 14;
  private static final int TUTORIAL_STEP_COMPLETE        = 15;
  private static final int TUTORIAL_TOTAL_STEPS          = 16;

  /**
   * Immutable data object describing a single tutorial step.
   * Blocking steps show a full-screen overlay with title/body/button.
   * Non-blocking steps show a guidance banner with bannerTitle/bannerText.
   */
  static final class TutorialStepDef {
    final boolean blocking;
    // Blocking overlay fields (used when blocking == true)
    final String title;
    final String body;
    final String buttonLabel;   // null → "Got it!"
    // Guidance banner fields (used when blocking == false)
    final String bannerTitle;
    final String bannerText;
    // Hero-tutorial: action hook string. When set on a banner step, the hero
    // tutorial advances when tutorialAdvanceHook(hook) is called. Null = no hook.
    final String hook;
    // Hero-tutorial: marks the final "complete" step (shows Back to Menu / Keep Playing).
    final boolean terminal;

    /** Blocking overlay step. */
    TutorialStepDef(String title, String body, String buttonLabel) {
      this.blocking = true;
      this.title = title;
      this.body = body;
      this.buttonLabel = buttonLabel;
      this.bannerTitle = "";
      this.bannerText = "";
      this.hook = null;
      this.terminal = false;
    }

    /** Non-blocking banner step. */
    TutorialStepDef(String bannerTitle, String bannerText) {
      this.blocking = false;
      this.title = "";
      this.body = "";
      this.buttonLabel = null;
      this.bannerTitle = bannerTitle;
      this.bannerText = bannerText;
      this.hook = null;
      this.terminal = false;
    }

    /** Hero-tutorial blocking step (with terminal flag). */
    TutorialStepDef(String title, String body, String buttonLabel, boolean terminal) {
      this.blocking = true;
      this.title = title;
      this.body = body;
      this.buttonLabel = buttonLabel;
      this.bannerTitle = "";
      this.bannerText = "";
      this.hook = null;
      this.terminal = terminal;
    }

    /** Hero-tutorial banner step with auto-advance hook (private; use {@link #banner}). */
    private TutorialStepDef(boolean bannerMarker, String bannerTitle, String bannerText, String hook) {
      this.blocking = false;
      this.title = "";
      this.body = "";
      this.buttonLabel = null;
      this.bannerTitle = bannerTitle;
      this.bannerText = bannerText;
      this.hook = hook;
      this.terminal = false;
    }

    /** Factory: hero-tutorial banner with auto-advance hook. */
    static TutorialStepDef banner(String bannerTitle, String bannerText, String hook) {
      return new TutorialStepDef(true, bannerTitle, bannerText, hook);
    }
  }

  private static final TutorialStepDef[] TUTORIAL_STEPS = {
    /* 0  INTRO */
    new TutorialStepDef(
      "Welcome to Baisch!",
      "This interactive tutorial guides you through a real game against a bot.\n\n"
      + "Follow the instructions shown at the top of the screen. "
      + "The tutorial advances automatically each time you complete an action.",
      "Let's go!"
    ),
    /* 1  TAKE_DEF_FIRST */
    new TutorialStepDef(
      "Pick up a defense card",
      "Tap one of your occupied defense slots to take that card back to your hand."
    ),
    /* 2  SELECT */
    new TutorialStepDef(
      "Select a hand card",
      "Tap any card at the bottom of the screen to select it (it will highlight)."
    ),
    /* 3  INFO_SYMBOLS */
    new TutorialStepDef(
      "Attack Symbols",
      "Every card has an attack symbol: \u2665 Hearts, \u2666 Diamonds, \u2663 Clubs, or \u2660 Spades.\n\n"
      + "You can combine multiple cards with the SAME symbol in a single attack — "
      + "their strengths add up.\n\n"
      + "Cards with DIFFERENT symbols cannot be combined. "
      + "Select a card first: all other hand cards of the same symbol will be combinable.\n\n"
      + "Your active symbol is locked-in on your first attack of the turn "
      + "and stays set until you click 'End Turn' — even after a loot resolves. "
      + "This means you can attack again with the same symbol before ending your turn.",
      null
    ),
    /* 4  LOOT */
    new TutorialStepDef(
      "Loot a harvest deck",
      "With a card selected, tap one of the tilted card stacks in the center to loot it."
    ),
    /* 5  INFO_LOOT */
    new TutorialStepDef(
      "How Looting Works",
      "When you attack a harvest deck, your total attack value is compared to the hidden top card of that deck.\n\n"
      + "\u2022 If your value is HIGHER (or equal): you win and take cards from the deck.\n"
      + "\u2022 If your value is LOWER: the attack fails — your card(s) go to the discard pile and you gain nothing.\n\n"
      + "The top card is face-down and unknown — it can be anything from 2 up to a Joker (which beats everything).\n\n"
      + "Safe strategy: the higher your attack value, the better your odds. "
      + "Combine same-symbol cards to add their values together and aim as close to 15 as possible.",
      null
    ),
    /* 6  INFO_JOKER */
    new TutorialStepDef(
      "The Joker Card",
      "The Joker is a special wild card.\n\n"
      + "\u2022 Offense: its attack value is 999 — it beats ANY defense card or harvest deck top card,\n"
      + "  with ONE exception: a Joker sitting on top of a harvest deck is unbeatable (value 1000).\n"
      + "\u2022 Defense: placing it in a shield slot gives only 1 strength — very weak.\n"
      + "\u2022 Hero trade: drag a Joker from your hand onto the cemetery pile to sacrifice it.\n"
      + "  One card is drawn from the deck, and you gain the hero whose number matches that card.\n"
      + "  The drawn card goes to the cemetery — you keep the hero.\n\n"
      + "The Joker has no fixed symbol, so it fits any attack group.",
      null
    ),
    /* 7  DEFENSE */
    new TutorialStepDef(
      "Place a defense card",
      "Select a hand card, then tap an empty shield slot (dotted outlines below your king)."
    ),
    /* 8  ENDTURN */
    new TutorialStepDef(
      "Finish your turn",
      "You are done for this turn — tap the 'Finish turn' button."
    ),
    /* 9  WAITING */
    new TutorialStepDef(
      "Bot is playing...",
      "Wait for the bot to finish its turn — the tutorial continues automatically."
    ),
    /* 10 INFO_EXPOSE */
    new TutorialStepDef(
      "Expose a Defense Card",
      "Each turn, if you did NOT perform any attack, you must expose one of your "
      + "face-down defense cards before ending your turn.\n\n"
      + "Tap 'Finish turn' — the game will ask you to choose which defense slot to expose. "
      + "The card stays in the slot but becomes visible to all players.\n\n"
      + "If you DO attack (loot or player attack), no card needs to be exposed.",
      null
    ),
    /* 11 INFO_KING */
    new TutorialStepDef(
      "The King Card",
      "Your king is the single card placed below your defense slots. It is your most powerful card.\n\n"
      + "To attack with it, you must first have NO defense cards in your slots. "
      + "Then:\n"
      + "1. Discard all your defense cards (drag each to the cemetery pile).\n"
      + "2. Swap your king: tap your king to select it, then tap a hand card — that card becomes your new king, "
      + "and your old king moves to hand ready to attack.\n"
      + "3. Attack: select your old king from your hand, then tap an enemy defense slot.\n\n"
      + "Warning: if a king attack FAILS, the king becomes exposed — "
      + "and if an exposed king is successfully attacked, that player is eliminated!",
      null
    ),
    /* 12 DISCARD_DEF */
    new TutorialStepDef(
      "Discard all defense cards",
      "Select each defense slot, then drag it onto the cemetery pile to discard it. Repeat for all slots."
    ),
    /* 13 SWITCH_KING */
    new TutorialStepDef(
      "Switch your king",
      "Tap your king to select it, then tap a hand card to swap it as your new king."
    ),
    /* 14 KING_ATTACK */
    new TutorialStepDef(
      "Attack with your king!",
      "Select your old king from your hand, then tap an enemy defense slot to attack with it."
    ),
    /* 15 COMPLETE */
    new TutorialStepDef(
      "Tutorial Complete!",
      "Well done! You've completed the interactive tutorial.\n\n"
      + "You now know how to select and combine cards, loot decks, "
      + "place defenses, expose cards, use your king, "
      + "end your turn, and attack enemies.\n\n"
      + "Feel free to keep playing or return to the menu.",
      null
    ),
  };

  /** Called at action points to advance the tutorial if the player just completed the expected step. */
  private void tutorialAdvance(int fromStep) {
    if (!isTutorial || tutorialStep != fromStep) return;
    tutorialStep++;
    gameState.setUpdateState(true);
  }

  private void buildTutorialOverlay() {
    if (tutorialStep < 0 || tutorialStep >= TUTORIAL_TOTAL_STEPS) return;
    if (TUTORIAL_STEPS[tutorialStep].blocking) {
      buildBlockingTutorialOverlay();
    } else {
      buildGuidanceBanner();
    }
  }

  /** Full-screen blocking overlay for info/intro/complete steps. */
  private void buildBlockingTutorialOverlay() {
    TutorialStepDef step = TUTORIAL_STEPS[tutorialStep];

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0f, 0f, 0f, 0.88f);
    overlayStage.addActor(bg);

    Table outer = new Table();
    outer.setFillParent(true);
    outer.center().pad(20f);

    Label stepLabel = new Label("Step " + (tutorialStep + 1) + " / " + TUTORIAL_TOTAL_STEPS, MyGdxGame.skin);
    stepLabel.setColor(1f, 1f, 1f, 0.5f);
    outer.add(stepLabel).padBottom(6).row();

    Label titleLbl = new Label(step.title, MyGdxGame.skin);
    titleLbl.setColor(Color.GOLD);
    outer.add(titleLbl).padBottom(14).row();

    Label bodyLbl = new Label(step.body, MyGdxGame.skin);
    bodyLbl.setWrap(true);
    outer.add(bodyLbl).width(390f).padBottom(24).row();

    if (tutorialStep == TUTORIAL_STEP_COMPLETE) {
      TextButton exitBtn = new TextButton("Back to Menu", MyGdxGame.skin);
      exitBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          tutorialStep = -1;
          emitGiveUp();
        }
      });
      outer.add(exitBtn).width(exitBtn.getPrefWidth() + 20).height(exitBtn.getPrefHeight()).padBottom(10).row();

      TextButton keepBtn = new TextButton("Keep Playing", MyGdxGame.skin);
      keepBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          tutorialStep = -1;
          overlayStage.clear();
          addMenuButtonToOverlay();
          gameState.setUpdateState(true);
        }
      });
      outer.add(keepBtn).width(keepBtn.getPrefWidth() + 20).height(keepBtn.getPrefHeight()).row();
    } else {
      final int nextStep = tutorialStep + 1;
      String btnLabel = step.buttonLabel != null ? step.buttonLabel : "Got it!";
      TextButton gotItBtn = new TextButton(btnLabel, MyGdxGame.skin);
      gotItBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          tutorialStep = nextStep;
          overlayStage.clear();
          addMenuButtonToOverlay();
          buildTutorialOverlay();
        }
      });
      outer.add(gotItBtn).width(gotItBtn.getPrefWidth() + 20).height(gotItBtn.getPrefHeight()).row();

      TextButton skipBtn = new TextButton("Skip Tutorial", MyGdxGame.skin);
      skipBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          tutorialStep = -1;
          overlayStage.clear();
          addMenuButtonToOverlay();
          gameState.setUpdateState(true);
        }
      });
      outer.add(skipBtn).width(skipBtn.getPrefWidth() + 20).height(skipBtn.getPrefHeight()).padTop(8).row();
    }

    overlayStage.addActor(outer);
  }

  /** Non-blocking guidance banner at the top of the screen for interactive steps. */
  private void buildGuidanceBanner() {
    TutorialStepDef step = TUTORIAL_STEPS[tutorialStep];

    float bannerH = 88f;
    float bannerY = MyGdxGame.HEIGHT - bannerH - 2f;

    Image bannerBg = new Image(MyGdxGame.skin, "white");
    bannerBg.setSize(MyGdxGame.WIDTH, bannerH);
    bannerBg.setPosition(0, bannerY);
    bannerBg.setColor(0f, 0.05f, 0.2f, 0.92f);
    overlayStage.addActor(bannerBg);

    Table banner = new Table();
    banner.setSize(MyGdxGame.WIDTH, bannerH);
    banner.setPosition(0, bannerY);
    banner.top().left().padTop(6f).padLeft(10f).padRight(10f);

    String headerText = "Step " + (tutorialStep + 1) + "/" + TUTORIAL_TOTAL_STEPS + "  " + step.bannerTitle;
    Label headerLbl = new Label(headerText, MyGdxGame.skin);
    headerLbl.setColor(Color.GOLD);
    headerLbl.setWrap(true);
    banner.add(headerLbl).width(MyGdxGame.WIDTH - 95f).left().padBottom(4f).row();

    Label bodyLbl = new Label(step.bannerText, MyGdxGame.skin);
    bodyLbl.setWrap(true);
    banner.add(bodyLbl).width(MyGdxGame.WIDTH - 95f).left().row();

    overlayStage.addActor(banner);

    TextButton skipBtn = new TextButton("Skip", MyGdxGame.plainSkin);
    skipBtn.setSize(70f, 30f);
    skipBtn.setPosition(MyGdxGame.WIDTH - 75f, bannerY + bannerH - 34f);
    skipBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        tutorialStep = -1;
        overlayStage.clear();
        addMenuButtonToOverlay();
        gameState.setUpdateState(true);
      }
    });
    overlayStage.addActor(skipBtn);
  }

  // ── Hero tutorial overlay (Issue #171) ──────────────────────────────────────

  /** Advances the hero tutorial when the current step's hook matches. */
  private void tutorialAdvanceHook(String hook) {
    if (!isHeroTutorial || heroTutorialStep < 0 || hook == null) return;
    if (heroTutorialSteps == null || heroTutorialStep >= heroTutorialSteps.length) return;
    String stepHook = heroTutorialSteps[heroTutorialStep].hook;
    if (hook.equals(stepHook)) {
      heroTutorialStep++;
      gameState.setUpdateState(true);
    }
  }

  /** Builds the hero-tutorial overlay (blocking info or non-blocking banner). */
  private void buildHeroTutorialOverlay() {
    if (heroTutorialSteps == null || heroTutorialStep < 0
        || heroTutorialStep >= heroTutorialSteps.length) return;
    TutorialStepDef step = heroTutorialSteps[heroTutorialStep];
    if (step.blocking) {
      buildHeroBlockingOverlay(step);
    } else {
      buildHeroBanner(step);
    }
  }

  private void buildHeroBlockingOverlay(final TutorialStepDef step) {
    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0f, 0f, 0f, 0.88f);
    overlayStage.addActor(bg);

    Table outer = new Table();
    outer.setFillParent(true);
    outer.center().pad(20f);

    Label heroLbl = new Label(heroTutorialName + " Tutorial", MyGdxGame.skin);
    heroLbl.setColor(1f, 1f, 1f, 0.5f);
    outer.add(heroLbl).padBottom(2).row();

    int total = heroTutorialSteps.length;
    Label stepLabel = new Label("Step " + (heroTutorialStep + 1) + " / " + total, MyGdxGame.skin);
    stepLabel.setColor(1f, 1f, 1f, 0.5f);
    outer.add(stepLabel).padBottom(6).row();

    Label titleLbl = new Label(step.title, MyGdxGame.skin);
    titleLbl.setColor(Color.GOLD);
    outer.add(titleLbl).padBottom(14).row();

    Label bodyLbl = new Label(step.body, MyGdxGame.skin);
    bodyLbl.setWrap(true);
    outer.add(bodyLbl).width(390f).padBottom(24).row();

    if (step.terminal) {
      TextButton exitBtn = new TextButton("Back to Menu", MyGdxGame.skin);
      exitBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          heroTutorialStep = -1;
          // Tear down the tutorial session and navigate away so the bot stops.
          emitGiveUpAndLeave();
        }
      });
      outer.add(exitBtn).width(exitBtn.getPrefWidth() + 20).height(exitBtn.getPrefHeight()).padBottom(10).row();

      TextButton keepBtn = new TextButton("Keep Playing", MyGdxGame.skin);
      keepBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          heroTutorialStep = -1;
          overlayStage.clear();
          addMenuButtonToOverlay();
          gameState.setUpdateState(true);
        }
      });
      outer.add(keepBtn).width(keepBtn.getPrefWidth() + 20).height(keepBtn.getPrefHeight()).row();
    } else {
      String btnLabel = step.buttonLabel != null ? step.buttonLabel : "Got it!";
      TextButton gotItBtn = new TextButton(btnLabel, MyGdxGame.skin);
      gotItBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          heroTutorialStep++;
          overlayStage.clear();
          addMenuButtonToOverlay();
          buildHeroTutorialOverlay();
        }
      });
      outer.add(gotItBtn).width(gotItBtn.getPrefWidth() + 20).height(gotItBtn.getPrefHeight()).row();

      TextButton skipBtn = new TextButton("Skip Tutorial", MyGdxGame.skin);
      skipBtn.addListener(new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
          heroTutorialStep = -1;
          emitGiveUpAndLeave();
        }
      });
      outer.add(skipBtn).width(skipBtn.getPrefWidth() + 20).height(skipBtn.getPrefHeight()).padTop(8).row();
    }

    overlayStage.addActor(outer);
  }

  private void buildHeroBanner(TutorialStepDef step) {
    float bannerH = 88f;
    float bannerY = MyGdxGame.HEIGHT - bannerH - 2f;

    Image bannerBg = new Image(MyGdxGame.skin, "white");
    bannerBg.setSize(MyGdxGame.WIDTH, bannerH);
    bannerBg.setPosition(0, bannerY);
    bannerBg.setColor(0f, 0.05f, 0.2f, 0.92f);
    overlayStage.addActor(bannerBg);

    Table banner = new Table();
    banner.setSize(MyGdxGame.WIDTH, bannerH);
    banner.setPosition(0, bannerY);
    banner.top().left().padTop(6f).padLeft(10f).padRight(10f);

    int total = heroTutorialSteps.length;
    String headerText = "Step " + (heroTutorialStep + 1) + "/" + total + "  " + step.bannerTitle;
    Label headerLbl = new Label(headerText, MyGdxGame.skin);
    headerLbl.setColor(Color.GOLD);
    headerLbl.setWrap(true);
    banner.add(headerLbl).width(MyGdxGame.WIDTH - 190f).left().padBottom(4f).row();

    Label bodyLbl = new Label(step.bannerText, MyGdxGame.skin);
    bodyLbl.setWrap(true);
    banner.add(bodyLbl).width(MyGdxGame.WIDTH - 190f).left().row();

    overlayStage.addActor(banner);

    TextButton skipBtn = new TextButton("Skip", MyGdxGame.plainSkin);
    skipBtn.setSize(70f, 30f);
    skipBtn.setPosition(MyGdxGame.WIDTH - 75f, bannerY + bannerH - 34f);
    skipBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        heroTutorialStep = -1;
        emitGiveUpAndLeave();
      }
    });
    overlayStage.addActor(skipBtn);

    // Manual "Next" button so the player can advance past steps whose hook
    // they may not be able to satisfy in the current state.
    TextButton nextBtn = new TextButton("Next ►", MyGdxGame.plainSkin);
    nextBtn.setSize(90f, 30f);
    nextBtn.setPosition(MyGdxGame.WIDTH - 170f, bannerY + bannerH - 34f);
    nextBtn.addListener(new ClickListener() {
      @Override public void clicked(InputEvent event, float x, float y) {
        heroTutorialStep++;
        overlayStage.clear();
        addMenuButtonToOverlay();
        buildHeroTutorialOverlay();
      }
    });
    overlayStage.addActor(nextBtn);
  }

  private void emitGiveUp() {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIndex", playerIndex);
      socket.emit("giveUp", data);
    } catch (JSONException e) { e.printStackTrace(); }
    // Clear the saved session so a page-refresh does not auto-reconnect to the running game.
    MyGdxGame.playerStorage.clearSessionId();
  }

  private void emitGiveUpAndLeave() {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIndex", playerIndex);
      socket.emit("giveUpAndLeave", data);
    } catch (JSONException e) { e.printStackTrace(); }
    navigateToLobby();
  }

  private void navigateToLobby() {
    screenDisposed = true;
    MyGdxGame.playerStorage.clearSessionId();
    Gdx.app.postRunnable(new Runnable() {
      @Override
      public void run() {
        game.setScreen(new MenuScreen(game, socket));
      }
    });
  }

  private void emitPriestConvert(int targetPlayerIdx, int cardId) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", playerIndex);
      data.put("targetPlayerIdx", targetPlayerIdx);
      data.put("cardId", cardId);
      socket.emit("priestConvert", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitPriestAttemptFailed() {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", playerIndex);
      socket.emit("priestAttemptFailed", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitReservistsKingBoost(int count) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIdx", playerIndex);
      data.put("count", count);
      socket.emit("reservistsKingBoost", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  /**
   * Issue #167: destroy {@code count} of the given player's placed mercenaries
   * (state==1 -> state==2). Used when a boosted defense card disappears from a
   * player's def slots due to a successful enemy attack/loot, so the
   * mercenaries that fought on it don't remain stuck in the in-use state.
   */
  private void destroyMercenariesForPlayer(Player p, int count) {
    if (p == null || count <= 0) return;
    for (Hero h : p.getHeroes()) {
      if ("Mercenaries".equals(h.getHeroName())) {
        com.mygdx.game.heroes.Mercenaries merc = (com.mygdx.game.heroes.Mercenaries) h;
        for (int i = 0; i < count; i++) merc.destroy();
        break;
      }
    }
  }

  private void emitTakeDefCard(int positionId) {
    if (socket == null) return;
    try {
      // Issue #167: if the def cards on this slot carried mercenaries, reset
      // their boost on peer clients first so the boost label disappears once
      // the cards return to hand.
      Card df = currentPlayer.getDefCards().get(positionId);
      if (df != null && df.getBoosted() > 0) {
        JSONObject reset = new JSONObject();
        reset.put("playerIdx", playerIndex);
        reset.put("slot", positionId);
        reset.put("level", 0);
        reset.put("boosted", 0);
        socket.emit("mercDefBoost", reset);
      }
      Card tdf = currentPlayer.getTopDefCards().get(positionId);
      if (tdf != null && tdf.getBoosted() > 0) {
        JSONObject reset = new JSONObject();
        reset.put("playerIdx", playerIndex);
        reset.put("slot", positionId);
        reset.put("level", 1);
        reset.put("boosted", 0);
        socket.emit("mercDefBoost", reset);
      }
      JSONObject payload = new JSONObject();
      payload.put("playerIdx", playerIndex);
      payload.put("positionId", positionId);
      socket.emit("takeDefCard", payload);
      tutorialAdvance(TUTORIAL_STEP_TAKE_DEF_FIRST);
      tutorialAdvanceHook("TAKE_DEF");
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitPutDefCard(int positionId, int cardId) {
    if (socket == null) return;
    try {
      JSONObject payload = new JSONObject();
      payload.put("playerIdx", playerIndex);
      payload.put("positionId", positionId);
      payload.put("cardId", cardId);
      socket.emit("putDefCard", payload);
      tutorialAdvance(TUTORIAL_STEP_DEFENSE);
      tutorialAdvanceHook("PUT_DEF");
    } catch (JSONException e) { e.printStackTrace(); }
  }

  // Apply a server-authoritative stateUpdate to local game state.
  // Clears and refills card collections in-place (preserves deck/cemetery/pickingDeck listener objects).
  private void applyStateUpdate(JSONObject state) {
    try {
      // 1. Advance current player if changed; clear per-turn exposed state on transition
      int serverCurrentIdx = state.getInt("currentPlayerIndex");
      int prevCurrentIdx = gameState.getCurrentPlayerIndex();
      if (prevCurrentIdx != serverCurrentIdx) {
        Player prevPlayer = gameState.getPlayers().get(prevCurrentIdx);
        prevPlayer.getPlayerTurn().getBatteryDeniedAttackCardIds().clear();
        prevPlayer.getPlayerTurn().setBatteryDenied(false);
        pendingExposeCard = false;
        // Note: turn notification is fired in the socket listener callback (not here)
        // so it works even when the tab is hidden and the render loop is paused.
      }
      // Issue #171: track the previous player index here (not only in show()) so that
      // MY_TURN_START fires correctly even when multiple stateUpdates arrive in the same
      // render frame (e.g. the bot plays and finishes its turn synchronously, sending both
      // "bot's turn" and "player's turn again" before the next requestAnimationFrame tick).
      if (isHeroTutorial && heroTutorialStep >= 0) {
        heroTutorialPrevPlayerIdx = prevCurrentIdx;
      }
      gameState.setCurrentPlayer(serverCurrentIdx);

      // Broadcast own Reservists count on every stateUpdate so all clients always see the
      // correct indicator. Safe: reservistsKingBoost is only relayed to others, never back,
      // and the receiver only redraws locally — no emit chain.
      for (Hero h : currentPlayer.getHeroes()) {
        if ("Reservists".equals(h.getHeroName())) {
          emitReservistsKingBoost(((Reservists) h).countReady());
          break;
        }
      }

      // 2. Rebuild main deck
      JSONArray deckJson = state.getJSONArray("deck");
      gameState.getCardDeck().getCards().clear();
      for (int i = 0; i < deckJson.length(); i++) {
        gameState.getCardDeck().getCards().add(Card.fromCardId(deckJson.getInt(i)));
      }

      // 3. Rebuild cemetery
      JSONArray cemJson = state.getJSONArray("cemetery");
      gameState.getCemeteryDeck().getCards().clear();
      for (int i = 0; i < cemJson.length(); i++) {
        gameState.getCemeteryDeck().getCards().add(Card.fromCardId(cemJson.getInt(i)));
      }

      // 4. Rebuild each player's hand, defCards, topDefCards
      JSONArray playersJson = state.getJSONArray("players");
      for (int i = 0; i < playersJson.length(); i++) {
        JSONObject pj = playersJson.getJSONObject(i);
        Player p = gameState.getPlayers().get(pj.getInt("index"));

        // Save tradeable card ID before clearing (Merchant trade pending)
        int tradeableCardId = -1;
        for (Card tc : p.getHandCards()) {
          if (tc.isTradeable()) { tradeableCardId = tc.getCardId(); break; }
        }

        p.getHandCards().clear();
        JSONArray handJson = pj.getJSONArray("hand");
        for (int h = 0; h < handJson.length(); h++) {
          p.getHandCards().add(Card.fromCardId(handJson.getInt(h)));
        }

        // Restore tradeable flag after hand rebuild
        if (tradeableCardId != -1) {
          for (Card tc : p.getHandCards()) {
            if (tc.getCardId() == tradeableCardId) { tc.setTradable(true); break; }
          }
        }

        // Clear coupSwapPendingCardId if the card is no longer in hand (consumed or lost)
        if (p == currentPlayer) {
          int pendingId = currentPlayer.getPlayerTurn().getCoupSwapPendingCardId();
          if (pendingId != -1) {
            boolean found = false;
            for (Card hc : p.getHandCards()) {
              if (hc.getCardId() == pendingId) { found = true; break; }
            }
            if (!found) currentPlayer.getPlayerTurn().setCoupSwapPendingCardId(-1);
          }
        }

        // Save local def covered-state overrides (spy flips) before rebuilding
        // Key = slot, Value = card ID that was face-up (only restore if card ID unchanged)
        Map<Integer, Integer> savedDefCovered = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, Card> e : p.getDefCards().entrySet()) {
          if (!e.getValue().isCovered()) savedDefCovered.put(e.getKey(), e.getValue().getCardId());
        }
        // Save local boost state before rebuilding (not tracked by server)
        Map<Integer, int[]> savedDefBoosted = new HashMap<Integer, int[]>();
        for (Map.Entry<Integer, Card> e : p.getDefCards().entrySet()) {
          if (e.getValue().getBoosted() > 0)
            savedDefBoosted.put(e.getKey(), new int[]{e.getValue().getCardId(), e.getValue().getBoosted()});
        }
        p.getDefCards().clear();
        JSONObject defJson = pj.getJSONObject("defCards");
        JSONObject defCoveredJson = pj.optJSONObject("defCardsCovered");
        Iterator<String> defKeys = defJson.keys();
        while (defKeys.hasNext()) {
          String key = defKeys.next();
          Card dc = Card.fromCardId(defJson.getInt(key));
          boolean covered = defCoveredJson == null || defCoveredJson.optBoolean(key, true);
          dc.setCovered(covered);
          p.getDefCards().put(Integer.parseInt(key), dc);
        }
        for (Map.Entry<Integer, int[]> e : savedDefBoosted.entrySet()) {
          Card bc = p.getDefCards().get(e.getKey());
          if (bc != null && bc.getCardId() == e.getValue()[0]) {
            bc.addBoosted(e.getValue()[1]);
          } else {
            // Issue #167: the previously boosted card is no longer in this
            // slot (looted or attacked away). Destroy the owner's
            // mercenaries that were placed on it so they don't stay stuck
            // in the in-use state forever.
            destroyMercenariesForPlayer(p, e.getValue()[1]);
          }
        }
        // Restore spy-flipped face-up state — only if the card at that slot is the same card
        for (Map.Entry<Integer, Integer> e : savedDefCovered.entrySet()) {
          Card dc = p.getDefCards().get(e.getKey());
          if (dc != null && dc.getCardId() == e.getValue()) dc.setCovered(false);
        }

        Map<Integer, int[]> savedTopBoosted = new HashMap<Integer, int[]>();
        for (Map.Entry<Integer, Card> e : p.getTopDefCards().entrySet()) {
          if (e.getValue().getBoosted() > 0)
            savedTopBoosted.put(e.getKey(), new int[]{e.getValue().getCardId(), e.getValue().getBoosted()});
        }
        p.getTopDefCards().clear();
        JSONObject topDefJson = pj.getJSONObject("topDefCards");
        JSONObject topDefCoveredJson = pj.optJSONObject("topDefCardsCovered");
        Iterator<String> topKeys = topDefJson.keys();
        while (topKeys.hasNext()) {
          String key = topKeys.next();
          Card tdc = Card.fromCardId(topDefJson.getInt(key));
          boolean topCovered = topDefCoveredJson == null || topDefCoveredJson.optBoolean(key, true);
          tdc.setCovered(topCovered);
          p.getTopDefCards().put(Integer.parseInt(key), tdc);
        }
        for (Map.Entry<Integer, int[]> e : savedTopBoosted.entrySet()) {
          Card bc = p.getTopDefCards().get(e.getKey());
          if (bc != null && bc.getCardId() == e.getValue()[0]) {
            bc.addBoosted(e.getValue()[1]);
          } else {
            // Issue #167: see comment above.
            destroyMercenariesForPlayer(p, e.getValue()[1]);
          }
        }

        // Sync king card from server (may transition null→card after setup phase)
        int serverKingId = pj.optInt("kingCard", 0);
        if (serverKingId > 0) {
          if (p.getKingCard() == null || p.getKingCard().getCardId() != serverKingId) {
            p.setKingCard(Card.fromCardId(serverKingId));
          }
          p.getKingCard().setCovered(pj.optBoolean("kingCovered", true));
        } else {
          p.setKingCard(null);
        }

        // Apply out flag; if this client's own player just became eliminated, switch to spectator mode
        // so game-action input is no longer processed.
        boolean wasOut = p.isOut();
        p.setOut(pj.optBoolean("isOut", false));
        if (!wasOut && p.isOut() && p == currentPlayer && !isSpectator) {
          isSpectator = true;
        }

        // Sync avatar icon
        p.setIcon(pj.optString("icon", ""));

        // Sync slot sabotage state from server-authoritative state
        for (int sl = 1; sl <= 3; sl++) p.clearSlotSabotaged(sl);
        JSONObject sabotagedJson = pj.optJSONObject("sabotaged");
        if (sabotagedJson != null) {
          Iterator<String> sabKeys = sabotagedJson.keys();
          while (sabKeys.hasNext()) {
            String key = sabKeys.next();
            p.setSlotSabotaged(Integer.parseInt(key), sabotagedJson.getInt(key));
          }
        }

        // Sync prey cards (captured this turn, locked until turn ends).
        // RACE-SAFE: a stale stateUpdate from setAttackPreview can arrive AFTER the client has
        // optimistically added the captured card IDs in the attack overlay handler. Taking the
        // UNION of server and local prey IDs (only while it is still this player's turn)
        // prevents the stale update from clobbering the locally-known captures. After the turn
        // ends (currentPlayerIndex moves on), the server's value is authoritative.
        ArrayList<Integer> newPreyIds = new ArrayList<Integer>();
        JSONArray preyJson = pj.optJSONArray("preyCards");
        if (preyJson != null) {
          for (int pr = 0; pr < preyJson.length(); pr++) newPreyIds.add(preyJson.getInt(pr));
        }
        boolean isThisPlayersTurn = (pj.getInt("index") == serverCurrentIdx);
        if (isThisPlayersTurn) {
          for (Integer localId : p.getPlayerTurn().getPreyCardIds()) {
            if (!newPreyIds.contains(localId)) newPreyIds.add(localId);
          }
        }
        p.getPlayerTurn().setPreyCardIds(newPreyIds);

        // Sync attack counter. RACE-SAFE: same reasoning as preyCardIds — a stale stateUpdate
        // from setAttackPreview can arrive after the client locally incremented the counter in
        // the attack overlay handler. While it is still this player's turn, take MAX(server, local).
        // After the turn ends, server is authoritative (server resets attackCount=0 on finishTurn,
        // so the next stateUpdate naturally syncs the value down).
        int serverAttackCount = pj.optInt("attackCount", 0);
        if (isThisPlayersTurn) {
          int localAttackCount = p.getPlayerTurn().getAttackCounter();
          p.getPlayerTurn().setAttackCounter(Math.max(serverAttackCount, localAttackCount));
        } else {
          p.getPlayerTurn().setAttackCounter(serverAttackCount);
        }

        // Restore per-turn client counters from server-authoritative state so they survive page refresh
        if (p == currentPlayer) {
          p.getPlayerTurn().setPickingDeckAttacks(pj.optInt("pickingDeckAttacks", 1));
          p.getPlayerTurn().setAttackingSymbolDirect(
              pj.optString("attackingSymbol", "none"),
              pj.optString("attackingSymbol2", "none"));
        }
      }

      // Sync heroes from server-authoritative state so missed relay events do not desync views.
      gameState.rebuildHeroesFromState(playersJson);

      // Sync local Saboteurs hero active count: count how many slots across all players are
      // owned by the local player (playerIndex) to keep the hero state consistent with server.
      int activeSaboCount = 0;
      for (Player gp : gameState.getPlayers()) {
        for (int sl = 1; sl <= 3; sl++) {
          if (gp.getSlotSaboteurOwnerIdx(sl) == playerIndex) activeSaboCount++;
        }
      }
      for (Hero h : currentPlayer.getHeroes()) {
        if ("Saboteurs".equals(h.getHeroName())) {
          ((Saboteurs) h).syncFromActiveCount(activeSaboCount);
          break;
        }
      }

      // 5. Rebuild picking decks in-place (keep PickingDeck objects to preserve listeners)
      // Sync attack preview for the defender/watcher overlay
      JSONObject serverPendingAtk = state.optJSONObject("pendingAttack");
      if (serverPendingAtk != null && serverPendingAtk.optInt("attackerIdx", -1) != playerIndex) {
        pendingAttackBroadcast = serverPendingAtk;
      } else {
        pendingAttackBroadcast = null;
      }

      // Sync loot preview — restore overlay for attacker on reconnect, watcher overlay for others
      JSONObject serverPendingLoot = state.optJSONObject("pendingLoot");
      if (serverPendingLoot != null
          && serverPendingLoot.optInt("attackerIdx", -1) == playerIndex
          && !currentPlayer.getPlayerTurn().isLootPending()) {
        // Restore the loot confirmation overlay so it reappears after a page refresh
        PlayerTurn rpt = currentPlayer.getPlayerTurn();
        rpt.setLootPending(true);
        rpt.setPendingPickingDeckIndex(serverPendingLoot.optInt("deckIndex", 0));
        rpt.setLootSuccess(serverPendingLoot.optBoolean("success", false));
        rpt.setKingUsed(serverPendingLoot.optBoolean("kingUsed", false));
        rpt.setPendingLootAttackSum(serverPendingLoot.optInt("attackSum", 0));
        rpt.setPendingLootDefStrength(serverPendingLoot.optInt("defStrength", 0));
        ArrayList<Card> rptAtkCards = new ArrayList<Card>();
        JSONArray rptAtkIds = serverPendingLoot.optJSONArray("attackCardIds");
        if (rptAtkIds != null) {
          for (int rai = 0; rai < rptAtkIds.length(); rai++) rptAtkCards.add(Card.fromCardId(rptAtkIds.getInt(rai)));
        }
        rpt.setPendingAttackCards(rptAtkCards);
        ArrayList<Card> rptOwnDefCards = new ArrayList<Card>();
        JSONArray rptOwnDefIds = serverPendingLoot.optJSONArray("ownDefCardIds");
        if (rptOwnDefIds != null) {
          for (int rai = 0; rai < rptOwnDefIds.length(); rai++) rptOwnDefCards.add(Card.fromCardId(rptOwnDefIds.getInt(rai)));
        }
        rpt.setPendingAttackOwnDefCards(rptOwnDefCards);
        pendingLootBroadcast = null;
      } else if (serverPendingLoot != null && serverPendingLoot.optInt("attackerIdx", -1) != playerIndex) {
        pendingLootBroadcast = serverPendingLoot;
      } else {
        pendingLootBroadcast = null;
      }

      // Sync pending hero selection after king defeat (only relevant to the attacker)
      JSONObject serverPendingHeroSel = state.optJSONObject("pendingHeroSelection");
      if (serverPendingHeroSel != null && serverPendingHeroSel.optInt("attackerIdx", -1) == playerIndex) {
        JSONArray optionsJson = serverPendingHeroSel.optJSONArray("options");
        if (optionsJson != null && optionsJson.length() > 0) {
          java.util.ArrayList<String> opts = new java.util.ArrayList<String>();
          for (int oi = 0; oi < optionsJson.length(); oi++) opts.add(optionsJson.getString(oi));
          pendingKingDefeatHeroOptions = opts;
        } else {
          pendingKingDefeatHeroOptions = null;
        }
      } else {
        pendingKingDefeatHeroOptions = null;
      }

      // Sync hero auction state from server
      JSONObject newAuction = state.optJSONObject("pendingHeroAuction");
      if (newAuction == null) {
        auctionBidHandCardIds.clear();
        auctionBidDefCardIds.clear();
        auctionSellHeroName = null;
      }
      pendingHeroAuction = newAuction;

      // 5. Rebuild picking decks in-place (keep PickingDeck objects to preserve listeners)
      JSONArray pickJson = state.getJSONArray("pickingDecks");
      for (int i = 0; i < Math.min(pickJson.length(), gameState.getPickingDecks().size()); i++) {
        JSONArray pdJson = pickJson.getJSONArray(i);
        gameState.getPickingDecks().get(i).getCards().clear();
        for (int j = 0; j < pdJson.length(); j++) {
          JSONObject co = pdJson.getJSONObject(j);
          Card c = Card.fromCardId(co.getInt("id"));
          c.setCovered(co.getBoolean("covered"));
          gameState.getPickingDecks().get(i).addCard(c);
        }
      }

      // 6. Winner index
      gameState.setWinnerIndex(state.optInt("winnerIndex", -1));

      // 6b. Setup phase flag (manual setup)
      boolean prevSetupPhase = gameState.isSetupPhase();
      boolean newSetupPhase = state.optBoolean("setupPhase", false);
      gameState.setSetupPhase(newSetupPhase);
      // When setup phase just ended, attach picking deck listeners (deferred from deserialization)
      if (prevSetupPhase && !newSetupPhase && gameState.getPickingDecks().size() >= 2) {
        PickingDeckListener pdl0 = new PickingDeckListener(gameState, gameState.getPickingDecks().get(0), gameState.getPickingDecks().get(1), 0);
        gameState.getPickingDecks().get(0).addListener(pdl0);
        PickingDeckListener pdl1 = new PickingDeckListener(gameState, gameState.getPickingDecks().get(1), gameState.getPickingDecks().get(0), 1);
        gameState.getPickingDecks().get(1).addListener(pdl1);
      }

      // 6c. Setup submitted map (which players have confirmed their manual setup)
      JSONObject ssJson = state.optJSONObject("setupSubmitted");
      if (ssJson != null) {
        Map<Integer, Boolean> ssMap = new HashMap<Integer, Boolean>();
        Iterator<String> ssKeys = ssJson.keys();
        while (ssKeys.hasNext()) {
          String k = ssKeys.next();
          ssMap.put(Integer.parseInt(k), ssJson.optBoolean(k, false));
        }
        gameState.setSetupSubmittedMap(ssMap);
        // Re-derive local setupSubmitted from server state (handles page refresh mid-setup)
        if (!setupSubmitted && Boolean.TRUE.equals(ssMap.get(playerIndex))) {
          setupSubmitted = true;
        }
      }

      // Sync round number
      int serverRound = state.optInt("roundNumber", 0);
      if (serverRound > 0) gameState.setRoundNumber(serverRound);

      // 7. Activity log
      JSONArray logJson = state.optJSONArray("log");
      if (logJson != null) activityLog = logJson;

      // 8. Merchant 2nd-try reveal
      JSONObject merchantRevealJson = state.optJSONObject("merchantReveal");
      if (merchantRevealJson != null) {
        merchantRevealCardId = merchantRevealJson.optInt("cardId", -1);
        merchantRevealPlayerIdx = merchantRevealJson.optInt("playerIdx", -1);
      } else {
        merchantRevealCardId = -1;
        merchantRevealPlayerIdx = -1;
      }

    } catch (JSONException e) { e.printStackTrace(); }
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClearColor(0, 0, 0, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    // Compute letterboxed region: scale 450x800 to fit the screen, preserving aspect ratio.
    float scale = Math.min(
        (float) Gdx.graphics.getWidth()  / MyGdxGame.WIDTH,
        (float) Gdx.graphics.getHeight() / MyGdxGame.HEIGHT);
    int gamePixelW = Math.round(MyGdxGame.WIDTH  * scale);
    int gamePixelH = Math.round(MyGdxGame.HEIGHT * scale);
    int offsetX    = (Gdx.graphics.getWidth()  - gamePixelW) / 2;
    int offsetY    = (Gdx.graphics.getHeight() - gamePixelH) / 2;
    // Upper portion is the square play area (WIDTH x WIDTH logical)
    int upperH     = Math.round(MyGdxGame.WIDTH * scale);
    // Lower portion is the hand area (WIDTH x (HEIGHT-WIDTH) logical)
    int lowerH     = gamePixelH - upperH;

    // Overlay stage always handles the menu button (and full menu when open).
    // Game/hand stages are added only when it is this client's active turn.
    if (menuOpen) {
      Gdx.input.setInputProcessor(overlayStage);
    } else if (!isSpectator) {
      // Active turn OR waiting: include game+hand stages so hero info overlays work when not your turn.
      // Game-action listeners (EnemyDefCardListener etc.) require selected cards/heroes which are
      // never set during a non-active turn, so enabling these stages is safe.
      Gdx.input.setInputProcessor(menuAndGameMulti);
    } else {
      // Spectator: overlay/menu input (includes chat, history, lens)
      Gdx.input.setInputProcessor(menuAndGameMulti);
    }

    // check if gameState has changed
    if (gameState.getUpdateState()) {
      gameState.setUpdateState(false);
      show();
    }

    // Safety net: while the local player has submitted manual setup but the game hasn't started yet,
    // emit a requestStateSync every 4 seconds so the screen self-recovers if the final
    // stateUpdate (setupPhase=false) was missed (e.g. tab was backgrounded on a mobile device).
    if (gameState.isSetupPhase() && setupSubmitted) {
      setupWaitTimer += delta;
      if (setupWaitTimer >= 4f) {
        setupWaitTimer = 0f;
        socket.emit("requestStateSync", new JSONObject());
      }
    } else {
      setupWaitTimer = 0f;
    }

    // Gameplay heartbeat resync: if no stateUpdate has arrived for 30 seconds during an active
    // game, request a full state sync from the server. This self-heals clients that silently
    // get out of sync (e.g. a missed update due to a brief network hiccup or a tab that was
    // briefly backgrounded during a game-state transition).
    if (!gameState.isSetupPhase() && !isTutorial) {
      syncHeartbeatTimer += delta;
      if (syncHeartbeatTimer >= 30f) {
        syncHeartbeatTimer = 0f;
        socket.emit("requestStateSync", new JSONObject());
      }
    } else {
      syncHeartbeatTimer = 0f;
    }

    // Tutorial step SELECT auto-advance: card selection is visual-only and doesn't trigger show(),
    // so we poll it every frame here instead.
    if (isTutorial && tutorialStep == TUTORIAL_STEP_SELECT) {
      for (Card c : currentPlayer.getHandCards()) {
        if (c.isSelected()) {
          tutorialStep = TUTORIAL_STEP_INFO_SYMBOLS;
          gameState.setUpdateState(true);
          break;
        }
      }
    }
    // Tutorial step DEFENSE auto-advance: defense card placement may go through OwnPlaceholderListener
    // which emits putDefCard directly without calling emitPutDefCard(). Poll here as fallback.
    // Only advance when a NEW card is placed — not just because pre-existing cards are present.
    if (isTutorial && tutorialStep == TUTORIAL_STEP_DEFENSE) {
      int defTotal = currentPlayer.getDefCards().size() + currentPlayer.getTopDefCards().size();
      if (tutorialDefenseBaseline < 0) {
        // Record the card count at the start of the DEFENSE step so we can detect additions.
        tutorialDefenseBaseline = defTotal;
      } else if (defTotal > tutorialDefenseBaseline) {
        tutorialDefenseBaseline = -1;
        tutorialStep = TUTORIAL_STEP_ENDTURN;
        gameState.setUpdateState(true);
      }
    } else if (tutorialStep != TUTORIAL_STEP_DEFENSE) {
      // Reset baseline whenever we leave the DEFENSE step.
      tutorialDefenseBaseline = -1;
    }
    // Tutorial step DISCARD_DEF: advance once all defense slots are empty (discarded to cemetery).
    if (isTutorial && tutorialStep == TUTORIAL_STEP_DISCARD_DEF) {
      if (currentPlayer.getDefCards().isEmpty() && currentPlayer.getTopDefCards().isEmpty()) {
        tutorialAdvance(TUTORIAL_STEP_DISCARD_DEF);
      }
    }
    // Tutorial step SWITCH_KING: advance once the coup-swap has been performed.
    // coupSwapPendingCardId is set to the old king's card ID when the swap happens.
    if (isTutorial && tutorialStep == TUTORIAL_STEP_SWITCH_KING) {
      if (currentPlayer.getPlayerTurn().getCoupSwapPendingCardId() != -1) {
        tutorialAdvance(TUTORIAL_STEP_SWITCH_KING);
      }
    }

    // Battery Tower bot notification timer — auto-dismiss after it expires
    if (batteryBotNotificationTimer > 0f) {
      batteryBotNotificationTimer -= delta;
      if (batteryBotNotificationTimer <= 0f) {
        batteryBotNotification = null;
        batteryBotNotificationTimer = 0f;
        gameState.setUpdateState(true);
      }
    }

    // Highlight hand area when own defense card is selected or being dragged
    boolean anyOwnDefSelected = isDraggingDefCard;
    if (!anyOwnDefSelected && !gameState.isSetupPhase()) {
      for (Card c : currentPlayer.getDefCards().values()) {
        if (c.isSelected()) { anyOwnDefSelected = true; break; }
      }
    }
    if (!anyOwnDefSelected && !gameState.isSetupPhase()) {
      for (Card c : currentPlayer.getTopDefCards().values()) {
        if (c.isSelected()) { anyOwnDefSelected = true; break; }
      }
    }
    // Show a semi-transparent green overlay when placing a defence card.
    handHighlight.setColor(0.3f, 0.8f, 0.3f, anyOwnDefSelected ? 0.45f : 0f);

    // Per-frame: golden highlight king card when king attack is available; white when selected (green applied by draw()).
    if (!isSpectator && !gameState.isSetupPhase() && gameState.getCurrentPlayer() == currentPlayer) {
      PlayerTurn ptKing = currentPlayer.getPlayerTurn();
      Card kingCard = currentPlayer.getKingCard();
      if (kingCard != null) {
        boolean canKingAtk = currentPlayer.getDefCards().isEmpty()
            && currentPlayer.getTopDefCards().isEmpty()
            && !ptKing.isKingUsedThisTurn()
            && !ptKing.isAttackPending();
        if (canKingAtk && !kingCard.isSelected()) {
          kingCard.setDefColor(new Color(1f, 0.85f, 0.1f, 1f));
        } else {
          kingCard.setDefColor(Color.WHITE);
        }
      }
    }

    // Per-frame: highlight looting decks golden when plundering is available
    // AND the player has at least one selected hand card with a matching attack symbol.
    if (!isSpectator && !gameState.isSetupPhase() && gameState.getCurrentPlayer() == currentPlayer) {
      PlayerTurn ptDeck = currentPlayer.getPlayerTurn();
      boolean shouldHighlight = false;
      if (ptDeck.getPickingDeckAttacks() > 0 && !ptDeck.isLootPending()) {
        String atkSym    = ptDeck.getAttackingSymbol()[0];
        String atkSymExt = ptDeck.getAttackingSymbol()[1];
        for (Card hc : currentPlayer.getHandCards()) {
          if (hc.isSelected()) {
            String sym = hc.getSymbol();
            if ("joker".equals(sym) || "none".equals(atkSym)
                || sym.equals(atkSym) || sym.equals(atkSymExt)) {
              shouldHighlight = true;
              break;
            }
          }
        }
      }
      for (PickingDeck hlDeck : gameState.getPickingDecks()) {
        for (Card c : hlDeck.getCards()) {
          c.setDefColor(shouldHighlight ? new Color(1f, 0.85f, 0.1f, 1f) : Color.WHITE);
        }
      }
    }

    /* Upper division (square play area) */
    Gdx.gl.glViewport(offsetX, offsetY + lowerH, gamePixelW, upperH);
    gameStage.getViewport().update(gamePixelW, upperH, true);
    gameStage.getViewport().setScreenBounds(offsetX, offsetY + lowerH, gamePixelW, upperH);
    gameStage.getViewport().apply();
    gameStage.act(delta);
    gameStage.draw();

    /* Lower division (hand area) */
    Gdx.gl.glViewport(offsetX, offsetY, gamePixelW, lowerH);
    handStage.getViewport().update(gamePixelW, lowerH, true);
    handStage.getViewport().setScreenBounds(offsetX, offsetY, gamePixelW, lowerH);
    handStage.getViewport().apply();
    handStage.act(delta);
    handStage.draw();

    /* Overlay (drag layer - always on top) */
    Gdx.gl.glViewport(offsetX, offsetY, gamePixelW, gamePixelH);
    overlayStage.getViewport().update(gamePixelW, gamePixelH, true);
    overlayStage.getViewport().setScreenBounds(offsetX, offsetY, gamePixelW, gamePixelH);
    overlayStage.getViewport().apply();
    overlayStage.act(delta);
    overlayStage.draw();
  }

  // ---- Joker sacrifice / hero acquisition ----

  /**
   * Sacrifice a joker card to draw a card that determines which hero the player receives.
   * Card index 2-13 → direct hero; Ace → choose by colour; Joker → free choice.
   */
  private void performJokerSacrifice(Card jokerCard) {
    // Remove the joker from hand and send it to the cemetery.
    currentPlayer.getHandCards().remove(jokerCard);
    gameState.getCemeteryDeck().addCard(jokerCard);
    // Store joker ID so completeHeroAcquisition can include it in the emit.
    currentPlayer.getPlayerTurn().setPendingJokerCardId(jokerCard.getCardId());

    // Draw a card from the deck to determine the hero.
    Card drawnCard = gameState.getCardDeck().getCard(gameState.getCemeteryDeck());
    // Guard: if deck was empty we get a placeholder — bail out gracefully.
    if (drawnCard == null || drawnCard.isPlaceholder()) {
      gameState.setUpdateState(true);
      return;
    }
    int drawnIndex   = drawnCard.getIndex();
    String drawnSym  = drawnCard.getSymbol();
    // Drawn card goes to the cemetery after its purpose is served.
    gameState.getCemeteryDeck().addCard(drawnCard);

    // Notify the server so all clients’ decks stay in sync.
    try {
      JSONObject emitData = new JSONObject();
      emitData.put("playerIdx",   playerIndex);
      emitData.put("jokerCardId", jokerCard.getCardId());
      emitData.put("drawnCardId", drawnCard.getCardId());
      socket.emit("jokerSacrifice", emitData);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    HeroesSquare hs = gameState.getHeroesSquare();

    if ("joker".equals(drawnSym)) {
      // Another joker drawn — free choice from ALL heroes (pool + already owned).
      triggerHeroChoice(buildHeroChoices(false, false));
    } else if (drawnIndex == 1) {
      // Ace: red suits (hearts/diamonds) → white heroes; black → black heroes.
      boolean isRed = "hearts".equals(drawnSym) || "diamonds".equals(drawnSym);
      java.util.ArrayList<Hero> choices = isRed
          ? buildHeroChoices(true, false)
          : buildHeroChoices(false, true);
      if (choices.isEmpty()) choices = buildHeroChoices(false, false); // fallback
      triggerHeroChoice(choices);
    } else {
      // Direct hero assignment by card index (2-13).
      Hero hero = hs.getHeroByCardIndex(drawnIndex);
      if (hero == null) {
        // That hero is already owned by a player — strip it from the owner.
        // Per issue #25 the drawing player receives NOTHING.
        String takenName = HeroesSquare.heroNameByCardIndex(drawnIndex);
        if (takenName != null) {
          int ownerIdx = gameState.findHeroOwnerIndex(takenName);
          if (ownerIdx >= 0) {
            players.get(ownerIdx).removeHeroByName(takenName);
            try {
              JSONObject emitData = new JSONObject();
              emitData.put("playerIndex", ownerIdx);
              emitData.put("heroName", takenName);
              socket.emit("heroLost", emitData);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        }
        gameState.setUpdateState(true);
      } else {
        completeHeroAcquisition(hero);
      }
    }
  }

  /** Set heroSelectionPending so the selection overlay appears on next render. */
  private void triggerHeroChoice(java.util.ArrayList<Hero> choices) {
    if (choices.isEmpty()) { gameState.setUpdateState(true); return; }
    currentPlayer.getPlayerTurn().setHeroChoices(choices);
    currentPlayer.getPlayerTurn().setHeroSelectionPending(true);
    gameState.setUpdateState(true);
  }

  /** Finalise hero acquisition: add hero to player, emit to server, trigger redraw. */
  private void completeHeroAcquisition(Hero hero) {
    currentPlayer.addHero(hero);
    // If the acquired hero is Reservists, immediately broadcast the count to all other clients.
    if ("Reservists".equals(hero.getHeroName())) {
      emitReservistsKingBoost(((Reservists) hero).countReady());
    }
    currentPlayer.getPlayerTurn().setHeroSelectionPending(false);
    currentPlayer.getPlayerTurn().getHeroChoices().clear();
    try {
      JSONObject emitData = new JSONObject();
      emitData.put("playerIndex", playerIndex);
      emitData.put("heroName",    hero.getHeroName());
      emitData.put("jokerCardId", currentPlayer.getPlayerTurn().getPendingJokerCardId());
      socket.emit("heroAcquired", emitData);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    gameState.setUpdateState(true);
  }

  // ---- end hero acquisition ----

  /**
   * Build the list of hero choices for an Ace or Joker draw.
   * Includes both heroes still available in the pool and heroes already owned by players,
   * so that the drawing player can choose to steal a hero from its current owner.
   * @param whiteOnly include only white heroes (for red Ace)
   * @param blackOnly include only black heroes (for black Ace)
   */
  private java.util.ArrayList<Hero> buildHeroChoices(boolean whiteOnly, boolean blackOnly) {
    HeroesSquare hs = gameState.getHeroesSquare();
    // Start from the still-available pool heroes (filtered if needed).
    java.util.ArrayList<Hero> result;
    if (whiteOnly) {
      result = new java.util.ArrayList<Hero>(hs.getAvailableWhiteHeroes());
    } else if (blackOnly) {
      result = new java.util.ArrayList<Hero>(hs.getAvailableBlackHeroes());
    } else {
      result = new java.util.ArrayList<Hero>(hs.getAvailableAllHeroes());
    }
    // Also include heroes already owned by any player.
    for (int i = 0; i < players.size(); i++) {
      java.util.ArrayList<Hero> owned = players.get(i).getHeroes();
      for (int j = 0; j < owned.size(); j++) {
        Hero h = owned.get(j);
        if (!whiteOnly && !blackOnly) {
          result.add(h);
        } else if (whiteOnly && isWhiteHero(h.getHeroName())) {
          result.add(h);
        } else if (blackOnly && !isWhiteHero(h.getHeroName())) {
          result.add(h);
        }
      }
    }
    return result;
  }

  private static boolean isWhiteHero(String name) {
    return "Mercenaries".equals(name) || "Marshal".equals(name) || "Spy".equals(name)
        || "Battery Tower".equals(name) || "Merchant".equals(name) || "Priest".equals(name);
  }

  public void removeAllListeners(Actor actor) {
    // See Card.removeAllListeners — the for-each + removeListener pattern is buggy
    // (skips elements as the Array shrinks). Use clearListeners() instead.
    actor.clearListeners();
  }

  /** Returns true if the given player has the Mercenaries hero and it is currently selected. */
  private boolean isMercenariesSelectedBy(Player player) {
    if (player == null) return false;
    for (Hero h : player.getHeroes()) {
      if ("Mercenaries".equals(h.getHeroName()) && h.isSelected()) return true;
    }
    return false;
  }

  /**
   * Issue #167: adds two translucent half-overlays to gameStage on top of the
   * given own defense card — green on the top half ("add mercenary") and red
   * on the bottom half ("remove mercenary"). Overlays are non-touchable so the
   * underlying def card listener still receives the click.
   */
  private void addMercenarySelectionHighlight(Card defCard) {
    float halfH = defCard.getHeight() / 2f;
    Image bottomHalf = new Image(MyGdxGame.skin.newDrawable("white", new Color(1f, 0f, 0f, 0.28f)));
    bottomHalf.setBounds(defCard.getX(), defCard.getY(), defCard.getWidth(), halfH);
    bottomHalf.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    Image topHalf = new Image(MyGdxGame.skin.newDrawable("white", new Color(0f, 1f, 0f, 0.28f)));
    topHalf.setBounds(defCard.getX(), defCard.getY() + halfH, defCard.getWidth(), halfH);
    topHalf.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    gameStage.addActor(bottomHalf);
    gameStage.addActor(topHalf);
  }

  /**
   * Issues #54, #175, #176, #178, #179, #180: generic translucent highlight overlay
   * placed on top of any actor (card, hero icon, etc.) to indicate it is targetable
   * for the currently selected hero's action. Overlay is non-touchable so the
   * underlying actor still receives the click.
   */
  private void addCardActionHighlight(Actor actor, Color color, com.badlogic.gdx.scenes.scene2d.Stage stage) {
    Image hi = new Image(MyGdxGame.skin.newDrawable("white", color));
    // For rotated cards we want an axis-aligned overlay matching the visual bounds.
    // Hand cards rotate via Actor.setRotation(); board cards (left/right/top players)
    // rotate via Card's internal `rotate` field used inside Card.draw — Actor.getRotation()
    // returns 0 in that case so we must inspect Card.getRotate() too (issue: highlight
    // overlay was vertical on horizontally-displayed enemy cards for left/right players).
    float w = actor.getWidth();
    float h = actor.getHeight();
    float rot = actor.getRotation();
    if (actor instanceof Card) {
      float cardRot = ((Card) actor).getRotate();
      if (Math.abs(cardRot) > 0.5f) rot = cardRot;
    }
    // Normalise to [-180,180]
    while (rot > 180f) rot -= 360f;
    while (rot < -180f) rot += 360f;
    boolean sideways = Math.abs(Math.abs(rot) - 90f) < 1f;
    if (sideways) {
      // Visual bounds for a card rotated 90/-90 around its centre: still centred on
      // (x + w/2, y + h/2) but visual size is (h, w).
      float cx = actor.getX() + w / 2f;
      float cy = actor.getY() + h / 2f;
      hi.setBounds(cx - h / 2f, cy - w / 2f, h, w);
    } else {
      hi.setBounds(actor.getX(), actor.getY(), w, h);
    }
    hi.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    stage.addActor(hi);
  }

  /** Returns the currently selected hero of the given player (if any), else null. */
  private Hero selectedHero(Player player) {
    if (player == null) return null;
    for (Hero h : player.getHeroes()) {
      if (h.isSelected()) return h;
    }
    return null;
  }

  /**
   * Issues #54, #178, #179, #180: when an attacker hero is selected (Spy/Saboteurs/Magician/Warlord),
   * tint each enemy defense card to indicate it is targetable by the current action.
   * Also handles empty enemy slots (Saboteurs).
   */
  private void applyEnemyDefCardHighlight(Card defCard, Player owner, Player current, int slot) {
    if (owner == current) return;
    Hero sel = selectedHero(current);
    if (sel == null) return;
    String name = sel.getHeroName();
    if ("Spy".equals(name)) {
      com.mygdx.game.heroes.Spy spy = (com.mygdx.game.heroes.Spy) sel;
      // Highlight only face-down enemy def cards while the spy still has flip actions.
      if (spy.getSpyAttacks() > 0 && !defCard.isPlaceholder() && defCard.isCovered()) {
        addCardActionHighlight(defCard, new Color(1f, 1f, 0f, 0.28f), gameStage);
      }
    } else if ("Saboteurs".equals(name)) {
      com.mygdx.game.heroes.Saboteurs sab = (com.mygdx.game.heroes.Saboteurs) sel;
      if (sab.isAvailable() && !owner.isSlotSabotaged(slot)) {
        addCardActionHighlight(defCard, new Color(1f, 0.6f, 0f, 0.28f), gameStage);
      }
    } else if ("Magician".equals(name) && !defCard.isPlaceholder()) {
      com.mygdx.game.heroes.Magician mag = (com.mygdx.game.heroes.Magician) sel;
      if (mag.getSpells() > 0) {
        addCardActionHighlight(defCard, new Color(0f, 0.7f, 1f, 0.28f), gameStage);
      }
    } else if ("Warlord".equals(name) && !defCard.isPlaceholder()) {
      com.mygdx.game.heroes.Warlord wl = (com.mygdx.game.heroes.Warlord) sel;
      if (wl.isAttackAvailable()) {
        // Issue: red highlight invisible on face-down red card backs — use bright magenta.
        addCardActionHighlight(defCard, new Color(1f, 0.1f, 0.85f, 0.45f), gameStage);
      }
    }
  }

  /** Issues #179, #180, #54: highlight enemy king when a hero action targets it. */
  private void applyEnemyKingHighlight(Card kingCard, Player owner, Player current) {
    if (owner == current) return;
    Hero sel = selectedHero(current);
    if (sel == null) return;
    boolean defenderHasNoDef = owner.getDefCards().isEmpty() && owner.getTopDefCards().isEmpty();
    String name = sel.getHeroName();
    if ("Magician".equals(name) && defenderHasNoDef) {
      com.mygdx.game.heroes.Magician mag = (com.mygdx.game.heroes.Magician) sel;
      if (mag.getSpells() > 0) {
        addCardActionHighlight(kingCard, new Color(0f, 0.7f, 1f, 0.28f), gameStage);
      }
    } else if ("Warlord".equals(name) && defenderHasNoDef) {
      com.mygdx.game.heroes.Warlord wl = (com.mygdx.game.heroes.Warlord) sel;
      if (wl.isAttackAvailable()) {
        addCardActionHighlight(kingCard, new Color(1f, 0.1f, 0.85f, 0.45f), gameStage);
      }
    } else if ("Spy".equals(name) && defenderHasNoDef && kingCard.isCovered()) {
      // Spy peek on king is allowed only when all defs are face-up; here the defender has none.
      com.mygdx.game.heroes.Spy spy = (com.mygdx.game.heroes.Spy) sel;
      if (spy.getSpyAttacks() > 0) {
        addCardActionHighlight(kingCard, new Color(1f, 1f, 0f, 0.28f), gameStage);
      }
    } else if ("Spy".equals(name) && !defenderHasNoDef && kingCard.isCovered()) {
      // Also: all def cards face-up case
      com.mygdx.game.heroes.Spy spy = (com.mygdx.game.heroes.Spy) sel;
      if (spy.getSpyAttacks() > 0) {
        boolean allFaceUp = true;
        for (Card dc : owner.getDefCards().values()) { if (dc.isCovered()) { allFaceUp = false; break; } }
        if (allFaceUp) {
          for (Card dc : owner.getTopDefCards().values()) { if (dc.isCovered()) { allFaceUp = false; break; } }
        }
        if (allFaceUp) addCardActionHighlight(kingCard, new Color(1f, 1f, 0f, 0.28f), gameStage);
      }
    }
  }

  /** Issue #175: highlight the enemy hand deck when the Priest is selected. */
  private void applyEnemyHandDeckHighlight(Card topHandCard, Player owner, Player current) {
    if (owner == current) return;
    Hero sel = selectedHero(current);
    if (sel == null || !"Priest".equals(sel.getHeroName())) return;
    com.mygdx.game.heroes.Priest priest = (com.mygdx.game.heroes.Priest) sel;
    if (priest.getConversionAttempts() > 0) {
      addCardActionHighlight(topHandCard, new Color(1f, 1f, 0f, 0.28f), gameStage);
    }
  }

  /** Issues #54, #176: highlight own hand cards when Spy/Merchant is selected (sacrifice / trade). */
  private void applyOwnHandCardHighlight(Card handCard, Player current) {
    Hero sel = selectedHero(current);
    if (sel == null) return;
    String name = sel.getHeroName();
    if ("Spy".equals(name)) {
      com.mygdx.game.heroes.Spy spy = (com.mygdx.game.heroes.Spy) sel;
      if (spy.getSpyExtends() > 0) {
        addCardActionHighlight(handCard, new Color(1f, 0f, 0f, 0.32f), handStage);
      }
    } else if ("Merchant".equals(name)) {
      com.mygdx.game.heroes.Merchant m = (com.mygdx.game.heroes.Merchant) sel;
      if (m.getTrades() > 0) {
        addCardActionHighlight(handCard, new Color(1f, 0.6f, 0f, 0.32f), handStage);
      }
    }
  }

  /**
   * Issue #174: when the player has the Fortified Tower hero with charges and exactly
   * one hand card is selected, highlight every own defense slot whose bottom card matches
   * the hand card's symbol (and is not already stacked) so the player sees where the
   * auto-stack click will work.
   */
  private void applyOwnDefCardFortifyHighlight(Card defCard, Player owner, Player current, int slot) {
    if (owner != current) return;
    if (defCard.getLevel() != 0) return;
    if (owner.getTopDefCards().containsKey(slot)) return;
    if (current.getSelectedHandCards().size() != 1) return;
    com.mygdx.game.heroes.FortifiedTower ft = null;
    for (Hero h : current.getHeroes()) {
      if ("Fortified Tower".equals(h.getHeroName())) { ft = (com.mygdx.game.heroes.FortifiedTower) h; break; }
    }
    if (ft == null || ft.getDefenseExpands() <= 0) return;
    Card handCard = current.getSelectedHandCards().get(0);
    if (handCard.getSymbol().equals(defCard.getSymbol())) {
      addCardActionHighlight(defCard, new Color(0.6f, 0f, 1f, 0.32f), gameStage);
    }
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
    if (INSTANCE == this) INSTANCE = null;
    dispose();

  }

  @Override
  public void dispose() {
    screenDisposed = true;
    // Remove all GameScreen-exclusive socket listeners so this instance can be GC'd.
    // (Events also used by MenuScreen — "gameState", "returnToLobby" — are guarded
    //  by the screenDisposed flag in their call() bodies instead.)
    socket.off("stateUpdate");
    socket.off("heroAcquired");
    socket.off("heroLost");
    socket.off("saboteurDestroyed");
    socket.off("spyFlip");
    socket.off("batteryDefenseCheck");
    socket.off("batteryAllowAttack");
    socket.off("batteryDenyAttack");
    socket.off("mercDefBoost");
    socket.off("reservistsKingBoost");
    gameStage.dispose();
    handStage.dispose();
    overlayStage.dispose();
    if (plainWhiteTexture != null) plainWhiteTexture.dispose();
    if (texGameBck != null) texGameBck.dispose();
    if (texHandBck != null) texHandBck.dispose();
    for (Texture t : gameAvatarTextures.values()) { if (t != null) t.dispose(); }
    gameAvatarTextures.clear();
    texMercenary.dispose();
    texSabotaged.dispose();
    texHearts.dispose();
    texHeartsRed.dispose();
    texDiamonds.dispose();
    texDiamondsRed.dispose();
    texClubs.dispose();
    texSpades.dispose();
    texSomeSymbol.dispose();
    texShieldCheck.dispose();
    texArrowDownShield.dispose();
    texMenuButton.dispose();
    texZoomButton.dispose();
  }

}
