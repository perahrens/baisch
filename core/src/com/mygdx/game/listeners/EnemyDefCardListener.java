package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.CardDeck;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.PlayerTurn;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Spy;

public class EnemyDefCardListener extends ClickListener {

  Card defCard;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;
  ArrayList<Player> players;
  Player player;
  GameState gameState;

  public EnemyDefCardListener() {
  }

  public EnemyDefCardListener(Card defCard, CardDeck cardDeck, CardDeck cemeteryDeck, Player player,
      ArrayList<Player> players) {
    this.defCard = defCard;
    this.cardDeck = cardDeck;
    this.cemeteryDeck = cemeteryDeck;
    this.player = player;
    this.players = players;
  }

  public EnemyDefCardListener(Card defCard, GameState gameState, Player player, ArrayList<Player> players) {
    this.defCard = defCard;
    this.gameState = gameState;
    this.cardDeck = gameState.getCardDeck();
    this.cemeteryDeck = gameState.getCemeteryDeck();
    this.player = player;
    this.players = players;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    // Ignore taps while ANY preview overlay is active
    PlayerTurn pt = player.getPlayerTurn();
    if (pt.isPlunderPending() || pt.isAttackPending()) return;

    // Spy peek: if spy is selected with no attack cards, flip enemy card face-up
    for (int si = 0; si < player.getHeroes().size(); si++) {
      if (player.getHeroes().get(si).getHeroName() == "Spy" && player.getHeroes().get(si).isSelected()) {
        Spy spy = (Spy) player.getHeroes().get(si);
        if (spy.getSpyAttacks() > 0
            && !player.getKingCard().isSelected()
            && player.getSelectedHandCards().isEmpty()) {
          defCard.setCovered(false);
          spy.spyAttack();
          if (gameState != null) gameState.setUpdateState(true);
        }
        return;
      }
    }

    // Find which player and slot owns this defCard
    int targetPlayerIdx = -1;
    int positionId = -1;
    int level = -1;
    Player targetPlayer = null;
    Map<Integer, Card> targetDefCards = null;
    Map<Integer, Card> targetTopDefCards = null;

    outer:
    for (int p = 0; p < players.size(); p++) {
      for (int c = 1; c <= 3; c++) {
        if (players.get(p).getDefCards().containsKey(c) && players.get(p).getDefCards().get(c) == defCard) {
          targetPlayerIdx = p;
          positionId = c;
          level = 0;
          targetPlayer = players.get(p);
          targetDefCards = targetPlayer.getDefCards();
          targetTopDefCards = targetPlayer.getTopDefCards();
          break outer;
        }
        if (players.get(p).getTopDefCards().containsKey(c) && players.get(p).getTopDefCards().get(c) == defCard) {
          targetPlayerIdx = p;
          positionId = c;
          level = 1;
          targetPlayer = players.get(p);
          targetDefCards = targetPlayer.getDefCards();
          targetTopDefCards = targetPlayer.getTopDefCards();
          break outer;
        }
      }
    }
    if (targetPlayer == null) return;

    // Only proceed if king card or hand cards are selected
    boolean kingSelected = player.getKingCard() != null && player.getKingCard().isSelected();
    if (!kingSelected && player.getSelectedHandCards().size() == 0) return;

    // King can only be used once per turn
    if (kingSelected && pt.isKingUsedThisTurn()) return;
    // King can only be used when the player has no defense cards
    if (kingSelected && (!player.getDefCards().isEmpty() || !player.getTopDefCards().isEmpty())) return;

    // Symbol constraint — joker bypasses; other cards must match the set symbol
    String attackSymbol = kingSelected ? player.getKingCard().getSymbol() : player.getSelectedHandCards().get(0).getSymbol();
    if (!"joker".equals(attackSymbol)) {
      if (pt.getAttackingSymbol()[0] != "none"
          && pt.getAttackingSymbol()[0] != attackSymbol
          && pt.getAttackingSymbol()[1] != attackSymbol) return;
    }

    // Snapshot attacking cards (empty for king attacks — king is not a hand card)
    ArrayList<Card> attackSnapshot = kingSelected
        ? new ArrayList<Card>()
        : new ArrayList<Card>(player.getSelectedHandCards());

    // Reveal the targeted defense card
    defCard.setCovered(false);
    Card topDefCard = (level == 0 && targetTopDefCards.containsKey(positionId))
        ? targetTopDefCards.get(positionId) : null;
    if (topDefCard != null) topDefCard.setCovered(false);

    // Compute result
    boolean success;
    if (kingSelected) {
      int attackSum = player.getKingCard().getStrength();
      int defenseStrength = 0;
      if ("joker".equals(defCard.getSymbol())) defenseStrength += 1; else defenseStrength += defCard.getStrength();
      if (topDefCard != null) {
        if ("joker".equals(topDefCard.getSymbol())) defenseStrength += 1; else defenseStrength += topDefCard.getStrength();
      }
      success = attackSum > defenseStrength;
    } else if (topDefCard != null) {
      success = player.attackEnemyDefense(defCard, topDefCard);
    } else {
      success = player.attackEnemyDefense(defCard);
    }

    // Store preview state
    if (!kingSelected) {
      pt.setAttackingSymbol(player.getSelectedHandCards().get(0).getSymbol(), player.hasHero("Lieutenant"));
    } else {
      // King attacks also lock the attack symbol (treated same as hand cards)
      pt.setAttackingSymbol(player.getKingCard().getSymbol(), player.hasHero("Lieutenant"));
    }
    pt.setKingUsed(kingSelected);
    pt.setPendingAttackCards(attackSnapshot);
    ArrayList<Card> defCardList = new ArrayList<Card>();
    defCardList.add(defCard);
    if (topDefCard != null) defCardList.add(topDefCard);
    pt.setPendingAttackDefCards(defCardList);
    pt.setAttackTargetPlayerIdx(targetPlayerIdx);
    pt.setAttackTargetPositionId(positionId);
    pt.setAttackTargetLevel(level);
    pt.setAttackSuccess(success);

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
