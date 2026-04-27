package com.mygdx.game;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.heroes.Mercenaries;

public class Card extends Actor {
  Sprite sprite;
  private boolean spriteIsBack = false;
  static TextureAtlas atlas = new TextureAtlas("data/skins/carddeck.atlas");
  static Texture[] jokerTextures = {
    new Texture("data/skins/Joker1.jpg"),
    new Texture("data/skins/Joker2.jpg"),
    new Texture("data/skins/Joker3.jpg")
  };
  // Reference dimensions from the atlas so joker sprites match regular cards exactly.
  private static final float CARD_ATLAS_WIDTH = atlas.createSprite("back", 1).getWidth();
  private static final float CARD_ATLAS_HEIGHT = atlas.createSprite("back", 1).getHeight();

  // card attributes
  private String symbol = null;
  private Integer index = null;
  public void setIndex(int idx) {
    this.index = idx;
  }

  public Integer getIndex() {
    return this.index;
  }

  // location attributes
  private int positionId;
  private int level = 0; // (0) bottom level is default; (1) top level relevant for fortified tower

  // state attributes
  private boolean isSelected = false;
  private boolean isCovered = false;
  private boolean isActive = false;
  private boolean isPlaceholder = false;
  private boolean isRemoved = false; // temporary information that card should be removed
  // private boolean isSuspended = false; // suspended card can only be used next
  // round
  private boolean isTradable = false; // information that card can be traded again and should be overlaid with
                                      // keep/cast button
  private boolean isSabotaged = false; // information that card is blocked by a saboteur and can not be used by the
                                       // player
  private int boosted = 0; // boosted by mercenary

  // texture attributes
  private float defWidth;
  private float defHeight;
  private Color defColor;
  private final float scaleFactor = 0.22f;
  private float rotate = 0;

  // placeholder card
  public Card() {
    sprite = atlas.createSprite("back", 3);

    symbol = "back";
    index = 3;

    positionId = -1;

    isPlaceholder = true;

    defWidth = sprite.getWidth() * scaleFactor;
    defHeight = sprite.getHeight() * scaleFactor;

    defColor = getColor();

    setWidth(defWidth);
    setHeight(defHeight);
    setBounds(getX(), getY(), getWidth(), getHeight());
  }

  // Factory method: construct a Card from a server card ID (1-55).
  // ID 1-13 = clubs 1-13, 14-26 = diamonds 1-13, 27-39 = hearts 1-13, 40-52 = spades 1-13.
  // ID 53-55 = joker 1-3.
  public static Card fromCardId(int cardId) {
    final String[] suits = { "clubs", "diamonds", "hearts", "spades" };
    if (cardId >= 53 && cardId <= 55) {
      return new Card("joker", cardId - 52); // joker 1, 2, 3
    }
    if (cardId >= 1 && cardId <= 52) {
      int suitIndex = (cardId - 1) / 13;
      int cardIndex = (cardId - 1) % 13 + 1;
      return new Card(suits[suitIndex], cardIndex);
    }
    return new Card(); // fallback placeholder
  }

  // standard playing card
  public Card(String symbol, int index) {
    if ("joker".equals(symbol)) {
      sprite = new Sprite(jokerTextures[index - 1]);
      sprite.setSize(CARD_ATLAS_WIDTH, CARD_ATLAS_HEIGHT);
    } else {
      sprite = atlas.createSprite(symbol, index);
    }

    this.symbol = symbol;
    this.index = index;

    positionId = -1;

    defWidth = sprite.getWidth() * scaleFactor;
    defHeight = sprite.getHeight() * scaleFactor;

    defColor = getColor();

    setWidth(defWidth);
    setHeight(defHeight);
    setBounds(getX(), getY(), getWidth(), getHeight());
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    // Only re-fetch the sprite when the covered+active state demands the card-back sprite,
    // otherwise keep the sprite that was set in the constructor. This avoids allocating a
    // new AtlasSprite on every draw call.
    if (isCovered && !isActive) {
      if (!spriteIsBack) { sprite = atlas.createSprite("back", 1); spriteIsBack = true; }
    } else {
      if (spriteIsBack || sprite == null) {
        if ("joker".equals(symbol)) {
          sprite = new Sprite(jokerTextures[index - 1]);
          sprite.setSize(CARD_ATLAS_WIDTH, CARD_ATLAS_HEIGHT);
        } else {
          sprite = atlas.createSprite(symbol, index);
        }
        spriteIsBack = false;
      }
    }
    Color color = defColor;
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
    if (isPlaceholder) {
      batch.setColor(color.r, color.g, color.b, color.a * parentAlpha * 0.5f);
    } else {
      if (isCovered) {
        if (isActive) {
          // set dark
          batch.setColor(color.r / 1.5f, color.g * 1.5f, color.b / 1.5f, color.a * parentAlpha);
          if (isSelected) {
            // set dark green
            batch.setColor(color.r / 2.0f, color.g, color.b / 2.0f, color.a * parentAlpha);
          } else {
            // set dark
          }
        } else {
          // covered + not active → back sprite (already set at top of method)
          if (isSelected) {
            // not selectable --> attack
          } else {
            // set red back
          }
        }
      } else {
        if (isActive) {
          if (isSelected) {
            // set green
            batch.setColor(color.r / 2.0f, color.g, color.b / 2.0f, color.a * parentAlpha);
          } else {
            // set white
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
          }
        } else {
          // set white
          batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
          if (isSelected) {
            // not selectable --> attack
          } else {
            // set white
          }
        }
      }
    }

    batch.draw(sprite, getX(), getY(), getWidth() / 2f, getHeight() / 2f, getWidth(), getHeight(), getScaleX(), getScaleY(), rotate);
  }

  public boolean isPlaceholder() {
    return isPlaceholder;
  }

  public void setPlaceholder(boolean isPlaceholder) {
    this.isPlaceholder = isPlaceholder;
  }

  public boolean isRemoved() {
    return isRemoved;
  }

  public void setRemoved(boolean isRemoved) {
    this.isRemoved = isRemoved;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public boolean isSelected() {
    return isSelected;
  }

  public String getSymbol() {
    return this.symbol;
  }


  public int getStrength() {
    int strength = index;
    if (index == 1)
      strength = 14;
    if (symbol == "joker")
      strength = 999;
    strength += getBoosted();
    return strength;
  }

  public void setCovered(boolean isCovered) {
    this.isCovered = isCovered;
  }

  public boolean isCovered() {
    return isCovered;
  }

  public void setTradable(boolean isTradeable) {
    this.isTradable = isTradeable;
  }

  public boolean isTradeable() {
    return isTradable;
  }

  public void setSabotaged(boolean isSabotaged) {
    this.isSabotaged = isSabotaged;
  }

  public boolean isSabotaged() {
    return isSabotaged;
  }

  public void setRotation(int rotation) {
    this.rotate = rotation;
  }

  /**
   * Internal rotation used by Card.draw (centered around the card's geometric centre).
   * Distinct from {@link com.badlogic.gdx.scenes.scene2d.Actor#getRotation()} which is
   * not used for board cards. Returned in degrees; 0 = upright, 90/-90 = sideways, 180 = upside-down.
   */
  public float getRotate() {
    return rotate;
  }

  public void addBoosted(int boost) {
    boosted += boost; // can be positive or negative
  }

  public int getBoosted() {
    return boosted;
  }

  // set mercenaries back to owner
  public void unboost(ArrayList<Player> players) {
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).hasHero("Mercenaries")) {
        for (int j = 0; j < players.get(i).getHeroes().size(); j++) {
          if ("Mercenaries".equals(players.get(i).getHeroes().get(j).getHeroName())) {
            Mercenaries mercenaries = (Mercenaries) players.get(i).getHeroes().get(j);
            while (getBoosted() > 0) {
              mercenaries.destroy();
              addBoosted(-1);
            }
          }
        }
      }
    }

  }

  public float getDefWidth() {
    return defWidth;
  }

  public float getDefHeight() {
    return defHeight;
  }

  public void dispose() {
    this.dispose();
  }

  public void removeAllListeners() {
    // The naive `for (EventListener l : getListeners()) removeListener(l)` pattern is
    // broken: libGDX's Array iterator advances by index, so when removeListener shrinks
    // the underlying array each iteration skips one element and roughly half of the
    // listeners survive. Real-world impact: after a coup/Warlord king swap, the new
    // king ended up with two click listeners (the anonymous one from Player.setKingCard
    // plus OwnKingCardListener) that toggled each other — clicks did nothing visible
    // until the next turn (#169). Use Actor.clearListeners() which clears via the
    // underlying DelayedRemovalArray.clear().
    clearListeners();
  }

  public void setDeckPosition() {
    setWidth(defWidth);
    setHeight(defHeight);
    setX(MyGdxGame.WIDTH / 2 - getWidth() / 2 + getHeight() / 2);
    setY(MyGdxGame.WIDTH / 2 - getHeight() / 2 - getHeight() / 2);
    rotate = 45;
    isCovered = true;
    isSelected = false;
  }

  public void setCemeteryPosition() {
    setWidth(defWidth);
    setHeight(defHeight);
    setX(MyGdxGame.WIDTH / 2 - defWidth / 2 - defHeight / 2);
    setY(MyGdxGame.WIDTH / 2);
    rotate = 45;
    isCovered = false;
    isSelected = false;
  }

  public int getPositionId() {
    return positionId;
  }

  public int getCardId() {
    if ("joker".equals(symbol)) return 52 + index;
    final String[] suits = { "clubs", "diamonds", "hearts", "spades" };
    for (int i = 0; i < suits.length; i++) {
      if (suits[i].equals(symbol)) {
        return i * 13 + index;
      }
    }
    return -1;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public void setMapPosition(int player, int position, int level) {
    setWidth(defWidth);
    setHeight(defHeight);
    isSelected = false;
    positionId = position;
    // Small margin from screen edges (kings) and gap between adjacent defense cards.
    // G ≈ 25% of card width gives a comfortable inset without shrinking the play area.
    final float G = defWidth * 0.25f;
    switch (player) {
    case 0:
      switch (position) {
      case 0:
        setX((MyGdxGame.WIDTH - getWidth()) / 2);
        setY(G);
        rotate = 0;
        break;
      default:
        setX((MyGdxGame.WIDTH - getWidth()) / 2 + (position - 2) * (getWidth() + G));
        setY(getHeight() + G);
        setY(getY() + 0.25f * level * getHeight()); // level shift
        rotate = 0;
        break;
      }
      break;
    case 1:
      switch (position) {
      case 0:
        setX((getHeight() - getWidth()) / 2 + G);
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2);
        rotate = -90;
        break;
      default:
        setX((getHeight() - getWidth()) / 2 + getHeight() + G);
        setX(getX() + 0.25f * level * getHeight()); // level shift
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2 + (2 - position) * (getWidth() + G));
        rotate = -90;
        break;
      }
      break;
    case 2:
      switch (position) {
      case 0:
        setX((MyGdxGame.WIDTH - getWidth()) / 2);
        setY(MyGdxGame.WIDTH - getHeight() - G);
        rotate = 180;
        break;
      default:
        setX((MyGdxGame.WIDTH - getWidth()) / 2 + (2 - position) * (getWidth() + G));
        setY(MyGdxGame.WIDTH - 2f * getHeight() - G);
        setY(getY() - 0.25f * level * getHeight()); // level shift
        rotate = 180;
        break;
      }
      break;
    case 3:
      switch (position) {
      case 0:
        setX((getHeight() - getWidth()) / 2 + MyGdxGame.WIDTH - getHeight() - G);
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2);
        rotate = 90;
        break;
      default:
        setX((getHeight() - getWidth()) / 2 + MyGdxGame.WIDTH - 2f * getHeight() - G);
        setX(getX() - 0.25f * level * getHeight()); // level shift
        setY(-(getHeight() - getWidth()) / 2 + (MyGdxGame.WIDTH - getWidth()) / 2 + (position - 2) * (getWidth() + G));
        rotate = 90;
        break;
      }
      break;
    }
  }

}
