// ── Bot AI ───────────────────────────────────────────────────────────────────
// Factory: call createBotAI(io, checkAndHandleWinner) once to get a bot instance
// with two public methods: isBot(player) and playBotTurnIfNeeded(sess).

module.exports = function createBotAI(io, checkAndHandleWinner) {

  // How long overlays stay visible (ms) — matches human player experience
  var BOT_ACTION_DELAY = 1500;

  function isBot(player) {
    return player && player.id.indexOf('bot_') === 0;
  }

  function playBotTurnIfNeeded(sess) {
    if (!sess || !sess.gameState) return;
    var gs = sess.gameState;
    var player = gs.players[gs.currentPlayerIndex];
    if (!isBot(player) || player.isOut) return;
    setTimeout(function() {
      if (!sess.gameState) return;
      executeBotTurn(sess);
    }, 1500);
  }

  // Returns the card suit name for a card ID
  function botCardSuit(cardId) {
    if (cardId > 52) return 'joker';
    var si = Math.floor((cardId - 1) / 13);
    return ['clubs', 'diamonds', 'hearts', 'spades'][si];
  }

  // Group non-joker hand cards by suit. Returns { suitName: [cardIds] }
  function botGroupBySuit(hand) {
    var groups = {};
    for (var i = 0; i < hand.length; i++) {
      var id = hand[i];
      if (id > 52) continue;
      var suit = botCardSuit(id);
      if (!groups[suit]) groups[suit] = [];
      groups[suit].push(id);
    }
    return groups;
  }

  // Find the minimal subset of cards (from sorted list) with combined strength >= threshold.
  // Returns array of cardIds, or null if impossible even with all cards.
  function botMinimalSubset(gs, cards, threshold) {
    if (!cards || cards.length === 0) return null;
    var sorted = cards.slice().sort(function(a, b) { return gs.cardStrength(a) - gs.cardStrength(b); });
    var total = 0;
    for (var i = 0; i < sorted.length; i++) total += gs.cardStrength(sorted[i]);
    if (total < threshold) return null;
    // Greedy: take strongest cards first until we meet threshold
    var chosen = [];
    var sum = 0;
    for (var j = sorted.length - 1; j >= 0 && sum < threshold; j--) {
      chosen.push(sorted[j]);
      sum += gs.cardStrength(sorted[j]);
    }
    return chosen;
  }

  // Find the best single-suit combination from a hand (returns {sum, cards, suit})
  function botBestSuitCombo(gs, hand) {
    var groups = botGroupBySuit(hand);
    var best = { sum: 0, cards: [], suit: 'none' };
    var suits = Object.keys(groups);
    for (var i = 0; i < suits.length; i++) {
      var suit = suits[i];
      var sum = 0;
      for (var j = 0; j < groups[suit].length; j++) sum += gs.cardStrength(groups[suit][j]);
      if (sum > best.sum) best = { sum: sum, cards: groups[suit], suit: suit };
    }
    return best;
  }

  // Choose the best plunder: smart multi-card combo, preferring bigger decks with larger margin.
  // Returns { deckIndex, cardIds, symbol, success } or null.
  function botChoosePlunder(gs, attackerIdx) {
    var p = gs.players[attackerIdx];
    if (!p || !p.hand || p.hand.length === 0 || (p.pickingDeckAttacks || 0) <= 0) return null;

    var groups = botGroupBySuit(p.hand);
    var bestChoice = null;
    var bestScore = -9999;

    for (var d = 0; d < gs.pickingDecks.length; d++) {
      var deck = gs.pickingDecks[d];
      if (deck.length === 0) continue;

      // Find any face-up card in the deck — that's what the player can see as a hint.
      var visibleStrength = 0;
      for (var fi = 0; fi < deck.length; fi++) {
        if (!deck[fi].covered) { visibleStrength = gs.cardStrength(deck[fi].id); break; }
      }
      if (visibleStrength === 0) continue; // entire deck is covered, skip

      // Bot is server-side so it can read the actual threshold (top covered card).
      var topCard = deck[deck.length - 1];
      var actualThreshold = gs.cardStrength(topCard.id);
      var deckSize = deck.length;

      // Target: aim for 12-15 total, at least meeting the real threshold.
      // Larger decks deserve a safer margin; smaller decks keep it economical.
      var safeTarget = Math.max(actualThreshold, deckSize >= 4 ? 14 : (deckSize >= 3 ? 13 : 12));

      var suits = Object.keys(groups);
      for (var si = 0; si < suits.length; si++) {
        var suit = suits[si];
        // Try to reach safeTarget; if impossible, just try to meet the actual threshold.
        var combo = botMinimalSubset(gs, groups[suit], safeTarget)
                 || botMinimalSubset(gs, groups[suit], actualThreshold);
        if (!combo) continue;
        var comboSum = 0;
        for (var ci = 0; ci < combo.length; ci++) comboSum += gs.cardStrength(combo[ci]);
        // Skip wildly over-spending
        if (comboSum > actualThreshold + 6 && comboSum > 15) continue;
        var success = (comboSum >= actualThreshold);
        var waste = Math.max(0, comboSum - actualThreshold);
        // Prefer success > bigger deck > less waste > fewer cards
        var score = (success ? 1000 : -500) + deckSize * 10 - waste * 2 - combo.length;
        if (score > bestScore) {
          bestScore = score;
          bestChoice = { deckIndex: d, cardIds: combo, symbol: suit, success: success };
        }
      }
    }
    // Don't attempt plunder if facing certain failure
    return (bestChoice && (bestChoice.success || bestScore > -200)) ? bestChoice : null;
  }

  // Choose the best defense attack.
  // allowScout=false: only attack known face-up cards.
  // allowScout=true: also probe covered cards with a weak card (scout).
  // Returns { defenderIdx, positionId, cardIds, symbol, success } or null.
  function botChooseDefAttack(gs, attackerIdx, allowScout) {
    var attacker = gs.players[attackerIdx];
    if (!attacker || !attacker.hand || attacker.hand.length === 0) return null;

    var groups = botGroupBySuit(attacker.hand);
    var bestChoice = null;
    var bestScore = -9999;

    for (var di = 0; di < gs.players.length; di++) {
      if (di === attackerIdx) continue;
      var defender = gs.players[di];
      if (defender.isOut) continue;

      for (var slot = 1; slot <= 3; slot++) {
        var defCardId = defender.defCards[slot];
        if (defCardId == null) continue;

        var isFaceUp = defender.defCardsCovered && defender.defCardsCovered[slot] === false;

        if (isFaceUp) {
          var defBoost = (defender.defCardsBoost && defender.defCardsBoost[slot]) || 0;
          var topCardId = defender.topDefCards ? defender.topDefCards[slot] : null;
          var topBoost = (defender.topDefCardsBoost && defender.topDefCardsBoost[slot]) || 0;
          var threshold = gs.cardStrength(defCardId) + defBoost
                        + (topCardId != null ? gs.cardStrength(topCardId) + topBoost : 0);
          var suits = Object.keys(groups);
          for (var si = 0; si < suits.length; si++) {
            var suit = suits[si];
            var combo = botMinimalSubset(gs, groups[suit], threshold);
            if (!combo) continue;
            var comboSum = 0;
            for (var ci = 0; ci < combo.length; ci++) comboSum += gs.cardStrength(combo[ci]);
            var success = (comboSum >= threshold);
            // Bonus if this would open the king (only shield remaining)
            var shieldsLeft = 0;
            for (var s = 1; s <= 3; s++) {
              if (defender.defCards[s] != null || defender.topDefCards[s] != null) shieldsLeft++;
            }
            var score = (success ? 1000 : -200) + (shieldsLeft === 1 ? 100 : 0) - combo.length;
            if (score > bestScore) {
              bestScore = score;
              bestChoice = { defenderIdx: di, positionId: slot, cardIds: combo, symbol: suit, success: success };
            }
          }
        } else if (allowScout && attacker.hand.length > 1) {
          // Scout: probe with weakest non-joker card to reveal the defense card
          var nonJokerHand = attacker.hand.filter(function(id) { return id <= 52; });
          var weakestId = null, weakestStr = 9999;
          for (var ci = 0; ci < nonJokerHand.length; ci++) {
            var s = gs.cardStrength(nonJokerHand[ci]);
            if (s < weakestStr) { weakestStr = s; weakestId = nonJokerHand[ci]; }
          }
          if (weakestId !== null) {
            var scoutScore = 1; // low priority vs face-up attacks
            if (scoutScore > bestScore) {
              bestScore = scoutScore;
              bestChoice = { defenderIdx: di, positionId: slot,
                             cardIds: [weakestId], symbol: botCardSuit(weakestId), success: false };
            }
          }
        }
      }
    }
    return bestChoice;
  }

  // Async: shows king-attack preview, waits BOT_ACTION_DELAY, resolves, then calls callback(true/false).
  function botTryKingAttackAsync(sess, gs, attackerIdx, callback) {
    var attacker = gs.players[attackerIdx];
    var groups = botGroupBySuit(attacker.hand);

    for (var di = 0; di < gs.players.length; di++) {
      if (di === attackerIdx) continue;
      var defender = gs.players[di];
      if (defender.isOut) continue;

      // All 3 defense slots must be empty (king exposed)
      var allEmpty = true;
      for (var s = 1; s <= 3; s++) {
        if (defender.defCards[s] != null || defender.topDefCards[s] != null) { allEmpty = false; break; }
      }
      if (!allEmpty) continue;

      var kingStr = gs.cardStrength(defender.kingCard) + (defender.kingCardBoost || 0);
      var suits = Object.keys(groups);
      for (var si = 0; si < suits.length; si++) {
        var suit = suits[si];
        var combo = botMinimalSubset(gs, groups[suit], kingStr);
        if (!combo) continue;
        var comboSum = 0;
        for (var ci = 0; ci < combo.length; ci++) comboSum += gs.cardStrength(combo[ci]);
        if (comboSum < kingStr) continue;
        gs.setAttackPreview({ attackerIdx: attackerIdx, defenderIdx: di, positionId: 0, level: 0,
                               attackingSymbol: suit, attackingSymbol2: 'none',
                               success: true, attackCardIds: combo,
                               defCardIds: [defender.kingCard] });
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        (function(capturedDi, capturedCombo) {
          setTimeout(function() {
            gs.kingAttackResolved(attackerIdx, capturedDi, true, capturedCombo, false);
            // Auto-pick hero if bot defeated a player with multiple heroes
            if (gs.pendingHeroSelection && gs.pendingHeroSelection.attackerIdx === attackerIdx) {
              gs.resolveHeroSelection(gs.pendingHeroSelection.options[0]);
            }
            io.to(sess.id).emit('stateUpdate', gs.serialize());
            checkAndHandleWinner(sess);
            callback(true);
          }, BOT_ACTION_DELAY);
        })(di, combo);
        return;
      }

      // Last resort: use a joker to eliminate
      var jokers = attacker.hand.filter(function(id) { return id > 52; });
      if (jokers.length > 0) {
        gs.setAttackPreview({ attackerIdx: attackerIdx, defenderIdx: di, positionId: 0, level: 0,
                               attackingSymbol: 'joker', attackingSymbol2: 'none',
                               success: true, attackCardIds: [jokers[0]],
                               defCardIds: [defender.kingCard] });
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        (function(capturedDi, capturedJoker) {
          setTimeout(function() {
            gs.kingAttackResolved(attackerIdx, capturedDi, true, [capturedJoker], false);
            // Auto-pick hero if bot defeated a player with multiple heroes
            if (gs.pendingHeroSelection && gs.pendingHeroSelection.attackerIdx === attackerIdx) {
              gs.resolveHeroSelection(gs.pendingHeroSelection.options[0]);
            }
            io.to(sess.id).emit('stateUpdate', gs.serialize());
            checkAndHandleWinner(sess);
            callback(true);
          }, BOT_ACTION_DELAY);
        })(di, jokers[0]);
        return;
      }
    }
    callback(false);
  }

  // After a plunder: optionally attack a face-up defense card, then finish the turn.
  function botContinueAfterPlunder(sess, gs, idx) {
    var atkAfterPlunder = botChooseDefAttack(gs, idx, false);
    if (atkAfterPlunder) {
      var apDefCardId = gs.players[atkAfterPlunder.defenderIdx].defCards[atkAfterPlunder.positionId];
      var apPreview = { attackerIdx: idx, defenderIdx: atkAfterPlunder.defenderIdx,
                        positionId: atkAfterPlunder.positionId, level: 0,
                        attackingSymbol: atkAfterPlunder.symbol, attackingSymbol2: 'none',
                        success: atkAfterPlunder.success, attackCardIds: atkAfterPlunder.cardIds,
                        defCardIds: apDefCardId != null ? [apDefCardId] : [] };
      gs.setAttackPreview(apPreview);
      io.to(sess.id).emit('stateUpdate', gs.serialize());
      botDoDefAttackWithBatteryCheck(sess, gs, atkAfterPlunder, apPreview, function() {
        botFinishTurn(sess, gs, idx, true);
      });
    } else {
      botFinishTurn(sess, gs, idx, false);
    }
  }

  // Expose defense/king if no attack was made, then finishTurn and chain next bot.
  function botFinishTurn(sess, gs, idx, attackedPlayer) {
    var p = gs.players[idx];
    if (!attackedPlayer && p) {
      var exposed = false;
      for (var slot = 1; slot <= 3; slot++) {
        if (p.defCards[slot] != null && p.defCardsCovered && p.defCardsCovered[slot] !== false) {
          gs.exposeDefCard(idx, slot);
          io.to(sess.id).emit('stateUpdate', gs.serialize());
          exposed = true;
          break;
        }
      }
      if (!exposed && p.kingCard != null && p.kingCovered !== false) {
        gs.exposeKingCard(idx);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
      }
    }
    gs.finishTurn();
    io.to(sess.id).emit('stateUpdate', gs.serialize());
    checkAndHandleWinner(sess);
    playBotTurnIfNeeded(sess);
  }

  // Replace an exposed (face-up) defense card with a face-down card from hand.
  // Returns true if performed.
  function botReplaceExposedDefense(gs, playerIdx) {
    var p = gs.players[playerIdx];
    if (!p || !p.defCardsCovered) return false;

    for (var slot = 1; slot <= 3; slot++) {
      if (p.defCardsCovered[slot] === false && p.defCards[slot] != null) {
        var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
        if (nonJokerHand.length < 1) return false;

        // Find weakest non-joker card to put face-down here
        var weakestId = null, weakestStr = 9999;
        for (var i = 0; i < nonJokerHand.length; i++) {
          var s = gs.cardStrength(nonJokerHand[i]);
          if (s < weakestStr) { weakestStr = s; weakestId = nonJokerHand[i]; }
        }
        if (weakestId === null) return false;

        // Move exposed card back to hand, place new card face-down
        var exposedCardId = p.defCards[slot];
        delete p.defCards[slot];
        delete p.defCardsCovered[slot];
        p.hand.push(exposedCardId);

        var hi = p.hand.indexOf(weakestId);
        if (hi !== -1) p.hand.splice(hi, 1);
        p.defCards[slot] = weakestId;
        if (!p.defCardsCovered) p.defCardsCovered = {};
        p.defCardsCovered[slot] = true;
        return true;
      }
    }
    return false;
  }

  // Returns true if the given player has Battery Tower with at least one charge.
  function defenderHasBatteryWithCharges(gs, defenderIdx) {
    var d = gs.players[defenderIdx];
    return d && (d.heroes || []).indexOf('Battery Tower') !== -1 && (d.batteryTowerCharges || 0) > 0;
  }

  // Perform a defense-card attack as the bot, respecting Battery Tower on the defender side.
  // atkPreviewData: the object already passed to gs.setAttackPreview()
  // resolveCallback: called after the attack is resolved (or denied) — no args.
  // If defender has Battery Tower, emits batteryDefenseCheck and stores pending state on sess.
  // Otherwise resolves immediately after BOT_ACTION_DELAY.
  function botDoDefAttackWithBatteryCheck(sess, gs, atkChoice, atkPreviewData, resolveCallback) {
    var defenderIdx = atkChoice.defenderIdx;
    if (defenderHasBatteryWithCharges(gs, defenderIdx)) {
      // Pause: store the pending attack on the session, then emit batteryDefenseCheck.
      // Resolution happens in the batteryAllowAttack / batteryDenyAttack socket handlers.
      sess.pendingBotBatteryAttack = {
        gs: gs,
        attackerIdx: atkPreviewData.attackerIdx,
        defenderIdx: defenderIdx,
        positionId: atkChoice.positionId,
        success: atkChoice.success,
        cardIds: atkChoice.cardIds,
        callback: resolveCallback
      };
      io.to(sess.id).emit('batteryDefenseCheck', {
        attackerIdx: atkPreviewData.attackerIdx,
        targetPlayerIdx: defenderIdx,
        positionId: atkChoice.positionId,
        level: 0,
        isKing: false,
        success: atkChoice.success,
        attackCardIds: atkChoice.cardIds
      });
    } else {
      setTimeout(function() {
        gs.defAttackResolved(atkPreviewData.attackerIdx, defenderIdx, atkChoice.positionId,
                             0, atkChoice.success, atkChoice.cardIds, false, []);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        checkAndHandleWinner(sess);
        resolveCallback();
      }, BOT_ACTION_DELAY);
    }
  }

  // Determine the hero name the bot should acquire from a joker sacrifice oracle card.
  function botHeroNameFromOracleCard(gs, oracleCardId) {
    var HERO_MAP = {
      2: 'Mercenaries', 3: 'Spy', 4: 'Marshal', 5: 'Battery Tower',
      6: 'Merchant',    7: 'Priest', 8: 'Reservists', 9: 'Saboteurs',
      10: 'Banneret',   11: 'Fortified Tower', 12: 'Magician', 13: 'Warlord'
    };
    var ALL_HEROES = Object.values(HERO_MAP);
    var RED_HEROES  = ['Merchant', 'Priest', 'Reservists', 'Mercenaries', 'Battery Tower', 'Marshal'];
    var BLACK_HEROES = ['Warlord', 'Spy', 'Banneret', 'Fortified Tower', 'Magician', 'Saboteurs'];

    // Collect already-owned heroes
    var owned = {};
    for (var i = 0; i < gs.players.length; i++) {
      (gs.players[i].heroes || []).forEach(function(h) { owned[h] = true; });
    }
    function pickFirst(list) {
      for (var j = 0; j < list.length; j++) { if (!owned[list[j]]) return list[j]; }
      return list.length > 0 ? list[0] : null; // fallback: grab any if all owned
    }

    if (oracleCardId > 52) return pickFirst(ALL_HEROES); // another joker: free choice

    var suitIdx = Math.floor((oracleCardId - 1) / 13);
    var cardIdx = (oracleCardId - 1) % 13 + 1;
    if (cardIdx === 1) {
      // Ace: red suits (diamonds=1, hearts=2) → red heroes; black → black heroes
      return pickFirst(suitIdx === 1 || suitIdx === 2 ? RED_HEROES : BLACK_HEROES);
    }
    return HERO_MAP[cardIdx] ? HERO_MAP[cardIdx] : pickFirst(ALL_HEROES);
  }

  // Sacrifice a joker for a hero, unless joker is needed for a critical action.
  // Returns true if performed.
  function botTryJokerSacrifice(sess, gs, playerIdx) {
    var p = gs.players[playerIdx];
    var jokers = p.hand.filter(function(id) { return id > 52; });
    if (jokers.length === 0) return false;

    // Exception 1: save joker to eliminate a player whose king is exposed
    for (var di = 0; di < gs.players.length; di++) {
      if (di === playerIdx || gs.players[di].isOut) continue;
      var defender = gs.players[di];
      var allEmpty = true;
      for (var s = 1; s <= 3; s++) {
        if (defender.defCards[s] != null || defender.topDefCards[s] != null) { allEmpty = false; break; }
      }
      if (allEmpty) {
        var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
        var bestCombo = botBestSuitCombo(gs, nonJokerHand);
        if (bestCombo.sum < gs.cardStrength(defender.kingCard)) return false; // need joker for kill
      }
    }

    // Exception 2: save joker if it is the only way to plunder a known deck
    if ((p.pickingDeckAttacks || 0) > 0) {
      var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
      var hasUncoveredDeck = false;
      var canPlunderWithoutJoker = false;
      for (var d = 0; d < gs.pickingDecks.length; d++) {
        var deck = gs.pickingDecks[d];
        if (deck.length === 0) continue;
        var topCard = deck[deck.length - 1];
        if (topCard.covered) continue;
        hasUncoveredDeck = true;
        var threshold = gs.cardStrength(topCard.id);
        var best = botBestSuitCombo(gs, nonJokerHand);
        if (best.sum >= threshold) { canPlunderWithoutJoker = true; break; }
      }
      if (hasUncoveredDeck && !canPlunderWithoutJoker) return false; // save joker for plunder
    }

    // Perform sacrifice
    if (gs.deck.length === 0) return false;
    var jokerId = jokers[0];
    var oracleCardId = gs.deck[gs.deck.length - 1];
    var heroName = botHeroNameFromOracleCard(gs, oracleCardId);
    if (!heroName) return false;

    gs.jokerSacrifice(playerIdx, jokerId, oracleCardId);
    gs.heroAcquired(playerIdx, heroName);
    io.to(sess.id).emit('stateUpdate', gs.serialize());
    return true;
  }

  function botFillDefense(gs, playerIdx) {
    var p = gs.players[playerIdx];
    var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
    for (var slot = 1; slot <= 3; slot++) {
      if (p.defCards[slot] == null && nonJokerHand.length > 1) {
        var weakestId = null, weakestStr = 9999;
        for (var i = 0; i < nonJokerHand.length; i++) {
          var s = gs.cardStrength(nonJokerHand[i]);
          if (s < weakestStr) { weakestStr = s; weakestId = nonJokerHand[i]; }
        }
        if (weakestId !== null) {
          gs.putDefCard(playerIdx, slot, weakestId);
          nonJokerHand.splice(nonJokerHand.indexOf(weakestId), 1);
        }
      }
    }
  }

  // Use any available active hero abilities before the main attack sequence.
  // Modifies gs in-place; returns true if any hero action was taken.
  function botUseActiveHeroes(gs, playerIdx) {
    var p = gs.players[playerIdx];
    if (!p) return false;
    var heroes = p.heroes || [];
    var acted = false;

    // Warlord king swap: upgrade king if a hand card is stronger
    if (heroes.indexOf('Warlord') !== -1 && (p.warlordAttacks || 0) > 0 && p.kingCard !== null) {
      var kingStr = gs.cardStrength(p.kingCard);
      var bestHandId = null, bestHandStr = kingStr;
      var nonJokerHand = p.hand.filter(function(id) { return id <= 52; });
      for (var i = 0; i < nonJokerHand.length; i++) {
        var s = gs.cardStrength(nonJokerHand[i]);
        if (s > bestHandStr) { bestHandStr = s; bestHandId = nonJokerHand[i]; }
      }
      if (bestHandId !== null) {
        gs.warlordKingSwap(playerIdx, p.kingCard, bestHandId);
        acted = true;
      }
    }

    // Merchant trade: discard weakest hand card and draw top deck card if it's an improvement
    if (heroes.indexOf('Merchant') !== -1 && (p.merchantTrades || 0) > 0 && gs.deck.length > 0) {
      var nonJokerHand2 = p.hand.filter(function(id) { return id <= 52; });
      if (nonJokerHand2.length > 0) {
        var weakestId = null, weakestStr2 = 9999;
        for (var i = 0; i < nonJokerHand2.length; i++) {
          var s = gs.cardStrength(nonJokerHand2[i]);
          if (s < weakestStr2) { weakestStr2 = s; weakestId = nonJokerHand2[i]; }
        }
        var topDeckId = gs.deck[gs.deck.length - 1];
        var topDeckStr = gs.cardStrength(topDeckId);
        if (weakestId !== null && topDeckStr > weakestStr2) {
          gs.merchantTrade(playerIdx, weakestId, topDeckId);
          // If the drawn card is a joker, do a second try with the next deck card
          var justDrawnIdx = p.hand.indexOf(topDeckId);
          var justDrawn = justDrawnIdx !== -1 ? topDeckId : null;
          if (justDrawn !== null && justDrawn > 52 && (p.merchantTrades === 0) && gs.deck.length > 0) {
            // merchantTrades was decremented — use merchantSecondTry to replace the joker
            var secondDeckId = gs.deck[gs.deck.length - 1];
            gs.merchantSecondTry(playerIdx, justDrawn, secondDeckId, false);
          }
          acted = true;
        }
      }
    }

    // Magician: replace an opponent's strongest defense slot with fresh deck cards
    if (heroes.indexOf('Magician') !== -1 && (p.magicianSpells || 0) > 0 && gs.deck.length >= 1) {
      var bestTargetInfo = null, bestTargetStr = 0;
      for (var di = 0; di < gs.players.length; di++) {
        if (di === playerIdx || gs.players[di].isOut) continue;
        var def = gs.players[di];
        for (var slot = 1; slot <= 3; slot++) {
          var defCardId = def.defCards[slot];
          if (defCardId == null) continue;
          var defStr = gs.cardStrength(defCardId);
          // Only cast on face-up (known) cards — avoids wasting spell on a weak hidden card
          var isFaceUp = def.defCardsCovered && def.defCardsCovered[slot] === false;
          if (isFaceUp && defStr > bestTargetStr) {
            bestTargetStr = defStr;
            var hasTop = def.topDefCards && def.topDefCards[slot] != null;
            bestTargetInfo = { di: di, slot: slot, hasTop: hasTop };
          }
        }
      }
      if (bestTargetInfo !== null && bestTargetStr > 7) { // only bother with medium+ cards
        var newBottom = gs.deck[gs.deck.length - 1];
        var newTop = -1;
        if (bestTargetInfo.hasTop && gs.deck.length >= 2) {
          newTop = gs.deck[gs.deck.length - 2];
        }
        gs.magicianSwap(playerIdx, bestTargetInfo.di, bestTargetInfo.slot,
                        newBottom, true, newTop, true);
        acted = true;
      }
    }

    // Fortified Tower: stack weakest hand card on a defense slot (1 fortify per turn)
    if (heroes.indexOf('Fortified Tower') !== -1 && p.hand.length > 0) {
      var fortified = false;
      for (var slot = 1; slot <= 3 && !fortified; slot++) {
        if (p.defCards[slot] != null && (p.topDefCards[slot] == null)) {
          var nonJokersForTower = p.hand.filter(function(id) { return id <= 52; });
          if (nonJokersForTower.length > 0) {
            var weakest = null, weakestS = 9999;
            for (var i = 0; i < nonJokersForTower.length; i++) {
              var s = gs.cardStrength(nonJokersForTower[i]);
              if (s < weakestS) { weakestS = s; weakest = nonJokersForTower[i]; }
            }
            if (weakest !== null) {
              gs.putTopDefCard(playerIdx, slot, weakest);
              fortified = true;
              acted = true;
            }
          }
        }
      }
    }

    return acted;
  }

  function executeBotTurn(sess) {
    var gs = sess.gameState;
    var idx = gs.currentPlayerIndex;
    var p = gs.players[idx];
    if (!p || p.isOut) return;

    // 1. Sacrifice joker for hero (unless saving it for critical use)
    botTryJokerSacrifice(sess, gs, idx);

    // 2. Fill empty defense slots with weakest hand cards
    botFillDefense(gs, idx);

    // 3. Replace any face-up (exposed) defense card with a fresh face-down card
    if (botReplaceExposedDefense(gs, idx)) {
      io.to(sess.id).emit('stateUpdate', gs.serialize());
    }

    // 3.5. Use active hero abilities (Warlord king swap, Merchant trade, Magician, Fortified Tower)
    if (botUseActiveHeroes(gs, idx)) {
      io.to(sess.id).emit('stateUpdate', gs.serialize());
    }

    // 4. Try to eliminate a player (king attack) — highest priority; async with overlay delay
    botTryKingAttackAsync(sess, gs, idx, function(didKingAttack) {
      if (didKingAttack) {
        botFinishTurn(sess, gs, idx, true);
        return;
      }

      // 5. Smart plunder — multi-card, economical combo
      var plunderChoice = botChoosePlunder(gs, idx);
      if (plunderChoice) {
        var plAtkSum = 0;
        for (var pci = 0; pci < plunderChoice.cardIds.length; pci++) plAtkSum += gs.cardStrength(plunderChoice.cardIds[pci]);
        var plDeck = gs.pickingDecks[plunderChoice.deckIndex];
        var plTopCard = plDeck && plDeck.length > 0 ? plDeck[plDeck.length - 1] : null;
        var plDefStrength = plTopCard ? gs.cardStrength(plTopCard.id) : 0;
        gs.setPlunderPreview({ attackerIdx: idx, deckIndex: plunderChoice.deckIndex,
                               attackCardIds: plunderChoice.cardIds,
                               attackingSymbol: plunderChoice.symbol, attackingSymbol2: 'none',
                               success: plunderChoice.success, attackSum: plAtkSum,
                               defCardId: plTopCard ? plTopCard.id : -1,
                               defStrength: plDefStrength });
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        var captured = plunderChoice;
        setTimeout(function() {
          gs.plunderResolved(idx, captured.deckIndex, captured.success,
                             captured.cardIds, false, []);
          io.to(sess.id).emit('stateUpdate', gs.serialize());
          checkAndHandleWinner(sess);
          // 6. After plunder: optional follow-up defense attack, then finish
          botContinueAfterPlunder(sess, gs, idx);
        }, BOT_ACTION_DELAY);
        return;
      }

      // 7. No plunder: attack a face-up defense card
      var atkChoice = botChooseDefAttack(gs, idx, false);
      if (atkChoice) {
        var atkDefCardId = gs.players[atkChoice.defenderIdx].defCards[atkChoice.positionId];
        var atkPreview = { attackerIdx: idx, defenderIdx: atkChoice.defenderIdx,
                           positionId: atkChoice.positionId, level: 0,
                           attackingSymbol: atkChoice.symbol, attackingSymbol2: 'none',
                           success: atkChoice.success, attackCardIds: atkChoice.cardIds,
                           defCardIds: atkDefCardId != null ? [atkDefCardId] : [] };
        gs.setAttackPreview(atkPreview);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        botDoDefAttackWithBatteryCheck(sess, gs, atkChoice, atkPreview, function() {
          botFinishTurn(sess, gs, idx, true);
        });
        return;
      }

      // 8. Scout: probe a covered defense card with a weak card to reveal it for future turns
      var scoutChoice = botChooseDefAttack(gs, idx, true);
      if (scoutChoice) {
        var scoutDefCardId = gs.players[scoutChoice.defenderIdx].defCards[scoutChoice.positionId];
        var scoutPreview = { attackerIdx: idx, defenderIdx: scoutChoice.defenderIdx,
                             positionId: scoutChoice.positionId, level: 0,
                             attackingSymbol: scoutChoice.symbol, attackingSymbol2: 'none',
                             success: scoutChoice.success, attackCardIds: scoutChoice.cardIds,
                             defCardIds: scoutDefCardId != null ? [scoutDefCardId] : [] };
        gs.setAttackPreview(scoutPreview);
        io.to(sess.id).emit('stateUpdate', gs.serialize());
        botDoDefAttackWithBatteryCheck(sess, gs, scoutChoice, scoutPreview, function() {
          botFinishTurn(sess, gs, idx, true);
        });
        return;
      }

      // No attack of any kind — expose defense or king, then finish
      botFinishTurn(sess, gs, idx, false);
    });
  }

  // Legacy helpers (still referenced from tutorial/other paths)
  function botStrongestCard(gs, hand) {
    if (!hand || hand.length === 0) return null;
    var best = hand[0], bestStr = gs.cardStrength(hand[0]);
    for (var i = 1; i < hand.length; i++) {
      var s = gs.cardStrength(hand[i]);
      if (s > bestStr) { bestStr = s; best = hand[i]; }
    }
    return best;
  }

  function botWeakestCard(gs, hand) {
    if (!hand || hand.length === 0) return null;
    var best = hand[0], bestStr = gs.cardStrength(hand[0]);
    for (var i = 1; i < hand.length; i++) {
      var s = gs.cardStrength(hand[i]);
      if (s < bestStr) { bestStr = s; best = hand[i]; }
    }
    return best;
  }

  /**
   * Compute the smart manual-setup card selection for a bot player.
   * Strategy:
   *   - King card  : highest-strength non-joker (jokers used only as fallback)
   *   - Def cards  : next 3 highest-strength non-jokers
   *   - Joker      : always kept in hand when possible
   *   - Discard    : 2 lowest-strength remaining cards (non-jokers preferred)
   *
   * Returns { kingId, defIds, discardIds } or null if the hand is too small.
   */
  function botComputeSetup(gs, playerIdx) {
    var bp = gs.players[playerIdx];
    if (!bp || bp.hand.length < 4) return null;

    var jokers = bp.hand.filter(function(id) { return id > 52; });
    var nonJokers = bp.hand.filter(function(id) { return id <= 52; });

    // Sort non-jokers descending by strength (highest = best king/def candidate)
    nonJokers.sort(function(a, b) { return gs.cardStrength(b) - gs.cardStrength(a); });

    // Build king + def from non-jokers first; use jokers only if non-jokers run out
    var pool = nonJokers.concat(jokers);
    var kingId = pool[0];
    var defIds = [pool[1], pool[2], pool[3]];

    // Remaining cards (not king or def), jokers pushed to end so they are discarded last
    var assigned = new Set([kingId, pool[1], pool[2], pool[3]]);
    var remaining = bp.hand.filter(function(id) { return !assigned.has(id); });
    remaining.sort(function(a, b) {
      var aJoker = a > 52, bJoker = b > 52;
      if (aJoker !== bJoker) return aJoker ? 1 : -1; // jokers survive, go last
      return gs.cardStrength(a) - gs.cardStrength(b); // lowest-strength first
    });

    // Discard 2 lowest (mirrors the 2 cemetery cards from auto-setup)
    var discardIds = remaining.slice(0, 2);

    return { kingId: kingId, defIds: defIds, discardIds: discardIds };
  }

  return {
    isBot: isBot,
    playBotTurnIfNeeded: playBotTurnIfNeeded,
    autoSetupBot: botComputeSetup
  };
};
