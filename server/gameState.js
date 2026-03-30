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
    this.dealCards(8);
    this.doSetup();
    this.initPickingDecks();
  }

  generateCards() {
    let cards = [];
    for (let i = 1; i <= 52; i++) cards.push(i);
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
      for (let j = 1; j <= 3; j++) p.defCards[j] = p.hand.pop();
      for (let j = 0; j < 2; j++) this.cemetery.push(p.hand.pop());
    }
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

  putDefCard(playerIdx, positionId, cardId) {
    const p = this.players[playerIdx];
    const i = p.hand.indexOf(cardId);
    if (i !== -1) p.hand.splice(i, 1);
    p.defCards[positionId] = cardId;
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

  plunderResolved(attackerIdx, deckIdx, success, attackCardIds) {
    const attacker = this.players[attackerIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    if (success) {
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
      // Keep the attacked (top) card face-up after a failed plunder,
      // then add a new face-down card on top.
      const deck = this.pickingDecks[deckIdx];
      if (deck.length > 0) deck[deck.length - 1].covered = false;
      if (this.deck.length > 0) this.pickingDecks[deckIdx].push({ id: this.deck.pop(), covered: true });
    }
  }

  defAttackResolved(attackerIdx, defenderIdx, positionId, level, success, attackCardIds) {
    const attacker = this.players[attackerIdx];
    const defender = this.players[defenderIdx];
    for (const cardId of attackCardIds) {
      const i = attacker.hand.indexOf(cardId);
      if (i !== -1) attacker.hand.splice(i, 1);
      this.cemetery.push(cardId);
    }
    if (success) {
      if (level === 0) {
        const defCardId = defender.defCards[positionId];
        if (defCardId !== undefined) { attacker.hand.push(defCardId); delete defender.defCards[positionId]; }
        const topCardId = defender.topDefCards[positionId];
        if (topCardId !== undefined) { attacker.hand.push(topCardId); delete defender.topDefCards[positionId]; }
      } else {
        const topCardId = defender.topDefCards[positionId];
        if (topCardId !== undefined) { attacker.hand.push(topCardId); delete defender.topDefCards[positionId]; }
      }
    }
  }

  finishTurn(nextPlayerIndex) {
    this.currentPlayerIndex = nextPlayerIndex;
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
        topDefCards: Object.assign({}, p.topDefCards),
        kingCard: p.kingCard,
      })),
      pickingDecks: this.pickingDecks.map(d => d.map(c => ({ id: c.id, covered: c.covered }))),
    };
  }
}

module.exports = GameState;
