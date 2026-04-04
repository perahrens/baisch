// Server-authoritative game state

class GameState {
  constructor(users) {
    this.deck = this.generateCards();
    this.cemetery = [];
    this.players = users.map((user, idx) => ({
      id: user.id,
      index: idx,
      hand: [],
      defCards: {},
      topDefCards: {},
      kingCard: null,
    }));
    this.pickingDecks = [[], []]; // each entry: { id, covered }
    this.currentPlayerIndex = 0;
    this.log = []; // activity log: [{ text, success }, ...], max 5 entries
    this.dealCards(8);
    this.doSetup();
    this.initPickingDecks();
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
    for (const p of this.players) {
      p.kingCard = p.hand.pop();
      p.kingCovered = true;
      p.isOut = false;
      for (let j = 1; j <= 3; j++) p.defCards[j] = p.hand.pop();
      for (let j = 0; j < 2; j++) this.cemetery.push(p.hand.pop());
      p.defCardsCovered = { 1: true, 2: true, 3: true };
      p.topDefCardsCovered = {};
    }
  }

  pushLog(text, success, neutral = false) {
    this.log.push({ text, success, neutral });
    if (this.log.length > 5) this.log.shift();
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
    this.pushLog(`P${playerIdx} took shield [${positionId}] to hand`, true, true);
  }

  putDefCard(playerIdx, positionId, cardId) {
    const p = this.players[playerIdx];
    const i = p.hand.indexOf(cardId);
    if (i !== -1) p.hand.splice(i, 1);
    p.defCards[positionId] = cardId;
    if (!p.defCardsCovered) p.defCardsCovered = {};
    p.defCardsCovered[positionId] = true; // newly placed card is always face-down
    this.pushLog(`P${playerIdx} placed shield at [${positionId}]`, true, true);
  }

  addToCemetery(playerIdx, cardIds, drawFromDeck) {
    const p = this.players[playerIdx];
    for (const cardId of cardIds) {
      const i = p.hand.indexOf(cardId);
      if (i !== -1) p.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    for (let i = 0; i < (drawFromDeck || 0); i++) {
      if (this.deck.length > 0) this.cemetery.push(this.deck.pop());
    }
  }

  plunderResolved(attackerIdx, deckIdx, success, attackCardIds, kingUsed) {
    const attacker = this.players[attackerIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    if (kingUsed) attacker.kingCovered = false;
    if (success) {
      this.pushLog(`P${attackerIdx} plundered deck ${deckIdx + 1}!`, true);
      // Move all cards from plundered deck into attacker's hand
      for (const c of this.pickingDecks[deckIdx]) attacker.hand.push(c.id);
      this.pickingDecks[deckIdx] = [];
      const otherIdx = 1 - deckIdx;
      // Add one face-DOWN card to the other deck (deck B grows by one hidden card)
      if (this.deck.length > 0) this.pickingDecks[otherIdx].push({ id: this.deck.pop(), covered: true });
      // Rebuild plundered deck: one face-up card (visible) + one face-down card on top
      if (this.deck.length > 0) this.pickingDecks[deckIdx].push({ id: this.deck.pop(), covered: false });
      if (this.deck.length > 0) this.pickingDecks[deckIdx].push({ id: this.deck.pop(), covered: true });
    } else {
      this.pushLog(`P${attackerIdx} plunder on deck ${deckIdx + 1} failed`, false);
      if (kingUsed) attacker.isOut = true;
      // Keep the attacked (top) card face-up after a failed plunder,
      // then add a new face-down card on top.
      const deck = this.pickingDecks[deckIdx];
      if (deck.length > 0) deck[deck.length - 1].covered = false;
      if (this.deck.length > 0) this.pickingDecks[deckIdx].push({ id: this.deck.pop(), covered: true });
    }
  }

  defAttackResolved(attackerIdx, defenderIdx, positionId, level, success, attackCardIds, kingUsed) {
    const attacker = this.players[attackerIdx];
    const defender = this.players[defenderIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    if (kingUsed) attacker.kingCovered = false;
    if (success) {
      this.pushLog(`P${attackerIdx} broke P${defenderIdx}'s shield [${positionId}]`, true);
      if (level === 0) {
        const defCardId = defender.defCards[positionId];
        if (defCardId !== undefined) { attacker.hand.push(defCardId); delete defender.defCards[positionId]; }
        const topCardId = defender.topDefCards[positionId];
        if (topCardId !== undefined) { attacker.hand.push(topCardId); delete defender.topDefCards[positionId]; }
      } else {
        const topCardId = defender.topDefCards[positionId];
        if (topCardId !== undefined) { attacker.hand.push(topCardId); delete defender.topDefCards[positionId]; }
      }
    } else {
      this.pushLog(`P${attackerIdx} missed P${defenderIdx}'s shield [${positionId}]`, false);
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

  finishTurn(currentPlayerIndex) {
    // Advance to the next non-eliminated player (server is authoritative)
    const n = this.players.length;
    let next = (currentPlayerIndex + 1) % n;
    let safety = 0;
    while (this.players[next].isOut && next !== currentPlayerIndex && safety++ < n) {
      next = (next + 1) % n;
    }
    this.currentPlayerIndex = next;
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
    this.pushLog(`P${playerIdx} sacrificed a Joker for a Hero`, true, true);
  }

  kingAttackResolved(attackerIdx, defenderIdx, success, attackCardIds, kingUsed) {
    const attacker = this.players[attackerIdx];
    const defender = this.players[defenderIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    if (kingUsed) attacker.kingCovered = false;
    if (success) {
      this.pushLog(`P${attackerIdx} defeated P${defenderIdx}!`, true);
      // Defender loses their king and is eliminated; attacker gains their cards
      defender.isOut = true;
      for (const cardId of defender.hand) attacker.hand.push(cardId);
      defender.hand = [];
      if (defender.kingCard !== null) {
        attacker.hand.push(defender.kingCard);
        defender.kingCard = null;
      }
    } else {
      this.pushLog(`P${attackerIdx} king assault on P${defenderIdx} failed`, false);
      if (kingUsed) attacker.isOut = true;
    }
  }

  warlordKingSwap(playerIdx, oldKingCardId, newKingCardId) {
    const p = this.players[playerIdx];
    const handIdx = p.hand.indexOf(newKingCardId);
    if (handIdx === -1) return;
    p.hand.splice(handIdx, 1);
    p.hand.push(oldKingCardId);
    p.kingCard = newKingCardId;
    this.pushLog(`P${playerIdx} swapped king (Warlord)`, true, true);
  }

  serialize() {
    return {
      currentPlayerIndex: this.currentPlayerIndex,
      deck: [...this.deck],
      cemetery: [...this.cemetery],
      players: this.players.map(p => ({
        index: p.index,
        hand: [...p.hand],
        defCards: Object.assign({}, p.defCards),
        defCardsCovered: Object.assign({}, p.defCardsCovered || {}),
        topDefCards: Object.assign({}, p.topDefCards),
        topDefCardsCovered: Object.assign({}, p.topDefCardsCovered || {}),
        kingCard: p.kingCard,
        kingCovered: p.kingCovered !== undefined ? p.kingCovered : true,
        isOut: p.isOut || false,
      })),
      pickingDecks: this.pickingDecks.map(d => d.map(c => ({ id: c.id, covered: c.covered }))),
      winnerIndex: this.checkWinner(),
      log: [...this.log],
    };
  }
}

module.exports = GameState;
