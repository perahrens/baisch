'use strict';
/**
 * Fast synchronous game simulator for MCTS.
 * No I/O, no hero abilities, no socket events.
 * Used by the MCTS bot personality to evaluate moves.
 */

function simCardStrength(cardId) {
  if (!cardId || cardId > 52) return 999;
  var idx = (cardId - 1) % 13 + 1;
  return idx === 1 ? 14 : idx;
}

// Strength of a card used as a DEFENCE card.
// Jokers (id > 52) have defence strength 1; all other cards use their normal strength.
function simDefCardStrength(cardId) {
  if (!cardId || cardId > 52) return 1;
  var idx = (cardId - 1) % 13 + 1;
  return idx === 1 ? 14 : idx;
}

function simCardSuit(cardId) {
  if (cardId > 52) return 'joker';
  var si = Math.floor((cardId - 1) / 13);
  return ['clubs', 'diamonds', 'hearts', 'spades'][si];
}

function simGroupBySuit(hand) {
  var groups = {};
  for (var i = 0; i < hand.length; i++) {
    var id = hand[i];
    if (id > 52) continue;
    var suit = simCardSuit(id);
    if (!groups[suit]) groups[suit] = [];
    groups[suit].push(id);
  }
  return groups;
}

/**
 * Find the minimal subset of `cards` whose combined strength is STRICTLY GREATER
 * than `threshold`.  Returns an array of card IDs, or null if impossible.
 */
function simMinimalWinning(cards, threshold) {
  if (!cards || cards.length === 0) return null;
  var sorted = cards.slice().sort(function(a, b) {
    return simCardStrength(a) - simCardStrength(b);
  });
  var total = 0;
  for (var i = 0; i < sorted.length; i++) total += simCardStrength(sorted[i]);
  if (total <= threshold) return null; // even all cards can't beat threshold
  // Greedy: take strongest cards first until sum > threshold
  var chosen = [];
  var sum = 0;
  for (var j = sorted.length - 1; j >= 0 && sum <= threshold; j--) {
    chosen.push(sorted[j]);
    sum += simCardStrength(sorted[j]);
  }
  return chosen;
}

/**
 * Deep-clone the game state for simulation.
 * Copies only the fields the simulator needs.
 */
function simClone(gs) {
  return {
    players: gs.players.map(function(p) {
      return {
        index: p.index,
        hand: p.hand.slice(),
        defCards: Object.assign({}, p.defCards),
        topDefCards: Object.assign({}, p.topDefCards || {}),
        defCardsCovered: Object.assign({}, p.defCardsCovered || {}),
        topDefCardsCovered: Object.assign({}, p.topDefCardsCovered || {}),
        defCardsBoost: Object.assign({}, p.defCardsBoost || {}),
        topDefCardsBoost: Object.assign({}, p.topDefCardsBoost || {}),
        kingCard: p.kingCard,
        kingCardBoost: p.kingCardBoost || 0,
        isOut: p.isOut,
        pickingDeckAttacks: p.pickingDeckAttacks != null ? p.pickingDeckAttacks : 1,
      };
    }),
    pickingDecks: gs.pickingDecks.map(function(deck) {
      return deck.map(function(c) { return { id: c.id, covered: c.covered }; });
    }),
    deck: gs.deck.slice(),
    cemetery: gs.cemetery.slice(),
    currentPlayerIndex: gs.currentPlayerIndex,
  };
}

/**
 * Returns all legal actions for the current player.
 * allowCovered: if true, also consider attacks on covered defense cards (used in rollouts).
 */
function simLegalActions(sim, allowCovered) {
  var idx = sim.currentPlayerIndex;
  var p = sim.players[idx];
  if (!p || p.isOut) return [{ type: 'pass' }];

  var actions = [];
  var groups = simGroupBySuit(p.hand);
  var jokers = p.hand.filter(function(id) { return id > 52; });
  var suits = Object.keys(groups);

  // ---- Loot ----
  if ((p.pickingDeckAttacks || 0) > 0) {
    for (var di = 0; di < sim.pickingDecks.length; di++) {
      var deck = sim.pickingDecks[di];
      if (deck.length === 0) continue;
      var topCard = deck[deck.length - 1];
      var threshold = simCardStrength(topCard.id);
      for (var si = 0; si < suits.length; si++) {
        var combo = simMinimalWinning(groups[suits[si]], threshold);
        if (combo) actions.push({ type: 'loot', deckIndex: di, cardIds: combo, symbol: suits[si] });
      }
      if (jokers.length > 0) {
        actions.push({ type: 'loot', deckIndex: di, cardIds: [jokers[0]], symbol: 'joker' });
      }
    }
  }

  // ---- Defense card attacks ----
  for (var dpi = 0; dpi < sim.players.length; dpi++) {
    if (dpi === idx) continue;
    var defender = sim.players[dpi];
    if (defender.isOut) continue;
    for (var slot = 1; slot <= 3; slot++) {
      var defCardId = defender.defCards[slot];
      if (defCardId == null) continue;
      var isFaceUp = defender.defCardsCovered[slot] === false;
      if (!isFaceUp && !allowCovered) continue;
      var defBoost = defender.defCardsBoost[slot] || 0;
      var topCardId = defender.topDefCards[slot];
      var topBoost = topCardId ? (defender.topDefCardsBoost[slot] || 0) : 0;
      var defThreshold = simDefCardStrength(defCardId) + defBoost
        + (topCardId ? simDefCardStrength(topCardId) + topBoost : 0);
      for (var ssi = 0; ssi < suits.length; ssi++) {
        var defCombo = simMinimalWinning(groups[suits[ssi]], defThreshold);
        if (defCombo) actions.push({ type: 'defAttack', defenderIdx: dpi, positionId: slot, cardIds: defCombo, symbol: suits[ssi] });
      }
    }
  }

  // ---- King attacks (all defense slots empty for target) ----
  for (var kpi = 0; kpi < sim.players.length; kpi++) {
    if (kpi === idx) continue;
    var kDefender = sim.players[kpi];
    if (kDefender.isOut) continue;
    if (kDefender.kingCard == null) continue;
    var allEmpty = true;
    for (var ks = 1; ks <= 3; ks++) {
      if (kDefender.defCards[ks] != null || kDefender.topDefCards[ks] != null) { allEmpty = false; break; }
    }
    if (!allEmpty) continue;
    var kingStr = simCardStrength(kDefender.kingCard) + (kDefender.kingCardBoost || 0);
    for (var ksi = 0; ksi < suits.length; ksi++) {
      var kingCombo = simMinimalWinning(groups[suits[ksi]], kingStr);
      if (kingCombo) actions.push({ type: 'kingAttack', defenderIdx: kpi, cardIds: kingCombo, symbol: suits[ksi] });
    }
    if (jokers.length > 0) {
      actions.push({ type: 'kingAttack', defenderIdx: kpi, cardIds: [jokers[0]], symbol: 'joker' });
    }
  }

  actions.push({ type: 'pass' });
  return actions;
}

function simPickCard(sim) {
  if (sim.deck.length === 0) {
    if (sim.cemetery.length === 0) return null;
    sim.deck = sim.cemetery.slice();
    sim.cemetery = [];
    // Shuffle
    for (var i = sim.deck.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var tmp = sim.deck[i]; sim.deck[i] = sim.deck[j]; sim.deck[j] = tmp;
    }
  }
  return sim.deck.pop();
}

function simFinishTurn(sim) {
  var idx = sim.currentPlayerIndex;
  if (sim.players[idx]) sim.players[idx].pickingDeckAttacks = 1;
  var n = sim.players.length;
  var next = (idx + 1) % n;
  var safety = 0;
  while (sim.players[next].isOut && next !== idx && ++safety < n) {
    next = (next + 1) % n;
  }
  sim.currentPlayerIndex = next;
}

function simApplyAction(sim, action) {
  var idx = sim.currentPlayerIndex;
  var p = sim.players[idx];
  var i, cardId;

  if (action.type === 'loot') {
    p.pickingDeckAttacks = 0;
    for (i = 0; i < action.cardIds.length; i++) {
      cardId = action.cardIds[i];
      var hi = p.hand.indexOf(cardId);
      if (hi !== -1) p.hand.splice(hi, 1);
      sim.cemetery.push(cardId);
    }
    var deck = sim.pickingDecks[action.deckIndex];
    var topCard = deck[deck.length - 1];
    var comboSum = action.symbol === 'joker' ? 999
      : action.cardIds.reduce(function(s, id) { return s + simCardStrength(id); }, 0);
    if (comboSum > simCardStrength(topCard.id)) {
      // Success: take all picking deck cards
      for (i = 0; i < deck.length; i++) p.hand.push(deck[i].id);
      sim.pickingDecks[action.deckIndex] = [];
      var other = 1 - action.deckIndex;
      var c1 = simPickCard(sim); if (c1) sim.pickingDecks[other].push({ id: c1, covered: true });
      var c2 = simPickCard(sim); if (c2) sim.pickingDecks[action.deckIndex].push({ id: c2, covered: false });
      var c3 = simPickCard(sim); if (c3) sim.pickingDecks[action.deckIndex].push({ id: c3, covered: true });
    } else {
      // Failed: deck gets another card
      var c4 = simPickCard(sim); if (c4) deck.push({ id: c4, covered: true });
    }
    simFinishTurn(sim);

  } else if (action.type === 'defAttack') {
    for (i = 0; i < action.cardIds.length; i++) {
      cardId = action.cardIds[i];
      var dhi = p.hand.indexOf(cardId);
      if (dhi !== -1) p.hand.splice(dhi, 1);
      sim.cemetery.push(cardId);
    }
    var defender = sim.players[action.defenderIdx];
    // Reveal the card
    defender.defCardsCovered[action.positionId] = false;
    // Compute whether attack wins
    var defCardId = defender.defCards[action.positionId];
    var defBoost = defender.defCardsBoost[action.positionId] || 0;
    var topCardId = defender.topDefCards[action.positionId];
    var topBoost = topCardId ? (defender.topDefCardsBoost[action.positionId] || 0) : 0;
    var threshold = simDefCardStrength(defCardId) + defBoost
      + (topCardId ? simDefCardStrength(topCardId) + topBoost : 0);
    var atkSum = action.cardIds.reduce(function(s, id) { return s + simCardStrength(id); }, 0);
    if (atkSum > threshold) {
      // Attacker takes defense card(s)
      if (defCardId != null) { p.hand.push(defCardId); delete defender.defCards[action.positionId]; }
      if (topCardId != null) { p.hand.push(topCardId); delete defender.topDefCards[action.positionId]; }
      delete defender.defCardsCovered[action.positionId];
      if (defender.topDefCardsCovered) delete defender.topDefCardsCovered[action.positionId];
    }
    simFinishTurn(sim);

  } else if (action.type === 'kingAttack') {
    for (i = 0; i < action.cardIds.length; i++) {
      cardId = action.cardIds[i];
      var khi = p.hand.indexOf(cardId);
      if (khi !== -1) p.hand.splice(khi, 1);
      sim.cemetery.push(cardId);
    }
    var kDefender = sim.players[action.defenderIdx];
    // King attack: attacker takes all defender's cards; defender is out
    for (i = 0; i < kDefender.hand.length; i++) p.hand.push(kDefender.hand[i]);
    kDefender.hand = [];
    if (kDefender.kingCard != null) { p.hand.push(kDefender.kingCard); kDefender.kingCard = null; }
    kDefender.isOut = true;
    simFinishTurn(sim);

  } else {
    // pass: expose first covered defense card to enable future attacks
    var exposed = false;
    for (var slot = 1; slot <= 3 && !exposed; slot++) {
      if (p.defCards[slot] != null && p.defCardsCovered[slot] !== false) {
        p.defCardsCovered[slot] = false;
        exposed = true;
      }
    }
    simFinishTurn(sim);
  }
}

function simWinner(sim) {
  var alive = sim.players.filter(function(p) { return !p.isOut; });
  return alive.length === 1 ? alive[0].index : -1;
}

/**
 * Run a random rollout from `sim` up to maxTurns, returning winning player index or -1.
 * allowCovered=true: permits attacking covered defense cards to make rollouts richer.
 */
function simRandomPlayout(sim, maxTurns) {
  maxTurns = maxTurns || 80;
  for (var t = 0; t < maxTurns; t++) {
    var winner = simWinner(sim);
    if (winner !== -1) return winner;
    var actions = simLegalActions(sim, true); // allow covered in rollouts
    var active = actions.filter(function(a) { return a.type !== 'pass'; });
    var chosen = active.length > 0
      ? active[Math.floor(Math.random() * active.length)]
      : actions[0];
    simApplyAction(sim, chosen);
  }
  // Timeout heuristic: prefer the player with fewest remaining defense cards
  var alive = sim.players.filter(function(p) { return !p.isOut; });
  if (alive.length === 0) return -1;
  var best = alive[0];
  var bestShields = 99;
  for (var pi = 0; pi < alive.length; pi++) {
    var shields = 0;
    for (var s = 1; s <= 3; s++) {
      if (alive[pi].defCards[s] != null || alive[pi].topDefCards[s] != null) shields++;
    }
    if (shields < bestShields) { bestShields = shields; best = alive[pi]; }
  }
  return best.index;
}

var MCTS_C = Math.sqrt(2);

/**
 * Run MCTS from the perspective of `playerIdx` to select the best first action.
 * Returns the chosen action object, or null if no active actions are available.
 */
function mctsSearch(gs, playerIdx, iterations) {
  iterations = iterations || 300;

  var sim0 = simClone(gs);
  sim0.currentPlayerIndex = playerIdx;
  var rootActions = simLegalActions(sim0, false);

  // Only offer active (non-pass) actions at the root; fall back to pass if nothing else.
  var searchActions = rootActions.filter(function(a) { return a.type !== 'pass'; });
  if (searchActions.length === 0) return null; // signal caller to pass
  if (searchActions.length === 1) return searchActions[0];

  var wins = new Array(searchActions.length).fill(0);
  var visits = new Array(searchActions.length).fill(0);
  var totalVisits = 0;

  for (var iter = 0; iter < iterations; iter++) {
    // UCB1 selection
    var chosen = 0;
    var bestScore = -Infinity;
    for (var i = 0; i < searchActions.length; i++) {
      var score = visits[i] === 0
        ? Infinity
        : wins[i] / visits[i] + MCTS_C * Math.sqrt(Math.log(totalVisits + 1) / visits[i]);
      if (score > bestScore) { bestScore = score; chosen = i; }
    }
    // Simulate
    var sim = simClone(gs);
    sim.currentPlayerIndex = playerIdx;
    simApplyAction(sim, searchActions[chosen]);
    var rolloutWinner = simRandomPlayout(sim, 80);
    // Backpropagate
    visits[chosen]++;
    totalVisits++;
    if (rolloutWinner === playerIdx) wins[chosen]++;
  }

  // Robust child selection: most visited
  var bestIdx = 0;
  for (var j = 1; j < searchActions.length; j++) {
    if (visits[j] > visits[bestIdx]) bestIdx = j;
  }
  return searchActions[bestIdx];
}

module.exports = { mctsSearch, simClone, simLegalActions, simWinner };
