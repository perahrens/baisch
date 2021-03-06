package com.mygdx.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Major;

public class Player {

  // inventar
  boolean isAlive;
  String playerName;
  ArrayList<Card> handCards;
  Map<Integer, Card> defCards;
  Map<Integer, Card> topDefCards; // defense cards from fortified tower
  Card kingCard;
  ArrayList<Hero> heroes;
  Dice dice;

  PlayerTurn playerTurn;

  String selectedSymbol; // enables the selection of hand cards of the same symbol

  public Player(String name) {
    // init inventar
    isAlive = true;
    playerName = name;
    handCards = new ArrayList<Card>();
    defCards = new HashMap<Integer, Card>();
    topDefCards = new HashMap<Integer, Card>();
    heroes = new ArrayList<Hero>();
    //dice = new Dice();

    playerTurn = new PlayerTurn();

    selectedSymbol = "none";
  }

  public void addHandCard(Card card) {

    final Card refCard = card;

    handCards.add(card);

    Array<EventListener> listeners = card.getListeners();
    for (EventListener listener : listeners) {
      card.removeListener(listener);
    }

    card.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        // unselect all defense and king cards
        kingCard.setSelected(false);
        for (int i = 1; i <= 3; i++) {
          if (defCards.containsKey(i)) {
            defCards.get(i).setSelected(false);
          }
        }

        // select hand card
        if (refCard.isSelected()) {
          refCard.setSelected(false);
        } else {
          if (refCard.getSymbol() == selectedSymbol) {
            refCard.setSelected(true);
          } else {
            for (int i = 0; i < handCards.size(); i++) {
              handCards.get(i).setSelected(false);
            }
            refCard.setSelected(true);
            selectedSymbol = refCard.getSymbol();
          }
        }
      };
    });
  }

  public void takeDefCard(int position) {
    System.out.println("takeDefCard()");

    // check if player has major
    boolean hasMajor = false;
    Major major = null;
    for (int i = 0; i < heroes.size(); i++) {
      if (heroes.get(i).getHeroName() == "Major") {
        // Major found
        major = (Major) heroes.get(i);
        hasMajor = true;
        break;
      }
    }

    boolean allowed = false;
    if (hasMajor) {
      if (major.getMobilizations() > 0) {
        allowed = true;
        major.mobilize();
      }
    } else {
      if (playerTurn.getTakeDefCard() > 0) {
        allowed = true;
        playerTurn.decreaseTakeDefCard();
      }
    }

    if (allowed) {
      addHandCard(defCards.get(position));
      defCards.remove(position);
      if (topDefCards.containsKey(position)) {
        addHandCard(topDefCards.get(position));
        topDefCards.remove(position);
      }
    } else {
      System.out.println("No more take actions allowed");
    }
  }

  public void putDefCard(Integer position, int level) {
    System.out.println("putDefCard()");

    // check if player has major
    boolean hasMajor = false;
    Major major = null;
    for (int i = 0; i < heroes.size(); i++) {
      if (heroes.get(i).getHeroName() == "Major") {
        // Major found
        major = (Major) heroes.get(i);
        hasMajor = true;
        break;
      }
    }

    boolean allowed = false;
    if (hasMajor) {
      if (major.getMobilizations() > 0) {
        allowed = true;
        major.mobilize();
      }
    } else if (level == 1) {
      allowed = true;
    } else {
      if (playerTurn.getPutDefCard() > 0) {
        allowed = true;
        playerTurn.decreasePutDefCard();
      }
    }

    if (allowed) {
      Iterator<Card> handCardsIt = handCards.iterator();
      while (handCardsIt.hasNext()) {
        Card handCard = handCardsIt.next();
        if (handCard.isSelected()) {
          addDefCard(position, handCard, level);
          handCard.setCovered(true);
          handCardsIt.remove();
        }
      }
    } else {
      System.out.println("No more put actions allowed");
    }
  }

  public void addDefCard(Integer position, Card card, int level) {
    if (position >= 1 && position <= 3) {

      final Map<Integer, Card> levelDefCards;
      if (level == 0) {
        levelDefCards = defCards;
      } else {
        levelDefCards = topDefCards;
      }

      card.setSelected(false);
      card.removeAllListeners();

      final Card refCard = card;

      card.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          // unselect all handcards
          for (int i = 0; i < handCards.size(); i++) {
            handCards.get(i).setSelected(false);
          }

          // select defense card
          if (refCard.isSelected()) {
            refCard.setSelected(false);
          } else {
            kingCard.setSelected(false);
            for (int i = 1; i <= 3; i++) {
              if (levelDefCards.containsKey(i)) {
                levelDefCards.get(i).setSelected(false);
              }
            }
            refCard.setSelected(true);
          }
        };
      });

      levelDefCards.put(position, card);
    } else {
      // not allowed
    }
  }

  public boolean attackEnemyDefense(Card defCard) {
    // make sum of selected handcards
    int attackSum = 0;
    Iterator<Card> handCardsIt = handCards.iterator();
    while (handCardsIt.hasNext()) {
      Card handCard = handCardsIt.next();
      if (handCard.isSelected()) {
        attackSum += handCard.getStrength();
      }
    }

    // Joker cards have value 1 in defense
    int defenseStrength = defCard.getStrength();
    if (defCard.getSymbol() == "joker") {
      defenseStrength = 1;
    }

    System.out.println("Attack enemy defense card " + attackSum + " <> " + defenseStrength);

    return (attackSum > defenseStrength);
  }

  public boolean attackEnemyDefense(Card defCard, Card topDefCard) {
    // make sum of selected handcards
    int attackSum = 0;
    Iterator<Card> handCardsIt = handCards.iterator();
    while (handCardsIt.hasNext()) {
      Card handCard = handCardsIt.next();
      if (handCard.isSelected()) {
        attackSum += handCard.getStrength();
      }
    }

    // Joker cards have value 1 in defense
    int defenseStrength = 0;
    if (defCard.getSymbol() == "joker") {
      defenseStrength++;
    } else {
      defenseStrength += defCard.getStrength();
    }
    if (topDefCard.getSymbol() == "joker") {
      defenseStrength++;
    } else {
      defenseStrength += topDefCard.getStrength();
    }

    System.out.println("Attack enemy defense card " + attackSum + " <> " + defenseStrength);

    return (attackSum > defenseStrength);
  }

  public int royalAttack(Card defCard) {
    int attackResult; // 0 lost, 1 equal, 2 win
    if (kingCard.getStrength() > defCard.getStrength()) {
      attackResult = 2;
    } else if (kingCard.getStrength() == defCard.getStrength()) {
      attackResult = 1;
    } else {
      attackResult = 0;
    }

    return attackResult;

  }

  public int royalAttack(Card defCard, Card topDefCard) {
    int attackResult; // 0 lost, 1 equal, 2 win

    // Joker cards have value 1 in defense
    int defenseStrength = 0;
    if (defCard.getSymbol() == "joker") {
      defenseStrength++;
    } else {
      defenseStrength += defCard.getStrength();
    }
    if (topDefCard.getSymbol() == "joker") {
      defenseStrength++;
    } else {
      defenseStrength += topDefCard.getStrength();
    }

    if (kingCard.getStrength() > defenseStrength) {
      attackResult = 2;
    } else if (kingCard.getStrength() == defenseStrength) {
      attackResult = 1;
    } else {
      attackResult = 0;
    }

    return attackResult;

  }

  public void attackPickingDeck(PickingDeck pickingDeck, PickingDeck pickingDeckOther, CardDeck cardDeck,
      CardDeck cemeteryDeck) {
    System.out.println("attackPickingDeck");
    ArrayList<Card> pickingCards = pickingDeck.getCards();
    Card pickingDefCard = pickingCards.get(pickingCards.size() - 1);
    pickingDefCard.setCovered(false);

    // make sum of selected handcards
    int attackSum = 0;
    for (int i = 0; i < handCards.size(); i++) {
      if (handCards.get(i).isSelected()) {
        attackSum += handCards.get(i).getStrength();
      }
    }

    System.out.println("Attack with " + attackSum + " defense is " + pickingDefCard.getStrength());

    // make actual attack
    if (attackSum > pickingDefCard.getStrength()) {
      Iterator<Card> pickingCardIt = pickingCards.iterator();
      while (pickingCardIt.hasNext()) {
        addHandCard(pickingCardIt.next());
        pickingCardIt.remove();
      }
      pickingDeckOther.addCard(cardDeck.getCard(cemeteryDeck));
      pickingDeck.addCard(cardDeck.getCard(cemeteryDeck));
      pickingDeck.getCards().get(pickingDeck.getCards().size() - 1).setCovered(false);
      pickingDeck.addCard(cardDeck.getCard(cemeteryDeck));
    } else {
      pickingDeck.addCard(cardDeck.getCard(cemeteryDeck));
    }

  }

  public void sortHandCards() {
    ArrayList<Card> hearts = new ArrayList<Card>();
    ArrayList<Card> diamonds = new ArrayList<Card>();
    ArrayList<Card> spades = new ArrayList<Card>();
    ArrayList<Card> clubs = new ArrayList<Card>();
    ArrayList<Card> jokers = new ArrayList<Card>();

    // sort by symbol
    for (int i = 0; i < handCards.size(); i++) {
      String symbol = handCards.get(i).getSymbol();
      if (symbol == "hearts")
        hearts.add(handCards.get(i));
      else if (symbol == "diamonds")
        diamonds.add(handCards.get(i));
      else if (symbol == "spades")
        spades.add(handCards.get(i));
      else if (symbol == "clubs")
        clubs.add(handCards.get(i));
      else if (symbol == "joker")
        jokers.add(handCards.get(i));
    }

    // sort each symbol by strength
    for (int i = 1; i < hearts.size(); i++) {
      for (int j = i; j > 0; j--) {
        if (hearts.get(j).getStrength() < hearts.get(j - 1).getStrength()) {
          Collections.swap(hearts, j, j - 1);
        }
      }
    }
    for (int i = 1; i < diamonds.size(); i++) {
      for (int j = i; j > 0; j--) {
        if (diamonds.get(j).getStrength() < diamonds.get(j - 1).getStrength()) {
          Collections.swap(diamonds, j, j - 1);
        }
      }
    }
    for (int i = 1; i < spades.size(); i++) {
      for (int j = i; j > 0; j--) {
        if (spades.get(j).getStrength() < spades.get(j - 1).getStrength()) {
          Collections.swap(spades, j, j - 1);
        }
      }
    }
    for (int i = 1; i < clubs.size(); i++) {
      for (int j = i; j > 0; j--) {
        if (clubs.get(j).getStrength() < clubs.get(j - 1).getStrength()) {
          Collections.swap(clubs, j, j - 1);
        }
      }
    }

    // refill handCards sorted
    handCards = new ArrayList<Card>();
    for (int i = 0; i < hearts.size(); i++)
      handCards.add(hearts.get(i));
    for (int i = 0; i < diamonds.size(); i++)
      handCards.add(diamonds.get(i));
    for (int i = 0; i < spades.size(); i++)
      handCards.add(spades.get(i));
    for (int i = 0; i < clubs.size(); i++)
      handCards.add(clubs.get(i));
    for (int i = 0; i < jokers.size(); i++)
      handCards.add(jokers.get(i));

  }

  public void throwDice() {
    dice.roll();
  }

  public String getPlayerName() {
    return playerName;
  }

  public int getDiceNumber() {
    return dice.getNumber();
  }

  public Dice getDice() {
    return dice;
  }

  public Card getKingCard() {
    return kingCard;
  }

  public void setKingCard(Card kingCard) {

    kingCard.setSelected(false);

    Array<EventListener> listeners = kingCard.getListeners();
    for (EventListener listener : listeners) {
      kingCard.removeListener(listener);
    }

    final Card refCard = kingCard;

    kingCard.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (refCard.isSelected()) {
          refCard.setSelected(false);
        } else {
          for (int i = 1; i <= 3; i++) {
            if (defCards.containsKey(i)) {
              defCards.get(i).setSelected(false);
            }
          }
          refCard.setSelected(true);
        }
      };
    });

    kingCard.setCovered(true);
    this.kingCard = kingCard;
  }

  public Card getLastHandCard() {
    Card card = handCards.get(handCards.size() - 1);
    handCards.remove(handCards.size() - 1);
    return card;

  }

  public Map<Integer, Card> getDefCards() {
    return defCards;
  }

  public Map<Integer, Card> getTopDefCards() {
    return topDefCards;
  }

  public ArrayList<Card> getHandCards() {
    return handCards;
  }

  public ArrayList<Card> getSelectedHandCards() {
    ArrayList<Card> selectedHandCards = new ArrayList<Card>();
    for (int i = 0; i < handCards.size(); i++) {
      if (handCards.get(i).isSelected()) {
        selectedHandCards.add(handCards.get(i));
      }
    }
    return selectedHandCards;
  }

  public ArrayList<Hero> getSelectedHeroes() {
    ArrayList<Hero> selectedHeroes = new ArrayList<Hero>();
    for (int i = 0; i < heroes.size(); i++) {
      if (heroes.get(i).isSelected()) {
        selectedHeroes.add(heroes.get(i));
      }
    }

    return selectedHeroes;
  }

  public void addHero(Hero hero) {
    heroes.add(hero);
  }

  public ArrayList<Hero> getHeroes() {
    return heroes;
  }

  public void newPlayerTurn() {
    // recover all player heroes
    for (int i = 0; i < heroes.size(); i++) {
      heroes.get(i).recover();
    }

    playerTurn = new PlayerTurn();
  }

  public PlayerTurn getPlayerTurn() {
    return playerTurn;
  }

  public String getSelectedSymbol() {
    return selectedSymbol;
  }

  public void setSelectedSymbol(String symbol) {
    selectedSymbol = symbol;
  }
  
  public void setDead() {
    this.isAlive = false;
  }

  public boolean hasHero(String heroName) {
    boolean hasHero = false;
    for (int i = 0; i < heroes.size(); i++) {
      if (heroes.get(i).getHeroName() == heroName) {
        hasHero = true;
      }
    }
    return hasHero;
  }

}
