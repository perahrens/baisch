package com.mygdx.game.listeners;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.PlayerTurn;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Mercenaries;

public class EnemyKingCardListener extends ClickListener {

  GameState gameState;
  Card kingCard;
  Player player;
  ArrayList<Player> players;

  public EnemyKingCardListener() {
  }

  public EnemyKingCardListener(GameState gameState, Card kingCard, Player player, ArrayList<Player> players) {
    this.gameState = gameState;
    this.kingCard = kingCard;
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    PlayerTurn pt = player.getPlayerTurn();
    if (pt.isPlunderPending() || pt.isAttackPending()) return;

    // Find which player owns this king card (the defender)
    int defenderIdx = -1;
    Player defender = null;
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).getKingCard() == kingCard) {
        defenderIdx = i;
        defender = players.get(i);
        break;
      }
    }
    if (defender == null || defender == player) return;

    // Guard: king can only be attacked when defender has NO defense cards
    if (!defender.getDefCards().isEmpty() || !defender.getTopDefCards().isEmpty()) return;

    boolean kingSelected = player.getKingCard() != null && player.getKingCard().isSelected();
    if (!kingSelected && player.getSelectedHandCards().size() == 0) return;

    // Attacker's king: one-use-per-turn, can only attack when attacker has no def cards
    if (kingSelected) {
      if (pt.isKingUsedThisTurn()) return;
      if (!player.getDefCards().isEmpty() || !player.getTopDefCards().isEmpty()) return;
    }

    // Symbol constraint — king attacks use the king's own symbol (same as hand cards)
    Card attackCard = kingSelected ? player.getKingCard() : player.getSelectedHandCards().get(0);
    String symbol = attackCard.getSymbol();
    // Symbol constraint — joker bypasses; other cards must match the set symbol
    if (!"joker".equals(symbol)) {
      if (pt.getAttackingSymbol()[0] != "none"
          && pt.getAttackingSymbol()[0] != symbol
          && pt.getAttackingSymbol()[1] != symbol) return;
    }

    // Lock attack symbol (same treatment as hand card attacks)
    pt.setAttackingSymbol(symbol, player.hasHero("Lieutenant"));

    // Compute attack vs defender's king strength
    int attackSum;
    if (kingSelected) {
      attackSum = player.getKingCard().getStrength();
    } else {
      attackSum = 0;
      for (Card c : player.getSelectedHandCards()) attackSum += c.getStrength();
    }
    int defStr = "joker".equals(kingCard.getSymbol()) ? 1 : kingCard.getStrength();
    boolean success = attackSum > defStr;

    // Reveal the defender's king
    kingCard.setCovered(false);

    // Snapshot attacking cards (empty for king-uses-own-king attacks)
    ArrayList<Card> attackSnapshot = kingSelected ? new ArrayList<Card>() : new ArrayList<Card>(player.getSelectedHandCards());

    // Set up attack preview overlay
    pt.setKingUsed(kingSelected);
    pt.setPendingAttackCards(attackSnapshot);
    pt.setPendingAttackDefCards(new ArrayList<Card>());
    pt.setAttackTargetPlayerIdx(defenderIdx);
    pt.setAttackTargetPositionId(-1);
    pt.setAttackTargetLevel(-1);
    pt.setAttackSuccess(success);
    pt.setAttackTargetIsKing(true);

    // Consume mercenary attack bonus immediately when attack is committed
    int mercBonus = pt.getMercenaryAttackBonus();
    if (mercBonus > 0) {
      for (Hero h : player.getHeroes()) {
        if (h.getHeroName() == "Mercenaries") {
          Mercenaries merc = (Mercenaries) h;
          for (int mi = 0; mi < mercBonus; mi++) merc.destroy();
          break;
        }
      }
      pt.resetMercenaryAttackBonus();
    }

    pt.setAttackPending(true);

    if (gameState != null) gameState.setUpdateState(true);
  }
}
