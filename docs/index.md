# Baisch - Complete Rulebook

This rulebook is written to work for both:
- offline/tabletop card play
- the online implementation at https://perahrens.github.io/baisch/

The interface can differ, but the gameplay rules are the same.

## 1. Goal

Eliminate all opponents.

A player is eliminated when their king is defeated in a king assault.

## 2. Components

- Standard 52-card deck
- 3 Joker cards
- Hero pieces/tokens (or equivalent markers)
- Optional dice (for turn-order variant)

## 3. Setup

Each player starts with:
- 1 king card (face-down)
- up to 3 face-down defense cards in slots 1 to 3
- remaining cards in hand

Shared area:
- Main deck (draw pile)
- Cemetery (discard pile)
- 2 center picking decks (plunder targets)

## 4. Turn Order

Default rule:
- At the start of each round, players roll dice.
- Highest roll goes first; continue in descending order.

Optional rule:
- No dice roll.
- Use fixed clockwise order from a chosen starting player.

## 5. Card Values

- Number cards: face value
- J = 11, Q = 12, K = 13, A = 14
- Joker:
  - in defense counts as 1
  - in attack is a wild high-impact card

## 6. Turn Structure

On your turn, you may perform legal actions in any valid order, then finish turn.

Core actions:
- Manage defense (take/put)
- Plunder a center picking deck
- Attack enemy defense cards
- Assault enemy king (when legal)
- Use hero abilities

## 7. Core Rules

### 7.1 Manage Defense (Take/Put)

- Take: move one of your own defense cards back to hand.
- Put: place one hand card face-down into an open defense slot.

### 7.2 Plunder

Attack one of the 2 center picking decks.

- Success: if your attack value beats the top defense value, you take all cards in that picking deck.
- Failure: attack cards go to cemetery; the attacked picking deck grows.

### 7.3 Attack Enemy Defense

- Build attack value with legal attack cards.
- If attack value beats target defense value: capture target card(s).
- If not: target remains; hidden card is usually revealed.

### 7.4 King Assault

You may attack an enemy king only when king-assault conditions are met by your rule set (normally when enemy defense is cleared).

- Success: enemy is eliminated.
- Failure with your own king committed: you can be eliminated.

### 7.5 Prey Cards

Cards captured during your current turn are prey cards.

- Prey cards are locked for the rest of the turn.
- They become usable from your next turn onward.

### 7.6 No-Attack Reveal Rule

If your turn ends without performing an attack, reveal one of your defense cards.

## 8. Hero Acquisition

Heroes are obtained by sacrificing a Joker.

Hero mapping by drawn card:

| Drawn Card | Hero / Result |
|---|---|
| 2 | Mercenaries |
| 3 | Marshal |
| 4 | Spy |
| 5 | Battery Tower |
| 6 | Merchant |
| 7 | Priest |
| 8 | Reservists |
| 9 | Banneret |
| 10 | Saboteurs |
| J | Fortified Tower |
| Q | Magician |
| K | Warlord |
| Red Ace | Choose a white hero (2-7) |
| Black Ace | Choose a black hero (8-K) |
| Joker | Free choice of any hero |

## 9. Complete Hero Rules (How To Use)

### White Heroes

#### Mercenaries

![Mercenaries](images/mercenaries.png)

How to use:
1. Before resolving an attack/plunder, activate Mercenaries.
2. Spend ready units: each spent unit gives +1 attack value.
3. Resolve combat with boosted total.

Limits and details:
- Units are finite and recover over turns.
- In the online game, up to 8 total units exist, with ready/destroyed states.
- Use only what is needed to secure the result.

#### Marshal

![Marshal](images/general.png)

How to use:
1. During your turn, perform additional mobilization actions.
2. Mobilization means take or put of defense cards.

Limits and details:
- Marshal increases defense-management economy versus baseline rules.
- Online implementation uses a higher per-turn mobilization budget.

#### Spy

![Spy](images/spy.png)

How to use:
1. Activate Spy and choose a face-down enemy defense card.
2. Reveal it (information gain).
3. Optional extension: discard one hand card to gain extra spy reveals.

Complete Spy details:
- Base: 1 spy reveal attack per turn.
- Extension: you may discard one card to gain +2 additional spy reveals.
- This extension can be used once per turn (while extend is available).
- If no face-down enemy defense card is available (all are face-up), Spy may target and reveal the enemy king.

#### Battery Tower

![Battery Tower](images/batterytower.png)

How to use:
1. Opponent declares attack on your defense/king.
2. If Battery Tower is charged, choose to deny.
3. Attack is canceled/denied per rules, and attacker loses access to committed attack cards for that turn.

Limits and details:
- Usually one charge/use per turn cycle.
- Must recover/recharge before next deny.

#### Merchant

![Merchant](images/merchant.png)

How to use:
1. During your turn, choose one card to trade (from hand; in some variants also legal from defense interactions).
2. Discard chosen card to cemetery.
3. Draw replacement from main deck.

Limits and details:
- Usually 1 trade per turn.
- Online rules may reveal the final trade draw to all players.

#### Priest

![Priest](images/priest.png)

How to use:
1. First establish current attack suit by starting an attack.
2. Activate Priest and target an opponent hand card.
3. If revealed card matches your active attack suit, convert/keep it; otherwise return it.

Limits and details:
- Usually up to 2 conversion attempts per turn.
- Priest is only selectable after attack suit is set.

### Dark Heroes

#### Reservists

![Reservists](images/reservists.png)

How to use:
1. Keep reservists for passive king-defense boost.
2. Spend reservists during attack/plunder for +1 each when needed.

Limits and details:
- Spending improves offense but weakens defense buffer.
- Units recover over turns.

#### Banneret

![Banneret](images/lieutenant.png)

How to use:
1. Use Banneret to broaden legal attack composition.
2. In supported contexts, include own defense cards in attack builds.

Limits and details:
- Use carefully: converting defense into attack can open your board.
- Confirm suit-combination legality before committing cards.

#### Saboteurs

![Saboteurs](images/saboteurs.png)

How to use:
1. Place saboteur on enemy defense slot.
2. Block that slot from normal defense management.

Limits and details:
- Saboteurs are limited units with destruction/repair/recovery states.
- Defender can clear sabotage through legal removal/sacrifice/combat outcomes.

#### Fortified Tower

![Fortified Tower](images/fortifiedtower.png)

How to use:
1. Choose one of your defense slots.
2. Add a face-down top defense card.
3. Defender value becomes combined stack value.

Limits and details:
- Usually 1 stack action per turn.
- Attackers must beat combined strength to capture.

#### Magician

![Magician](images/mage.png)

How to use:
1. Target a defense slot (enemy or own, depending on format).
2. Replace card(s) with fresh draw(s) from deck.
3. Old card(s) go to cemetery.

Limits and details:
- Usually 1 spell per turn.
- Powerful for disrupting known strong defenses.

#### Warlord

![Warlord](images/warlord.png)

How to use:
1. Activate Warlord to unlock special king-based attack line.
2. Optionally perform king swap (variant/implementation-specific) for tactical setup.

Limits and details:
- King-based aggression is high risk.
- Failed king-committed attacks can cause self-elimination.

## 10. Optional Rules

- Fixed clockwise turn order (no dice each round).
- Faster mode: shorter timer / fewer planning pauses.

## 11. Win Condition

Last non-eliminated player wins.

## 12. Quick Beginner Guide

1. Keep at least one strong hidden defense.
2. Use plunder to stabilize weak hands.
3. Do not commit king assaults unless odds are clearly favorable.
4. Use heroes for timing advantage, not just raw value.
5. Track enemy resources (charges, units, per-turn uses).

## 13. Notes on Offline vs Online

- Offline and online use the same core mechanics.
- Online UI may automate checks and reveal states.
- If a table rule conflicts with this page, agree house rules before game start.
