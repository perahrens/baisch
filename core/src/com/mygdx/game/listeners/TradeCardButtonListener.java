package com.mygdx.game.listeners;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

public class TradeCardButtonListener extends ClickListener {

  Card tradeableCard;
  Player player;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;
  GameState gameState;
  Socket socket;
  int playerIdx;

  public TradeCardButtonListener(Card tradeableCard, Player player, CardDeck cardDeck, CardDeck cemeteryDeck,
      GameState gameState, Socket socket, int playerIdx) {
    this.tradeableCard = tradeableCard;
    this.player = player;
    this.cardDeck = cardDeck;
    this.cemeteryDeck = cemeteryDeck;
    this.gameState = gameState;
    this.socket = socket;
    this.playerIdx = playerIdx;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    // Move the 1st drawn card (tradeable) to cemetery — visible to all via stateUpdate
    int firstCardId = tradeableCard.getCardId();
    player.getHandCards().remove(tradeableCard);
    cemeteryDeck.addCard(tradeableCard);
    tradeableCard.setTradable(false);

    // Draw 2nd replacement card
    Card secondCard = cardDeck.getCard(cemeteryDeck);
    boolean isJoker = "joker".equals(secondCard.getSymbol());
    if (isJoker) {
      // Joker on 2nd try: discarded to cemetery (not kept)
      cemeteryDeck.addCard(secondCard);
    } else {
      player.addHandCard(secondCard);
    }

    // Emit merchantSecondTry — server reveals 2nd drawn card to all players
    if (socket != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("playerIdx", playerIdx);
        data.put("firstCardId", firstCardId);
        data.put("secondCardId", secondCard.getCardId());
        data.put("isJoker", isJoker);
        socket.emit("merchantSecondTry", data);
      } catch (JSONException e) { e.printStackTrace(); }
    }

    if (gameState != null) gameState.setUpdateState(true);
  }

}
