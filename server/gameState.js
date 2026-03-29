// Centralized game state for the server


class GameState {
  constructor(users) {
    this.deck = this.generateCards();
    this.players = users.map((user, idx) => ({
      id: user.id,
      index: idx,
      hand: [],
      // Add more player properties as needed
    }));
    this.board = [];
    this.dealCards(8); // Example: 8 cards per player
  }

  generateCards() {
    // Example: generate a shuffled deck
    let cards = [];
    for (let i = 1; i <= 52; i++) cards.push(i);
    for (let i = cards.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [cards[i], cards[j]] = [cards[j], cards[i]];
    }
    return cards;
  }

  dealCards(numCards) {
    for (let p of this.players) {
      for (let i = 0; i < numCards; i++) {
        if (this.deck.length > 0) {
          p.hand.push(this.deck.pop());
        }
      }
    }
  }
}

module.exports = GameState;
