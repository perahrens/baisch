package com.mygdx.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

public class CardDeck extends Actor {
	
	Sprite sprite;

	ArrayList<Card> cards;
	final String[] symbols = {"clubs", "diamonds", "hearts", "spades"};
	
	public CardDeck() {
		sprite = new Sprite();
		
		setWidth(200);
		setHeight(200);
		
		//create array with all cards
		cards = new ArrayList<Card>();
		int k = 0;
		for (int i = 0; i<symbols.length; i++) {
			for (int j = 1; j<=13; j++) {
				cards.add(new Card(symbols[i], j));
				k++;
			}
		}
		//add 3 joker cards
		for (int i = 1; i <= 3; i++) {
			cards.add(new Card("joker", i));
		}
		
		//shuffle cards
		Collections.shuffle(cards);
	}
	
	public void refillCardDeck(CardDeck cemeteryDeck) {
		//create array with all cards
		Iterator<Card> cemeteryCardIt = cemeteryDeck.getCards().iterator();
		while (cemeteryCardIt.hasNext()) {
			cards.add(cemeteryCardIt.next());
			cemeteryCardIt.remove();
		}
		
		//set status of all cards correctly
		for (int i = 0; i < cards.size(); i++) {
			cards.get(i).setActive(false);
			cards.get(i).setCovered(true);
			cards.get(i).setSelected(false);
		}
		
		//shuffle cards
		Collections.shuffle(cards);
		
	}
	
	public CardDeck(boolean isEmpty) {
		if (isEmpty) {
			cards = new ArrayList<Card>();
		} else {
			new CardDeck();
		}
	}
	
	//TODO rename to pickCard
	public Card getCard(CardDeck cemeteryDeck) {
		//if is empty, refill
		if (cards.size() < 1) {
			if (cemeteryDeck.getCards().size() >= 1) {
				System.out.println("refill card deck with cemetery deck");
				refillCardDeck(cemeteryDeck);
			} else {
				System.out.println("NO more cards available");
			}
			
		}
		Card card = cards.get(cards.size()-1);
		cards.remove(cards.size()-1);
		System.out.println("get card size after " + cards.size());
		return card;
	}
	
	public ArrayList<Card> getCards() {
		return this.cards;
	}
	
	public void addCard(Card card) {
		Array<EventListener> listeners = card.getListeners();
		for (EventListener listener : listeners) {
			card.removeListener(listener);
		}

		card.setCovered(false);
		cards.add(card);
	}
	
}
