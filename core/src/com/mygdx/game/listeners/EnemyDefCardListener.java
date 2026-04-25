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
import com.mygdx.game.heroes.BatteryTower;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Magician;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.heroes.Warlord;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

public class EnemyDefCardListener extends ClickListener {

  Card defCard;
  CardDeck cardDeck;
  CardDeck cemeteryDeck;
  ArrayList<Player> players;
  Player player;
  GameState gameState;
  SocketClient socket;
  int playerIdx;

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

  public EnemyDefCardListener(Card defCard, GameState gameState, Player player, ArrayList<Player> players,
      SocketClient socket, int playerIdx) {
    this(defCard, gameState, player, players);
    this.socket = socket;
    this.playerIdx = playerIdx;
  }

  @Override
  public void clicked(InputEvent event, float x, float y) {
    // Ignore taps while ANY preview overlay is active
    PlayerTurn pt = player.getPlayerTurn();
    if (pt.isLootPending() || pt.isAttackPending()) return;

    // Spy peek: if spy is selected with no attack cards, flip enemy card face-up
    for (int si = 0; si < player.getHeroes().size(); si++) {
      if (player.getHeroes().get(si).getHeroName() == "Spy" && player.getHeroes().get(si).isSelected()) {
        Spy spy = (Spy) player.getHeroes().get(si);
        if (spy.getSpyAttacks() > 0
            && !player.getKingCard().isSelected()
            && player.getSelectedHandCards().isEmpty()) {
          defCard.setCovered(false);
          spy.spyAttack();
          emitSpyFlip();
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

    // Normalize: if the top card of a stacked slot was clicked, redirect to a full-slot attack
    // (both bottom and top cards are revealed and their strengths are summed).
    Card primaryCard = defCard;
    if (level == 1 && targetDefCards.containsKey(positionId)) {
      primaryCard = targetDefCards.get(positionId);
      level = 0; // full-slot attack
    }

    // Saboteurs: if selected and available, place a saboteur on this slot instead of attacking.
    for (int si = 0; si < player.getHeroes().size(); si++) {
      if ("Saboteurs".equals(player.getHeroes().get(si).getHeroName())
          && player.getHeroes().get(si).isSelected()) {
        Saboteurs saboteurs = (Saboteurs) player.getHeroes().get(si);
        if (saboteurs.isAvailable() && !targetPlayer.isSlotSabotaged(positionId)) {
          targetPlayer.setSlotSabotaged(positionId, playerIdx);
          saboteurs.sabotage();
          emitSabotage(targetPlayerIdx, positionId);
          if (gameState != null) gameState.setUpdateState(true);
        }
        return;
      }
    }

    // Magician: if Magician is selected and no attack cards chosen, perform a spell swap.
    for (int mi = 0; mi < player.getHeroes().size(); mi++) {
      if ("Magician".equals(player.getHeroes().get(mi).getHeroName()) && player.getHeroes().get(mi).isSelected()) {
        Magician magician = (Magician) player.getHeroes().get(mi);
        if (magician.getSpells() > 0
            && player.getSelectedHandCards().isEmpty()
            && !player.getKingCard().isSelected()) {
          // Swap each card in the slot: discard to cemetery, replace with a new deck card,
          // preserving the face-up/face-down inversion.
          boolean bottomCovered = !primaryCard.isCovered(); // inverted
          Card newBottom = gameState.getCardDeck().getCard(gameState.getCemeteryDeck());
          boolean hasTop = targetTopDefCards.containsKey(positionId);
          Card newTop = null;
          boolean topCovered = true;
          if (hasTop) {
            Card oldTop = targetTopDefCards.get(positionId);
            topCovered = !oldTop.isCovered(); // inverted
            newTop = gameState.getCardDeck().getCard(gameState.getCemeteryDeck());
            gameState.getCemeteryDeck().addCard(oldTop);
            targetTopDefCards.remove(positionId);
          }
          gameState.getCemeteryDeck().addCard(primaryCard);
          targetDefCards.remove(positionId);
          // Place new bottom card
          newBottom.setCovered(bottomCovered);
          targetPlayer.addDefCard(positionId, newBottom, 0);
          // Place new top card (if slot was stacked), with its own inverted covered state
          if (newTop != null) {
            newTop.setCovered(topCovered);
            targetPlayer.addDefCard(positionId, newTop, 1);
          }
          magician.castSpell();
          // Emit to server
          if (socket != null) {
            try {
              JSONObject data = new JSONObject();
              data.put("playerIdx", playerIdx);
              data.put("targetPlayerIdx", targetPlayerIdx);
              data.put("positionId", positionId);
              data.put("bottomCardId", newBottom.getCardId());
              data.put("bottomCovered", bottomCovered);
              data.put("topCardId", newTop != null ? newTop.getCardId() : -1);
              data.put("topCovered", topCovered);
              socket.emit("magicianSwap", data);
            } catch (JSONException e) { e.printStackTrace(); }
          }
          gameState.setUpdateState(true);
        }
        return;
      }
    }

    // Warlord: detect before the 'nothing selected' guard — selecting the Warlord hero
    // and clicking an enemy defense card forces the king card to attack.
    boolean warlordAttack = false;
    Warlord warlord = null;
    for (Hero wh : player.getHeroes()) {
      if ("Warlord".equals(wh.getHeroName())) { warlord = (Warlord) wh; }
      if ("Warlord".equals(wh.getHeroName()) && wh.isSelected()) { warlordAttack = true; }
    }

    // Only proceed if king card or hand cards are selected (Warlord bypasses this)
    boolean kingSelected = player.getKingCard() != null && player.getKingCard().isSelected();
    if (warlordAttack) {
      // Force king as attacker; clear any hand card selections
      kingSelected = true;
      for (Card hc : player.getHandCards()) hc.setSelected(false);
      if (warlord == null || !warlord.isAttackAvailable()) return;
    } else {
      boolean hasSelectedDefCards = !player.getSelectedDefCards().isEmpty();
      if (!kingSelected && player.getSelectedHandCards().size() == 0
          && !(player.hasHero("Banneret") && hasSelectedDefCards)) return;
      // King can only be used when the player has no defense cards
      if (kingSelected && (!player.getDefCards().isEmpty() || !player.getTopDefCards().isEmpty())) return;
    }

    // King can only be used once per turn (Warlord grants an extra attack and bypasses this)
    if (kingSelected && !warlordAttack && pt.isKingUsedThisTurn()) return;

    // Symbol constraint — joker bypasses; other cards must match the set symbol
    // For Banneret using only own def cards (no hand cards), use the first def card's symbol
    String attackSymbol;
    if (kingSelected) {
      attackSymbol = player.getKingCard().getSymbol();
    } else if (!player.getSelectedHandCards().isEmpty()) {
      attackSymbol = player.getSelectedHandCards().get(0).getSymbol();
    } else {
      // Banneret: only own def cards selected
      attackSymbol = player.getSelectedDefCards().get(0).getSymbol();
    }
    if (!"joker".equals(attackSymbol)) {
      if (!"none".equals(pt.getAttackingSymbol()[0])
          && !pt.getAttackingSymbol()[0].equals(attackSymbol)
          && !pt.getAttackingSymbol()[1].equals(attackSymbol)) return;
    }

    // Snapshot attacking hand cards (empty for king attacks — king is not a hand card)
    // Also snapshot own def cards used as attackers (Banneret)
    ArrayList<Card> attackSnapshot = kingSelected
        ? new ArrayList<Card>()
        : new ArrayList<Card>(player.getSelectedHandCards());
    ArrayList<Card> attackOwnDefSnapshot = new ArrayList<Card>(player.getSelectedDefCards());

    // Immediately reveal the targeted defense card(s)
    primaryCard.setCovered(false);
    Card topDefCard = (level == 0 && targetTopDefCards.containsKey(positionId))
        ? targetTopDefCards.get(positionId) : null;
    if (topDefCard != null) topDefCard.setCovered(false);

    // Compute result
    boolean success;
    if (kingSelected) {
      int attackSum = player.getKingCard().getStrength();
      int defenseStrength = 0;
      if ("joker".equals(primaryCard.getSymbol())) defenseStrength += 1; else defenseStrength += primaryCard.getStrength();
      if (topDefCard != null) {
        if ("joker".equals(topDefCard.getSymbol())) defenseStrength += 1; else defenseStrength += topDefCard.getStrength();
      }
      success = attackSum > defenseStrength;
    } else if (topDefCard != null) {
      success = player.attackEnemyDefense(primaryCard, topDefCard);
    } else {
      success = player.attackEnemyDefense(primaryCard);
    }

    // Store preview state
    if (!kingSelected) {
      pt.setAttackingSymbol(attackSymbol, player.hasHero("Banneret"));
    } else {
      // King attacks also lock the attack symbol (treated same as hand cards)
      pt.setAttackingSymbol(player.getKingCard().getSymbol(), player.hasHero("Banneret"));
    }
    pt.setKingUsed(kingSelected);
    pt.setPendingAttackCards(attackSnapshot);
    pt.setPendingAttackOwnDefCards(attackOwnDefSnapshot);
    ArrayList<Card> defCardList = new ArrayList<Card>();
    defCardList.add(primaryCard);
    if (topDefCard != null) defCardList.add(topDefCard);
    pt.setPendingAttackDefCards(defCardList);
    pt.setAttackTargetPlayerIdx(targetPlayerIdx);
    pt.setAttackTargetPositionId(positionId);
    pt.setAttackTargetLevel(level);
    pt.setAttackSuccess(success);

    // Consume mercenary attack bonus immediately when attack is committed
    int mercBonus = pt.getMercenaryAttackBonus();
    pt.setPendingAttackMercenaryBonus(mercBonus);
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

    // Consume Warlord charge after attack is committed
    if (warlordAttack && warlord != null) {
      // Mark this pending attack as a Warlord extra-attack so the resolved
      // callback does NOT mark the king as spent for the turn (Warlord grants
      // an additional attack action; the regular king attack/loot must
      // remain available).
      pt.setPendingAttackIsWarlord(true);
      warlord.useAttack();
      pt.increaseAttackCounter(); // count Warlord attack locally so Finish Turn never triggers expose
      if (socket != null) {
        try {
          JSONObject warlordData = new JSONObject();
          warlordData.put("playerIdx", playerIdx);
          socket.emit("warlordDirectAttack", warlordData);
        } catch (JSONException e) { e.printStackTrace(); }
      }
    }

    // Only trigger the Battery Tower intercept flow when the defender actually has one with charges.
    BatteryTower defBt = getBatteryTower(players.get(targetPlayerIdx));
    if (socket != null && defBt != null && defBt.getCharges() > 0) {
      pt.setBatteryWaiting(true);
      emitBatteryDefenseCheck(targetPlayerIdx, positionId, level, false,
          attackSnapshot, pt.isAttackSuccess());
    }

    // Emit attack preview to server so the defender can see the battle in progress.
    // Skip if Battery Tower intercept is pending — preview will be emitted after the decision.
    if (socket != null && !pt.isBatteryWaiting()) {
      emitAttackPreview(targetPlayerIdx, positionId, level, kingSelected, success,
          attackSnapshot, attackOwnDefSnapshot, primaryCard, topDefCard);
    }

    if (gameState != null) gameState.setUpdateState(true);
  }

  private BatteryTower getBatteryTower(Player p) {
    if (p == null) return null;
    for (int i = 0; i < p.getHeroes().size(); i++) {
      if (p.getHeroes().get(i) instanceof BatteryTower) {
        return (BatteryTower) p.getHeroes().get(i);
      }
    }
    return null;
  }

  void emitBatteryDefenseCheck(int targetPlayerIdx, int positionId, int level,
      boolean isKing, ArrayList<Card> attackCards, boolean success) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", playerIdx);
      data.put("targetPlayerIdx", targetPlayerIdx);
      data.put("positionId", positionId);
      data.put("level", level);
      data.put("isKing", isKing);
      data.put("success", success);
      JSONArray atkIds = new JSONArray();
      for (Card c : attackCards) atkIds.put(c.getCardId());
      data.put("attackCardIds", atkIds);
      socket.emit("batteryDefenseCheck", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitSabotage(int defenderIdx, int positionId) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", playerIdx);
      data.put("defenderIdx", defenderIdx);
      data.put("positionId", positionId);
      socket.emit("sabotage", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

  private void emitSpyFlip() {
    if (socket == null) return;
    // Find target player index and slot for this defCard
    for (int p = 0; p < players.size(); p++) {
      for (int c = 1; c <= 3; c++) {
        if (players.get(p).getDefCards().containsKey(c) && players.get(p).getDefCards().get(c) == defCard) {
          try {
            JSONObject data = new JSONObject();
            data.put("targetPlayerIdx", p);
            data.put("slot", c);
            data.put("level", 0);
            socket.emit("spyFlip", data);
          } catch (JSONException e) { e.printStackTrace(); }
          return;
        }
        if (players.get(p).getTopDefCards().containsKey(c) && players.get(p).getTopDefCards().get(c) == defCard) {
          try {
            JSONObject data = new JSONObject();
            data.put("targetPlayerIdx", p);
            data.put("slot", c);
            data.put("level", 1);
            socket.emit("spyFlip", data);
          } catch (JSONException e) { e.printStackTrace(); }
          return;
        }
      }
    }
  }

  private void emitAttackPreview(int targetPlayerIdx, int positionId, int level, boolean kingUsed,
      boolean success, ArrayList<Card> attackCards, ArrayList<Card> ownDefCards,
      Card primaryCard, Card topDefCard) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("attackerIdx", playerIdx);
      data.put("defenderIdx", targetPlayerIdx);
      data.put("positionId", positionId);
      data.put("level", level);
      JSONArray atkIds = new JSONArray();
      for (Card c : attackCards) atkIds.put(c.getCardId());
      data.put("attackCardIds", atkIds);
      JSONArray ownDefIds = new JSONArray();
      for (Card c : ownDefCards) ownDefIds.put(c.getCardId());
      data.put("ownDefCardIds", ownDefIds);
      JSONArray defIds = new JSONArray();
      defIds.put(primaryCard.getCardId());
      if (topDefCard != null) defIds.put(topDefCard.getCardId());
      data.put("defCardIds", defIds);
      data.put("kingUsed", kingUsed);
      data.put("kingCardId", kingUsed && player.getKingCard() != null ? player.getKingCard().getCardId() : -1);
      data.put("mercenaryBonus", player.getPlayerTurn().getPendingAttackMercenaryBonus());
      // Issue #167: include defender's mercenary boost so watcher overlay shows the
      // correct defense sum (Card.fromCardId rebuilds cards without boost).
      int defMercBonus = primaryCard.getBoosted();
      if (topDefCard != null) defMercBonus += topDefCard.getBoosted();
      data.put("defMercBonus", defMercBonus);
      JSONArray defBoosts = new JSONArray();
      defBoosts.put(primaryCard.getBoosted());
      if (topDefCard != null) defBoosts.put(topDefCard.getBoosted());
      data.put("defCardBoosts", defBoosts);
      data.put("reservistBonus", player.getPlayerTurn().getReservistAttackBonus());
      data.put("success", success);
      data.put("attackingSymbol", player.getPlayerTurn().getAttackingSymbol()[0]);
      data.put("attackingSymbol2", player.getPlayerTurn().getAttackingSymbol()[1]);
      socket.emit("attackPreview", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }

}
