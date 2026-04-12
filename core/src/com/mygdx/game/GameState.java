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

  // True while the manual setup phase is in progress (server uses setupPhase flag).
  private boolean setupPhase = false;
  public boolean isSetupPhase() { return setupPhase; }
  public void setSetupPhase(boolean v) { setupPhase = v; }

  private Socket socket;

  private int winnerIndex = -1;
  public int getWinnerIndex() { return winnerIndex; }
  public void setWinnerIndex(int idx) { this.winnerIndex = idx; }

  // Priest overlay state: which enemy deck is open (-1 = none), which card was revealed (-1 = none)
  private int priestTargetPlayerIdx = -1;
  private int priestRevealedCardId = -1;
  public int getPriestTargetPlayerIdx() { return priestTargetPlayerIdx; }
  public void setPriestTargetPlayerIdx(int idx) { priestTargetPlayerIdx = idx; }
  public int getPriestRevealedCardId() { return priestRevealedCardId; }
  public void setPriestRevealedCardId(int id) { priestRevealedCardId = id; }

  private PickingDeckListener pickingDeckListenerOne;
  private PickingDeckListener pickingDeckListenerTwo;

  private CemeteryDeckListener cemeteryDeckListener;

  public SocketClient getSocket() { return socket; }
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
        String name = pj.optString("name", "Player " + idx);
        Player p = new Player(name);

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

        int kingId = pj.optInt("kingCard", 0);
        if (kingId > 0) p.setKingCard(Card.fromCardId(kingId));

        // Saboteurs slot state
        JSONObject sabotagedJson = pj.optJSONObject("sabotaged");
        if (sabotagedJson != null) {
          Iterator<String> sabKeys = sabotagedJson.keys();
          while (sabKeys.hasNext()) {
            String key = sabKeys.next();
            p.setSlotSabotaged(Integer.parseInt(key), sabotagedJson.getInt(key));
          }
        }

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
        currentPlayer = roundOrder.get(cpIdx);
      // 6. Listeners (only if picking decks are initialised — not true during setup phase)
      if (pickingDecks.size() >= 2) {
        pickingDeckListenerOne = new PickingDeckListener(this, pickingDecks.get(0), pickingDecks.get(1), 0);
        pickingDecks.get(0).addListener(pickingDeckListenerOne);
        pickingDeckListenerTwo = new PickingDeckListener(this, pickingDecks.get(1), pickingDecks.get(0), 1);
        pickingDecks.get(1).addListener(pickingDeckListenerTwo);
      } else {
        // Ensure we always have two (possibly empty) picking deck slots
        while (pickingDecks.size() < 2) pickingDecks.add(new PickingDeck());
      }
      cemeteryDeckListener = new CemeteryDeckListener(this);
      cemeteryDeck.addListener(cemeteryDeckListener);

      // 7. Heroes from server-authoritative state
      rebuildHeroesFromState(playersJson);

      // 8. Setup phase flag
      setupPhase = fullState.optBoolean("setupPhase", false);

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

  // Assign heroes only (no card manipulation) — players now start with NO hero.
  // Hero acquisition happens via joker sacrifice during gameplay.
  private void doHeroSetup(int numPlayers) {
    // intentionally empty — heroes are earned, not assigned at start
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
    for (int i = 0; i < numPlayers; i++) {
      // set kingCard
      players.get(i).setKingCard(players.get(i).getLastHandCard());

      // add defCards
      for (int j = 1; j <= 3; j++) {
        Card defCard = players.get(i).getLastHandCard();
        defCard.setCovered(true);
        players.get(i).addDefCard(j, defCard, 0);
      }

      // players start with NO hero — heroes are acquired via joker sacrifice

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

  /**
   * Rebuild all heroes from a server-authoritative players JSON array.
   *
   * <p>Existing hero instances are reused for heroes the same player already owns. This
   * preserves all per-turn counters (Priest conversionAttempts, Merchant trades, Marshal
   * mobilizations, etc.) through routine stateUpdate syncs so heroes cannot be exploited by
   * triggering a server event to "reload" their counters.
   *
   * <p>When a hero instance is newly created (first acquisition or reconnect), the server-
   * serialized counter is applied — e.g. {@code priestConversionAttempts} — so that a
   * reconnecting player cannot use more attempts than the server permits.
   */
  public void rebuildHeroesFromState(JSONArray playersJson) throws JSONException {
    // Save current hero instances keyed by playerIdx → heroName.
    @SuppressWarnings("unchecked")
    java.util.Map<String, Hero>[] savedHeroes = new java.util.Map[players.size()];
    for (int i = 0; i < players.size(); i++) {
      savedHeroes[i] = new java.util.HashMap<String, Hero>();
      for (Hero h : players.get(i).getHeroes()) {
        savedHeroes[i].put(h.getHeroName(), h);
      }
    }

    heroesSquare = new HeroesSquare();
    for (int i = 0; i < players.size(); i++) {
      players.get(i).getHeroes().clear();
    }

    for (int i = 0; i < playersJson.length(); i++) {
      JSONObject pj = playersJson.getJSONObject(i);
      int idx = pj.getInt("index");
      JSONArray heroesJson = pj.optJSONArray("heroes");
      if (heroesJson == null) continue;

      for (int h = 0; h < heroesJson.length(); h++) {
        String heroName = heroesJson.getString(h);
        Hero existing = (savedHeroes[idx] != null) ? savedHeroes[idx].get(heroName) : null;
        Hero syncedHero;
        if (existing != null) {
          // Reuse existing instance — preserves mid-turn state for all heroes.
          heroesSquare.consumeHeroByName(heroName); // remove from pool to keep it consistent
          players.get(idx).addHero(existing);
          syncedHero = existing;
        } else {
          // New acquisition or reconnect: get a fresh hero from the pool.
          applyHeroAcquired(idx, heroName);
          ArrayList<Hero> heroList = players.get(idx).getHeroes();
          syncedHero = heroList.get(heroList.size() - 1);
        }

        // Apply server-authoritative hero counters so reconnects cannot refresh turn-limited actions.
        if (syncedHero instanceof com.mygdx.game.heroes.Priest) {
          int serverAttempts = pj.optInt("priestConversionAttempts", 2);
          ((com.mygdx.game.heroes.Priest) syncedHero).setConversionAttempts(serverAttempts);
        } else if (syncedHero instanceof com.mygdx.game.heroes.Magician) {
          int serverSpells = pj.optInt("magicianSpells", 1);
          ((com.mygdx.game.heroes.Magician) syncedHero).setSpells(serverSpells);
        } else if (syncedHero instanceof com.mygdx.game.heroes.Merchant) {
          int serverTrades = pj.optInt("merchantTrades", 1);
          ((com.mygdx.game.heroes.Merchant) syncedHero).setTrades(serverTrades);
        } else if (syncedHero instanceof com.mygdx.game.heroes.Warlord) {
          int serverAttacks = pj.optInt("warlordAttacks", 1);
          ((com.mygdx.game.heroes.Warlord) syncedHero).setAttacks(serverAttacks);
        } else if (syncedHero instanceof com.mygdx.game.heroes.Spy) {
          int serverSpyAttacks = pj.optInt("spyAttacks", 1);
          int serverSpyMaxAttacks = pj.optInt("spyMaxAttacks", 1);
          int serverSpyExtends = pj.optInt("spyExtends", 1);
          com.mygdx.game.heroes.Spy spy = (com.mygdx.game.heroes.Spy) syncedHero;
          spy.setSpyAttacks(serverSpyAttacks);
          spy.setSpyMaxAttacks(serverSpyMaxAttacks);
          spy.setSpyExtends(serverSpyExtends);
        }
      }
    }
  }

  /**
   * Apply a hero acquisition received from another client via the heroAcquired socket event.
   * Consumes the named hero from the HeroesSquare pool and adds it to the player.
   */
  public void applyHeroAcquired(int playerIdx, String heroName) {
    Hero hero = heroesSquare.consumeHeroByName(heroName);
    if (hero == null) {
      // Hero was stolen from another player (not drawn from the square).
      // Strip it from whoever currently owns it so there are no duplicates.
      int ownerIdx = findHeroOwnerIndex(heroName);
      if (ownerIdx >= 0 && ownerIdx != playerIdx) {
        hero = players.get(ownerIdx).removeHeroByName(heroName);
      }
    }
    if (hero != null && playerIdx >= 0 && playerIdx < players.size()) {
      players.get(playerIdx).addHero(hero);
    }
  }

  /**
   * Find the index of the player who currently owns a hero with the given name.
   * Returns -1 if no player owns that hero.
   */
  public int findHeroOwnerIndex(String heroName) {
    for (int i = 0; i < players.size(); i++) {
      ArrayList<Hero> heroes = players.get(i).getHeroes();
      for (int j = 0; j < heroes.size(); j++) {
        if (heroName.equals(heroes.get(j).getHeroName())) {
          return i;
        }
      }
    }
    return -1;
  }

}
