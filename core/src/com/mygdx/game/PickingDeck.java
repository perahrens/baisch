package com.mygdx.game;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class PickingDeck extends Actor {
	Sprite sprite;
	ArrayList<Card> cards = new ArrayList<Card>();
	
	public PickingDeck() {
		sprite = new Sprite();
		
		setWidth(200);
		setHeight(200);

		/*addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("select picking deck");
				cards.get(cards.size()-1).uncoverCard();
			};
		});*/
	}
	
	public void addCard(Card card) {
		cards.add(card);
	}
	
	public ArrayList<Card> getCards() {
		return this.cards;
	}
}
