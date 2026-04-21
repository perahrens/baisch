package com.mygdx.game;

/**
 * Issue #171: per-hero interactive tutorial step sequences.
 *
 * Each sequence is a list of {@link GameScreen.TutorialStepDef} entries.
 * Steps are either:
 *   - blocking info overlays the player dismisses with "Got it!"
 *   - non-blocking banner steps that auto-advance when an action hook fires
 *     (e.g. "FINISH_TURN", "PLUNDER", "PUT_DEF", "TAKE_DEF")
 *   - a final terminal blocking step (Back to Menu / Keep Playing)
 *
 * Banner steps also have a "Next" button so the player can skip past
 * action gates they cannot or do not want to satisfy.
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
      + "You have up to 8 units. 4 units recover at the start of each new turn.\n\n"
      + "This tutorial walks you through both uses.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Defense Boost",
      "To boost a defense card:\n"
      + "  1. Tap the Mercenaries hero icon to select it.\n"
      + "  2. Tap one of your own defense cards (top half adds, bottom half removes).\n\n"
      + "A pawn icon and +N label appears on the boosted card.\n\n"
      + "Try it now: place mercenaries on a defense card.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try the Defense Boost",
      "Tap Mercenaries, then tap one of your own defense cards to add a unit.",
      "MERC_DEF_BOOST"),
    new GameScreen.TutorialStepDef(
      "Remove Mercenaries",
      "Tap Mercenaries again, then tap the bottom half of the boosted defense card. "
      + "The unit returns to your pool.\n\n"
      + "Mercenaries on a defense card are also lost if that card is taken away by a successful attack.",
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
    DONE,
  };

  // Marshal
  private static final GameScreen.TutorialStepDef[] MARSHAL = new GameScreen.TutorialStepDef[] {
    new GameScreen.TutorialStepDef(
      "Marshal",
      "Normally you have 1 take + 1 put action for defense cards per turn.\n\n"
      + "With the Marshal, both limits are replaced by a shared pool of 3 actions "
      + "you can mix and match in any combination.\n\n"
      + "It's passive - no activation needed.",
      "Begin"),
    GameScreen.TutorialStepDef.banner(
      "Take a Defense Card",
      "Tap one of your defense cards to take it back to your hand. With the Marshal you can "
      + "take more than one this turn.",
      "TAKE_DEF"),
    GameScreen.TutorialStepDef.banner(
      "Place a Defense Card",
      "Select a hand card, then tap an empty shield slot to place it. "
      + "You can do this multiple times this turn.",
      "PUT_DEF"),
    new GameScreen.TutorialStepDef(
      "Use the Pool Freely",
      "You have actions left! Try discarding all 3 defense cards, then placing 3 fresh ones - "
      + "all in a single Marshal turn. When done, finish your turn.",
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
      "The Spy lets you flip a face-down enemy defense card so you (and only you) can see its value.\n\n"
      + "You start with 1 flip per turn.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Flip an Enemy Card",
      "Tap the Spy hero icon to select it, then tap one of the bot's face-down defense cards. "
      + "The card flips face-up for you only.",
      null),
    new GameScreen.TutorialStepDef(
      "Extend Mode",
      "If you sacrifice one of your own defense cards to the Spy, you gain +2 extra flips this turn (max 3 total).\n\n"
      + "To extend: with Spy selected, tap one of your own defense cards.",
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
    new GameScreen.TutorialStepDef(
      "Defend with Battery Tower",
      "If the bot attacks one of your defense cards or your king this turn, an Allow / Deny prompt appears.\n\n"
      + "Tap 'Battery Tower' (Deny) to block the attack. The attacker's hand cards are then locked for the rest of their turn and their attack cards are revealed to you.",
      null),
    new GameScreen.TutorialStepDef(
      "Recovery",
      "Battery Tower automatically refills 1 charge at the start of each new turn - "
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
      "Tap the Merchant hero icon, then tap the hand card you want to replace.",
      null),
    new GameScreen.TutorialStepDef(
      "Keep or Re-roll",
      "After the new card is shown:\n"
      + "  - Keep it: old card is discarded, new card stays.\n"
      + "  - 2nd chance: discard the new card and draw again (uses your trade action again).",
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
      "After initiating an attack on an enemy defense card, the Priest can attempt to convert "
      + "one of the defender's hand cards to your attack symbol - adding it to your hand.\n\n"
      + "You have up to 2 conversion attempts per turn.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Set Your Attack Symbol",
      "Select a hand card. The symbol on that card becomes your attack symbol for this turn. "
      + "The Priest needs an attack symbol locked in.",
      null),
    new GameScreen.TutorialStepDef(
      "Initiate an Attack",
      "Tap an enemy defense card to start an attack. After the attack overlay appears, the Priest can act.",
      null),
    new GameScreen.TutorialStepDef(
      "Convert a Card",
      "Tap the Priest hero icon and follow the conversion dialog. The bot's hand is revealed and you pick a card.",
      null),
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
      "The Banneret unlocks two passive abilities: Dual Symbol and Defense-to-Attack.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Dual Symbol (Passive)",
      "When you start an attack, a paired symbol unlocks automatically:\n"
      + "  - Hearts <-> Diamonds\n"
      + "  - Spades <-> Clubs\n\n"
      + "Both symbols can be combined in the same attack round.",
      null),
    new GameScreen.TutorialStepDef(
      "Defense-to-Attack (Passive)",
      "Your own defense cards can also be used as attack cards. Select a mix of hand cards "
      + "and own defense cards, then attack as usual. Used defense cards are discarded.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Try a Plunder",
      "Select hand cards (and optionally own defense cards) and plunder a harvest deck. "
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
    new GameScreen.TutorialStepDef(
      "Removal & Recovery",
      "The enemy can remove a saboteur on their turn. If they attack through it and win, "
      + "the saboteur dies - recover takes 2 turns.",
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
      "The Fortified Tower lets you place a second card face-down on top of an existing defense slot - "
      + "an attacker must break through the top card before reaching the card below.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Stack a Defense Card",
      "  1. Select a hand card with the same symbol as a defense card.\n"
      + "  2. Tap the Fortified Tower hero icon to select it.\n"
      + "  3. Tap one of your own defense slots - the hand card stacks on top.",
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
      + "The new cards' face state is inverted: face-down becomes face-up and vice versa.",
      "Begin"),
    new GameScreen.TutorialStepDef(
      "Cast Card Replacement",
      "Tap the Magician hero icon, then tap any enemy defense card. "
      + "The slot is rebuilt with new cards from the deck.",
      null),
    new GameScreen.TutorialStepDef(
      "Stacked Slots",
      "Stacked slots (two cards) are replaced layer by layer - both cards swap.",
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
    new GameScreen.TutorialStepDef(
      "King Swap",
      "Swap your king with a hand card any time:\n"
      + "  1. Tap your own king card to select it.\n"
      + "  2. Tap the hand card you want as the new king.\n\n"
      + "The old king moves to your hand. This does NOT consume your normal once-per-turn king attack.",
      null),
    GameScreen.TutorialStepDef.banner(
      "Finish Your Turn",
      "Click 'Finish turn' when done.",
      "FINISH_TURN"),
    DONE,
  };
}
