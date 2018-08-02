package com.mygdx.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.heroes.Hero;
import com.mygdx.heroes.Magician;
import com.mygdx.heroes.Merchant;
import com.mygdx.heroes.Spy;

//public class GameScreen extends AbstractScreen {
public class GameScreen extends ScreenAdapter {
	
	private GameState gameState;
	
	InputMultiplexer inMulti;
	InputProcessor inProTop;
	InputProcessor inProBottom;
	
	private FitViewport fitVPGame;
	private FitViewport fitVPHand;
	
	private Stage gameStage;
	private Stage handStage; 
	
	private SpriteBatch batch;
	private Label myPlayerLabel;
	
	private Label roundCounter;
	
	private TextButton finishTurn;
	
	public GameScreen(Game game) {
		//init game
		gameState = new GameState(4,8);
		batch = new SpriteBatch();
		
	}

	@Override
	public void show() {
		System.out.println("show()");
		
		Player currentPlayer = gameState.getCurrentPlayer();
		
		//create stages and input handlers
		gameStage = new Stage();
		fitVPGame = new FitViewport(Gdx.graphics.getWidth(),Gdx.graphics.getWidth());
		gameStage.setViewport(fitVPGame);
		
		handStage = new Stage();
		fitVPHand = new FitViewport(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()-Gdx.graphics.getWidth());
		handStage.setViewport(fitVPHand);
		
		inMulti = new InputMultiplexer();
		inMulti.addProcessor(gameStage);
		inMulti.addProcessor(handStage);
		Gdx.input.setInputProcessor(inMulti);
				
		//create stage backgrounds
		Image gameBck = new Image(MyGdxGame.skin, "white");
		gameBck.setFillParent(true);
		gameBck.setColor(0.85f, 0.73f, 0.55f, 1);
		gameStage.addActor(gameBck);
		
		Image handBck = new Image(MyGdxGame.skin, "white");
		handBck.setFillParent(true);
		handBck.setColor(1f,1f,1f,0.5f);
		handStage.addActor(handBck);
		
		
		ArrayList<Player> players = gameState.getPlayers();
		
		showGameStage(players, currentPlayer);
		showHandStage(players, currentPlayer);
		
	}
	
	public void showGameStage (ArrayList<Player> players, Player currentPlayer) {
		Card infoCard = new Card();
		
		//draw round number
		roundCounter = new Label("Round " + gameState.getRoundNumber(), MyGdxGame.skin);
		roundCounter.setColor(0f, 0f, 0f, 1.0f);
		roundCounter.setPosition(0, Gdx.graphics.getWidth()-roundCounter.getHeight());
		gameStage.addActor(roundCounter);

		//draw card deck and cemetery
		ArrayList<Card> deckCards = gameState.getCardDeck().getCards();
		for (int i = 0; i < deckCards.size(); i++) {
			Card deckCard = deckCards.get(i);
			deckCard.setDeckPosition();
			deckCard.setY(deckCard.getY()+i*0.3f);
			gameStage.addActor(deckCard);
		}
		
		CardDeck cemeteryDeck = gameState.getCemeteryDeck();
		ArrayList<Card> cemeteryCards = gameState.getCemeteryDeck().getCards();
		for (int i = 0; i < cemeteryCards.size(); i++) {
			Card cemeteryCard = cemeteryCards.get(i);
			cemeteryCard.setCemeteryPosition();
			cemeteryCard.setY(cemeteryCard.getY()+i*0.3f);
			gameStage.addActor(cemeteryCard);
		}
		cemeteryDeck.setRotation(45);
		cemeteryDeck.setWidth(infoCard.getDefWidth());
		cemeteryDeck.setHeight(infoCard.getDefHeight());
		cemeteryDeck.setX(MyGdxGame.WIDTH/2f-infoCard.getDefWidth()/2f);
		cemeteryDeck.setY(MyGdxGame.WIDTH/2f);
		gameStage.addActor(cemeteryDeck);
		
		//draw picking decks
		ArrayList<PickingDeck> pickingDecks = gameState.getPickingDecks();
		for (int i = 0; i < pickingDecks.size(); i++) {
			ArrayList<Card> pickingCards = pickingDecks.get(i).getCards();
			for (int j = 0; j < pickingCards.size(); j++) {
				pickingCards.get(j).setX(MyGdxGame.WIDTH/2-pickingCards.get(j).getDefWidth()/2+(2*i-1)*0.8f*pickingCards.get(j).getDefWidth());
				pickingCards.get(j).setY(MyGdxGame.WIDTH/2-pickingCards.get(j).getDefHeight()/2+(2*i-1)*0.8f*pickingCards.get(j).getDefWidth());
				pickingCards.get(j).setRotation(45);
				//shift offset
				pickingCards.get(j).setX(pickingCards.get(j).getX() + 0.1f*(pickingCards.size()-1)*pickingCards.get(j).getDefHeight());
				pickingCards.get(j).setY(pickingCards.get(j).getY() - 0.1f*(pickingCards.size()-1)*pickingCards.get(j).getDefHeight());
				//shift for each card
				pickingCards.get(j).setX(pickingCards.get(j).getX() - 0.2f*(j)*pickingCards.get(j).getDefHeight());
				pickingCards.get(j).setY(pickingCards.get(j).getY() + 0.2f*(j)*pickingCards.get(j).getDefHeight());
				gameStage.addActor(pickingCards.get(j));
			}
			pickingDecks.get(i).setX(MyGdxGame.WIDTH/2-pickingCards.get(0).getDefWidth()/2+(2*i-1)*0.8f*pickingCards.get(0).getDefWidth());
			pickingDecks.get(i).setY(MyGdxGame.WIDTH/2-pickingCards.get(0).getDefHeight()/2+(2*i-1)*0.8f*pickingCards.get(0).getDefWidth());
			pickingDecks.get(i).setWidth(pickingCards.get(0).getDefWidth());
			pickingDecks.get(i).setHeight(pickingCards.get(0).getDefHeight());
			pickingDecks.get(i).setRotation(45);
			gameStage.addActor(pickingDecks.get(i));
		}
		
		//draw game status of players
		for (int i = 0; i < players.size(); i++) {
			System.out.println("Player " + players.get(i).getPlayerName() + " hand = " + players.get(i).getHandCards().size() );
			System.out.println("Player " + players.get(i).getPlayerName() + " def = " + players.get(i).getDefCards().size() );
			
			//display dice
			Dice dice = players.get(i).getDice();
			dice.setMapPosition(i);
			gameStage.addActor(dice);
			
			//display hand cards
			ArrayList<Card> handCards = players.get(i).getHandCards();
			for (int j = 0; j < handCards.size(); j++) {
				handCards.get(j).setCovered(true);
				handCards.get(j).setRotation(0);
				handCards.get(j).setActive(false);
				handCards.get(j).setSelected(false);
				handCards.get(j).setSize(handCards.get(j).getDefWidth(), handCards.get(j).getDefHeight());
				handCards.get(j).setPosition(dice.getX(), dice.getY());
			
				switch (i) {
						case 0:
							handCards.get(j).setPosition(handCards.get(j).getX()+1.5f*dice.getWidth()+j*5f, handCards.get(j).getY());
							break;
						case 1:
							handCards.get(j).setRotation(-90);
							handCards.get(j).setPosition(handCards.get(j).getX(), handCards.get(j).getY()-2f*dice.getWidth()-j*5f);
							break;
						case 2:
							handCards.get(j).setRotation(-180);
							handCards.get(j).setPosition(handCards.get(j).getX()-2f*dice.getWidth()-j*5f, handCards.get(j).getY()-dice.getWidth());
							break;
						case 3:
							handCards.get(j).setRotation(90);
							handCards.get(j).setPosition(handCards.get(j).getX()-dice.getWidth(), handCards.get(j).getY()+dice.getWidth()+j*5f);
							break;
						default:
							break;
						}
				
					gameStage.addActor(handCards.get(j));
			}
			
			//display king cards
			final Card kingCard = players.get(i).getKingCard();
			kingCard.setMapPosition(i, 0);
			//make own covered cards visible
			if (players.get(i) == currentPlayer) {
				kingCard.setActive(true);
			} else {
				kingCard.setActive(false);
			}
			
			Array<EventListener> kingListeners = kingCard.getListeners();
			for (EventListener listener : kingListeners) {
				kingCard.removeListener(listener);
			}
			
			kingCard.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (gameState.getCurrentPlayer().getSelectedHeroes().size() > 0) {
						for (int i = 0; i < gameState.getCurrentPlayer().getHeroes().size(); i++) {
							if (gameState.getCurrentPlayer().getHeroes().get(i).getHeroName() == "Spy" &&
									gameState.getCurrentPlayer().getHeroes().get(i).isSelected() ) {
								Spy spy = (Spy) gameState.getCurrentPlayer().getHeroes().get(i);
								//check if all def cards are uncovered
								//first find player
								System.out.println("this player will be ");
								Player targetPlayer = gameState.getPlayers().get(0);;
								for (int j = 0; i < gameState.getPlayers().size(); i++) {
									if (gameState.getPlayers().get(j).getKingCard() == kingCard) {
										System.out.println("this player " + gameState.getPlayers().get(j).getPlayerName());
										targetPlayer = gameState.getPlayers().get(j);
									}
								}
								
								boolean allUncovered = true;
								for (int j = 1; j <= 3; j++) {
									if (targetPlayer.getDefCards().containsKey(j)) {
										if (targetPlayer.getDefCards().get(j).isCovered()) {
											allUncovered = false;
										}
									}
								}
								
								if (spy.getSpyAttacks() > 0 && allUncovered) {
									spy.spyAttack();
									System.out.println("Number spy attacks left = " + spy.getSpyAttacks());
									kingCard.setCovered(false);
								}
							}
						}
					}
				}}
			);
			
			gameStage.addActor(kingCard);

			//display defense cards and placeholders
			Map<Integer, Card> defCards = players.get(i).getDefCards();
			for (int j = 1; j <= 3; j++) {
				final Card defCard;
				if (defCards.containsKey(j))  {
					defCard = defCards.get(j);
					defCard.setPlaceholder(false);
					if (players.get(i) != currentPlayer) {
						Array<EventListener> listeners = defCard.getListeners();
						for (EventListener listener : listeners) {
							defCard.removeListener(listener);
						}
						defCard.addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								if (gameState.getCurrentPlayer().getSelectedHeroes().size() > 0) {
									for (int i = 0; i < gameState.getCurrentPlayer().getHeroes().size(); i++) {
										if (gameState.getCurrentPlayer().getHeroes().get(i).getHeroName() == "Spy" &&
												gameState.getCurrentPlayer().getHeroes().get(i).isSelected() ) {
											Spy spy = (Spy) gameState.getCurrentPlayer().getHeroes().get(i);
											if (spy.getSpyAttacks() > 0) {
												spy.spyAttack();
												System.out.println("Number spy attacks left = " + spy.getSpyAttacks());
												defCard.setCovered(false);
											}
										}
									}
								}
								
								if (gameState.getCurrentPlayer().getSelectedHandCards().size() > 0) {
									if (gameState.getCurrentPlayer().getPlayerTurn().getAttackingSymbol() == "none" || 
											gameState.getCurrentPlayer().getPlayerTurn().getAttackingSymbol() == gameState.getCurrentPlayer().getSelectedHandCards().get(0).getSymbol()) {
										gameState.getCurrentPlayer().getPlayerTurn().setAttackingSymbol(gameState.getCurrentPlayer().getSelectedHandCards().get(0).getSymbol());
										defCard.setCovered(false);
										boolean attackSuccess = gameState.getCurrentPlayer().attackEnemyDefense(defCard);

										//selected hand cards to cemetery deck
										Iterator<Card> handCardIt = gameState.getCurrentPlayer().getHandCards().iterator();
										while (handCardIt.hasNext()) {
											Card currCard = handCardIt.next();
											if (currCard.isSelected()) {
												gameState.getCemeteryDeck().addCard(currCard);
												handCardIt.remove();
											}
										}
										
										if (attackSuccess) {
											gameState.getCurrentPlayer().addHandCard(defCard);
											defCard.setRemoved(true);
										}
										
										gameState.setUpdateState(true);
									}
								}
							}
						});
					} else {
						Array<EventListener> listeners = defCard.getListeners();
						for (EventListener listener : listeners) {
							defCard.removeListener(listener);
						}
						
						final Card refCard = defCard;
						
						defCard.addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
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
								}
							};
						});
					}
				} else {
					//create placeholder card
					defCard = new Card();;
					
					Array<EventListener> listeners = defCard.getListeners();
					for (EventListener listener : listeners) {
						defCard.removeListener(listener);
					}
					
					//listener for placeholder card
					if (players.get(i) == currentPlayer) {
						defCard.addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								//get selected hand cards
								if (gameState.getCurrentPlayer().getSelectedHandCards().size() == 1) {
									if (gameState.getCurrentPlayer().getPlayerTurn().getPutDefCard() > 0) {
										gameState.getCurrentPlayer().putDefCard(defCard.getPositionId());
										show();
									} else {
										System.out.println("no more put allowed");
									}
								} else {
									System.out.println("Select only one handcard");
								}
							}
						});
					}
				}
				
				if (defCard.getRemoved()) {
					players.get(i).getDefCards().remove(j);
					System.out.println("Def card removed!");
				}
				
				defCard.setMapPosition(i, j);
				if (players.get(i) == currentPlayer) {
					defCard.setActive(true);
				} else {
					defCard.setActive(false);
				}
				gameStage.addActor(defCard);
			}
			
			//display myPlayerLabel
			Label playerLabel = new Label(players.get(i).getPlayerName(), MyGdxGame.skin);
			playerLabel.setColor(0f, 0f, 0f, 1.0f);
			switch (i) {
			case 0:
				playerLabel.setPosition((MyGdxGame.WIDTH-playerLabel.getWidth())/2-kingCard.getDefHeight(), kingCard.getDefWidth()/2);
				break;
			case 1:
				playerLabel.setPosition(0, (MyGdxGame.WIDTH-playerLabel.getHeight())/2+kingCard.getDefWidth());
				break;
			case 2:
				playerLabel.setPosition((MyGdxGame.WIDTH-playerLabel.getWidth())/2+kingCard.getDefHeight(), MyGdxGame.WIDTH-playerLabel.getHeight()-kingCard.getDefWidth()/2);
				break;
			case 3:
				playerLabel.setPosition(MyGdxGame.WIDTH-playerLabel.getWidth(), (MyGdxGame.WIDTH-playerLabel.getHeight())/2-kingCard.getDefWidth());
				break;
			default:
				break;
			}
			
			//display heroes
			ArrayList<Hero> playerHeroes = players.get(i).getHeroes();
			for (int j = 0; j < playerHeroes.size(); j++) {
				playerHeroes.get(j).setSelected(false);
				playerHeroes.get(j).setHand(false);
				playerHeroes.get(j).setPosition(playerLabel.getX(), playerLabel.getY());
				
				switch (i) {
					case 0:
						playerHeroes.get(j).setPosition(playerHeroes.get(j).getX()-playerLabel.getWidth()-j*playerHeroes.get(j).getWidth()/3, playerHeroes.get(j).getY()-playerHeroes.get(j).getHeight()/4);
						break;
					case 1:
						playerHeroes.get(j).setPosition(playerHeroes.get(j).getX(), playerHeroes.get(j).getY()+j*playerHeroes.get(j).getHeight()/3);
						break;
					case 2:
						playerHeroes.get(j).setPosition(playerHeroes.get(j).getX()+playerLabel.getWidth()+j*playerHeroes.get(j).getWidth()/3, MyGdxGame.WIDTH-playerHeroes.get(j).getHeight());
						break;
					case 3:
						playerHeroes.get(j).setPosition(playerHeroes.get(j).getX(), playerHeroes.get(j).getY()-2*playerLabel.getHeight()-j*playerHeroes.get(j).getHeight()/3);
						break;
					default:
						break;
					}
				
				gameStage.addActor(playerHeroes.get(j));
			}
			
			gameStage.addActor(playerLabel);
		}
	}
	
	public void showHandStage (ArrayList<Player> players, Player currentPlayer) {
		//draw hand status of active player
		for (int i = 0; i < players.size(); i++) {
			ArrayList<Card> handCards = players.get(i).getHandCards();
			for (int j = 0; j < handCards.size(); j++) {
				
				final Card refCard = handCards.get(j);
				
				Array<EventListener> listeners = handCards.get(j).getListeners();
				for (EventListener listener : listeners) {
					handCards.get(j).removeListener(listener);
				}
				
				handCards.get(j).addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						//unselect all defense and king cards
						gameState.getCurrentPlayer().getKingCard().setSelected(false);
						for (int i = 1; i <= 3; i++) {
							if (gameState.getCurrentPlayer().getDefCards().containsKey(i)) {
								gameState.getCurrentPlayer().getDefCards().get(i).setSelected(false);
							}
						}
						
						//select hand card
						if (refCard.isSelected()) {
							refCard.setSelected(false);
						} else {
							if (refCard.getSymbol() == gameState.getCurrentPlayer().getSelectedSymbol()) {
								refCard.setSelected(true);
							} else {
								for (int i = 0; i < gameState.getCurrentPlayer().getHandCards().size(); i++) {
									gameState.getCurrentPlayer().getHandCards().get(i).setSelected(false);
								}
								refCard.setSelected(true);
								gameState.getCurrentPlayer().setSelectedSymbol(refCard.getSymbol());
							}
						}
						
						//check hero functions on hand cards
						if (gameState.getCurrentPlayer().getSelectedHeroes().size() > 0) {
							for (int i = 0; i < gameState.getCurrentPlayer().getHeroes().size(); i++) {
								//if spy is selected, cast card away
								if (gameState.getCurrentPlayer().getHeroes().get(i).getHeroName() == "Spy" &&
										gameState.getCurrentPlayer().getHeroes().get(i).isSelected()) {
									Spy spy = (Spy) gameState.getCurrentPlayer().getHeroes().get(i);
									if (gameState.getCurrentPlayer().getSelectedHandCards().size() == 1 &&
											spy.getSpyExtends() > 0) {
										//cast away selected card
										Iterator<Card> handCardIt = gameState.getCurrentPlayer().getHandCards().iterator();
										while (handCardIt.hasNext()) {
											Card currCard = handCardIt.next();
											if (currCard.isSelected()) {
												System.out.println("Remove handcard " + currCard.getStrength());
												gameState.getCemeteryDeck().addCard(currCard);
												handCardIt.remove();
											}
										}
										
										//extends spy attacks
										spy.spyExtend();
									}
								} else if (gameState.getCurrentPlayer().getHeroes().get(i).getHeroName() == "Merchant" &&
										gameState.getCurrentPlayer().getHeroes().get(i).isSelected()) {
									Merchant merchant  = (Merchant) gameState.getCurrentPlayer().getHeroes().get(i);
									if (gameState.getCurrentPlayer().getSelectedHandCards().size() == 1 &&
											merchant.getTrades() > 0) {
										Iterator<Card> handCardIt = gameState.getCurrentPlayer().getHandCards().iterator();
										while (handCardIt.hasNext()) {
											Card currCard = handCardIt.next();
											if (currCard.isSelected()) {
												System.out.println("Remove handcard " + currCard.getStrength());
												gameState.getCemeteryDeck().addCard(currCard);
												handCardIt.remove();
											}
										}
										
										//get new card from deck
										merchant.trade();
										gameState.getCurrentPlayer().addHandCard(gameState.getCardDeck().getCard(gameState.getCemeteryDeck()));
										
										//pop up dialog whether to keep or not
										/*
										boolean keepCard = false;
										if (!keepCard) {
											
										}*/
									}
								}
							}
							gameState.setUpdateState(true);
						}
					};
				});
				
				handCards.get(j).setActive(false);
				handCards.get(j).setSelected(false);
			}
		}
			
		//draw heroes and handcards only for current player
		ArrayList<Card> handCards = currentPlayer.getHandCards();
		ArrayList<Hero> playerHeroes = currentPlayer.getHeroes();
		currentPlayer.sortHandCards();
		for (int j = 0; j < handCards.size(); j++) {
			handCards.get(j).setCovered(false);
			handCards.get(j).setActive(true);
			handCards.get(j).setRotation(0);
			handCards.get(j).setWidth(handCards.get(j).getDefWidth()*2);
			handCards.get(j).setHeight(handCards.get(j).getDefHeight()*2);
			if (j < 5) {
				handCards.get(j).setX(j*handCards.get(j).getWidth());
				handCards.get(j).setY(MyGdxGame.WIDTH/2);
			} else {
				handCards.get(j).setX((j-5)*handCards.get(j).getWidth());
				handCards.get(j).setY(MyGdxGame.WIDTH/2-handCards.get(j).getHeight());
			}
			handStage.addActor(handCards.get(j));
		}
		
		//display all heroes of current player
		for (int j = 0; j < playerHeroes.size(); j++) {
			final Hero refHero = playerHeroes.get(j); 
			
			playerHeroes.get(j).setHand(true);
			playerHeroes.get(j).setPosition(j*playerHeroes.get(j).getWidth(), 0);
			
			Array<EventListener> listeners = playerHeroes.get(j).getListeners();
			for (EventListener listener : listeners) {
				playerHeroes.get(j).removeListener(listener);
			}
			
			playerHeroes.get(j).addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (refHero.isReady() && refHero.isSelectable()) {
						System.out.println("Hero isSelected=" + refHero.isSelected());
						//unselect all defense and king cards
						gameState.getCurrentPlayer().getKingCard().setSelected(false);
						for (int i = 1; i <= 3; i++) {
							if (gameState.getCurrentPlayer().getDefCards().containsKey(i)) {
								gameState.getCurrentPlayer().getDefCards().get(i).setSelected(false);
							}
						}
						
						//unselect all hand cards
						for (int i = 0; i < gameState.getCurrentPlayer().getHandCards().size(); i++) {
							gameState.getCurrentPlayer().getHandCards().get(i).setSelected(false);
						}
						
						//select current hero
						if (refHero.isSelected()) {
							//unselect selected hero
							refHero.setSelected(false);
						} else {
							//unselect all other heroes and only select new one
							for (int i = 0; i < gameState.getCurrentPlayer().getHeroes().size(); i++) {
								gameState.getCurrentPlayer().getHeroes().get(i).setSelected(false);
							}
							refHero.setSelected(true);
						}
						System.out.println("Hero  isSelected=" + refHero.isSelected());
					}
				};
			});
			
			Label heroLabel = new Label(playerHeroes.get(j).getHeroID(), MyGdxGame.skin);
			heroLabel.setPosition(j*playerHeroes.get(j).getWidth()+(playerHeroes.get(j).getWidth()-heroLabel.getWidth())/2, playerHeroes.get(j).getHeight());
			
			handStage.addActor(playerHeroes.get(j));
			handStage.addActor(heroLabel);
		}
		
		//turn info and button
		finishTurn = new TextButton("Finish turn", MyGdxGame.skin);
		finishTurn.setPosition(Gdx.graphics.getWidth()-finishTurn.getWidth(), 0);
		myPlayerLabel = new Label(currentPlayer.getPlayerName(), MyGdxGame.skin);
		myPlayerLabel.setPosition(Gdx.graphics.getWidth()-myPlayerLabel.getWidth(), finishTurn.getHeight());
		finishTurn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Player currentPlayer = gameState.getNextPlayer();
				System.out.println("Next player " + currentPlayer.getPlayerName());
				show();
			};
		});
		handStage.addActor(myPlayerLabel);
		
		//add attacking symbol
		String attackingSymbol = currentPlayer.getPlayerTurn().getAttackingSymbol();
		Texture symbolTexture;
		TextureRegion symbolRegion;
		
		if (attackingSymbol == "hearts") {
			symbolTexture =  new Texture(Gdx.files.internal("data/skins/hearts.png"));
			symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
		} else if (attackingSymbol == "diamonds") {
			symbolTexture =  new Texture(Gdx.files.internal("data/skins/diamonds.png"));
			symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
		} else if (attackingSymbol == "clubs") {
			symbolTexture =  new Texture(Gdx.files.internal("data/skins/clubs.png"));
			symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
		} else if (attackingSymbol == "spades") {
			symbolTexture =  new Texture(Gdx.files.internal("data/skins/spades.png"));
			symbolRegion = new TextureRegion(symbolTexture, 0, 0, 512, 512);
		} else {
			symbolTexture = new Texture(Gdx.files.internal("data/skins/someSymbol.png"));
			symbolRegion = new TextureRegion(symbolTexture, 0, 0, 342, 512);
		}

		Image symbolImage = new Image(symbolRegion);
		symbolImage.setBounds(symbolImage.getX(), symbolImage.getY(), symbolImage.getWidth()/10f, symbolImage.getHeight()/10f);
		symbolImage.setPosition(Gdx.graphics.getWidth()-symbolImage.getWidth(), finishTurn.getHeight()+myPlayerLabel.getHeight());
		handStage.addActor(symbolImage);
		
		//add hand image
		Texture handTexture = new Texture(Gdx.files.internal("data/skins/hand.png"));
		TextureRegion handRegion = new TextureRegion(handTexture, 0, 0, 512, 512);
		Image handImage = new Image(handRegion);
		handImage.setBounds(handImage.getX(), handImage.getY(), handImage.getWidth()/5f, handImage.getHeight()/5f);
		handImage.setPosition(Gdx.graphics.getWidth()-(myPlayerLabel.getWidth()+handImage.getWidth()), 0);
		
		Array<EventListener> listeners = handImage.getListeners();
		for (EventListener listener : listeners) {
			handImage.removeListener(listener);
		}
		
		handImage.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Map<Integer, Card> defCards = gameState.getCurrentPlayer().getDefCards();
				for (int j = 1; j <= 3; j++) {
					if (defCards.containsKey(j)) {
						if (defCards.get(j).isSelected()) {
							gameState.getCurrentPlayer().takeDefCard(j);
						}
					}
				}
				show();
			};
		});
		
		handStage.addActor(handImage);
		handStage.addActor(finishTurn);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT); 
		
		//check if gameState has changed
		if (gameState.getUpdateState()) {
			gameState.setUpdateState(false);
			show();
		}
		
		/*Upper division*/
			Gdx.gl.glViewport( 0,Gdx.graphics.getHeight()-Gdx.graphics.getWidth(),Gdx.graphics.getWidth(),Gdx.graphics.getWidth());
			gameStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getWidth(), true);
			gameStage.getViewport().setScreenBounds(0, Gdx.graphics.getHeight()-Gdx.graphics.getWidth(),Gdx.graphics.getWidth(),Gdx.graphics.getWidth());
			gameStage.getViewport().apply();
			gameStage.act(delta);
			gameStage.draw( );
			
		batch.begin();
		//playerName.setColor(0f, 0f, 0f, 1.0f);
		//playerName.draw(batch, "Player 1", Gdx.graphics.getWidth()/2f-25, playerName.getLineHeight());
			batch.end();

			/*Lower division*/
			Gdx.gl.glViewport( 0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()-Gdx.graphics.getWidth());
			handStage.getViewport().update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()-Gdx.graphics.getWidth(), true);
			handStage.getViewport().setScreenBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()-Gdx.graphics.getWidth());
			handStage.getViewport().apply();
			handStage.act(delta);
			handStage.draw();
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
