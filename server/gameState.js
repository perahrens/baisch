// Server-authoritative game state

class GameState {
  constructor(users, opts) {
    const startingCards = (opts && opts.startingCards) ? Math.min(10, Math.max(6, parseInt(opts.startingCards, 10))) : 8;
    const manualSetup = !!(opts && opts.manualSetup);
    this.deck = this.generateCards();
    this.cemetery = [];
    this.players = users.map((user, idx) => ({
      id: user.id,
      name: user.name || ('Player' + idx),
      index: idx,
      hand: [],
      defCards: {},
      topDefCards: {},
      kingCard: null,
      heroes: [],
      preyCards: [],
    }));
    this.pickingDecks = [[], []]; // each entry: { id, covered }
    this.currentPlayerIndex = 0;
    this.log = []; // activity log: [{ text, success }, ...], max 5 entries
    this.lastMerchantReveal = null; // set during 2nd-try, cleared on finishTurn
    this.pendingAttack = null; // current attack preview broadcast, cleared on defAttackResolved
    this.pendingPlunder = null; // current plunder preview broadcast, cleared on plunderResolved
    this.setupPhase = manualSetup;
    this.setupSubmitted = {}; // { playerIdx: true } — tracks who confirmed during manual setup
    this.dealCards(startingCards);
    if (manualSetup) {
      this.initPlayerStats(); // init combat counters without placing king/def/cemetery
    } else {
      this.doSetup();
      this.initPickingDecks();
    }
  }

  /** Initialise per-player combat counters (shared by auto and manual setup). */
  initPlayerStats() {
    for (const p of this.players) {
      p.kingCovered = true;
      p.isOut = false;
      p.defCardsCovered = { 1: true, 2: true, 3: true };
      p.topDefCardsCovered = {};
      p.sabotaged = {};
      p.attackCount = 0;
      p.priestConversionAttempts = 2;
      p.magicianSpells = 1;
      p.merchantTrades = 1;
      p.warlordAttacks = 1;
      p.spyAttacks = 1;
      p.spyMaxAttacks = 1;
      p.spyExtends = 1;
      p.pickingDeckAttacks = 1;
      p.attackingSymbol = 'none';
      p.attackingSymbol2 = 'none';
    }
  }

  /**
   * Apply a manual setup submission for one player.
   * kingCardId and defCardIds (array of 3) must all be in the player's current hand.
   * Returns true on success, false if validation fails.
   */
  applyManualSetup(playerIdx, kingCardId, defCardIds, discardCardIds) {
    const p = this.players[playerIdx];
    if (!p) return false;
    if (this.setupSubmitted[playerIdx]) return false; // already submitted

    const ids = [kingCardId, ...defCardIds];
    if (ids.length !== 4) return false;

    // All cards must be in hand and no duplicates
    const seen = new Set();
    for (const id of ids) {
      if (p.hand.indexOf(id) === -1) return false;
      if (seen.has(id)) return false;
      seen.add(id);
    }

    // Remove from hand and place
    for (const id of ids) {
      const i = p.hand.indexOf(id);
      p.hand.splice(i, 1);
    }
    p.kingCard = kingCardId;
    p.defCards = { 1: defCardIds[0], 2: defCardIds[1], 3: defCardIds[2] };

    // Remove discarded cards from hand (must not be king or def; anything else is fair game)
    if (Array.isArray(discardCardIds)) {
      const keepSet = new Set([kingCardId, ...defCardIds]);
      for (const id of discardCardIds) {
        if (keepSet.has(id)) continue; // ignore invalid discard of selected cards
        const i = p.hand.indexOf(id);
        if (i !== -1) p.hand.splice(i, 1);
      }
    }

    this.setupSubmitted[playerIdx] = true;

    // If all players have submitted, finalise setup and start the game
    let allDone = true;
    for (let i = 0; i < this.players.length; i++) {
      if (!this.setupSubmitted[i]) { allDone = false; break; }
    }
    if (allDone) {
      this.setupPhase = false;
      this.initPickingDecks();
    }
    return true;
  }

  generateCards() {
    let cards = [];
    for (let i = 1; i <= 52; i++) cards.push(i);
    // IDs 53, 54, 55 = joker cards
    cards.push(53); cards.push(54); cards.push(55);
    for (let i = cards.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [cards[i], cards[j]] = [cards[j], cards[i]];
    }
    return cards;
  }

  dealCards(n) {
    for (const p of this.players) {
      for (let i = 0; i < n; i++) {
        if (this.deck.length > 0) p.hand.push(this.deck.pop());
      }
    }
  }

  // Mirrors Java doRandomSetup: king + 3 defCards + 2 cemetery per player
  doSetup() {
    this.initPlayerStats();
    for (const p of this.players) {
      p.kingCard = p.hand.pop();
      for (let j = 1; j <= 3; j++) p.defCards[j] = p.hand.pop();
      for (let j = 0; j < 2; j++) this.cemetery.push(p.hand.pop());
    }
  }

  pname(idx) {
    return (this.players[idx] && this.players[idx].name) || ('P' + idx);
  }

  setPlunderPreview(data) {
    // Immediately remove committed hand-based attack cards from attacker's hand.
    // This prevents them reappearing in hand if the player refreshes before confirming the overlay.
    const p = data.attackerIdx !== undefined ? this.players[data.attackerIdx] : null;
    if (p) {
      if (p.pickingDeckAttacks > 0) p.pickingDeckAttacks--;
      if (data.attackingSymbol) { p.attackingSymbol = data.attackingSymbol; p.attackingSymbol2 = data.attackingSymbol2 || 'none'; }
    }
    const lockedHandCards = [];
    if (p && data.attackCardIds) {
      for (const cardId of data.attackCardIds) {
        const i = p.hand.indexOf(cardId);
        if (i !== -1) { p.hand.splice(i, 1); lockedHandCards.push(cardId); }
      }
    }
    this.pendingPlunder = Object.assign({}, data, { _lockedHandCards: lockedHandCards });
  }

  setAttackPreview(data) {
    // Track attacking symbol so the client can restore it on page refresh
    if (data.attackerIdx !== undefined && data.attackingSymbol) {
      const atk = this.players[data.attackerIdx];
      if (atk) { atk.attackingSymbol = data.attackingSymbol; atk.attackingSymbol2 = data.attackingSymbol2 || 'none'; }
    }
    this.pendingAttack = data;
    // Mark targeted defense card(s) as face-up so the defender sees the reveal immediately
    const { defenderIdx, positionId, level } = data;
    const defender = this.players[defenderIdx];
    if (defender && positionId !== undefined) {
      if (!defender.defCardsCovered) defender.defCardsCovered = {};
      if (!defender.topDefCardsCovered) defender.topDefCardsCovered = {};
      if (level === 0) {
        if (defender.defCards[positionId] !== undefined) defender.defCardsCovered[positionId] = false;
        if (defender.topDefCards[positionId] !== undefined) defender.topDefCardsCovered[positionId] = false;
      } else {
        if (defender.topDefCards[positionId] !== undefined) defender.topDefCardsCovered[positionId] = false;
      }
    }
  }

  pushLog(text, success, neutral = false) {
    this.log.push({ text, success, neutral });
  }

  cardStrength(cardId) {
    if (!cardId || cardId > 52) return 999;
    const index = (cardId - 1) % 13 + 1;
    return index === 1 ? 14 : index;
  }

  // Mirrors Java picking deck init (5 cards, card1/card2 face-up, rest face-down)
  initPickingDecks() {
    const c = [];
    for (let i = 0; i < 5; i++) {
      if (this.deck.length > 0) c.push(this.deck.pop());
    }
    this.pickingDecks[0].push({ id: c[0], covered: false });
    this.pickingDecks[1].push({ id: c[1], covered: false });
    this.pickingDecks[0].push({ id: c[2], covered: true });
    this.pickingDecks[1].push({ id: c[3], covered: true });
    if (this.cardStrength(c[0]) < this.cardStrength(c[1])) {
      this.pickingDecks[0].push({ id: c[4], covered: true });
    } else {
      this.pickingDecks[1].push({ id: c[4], covered: true });
    }
  }

  /**
   * Draw the next card from the deck, automatically reshuffling the cemetery into
   * the deck (with a fresh Fisher-Yates shuffle) when the deck runs empty.
   * Returns null only when both the deck and the cemetery are truly empty.
   */
  pickCard() {
    if (this.deck.length === 0) {
      if (this.cemetery.length > 0) {
        this.deck = [...this.cemetery];
        this.cemetery = [];
        for (let i = this.deck.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [this.deck[i], this.deck[j]] = [this.deck[j], this.deck[i]];
        }
        console.log('Deck empty — reshuffled cemetery (' + this.deck.length + ' cards) into deck');
      } else {
        return null; // both deck and cemetery are empty
      }
    }
    return this.deck.pop();
  }

  priestAttemptFailed(attackerIdx) {
    const attacker = this.players[attackerIdx];
    if ((attacker.priestConversionAttempts || 0) <= 0) return;
    attacker.priestConversionAttempts--;
    this.pushLog(`${this.pname(attackerIdx)} (Priest) missed — no match`, false);
  }

  priestConvert(attackerIdx, targetPlayerIdx, cardId) {
    const attacker = this.players[attackerIdx];
    // Enforce server-authoritative attempt limit (2 per turn).
    if ((attacker.priestConversionAttempts || 0) <= 0) {
      console.log(`priestConvert: rejected — player ${attackerIdx} has no attempts remaining`);
      return;
    }
    const target = this.players[targetPlayerIdx];
    const idx = target.hand.indexOf(cardId);
    if (idx !== -1) {
      target.hand.splice(idx, 1);
      attacker.hand.push(cardId);
    }
    attacker.priestConversionAttempts = 0; // success burns all remaining attempts
    this.pushLog(`${this.pname(attackerIdx)} (Priest) took card from ${this.pname(targetPlayerIdx)}`, true);
  }

  // ---- Saboteurs actions ----

  sabotage(attackerIdx, defenderIdx, positionId) {
    const defender = this.players[defenderIdx];
    if (!defender.sabotaged) defender.sabotaged = {};
    defender.sabotaged[positionId] = attackerIdx;
    this.pushLog(`${this.pname(attackerIdx)} placed saboteur on ${this.pname(defenderIdx)}'s field [${positionId}]`, true);
  }

  sabotageCallback(attackerIdx, defenderIdx, positionId) {
    const defender = this.players[defenderIdx];
    if (defender.sabotaged) delete defender.sabotaged[positionId];
    this.pushLog(`${this.pname(attackerIdx)} recalled saboteur from ${this.pname(defenderIdx)}'s field [${positionId}]`, true, true);
  }

  sabotageSacrifice(defenderIdx, positionId) {
    const p = this.players[defenderIdx];
    const attackerIdx = p.sabotaged ? p.sabotaged[positionId] : undefined;
    const cardId = p.defCards[positionId];
    if (cardId !== undefined) {
      delete p.defCards[positionId];
      if (p.defCardsCovered) delete p.defCardsCovered[positionId];
      this.cemetery.push(cardId);
    }
    if (p.sabotaged) delete p.sabotaged[positionId];
    this.pushLog(`${this.pname(defenderIdx)} sacrificed shield [${positionId}] to destroy saboteur`, false);
    return attackerIdx;
  }

  sabotageEmptySlotSacrifice(defenderIdx, positionId, handCardId) {
    const p = this.players[defenderIdx];
    const attackerIdx = p.sabotaged ? p.sabotaged[positionId] : undefined;
    const i = p.hand.indexOf(handCardId);
    if (i !== -1) p.hand.splice(i, 1);
    this.cemetery.push(handCardId);
    if (p.sabotaged) delete p.sabotaged[positionId];
    this.pushLog(`${this.pname(defenderIdx)} sacrificed a card to clear saboteur at [${positionId}]`, false);
    return attackerIdx;
  }

  // ---- Action methods ----

  takeDefCard(playerIdx, positionId) {
    const p = this.players[playerIdx];
    const cardId = p.defCards[positionId];
    if (cardId !== undefined) {
      p.hand.push(cardId);
      delete p.defCards[positionId];
      if (p.defCardsCovered) delete p.defCardsCovered[positionId];
    }
    const topCardId = p.topDefCards[positionId];
    if (topCardId !== undefined) {
      p.hand.push(topCardId);
      delete p.topDefCards[positionId];
      if (p.topDefCardsCovered) delete p.topDefCardsCovered[positionId];
    }
    this.pushLog(`${this.pname(playerIdx)} took shield [${positionId}] to hand`, true, true);
  }

  putDefCard(playerIdx, positionId, cardId) {
    const p = this.players[playerIdx];
    const i = p.hand.indexOf(cardId);
    if (i !== -1) p.hand.splice(i, 1);
    p.defCards[positionId] = cardId;
    if (!p.defCardsCovered) p.defCardsCovered = {};
    p.defCardsCovered[positionId] = true; // newly placed card is always face-down
    this.pushLog(`${this.pname(playerIdx)} placed shield at [${positionId}]`, true, true);
  }

  putTopDefCard(playerIdx, positionId, cardId) {
    const p = this.players[playerIdx];
    const i = p.hand.indexOf(cardId);
    if (i !== -1) p.hand.splice(i, 1);
    p.topDefCards[positionId] = cardId;
    if (!p.topDefCardsCovered) p.topDefCardsCovered = {};
    p.topDefCardsCovered[positionId] = true; // newly stacked card is face-down
    this.pushLog(`${this.pname(playerIdx)} fortified shield at [${positionId}]`, true, true);
  }

  magicianSwap(playerIdx, targetPlayerIdx, positionId, newBottomCardId, bottomCovered, newTopCardId, topCovered) {
    const attacker = this.players[playerIdx];
    if ((attacker.magicianSpells || 0) <= 0) {
      console.log(`magicianSwap: rejected — player ${playerIdx} has no spells remaining`);
      return;
    }

    const target = this.players[targetPlayerIdx];
    // Discard old bottom card
    const oldBottom = target.defCards[positionId];
    if (oldBottom !== undefined) this.cemetery.push(oldBottom);
    // Discard old top card (if any)
    const oldTop = target.topDefCards[positionId];
    if (oldTop !== undefined) { this.cemetery.push(oldTop); delete target.topDefCards[positionId]; }
    // Remove from deck and place new bottom card
    const bottomIdx = this.deck.indexOf(newBottomCardId);
    if (bottomIdx !== -1) this.deck.splice(bottomIdx, 1);
    target.defCards[positionId] = newBottomCardId;
    if (!target.defCardsCovered) target.defCardsCovered = {};
    target.defCardsCovered[positionId] = bottomCovered;
    // Place new top card if the slot was originally stacked
    if (newTopCardId !== -1) {
      const topIdx = this.deck.indexOf(newTopCardId);
      if (topIdx !== -1) this.deck.splice(topIdx, 1);
      target.topDefCards[positionId] = newTopCardId;
      if (!target.topDefCardsCovered) target.topDefCardsCovered = {};
      target.topDefCardsCovered[positionId] = topCovered;
    }
    attacker.magicianSpells--;
    this.pushLog(`${this.pname(playerIdx)} cast Magician on ${this.pname(targetPlayerIdx)}'s shield [${positionId}]`, true);
  }

  spyFlip(playerIdx, targetPlayerIdx, slot) {
    const p = this.players[playerIdx];
    if (!p) return false;
    if ((p.spyAttacks || 0) <= 0) {
      console.log(`spyFlip: rejected attack — player ${playerIdx} has no spy attacks remaining`);
      return false;
    }
    p.spyAttacks--;
    // Persist the flip so the subsequent stateUpdate doesn't re-cover the card
    if (slot === -1) {
      const target = this.players[targetPlayerIdx];
      if (target) target.kingCovered = false;
    }
    return true;
  }

  spyExtend(playerIdx, cardId) {
    const p = this.players[playerIdx];
    if (!p) return false;
    if ((p.spyExtends || 0) <= 0) {
      console.log(`spyExtend: rejected — player ${playerIdx} has no extends remaining`);
      return false;
    }
    // Remove the sacrificed card from hand, defCards, or topDefCards
    if (cardId !== undefined && cardId !== null) {
      const handIdx = p.hand.indexOf(cardId);
      if (handIdx !== -1) {
        p.hand.splice(handIdx, 1);
      } else {
        for (const key of Object.keys(p.defCards || {})) {
          if (p.defCards[key] === cardId) {
            delete p.defCards[key];
            if (p.defCardsCovered) delete p.defCardsCovered[key];
            break;
          }
        }
        for (const key of Object.keys(p.topDefCards || {})) {
          if (p.topDefCards[key] === cardId) {
            delete p.topDefCards[key];
            if (p.topDefCardsCovered) delete p.topDefCardsCovered[key];
            break;
          }
        }
      }
      this.cemetery.push(cardId);
    }
    p.spyExtends--;
    p.spyAttacks = Math.min((p.spyAttacks || 0) + 2, 3);
    p.spyMaxAttacks = Math.min((p.spyMaxAttacks || 1) + 2, 3);
    return true;
  }

  addToCemetery(playerIdx, cardIds, drawFromDeck) {
    const p = this.players[playerIdx];
    for (const cardId of cardIds) {
      const i = p.hand.indexOf(cardId);
      if (i !== -1) p.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    for (let i = 0; i < (drawFromDeck || 0); i++) {
      const drawn = this.pickCard();
      if (drawn !== null) this.cemetery.push(drawn);
    }
  }

  discardDefCards(playerIdx, slots) {
    const p = this.players[playerIdx];
    for (const entry of slots) {
      const slot = entry.slot;
      if (entry.isTop) {
        const cardId = p.topDefCards[slot];
        if (cardId !== undefined) { this.cemetery.push(cardId); delete p.topDefCards[slot]; }
        if (p.topDefCardsCovered) delete p.topDefCardsCovered[slot];
      } else {
        const cardId = p.defCards[slot];
        if (cardId !== undefined) { this.cemetery.push(cardId); delete p.defCards[slot]; }
        if (p.defCardsCovered) delete p.defCardsCovered[slot];
      }
    }
    this.pushLog(`${this.pname(playerIdx)} discarded own shield(s)`, true, true);
  }

  plunderResolved(attackerIdx, deckIdx, success, attackCardIds, kingUsed, attackerOwnDefCardIds) {
    // Use cards pre-locked in setPlunderPreview if available (prevents reappearance on refresh)
    const lockedHandCards = this.pendingPlunder ? (this.pendingPlunder._lockedHandCards || []) : [];
    this.pendingPlunder = null;
    const attacker = this.players[attackerIdx];
    const handCardsToProcess = lockedHandCards.length > 0 ? lockedHandCards : attackCardIds;
    for (const cardId of handCardsToProcess) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1); // already removed from hand if locking was used
      this.cemetery.push(cardId);
    }
    // Banneret: own def cards used as attackers go to cemetery
    for (const cardId of (attackerOwnDefCardIds || [])) {
      for (const slot of Object.keys(attacker.defCards)) {
        if (attacker.defCards[slot] === cardId) { delete attacker.defCards[slot]; break; }
      }
      for (const slot of Object.keys(attacker.topDefCards || {})) {
        if (attacker.topDefCards[slot] === cardId) { delete attacker.topDefCards[slot]; break; }
      }
      this.cemetery.push(cardId);
    }
    if (kingUsed) attacker.kingCovered = false;
    if (success) {
      this.pushLog(`${this.pname(attackerIdx)} plundered deck ${deckIdx + 1}!`, true);
      // Move all cards from plundered deck into attacker's hand
      for (const c of this.pickingDecks[deckIdx]) attacker.hand.push(c.id);
      this.pickingDecks[deckIdx] = [];
      const otherIdx = 1 - deckIdx;
      // Add one face-DOWN card to the other deck (deck B grows by one hidden card)
      const c1 = this.pickCard(); if (c1 !== null) this.pickingDecks[otherIdx].push({ id: c1, covered: true });
      // Rebuild plundered deck: one face-up card (visible) + one face-down card on top
      const c2 = this.pickCard(); if (c2 !== null) this.pickingDecks[deckIdx].push({ id: c2, covered: false });
      const c3 = this.pickCard(); if (c3 !== null) this.pickingDecks[deckIdx].push({ id: c3, covered: true });
    } else {
      this.pushLog(`${this.pname(attackerIdx)} plunder on deck ${deckIdx + 1} failed`, false);
      if (kingUsed) attacker.isOut = true;
      // Keep the attacked (top) card face-up after a failed plunder,
      // then add a new face-down card on top.
      const deck = this.pickingDecks[deckIdx];
      if (deck.length > 0) deck[deck.length - 1].covered = false;
      const c4 = this.pickCard(); if (c4 !== null) this.pickingDecks[deckIdx].push({ id: c4, covered: true });
    }
  }

  defAttackResolved(attackerIdx, defenderIdx, positionId, level, success, attackCardIds, kingUsed, attackerOwnDefCardIds) {
    this.pendingAttack = null;
    const attacker = this.players[attackerIdx];
    attacker.attackCount = (attacker.attackCount || 0) + 1;
    const defender = this.players[defenderIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    // Banneret: own def cards used as attackers go to cemetery
    for (const cardId of (attackerOwnDefCardIds || [])) {
      for (const slot of Object.keys(attacker.defCards)) {
        if (attacker.defCards[slot] === cardId) { delete attacker.defCards[slot]; break; }
      }
      for (const slot of Object.keys(attacker.topDefCards || {})) {
        if (attacker.topDefCards[slot] === cardId) { delete attacker.topDefCards[slot]; break; }
      }
      this.cemetery.push(cardId);
    }
    if (kingUsed) attacker.kingCovered = false;
    if (success) {
      this.pushLog(`${this.pname(attackerIdx)} broke ${this.pname(defenderIdx)}'s shield [${positionId}]`, true);
      // If the slot was sabotaged, clear it (saboteur destroyed when card is removed by attack)
      if (defender.sabotaged && defender.sabotaged[positionId] !== undefined) {
        delete defender.sabotaged[positionId];
      }
      if (!attacker.preyCards) attacker.preyCards = [];
      if (level === 0) {
        const defCardId = defender.defCards[positionId];
        if (defCardId !== undefined) { attacker.hand.push(defCardId); attacker.preyCards.push(defCardId); delete defender.defCards[positionId]; }
        const topCardId = defender.topDefCards[positionId];
        if (topCardId !== undefined) { attacker.hand.push(topCardId); attacker.preyCards.push(topCardId); delete defender.topDefCards[positionId]; }
      } else {
        const topCardId = defender.topDefCards[positionId];
        if (topCardId !== undefined) { attacker.hand.push(topCardId); attacker.preyCards.push(topCardId); delete defender.topDefCards[positionId]; }
      }
    } else {
      this.pushLog(`${this.pname(attackerIdx)} missed ${this.pname(defenderIdx)}'s shield [${positionId}]`, false);
      if (kingUsed) attacker.isOut = true;
      // Mark attacked card(s) as revealed (face-up) — they stay in defCards but must remain visible
      if (!defender.defCardsCovered) defender.defCardsCovered = {};
      if (!defender.topDefCardsCovered) defender.topDefCardsCovered = {};
      if (level === 0) {
        defender.defCardsCovered[positionId] = false;
        if (defender.topDefCards[positionId] !== undefined) defender.topDefCardsCovered[positionId] = false;
      } else {
        defender.topDefCardsCovered[positionId] = false;
      }
    }
  }

  finishTurn() {
    // Always use the server's own authoritative currentPlayerIndex — never trust the client.
    const currentPlayerIndex = this.currentPlayerIndex;
    this.lastMerchantReveal = null; // clear Merchant 2nd-try reveal on turn end
    if (this.players[currentPlayerIndex]) {
      this.players[currentPlayerIndex].preyCards = [];
      this.players[currentPlayerIndex].attackCount = 0; // reset for next turn
      this.players[currentPlayerIndex].priestConversionAttempts = 2; // reset Priest uses for next turn
      this.players[currentPlayerIndex].magicianSpells = 1;
      this.players[currentPlayerIndex].merchantTrades = 1;
      this.players[currentPlayerIndex].warlordAttacks = 1;
      this.players[currentPlayerIndex].spyAttacks = 1;
      this.players[currentPlayerIndex].spyMaxAttacks = 1;
      this.players[currentPlayerIndex].spyExtends = 1;
      this.players[currentPlayerIndex].pickingDeckAttacks = 1;
      this.players[currentPlayerIndex].attackingSymbol = 'none';
      this.players[currentPlayerIndex].attackingSymbol2 = 'none';
    }
    // Advance to the next non-eliminated player (server is authoritative)
    const n = this.players.length;
    let next = (currentPlayerIndex + 1) % n;
    let safety = 0;
    while (this.players[next].isOut && next !== currentPlayerIndex && safety++ < n) {
      next = (next + 1) % n;
    }
    this.currentPlayerIndex = next;
  }

  exposeDefCard(playerIdx, slot) {
    const p = this.players[playerIdx];
    if (!p) return;
    if (!p.defCardsCovered) p.defCardsCovered = {};
    if (!p.topDefCardsCovered) p.topDefCardsCovered = {};
    // Expose the top card first if present (it sits on the regular card)
    if (p.topDefCards[slot] !== undefined) {
      p.topDefCardsCovered[slot] = false;
    } else if (p.defCards[slot] !== undefined) {
      p.defCardsCovered[slot] = false;
    }
    this.pushLog(`${this.pname(playerIdx)} exposed slot ${slot} (no attack)`, false, true);
  }

  exposeKingCard(playerIdx) {
    const p = this.players[playerIdx];
    if (!p) return;
    p.kingCovered = false;
    this.pushLog(`${this.pname(playerIdx)} exposed their king (no attack)`, false, true);
  }

  checkWinner() {
    const alive = this.players.filter(p => !p.isOut);
    return alive.length === 1 ? alive[0].index : -1;
  }

  /**
   * A player sacrificed a joker card to draw a hero-determining card.
   * Removes both cards from deck/hand and adds them to the cemetery
   * so all clients stay in sync after they receive this stateUpdate.
   */
  jokerSacrifice(playerIdx, jokerCardId, drawnCardId) {
    const p = this.players[playerIdx];
    // Remove joker from the player's hand.
    const jokerHandIdx = p.hand.indexOf(jokerCardId);
    if (jokerHandIdx !== -1) p.hand.splice(jokerHandIdx, 1);
    this.cemetery.push(jokerCardId);
    // Remove the drawn card from the deck (it was the "oracle" card for hero type).
    const drawnDeckIdx = this.deck.indexOf(drawnCardId);
    if (drawnDeckIdx !== -1) this.deck.splice(drawnDeckIdx, 1);
    this.cemetery.push(drawnCardId);
    this.pushLog(`${this.pname(playerIdx)} sacrificed a Joker for a Hero`, true, true);
  }

  kingAttackResolved(attackerIdx, defenderIdx, success, attackCardIds, kingUsed) {
    const attacker = this.players[attackerIdx];
    attacker.attackCount = (attacker.attackCount || 0) + 1;
    const defender = this.players[defenderIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    if (kingUsed) attacker.kingCovered = false;
    if (success) {
      this.pushLog(`${this.pname(attackerIdx)} defeated ${this.pname(defenderIdx)}!`, true);
      // Defender loses their king and is eliminated; attacker gains their cards as prey
      defender.isOut = true;
      if (!attacker.preyCards) attacker.preyCards = [];
      for (const cardId of defender.hand) {
        attacker.hand.push(cardId);
        attacker.preyCards.push(cardId);
      }
      defender.hand = [];
      if (defender.kingCard !== null) {
        attacker.hand.push(defender.kingCard);
        attacker.preyCards.push(defender.kingCard);
        defender.kingCard = null;
      }
      // Hero stealing: auto-transfer if defender has exactly one hero; queue selection otherwise
      const defHeroes = defender.heroes || [];
      if (defHeroes.length === 1) {
        this.heroAcquired(attackerIdx, defHeroes[0]);
      } else if (defHeroes.length > 1) {
        this.pendingHeroSelection = { attackerIdx, defenderIdx, options: [...defHeroes] };
      }
    } else {
      this.pushLog(`${this.pname(attackerIdx)} king assault on ${this.pname(defenderIdx)} failed`, false);
      if (kingUsed) attacker.isOut = true;
    }
  }

  resolveHeroSelection(heroName) {
    if (!this.pendingHeroSelection) return;
    const { attackerIdx, defenderIdx } = this.pendingHeroSelection;
    const defender = this.players[defenderIdx];
    // Remove all of the defender's heroes (they return to the pool / are lost)
    defender.heroes = [];
    // Give the chosen hero to the attacker (heroAcquired removes it from all players first)
    this.heroAcquired(attackerIdx, heroName);
    this.pendingHeroSelection = null;
  }

  warlordDirectAttack(playerIdx) {
    const p = this.players[playerIdx];
    if ((p.warlordAttacks || 0) <= 0) {
      console.log(`warlordDirectAttack: rejected — player ${playerIdx} has no attacks remaining`);
      return false;
    }
    p.warlordAttacks--;
    this.pushLog(`${this.pname(playerIdx)} used Warlord direct attack`, true, true);
    return true;
  }

  warlordKingSwap(playerIdx, oldKingCardId, newKingCardId) {
    const p = this.players[playerIdx];
    if ((p.warlordAttacks || 0) <= 0) {
      console.log(`warlordKingSwap: rejected — player ${playerIdx} has no attacks remaining`);
      return;
    }
    const handIdx = p.hand.indexOf(newKingCardId);
    if (handIdx === -1) return;
    p.hand.splice(handIdx, 1);
    p.hand.push(oldKingCardId);
    p.kingCard = newKingCardId;
    p.kingCovered = true; // new king is always placed face-down
    p.warlordAttacks--;
    this.pushLog(`${this.pname(playerIdx)} swapped king (Warlord)`, true, true);
  }

  merchantTrade(playerIdx, discardedCardId, drawnCardId) {
    const p = this.players[playerIdx];
    if ((p.merchantTrades || 0) <= 0) {
      console.log(`merchantTrade: rejected — player ${playerIdx} has no trades remaining`);
      return;
    }
    this.lastMerchantReveal = null;
    // Remove discarded card from hand, defCards, or topDefCards
    const handIdx = p.hand.indexOf(discardedCardId);
    if (handIdx !== -1) {
      p.hand.splice(handIdx, 1);
    } else {
      for (const key of Object.keys(p.defCards || {})) {
        if (p.defCards[key] === discardedCardId) { delete p.defCards[key]; break; }
      }
      for (const key of Object.keys(p.topDefCards || {})) {
        if (p.topDefCards[key] === discardedCardId) { delete p.topDefCards[key]; break; }
      }
    }
    this.cemetery.push(discardedCardId);
    // Remove drawn card from deck and give it to the player
    const deckIdx = this.deck.indexOf(drawnCardId);
    if (deckIdx !== -1) this.deck.splice(deckIdx, 1);
    p.hand.push(drawnCardId);
    p.merchantTrades--;
    this.pushLog(`${this.pname(playerIdx)} used Merchant trade`, true, true);
  }

  merchantSecondTry(playerIdx, firstCardId, secondCardId, isJoker) {
    const p = this.players[playerIdx];
    // Move 1st drawn card from hand to cemetery (visible to all)
    const firstIdx = p.hand.indexOf(firstCardId);
    if (firstIdx !== -1) p.hand.splice(firstIdx, 1);
    this.cemetery.push(firstCardId);
    // Remove 2nd drawn card from deck
    const deckIdx = this.deck.indexOf(secondCardId);
    if (deckIdx !== -1) this.deck.splice(deckIdx, 1);
    if (isJoker) {
      this.cemetery.push(secondCardId);
    } else {
      p.hand.push(secondCardId);
    }
    // Reveal 2nd drawn card to all clients
    this.lastMerchantReveal = { playerIdx, cardId: secondCardId };
    this.pushLog(`${this.pname(playerIdx)} used Merchant 2nd try`, true, true);
  }

  heroAcquired(playerIdx, heroName) {
    if (!heroName) return;
    if (playerIdx < 0 || playerIdx >= this.players.length) return;

    // Keep hero ownership unique across all players.
    for (const p of this.players) {
      p.heroes = (p.heroes || []).filter(h => h !== heroName);
    }

    const target = this.players[playerIdx];
    if (!target.heroes) target.heroes = [];
    target.heroes.push(heroName);
  }

  heroLost(playerIdx, heroName) {
    if (!heroName) return;

    if (playerIdx >= 0 && playerIdx < this.players.length) {
      const p = this.players[playerIdx];
      p.heroes = (p.heroes || []).filter(h => h !== heroName);
      return;
    }

    // Fallback: remove from everyone if the sender index is invalid.
    for (const p of this.players) {
      p.heroes = (p.heroes || []).filter(h => h !== heroName);
    }
  }

  serialize() {
    return {
      currentPlayerIndex: this.currentPlayerIndex,
      setupPhase: this.setupPhase || false,
      setupSubmitted: Object.assign({}, this.setupSubmitted || {}),
      deck: [...this.deck],
      cemetery: [...this.cemetery],
      players: this.players.map(p => ({
        index: p.index,
        name: p.name,
        hand: [...p.hand],
        defCards: Object.assign({}, p.defCards),
        defCardsCovered: Object.assign({}, p.defCardsCovered || {}),
        topDefCards: Object.assign({}, p.topDefCards),
        topDefCardsCovered: Object.assign({}, p.topDefCardsCovered || {}),
        kingCard: p.kingCard,
        kingCovered: p.kingCovered !== undefined ? p.kingCovered : true,
        isOut: p.isOut || false,
        sabotaged: Object.assign({}, p.sabotaged || {}),
        heroes: [...(p.heroes || [])],
        preyCards: [...(p.preyCards || [])],
        attackCount: p.attackCount || 0,
        priestConversionAttempts: p.priestConversionAttempts !== undefined ? p.priestConversionAttempts : 2,
        magicianSpells: p.magicianSpells !== undefined ? p.magicianSpells : 1,
        merchantTrades: p.merchantTrades !== undefined ? p.merchantTrades : 1,
        warlordAttacks: p.warlordAttacks !== undefined ? p.warlordAttacks : 1,
        spyAttacks: p.spyAttacks !== undefined ? p.spyAttacks : 1,
        spyMaxAttacks: p.spyMaxAttacks !== undefined ? p.spyMaxAttacks : 1,
        spyExtends: p.spyExtends !== undefined ? p.spyExtends : 1,
        pickingDeckAttacks: p.pickingDeckAttacks !== undefined ? p.pickingDeckAttacks : 1,
        attackingSymbol: p.attackingSymbol || 'none',
        attackingSymbol2: p.attackingSymbol2 || 'none',
      })),
      pickingDecks: this.pickingDecks.map(d => d.map(c => ({ id: c.id, covered: c.covered }))),
      winnerIndex: this.checkWinner(),
      log: [...this.log],
      merchantReveal: this.lastMerchantReveal || null,
      pendingAttack: this.pendingAttack || null,
      pendingPlunder: this.pendingPlunder || null,
      pendingHeroSelection: this.pendingHeroSelection || null,
    };
  }
}

module.exports = GameState;
