package com.mygdx.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.heroes.BatteryTower;
import com.mygdx.game.heroes.FortifiedTower;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.King;
import com.mygdx.game.heroes.Lieutenant;
import com.mygdx.game.heroes.Magician;
import com.mygdx.game.heroes.Major;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Merchant;
import com.mygdx.game.heroes.Priest;
import com.mygdx.game.heroes.Reservists;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.heroes.Spy;

public class GameState {

	//status saved in variables
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
	
	//private PickingDeck pickingDeckOne;
	//private PickingDeck pickingDeckTwo;

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
			
			//distribute cards
			for (int j = 0; j < startCards; j++) {
				players.get(i).addHandCard(cardDeck.getCard(cemeteryDeck));
			}
		}
		
		//setup for testing
		doRandomSetup(numPlayers);
		
		//fill picking decks with 5 cards 
		pickingDecks.add(new PickingDeck());
		pickingDecks.add(new PickingDeck());
		final PickingDeck pickingDeckOneRef = pickingDecks.get(0);
		final PickingDeck pickingDeckTwoRef = pickingDecks.get(1);
		
		Card card1 = cardDeck.getCard(cemeteryDeck); //open picking card
		Card card2 = cardDeck.getCard(cemeteryDeck); //open picking card
		Card card3 = cardDeck.getCard(cemeteryDeck); //covered picking card
		Card card4 = cardDeck.getCard(cemeteryDeck); //covered picking card
		Card card5 = cardDeck.getCard(cemeteryDeck); //covered picking card
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
		
		pickingDecks.get(0).addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (currentPlayer.getPlayerTurn().getPickingDeckAttacks() > 0) {
					System.out.println("Selected handcards " + currentPlayer.getSelectedHandCards().size());
					if (currentPlayer.getSelectedHandCards().size() > 0) {
						if (currentPlayer.getPlayerTurn().getAttackingSymbol() == "none" || 
							currentPlayer.getPlayerTurn().getAttackingSymbol() == currentPlayer.getSelectedHandCards().get(0).getSymbol()) {
							currentPlayer.getPlayerTurn().decreasePickingDeckAttacks();
							currentPlayer.attackPickingDeck(pickingDeckOneRef, pickingDeckTwoRef, cardDeck, cemeteryDeck);
							Iterator<Card> handCardIt = currentPlayer.getHandCards().iterator();
							while (handCardIt.hasNext()) {
								Card currCard = handCardIt.next();
								if (currCard.isSelected()) {
									System.out.println("Remove handcard " + currCard.getStrength());
									currentPlayer.getPlayerTurn().setAttackingSymbol(currCard.getSymbol());
									cemeteryDeck.addCard(currCard);
									handCardIt.remove();
								}
							}
						}
					}
				} else {
					System.out.println("No more picking attacks allowed");
				}
				setUpdateState(true);
			};
		});
		pickingDecks.get(1).addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (currentPlayer.getPlayerTurn().getPickingDeckAttacks() > 0) {
					System.out.println("Selected handcards " + currentPlayer.getSelectedHandCards().size());
					if (currentPlayer.getSelectedHandCards().size() > 0) {
						if (currentPlayer.getPlayerTurn().getAttackingSymbol() == "none" || 
							currentPlayer.getPlayerTurn().getAttackingSymbol() == currentPlayer.getSelectedHandCards().get(0).getSymbol()) {
							currentPlayer.getPlayerTurn().decreasePickingDeckAttacks();
							currentPlayer.attackPickingDeck(pickingDeckTwoRef, pickingDeckOneRef, cardDeck, cemeteryDeck);
							Iterator<Card> handCardIt = currentPlayer.getHandCards().iterator();
							while (handCardIt.hasNext()) {
								Card currCard = handCardIt.next();
								if (currCard.isSelected()) {
									System.out.println("Remove handcard " + currCard.getStrength());
									currentPlayer.getPlayerTurn().setAttackingSymbol(currCard.getSymbol());
									cemeteryDeck.addCard(currCard);
									handCardIt.remove();
								}
							}
						}
					}
				} else {
					System.out.println("No more picking attacks allowed");
				}
				setUpdateState(true);
			};
		});
		
		
		//add cemetery deck listener 
		cemeteryDeck.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Cemetery");
				if (currentPlayer.getSelectedHandCards().size() > 0) {
					Iterator<Card> handCardIt = currentPlayer.getHandCards().iterator();
					while (handCardIt.hasNext()) {
						Card currCard = handCardIt.next();
						if (currCard.isSelected()) {
							System.out.println("Remove handcard " + currCard.getStrength());
							cemeteryDeck.addCard(currCard);
							//if joker, get hero
							if (currCard.getSymbol() == "joker") {
								System.out.println("Get hero");
								Card heroCard = cardDeck.getCard(cemeteryDeck);
								System.out.println("Hero card is " + heroCard.getStrength());
								Hero hero = heroesSquare.getHero(heroCard.getStrength());
								if (hero != null) {
									currentPlayer.addHero(hero);
								}
								cemeteryDeck.addCard(heroCard);
							}
							handCardIt.remove();
							setUpdateState(true);
						}
					}
				}
			}
		});
		
		//start round
		throwDices();

	}
	
	//for tests
	public void doRandomSetup(int numPlayers) {
		for (int i = 0; i < numPlayers; i++) {
			//set kingCard
			players.get(i).setKingCard(players.get(i).getLastHandCard());
			
			//add defCards
			for (int j = 1; j <= 3; j++) {
				Card defCard = players.get(i).getLastHandCard();
				boolean isSelected = defCard.isSelected();
				
				defCard.setCovered(true);
				players.get(i).addDefCard(j, defCard);
			}
			
			//add heroes
			Hero hero1 = heroesSquare.getHero(2*i+2);
			Hero hero2 = heroesSquare.getHero(2*i+3);
			players.get(i).addHero(hero1);
			players.get(i).addHero(hero2);
			
			for (int j = 0; j < 2; j++) {
				//cemeteryDeck.addCard(players.get(i).getLastHandCard());
			}
		}
	}
	
	public void throwDices() {
		//throw dice
		for (int i = 0; i < players.size(); i++) {
			players.get(i).throwDice();
		}
		
		//create order
		Collections.copy(roundOrder, players);
		for (int i = 1; i < players.size(); i++) {
			for (int j = i; j > 0; j--) {
				if (roundOrder.get(j).getDiceNumber() > roundOrder.get(j-1).getDiceNumber()) {
					Collections.swap(roundOrder, j, j-1);
				}
			}
		}
		currentPlayerIt = roundOrder.iterator();
		currentPlayer = currentPlayerIt.next();
		
	}
	
	public ArrayList<Player> getPlayers() {
		return players;
	}
	
	public Player getCurrentPlayer() {
		return currentPlayer;
	}
	
	public Player getNextPlayer() {
		Player nextPlayer;
		if (currentPlayerIt.hasNext()) {
			nextPlayer = currentPlayerIt.next();
		} else {
			roundNumber ++;
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
