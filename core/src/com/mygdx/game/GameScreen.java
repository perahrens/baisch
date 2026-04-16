
package com.mygdx.game;
import com.mygdx.game.util.JSONObject;
import com.mygdx.game.util.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
  private Label myPlayerLabel;
  private Label roundCounter;
  private TextButton finishTurnButton;

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
  private InputMultiplexer menuAndGameMulti;
  // Battery Tower: stored when this local player is the defender and must allow/deny
  private JSONObject pendingBatteryDefCheck = null;
  // Attack preview broadcast: set from stateUpdate when another player has a pending attack
  private JSONObject pendingAttackBroadcast = null;
  // Plunder preview broadcast: set from stateUpdate when another player has a pending plunder
  private JSONObject pendingPlunderBroadcast = null;
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
  // Set when the current player ended their turn without attacking -- they must expose a defense card.
  private boolean pendingExposeCard = false;
  // Tutorial mode: guided overlay steps for new players
  private boolean isTutorial = false;
  private int tutorialStep = 0;
  private JSONArray activityLog = new JSONArray();
  // Emit Reservists count to other clients once on first render (before any stateUpdate fires)
  private boolean initialReservistsBroadcastDone = false;

  // ── Manual setup phase ────────────────────────────────────────────────────
  // -1 = not yet selected; >= 1 = card ID of the chosen king
  private int setupSelectedKingId = -1;
  // IDs of the 3 selected defense cards (-1 = not yet selected)
  private final int[] setupSelectedDefIds = { -1, -1, -1 };
  // True after the player has clicked Confirm (waiting for others to finish)
  private boolean setupSubmitted = false;
  private final ArrayList<Integer> setupDiscardIds = new ArrayList<Integer>();

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
  private Texture texSword;
  private Texture texCrone;
  private Texture texShieldCheck;
  private Texture texArrowDownShield;

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
              Map<Integer, Card> cards = (level == 0) ? p.getDefCards() : p.getTopDefCards();
              Card c = cards.get(slot);
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

    // Game ended — server tells all clients to return to the lobby
    socket.on("returnToLobby", new SocketListener() {
      @Override
      public void call(Object... args) {
        // MenuScreen's own returnToLobby listener will handle the screen switch;
        // nothing to do here since MenuScreen is still registered on the socket.
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

    gameBck = new Image(MyGdxGame.skin, "white");
    gameBck.setFillParent(true);
    gameBck.setColor(0.85f, 0.73f, 0.55f, 1);
    gameStage.addActor(gameBck);

    handBck = new Image(MyGdxGame.skin, "white");
    handBck.setFillParent(true);
    handBck.setColor(1f, 1f, 1f, 0.5f);
    handStage.addActor(handBck);

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
    texSword           = new Texture(Gdx.files.internal("data/skins/sword.png"));
    texCrone           = new Texture(Gdx.files.internal("data/skins/crone.png"));
    texShieldCheck     = new Texture(Gdx.files.internal("data/skins/shield-check-f.png"));
    texArrowDownShield = new Texture(Gdx.files.internal("data/skins/arrow-down-shield.png"));

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
    MyGdxGame.setMusicTrack(null); // no music during the game

    players = gameState.getPlayers();
    // Spectators always follow the player whose turn it currently is.
    if (isSpectator) {
      currentPlayer = gameState.getCurrentPlayer();
    }

    gameStage.clear();
    handStage.clear();
    overlayStage.clear();

    gameStage.addActor(gameBck);
    handStage.addActor(handBck);

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
    if (menuOpen) {
      buildMenuOverlay();
    } else {
      addMenuButtonToOverlay();
      if (isTutorial && tutorialStep >= 0) {
        buildTutorialOverlay();
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
    // requiredDiscards = hand - king(1) - def(3) - keep(2)  =  hand - 6
    final int requiredDiscards = Math.max(0, allHandSize - 6);
    final boolean inDiscardPhase = (setupSelectedKingId != -1 && _defCount == 3 && requiredDiscards > 0);
    String statusText;
    if (setupSubmitted) {
      statusText = "Waiting for other players...";
    } else if (setupSelectedKingId == -1) {
      statusText = "Select your king card";
    } else if (_defCount < 3) {
      statusText = "Select defense card " + (_defCount + 1) + " of 3";
    } else if (inDiscardPhase) {
      int stillNeeded = requiredDiscards - setupDiscardIds.size();
      if (stillNeeded > 0) {
        statusText = "Discard " + stillNeeded + " more card" + (stillNeeded > 1 ? "s" : "");
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
        } else if (inDiscardPhase && setupDiscardIds.contains(cardId)) {
          c.setColor(new Color(0.45f, 0.45f, 0.45f, 1f)); // grayed = marked for discard
        } else {
          c.setColor(Color.WHITE);
        }

        final boolean cardIsKing = (cardId == setupSelectedKingId);
        boolean cardIsDef = false;
        for (int id : setupSelectedDefIds) if (id == cardId) { cardIsDef = true; break; }
        final boolean cardIsKeepSelected = cardIsKing || cardIsDef;
        final boolean cardIsDiscard = inDiscardPhase && setupDiscardIds.contains(cardId);

        c.removeAllListeners();
        if (inDiscardPhase && !cardIsKeepSelected) {
          // In discard phase: tap non-king/non-def cards to toggle discard
          c.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              if (setupDiscardIds.contains(cardId)) {
                setupDiscardIds.remove((Integer) cardId);
              } else {
                setupDiscardIds.add(cardId);
              }
              gameState.setUpdateState(true);
            }
          });
        } else if (!inDiscardPhase) {
          // In king/def selection phase
          c.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              // Deselect king
              if (cardId == setupSelectedKingId) {
                setupSelectedKingId = -1;
                setupDiscardIds.clear();
                gameState.setUpdateState(true);
                return;
              }
              // Deselect def
              for (int slot = 0; slot < 3; slot++) {
                if (setupSelectedDefIds[slot] == cardId) {
                  setupSelectedDefIds[slot] = -1;
                  for (int s = slot; s < 2; s++) setupSelectedDefIds[s] = setupSelectedDefIds[s + 1];
                  setupSelectedDefIds[2] = -1;
                  setupDiscardIds.clear();
                  gameState.setUpdateState(true);
                  return;
                }
              }
              // Select as king
              if (setupSelectedKingId == -1) {
                setupSelectedKingId = cardId;
                setupDiscardIds.clear();
                gameState.setUpdateState(true);
                return;
              }
              // Select as def (up to 3)
              for (int slot = 0; slot < 3; slot++) {
                if (setupSelectedDefIds[slot] == -1) {
                  setupSelectedDefIds[slot] = cardId;
                  setupDiscardIds.clear();
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

      // ── Discard labels (when in discard phase) ───────────────────────────
      if (inDiscardPhase) {
        for (int i = 0; i < count; i++) {
          final Card c2 = handCards.get(i);
          final int cid = c2.getCardId();
          if (cid == setupSelectedKingId) continue;
          boolean isd = false; for (int id : setupSelectedDefIds) if (id == cid) { isd = true; break; }
          if (isd) continue;
          // Show DISCARD / KEEP label below the card
          boolean markedDiscard = setupDiscardIds.contains(cid);
          Label discardLabel = new Label(markedDiscard ? "DISCARD" : "KEEP", MyGdxGame.skin);
          discardLabel.setColor(markedDiscard ? Color.RED : new Color(0.7f, 0.7f, 0.7f, 1f));
          discardLabel.pack();
          discardLabel.setPosition(c2.getX() + cardW / 2f - discardLabel.getWidth() / 2f,
              handY - discardLabel.getHeight() - 4f);
          handStage.addActor(discardLabel);
        }
      }

      // ── Confirm button (shown when king + 3 def chose AND discards done) ─
      int defCount = 0;
      for (int id : setupSelectedDefIds) if (id != -1) defCount++;
      boolean discardsReady = (requiredDiscards == 0) || (setupDiscardIds.size() == requiredDiscards);
      if (setupSelectedKingId != -1 && defCount == 3 && discardsReady) {
        final ArrayList<Integer> frozenDiscards = new ArrayList<Integer>(setupDiscardIds);
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
              JSONArray discardIds = new JSONArray();
              for (int id : frozenDiscards) discardIds.put(id);
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
    roundCounter.setColor(0f, 0f, 0f, 1.0f);
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
        }

        // Count label only for other players (not the local player).
        // Centred directly on the deck visual centre: (anchorX+W/2, anchorY+H/2).
        if (i != playerIndex) {
          Label handCountLabel = new Label(String.valueOf(handCards.size()), MyGdxGame.skin);
          handCountLabel.setColor(Color.BLACK);
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
            gameState.getCurrentPlayer().getTopDefCards(), gameState.getCurrentPlayer().getHandCards());
        kingCard.addListener(ownKingCardListener);
      }

      gameStage.addActor(kingCard);

      if (kingCard.getBoosted() > 0) {
        TextureRegion mercenaryRegion = new TextureRegion(texMercenary, 0, 0, 512, 512);
        Image mercenaryImage = new Image(mercenaryRegion);
        mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(), mercenaryImage.getWidth() / 20f,
            mercenaryImage.getHeight() / 20f);
        mercenaryImage.setPosition(kingCard.getX(), kingCard.getY());
        mercenaryImage.setX(mercenaryImage.getX() + kingCard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
        mercenaryImage.setY(mercenaryImage.getY() + kingCard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);
        removeAllListeners(mercenaryImage);
        mercenaryImageListener = new MercenaryImageListener(gameState, kingCard, currentPlayer);
        mercenaryImage.addListener(mercenaryImageListener);
        gameStage.addActor(mercenaryImage);

        String boostCount = String.valueOf(kingCard.getBoosted());
        Label boostCountLabel = new Label(boostCount, MyGdxGame.skin);
        boostCountLabel.setColor(Color.GOLD);
        boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());
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
          removeAllListeners(mercenaryImage);
          mercenaryImageListener = new MercenaryImageListener(gameState, defCard, currentPlayer,
              socket, playerIndex, defCard.getPositionId(), 0);
          mercenaryImage.addListener(mercenaryImageListener);
          gameStage.addActor(mercenaryImage);

          String boostCount = "+" + String.valueOf(defCard.getBoosted());
          Label boostCountLabel = new Label(boostCount, MyGdxGame.skin);
          boostCountLabel.setColor(Color.GOLD);
          boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());
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
        }
      }

      // display player label
      Label playerLabel = new Label(players.get(i).getPlayerName(), MyGdxGame.skin);
      // Highlight the player whose turn it currently is
      if (players.get(i) == gameState.getCurrentPlayer()) {
        playerLabel.setColor(Color.GOLD);
      } else {
        playerLabel.setColor(0f, 0f, 0f, 1.0f);
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
    }

    // Add hand count labels AFTER all player actors so they render on top
    for (Label lbl : handCountLabels) {
      gameStage.addActor(lbl);
    }

    // Plunder preview overlay — added LAST so it renders on top of everything
    if (currentPlayer.getPlayerTurn().isPlunderPending()) {
      final Player plunderPlayer = currentPlayer;
      final PlayerTurn pt = plunderPlayer.getPlayerTurn();
      final boolean plunderSuccess = pt.isPlunderSuccess();

      // Semi-transparent black tint over the whole board; catches any tap to confirm
      Image overlay = new Image(MyGdxGame.skin, "white");
      overlay.setFillParent(true);
      overlay.setColor(0f, 0f, 0f, 0.45f);
      overlay.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          final int deckIdx = pt.getPendingPickingDeckIndex();
          PickingDeck thisD = gameState.getPickingDecks().get(deckIdx);
          PickingDeck otherD = gameState.getPickingDecks().get(1 - deckIdx);
          if (plunderSuccess) {
            Iterator<Card> it = thisD.getCards().iterator();
            while (it.hasNext()) { plunderPlayer.addHandCard(it.next()); it.remove(); }
            otherD.addCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
            thisD.addCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
            thisD.getCards().get(thisD.getCards().size() - 1).setCovered(false);
            thisD.addCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
            if (pt.isKingUsed()) plunderPlayer.getKingCard().setCovered(false);
          } else {
            Card newPickCard = gameState.getCardDeck().getCard(gameState.getCemeteryDeck());
            newPickCard.setCovered(true);
            thisD.addCard(newPickCard);
            if (pt.isKingUsed()) {
              plunderPlayer.getKingCard().setCovered(false);
              plunderPlayer.setOut(true);
            }
          }
          for (Card c : pt.getPendingAttackCards()) {
            plunderPlayer.getHandCards().remove(c);
            gameState.getCemeteryDeck().addCard(c);
          }
          for (Card c : pt.getPendingAttackOwnDefCards()) {
            int slot = c.getPositionId();
            plunderPlayer.getDefCards().remove(slot);
            plunderPlayer.getTopDefCards().remove(slot);
            gameState.getCemeteryDeck().addCard(c);
          }
          pt.setPlunderPending(false);
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
            emitData.put("success", plunderSuccess);
            emitData.put("kingUsed", pt.isKingUsed());
            JSONArray atkIdArr = new JSONArray();
            for (Card c : pt.getPendingAttackCards()) atkIdArr.put(c.getCardId());
            emitData.put("attackCardIds", atkIdArr);
            JSONArray ownDefIdArr = new JSONArray();
            for (Card c : pt.getPendingAttackOwnDefCards()) ownDefIdArr.put(c.getCardId());
            emitData.put("attackerOwnDefCardIds", ownDefIdArr);
            socket.emit("plunderResolved", emitData);
          } catch (JSONException e) {
            e.printStackTrace();
          }
          pt.getPendingAttackCards().clear();
          pt.getPendingAttackOwnDefCards().clear();
          pt.resetReservistAttackBonus();
          pt.resetPendingAttackMercenaryBonus();
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

      int plAtkSum = pt.getPendingPlunderAttackSum() + pt.getReservistAttackBonus();
      int plDefStr = pt.getPendingPlunderDefStrength();

      Label plunderResultLabel = new Label(
          plunderSuccess ? "SUCCESS!  Tap to claim the cards."
                        : "FAILED.  Tap to continue.",
          MyGdxGame.skin);
      plunderResultLabel.setColor(plunderSuccess ? Color.GREEN : Color.RED);
      plunderResultLabel.setPosition(
          MyGdxGame.WIDTH / 2f - plunderResultLabel.getPrefWidth() / 2f,
          plBotY - 44f);

      gameStage.addActor(overlay);

      // Column headers
      Label plAtkHdr = new Label("ATTACK", MyGdxGame.skin);
      plAtkHdr.setColor(Color.CYAN);
      plAtkHdr.setPosition(plLeftX, plBotY + plCH + 5f);
      gameStage.addActor(plAtkHdr);
      Label plDefHdr = new Label("PLUNDER", MyGdxGame.skin);
      plDefHdr.setColor(Color.ORANGE);
      plDefHdr.setPosition(plRightX, plBotY + plCH + 5f);
      gameStage.addActor(plDefHdr);

      // Attack cards (left column)
      if (pt.isKingUsed() && plunderPlayer.getKingCard() != null) {
        Card kd = Card.fromCardId(plunderPlayer.getKingCard().getCardId());
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

      gameStage.addActor(plunderResultLabel);

      // Reservists plunder boost button — only when currently failing but can be won
      for (Hero resH : plunderPlayer.getHeroes()) {
        if ("Reservists".equals(resH.getHeroName())) {
          final Reservists resHero = (Reservists) resH;
          boolean canFlipPlunder = !pt.isPlunderSuccess() && resHero.isAvailable()
              && (pt.getPendingPlunderAttackSum() + pt.getReservistAttackBonus() + resHero.countReady())
                  > pt.getPendingPlunderDefStrength();
          if (canFlipPlunder) {
            TextButton resBtn = new TextButton("Reservists +1  (" + resHero.countReady() + " left)", MyGdxGame.skin);
            resBtn.setWidth(MyGdxGame.WIDTH / 3f);
            resBtn.setPosition(MyGdxGame.WIDTH / 2f - resBtn.getWidth() / 2f, MyGdxGame.WIDTH * 0.42f);
            resBtn.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                resHero.spend();
                emitReservistsKingBoost(resHero.countReady());
                pt.incrementReservistAttackBonus();
                boolean newPlunderSuccess =
                    pt.getPendingPlunderAttackSum() + pt.getReservistAttackBonus() > pt.getPendingPlunderDefStrength();
                pt.setPlunderSuccess(newPlunderSuccess);
                // Re-emit plunderPreview so watchers see the updated sum and outcome
                if (socket != null) {
                  try {
                    JSONObject plPreview = new JSONObject();
                    plPreview.put("attackerIdx", playerIndex);
                    plPreview.put("deckIndex", plDeckIdx);
                    ArrayList<Card> plDeckCurr = gameState.getPickingDecks().get(plDeckIdx).getCards();
                    plPreview.put("defCardId", plDeckCurr.isEmpty() ? -1 : plDeckCurr.get(plDeckCurr.size() - 1).getCardId());
                    plPreview.put("attackSum", pt.getPendingPlunderAttackSum() + pt.getReservistAttackBonus());
                    plPreview.put("defStrength", pt.getPendingPlunderDefStrength());
                    plPreview.put("success", newPlunderSuccess);
                    plPreview.put("kingUsed", pt.isKingUsed());
                    plPreview.put("kingCardId", pt.isKingUsed() && plunderPlayer.getKingCard() != null ? plunderPlayer.getKingCard().getCardId() : -1);
                    plPreview.put("mercenaryBonus", pt.getPendingAttackMercenaryBonus());
                    plPreview.put("reservistBonus", pt.getReservistAttackBonus());
                    JSONArray plResAtkIds = new JSONArray();
                    for (Card c : pt.getPendingAttackCards()) plResAtkIds.put(c.getCardId());
                    plPreview.put("attackCardIds", plResAtkIds);
                    JSONArray plResOwnIds = new JSONArray();
                    for (Card c : pt.getPendingAttackOwnDefCards()) plResOwnIds.put(c.getCardId());
                    plPreview.put("ownDefCardIds", plResOwnIds);
                    socket.emit("plunderPreview", plPreview);
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

    // Plunder watcher overlay — shown to non-plundering players when another player is plundering
    if (pendingPlunderBroadcast != null && !currentPlayer.getPlayerTurn().isPlunderPending()) {
      try {
        final int plBcAtkIdx = pendingPlunderBroadcast.getInt("attackerIdx");
        final boolean plBcSuccess = pendingPlunderBroadcast.getBoolean("success");
        final boolean plBcKingUsed = pendingPlunderBroadcast.optBoolean("kingUsed", false);
        final int plBcKingCardId = pendingPlunderBroadcast.optInt("kingCardId", -1);
        final int plBcDefCardId = pendingPlunderBroadcast.optInt("defCardId", -1);
        final int plBcAtkSum = pendingPlunderBroadcast.optInt("attackSum", 0)
            + pendingPlunderBroadcast.optInt("reservistBonus", 0);
        final int plBcDefStr = pendingPlunderBroadcast.optInt("defStrength", 0);
        final JSONArray plBcAtkIds = pendingPlunderBroadcast.optJSONArray("attackCardIds");
        final JSONArray plBcOwnDefIds = pendingPlunderBroadcast.optJSONArray("ownDefCardIds");

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
        Label wPlAtkHdr = new Label(plAtkName + " plunders:", MyGdxGame.skin);
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

        Label wPlResultLbl = new Label(plBcSuccess ? plAtkName + " plunders!" : plAtkName + " fails!", MyGdxGame.skin);
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
            } catch (JSONException e) {
              e.printStackTrace();
            }
          } else {
            // Regular defense card attack
            if (atkSuccess) {
              for (Card dc : apt.getPendingAttackDefCards()) {
                dc.setRemoved(true);
                atkPlayer.addHandCard(dc);
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
          if (apt.isKingUsed()) apt.setKingUsedThisTurn(true);
          // Clear hand card attack boost visuals after attack resolves
          for (Card c : atkPlayer.getHandCards()) {
            c.setSelected(false);
            while (c.getBoosted() > 0) c.addBoosted(-1);
          }
          // Clear own def card selections
          for (Card c : atkPlayer.getDefCards().values()) c.setSelected(false);
          for (Card c : atkPlayer.getTopDefCards().values()) c.setSelected(false);
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
        for (int ai = 0; ai < atkSrc.size(); ai++) {
          Card disp = Card.fromCardId(atkSrc.get(ai).getCardId());
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(aW, aH);
          disp.setPosition(leftX + ai * (aW + 4f), cBotY + (bCH - aH) / 2f);
          gameStage.addActor(disp);
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
          Card disp = Card.fromCardId(pendingDefViz.get(di).getCardId());
          disp.setCovered(false); disp.setActive(true);
          disp.setSize(dW, dH);
          disp.setPosition(rightX + di * (dW + 4f), cBotY + (bCH - dH) / 2f);
          gameStage.addActor(disp);
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
              resBtn.setWidth(MyGdxGame.WIDTH / 3f);
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
        final int bcResBonus = pendingAttackBroadcast.optInt("reservistBonus", 0);
        final JSONArray bcAtkIds = pendingAttackBroadcast.optJSONArray("attackCardIds");
        final JSONArray bcOwnDefIds = pendingAttackBroadcast.optJSONArray("ownDefCardIds");
        final JSONArray bcDefIds = pendingAttackBroadcast.optJSONArray("defCardIds");

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
          disp.setPosition(wRightX + di * (wDW + 4f), wBotY + (wCH - wDH) / 2f);
          gameStage.addActor(disp);
          wDefSum += "joker".equals(disp.getSymbol()) ? 1 : disp.getStrength();
        }

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

      // Layout hero buttons in rows of 4
      float btnW    = MyGdxGame.WIDTH / 5f;
      float btnGapX = MyGdxGame.WIDTH * 0.05f;
      float startX  = (MyGdxGame.WIDTH - 4f * btnW - 3f * btnGapX) / 2f;
      float startY  = MyGdxGame.WIDTH * 0.62f;
      float rowH    = 0f;

      for (int ci = 0; ci < choices.size(); ci++) {
        final Hero choice = choices.get(ci);
        TextButton heroBtn = new TextButton(choice.getHeroName(), MyGdxGame.skin);
        if (rowH == 0f) rowH = heroBtn.getHeight() + 8f;
        int col = ci % 4;
        int row = ci / 4;
        heroBtn.setWidth(btnW);
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

      float btnW    = MyGdxGame.WIDTH / 5f;
      float btnGapX = MyGdxGame.WIDTH * 0.05f;
      float startX  = (MyGdxGame.WIDTH - 4f * btnW - 3f * btnGapX) / 2f;
      float startY  = MyGdxGame.WIDTH * 0.62f;
      float rowH    = 0f;

      for (int ci = 0; ci < kdOptions.size(); ci++) {
        final String heroName = kdOptions.get(ci);
        TextButton heroBtn = new TextButton(heroName, MyGdxGame.skin);
        if (rowH == 0f) rowH = heroBtn.getHeight() + 8f;
        int col = ci % 4;
        int row = ci / 4;
        heroBtn.setWidth(btnW);
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
        float btnW    = MyGdxGame.WIDTH / 5f;
        float btnGapX = MyGdxGame.WIDTH * 0.05f;
        float startX  = (MyGdxGame.WIDTH - 4f * btnW - 3f * btnGapX) / 2f;
        float startY  = MyGdxGame.WIDTH * 0.58f;
        float rowH    = 0f;
        for (int ci = 0; ci < phList.size(); ci++) {
          final String hName = phList.get(ci).getHeroName();
          TextButton hBtn = new TextButton(hName, MyGdxGame.skin);
          if (rowH == 0f) rowH = hBtn.getHeight() + 8f;
          int col = ci % 4;
          int row = ci / 4;
          hBtn.setWidth(btnW);
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
        selCancel.setPosition(MyGdxGame.WIDTH / 2f - selCancel.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.35f);
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
        mbTitle.setPosition(MyGdxGame.WIDTH / 2f - mbTitle.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.62f);
        gameStage.addActor(mbTitle);

        Label mbValue = new Label(String.valueOf(auctionSellMinBid), MyGdxGame.skin);
        mbValue.setFontScale(2f);
        mbValue.setColor(Color.WHITE);
        mbValue.setPosition(MyGdxGame.WIDTH / 2f - mbValue.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.47f);
        gameStage.addActor(mbValue);

        TextButton minusBtn = new TextButton("-", MyGdxGame.skin);
        minusBtn.setSize(90f, 70f);
        minusBtn.setPosition(MyGdxGame.WIDTH / 2f - 140f, MyGdxGame.WIDTH * 0.46f);
        minusBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (auctionSellMinBid > 1) { auctionSellMinBid--; gameState.setUpdateState(true); }
          }
        });
        gameStage.addActor(minusBtn);

        TextButton plusBtn = new TextButton("+", MyGdxGame.skin);
        plusBtn.setSize(90f, 70f);
        plusBtn.setPosition(MyGdxGame.WIDTH / 2f + 60f, MyGdxGame.WIDTH * 0.46f);
        plusBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            auctionSellMinBid++;
            gameState.setUpdateState(true);
          }
        });
        gameStage.addActor(plusBtn);

        TextButton confirmBtn = new TextButton("Start Auction", MyGdxGame.skin);
        confirmBtn.setPosition(MyGdxGame.WIDTH / 2f - confirmBtn.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.33f);
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
        mbCancel.setPosition(MyGdxGame.WIDTH / 2f - mbCancel.getPrefWidth() / 2f, MyGdxGame.WIDTH * 0.24f);
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
    if (pendingHeroAuction != null) {
      try {
        final int sellerIdx    = pendingHeroAuction.getInt("sellerIdx");
        final String aHeroName = pendingHeroAuction.getString("heroName");
        final int minBidVal    = pendingHeroAuction.getInt("minBid");
        final int curBidderIdx = pendingHeroAuction.getInt("currentBidderIdx");
        final JSONObject curBid = pendingHeroAuction.optJSONObject("currentBid");

        Image aOv = new Image(MyGdxGame.skin, "white");
        aOv.setFillParent(true);
        aOv.setColor(0f, 0f, 0f, 0.82f);
        gameStage.addActor(aOv);

        String sellerName = (sellerIdx >= 0 && sellerIdx < players.size())
            ? players.get(sellerIdx).getPlayerName() : "Player " + sellerIdx;
        Label aTitle = new Label(sellerName + " is selling " + aHeroName
            + "  (min bid: " + minBidVal + ")", MyGdxGame.skin);
        aTitle.setColor(Color.GOLD);
        aTitle.setPosition(MyGdxGame.WIDTH / 2f - aTitle.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.88f);
        gameStage.addActor(aTitle);

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
                heroImg.setPosition(MyGdxGame.WIDTH / 2f - heroW / 2f, MyGdxGame.HEIGHT * 0.82f - heroH / 2f);
                gameStage.addActor(heroImg);
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
          bidLabel.setPosition(MyGdxGame.WIDTH / 2f - bidLabel.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.70f);
          gameStage.addActor(bidLabel);
        } else {
          Label noBid = new Label("No bids yet", MyGdxGame.skin);
          noBid.setColor(Color.LIGHT_GRAY);
          noBid.setPosition(MyGdxGame.WIDTH / 2f - noBid.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.70f);
          gameStage.addActor(noBid);
        }

        if (!isSpectator && curBidderIdx == playerIndex) {
          // ── This player's bid turn ──────────────────────────────────────
          Label yourTurnLbl = new Label("Your turn — select cards to bid:", MyGdxGame.skin);
          yourTurnLbl.setColor(Color.GREEN);
          yourTurnLbl.setPosition(MyGdxGame.WIDTH / 2f - yourTurnLbl.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.62f);
          gameStage.addActor(yourTurnLbl);

          // Hand cards as toggle buttons
          final ArrayList<Card> myHand = currentPlayer.getHandCards();
          float cBtnW = MyGdxGame.WIDTH / 6.5f;
          float cBtnH = cBtnW * 1.2f;
          float cGapX = MyGdxGame.WIDTH * 0.008f;
          int maxPerRow = 6;
          float handRowsY = MyGdxGame.HEIGHT * 0.54f;
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
            gameStage.addActor(cb);
          }

          // Defense cards as toggle buttons
          Map<Integer, Card> myDefs = currentPlayer.getDefCards();
          Map<Integer, Card> myTopDefs = currentPlayer.getTopDefCards();
          java.util.List<Integer> defIds = new java.util.ArrayList<Integer>();
          for (Card dc : myDefs.values()) defIds.add(dc.getCardId());
          for (Card dc : myTopDefs.values()) defIds.add(dc.getCardId());
          if (!defIds.isEmpty()) {
            Label defHdr = new Label("Defense:", MyGdxGame.skin);
            defHdr.setColor(Color.YELLOW);
            float defRowY = MyGdxGame.HEIGHT * 0.32f;
            defHdr.setPosition(MyGdxGame.WIDTH / 2f - defHdr.getPrefWidth() / 2f, defRowY + cBtnH + 2f);
            gameStage.addActor(defHdr);
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
              gameStage.addActor(dcBtn);
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
          strLbl.setPosition(MyGdxGame.WIDTH / 2f - strLbl.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.20f);
          gameStage.addActor(strLbl);

          final int finalTotal = totalBidStr;
          TextButton bidBtn = new TextButton("Bid", MyGdxGame.skin);
          bidBtn.setColor(canBid ? Color.WHITE : Color.DARK_GRAY);
          bidBtn.setPosition(MyGdxGame.WIDTH / 2f - bidBtn.getPrefWidth() - 16f, MyGdxGame.HEIGHT * 0.12f);
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
          gameStage.addActor(bidBtn);

          TextButton passBtn = new TextButton("Pass", MyGdxGame.skin);
          passBtn.setPosition(MyGdxGame.WIDTH / 2f + 16f, MyGdxGame.HEIGHT * 0.12f);
          passBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              socket.emit("heroAuctionPass", new JSONObject());
              auctionBidHandCardIds.clear();
              auctionBidDefCardIds.clear();
              gameState.setUpdateState(true);
            }
          });
          gameStage.addActor(passBtn);

        } else {
          // ── Waiting for another player (or seller watching) ─────────────
          String waitName = (curBidderIdx >= 0 && curBidderIdx < players.size())
              ? players.get(curBidderIdx).getPlayerName() : "Player " + curBidderIdx;
          Label waitLbl = new Label("Waiting for " + waitName + " to bid or pass...", MyGdxGame.skin);
          waitLbl.setColor(Color.LIGHT_GRAY);
          waitLbl.setPosition(MyGdxGame.WIDTH / 2f - waitLbl.getPrefWidth() / 2f, MyGdxGame.HEIGHT * 0.5f);
          gameStage.addActor(waitLbl);
        }
      } catch (JSONException e) { e.printStackTrace(); }
    }

    // Sword overlay on both harvest decks when plunder is available — added late so it sits above all cards.
    // Crone overlay on own king card when king attack is possible.
    if (gameState.getCurrentPlayer() == currentPlayer) {
      PlayerTurn ptGame = currentPlayer.getPlayerTurn();

      if (ptGame.getPickingDeckAttacks() > 0 && !ptGame.isPlunderPending()) {
        ArrayList<PickingDeck> swordDecks = gameState.getPickingDecks();
        for (int si = 0; si < swordDecks.size(); si++) {
          ArrayList<Card> sCards = swordDecks.get(si).getCards();
          if (!sCards.isEmpty()) {
            Card topCard = sCards.get(sCards.size() - 1);
            float iconSize = topCard.getDefWidth() * 0.55f;
            float cx = topCard.getX() + topCard.getDefWidth() / 2f;
            float cy = topCard.getY() + topCard.getDefHeight() / 2f;
            Image swordImg = new Image(new TextureRegion(texSword, 0, 0, texSword.getWidth(), texSword.getHeight())) {
              @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
            };
            swordImg.setSize(iconSize, iconSize);
            swordImg.setPosition(cx - iconSize / 2f, cy - iconSize / 2f);
            gameStage.addActor(swordImg);
          }
        }
      }

      boolean canKingAtk = currentPlayer.getDefCards().isEmpty()
          && currentPlayer.getTopDefCards().isEmpty()
          && !ptGame.isKingUsedThisTurn()
          && !ptGame.isAttackPending();
      if (canKingAtk && currentPlayer.getKingCard() != null) {
        Card kc = currentPlayer.getKingCard();
        float iconSize = kc.getDefWidth() * 0.55f;
        float cx = kc.getX() + kc.getDefWidth() / 2f;
        float cy = kc.getY() + kc.getDefHeight() / 2f;
        Image croneImg = new Image(new TextureRegion(texCrone, 0, 0, texCrone.getWidth(), texCrone.getHeight())) {
          @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
        };
        croneImg.setSize(iconSize, iconSize);
        croneImg.setPosition(cx - iconSize / 2f, cy - iconSize / 2f);
        gameStage.addActor(croneImg);
      }
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

    // Merchant 2nd-try reveal: display the drawn card face-up for all non-trading clients
    if (merchantRevealCardId != -1 && merchantRevealPlayerIdx != playerIndex) {
      Card revealCard = Card.fromCardId(merchantRevealCardId);
      float rcw = revealCard.getDefWidth() * 1.5f;
      float rch = revealCard.getDefHeight() * 1.5f;
      revealCard.setWidth(rcw);
      revealCard.setHeight(rch);
      revealCard.setPosition(
          (MyGdxGame.WIDTH - rcw) / 2f,
          (MyGdxGame.WIDTH - rch) / 2f);
      gameStage.addActor(revealCard);
      Label revealLabel = new Label("Merch. reveal (P" + merchantRevealPlayerIdx + ")", MyGdxGame.skin);
      revealLabel.setColor(Color.GREEN);
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
            }
          }
          gameStage.addActor(display);
        }

        // Buttons below the cards
        float btnY = cardRowY - 55f;
        float btnW = 120f;
        if (revealedId >= 0) {
          if (priest.getConversionAttempts() > 0) {
            TextButton tryAgainBtn = new TextButton("Try again", MyGdxGame.skin);
            tryAgainBtn.setSize(btnW, 45f);
            tryAgainBtn.setPosition(MyGdxGame.WIDTH / 2f - btnW / 2f, btnY);
            tryAgainBtn.addListener(new ClickListener() {
              @Override
              public void clicked(InputEvent event, float x, float y) {
                gameState.setPriestRevealedCardId(-1);
                gameState.setPriestTargetPlayerIdx(-1);
                gameState.setUpdateState(true);
              }
            });
            gameStage.addActor(tryAgainBtn);
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
                        && currentPlayer.canMobilize() && !currentPlayer.isSlotSabotaged(posId)) {
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

    // Compute row distribution: when >10 cards, extra cards go first to upper row then lower row
    int handCardCount = handCards.size();
    int upperCount, lowerCount;
    if (handCardCount <= 5) {
      upperCount = handCardCount;
      lowerCount = 0;
    } else if (handCardCount <= 10) {
      upperCount = 5;
      lowerCount = handCardCount - 5;
    } else {
      int extra = handCardCount - 10;
      upperCount = 5 + (extra + 1) / 2;
      lowerCount = 5 + extra / 2;
    }

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
        boostCountLabel.setColor(Color.GOLD);
        boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());
        handStage.addActor(boostCountLabel);
      }

      if (handCards.get(j).isTradeable()) {
        // Merchant dialog is handled as a modal overlay in showGameStage — skip inline buttons here.
      }
    }

    // Display all heroes of current player
    for (int j = 0; j < playerHeroes.size(); j++) {
      final Hero hero = playerHeroes.get(j);
      hero.setHand(true);
      hero.setPosition(j * hero.getWidth(), 0);

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
      heroLabel.setPosition(j * hero.getWidth() + (hero.getWidth() - heroLabel.getWidth()) / 2, hero.getHeight());
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
        btCountLabel.setColor(Color.YELLOW);
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
        readyCountLabel.setColor(Color.GOLD);
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
    finishTurnButton.setSize(finishTurnButton.getWidth() * 1.5f, finishTurnButton.getHeight() * 1.5f);
    finishTurnButton.setPosition(MyGdxGame.WIDTH - finishTurnButton.getWidth(), 0);
    myPlayerLabel = new Label(currentPlayer.getPlayerName(), MyGdxGame.skin);

    // Turn indicator (spectators are never "my turn")
    boolean isMyTurn = !isSpectator && (gameState.getCurrentPlayer() == currentPlayer);

    // "Sacrifice Joker" button — only on your turn, bottom-left of hand stage
    if (isMyTurn && !currentPlayer.getPlayerTurn().isHeroSelectionPending()) {
      Card jokerInHand = null;
      for (Card hc : handCards) {
        if ("joker".equals(hc.getSymbol())) { jokerInHand = hc; break; }
      }
      if (jokerInHand != null) {
        final Card theJoker = jokerInHand;
        TextButton heroBtn = new TextButton("Get Hero", MyGdxGame.skin);
        heroBtn.setSize(theJoker.getWidth(), heroBtn.getPrefHeight());
        heroBtn.setPosition(theJoker.getX(), theJoker.getY());
        heroBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            performJokerSacrifice(theJoker);
          }
        });
        handStage.addActor(heroBtn);
      }
    }

    // "Sell Hero" button — only on your turn, no ongoing auction, at least one hero owned
    if (isMyTurn && pendingHeroAuction == null && auctionSellHeroName == null
        && !playerHeroes.isEmpty()) {
      final ArrayList<Hero> phForSell = playerHeroes;
      float heroW = phForSell.get(0).getWidth();
      TextButton sellHeroBtn = new TextButton("Sell Hero", MyGdxGame.skin);
      sellHeroBtn.setSize(heroW * 1.2f, sellHeroBtn.getPrefHeight());
      sellHeroBtn.setPosition(phForSell.size() * heroW + 8f, 0f);
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
      handStage.addActor(sellHeroBtn);
    }

    // Spectators: hide finish-turn button and show a spectator indicator instead.
    // Regular players: show button only on their turn.
    if (isSpectator) {
      finishTurnButton.setVisible(false);
      Label spectatorLabel = new Label("Spectator Mode", MyGdxGame.skin);
      spectatorLabel.setColor(Color.CYAN);
      spectatorLabel.setPosition(MyGdxGame.WIDTH - spectatorLabel.getPrefWidth(), 0);
      handStage.addActor(spectatorLabel);
    } else if (isMyTurn && pendingExposeCard) {
      finishTurnButton.setVisible(false);
      addExposeCardOverlay();
    } else {
      finishTurnButton.setVisible(isMyTurn);
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
    Table hudPanel = new Table(MyGdxGame.skin);
    hudPanel.setBackground(MyGdxGame.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.4f)));
    hudPanel.pad(hudPad);
    hudPanel.add(myPlayerLabel).padBottom(2f).row();
    hudPanel.add(iconsRow);
    hudPanel.pack();
    hudPanel.setPosition(
        MyGdxGame.WIDTH - hudPanel.getWidth() - 2f,
        finishTurnButton.getHeight() + 2f);
    handStage.addActor(hudPanel);

    handStage.addActor(finishTurnButton);
  }

  private void addExposeCardOverlay() {
    float stageW = MyGdxGame.WIDTH;
    float stageH = MyGdxGame.HEIGHT - MyGdxGame.WIDTH;

    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setSize(stageW, stageH);
    bg.setPosition(0, 0);
    bg.setColor(0f, 0f, 0f, 0.72f);
    handStage.addActor(bg);

    Label prompt = new Label("No attack -- expose a defense card:", MyGdxGame.skin);
    prompt.setColor(Color.YELLOW);
    prompt.setPosition(stageW / 2f - prompt.getPrefWidth() / 2f, stageH - prompt.getPrefHeight() - 6);
    handStage.addActor(prompt);

    float btnW = stageW / 4f;
    float btnX = 4;
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
      slotBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          pendingExposeCard = false;
          try {
            JSONObject exposeData = new JSONObject();
            exposeData.put("playerIdx", playerIndex);
            exposeData.put("slot", finalSlot);
            socket.emit("exposeDefCard", exposeData);
            JSONObject ftData = new JSONObject();
            ftData.put("currentPlayerIndex", gameState.getCurrentPlayerIndex());
            socket.emit("finishTurn", ftData);
          } catch (JSONException ex) { ex.printStackTrace(); }
        }
      });
      handStage.addActor(slotBtn);
    }
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
    table.add(resumeBtn).width(300).height(60).padBottom(14).row();

    TextButton historyBtn = new TextButton("History", MyGdxGame.skin);
    historyBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        GameScreen.this.showLogOverlay();
      }
    });
    table.add(historyBtn).width(300).height(60).padBottom(14).row();

    final boolean musicOn = MyGdxGame.playerStorage.getMusicEnabled();
    TextButton musicBtn = new TextButton(musicOn ? "Music ON" : "Music OFF", MyGdxGame.skin);
    musicBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        boolean newEnabled = !MyGdxGame.playerStorage.getMusicEnabled();
        MyGdxGame.setMusicEnabled(newEnabled);
        buildMenuOverlay();
      }
    });
    table.add(musicBtn).width(300).height(60).padBottom(14).row();

    if (isSpectator || (currentPlayer != null && currentPlayer.isOut())) {
      TextButton leaveBtn = new TextButton("Leave Game", MyGdxGame.skin);
      leaveBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          closeMenu();
          navigateToLobby();
        }
      });
      table.add(leaveBtn).width(300).height(60).row();
    } else {
      TextButton giveUpStayBtn = new TextButton("Give Up & Stay", MyGdxGame.skin);
      giveUpStayBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          closeMenu();
          emitGiveUp();
        }
      });
      table.add(giveUpStayBtn).width(300).height(60).padBottom(14).row();

      TextButton giveUpLeaveBtn = new TextButton("Give Up & Leave", MyGdxGame.skin);
      giveUpLeaveBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          closeMenu();
          emitGiveUpAndLeave();
        }
      });
      table.add(giveUpLeaveBtn).width(300).height(60).row();
    }

    overlayStage.addActor(table);
  }

  private void showLogOverlay() {
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

    if (activityLog.length() == 0) {
      Label emptyLabel = new Label("No history yet.", MyGdxGame.skin);
      emptyLabel.setColor(Color.GRAY);
      inner.add(emptyLabel).row();
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
    }

    ScrollPane scroll = new ScrollPane(inner, MyGdxGame.skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    scroll.layout();
    scroll.setScrollPercentY(1f); // start scrolled to the bottom (most recent)
    outer.add(scroll).expandX().fillX().expandY().fillY().padBottom(8f).row();

    TextButton backBtn = new TextButton("Back", MyGdxGame.skin);
    backBtn.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        buildMenuOverlay();
      }
    });
      outer.add(backBtn).width(300).height(60).padTop(8).row();

    overlayStage.addActor(outer);
  }

  private void addMenuButtonToOverlay() {
    TextButton menuBtn = new TextButton("Menu", MyGdxGame.skin);
    menuBtn.setSize(menuBtn.getWidth() * 1.5f, menuBtn.getHeight() * 1.5f);
    menuBtn.setPosition(MyGdxGame.WIDTH - menuBtn.getWidth(),
        MyGdxGame.HEIGHT - menuBtn.getHeight());
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
    overlayStage.clear();
    addMenuButtonToOverlay();
    // render() will set the correct input processor next frame
  }

  // ── Tutorial overlay ────────────────────────────────────────────────────────
  private static final String[] TUTORIAL_TITLES = {
    "Welcome to Baisch!",
    "Your Hand Cards",
    "Plundering",
    "Defense Cards",
    "Attacking",
    "Goal of the Game",
    "Tutorial Complete!"
  };
  private static final String[] TUTORIAL_TEXTS = {
    "This tutorial will teach you the core mechanics.\n\n"
      + "You are playing against a bot opponent.\n"
      + "The board shows your cards at the bottom, harvest decks in the center, "
      + "and your king card with shield slots on the left.",
    "The cards at the bottom of the screen are your hand cards.\n\n"
      + "Tap a card to select it (it will highlight).\n"
      + "You use hand cards to plunder harvest decks, attack enemies, "
      + "or place them as defense shields.",
    "To plunder, select one or more hand cards, then tap a harvest deck "
      + "(the tilted card stacks in the center).\n\n"
      + "Your attack strength must exceed the top card's defense. "
      + "If you succeed, you take all cards from that deck!",
    "To protect your king, place defense cards in your 3 shield slots.\n\n"
      + "Tap a hand card to select it, then tap an empty shield slot "
      + "(the dotted outlines near your king).\n"
      + "Each slot holds up to 2 stacked cards.",
    "To attack another player, select hand cards and tap one of their defense slots.\n\n"
      + "If your attack strength exceeds the defense card, you destroy it. "
      + "Once all 3 shields are gone, their king is exposed!",
    "The goal is to be the last player with a covered king card.\n\n"
      + "If your king gets exposed and attacked, you are eliminated. "
      + "Use your turns wisely: plunder for cards, build defenses, and attack enemies!",
    "You now know the basics!\n\n"
      + "Feel free to keep playing this tutorial game, "
      + "or press the button below to return to the main menu."
  };

  private void buildTutorialOverlay() {
    if (tutorialStep < 0 || tutorialStep >= TUTORIAL_TITLES.length) return;

    // Semi-transparent backdrop
    Image bg = new Image(MyGdxGame.skin, "white");
    bg.setFillParent(true);
    bg.setColor(0f, 0f, 0f, 0.75f);
    overlayStage.addActor(bg);

    Table outer = new Table();
    outer.setFillParent(true);
    outer.center();

    // Step counter
    Label stepLabel = new Label("Step " + (tutorialStep + 1) + " / " + TUTORIAL_TITLES.length,
        MyGdxGame.skin);
    stepLabel.setColor(1f, 1f, 1f, 0.5f);
    outer.add(stepLabel).padBottom(6).row();

    // Title
    Label titleLabel = new Label(TUTORIAL_TITLES[tutorialStep], MyGdxGame.skin);
    titleLabel.setColor(Color.GOLD);
    outer.add(titleLabel).padBottom(14).row();

    // Body text
    Label bodyLabel = new Label(TUTORIAL_TEXTS[tutorialStep], MyGdxGame.skin);
    bodyLabel.setWrap(true);
    outer.add(bodyLabel).width(380f).padBottom(20).row();

    // Buttons
    final boolean isLastStep = (tutorialStep == TUTORIAL_TITLES.length - 1);
    if (isLastStep) {
      TextButton exitBtn = new TextButton("Back to Menu", MyGdxGame.skin);
      exitBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          tutorialStep = -1;
          emitGiveUp();
        }
      });
      outer.add(exitBtn).width(300).height(50).padBottom(10).row();

      TextButton keepBtn = new TextButton("Keep Playing", MyGdxGame.skin);
      keepBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          tutorialStep = -1;
          overlayStage.clear();
          addMenuButtonToOverlay();
          gameState.setUpdateState(true);
        }
      });
      outer.add(keepBtn).width(300).height(50).row();
    } else {
      TextButton nextBtn = new TextButton("Next", MyGdxGame.skin);
      nextBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          tutorialStep++;
          overlayStage.clear();
          addMenuButtonToOverlay();
          buildTutorialOverlay();
        }
      });
      outer.add(nextBtn).width(300).height(50).padBottom(10).row();

      TextButton skipBtn = new TextButton("Skip Tutorial", MyGdxGame.skin);
      skipBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          tutorialStep = -1;
          overlayStage.clear();
          addMenuButtonToOverlay();
          gameState.setUpdateState(true);
        }
      });
      outer.add(skipBtn).width(300).height(50).row();
    }

    overlayStage.addActor(outer);
  }

  private void emitGiveUp() {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("playerIndex", playerIndex);
      socket.emit("giveUp", data);
    } catch (JSONException e) { e.printStackTrace(); }
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

  private void emitTakeDefCard(int positionId) {
    if (socket == null) return;
    try {
      JSONObject payload = new JSONObject();
      payload.put("playerIdx", playerIndex);
      payload.put("positionId", positionId);
      socket.emit("takeDefCard", payload);
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

        // Restore coup-swap auto-select: keep old king selected in hand after a non-warlord swap
        if (p == currentPlayer) {
          int pendingId = currentPlayer.getPlayerTurn().getCoupSwapPendingCardId();
          if (pendingId != -1) {
            boolean found = false;
            for (Card hc : p.getHandCards()) {
              if (hc.getCardId() == pendingId) {
                hc.setSelected(true);
                p.setSelectedSymbol(hc.getSymbol());
                found = true;
                break;
              }
            }
            if (!found) {
              // Card no longer in hand (consumed or lost) — clear the pending flag
              currentPlayer.getPlayerTurn().setCoupSwapPendingCardId(-1);
            }
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
          if (bc != null && bc.getCardId() == e.getValue()[0]) bc.addBoosted(e.getValue()[1]);
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
          if (bc != null && bc.getCardId() == e.getValue()[0]) bc.addBoosted(e.getValue()[1]);
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

        // Apply out flag
        p.setOut(pj.optBoolean("isOut", false));

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

        // Sync prey cards (captured this turn, locked until turn ends)
        ArrayList<Integer> newPreyIds = new ArrayList<Integer>();
        JSONArray preyJson = pj.optJSONArray("preyCards");
        if (preyJson != null) {
          for (int pr = 0; pr < preyJson.length(); pr++) newPreyIds.add(preyJson.getInt(pr));
        }
        p.getPlayerTurn().setPreyCardIds(newPreyIds);
        p.getPlayerTurn().setAttackCounter(pj.optInt("attackCount", 0));

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

      // Sync plunder preview — restore overlay for attacker on reconnect, watcher overlay for others
      JSONObject serverPendingPlunder = state.optJSONObject("pendingPlunder");
      if (serverPendingPlunder != null
          && serverPendingPlunder.optInt("attackerIdx", -1) == playerIndex
          && !currentPlayer.getPlayerTurn().isPlunderPending()) {
        // Restore the plunder confirmation overlay so it reappears after a page refresh
        PlayerTurn rpt = currentPlayer.getPlayerTurn();
        rpt.setPlunderPending(true);
        rpt.setPendingPickingDeckIndex(serverPendingPlunder.optInt("deckIndex", 0));
        rpt.setPlunderSuccess(serverPendingPlunder.optBoolean("success", false));
        rpt.setKingUsed(serverPendingPlunder.optBoolean("kingUsed", false));
        rpt.setPendingPlunderAttackSum(serverPendingPlunder.optInt("attackSum", 0));
        rpt.setPendingPlunderDefStrength(serverPendingPlunder.optInt("defStrength", 0));
        ArrayList<Card> rptAtkCards = new ArrayList<Card>();
        JSONArray rptAtkIds = serverPendingPlunder.optJSONArray("attackCardIds");
        if (rptAtkIds != null) {
          for (int rai = 0; rai < rptAtkIds.length(); rai++) rptAtkCards.add(Card.fromCardId(rptAtkIds.getInt(rai)));
        }
        rpt.setPendingAttackCards(rptAtkCards);
        ArrayList<Card> rptOwnDefCards = new ArrayList<Card>();
        JSONArray rptOwnDefIds = serverPendingPlunder.optJSONArray("ownDefCardIds");
        if (rptOwnDefIds != null) {
          for (int rai = 0; rai < rptOwnDefIds.length(); rai++) rptOwnDefCards.add(Card.fromCardId(rptOwnDefIds.getInt(rai)));
        }
        rpt.setPendingAttackOwnDefCards(rptOwnDefCards);
        pendingPlunderBroadcast = null;
      } else if (serverPendingPlunder != null && serverPendingPlunder.optInt("attackerIdx", -1) != playerIndex) {
        pendingPlunderBroadcast = serverPendingPlunder;
      } else {
        pendingPlunderBroadcast = null;
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
      // Spectator: only overlay/menu input
      Gdx.input.setInputProcessor(overlayStage);
    }

    // check if gameState has changed
    if (gameState.getUpdateState()) {
      gameState.setUpdateState(false);
      show();
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
    // Normal: soft teal; defense-selected: bright green highlight (both fully opaque)
    handBck.setColor(anyOwnDefSelected ? 0.3f : 0.78f, anyOwnDefSelected ? 0.9f : 0.93f,
        anyOwnDefSelected ? 0.3f : 0.87f, 1.0f);

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
    Array<EventListener> listeners = actor.getListeners();
    for (EventListener listener : listeners) {
      actor.removeListener(listener);
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
    dispose();

  }

  @Override
  public void dispose() {
    gameStage.dispose();
    handStage.dispose();

  }

}
