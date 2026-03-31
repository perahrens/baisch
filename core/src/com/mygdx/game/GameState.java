package com.mygdx.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;

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

  private Socket socket;

  private int winnerIndex = -1;
  public int getWinnerIndex() { return winnerIndex; }
  public void setWinnerIndex(int idx) { this.winnerIndex = idx; }

  private PickingDeckListener pickingDeckListenerOne;
  private PickingDeckListener pickingDeckListenerTwo;

  private CemeteryDeckListener cemeteryDeckListener;

  public Socket getSocket() { return socket; }
  public void setSocket(Socket socket) { this.socket = socket; }

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

    pickingDeckListenerOne = new PickingDeckListener(this, pickingDecks.get(0), pickingDecks.get(1), 0);
    pickingDecks.get(0).addListener(pickingDeckListenerOne);

    pickingDeckListenerTwo = new PickingDeckListener(this, pickingDecks.get(1), pickingDecks.get(0), 1);
    pickingDecks.get(1).addListener(pickingDeckListenerTwo);

    cemeteryDeckListener = new CemeteryDeckListener(this);
    cemeteryDeck.addListener(cemeteryDeckListener);

    // Determine starting player by dice
    throwDices();
  }

  // Constructor for server-authoritative full state (no local doRandomSetup).
  // fullState is the JSON object from server's gameState.serialize().
  public GameState(JSONObject fullState) {
    roundNumber = 0;
    players = new ArrayList<Player>();
    roundOrder = new ArrayList<Player>();
    pickingDecks = new ArrayList<PickingDeck>();
    cemeteryDeck = new CardDeck(true);
    heroesSquare = new HeroesSquare();
    updateState = false;

    try {
      // 1. Build players (hand, defCards, topDefCards, kingCard)
      JSONArray playersJson = fullState.getJSONArray("players");
      for (int i = 0; i < playersJson.length(); i++) {
        JSONObject pj = playersJson.getJSONObject(i);
        int idx = pj.getInt("index");
        Player p = new Player("Player " + idx);

        JSONArray handJson = pj.getJSONArray("hand");
        for (int h = 0; h < handJson.length(); h++) {
          p.handCards.add(Card.fromCardId(handJson.getInt(h)));
        }

        JSONObject defJson = pj.getJSONObject("defCards");
        Iterator<String> defKeys = defJson.keys();
        while (defKeys.hasNext()) {
          String key = defKeys.next();
          Card dc = Card.fromCardId(defJson.getInt(key));
          dc.setCovered(true);
          p.defCards.put(Integer.parseInt(key), dc);
        }

        JSONObject topDefJson = pj.getJSONObject("topDefCards");
        Iterator<String> topKeys = topDefJson.keys();
        while (topKeys.hasNext()) {
          String key = topKeys.next();
          Card tdc = Card.fromCardId(topDefJson.getInt(key));
          tdc.setCovered(true);
          p.topDefCards.put(Integer.parseInt(key), tdc);
        }

        p.setKingCard(Card.fromCardId(pj.getInt("kingCard")));
        players.add(p);
      }
      roundOrder = new ArrayList<Player>(players);

      // 2. Deck
      cardDeck = new CardDeck(true);
      JSONArray deckJson = fullState.getJSONArray("deck");
      for (int i = 0; i < deckJson.length(); i++) {
        cardDeck.getCards().add(Card.fromCardId(deckJson.getInt(i)));
      }

      // 3. Cemetery
      JSONArray cemJson = fullState.getJSONArray("cemetery");
      for (int i = 0; i < cemJson.length(); i++) {
        cemeteryDeck.getCards().add(Card.fromCardId(cemJson.getInt(i)));
      }

      // 4. Picking decks
      JSONArray pickJson = fullState.getJSONArray("pickingDecks");
      for (int i = 0; i < pickJson.length(); i++) {
        PickingDeck pd = new PickingDeck();
        JSONArray pdJson = pickJson.getJSONArray(i);
        for (int j = 0; j < pdJson.length(); j++) {
          JSONObject co = pdJson.getJSONObject(j);
          Card c = Card.fromCardId(co.getInt("id"));
          c.setCovered(co.getBoolean("covered"));
          pd.addCard(c);
        }
        pickingDecks.add(pd);
      }

      // 5. Current player (sequential order, start from server's currentPlayerIndex)
      int cpIdx = fullState.getInt("currentPlayerIndex");
      currentPlayerIt = roundOrder.iterator();
      for (int i = 0; i < cpIdx; i++) currentPlayerIt.next();
      currentPlayer = currentPlayerIt.next();

      // 6. Listeners
      pickingDeckListenerOne = new PickingDeckListener(this, pickingDecks.get(0), pickingDecks.get(1), 0);
      pickingDecks.get(0).addListener(pickingDeckListenerOne);
      pickingDeckListenerTwo = new PickingDeckListener(this, pickingDecks.get(1), pickingDecks.get(0), 1);
      pickingDecks.get(1).addListener(pickingDeckListenerTwo);
      cemeteryDeckListener = new CemeteryDeckListener(this);
      cemeteryDeck.addListener(cemeteryDeckListener);

      // 7. Heroes (deterministic per player index, same as doRandomSetup)
      doHeroSetup(players.size());

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  // Set current player directly by index (used by applyStateUpdate on turn change).
  public void setCurrentPlayer(int idx) {
    Player newPlayer = players.get(idx);
    if (newPlayer != currentPlayer) {
      newPlayer.newPlayerTurn();
      currentPlayer = newPlayer;
    }
  }

  // Assign heroes only (no card manipulation) — mirrors the hero part of doRandomSetup.
  private void doHeroSetup(int numPlayers) {
    int[] heroIndices = {2, 4, 12, 13};
    for (int i = 0; i < numPlayers; i++) {
      if (i < heroIndices.length) {
        Hero hero = heroesSquare.getHero(heroIndices[i]);
        if (hero != null) players.get(i).addHero(hero);
      }
    }
  }

  public GameState(int numPlayers, int startCards) {
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

    pickingDeckListenerOne = new PickingDeckListener(this, pickingDecks.get(0), pickingDecks.get(1), 0);
    pickingDecks.get(0).addListener(pickingDeckListenerOne);

    pickingDeckListenerTwo = new PickingDeckListener(this, pickingDecks.get(1), pickingDecks.get(0), 1);
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
