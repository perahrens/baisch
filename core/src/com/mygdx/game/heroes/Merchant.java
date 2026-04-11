package com.mygdx.game.heroes;

public class Merchant extends Hero {
  private int trades;

  public Merchant() {
    heroName = "Merchant";
    heroID = "Merch.";
    sprite = atlas.createSprite("wq", -1);

    trades = 1;

    isSelectable = true;
    isSelected = false;

    setWidth(sprite.getWidth() * scaleFactor);
    setHeight(sprite.getHeight() * scaleFactor);

  }

  /*
   * if hero is selected and handcard is selected cast away handcard and get new
   * handcard from deck decrement number of trades if last trade, then make card
   * visible for all players
   */
  public void trade() {
    trades--;
    if (trades == 0) {
      isSelectable = false;
    }
  }

  public int getTrades() {
    return trades;
  }

  public void setTrades(int trades) {
    this.trades = Math.max(0, trades);
    if (this.trades > 0) {
      isSelectable = true;
    } else {
      isSelectable = false;
      isSelected = false;
    }
  }

  @Override
  public void recover() {
    trades = 1;
    isSelectable = true;
  }

}
