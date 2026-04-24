---
description: "Reference for Baisch game rules. Always consult this before implementing any game mechanic, turn logic, hero ability, or penalty check. Violations of these rules are bugs."
---
# Baisch – Game Rules Reference

This file is the authoritative ruleset for the Baisch card game. Any code change that touches turn flow, attack resolution, hero abilities, or penalty checks **must comply with these rules**.

---

## Overview

2–4 players compete to be the last player alive. A player is eliminated when their **king card** is captured. Players take turns in dice-roll order; the highest roller each round goes first.

---

## Turn Structure

Each turn a player may perform any combination of the following actions in any order:

1. **Mobilise** (take/put defence cards) — 1 take and 1 put per turn by default (3 combined with Marshal)
2. **Attack** enemy defence cards (with hand cards, own defence cards via Banneret, Reservists boost, or own king)
3. **King assault** on an exposed enemy king card
4. **Loot** a picking deck
5. **Use hero abilities** (Spy, Merchant, Priest, Warlord, etc.)

The turn ends when the player clicks **Finish Turn**.

---

## Expose-Defence-Card Penalty

> **Rule**: If a player ends their turn without having performed at least one **attack**, they must expose (flip face-up) one of their own covered defence cards. If all defence slots are empty they must expose their king card instead.

### What counts as an "attack" for this rule:
| Action | Counts? |
|---|---|
| Attacking an enemy defence card (with hand cards) | ✅ Yes |
| Attacking an enemy defence card (king used as attacker) | ✅ Yes |
| Attacking an enemy defence card (Banneret own-def-cards) | ✅ Yes |
| Attacking an enemy king card (king assault) | ✅ Yes |
| Warlord direct attack | ✅ Yes |
| **Looting a picking deck** | ❌ **No** |
| Taking or placing a defence card (mobilise) | ❌ No |
| Using a hero ability (Spy, Merchant, Priest, etc.) | ❌ No |
| Warlord king swap | ❌ No |

### Implementation details:
- Server: `attackCount` per player is incremented only in `defAttackResolved`, `kingAttackResolved`, and `warlordDirectAttack`.
- `lootResolved` must **not** increment `attackCount`.
- Client: `playerTurn.attackCounter == 0` triggers the expose prompt in `FinishTurnButtonListener`.
- Client: `increaseAttackCounter()` **must** be called locally for **every** attack type (including regular defence-card attacks and king assaults) when the attack overlay is confirmed — before `setUpdateState(true)` fires the re-render. This prevents a race-condition where the user clicks "Finish Turn" before the server's `stateUpdate` arrives with the updated `attackCount`, which would falsely trigger the expose-card penalty.
- The sole exception is Warlord direct attacks: `increaseAttackCounter()` is called at commit time (in `EnemyDefCardListener`) rather than at overlay-confirm time — the pattern is therefore `if (!apt.isPendingAttackIsWarlord()) apt.increaseAttackCounter();` in the overlay click handler.
- Client `applyStateUpdate` **must** be race-safe: a stale `stateUpdate` from `setAttackPreview` (emitted before the user confirmed the attack) can arrive AFTER the local optimistic increment. The handler therefore takes `Math.max(serverAttackCount, localAttackCount)` for the player whose turn it currently is. After the turn ends (current player index moves on) the server's value is authoritative.

---

## Attack Rules

### Defence Card Attack
- The attacker selects one or more hand cards of the **same suit** (unless Banneret: same-colour pair allowed).
- The combined strength of selected cards must **exceed** (not equal) the defence card's strength (plus top defence card if present — Fortified Tower).
- The attacker's king card can be used instead of hand cards (but cannot be combined with hand cards).
- The attacking symbol is **locked** after the first attack of a turn; all subsequent attacks in the same turn must use the same suit (or same-colour pair if Banneret).
- On **success**: the attacker gains the captured defence card(s) as **prey cards**.
- On **failure** (attacker used the king): the attacker is eliminated.

### Prey Cards
- A prey card is a defence card (or king card) captured by the current player during their current turn.
- Prey cards are placed in the attacker's hand but are **locked** (greyed-out) for the rest of the turn — they cannot be used in attacks, placed as defence, or discarded until the turn ends.
- `finishTurn()` on the server clears `preyCards = []` so captured cards become normal hand cards next turn.
- **Client implementation**: When a successful defence-card attack is confirmed in the overlay click handler, the captured card IDs **must** be added to `atkPlayer.getPlayerTurn().getPreyCardIds()` locally — before `setUpdateState(true)` triggers the re-render. If this is omitted, the first re-render fires before the server's `stateUpdate` arrives with `preyCards`, so the card briefly appears as usable (race-condition bug). The server's `stateUpdate` will confirm the prey list on arrival.
- **Client implementation**: `applyStateUpdate` **must** take the UNION of server `preyCards` and local `preyCardIds` while it is still this player's turn. A stale `stateUpdate` from `setAttackPreview` may arrive after the local optimistic update and would otherwise clobber the locally-known captures. After the turn ends, server is authoritative.
- Cards gained from a **loot** are NOT prey cards — they become immediately usable (by design).

### King Assault
- The defender's king must be **exposed** (face-up) for a king assault to be possible.
- A king assault is only possible when all 3 of the defender's defence slots are empty.
- The attacker selects hand cards or their king whose combined strength beats the defending king's strength (plus any Reservists boost the defender has).
- On **success**: the defender is eliminated; the attacker gains all of the defender's cards.
- On **failure**: the defending king is exposed (flipped face-up) as a result of the attack. If the attacker used their king, the attacker is also eliminated.

### Loot
- The attacker selects hand cards of the same suit whose combined strength exceeds the **top card** of the target picking deck.
- On **success**: the attacker gains all cards in that picking deck; the deck is rebuilt.
- On **failure** (attacker used their king): the attacker is eliminated. Otherwise no penalty for failure.
- **Looting does not count as an attack** for the expose-defence-card penalty check.

---

## Mobilise Actions

- Default: 1 **take** action + 1 **put** action per turn.
- With Marshal hero: 3 combined take/put actions per turn.
- **Take**: move a face-down defence card (and its top card if Fortified Tower stacked one) from the field back to hand.
- **Put**: place a selected hand card face-down into an empty or occupied defence slot.
- Sabotaged slots cannot be taken from or placed into (must be sacrificed to remove the saboteur).

---

## Hero Rules Summary

### Mercenaries
- Up to 8 figures. Clicking the hero before committing an attack adds +1 attack strength per figure spent.
- Recovers 4 figures per turn.

### Marshal
- Replaces default 1 take + 1 put with **3 combined mobilise actions** per turn.

### Spy
- 1 spy action per turn: attack a face-up enemy defence card and flip it face-up (reveal) instead of capturing.
- Can extend for +2 actions per turn by sacrificing a hand card. Maximum 1 extend per sacrifice.

### Battery Tower
- 1 charge per turn (recharges at start of owner's next turn).
- When the owner's defence card or king is attacked, the owner may spend 1 charge to **cancel** the attack: the attacker's hand cards used are locked for the rest of their turn.

### Merchant
- 1 trade per turn: discard a hand card (or defence card), draw a replacement from the deck.
- On the **last** trade of the turn the drawn card is revealed to all players (Merchant second-try reveal).

### Priest
- 2 conversion attempts per turn.
- After committing an attack with a specific suit, the Priest can convert one matching hand card from the **target player** to the attacker's own hand.

### Reservists
- Up to 4 figures (starts with 2 ready). Each figure passively adds +1 to the owner's king card defence strength.
- During an attack/loot, the owner may spend figures to add +1 per figure to the **attack** strength.
- Recovers 2 figures per turn.

### Banneret
- Allows two suits of the **same colour** (hearts + diamonds, or clubs + spades) in a single attack.
- This applies from the **first attack** of the turn (before any symbol is locked).
- Also allows the owner's face-down defence cards to be used as additional attack cards.

### Saboteurs
- 2 figures. Deploy one onto an enemy defence slot to block it (cannot take/put while sabotaged).
- The defender can sacrifice the slot's defence card (or a hand card if the slot is empty) to remove the figure.
- Saboteurs recover over 2 turns after being destroyed.

### Fortified Tower
- 1 fortify action per turn: stack an additional hand card face-down on top of an existing defence card.
- Attackers must beat the **combined** strength of both cards.
- The top card is removed first when the slot is attacked successfully.

### Magician
- 1 spell per turn: replace the bottom (and optionally top) defence card of any player's slot with fresh cards drawn from the deck. Old cards are discarded.

### Warlord
- 1 direct attack charge per turn (separate from the king-swap charge).
- Direct attack: select Warlord + a hand card; the king attacks a target defence card directly, without needing to clear own defence slots first. **Counts as an attack** for the expose-penalty check.
- King swap: swap the player's king with a hand card (1 swap charge per turn, independent of the direct attack charge).
- Direct attack charges and king swap charges are **separate** counters; using one does not consume the other.

---

## Joker Rules

- IDs 53–55 are joker cards.
- **In defence**: a joker card has strength **1**.
- **In attack**: a joker card has **unlimited** strength (beats any defence/king card).
- **Sacrifice**: a player can sacrifice a joker from hand to draw a card that determines their hero. Drawing a red ace = free choice of white hero; black ace = free choice of black hero; joker = free choice of any hero; numbered card = specific hero.

---

## Elimination

A player is eliminated when:
1. Their king card is successfully attacked in a **king assault**, OR
2. They used their king card in an **attack that failed** (defence/loot/assault), OR
3. Their king card was targeted by a Warlord direct attack that succeeded.

An eliminated player's hand cards, king card, and heroes go to the attacker. If the defender had multiple heroes, the attacker chooses one; the rest are lost.

---

## Winning Condition

The game ends when only **one player** remains. That player is the winner.

---

## Notes for Coding Agents

- Never treat a loot as an attack for the expose-defence-card penalty.
- `attackCount` on the server and `attackCounter` on the client track **real attacks** only (defence/king attacks and Warlord direct attacks).
- The Banneret dual-symbol rule applies from the **first** attack of the turn; it does not require a prior symbol to have been locked.
- Warlord direct attack and Warlord king swap use **separate** server-side counters (`warlordAttacks` and `warlordSwaps`).
- The Marshal hero grants 3 **combined** take/put actions, not 3 takes + 3 puts.
