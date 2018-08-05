package com.mygdx.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class OwnDefCardListener extends ClickListener {

	public OwnDefCardListener() {
		System.out.println("created OwnDefCardListener");
	}
	
	@Override
	public void clicked(InputEvent event, float x, float y) {
		
		System.out.println("clicked OwnDefCardListener");
		/*
		//unselect all handcards
		for (int i = 0; i < gameState.getCurrentPlayer().getHandCards().size(); i++) {
			gameState.getCurrentPlayer().getHandCards().get(i).setSelected(false);
		}
		
		//select defense card
		if (refCard.isSelected()) {
			refCard.setSelected(false);
		} else {
			gameState.getCurrentPlayer().getKingCard().setSelected(false);
			for (int i = 1; i <= 3; i++) {
				if (gameState.getCurrentPlayer().getDefCards().containsKey(i)) {
					gameState.getCurrentPlayer().getDefCards().get(i).setSelected(false);
				}
			}
			refCard.setSelected(true);
		}*/
	};
	
}
