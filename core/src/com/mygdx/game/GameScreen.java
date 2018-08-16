package com.mygdx.game;

import java.util.ArrayList;
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
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.heroes.Hero;
import com.mygdx.heroes.Mercenaries;
import com.mygdx.listeners.EnemyDefCardListener;
import com.mygdx.listeners.EnemyHandCardListener;
import com.mygdx.listeners.EnemyKingCardListener;
import com.mygdx.listeners.FinishTurnButtonListener;
import com.mygdx.listeners.HandImageListener;
import com.mygdx.listeners.KeepCardButtonListener;
import com.mygdx.listeners.MercenaryImageListener;
import com.mygdx.listeners.OwnDefCardListener;
import com.mygdx.listeners.OwnHandCardListener;
import com.mygdx.listeners.OwnHeroListener;
import com.mygdx.listeners.OwnKingCardListener;
import com.mygdx.listeners.OwnPlaceholderListener;
import com.mygdx.listeners.SabotagedImageListener;
import com.mygdx.listeners.TradeCardButtonListener;

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

  public GameScreen(Game game) {
    // init game
    gameState = new GameState(4, 8);

    // create stages and input handlers
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

    // create stage backgrounds
    gameBck = new Image(MyGdxGame.skin, "white");
    gameBck.setFillParent(true);
    gameBck.setColor(0.85f, 0.73f, 0.55f, 1);
    gameStage.addActor(gameBck);

    handBck = new Image(MyGdxGame.skin, "white");
    handBck.setFillParent(true);
    handBck.setColor(1f, 1f, 1f, 0.5f);
    handStage.addActor(handBck);

  }

  @Override
  public void show() {
    System.out.println("show()");
    Gdx.app.log("Java Heap", String.valueOf(Gdx.app.getJavaHeap()));
    Gdx.app.log("Native Heap", String.valueOf(Gdx.app.getNativeHeap()));

    currentPlayer = gameState.getCurrentPlayer();
    players = gameState.getPlayers();

    gameStage.clear();
    handStage.clear();

    gameStage.addActor(gameBck);
    handStage.addActor(handBck);

    showGameStage(players, currentPlayer);
    showHandStage(players, currentPlayer);

  }

  public void showGameStage(ArrayList<Player> players, Player currentPlayer) {
    Card infoCard = new Card();

    // draw round number
    roundCounter = new Label("Round " + gameState.getRoundNumber(), MyGdxGame.skin);
    roundCounter.setColor(0f, 0f, 0f, 1.0f);
    roundCounter.setPosition(0, Gdx.graphics.getWidth() - roundCounter.getHeight());
    gameStage.addActor(roundCounter);

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
      pickingDecks.get(i).setX(MyGdxGame.WIDTH / 2 - pickingCards.get(0).getDefWidth() / 2
          + (2 * i - 1) * 0.8f * pickingCards.get(0).getDefWidth());
      pickingDecks.get(i).setY(MyGdxGame.WIDTH / 2 - pickingCards.get(0).getDefHeight() / 2
          + (2 * i - 1) * 0.8f * pickingCards.get(0).getDefWidth());
      pickingDecks.get(i).setWidth(pickingCards.get(0).getDefWidth());
      pickingDecks.get(i).setHeight(pickingCards.get(0).getDefHeight());
      pickingDecks.get(i).setRotation(45);
      gameStage.addActor(pickingDecks.get(i));
    }

    // draw game status of players
    for (int i = 0; i < players.size(); i++) {
      System.out
          .println("Player " + players.get(i).getPlayerName() + " hand = " + players.get(i).getHandCards().size());
      System.out.println("Player " + players.get(i).getPlayerName() + " def = " + players.get(i).getDefCards().size());

      // display dice
      Dice dice = players.get(i).getDice();
      dice.setMapPosition(i);
      gameStage.addActor(dice);

      // display hand cards
      ArrayList<Card> handCards = players.get(i).getHandCards();
      for (int j = 0; j < handCards.size(); j++) {
        final Card handCard = (Card) handCards.get(j);
        handCards.get(j).setCovered(true);
        handCards.get(j).setRotation(0);
        handCards.get(j).setActive(false);
        handCards.get(j).setSelected(false);
        handCards.get(j).setSize(handCards.get(j).getDefWidth(), handCards.get(j).getDefHeight());
        handCards.get(j).setPosition(dice.getX(), dice.getY());

        switch (i) {
        case 0:
          handCards.get(j).setPosition(handCards.get(j).getX() + 1.5f * dice.getWidth() + j * 5f,
              handCards.get(j).getY());
          break;
        case 1:
          handCards.get(j).setRotation(-90);
          handCards.get(j).setPosition(handCards.get(j).getX(),
              handCards.get(j).getY() - 2f * dice.getWidth() - j * 5f);
          break;
        case 2:
          handCards.get(j).setRotation(-180);
          handCards.get(j).setPosition(handCards.get(j).getX() - 2f * dice.getWidth() - j * 5f,
              handCards.get(j).getY() - dice.getWidth());
          break;
        case 3:
          handCards.get(j).setRotation(90);
          handCards.get(j).setPosition(handCards.get(j).getX() - dice.getWidth(),
              handCards.get(j).getY() + dice.getWidth() + j * 5f);
          break;
        default:
          break;
        }

        // add listener for priest functionality
        handCard.removeAllListeners();

        enemyHandCardListener = new EnemyHandCardListener(handCard, gameState.getCurrentPlayer(),
            gameState.getPlayers());
        handCard.addListener(enemyHandCardListener);

        gameStage.addActor(handCard);
      }

      // display king cards
      final Card kingCard = players.get(i).getKingCard();
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
            gameState.getPlayers());
        kingCard.addListener(enemyKingCardListener);
      } else {
        ownKingCardListener = new OwnKingCardListener(gameState, currentPlayer,
            gameState.getCurrentPlayer().getKingCard(), gameState.getCurrentPlayer().getDefCards(),
            gameState.getCurrentPlayer().getTopDefCards(), gameState.getCurrentPlayer().getHandCards());

        kingCard.addListener(ownKingCardListener);
      }

      gameStage.addActor(kingCard);

      if (kingCard.getBoosted() > 0) {
        // show saboteur spite
        Texture mercenaryTexture = new Texture(Gdx.files.internal("data/skins/whitepawn.png"));
        TextureRegion mercenaryRegion = new TextureRegion(mercenaryTexture, 0, 0, 512, 512);
        Image mercenaryImage = new Image(mercenaryRegion);
        mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(), mercenaryImage.getWidth() / 20f,
            mercenaryImage.getHeight() / 20f);
        // set to center of def card
        mercenaryImage.setPosition(kingCard.getX(), kingCard.getY());
        mercenaryImage.setX(mercenaryImage.getX() + kingCard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
        mercenaryImage.setY(mercenaryImage.getY() + kingCard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);

        // add listener to take back own mercenaries
        removeAllListeners(mercenaryImage);
        mercenaryImageListener = new MercenaryImageListener(gameState, kingCard, currentPlayer);
        mercenaryImage.addListener(mercenaryImageListener);

        gameStage.addActor(mercenaryImage);

        // boost counter in center
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
          // card is a valid defense card
          defCard = defCards.get(j);
          defCard.setPlaceholder(false);
          defCard.removeAllListeners();
          if (players.get(i) != currentPlayer) {
            enemyDefCardListener = new EnemyDefCardListener(defCard, gameState.getCardDeck(),
                gameState.getCemeteryDeck(), gameState.getCurrentPlayer(), gameState.getPlayers());
            defCard.addListener(enemyDefCardListener);

          } else {
            ownDefCardListener = new OwnDefCardListener(gameState, defCard, gameState.getCurrentPlayer().getKingCard(),
                gameState.getCurrentPlayer().getDefCards(), gameState.getCurrentPlayer().getTopDefCards(),
                gameState.getCurrentPlayer().getHandCards(), gameState.getCurrentPlayer(), gameState.getPlayers());
            defCard.addListener(ownDefCardListener);
          }
        } else {
          // create placeholder card
          defCard = new Card();
          defCard.removeAllListeners();

          // listener for placeholder card
          if (players.get(i) == currentPlayer) {
            ownPlaceholderListener = new OwnPlaceholderListener(defCard, gameState.getCurrentPlayer());
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

        // check if card is sabotaged
        if (defCard.isSabotaged()) {
          // show saboteur spite
          Texture sabotagedTexture = new Texture(Gdx.files.internal("data/skins/sabotaged.png"));
          TextureRegion sabotagedRegion = new TextureRegion(sabotagedTexture, 0, 0, 64, 64);
          Image sabotagedImage = new Image(sabotagedRegion);
          sabotagedImage.setBounds(sabotagedImage.getX(), sabotagedImage.getY(), sabotagedImage.getWidth() / 2f,
              sabotagedImage.getHeight() / 2f);
          // set to center of def card
          sabotagedImage.setPosition(defCard.getX(), defCard.getY());
          sabotagedImage.setX(sabotagedImage.getX() + defCard.getWidth() / 2f - sabotagedImage.getWidth() / 2f);
          sabotagedImage.setY(sabotagedImage.getY() + defCard.getHeight() / 2f - sabotagedImage.getHeight() / 2f);

          // add listener to take back own saboteurs
          removeAllListeners(sabotagedImage);
          sabotagedImageListener = new SabotagedImageListener(gameState, defCard, currentPlayer);
          sabotagedImage.addListener(sabotagedImageListener);

          gameStage.addActor(sabotagedImage);
        }

        if (defCard.getBoosted() > 0) {
          // show saboteur spite
          Texture mercenaryTexture = new Texture(Gdx.files.internal("data/skins/whitepawn.png"));
          TextureRegion mercenaryRegion = new TextureRegion(mercenaryTexture, 0, 0, 512, 512);
          Image mercenaryImage = new Image(mercenaryRegion);
          mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(), mercenaryImage.getWidth() / 20f,
              mercenaryImage.getHeight() / 20f);
          // set to center of def card
          mercenaryImage.setPosition(defCard.getX(), defCard.getY());
          mercenaryImage.setX(mercenaryImage.getX() + defCard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
          mercenaryImage.setY(mercenaryImage.getY() + defCard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);

          // add listener to take back own mercenaries
          removeAllListeners(mercenaryImage);
          mercenaryImageListener = new MercenaryImageListener(gameState, defCard, currentPlayer);
          mercenaryImage.addListener(mercenaryImageListener);

          gameStage.addActor(mercenaryImage);

          // boost counter in center
          String boostCount = String.valueOf(defCard.getBoosted());
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
            enemyDefCardListener = new EnemyDefCardListener(topDefCard, gameState.getCardDeck(),
                gameState.getCemeteryDeck(), gameState.getCurrentPlayer(), gameState.getPlayers());
            topDefCard.addListener(enemyDefCardListener);

          } else {
            ownDefCardListener = new OwnDefCardListener(gameState, topDefCard,
                gameState.getCurrentPlayer().getKingCard(), gameState.getCurrentPlayer().getDefCards(),
                gameState.getCurrentPlayer().getTopDefCards(), gameState.getCurrentPlayer().getHandCards(),
                gameState.getCurrentPlayer(), gameState.getPlayers());
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

      // display myPlayerLabel
      Label playerLabel = new Label(players.get(i).getPlayerName(), MyGdxGame.skin);
      playerLabel.setColor(0f, 0f, 0f, 1.0f);
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
        playerHeroes.get(j).setSelected(false);
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
  }

  public void showHandStage(ArrayList<Player> players, Player currentPlayer) {
    // draw hand status of active player
    for (int i = 0; i < players.size(); i++) {

      ArrayList<Card> handCards = players.get(i).getHandCards();
      for (int j = 0; j < handCards.size(); j++) {

        if (players.get(i) == gameState.getCurrentPlayer()) {
          final Card handCard = handCards.get(j);
          handCard.removeAllListeners();

          ownHandCardListener = new OwnHandCardListener(handCard, gameState.getCurrentPlayer(), gameState.getCardDeck(),
              gameState.getCemeteryDeck());
          handCard.addListener(ownHandCardListener);

          handCards.get(j).setActive(false);
          handCards.get(j).setSelected(false);
        }
      }
    }

    // draw heroes and handcards only for current player
    final ArrayList<Card> handCards = currentPlayer.getHandCards();
    ArrayList<Hero> playerHeroes = currentPlayer.getHeroes();
    currentPlayer.sortHandCards();
    for (int j = 0; j < handCards.size(); j++) {
      Card handcard = handCards.get(j);
      handcard.setCovered(false);
      handcard.setActive(true);
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
        // show saboteur spite
        Texture mercenaryTexture = new Texture(Gdx.files.internal("data/skins/whitepawn.png"));
        TextureRegion mercenaryRegion = new TextureRegion(mercenaryTexture, 0, 0, 512, 512);
        Image mercenaryImage = new Image(mercenaryRegion);
        mercenaryImage.setBounds(mercenaryImage.getX(), mercenaryImage.getY(), mercenaryImage.getWidth() / 10f,
            mercenaryImage.getHeight() / 10f);
        // set to center of def card
        mercenaryImage.setPosition(handcard.getX(), handcard.getY());
        mercenaryImage.setX(mercenaryImage.getX() + handcard.getWidth() / 2f - mercenaryImage.getWidth() / 2f);
        mercenaryImage.setY(mercenaryImage.getY() + handcard.getHeight() / 2f - mercenaryImage.getHeight() / 2f);

        // add listener to take back own mercenaries
        removeAllListeners(mercenaryImage);
        mercenaryImageListener = new MercenaryImageListener(gameState, handcard, currentPlayer);
        mercenaryImage.addListener(mercenaryImageListener);

        handStage.addActor(mercenaryImage);

        // boost counter in center
        String boostCount = String.valueOf(handcard.getBoosted());
        Label boostCountLabel = new Label(boostCount, MyGdxGame.skin);
        boostCountLabel.setColor(Color.GOLD);
        boostCountLabel.setPosition(mercenaryImage.getX() + mercenaryImage.getWidth() / 2f, mercenaryImage.getY());

        handStage.addActor(boostCountLabel);
      }

      // add keep/trade buttons if card is tradeable
      if (handCards.get(j).isTradeable()) {
        final Card tradeableCard = handCards.get(j);

        TextButton keepCardButton = new TextButton("Keep", MyGdxGame.skin);
        keepCardButton.setX(handCards.get(j).getX() + (handCards.get(j).getWidth() - keepCardButton.getWidth()) / 2f);
        keepCardButton.setY(handCards.get(j).getY() + (handCards.get(j).getHeight() + keepCardButton.getHeight()) / 2f);

        TextButton tradeCardButton = new TextButton("Trade", MyGdxGame.skin);
        tradeCardButton.setX(handCards.get(j).getX() + (handCards.get(j).getWidth() - tradeCardButton.getWidth()) / 2f);
        tradeCardButton
            .setY(handCards.get(j).getY() + (handCards.get(j).getHeight() - 3 * tradeCardButton.getHeight()) / 2f);

        // remove old listeners
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

    // display all heroes of current player
    for (int j = 0; j < playerHeroes.size(); j++) {
      final Hero hero = playerHeroes.get(j);

      hero.setHand(true);
      hero.setPosition(j * hero.getWidth(), 0);

      // if attacking symbol is given, priest should be selectable
      if (hero.getHeroName() == "Priest") {
        if (gameState.getCurrentPlayer().getPlayerTurn().getAttackingSymbol()[0] != "none") {
          hero.setSelectable(true);
        } else {
          hero.setSelectable(false);
        }
      }

      hero.removeAllListeners();

      ownHeroListener = new OwnHeroListener(hero, gameState.getCurrentPlayer());
      hero.addListener(ownHeroListener);

      Label heroLabel = new Label(hero.getHeroID(), MyGdxGame.skin);
      heroLabel.setPosition(j * hero.getWidth() + (hero.getWidth() - heroLabel.getWidth()) / 2, hero.getHeight());

      handStage.addActor(hero);
      handStage.addActor(heroLabel);

      if (hero.getHeroName() == "Mercenaries") {
        // ready counter in center
        Mercenaries mercenaries = (Mercenaries) hero;
        String readyCount = String.valueOf(mercenaries.countReady());
        Label readyCountLabel = new Label(readyCount, MyGdxGame.skin);
        readyCountLabel.setColor(Color.GOLD);
        readyCountLabel.setPosition(hero.getX() + hero.getWidth() / 2f, hero.getY());
        
        handStage.addActor(readyCountLabel);
      }
    }

    // turn info and button
    finishTurnButton = new TextButton("Finish turn", MyGdxGame.skin);
    finishTurnButton.setPosition(Gdx.graphics.getWidth() - finishTurnButton.getWidth(), 0);
    myPlayerLabel = new Label(currentPlayer.getPlayerName(), MyGdxGame.skin);
    myPlayerLabel.setPosition(Gdx.graphics.getWidth() - myPlayerLabel.getWidth(), finishTurnButton.getHeight());

    finishTurnButtonListener = new FinishTurnButtonListener(gameState);
    finishTurnButton.addListener(finishTurnButtonListener);

    handStage.addActor(myPlayerLabel);

    // add attacking symbol
    String attackingSymbol = currentPlayer.getPlayerTurn().getAttackingSymbol()[0];
    Texture symbolTexture;
    TextureRegion symbolRegion;
    if (attackingSymbol == "hearts") {
      symbolTexture = new Texture(Gdx.files.internal("data/skins/hearts.png"));
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else if (attackingSymbol == "diamonds") {
      symbolTexture = new Texture(Gdx.files.internal("data/skins/diamonds.png"));
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else if (attackingSymbol == "clubs") {
      symbolTexture = new Texture(Gdx.files.internal("data/skins/clubs.png"));
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else if (attackingSymbol == "spades") {
      symbolTexture = new Texture(Gdx.files.internal("data/skins/spades.png"));
      symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
    } else {
      symbolTexture = new Texture(Gdx.files.internal("data/skins/someSymbol.png"));
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
      Texture symbolTextureExt = new Texture(Gdx.files.internal("data/skins/someSymbol.png"));
      TextureRegion symbolRegionExt = new TextureRegion(symbolTexture, 0, 0, 342, 512);
      if (attackingSymbolExt == "hearts") {
        symbolTextureExt = new Texture(Gdx.files.internal("data/skins/hearts.png"));
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      } else if (attackingSymbolExt == "diamonds") {
        symbolTextureExt = new Texture(Gdx.files.internal("data/skins/diamonds.png"));
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      } else if (attackingSymbolExt == "clubs") {
        symbolTextureExt = new Texture(Gdx.files.internal("data/skins/clubs.png"));
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      } else if (attackingSymbolExt == "spades") {
        symbolTextureExt = new Texture(Gdx.files.internal("data/skins/spades.png"));
        symbolRegionExt = new TextureRegion(symbolTextureExt, 0, 0, 512, 512);
      }

      Image symbolImageExt = new Image(symbolRegionExt);
      symbolImageExt.setBounds(symbolImageExt.getX(), symbolImageExt.getY(), symbolImageExt.getWidth() / 10f,
          symbolImageExt.getHeight() / 10f);
      symbolImageExt.setPosition(Gdx.graphics.getWidth() - 1.5f * symbolImage.getWidth(),
          finishTurnButton.getHeight() + myPlayerLabel.getHeight());
      handStage.addActor(symbolImageExt);
    }

    // add hand image
    Texture handTexture = new Texture(Gdx.files.internal("data/skins/hand.png"));
    TextureRegion handRegion = new TextureRegion(handTexture, 0, 0, 512, 512);
    Image handImage = new Image(handRegion);
    handImage.setBounds(handImage.getX(), handImage.getY(), handImage.getWidth() / 5f, handImage.getHeight() / 5f);
    handImage.setPosition(Gdx.graphics.getWidth() - (myPlayerLabel.getWidth() + handImage.getWidth()), 0);

    removeAllListeners(handImage);

    handImageListener = new HandImageListener(gameState, gameState.getCurrentPlayer());
    handImage.addListener(handImageListener);

    handStage.addActor(handImage);
    handStage.addActor(finishTurnButton);
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

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
