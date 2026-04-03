
package com.mygdx.game;
import org.json.JSONObject;
import org.json.JSONArray;

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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.PickingDeck;
import java.util.Iterator;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.BatteryTower;
import com.mygdx.game.heroes.Major;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.listeners.EnemyDefCardListener;
import com.mygdx.game.listeners.EnemyHandCardListener;
import com.mygdx.game.listeners.EnemyKingCardListener;
import com.mygdx.game.listeners.FinishTurnButtonListener;
import com.mygdx.game.listeners.HandImageListener;
import com.mygdx.game.listeners.KeepCardButtonListener;
import com.mygdx.game.listeners.MercenaryImageListener;
import com.mygdx.game.listeners.OwnDefCardListener;
import com.mygdx.game.listeners.OwnHandCardListener;
import com.mygdx.game.listeners.OwnHeroListener;
import com.mygdx.game.listeners.OwnKingCardListener;
import com.mygdx.game.listeners.OwnPlaceholderListener;
import com.mygdx.game.listeners.SabotagedImageListener;
import com.mygdx.game.listeners.TradeCardButtonListener;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;

//public class GameScreen extends AbstractScreen {
public class GameScreen extends ScreenAdapter {

  private GameState gameState;

  // screen objects
  InputMultiplexer inMulti;
  InputProcessor inProTop;
  InputProcessor inProBottom;
  private FitViewport fitVPGame;
  private FitViewport fitVPHand;
  private Stage gameStage;
  private Stage handStage;
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

  // handStage
  private OwnHandCardListener ownHandCardListener;
  private OwnHeroListener ownHeroListener;
  private KeepCardButtonListener keepCardButtonListener;
  private TradeCardButtonListener tradeCardButtonListener;
  private FinishTurnButtonListener finishTurnButtonListener;
  private HandImageListener handImageListener;

  private int playerIndex;
  private JSONObject centralizedState;
  private Socket socket;
  // Battery Tower: stored when this local player is the defender and must allow/deny
  private JSONObject pendingBatteryDefCheck = null;
  // Battery Tower: card IDs revealed to the defender after they allow or deny
  private org.json.JSONArray pendingBatteryResultCards = null;
  private JSONArray activityLog = new JSONArray();
  private boolean logExpanded = false;

  // Textures cached once to avoid leaking a new Texture on every show() call
  private Texture texMercenary;
  private Texture texSabotaged;
  private Texture texHand;
  private Texture texHearts;
  private Texture texDiamonds;
  private Texture texClubs;
  private Texture texSpades;
  private Texture texSomeSymbol;
  private Texture texSword;
  private Texture texCrone;
  private Texture texShieldCheck;
  private Texture texArrowDownShield;

  // New constructor for centralized state
  public GameScreen(Game game, JSONObject centralizedState, int playerIndex, Socket socket) {
    this(game, centralizedState, playerIndex, socket, "None");
  }

  private String startingHero = "None";

  public GameScreen(Game game, JSONObject centralizedState, int playerIndex, Socket socket, String startingHero) {
    this.startingHero = startingHero;
    this.socket = socket;
    this.playerIndex = playerIndex;
    this.centralizedState = centralizedState;

    // Build all game state from the server-provided authoritative state
    gameState = new GameState(centralizedState);
    gameState.setSocket(socket);
    players = gameState.getPlayers();
    currentPlayer = players.get(playerIndex);

    // Apply testing starting hero if one was selected in the menu
    if (startingHero != null && !startingHero.equals("None")) {
      gameState.applyHeroAcquired(playerIndex, startingHero);
    }

    // Single stateUpdate listener — replaces all specific sync events
    socket.on("stateUpdate", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
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
    final Socket theSocket = socket;
    final String theStartingHero = startingHero;
    socket.on("gameState", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int newPlayerIndex = data.getInt("playerIndex");
              org.json.JSONObject newState = data.getJSONObject("gameState");
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
    socket.on("heroAcquired", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
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

    // Handle incoming mercenary defense boost from another client
    // Spy flip relay: another player used spy to reveal one of our defense cards
    socket.on("spyFlip", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            try {
              int tIdx = data.getInt("targetPlayerIdx");
              int slot = data.getInt("slot");
              int level = data.getInt("level");
              Player p = gameState.getPlayers().get(tIdx);
              Map<Integer, Card> cards = (level == 0) ? p.getDefCards() : p.getTopDefCards();
              Card c = cards.get(slot);
              if (c != null) c.setCovered(false);
              gameState.setUpdateState(true);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });

    socket.on("batteryDefenseCheck", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
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
                  if (me.getHeroes().get(i).getHeroName() == "Battery Tower") {
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
                    org.json.JSONObject allow = new org.json.JSONObject();
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

    socket.on("batteryAllowAttack", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
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
                gameState.setUpdateState(true);
              }
            } catch (JSONException e) { e.printStackTrace(); }
          }
        });
      }
    });

    socket.on("batteryDenyAttack", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
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

    socket.on("mercDefBoost", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        final org.json.JSONObject data = (org.json.JSONObject) args[0];
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

    // Initialize stages
    gameStage = new Stage();
    fitVPGame = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getWidth());
    gameStage.setViewport(fitVPGame);

    handStage = new Stage();
    fitVPHand = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - Gdx.graphics.getWidth());
    handStage.setViewport(fitVPHand);

    inMulti = new InputMultiplexer();
    inMulti.addProcessor(gameStage);
    inMulti.addProcessor(handStage);
    Gdx.input.setInputProcessor(inMulti);

    gameBck = new Image(MyGdxGame.skin, "white");
    gameBck.setFillParent(true);
    gameBck.setColor(0.85f, 0.73f, 0.55f, 1);
    gameStage.addActor(gameBck);

    handBck = new Image(MyGdxGame.skin, "white");
    handBck.setFillParent(true);
    handBck.setColor(1f, 1f, 1f, 0.5f);
    handStage.addActor(handBck);

    texMercenary  = new Texture(Gdx.files.internal("data/skins/whitepawn.png"));
    texSabotaged  = new Texture(Gdx.files.internal("data/skins/sabotaged.png"));
    texHand       = new Texture(Gdx.files.internal("data/skins/hand.png"));
    texHearts     = new Texture(Gdx.files.internal("data/skins/hearts.png"));
    texDiamonds   = new Texture(Gdx.files.internal("data/skins/diamonds.png"));
    texClubs      = new Texture(Gdx.files.internal("data/skins/clubs.png"));
    texSpades     = new Texture(Gdx.files.internal("data/skins/spades.png"));
    texSomeSymbol = new Texture(Gdx.files.internal("data/skins/someSymbol.png"));
    texSword           = new Texture(Gdx.files.internal("data/skins/sword.png"));
    texCrone           = new Texture(Gdx.files.internal("data/skins/crone.png"));
    texShieldCheck     = new Texture(Gdx.files.internal("data/skins/shield-check-f.png"));
    texArrowDownShield = new Texture(Gdx.files.internal("data/skins/arrow-down-shield.png"));
  }

  @Override
  public void show() {
    Gdx.app.log("Java Heap", String.valueOf(Gdx.app.getJavaHeap()));
    Gdx.app.log("Native Heap", String.valueOf(Gdx.app.getNativeHeap()));

    players = gameState.getPlayers();
    // currentPlayer stays as this client's own player (set in constructor from playerIndex)

    gameStage.clear();
    handStage.clear();

    gameStage.addActor(gameBck);
    handStage.addActor(handBck);

    showGameStage(players, currentPlayer);
    showHandStage(players, currentPlayer);
  }

  public void showGameStage(ArrayList<Player> players, Player currentPlayer) {
    Card infoCard = new Card();
    float cardW = infoCard.getDefWidth();
    float cardH = infoCard.getDefHeight();
    // Collect hand count labels to be added LAST (on top of all card actors)
    ArrayList<Label> handCountLabels = new ArrayList<Label>();

    // draw round number
    roundCounter = new Label("Round " + gameState.getRoundNumber(), MyGdxGame.skin);
    roundCounter.setColor(0f, 0f, 0f, 1.0f);
    roundCounter.setPosition(0, Gdx.graphics.getWidth() - roundCounter.getHeight());
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
      System.out.println("Player " + players.get(i).getPlayerName() + " hand = " + players.get(i).getHandCards().size());
      System.out.println("Player " + players.get(i).getPlayerName() + " def = " + players.get(i).getDefCards().size());

      // display dice
      Dice dice = players.get(i).getDice();
      dice.setMapPosition(i);
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
        switch (i) {
        case 0: // king right visual edge = (WIDTH+W)/2; deck visual left = king_right + W/2
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
              gameState.getPlayers());
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
      kingCard.setMapPosition(i, 0, 0);
      // make own covered cards visible
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
          }
        } else {
          defCard = new Card();
          defCard.removeAllListeners();
          if (players.get(i) == currentPlayer) {
            ownPlaceholderListener = new OwnPlaceholderListener(defCard, gameState.getCurrentPlayer(), gameState);
            defCard.addListener(ownPlaceholderListener);
          }
        }

        if (defCard.isRemoved()) {
          players.get(i).getDefCards().remove(j);
          System.out.println("Def card removed!");
        }

        defCard.setMapPosition(i, j, 0);
        if (players.get(i) == currentPlayer) {
          defCard.setActive(true);
        } else {
          defCard.setActive(false);
        }
        gameStage.addActor(defCard);

        if (defCard.isSabotaged()) {
          TextureRegion sabotagedRegion = new TextureRegion(texSabotaged, 0, 0, 64, 64);
          Image sabotagedImage = new Image(sabotagedRegion);
          sabotagedImage.setBounds(sabotagedImage.getX(), sabotagedImage.getY(),
              sabotagedImage.getWidth() / 5f, sabotagedImage.getHeight() / 5f);
          sabotagedImage.setPosition(defCard.getX(), defCard.getY());
          sabotagedImage.setX(sabotagedImage.getX() + defCard.getWidth() / 2f - sabotagedImage.getWidth() / 2f);
          sabotagedImage.setY(sabotagedImage.getY() + defCard.getHeight() / 2f - sabotagedImage.getHeight() / 2f);
          removeAllListeners(sabotagedImage);
          sabotagedImageListener = new SabotagedImageListener(gameState, defCard, currentPlayer);
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
          }
          topDefCard.setMapPosition(i, j, 1);
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
      switch (i) {
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

        switch (i) {
        case 0:
          playerHeroes.get(j).setPosition(
              playerHeroes.get(j).getX() - playerLabel.getWidth() - j * playerHeroes.get(j).getWidth() / 3,
              playerHeroes.get(j).getY() - playerHeroes.get(j).getHeight() / 4);
          break;
        case 1:
          playerHeroes.get(j).setPosition(playerHeroes.get(j).getX(),
              playerHeroes.get(j).getY() + j * playerHeroes.get(j).getHeight() / 3);
          break;
        case 2:
          playerHeroes.get(j).setPosition(
              playerHeroes.get(j).getX() + playerLabel.getWidth() + j * playerHeroes.get(j).getWidth() / 3,
              MyGdxGame.WIDTH - playerHeroes.get(j).getHeight());
          break;
        case 3:
          playerHeroes.get(j).setPosition(playerHeroes.get(j).getX(),
              playerHeroes.get(j).getY() - 2 * playerLabel.getHeight() - j * playerHeroes.get(j).getHeight() / 3);
          break;
        default:
          break;
        }

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
          pt.setPlunderPending(false);
          if (pt.isKingUsed()) pt.setKingUsedThisTurn(true);
          // Broadcast to server (server applies + broadcasts stateUpdate to all)
          try {
            org.json.JSONObject emitData = new org.json.JSONObject();
            emitData.put("attackerIdx", gameState.getCurrentPlayerIndex());
            emitData.put("deckIndex", deckIdx);
            emitData.put("success", plunderSuccess);
            emitData.put("kingUsed", pt.isKingUsed());
            org.json.JSONArray atkIdArr = new org.json.JSONArray();
            for (Card c : pt.getPendingAttackCards()) atkIdArr.put(c.getCardId());
            emitData.put("attackCardIds", atkIdArr);
            socket.emit("plunderResolved", emitData);
          } catch (JSONException e) {
            e.printStackTrace();
          }
          pt.getPendingAttackCards().clear();
          gameState.setUpdateState(true);
        }
      });

      // Result label on top of the tint
      Label plunderResultLabel = new Label(
          plunderSuccess ? "SUCCESS!  Tap anywhere to claim the cards."
                        : "FAILED.  Tap anywhere to continue.",
          MyGdxGame.skin);
      plunderResultLabel.setColor(plunderSuccess ? Color.GREEN : Color.RED);
      plunderResultLabel.setPosition(
          MyGdxGame.WIDTH / 2f - plunderResultLabel.getPrefWidth() / 2f,
          MyGdxGame.WIDTH / 2f);

      gameStage.addActor(overlay);
      gameStage.addActor(plunderResultLabel);
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
              org.json.JSONObject emitData = new org.json.JSONObject();
              emitData.put("attackerIdx", gameState.getCurrentPlayerIndex());
              emitData.put("defenderIdx", apt.getAttackTargetPlayerIdx());
              emitData.put("success", atkSuccess);
              emitData.put("kingUsed", apt.isKingUsed());
              org.json.JSONArray atkIds = new org.json.JSONArray();
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
              org.json.JSONObject emitData = new org.json.JSONObject();
              emitData.put("attackerIdx", gameState.getCurrentPlayerIndex());
              emitData.put("targetPlayerIdx", apt.getAttackTargetPlayerIdx());
              emitData.put("positionId", apt.getAttackTargetPositionId());
              emitData.put("level", apt.getAttackTargetLevel());
              emitData.put("success", atkSuccess);
              emitData.put("kingUsed", apt.isKingUsed());
              org.json.JSONArray atkIds = new org.json.JSONArray();
              for (Card c : apt.getPendingAttackCards()) { atkIds.put(c.getCardId()); }
              emitData.put("attackCardIds", atkIds);
              socket.emit("defAttackResolved", emitData);
            } catch (JSONException e) {
              e.printStackTrace();
            }
            apt.getPendingAttackDefCards().clear();
          }
          apt.getPendingAttackCards().clear();
          apt.setAttackPending(false);
          apt.setAttackTargetIsKing(false);
          if (apt.isKingUsed()) apt.setKingUsedThisTurn(true);
          // Clear hand card attack boost visuals after attack resolves
          for (Card c : atkPlayer.getHandCards()) {
            c.setSelected(false);
            while (c.getBoosted() > 0) c.addBoosted(-1);
          }
          gameState.setUpdateState(true);
        }
      });

      String normalText = targetIsKing
          ? (atkSuccess ? "KING DEFEATED!  Tap to claim." : "KING ATTACK FAILED.  Tap to continue.")
          : (atkSuccess ? "ATTACK SUCCESS!  Tap to claim the defense card." : "ATTACK FAILED.  Tap to continue.");
      String resultText = batteryWaiting ? "Waiting for defender..." : normalText;
      Label atkResultLabel = new Label(resultText, MyGdxGame.skin);
      atkResultLabel.setColor(batteryWaiting ? Color.YELLOW : (atkSuccess ? Color.GREEN : Color.RED));
      atkResultLabel.setPosition(
          MyGdxGame.WIDTH / 2f - atkResultLabel.getPrefWidth() / 2f,
          MyGdxGame.WIDTH / 2f);

      gameStage.addActor(atkOverlay);
      gameStage.addActor(atkResultLabel);
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
            org.json.JSONObject resp = new org.json.JSONObject();
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
            org.json.JSONObject resp = new org.json.JSONObject();
            resp.put("attackerIdx", btCheck.getInt("attackerIdx"));
            resp.put("targetPlayerIdx", btCheck.getInt("targetPlayerIdx"));
            resp.put("positionId", btCheck.getInt("positionId"));
            resp.put("isKing", btCheck.optBoolean("isKing", false));
            org.json.JSONArray atkExpIds = btCheck.optJSONArray("attackCardIds");
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
      final org.json.JSONArray resultCards = pendingBatteryResultCards;
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
            Hero consumed = gameState.getHeroesSquare().consumeHeroByName(choice.getHeroName());
            if (consumed != null) completeHeroAcquisition(consumed);
          }
        });
        gameStage.addActor(heroBtn);
      }
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

    // Activity log panel — added LAST so it renders on top of everything.
    // Dark box, top-right corner; 50% scale by default, 100% on mouse hover.
    if (activityLog.length() > 0) {
      try {
        final float padding = 6f;
        final float lineH = roundCounter.getPrefHeight() + 4f;
        int firstEntry = Math.max(0, activityLog.length() - 5);
        int count = activityLog.length() - firstEntry;

        float maxLabelW = 0f;
        Label[] entryLabels = new Label[count];
        Color[] entryColors = new Color[count];
        for (int li = 0; li < count; li++) {
          JSONObject entry = activityLog.getJSONObject(firstEntry + li);
          String entryText = entry.optString("text", "");
          boolean entryNeutral = entry.optBoolean("neutral", false);
          boolean entrySuccess = entry.optBoolean("success", true);
          Label lbl = new Label(entryText, MyGdxGame.skin);
          lbl.pack();
          maxLabelW = Math.max(maxLabelW, lbl.getWidth());
          Color lc = entryNeutral
              ? new Color(0.85f, 0.85f, 0.85f, 1f)
              : (entrySuccess ? new Color(0.3f, 0.95f, 0.3f, 1f) : new Color(0.95f, 0.3f, 0.25f, 1f));
          entryLabels[li] = lbl;
          entryColors[li] = lc;
        }

        float fullW = maxLabelW + 2f * padding;
        float fullH = count * lineH + 2f * padding;

        final Group logGroup = new Group();
        logGroup.setTransform(true);
        logGroup.setSize(fullW, fullH);

        Image logBg = new Image(MyGdxGame.skin, "white");
        logBg.setColor(0.12f, 0.12f, 0.18f, 0.90f);
        logBg.setSize(fullW, fullH);
        logGroup.addActor(logBg);

        float ly = fullH - padding - lineH;
        for (int li = 0; li < count; li++) {
          Label lbl = entryLabels[li];
          lbl.setColor(entryColors[li]);
          lbl.setPosition(padding, ly + (lineH - lbl.getHeight()) * 0.5f);
          logGroup.addActor(lbl);
          ly -= lineH;
        }

        logGroup.setOrigin(fullW, fullH);
        logGroup.setPosition(MyGdxGame.WIDTH - fullW, MyGdxGame.WIDTH - fullH);
        logGroup.setScale(logExpanded ? 1f : 0.5f);

        logGroup.addListener(new ClickListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer != -1) return;
            logExpanded = true;
            logGroup.setScale(1f);
          }
          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (pointer != -1) return;
            logExpanded = false;
            logGroup.setScale(0.5f);
          }
        });

        gameStage.addActor(logGroup);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  public void showHandStage(ArrayList<Player> players, Player currentPlayer) {
    // Set up own hand card listeners for the current turn player
    for (int i = 0; i < players.size(); i++) {
      ArrayList<Card> handCards = players.get(i).getHandCards();
      for (int j = 0; j < handCards.size(); j++) {
        if (players.get(i) == gameState.getCurrentPlayer()) {
          final Card handCard = handCards.get(j);
          handCard.removeAllListeners();
          // If battery tower denied this turn, lock only the specific cards used in the denied attack
          boolean isDeniedCard = gameState.getCurrentPlayer().getPlayerTurn().getBatteryDeniedAttackCardIds().contains(handCard.getCardId());
          if (!isDeniedCard) {
            ownHandCardListener = new OwnHandCardListener(handCard, gameState.getCurrentPlayer(), gameState.getCardDeck(),
                gameState.getCemeteryDeck(), gameState);
            handCard.addListener(ownHandCardListener);
          }
          handCards.get(j).setActive(false);
        }
      }
    }

    // Draw heroes and hand cards only for the current (own) player
    final ArrayList<Card> handCards = currentPlayer.getHandCards();
    ArrayList<Hero> playerHeroes = currentPlayer.getHeroes();
    currentPlayer.sortHandCards();
    ArrayList<Integer> deniedCardIds = currentPlayer.getPlayerTurn().getBatteryDeniedAttackCardIds();

    for (int j = 0; j < handCards.size(); j++) {
      Card handcard = handCards.get(j);
      handcard.setCovered(false);
      handcard.setActive(true);
      if (deniedCardIds.contains(handcard.getCardId())) handcard.setColor(0.4f, 0.4f, 0.4f, 1f);
      else handcard.setColor(Color.WHITE);
      handcard.setRotation(0);
      handcard.setWidth(handcard.getDefWidth() * 2);
      handcard.setHeight(handcard.getDefHeight() * 2);
      if (j < 5) {
        handcard.setX(j * handcard.getWidth());
        handcard.setY(MyGdxGame.WIDTH / 2);
      } else {
        handcard.setX((j - 5) * handcard.getWidth());
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
        final Card tradeableCard = handCards.get(j);

        TextButton keepCardButton = new TextButton("Keep", MyGdxGame.skin);
        keepCardButton.setX(handCards.get(j).getX() + (handCards.get(j).getWidth() - keepCardButton.getWidth()) / 2f);
        keepCardButton.setY(handCards.get(j).getY() + (handCards.get(j).getHeight() + keepCardButton.getHeight()) / 2f);

        TextButton tradeCardButton = new TextButton("Trade", MyGdxGame.skin);
        tradeCardButton.setX(handCards.get(j).getX() + (handCards.get(j).getWidth() - tradeCardButton.getWidth()) / 2f);
        tradeCardButton
            .setY(handCards.get(j).getY() + (handCards.get(j).getHeight() - 3 * tradeCardButton.getHeight()) / 2f);

        removeAllListeners(keepCardButton);
        removeAllListeners(tradeCardButton);

        keepCardButtonListener = new KeepCardButtonListener(tradeableCard);
        keepCardButton.addListener(keepCardButtonListener);

        tradeCardButtonListener = new TradeCardButtonListener();
        tradeCardButton.addListener(tradeCardButtonListener);

        handStage.addActor(keepCardButton);
        handStage.addActor(tradeCardButton);
      }
    }

    // Display all heroes of current player
    for (int j = 0; j < playerHeroes.size(); j++) {
      final Hero hero = playerHeroes.get(j);
      hero.setHand(true);
      hero.setPosition(j * hero.getWidth(), 0);

      if (hero.getHeroName() == "Priest") {
        if (gameState.getCurrentPlayer().getPlayerTurn().getAttackingSymbol()[0] != "none") {
          hero.setSelectable(true);
        } else {
          hero.setSelectable(false);
        }
      }

      hero.removeAllListeners();
      ownHeroListener = new OwnHeroListener(hero, gameState.getCurrentPlayer(), gameState);
      hero.addListener(ownHeroListener);

      Label heroLabel = new Label(hero.getHeroID(), MyGdxGame.skin);
      heroLabel.setPosition(j * hero.getWidth() + (hero.getWidth() - heroLabel.getWidth()) / 2, hero.getHeight());

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

      if (hero.getHeroName() == "Major") {
        Major major = (Major) hero;
        String mobCount = major.getMobilizations() + "/3";
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
    }

    // Turn info and button
    finishTurnButton = new TextButton("Finish turn", MyGdxGame.skin);
    finishTurnButton.setPosition(Gdx.graphics.getWidth() - finishTurnButton.getWidth(), 0);
    myPlayerLabel = new Label(currentPlayer.getPlayerName(), MyGdxGame.skin);
    myPlayerLabel.setPosition(Gdx.graphics.getWidth() - myPlayerLabel.getWidth(), finishTurnButton.getHeight());

    // Turn indicator
    boolean isMyTurn = (gameState.getCurrentPlayer() == currentPlayer);

    // "Sacrifice Joker" button — only on your turn, bottom-left of hand stage
    if (isMyTurn && !currentPlayer.getPlayerTurn().isHeroSelectionPending()) {
      Card jokerInHand = null;
      for (Card hc : handCards) {
        if ("joker".equals(hc.getSymbol())) { jokerInHand = hc; break; }
      }
      if (jokerInHand != null) {
        final Card theJoker = jokerInHand;
        TextButton heroBtn = new TextButton("Sacrifice Joker: Get Hero", MyGdxGame.skin);
        heroBtn.setPosition(0, 0);
        heroBtn.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            performJokerSacrifice(theJoker);
          }
        });
        handStage.addActor(heroBtn);
      }
    }

    // Only enable finish-turn button when it is this player's turn
    finishTurnButton.setVisible(isMyTurn);

    finishTurnButtonListener = new FinishTurnButtonListener(gameState, socket);
    finishTurnButton.addListener(finishTurnButtonListener);

    handStage.addActor(myPlayerLabel);

    // Add attacking symbol
    String attackingSymbol = currentPlayer.getPlayerTurn().getAttackingSymbol()[0];
    Texture symbolTexture;
    TextureRegion symbolRegion;
    if (attackingSymbol == "hearts") {
      symbolTexture = texHearts;
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else if (attackingSymbol == "diamonds") {
      symbolTexture = texDiamonds;
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else if (attackingSymbol == "clubs") {
      symbolTexture = texClubs;
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else if (attackingSymbol == "spades") {
      symbolTexture = texSpades;
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else {
      symbolTexture = texSomeSymbol;
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 342, 512);
    }

    Image symbolImage = new Image(symbolRegion);
    symbolImage.setBounds(symbolImage.getX(), symbolImage.getY(), symbolImage.getWidth() / 10f,
        symbolImage.getHeight() / 10f);
    symbolImage.setPosition(Gdx.graphics.getWidth() - symbolImage.getWidth(),
        finishTurnButton.getHeight() + myPlayerLabel.getHeight());
    handStage.addActor(symbolImage);

    String attackingSymbolExt = currentPlayer.getPlayerTurn().getAttackingSymbol()[1];
    if (attackingSymbolExt != "none") {
      Texture symbolTextureExt = texSomeSymbol;
      TextureRegion symbolRegionExt = new TextureRegion(symbolTexture, 0, 0, 342, 512);
      if (attackingSymbolExt == "hearts") {
        symbolTextureExt = texHearts;
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      } else if (attackingSymbolExt == "diamonds") {
        symbolTextureExt = texDiamonds;
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      } else if (attackingSymbolExt == "clubs") {
        symbolTextureExt = texClubs;
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      } else if (attackingSymbolExt == "spades") {
        symbolTextureExt = texSpades;
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      }

      Image symbolImageExt = new Image(symbolRegionExt);
      symbolImageExt.setBounds(symbolImageExt.getX(), symbolImageExt.getY(), symbolImageExt.getWidth() / 10f,
          symbolImageExt.getHeight() / 10f);
      symbolImageExt.setPosition(Gdx.graphics.getWidth() - 1.5f * symbolImage.getWidth(),
          finishTurnButton.getHeight() + myPlayerLabel.getHeight());
      handStage.addActor(symbolImageExt);
    }

    // Add hand image
    TextureRegion handRegion = new TextureRegion(texHand, 0, 0, 512, 512);
    Image handImage = new Image(handRegion);
    handImage.setBounds(handImage.getX(), handImage.getY(), handImage.getWidth() / 5f, handImage.getHeight() / 5f);
    handImage.setPosition(Gdx.graphics.getWidth() - (myPlayerLabel.getWidth() + handImage.getWidth()), 0);

    removeAllListeners(handImage);
    handImageListener = new HandImageListener(gameState, currentPlayer);
    handImage.addListener(handImageListener);

    handStage.addActor(handImage);

    // Shield indicators to the left of the hand image (stacked vertically):
    //   arrow-down-shield = take defense card available (bottom slot)
    //   shield-check-f    = put defense card available (top slot)
    if (isMyTurn) {
      PlayerTurn ptHand = currentPlayer.getPlayerTurn();
      float iconSize = handImage.getHeight() / 3f;
      float iconX = handImage.getX() - iconSize - 2f;
      float slot0Y = handImage.getY();              // bottom icon
      float slot1Y = handImage.getY() + iconSize;   // top icon
      Major majorHero = null;
      for (int mi = 0; mi < currentPlayer.getHeroes().size(); mi++) {
        if (currentPlayer.getHeroes().get(mi).getHeroName() == "Major") {
          majorHero = (Major) currentPlayer.getHeroes().get(mi);
          break;
        }
      }
      boolean showTakeShield = majorHero != null ? majorHero.getMobilizations() > 0 : ptHand.getTakeDefCard() > 0;
      boolean showPutShield  = majorHero != null ? majorHero.getMobilizations() > 0 : ptHand.getPutDefCard() > 0;
      if (showTakeShield) {
        Image arrowShieldImg = new Image(new TextureRegion(texArrowDownShield,
            0, 0, texArrowDownShield.getWidth(), texArrowDownShield.getHeight())) {
          @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
        };
        arrowShieldImg.setSize(iconSize, iconSize);
        arrowShieldImg.setPosition(iconX, slot0Y);
        arrowShieldImg.setColor(Color.GREEN);
        handStage.addActor(arrowShieldImg);
      }
      if (showPutShield) {
        Image shieldCheckImg = new Image(new TextureRegion(texShieldCheck,
            0, 0, texShieldCheck.getWidth(), texShieldCheck.getHeight())) {
          @Override public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) { return null; }
        };
        shieldCheckImg.setSize(iconSize, iconSize);
        shieldCheckImg.setPosition(iconX, slot1Y);
        shieldCheckImg.setColor(Color.GREEN);
        handStage.addActor(shieldCheckImg);
      }
    }

    handStage.addActor(finishTurnButton);
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
      }
      gameState.setCurrentPlayer(serverCurrentIdx);

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

        p.getHandCards().clear();
        JSONArray handJson = pj.getJSONArray("hand");
        for (int h = 0; h < handJson.length(); h++) {
          p.getHandCards().add(Card.fromCardId(handJson.getInt(h)));
        }

        // Save local def covered-state overrides (spy flips) before rebuilding
        Map<Integer, Boolean> savedDefCovered = new HashMap<Integer, Boolean>();
        for (Map.Entry<Integer, Card> e : p.getDefCards().entrySet()) {
          if (!e.getValue().isCovered()) savedDefCovered.put(e.getKey(), false);
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
        // Restore spy-flipped face-up state
        for (Map.Entry<Integer, Boolean> e : savedDefCovered.entrySet()) {
          Card dc = p.getDefCards().get(e.getKey());
          if (dc != null) dc.setCovered(false);
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

        // Apply king card covered state and out flag
        p.setOut(pj.optBoolean("isOut", false));
        if (p.getKingCard() != null) p.getKingCard().setCovered(pj.optBoolean("kingCovered", true));
      }

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

      // 7. Activity log
      JSONArray logJson = state.optJSONArray("log");
      if (logJson != null) activityLog = logJson;

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    // Block all input when it is not this client's turn,
    // EXCEPT when a Battery Tower intercept awaits the defender's decision
    if (gameState.getCurrentPlayer() == currentPlayer || pendingBatteryDefCheck != null || pendingBatteryResultCards != null) {
      Gdx.input.setInputProcessor(inMulti);
    } else {
      Gdx.input.setInputProcessor(null);
    }

    // check if gameState has changed
    if (gameState.getUpdateState()) {
      gameState.setUpdateState(false);
      show();
    }

    /* Upper division */
    Gdx.gl.glViewport(0, Gdx.graphics.getHeight() - Gdx.graphics.getWidth(), Gdx.graphics.getWidth(),
        Gdx.graphics.getWidth());
    gameStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getWidth(), true);
    gameStage.getViewport().setScreenBounds(0, Gdx.graphics.getHeight() - Gdx.graphics.getWidth(),
        Gdx.graphics.getWidth(), Gdx.graphics.getWidth());
    gameStage.getViewport().apply();
    gameStage.act(delta);
    gameStage.draw();

    /* Lower division */
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - Gdx.graphics.getWidth());
    handStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - Gdx.graphics.getWidth(), true);
    handStage.getViewport().setScreenBounds(0, 0, Gdx.graphics.getWidth(),
        Gdx.graphics.getHeight() - Gdx.graphics.getWidth());
    handStage.getViewport().apply();
    handStage.act(delta);
    handStage.draw();
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
      // Another joker drawn — free choice from ALL remaining heroes.
      triggerHeroChoice(hs.getAvailableAllHeroes());
    } else if (drawnIndex == 1) {
      // Ace: red suits (hearts/diamonds) → white heroes; black → black heroes.
      boolean isRed = "hearts".equals(drawnSym) || "diamonds".equals(drawnSym);
      java.util.ArrayList<Hero> choices = isRed
          ? hs.getAvailableWhiteHeroes()
          : hs.getAvailableBlackHeroes();
      if (choices.isEmpty()) choices = hs.getAvailableAllHeroes(); // fallback
      triggerHeroChoice(choices);
    } else {
      // Direct hero assignment by card index (2-13).
      Hero hero = hs.getHeroByCardIndex(drawnIndex);
      if (hero == null) {
        // That hero is already taken — let the player choose from remaining.
        java.util.ArrayList<Hero> choices = hs.getAvailableAllHeroes();
        if (!choices.isEmpty()) triggerHeroChoice(choices);
        else gameState.setUpdateState(true);
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
