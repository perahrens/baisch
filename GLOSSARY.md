# Baisch – Game Glossary

This glossary defines the key terms used throughout the Baisch codebase, issues, and conversations about the game.

---

## Players & Roles

| Term | Definition |
|---|---|
| **Player** | A participant in a game session. 2–4 players compete against each other. |
| **Spectator** | A connected user who watches an ongoing game without participating. |
| **Current Player** | The player whose turn it is right now. |
| **Eliminated / Out** | A player whose king card has been captured. They are removed from the turn order and can no longer act. |
| **Winner** | The last player still alive (not eliminated). The game ends and returns to the lobby after 5 seconds. |

---

## Cards

| Term | Definition |
|---|---|
| **Card** | A standard playing card identified by suit (symbol) and value (strength). Card IDs 1–52 map to the standard 52-card deck; IDs 53–55 are jokers. |
| **Strength** | The numeric value of a card used in attack and defence comparisons. Ace = 14, King = 13, … , 2 = 2. Joker cards have strength 1 when used in defence and unlimited strength when played from hand. |
| **Symbol / Suit** | The suit of a card: `hearts`, `diamonds`, `spades`, or `clubs`. Joker cards use the symbol `joker`. |
| **Covered / Face-down** | A card that is hidden from opponents (its value is not visible). Newly placed defence cards and the king are always placed face-down. |
| **Face-up / Revealed** | A card whose value is visible to all players (e.g. after a failed attack or merchant reveal). |
| **Hand Card** | A card held in a player's hand. Only the owning player can see their own hand cards. |
| **King Card** | A special card assigned to each player at setup. It represents the player's "life". If an enemy successfully attacks the king, the owning player is eliminated. |
| **Defence Card (Shield)** | One of up to three cards placed face-down in a player's defence row (slots 1, 2, 3). Attackers must beat the shield's strength to capture it. Also called a *shield*. |
| **Top Defence Card** | A second card stacked on top of an existing defence card (enabled by the Fortified Tower hero). The combined strength of both cards must be beaten. |
| **Prey Card** | A defence card or king card captured by the current player during their turn. Prey cards are locked (greyed-out) and cannot be used for attacks until the turn ends. |
| **Joker Card** | A special card (IDs 53–55) that can be sacrificed to obtain a hero. When used as a defence card it has strength 1. |

---

## Decks

| Term | Definition |
|---|---|
| **Card Deck** | The main draw pile. Cards are drawn from here during setup and gameplay. When empty it is automatically reshuffled from the Cemetery. |
| **Cemetery Deck (Cemetery)** | The discard pile. Cards that are spent in attacks, discarded by heroes, or used in sacrifices go here. |
| **Picking Deck** | One of two face-up/face-down stacks placed in the middle of the table. Players can *plunder* a picking deck to gain all its cards. There are always two picking decks (index 0 and 1). |

---

## Turn & Actions

| Term | Definition |
|---|---|
| **Turn** | One player's action phase. A turn ends when the player clicks **Finish Turn**. |
| **Round** | One full cycle of turns (every player has had their turn once). Turn order within a round is determined by dice rolls. |
| **Finish Turn** | The button/action that ends the current player's turn and advances to the next player. |
| **Mobilise Action** | Taking or placing a defence card. Each turn the player has 1 *take* action and 1 *put* action by default (3 total with the Marshal hero). |
| **Take Defence Card** | A mobilise action that moves one of the player's own face-down defence cards (and its top card if present) back into their hand. |
| **Put Defence Card** | A mobilise action that places a selected hand card face-down into one of the player's three defence slots. |
| **Attack (Defence Card Attack)** | Selecting hand cards (and optionally own defence cards via Banneret) whose combined strength exceeds an enemy's defence card strength. On success the attacker gains the card(s); on failure the attacked card remains face-up. |
| **King Attack (Royal Attack)** | Using the player's king card to attack an enemy defence card. The king's strength is compared directly against the defence card. Result: win (2), draw (1), or loss (0). |
| **King Assault** | Using hand cards (or the king card) to attack an enemy's *king card* directly. Success eliminates the defender; failure with king used eliminates the attacker. |
| **Plunder** | Attacking a picking deck. If the attack sum beats the top card's strength the attacker gains all cards in that picking deck; otherwise the deck grows by one new card. |
| **Joker Sacrifice** | Discarding a joker card to draw one card from the top of the deck, which determines the type of hero awarded. |
| **Coup (King Swap)** | Replacing the player's current king card with a hand card (without using the Warlord hero). The old king goes to hand and can immediately be used for an attack. |
| **Hero Selection** | When a joker sacrifice results in an ace (or joker determines a free choice), the player selects their hero from a list of available options. |

---

## Game Screens & UI

| Term | Definition |
|---|---|
| **Game Lobby** | The waiting room where players join, set their name, mark themselves as ready, and optionally pre-select a starting hero before the game begins. |
| **Game Screen** | The main in-game view showing both players' fields, hand cards, picking decks, heroes, dice, and action buttons. |
| **Overlay** | A temporary UI layer shown during attacks or special hero actions (e.g. attack confirmation, plunder confirmation, Priest card selection, hero choice). |
| **Activity Log** | A small scrollable area in the game screen that shows the last 5 game events (attacks, plunders, mobilisations, etc.) with colour-coded success/failure indicators. |
| **Hand Area** | The bottom portion of the game screen displaying the current player's hand cards. |
| **Field / Board** | The area showing a player's defence slots, king card, and heroes. |
| **Defence Slot** | One of three positions (1, 2, 3) on a player's field where a defence card can be placed. |
| **Sabotaged Indicator** | A visual marker on a defence slot showing that a Saboteurs figure is present. While sabotaged, the slot cannot be taken or placed into (only sacrificed). |

---

## Heroes

Heroes are acquired by sacrificing a joker card. The drawn card (or player choice for aces/jokers) determines which hero is granted.

| Hero | Card | Type | Ability Summary |
|---|---|---|---|
| **Mercenaries** | 2 | White | Up to 8 mercenary figures. Each click before an attack adds +1 to attack strength. Recovers 4 per turn. |
| **Marshal** | 3 | White | Replaces the default take/put mobilise actions with 3 combined mobilise actions per turn. |
| **Spy** | 4 | White | Allows 1 attack per turn that reveals (flips face-up) an enemy defence card. Can extend for +2 extra reveal actions by sacrificing a card. |
| **Battery Tower** | 5 | White | 1 charge per turn. When the owner's defence card or king is attacked, the owner can spend 1 charge to deny the attack: attacker's used hand cards are locked for the rest of their turn. |
| **Merchant** | 6 | White | 1 trade per turn: discard a hand card (or defence card) and draw a replacement from the deck. On the last trade the drawn card is revealed to all players. |
| **Priest** | 7 | White | 2 conversion attempts per turn. After committing an attack with the same suit, convert one of the target player's matching hand cards to your own hand. |
| **Reservists** | 8 | Black | Up to 4 reservist figures (starts 2 ready). Each figure passively adds +1 to king card defence strength. During an attack/plunder overlay, spend figures to add +1 to attack. Recovers 2 per turn. |
| **Banneret** | 9 | Black | Allows two attacking suits of the same colour (hearts + diamonds, or spades + clubs) in a single attack. Also allows own face-down defence cards to be used as additional attack cards. |
| **Saboteurs** | 10 | Black | 2 saboteur figures. Deploy one onto an enemy defence slot to block that slot (cannot be taken or placed into). The defender can sacrifice the slot's card (or a hand card for an empty slot) to remove the saboteur. Saboteurs recover over 2 turns after being destroyed. |
| **Fortified Tower** | J | Black | 1 fortify action per turn: stack an additional hand card face-down on top of an existing defence card (a *top defence card*). The combined strength of both cards must be beaten by an attacker. |
| **Magician** | Q | Black | 1 spell per turn: replace the bottom (and optionally top) defence card of any player's slot with fresh cards drawn from the deck. The old cards are discarded. |
| **Warlord** | K | Black | 1 attack charge per turn. Select Warlord + a hand card to perform a direct king-versus-defence-card attack without needing to clear own defence cards first. Also enables *king swap*: swap king with a hand card. |

### Hero Acquisition
- **White heroes** (2–7): obtained by drawing a **red ace** (free choice from white heroes) or a card numbered 2–7.
- **Black heroes** (8–K): obtained by drawing a **black ace** (free choice from black heroes) or a card numbered 8–K.
- **Joker**: free choice of *any* available hero.

---

## Networking & Lobby Terms

| Term | Definition |
|---|---|
| **Socket** | The WebSocket connection between a client and the server, used to synchronise all game events in real time. |
| **Socket ID** | A unique identifier assigned by the server to each connected client. |
| **`getUsers` event** | Server broadcast listing all currently connected users and their ready status. |
| **`isReady`** | A flag on a user object indicating the player has clicked the ready button in the lobby. The game starts when all players are ready. |
| **`stateUpdate` event** | A server broadcast sent after every game action, carrying the full serialised game state so all clients stay in sync. |
| **`returnToLobby` event** | Sent by the server 5 seconds after a winner is found; all clients return to the lobby screen. |
| **Hero Selection (lobby)** | Before the game starts, players can reserve a preferred starting hero in the lobby. This is tracked server-side via `heroSelections`. |
| **`heroReleased` event** | Broadcast when a player disconnects from the lobby, freeing their reserved hero for others. |

---

## Miscellaneous

| Term | Definition |
|---|---|
| **Dice Roll** | At the start of each round each player rolls a virtual die; the highest roller goes first. |
| **Heroes Square** | The shared pool of available hero tokens. When a hero is acquired it is consumed from this pool and cannot be chosen by another player. |
| **Relict** | A legacy placeholder class for special game objects (currently not actively used in gameplay). |
| **`isOut`** | Internal flag on a player marking them as eliminated. Eliminated players are skipped in turn order. |
| **`preyCards`** | Cards captured by the current player during their turn. They are locked until the turn ends, after which they become usable hand cards. |
| **`pendingAttack` / `pendingPlunder`** | Server-side state stored while waiting for the client to confirm an attack or plunder result. Cleared once the action is resolved. |
