package com.mygdx.game.listeners;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.Card;
import com.mygdx.game.GameState;
import com.mygdx.game.Player;
import com.mygdx.game.PlayerTurn;
import com.mygdx.game.heroes.BatteryTower;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Reservists;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.heroes.Warlord;
import com.mygdx.game.net.SocketClient;
import com.mygdx.game.util.JSONArray;
import com.mygdx.game.util.JSONException;
import com.mygdx.game.util.JSONObject;

public class EnemyKingCardListener extends ClickListener {

  GameState gameState;
  Card kingCard;
  Player player;
  ArrayList<Player> players;
  SocketClient socket;
  int playerIdx;

  public EnemyKingCardListener() {
  }

  public EnemyKingCardListener(GameState gameState, Card kingCard, Player player, ArrayList<Player> players) {
    this.gameState = gameState;
    this.kingCard = kingCard;
    this.player = player;
    this.players = players;
  }

  public EnemyKingCardListener(GameState gameState, Card kingCard, Player player, ArrayList<Player> players,
      SocketClient socket, int playerIdx) {
    this(gameState, kingCard, player, players);
    this.socket = socket;
    this.playerIdx = playerIdx;
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

    // Spy peek: if Spy is selected with attacks remaining and ALL of the defender's
    // defense cards are already face-up, allow flipping the king card.
    for (int si = 0; si < player.getHeroes().size(); si++) {
      Hero h = player.getHeroes().get(si);
      if ("Spy".equals(h.getHeroName()) && h.isSelected()) {
        Spy spy = (Spy) h;
        if (spy.getSpyAttacks() > 0
            && kingCard.isCovered()
            && !player.getKingCard().isSelected()
            && player.getSelectedHandCards().isEmpty()) {
          boolean allFaceUp = true;
          for (Card dc : defender.getDefCards().values()) {
            if (dc.isCovered()) { allFaceUp = false; break; }
          }
          if (allFaceUp) {
            for (Card dc : defender.getTopDefCards().values()) {
              if (dc.isCovered()) { allFaceUp = false; break; }
            }
          }
          if (allFaceUp) {
            kingCard.setCovered(false);
            spy.spyAttack();
            emitSpyKingFlip(defenderIdx);
            if (gameState != null) gameState.setUpdateState(true);
          }
        }
        return;
      }
    }

    // Guard: king can only be attacked when defender has NO defense cards
    if (!defender.getDefCards().isEmpty() || !defender.getTopDefCards().isEmpty()) return;

    // Warlord: detect before the 'nothing selected' guard — selecting the Warlord hero
    // and clicking an enemy king (when defender has no defense) forces the king card to attack.
    // This bypasses the restriction that the attacker must have no defense cards of their own.
    boolean warlordAttack = false;
    Warlord warlord = null;
    for (Hero wh : player.getHeroes()) {
      if ("Warlord".equals(wh.getHeroName())) { warlord = (Warlord) wh; }
      if ("Warlord".equals(wh.getHeroName()) && wh.isSelected()) { warlordAttack = true; }
    }

    boolean kingSelected = player.getKingCard() != null && player.getKingCard().isSelected();
    if (warlordAttack) {
      // Force king as attacker; clear any hand card selections
      kingSelected = true;
      for (Card hc : player.getHandCards()) hc.setSelected(false);
      if (warlord == null || !warlord.isAttackAvailable()) return;
    } else {
      if (!kingSelected && player.getSelectedHandCards().size() == 0) return;
    }

    // Attacker's king: one-use-per-turn (Warlord grants an extra attack and bypasses this)
    if (kingSelected && !warlordAttack && pt.isKingUsedThisTurn()) return;
    // Without Warlord, king can only be used when attacker has no defense cards
    if (kingSelected && !warlordAttack && (!player.getDefCards().isEmpty() || !player.getTopDefCards().isEmpty())) return;

    // Symbol constraint — king attacks use the king's own symbol (same as hand cards)
    Card attackCard = kingSelected ? player.getKingCard() : player.getSelectedHandCards().get(0);
    String symbol = attackCard.getSymbol();
    // Symbol constraint — joker bypasses; other cards must match the set symbol
    if (!"joker".equals(symbol)) {
      if (!"none".equals(pt.getAttackingSymbol()[0])
          && !pt.getAttackingSymbol()[0].equals(symbol)
          && !pt.getAttackingSymbol()[1].equals(symbol)) return;
    }

    // Lock attack symbol (same treatment as hand card attacks)
    pt.setAttackingSymbol(symbol, player.hasHero("Banneret"));

    // Compute attack vs defender's king strength
    int attackSum;
    if (kingSelected) {
      attackSum = player.getKingCard().getStrength();
    } else {
      attackSum = 0;
      for (Card c : player.getSelectedHandCards()) attackSum += c.getStrength();
    }
    int defStr = "joker".equals(kingCard.getSymbol()) ? 1 : kingCard.getStrength();
    // Defending player's ready reservists automatically boost king defence strength
    for (Hero h : defender.getHeroes()) {
      if ("Reservists".equals(h.getHeroName())) {
        defStr += ((Reservists) h).countReady();
        break;
      }
    }
    boolean success = attackSum > defStr;

    // Reveal the defender's king — only if no Battery Tower intercept will happen
    if (socket == null) {
      kingCard.setCovered(false);
    }

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
    // Store base sums so the Reservists overlay button can recalculate for king attacks
    pt.setPendingAttackBaseSum(attackSum);
    pt.setPendingAttackDefStr(defStr);

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

    // Consume Warlord charge after attack is committed
    if (warlordAttack && warlord != null) {
      // Mark this pending attack as a Warlord extra-attack so the resolved
      // callback does NOT mark the king as spent for the turn (Warlord grants
      // an additional attack action; the regular king attack/plunder must
      // remain available).
      pt.setPendingAttackIsWarlord(true);
      warlord.useAttack();
      if (socket != null) {
        try {
          JSONObject warlordData = new JSONObject();
          warlordData.put("playerIdx", playerIdx);
          socket.emit("warlordDirectAttack", warlordData);
        } catch (JSONException e) { e.printStackTrace(); }
      }
    }

    // Only trigger the Battery Tower intercept flow when the defender actually has one with charges.
    BatteryTower defBt = null;
    for (Hero h : defender.getHeroes()) {
      if (h instanceof BatteryTower) { defBt = (BatteryTower) h; break; }
    }
    if (socket != null && defBt != null && defBt.getCharges() > 0) {
      pt.setBatteryWaiting(true);
      try {
        JSONObject data = new JSONObject();
        data.put("attackerIdx", playerIdx);
        data.put("targetPlayerIdx", defenderIdx);
        data.put("positionId", -1);
        data.put("level", -1);
        data.put("isKing", true);
        data.put("success", success);
        JSONArray atkIds = new JSONArray();
        for (Card c : attackSnapshot) atkIds.put(c.getCardId());
        data.put("attackCardIds", atkIds);
        socket.emit("batteryDefenseCheck", data);
      } catch (JSONException e) { e.printStackTrace(); }
    }

    if (gameState != null) gameState.setUpdateState(true);
  }

  private void emitSpyKingFlip(int defenderIdx) {
    if (socket == null) return;
    try {
      JSONObject data = new JSONObject();
      data.put("targetPlayerIdx", defenderIdx);
      data.put("slot", -1); // -1 signals a king card flip
      data.put("level", -1);
      socket.emit("spyFlip", data);
    } catch (JSONException e) { e.printStackTrace(); }
  }
}
