package com.mygdx.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;

import com.mygdx.game.heroes.Hero;
import com.mygdx.game.listeners.CemeteryDeckListener;
import com.mygdx.game.listeners.PickingDeckListener;

public class GameState {

  // status saved in variables
  private int roundNumber;
  private Player currentPlayer;
  private ArrayList<Player> players;
  private ArrayList<Player> roundOrder;
  Iterator<Player> currentPlayerIt;
  private ArrayList<PickingDeck> pickingDecks;
  private CardDeck cardDeck;
  private CardDeck cemeteryDeck;
  private HeroesSquare heroesSquare;

  private boolean updateState;

  private PickingDeckListener pickingDeckListenerOne;
  private PickingDeckListener pickingDeckListenerTwo;

  private CemeteryDeckListener cemeteryDeckListener;

  // Constructor for centralized (server-driven) game state.
  // Players already have their hand cards from the server.
  // deckJson contains the remaining card IDs in the same order the server holds them.
  public GameState(ArrayList<Player> players, JSONArray deckJson) {
    roundNumber = 0;
    this.players = players;
    roundOrder = new ArrayList<Player>(players);
    pickingDecks = new ArrayList<PickingDeck>();
    cemeteryDeck = new CardDeck(true);
    heroesSquare = new HeroesSquare();
    updateState = false;

    // Rebuild card deck from server-provided remaining IDs
    cardDeck = new CardDeck(true); // empty
    try {
      for (int i = 0; i < deckJson.length(); i++) {
        cardDeck.addCard(Card.fromCardId(deckJson.getInt(i)));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // Assign king cards, defense cards, heroes from hand (deterministic for all clients)
    doRandomSetup(players.size());

    // Fill picking decks from the server deck
    pickingDecks.add(new PickingDeck());
    pickingDecks.add(new PickingDeck());
    Card card1 = cardDeck.getCard(cemeteryDeck);
    Card card2 = cardDeck.getCard(cemeteryDeck);
    Card card3 = cardDeck.getCard(cemeteryDeck);
    Card card4 = cardDeck.getCard(cemeteryDeck);
    Card card5 = cardDeck.getCard(cemeteryDeck);
    card3.setCovered(true);
    card4.setCovered(true);
    card5.setCovered(true);
    pickingDecks.get(0).addCard(card1);
    pickingDecks.get(1).addCard(card2);
    pickingDecks.get(0).addCard(card3);
    pickingDecks.get(1).addCard(card4);
    if (card1.getStrength() < card2.getStrength()) {
      pickingDecks.get(0).addCard(card5);
    } else {
      pickingDecks.get(1).addCard(card5);
    }

    pickingDeckListenerOne = new PickingDeckListener(this, pickingDecks.get(0), pickingDecks.get(1));
    pickingDecks.get(0).addListener(pickingDeckListenerOne);

    pickingDeckListenerTwo = new PickingDeckListener(this, pickingDecks.get(1), pickingDecks.get(0));
    pickingDecks.get(1).addListener(pickingDeckListenerTwo);

    cemeteryDeckListener = new CemeteryDeckListener(this);
    cemeteryDeck.addListener(cemeteryDeckListener);

    // Determine starting player by dice
    throwDices();
  }

  public GameState(int numPlayers, int startCards) {
    roundNumber = 0;
    players = new ArrayList<Player>();
    roundOrder = new ArrayList<Player>();
    pickingDecks = new ArrayList<PickingDeck>();
    cardDeck = new CardDeck();
    cemeteryDeck = new CardDeck(true);
    heroesSquare = new HeroesSquare();
    updateState = false;

    for (int i = 0; i < numPlayers; i++) {
      System.out.println("init player");
      players.add(new Player("Player " + i));
      roundOrder.add(players.get(i));

      // distribute cards
      for (int j = 0; j < startCards; j++) {
        players.get(i).addHandCard(cardDeck.getCard(cemeteryDeck));
      }
    }

    // setup for testing
    doRandomSetup(numPlayers);

    // fill picking decks with 5 cards
    pickingDecks.add(new PickingDeck());
    pickingDecks.add(new PickingDeck());
    // final PickingDeck pickingDeckOneRef = pickingDecks.get(0);
    // final PickingDeck pickingDeckTwoRef = pickingDecks.get(1);

    Card card1 = cardDeck.getCard(cemeteryDeck); // open picking card
    Card card2 = cardDeck.getCard(cemeteryDeck); // open picking card
    Card card3 = cardDeck.getCard(cemeteryDeck); // covered picking card
    Card card4 = cardDeck.getCard(cemeteryDeck); // covered picking card
    Card card5 = cardDeck.getCard(cemeteryDeck); // covered picking card
    card3.setCovered(true);
    card4.setCovered(true);
    card5.setCovered(true);
    pickingDecks.get(0).addCard(card1);
    pickingDecks.get(1).addCard(card2);
    pickingDecks.get(0).addCard(card3);
    pickingDecks.get(1).addCard(card4);
    if (card1.getStrength() < card2.getStrength()) {
      pickingDecks.get(0).addCard(card5);
    } else {
      pickingDecks.get(1).addCard(card5);
    }

    pickingDeckListenerOne = new PickingDeckListener(this, pickingDecks.get(0), pickingDecks.get(1));
    pickingDecks.get(0).addListener(pickingDeckListenerOne);

    pickingDeckListenerTwo = new PickingDeckListener(this, pickingDecks.get(1), pickingDecks.get(0));
    pickingDecks.get(1).addListener(pickingDeckListenerTwo);

    // add cemetery deck listener
    cemeteryDeckListener = new CemeteryDeckListener(this);
    cemeteryDeck.addListener(cemeteryDeckListener);

    // start round
    throwDices();

  }

  // for tests
  public void doRandomSetup(int numPlayers) {
    int[] heroIndices = {2, 4, 12, 13};
    for (int i = 0; i < numPlayers; i++) {
      // set kingCard
      players.get(i).setKingCard(players.get(i).getLastHandCard());

      // add defCards
      for (int j = 1; j <= 3; j++) {
        Card defCard = players.get(i).getLastHandCard();
        defCard.setCovered(true);
        players.get(i).addDefCard(j, defCard, 0);
      }

      // add heroes (assign if available)
      if (i < heroIndices.length) {
        Hero hero = heroesSquare.getHero(heroIndices[i]);
        players.get(i).addHero(hero);
      }

      for (int j = 0; j < 2; j++) {
        cemeteryDeck.addCard(players.get(i).getLastHandCard());
      }
    }
  }

  public void throwDices() {
    // throw dice
    for (int i = 0; i < players.size(); i++) {
      players.get(i).throwDice();
    }

    // create order
    Collections.copy(roundOrder, players);
    for (int i = 1; i < players.size(); i++) {
      for (int j = i; j > 0; j--) {
        if (roundOrder.get(j).getDiceNumber() > roundOrder.get(j - 1).getDiceNumber()) {
          Collections.swap(roundOrder, j, j - 1);
        }
      }
    }
    currentPlayerIt = roundOrder.iterator();
    currentPlayer = currentPlayerIt.next();

  }

  public ArrayList<Player> getPlayers() {
    ArrayList<Player> alivePlayers = new ArrayList<Player>();
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).isAlive)
        alivePlayers.add(players.get(i));
    }
    return alivePlayers;
  }

  public Player getCurrentPlayer() {
    return currentPlayer;
  }

  public int getCurrentPlayerIndex() {
    return players.indexOf(currentPlayer);
  }

  public Player getNextPlayer() {
    Player nextPlayer;
    if (currentPlayerIt.hasNext()) {
      nextPlayer = currentPlayerIt.next();
    } else {
      roundNumber++;
      throwDices();
      currentPlayerIt = roundOrder.iterator();
      nextPlayer = currentPlayerIt.next();
    }
    nextPlayer.newPlayerTurn();
    currentPlayer = nextPlayer;
    return nextPlayer;
  }

  public CardDeck getCardDeck() {
    return cardDeck;
  }

  public CardDeck getCemeteryDeck() {
    return cemeteryDeck;
  }

  public ArrayList<PickingDeck> getPickingDecks() {
    return pickingDecks;
  }

  public HeroesSquare getHeroesSquare() {
    return heroesSquare;
  }

  public void nextRound() {
    roundNumber++;
  }

  public int getRoundNumber() {
    return roundNumber;
  }

  public boolean getUpdateState() {
    return updateState;
  }

  public void setUpdateState(boolean updateState) {
    this.updateState = updateState;
  }

}
