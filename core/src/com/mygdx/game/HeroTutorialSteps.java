package com.mygdx.game;

/**
 * Issue #171: per-hero interactive tutorial step sequences.
 *
 * Design pattern: each instructional blocking overlay is followed by a
 * non-blocking banner that pauses the flow (advancing on a relevant action
 * hook where available, otherwise via the manual Next button on the banner).
 * This prevents subsequent explanations from popping up before the player
 * has had a chance to actually try the previous instruction.
 *
 * Hooks fired by GameScreen:
 *   FINISH_TURN     - player clicked Finish Turn
 *   PLUNDER         - player resolved a plunder
 *   PUT_DEF         - player placed a defense card
 *   TAKE_DEF        - player took a defense card back
 *   MY_TURN_START   - player turn started (after bot turn ended)
 */
final class HeroTutorialSteps {
  private HeroTutorialSteps() {}

  static GameScreen.TutorialStepDef[] forHero(String heroName) {
    if ("Mercenaries".equals(heroName))     return MERCENARIES;
    if ("Marshal".equals(heroName))         return MARSHAL;
    if ("Spy".equals(heroName))             return SPY;
    if ("Battery Tower".equals(heroName))   return BATTERY_TOWER;
    if ("Merchant".equals(heroName))        return MERCHANT;
    if ("Priest".equals(heroName))          return PRIEST;
    if ("Reservists".equals(heroName))      return RESERVISTS;
    if ("Banneret".equals(heroName))        return BANNERET;
    if ("Saboteurs".equals(heroName))       return SABOTEURS;
    if ("Fortified Tower".equals(heroName)) return FORTIFIED_TOWER;
    if ("Magician".equals(heroName))        return MAGICIAN;
    if ("Warlord".equals(heroName))         return WARLORD;
    return null;
  }

  // Common terminal step shared by all hero tutorials.
  private static final GameScreen.TutorialStepDef DONE =
    new GameScreen.TutorialStepDef(
      "Tutorial Complete!",
      "Well done! You've practised the key abilities of this hero.\n\n"
      + "You can keep playing this game freely or return to the menu to try another tutorial.",
      "Back to Menu",
      true);

  // Mercenaries
  private static final GameScreen.TutorialStepDef[] MERCENARIES = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Mercenaries",
      "You start with the Mercenaries hero. Mercenaries add +1 to attack or defense.\n\n"
      + "You have up to 8 units. 4 units recover at the start of each new turn.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Defense Boost",
      "To boost a defense card:\n"
      + "  1. Tap the Mercenaries hero icon to select it.\n"
      + "  2. Tap one of your own defense cards (top half adds, bottom half removes).\n\n"
      + "A pawn icon and +N label appears on the boosted card.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try the Defense Boost",
      "Tap Mercenaries, then tap one of your own defense cards. Click Next when done.",
      null),
    new GameScreen.TutorialStepDef(
      "Attack Boost",
      "To boost an attack:\n"
      + "  1. Select hand cards for an attack.\n"
      + "  2. Tap the Mercenaries hero icon - each tap adds +1 attack strength.\n\n"
      + "Then plunder a deck or attack an enemy as usual.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try a Plunder with Mercenaries",
      "Select a hand card, tap Mercenaries to boost, then tap a harvest deck.",
      "PLUNDER"),
    new GameScreen.TutorialStepDef(
      "Recovery",
      "Mercenaries lost in a failed attack are gone forever.\n\n"
      + "Each new turn you regain up to 4 units (capped at 8 total). "
      + "End your turn now and watch your pool refill.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' to let the bot play and see your mercenaries recover.",
      "FINISH_TURN"),
    GameScreen.TutorialStepDef.banner(
      "Bot is Playing...",
      "Wait for the bot to finish its turn.",
      "MY_TURN_START"),
    DONE,
  };

  // Marshal
  private static final GameScreen.TutorialStepDef[] MARSHAL = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Marshal",
      "Normally you have 1 take + 1 put action for defense cards per turn.\n\n"
      + "With the Marshal, both limits are replaced by a shared pool of 3 actions "
      + "you can mix and match in any combination. It's passive - no activation needed.",
      "Begin"),
    GameScreen.TutorialStepDef.banner(
      "Take a Defense Card",
      "Tap one of your defense cards to take it back to your hand.",
      "TAKE_DEF"),
    GameScreen.TutorialStepDef.banner(
      "Place a Defense Card",
      "Select a hand card, then tap an empty shield slot to place it.",
      "PUT_DEF"),
    new GameScreen.TutorialStepDef(
      "Use the Pool Freely",
      "You have actions left! Try discarding all 3 defense cards, then placing 3 fresh ones - "
      + "all in a single Marshal turn. Click Got it when ready to finish your turn.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when you're satisfied with your defense layout.",
      "FINISH_TURN"),
    DONE,
  };

  // Spy
  private static final GameScreen.TutorialStepDef[] SPY = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Spy",
      "The Spy lets you flip a face-down enemy defense card so everyone can see its value.\n\n"
      + "You start with 1 flip per turn.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Flip an Enemy Card",
      "Tap the Spy hero icon to select it, then tap one of the bot's face-down defense cards. "
      + "The card flips face-up and its value becomes visible to all players.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try Flipping",
      "Tap Spy, then tap an enemy face-down defense card. Click Next when done.",
      null),
    new GameScreen.TutorialStepDef(
      "Extend Mode",
      "If you sacrifice one of your own defense cards to the Spy, you gain +2 extra flips this turn (max 3 total).\n\n"
      + "To extend: with Spy selected, tap one of your own defense cards.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try Extending (Optional)",
      "Tap Spy, then tap one of your own defense cards to gain +2 flips. Or click Next to skip.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Spy reveals reset each turn. Click 'Finish turn' when done.",
      "FINISH_TURN"),
    DONE,
  };

  // Battery Tower
  private static final GameScreen.TutorialStepDef[] BATTERY_TOWER = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Battery Tower",
      "When one of your defense cards or your king is attacked, you can spend a charge to "
      + "completely deny that attack. You start with 1 charge that auto-refills each turn.\n\n"
      + "To trigger this scenario the bot must attack you - finish your turn first.",
      "Begin"),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' to let the bot play. You can place defense cards first if you wish.",
      "FINISH_TURN"),
    GameScreen.TutorialStepDef.banner(
      "Defend with Battery Tower",
      "If the bot attacks one of your defense cards or your king, an Allow / Deny prompt appears. "
      + "Tap 'Battery Tower' (Deny) to block the attack.",
      "MY_TURN_START"),
    new GameScreen.TutorialStepDef(
      "After the Bot's Turn",
      "If you denied an attack, the attacker's hand cards are locked for the rest of their turn "
      + "and their attack cards are revealed to you.\n\n"
      + "Battery Tower automatically refills 1 charge at the start of each new turn - "
      + "so you always have one ready when defending.",
      null),
    DONE,
  };

  // Merchant
  private static final GameScreen.TutorialStepDef[] MERCHANT = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Merchant",
      "The Merchant trades a hand card for a new random card from the deck (once per turn).\n\n"
      + "You see the new card first and can choose to keep it or try a 2nd time.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Trade a Card",
      "Tap the Merchant hero icon, then tap the hand card you want to replace.\n\n"
      + "After the new card is shown:\n"
      + "  - Keep it: old card is discarded, new card stays.\n"
      + "  - 2nd chance: discard the new card and draw again.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try the Merchant",
      "Tap Merchant, then tap a hand card to trade it. Click Next when done.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done. You get 1 trade per new turn.",
      "FINISH_TURN"),
    DONE,
  };

  // Priest
  private static final GameScreen.TutorialStepDef[] PRIEST = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Priest",
      "The Priest lets you steal a card from the enemy's hand - but only cards matching your attack symbol.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "How to Use the Priest",
      "To steal an enemy hand card:\n  1. Tap the Priest hero icon to select it.\n  2. Tap the enemy's hand card deck.\n  3. Pick a card matching your attack symbol to take it.\n\n"
      + "Note: you must make at least one attack or plunder first this turn to set your attack symbol.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try the Priest",
      "First plunder or attack to set your symbol, then tap Priest and tap the bot's hand deck. Click Next when done.",
      "PLUNDER"),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done. Conversion attempts reset each new turn.",
      "FINISH_TURN"),
    DONE,
  };

  // Reservists
  private static final GameScreen.TutorialStepDef[] RESERVISTS = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Reservists",
      "Reservists provide both a passive defense bonus and an on-demand attack boost.\n\n"
      + "You start with 2 ready units (max 4). 2 recover each new turn.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Defense Bonus (Passive)",
      "Each ready reservist automatically adds +1 to your king card's defense strength. "
      + "Look at your king - the bonus is reflected in the displayed strength.",
      null),
    new GameScreen.TutorialStepDef(
      "Attack Boost",
      "During an attack, when the attack preview overlay shows, a 'Reservists +1' button appears "
      + "if spending a reservist would flip a failing attack to a success.\n\n"
      + "Tap it to spend one unit and add +1 to the attack.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try an Attack",
      "Plunder a harvest deck or attack an enemy defense card. If the attack is failing and you "
      + "have ready reservists, the boost button will appear.",
      "PLUNDER"),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn'. 2 reservists will recover for next turn.",
      "FINISH_TURN"),
    DONE,
  };

  // Banneret
  private static final GameScreen.TutorialStepDef[] BANNERET = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Banneret",
      "The Banneret unlocks one passive ability: Dual Symbol.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Dual Symbol (Passive)",
      "When you start an attack, a paired symbol unlocks automatically:\n"
      + "  - Hearts <-> Diamonds\n"
      + "  - Spades <-> Clubs\n\n"
      + "Both symbols can be combined in the same attack round.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try a Plunder",
      "Select hand cards and plunder a harvest deck. "
      + "Notice the dual symbol unlocking.",
      "PLUNDER"),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done.",
      "FINISH_TURN"),
    DONE,
  };

  // Saboteurs
  private static final GameScreen.TutorialStepDef[] SABOTEURS = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Saboteurs",
      "You have 2 saboteur units. Place them on empty enemy defense slots to block them - "
      + "the enemy cannot place a defense card there.\n\n"
      + "If the enemy attacks through a sabotaged slot and wins, the saboteur is destroyed "
      + "and takes 2 turns to recover.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Sabotage an Empty Slot",
      "Tap the Saboteurs hero icon, then tap an empty defense slot of the bot.\n\n"
      + "(If the bot has no empty slots, attack one of their defense cards first to clear a slot.)",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try Sabotaging",
      "Tap Saboteurs, then tap an empty enemy defense slot. Click Next when done.",
      null),
    new GameScreen.TutorialStepDef(
      "Removal & Recovery",
      "The enemy can remove a saboteur on their turn. If they attack through it and win, "
      + "the saboteur dies - recovery takes 2 turns.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' to let the bot play.",
      "FINISH_TURN"),
    DONE,
  };

  // Fortified Tower
  private static final GameScreen.TutorialStepDef[] FORTIFIED_TOWER = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Fortified Tower",
      "The Fortified Tower lets you stack a second card on top of a defense slot. "
      + "When the slot is attacked, the values of both cards are added together - "
      + "making it much harder to break through.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Stack a Defense Card",
      "To stack a card:\n"
      + "  1. Tap the Fortified Tower hero icon to select it.\n"
      + "  2. Tap a hand card to select it.\n"
      + "  3. Tap one of your own defense slots - the hand card stacks on top.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try Stacking",
      "Tap Fortified Tower, select a hand card, then tap one of your own defense slots. "
      + "Click Next when done.",
      null),
    new GameScreen.TutorialStepDef(
      "Take the Stack Back",
      "You can take both cards back in one defense-take action by tapping the stacked slot. "
      + "The Marshal hero pairs nicely here.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done. The Tower's stack action refreshes each new turn.",
      "FINISH_TURN"),
    DONE,
  };

  // Magician
  private static final GameScreen.TutorialStepDef[] MAGICIAN = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Magician",
      "Once per turn, the Magician replaces all cards in an enemy defense slot with random "
      + "cards drawn from the deck. The replaced cards go to the cemetery.\n\n"
      + "The new cards' face state is inverted: face-down becomes face-up and vice versa. "
      + "Stacked slots (two cards) are replaced layer by layer - both cards swap.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Cast Card Replacement",
      "Tap the Magician hero icon, then tap any enemy defense card. "
      + "The slot is rebuilt with new cards from the deck.\n\n"
      + "If the enemy has no defense cards left, you may instead target their king card "
      + "to replace it.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try the Magician",
      "Tap Magician, then tap any enemy defense card (or king card if the enemy has no "
      + "defenses). Click Next when done.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done. The spell recharges next turn.",
      "FINISH_TURN"),
    DONE,
  };

  // Warlord
  private static final GameScreen.TutorialStepDef[] WARLORD = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Warlord",
      "The Warlord grants two abilities: a Direct King Attack (once per turn) and "
      + "a King Swap (swap your king with a hand card without spending the king attack action).",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Direct King Attack",
      "Your king can attack an enemy defense card directly - no need to clear your own defense cards first.\n\n"
      + "  1. Tap the Warlord hero icon to select it.\n"
      + "  2. Tap an enemy defense card.\n\n"
      + "Cannot be combined with hand cards in the same attack.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try a Direct King Attack",
      "Tap Warlord, then tap an enemy defense card. Click Next when done (or to skip).",
      null),
    new GameScreen.TutorialStepDef(
      "King Swap",
      "Swap your king with a hand card any time:\n"
      + "  1. Tap your own king card to select it.\n"
      + "  2. Tap the hand card you want as the new king.\n\n"
      + "The old king moves to your hand. This does NOT consume your normal once-per-turn king attack.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try a King Swap (Optional)",
      "Tap your own king, then tap a hand card to swap. Or click Next to skip.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done.",
      "FINISH_TURN"),
    DONE,
  };
}
