package com.mygdx.game.heroes;

/** Provides help text descriptions for each hero. */
public class HeroDescriptions {

  public static String get(String name) {
    if ("Warlord".equals(name)) {
      return "ABILITY 1 – Direct King Attack\n"
           + "(once per turn)\n"
           + "\n"
           + "Your king attacks an enemy defense card directly, bypassing the normal requirement\n"
           + "of having no own defense cards first. Cannot combine with hand cards in the same attack.\n"
           + "\n"
           + "To activate:\n"
           + "  • Select Warlord\n"
           + "  • Tap an enemy defense card\n"
           + "\n"
           + "ABILITY 2 – King Swap\n"
           + "\n"
           + "Swap your king card with a hand card. The hand card becomes the new king;\n"
           + "the old king moves to your hand.\n"
           + "\n"
           + "To activate:\n"
           + "  • Select your own king card\n"
           + "  • Tap the hand card you want to place as the new king";
    } else if ("Magician".equals(name)) {
      return "ABILITY – Card Replacement\n"
           + "(once per turn)\n"
           + "\n"
           + "Replaces all cards in an enemy defense slot with random cards drawn from the deck.\n"
           + "Replaced cards go to the cemetery.\n"
           + "\n"
           + "The new cards inherit an inverted face state: whatever was face-down appears face-up\n"
           + "and vice versa. Stacked slots (two cards) are replaced independently layer by layer.\n"
           + "\n"
           + "To activate:\n"
           + "  • Select Magician\n"
           + "  • Tap any enemy defense card";
    } else if ("Spy".equals(name)) {
      return "ABILITY – Card Peek\n"
           + "(starts with 1 flip per turn)\n"
           + "\n"
           + "Flip a face-down enemy defense card face-up to see its value.\n"
           + "Only you see the identity; the card stays revealed until the turn ends.\n"
           + "\n"
           + "Extend mode: sacrifice one of your own defense cards to gain +2 extra flip actions\n"
           + "this turn (max 3 total).\n"
           + "\n"
           + "To activate:\n"
           + "  • Select Spy, then tap a face-down enemy card\n"
           + "  • To extend: with Spy selected, tap one of your own defense cards";
    } else if ("Battery Tower".equals(name)) {
      return "ABILITY – Attack Denial\n"
           + "(1 charge per turn, auto-refills)\n"
           + "\n"
           + "When one of your defense cards or your king is under attack, spend the charge\n"
           + "to completely deny that attack. The attacker’s remaining hand cards are locked\n"
           + "for the turn and their attack cards are revealed to you.\n"
           + "\n"
           + "To activate:\n"
           + "  • When an incoming attack prompt appears, tap the Battery Tower button to block it";
    } else if ("Fortified Tower".equals(name)) {
      return "ABILITY – Defense Stack\n"
           + "(once per turn)\n"
           + "\n"
           + "Places a second card face-down on top of an existing defense slot, adding a layer\n"
           + "of protection. The attacker must break through the top card before reaching the card below.\n"
           + "\n"
           + "To activate:\n"
           + "  • Select Fortified Tower\n"
           + "  • Tap one of your own defense slots that already has a card";
    } else if ("Saboteurs".equals(name)) {
      return "ABILITY – Slot Blockade\n"
           + "(2 saboteur units)\n"
           + "\n"
           + "Place a saboteur on an empty enemy defense slot to block it.\n"
           + "The enemy cannot place a defense card there while the saboteur is active.\n"
           + "The enemy can remove it on their turn.\n"
           + "\n"
           + "If the enemy attacks through the sabotaged slot and wins, the saboteur is destroyed\n"
           + "and takes 2 turns to recover.\n"
           + "\n"
           + "To activate:\n"
           + "  • Select Saboteurs\n"
           + "  • Tap an empty enemy defense slot";
    } else if ("Mercenaries".equals(name)) {
      return "ATTACK BOOST\n"
           + "\n"
           + "With attack hand cards already selected, tap the Mercenaries to add a unit\n"
           + "to the battle (+1 attack strength per tap). Tap multiple times for more boost.\n"
           + "\n"
           + "To activate: select attack cards first, then tap Mercenaries\n"
           + "\n"
           + "DEFENSE BOOST\n"
           + "\n"
           + "Assign a mercenary to one of your own defense cards (+1 defense strength).\n"
           + "\n"
           + "To activate: select Mercenaries, then tap your own defense card\n"
           + "\n"
           + "You have up to 8 units total. 4 recover each new turn.\n"
           + "Units lost in a failed attack are gone.";
    } else if ("Marshal".equals(name)) {
      return "ABILITY – Flexible Repositioning\n"
           + "(passive, always active)\n"
           + "\n"
           + "Normally you have 1 take and 1 put action for defense cards per turn.\n"
           + "With the Marshal, both limits are replaced by a shared pool of 3 actions\n"
           + "that can be used in any combination of takes and puts.\n"
           + "\n"
           + "No activation needed. The expanded action pool is available automatically each turn.";
    } else if ("Merchant".equals(name)) {
      return "ABILITY – Card Trade\n"
           + "(once per turn)\n"
           + "\n"
           + "Trade a hand card for a new random card from the deck.\n"
           + "The drawn card is shown to you first:\n"
           + "\n"
           + "  • Keep it: the old card is discarded, the new card stays\n"
           + "  • 2nd chance: discard the drawn card and draw again (spends the trade action again)\n"
           + "\n"
           + "To activate:\n"
           + "  • Select Merchant\n"
           + "  • Tap the hand card you want to replace";
    } else if ("Priest".equals(name)) {
      return "ABILITY – Card Conversion\n"
           + "(up to 2 attempts per turn)\n"
           + "\n"
           + "After initiating an attack on an enemy defense card, attempt to convert the defending\n"
           + "card to your attack symbol. The enemy’s hand is revealed and you pick a card;\n"
           + "on success it is added to your hand.\n"
           + "\n"
           + "Requires: your attack symbol must already be set\n"
           + "(i.e. you have started an attack by selecting hand cards).\n"
           + "\n"
           + "To activate:\n"
           + "  • Initiate an attack first (select hand cards + tap enemy defense)\n"
           + "  • Then select Priest and follow the conversion dialog";
    } else if ("Banneret".equals(name)) {
      return "ABILITY 1 – Dual Symbol\n"
           + "(passive, always active)\n"
           + "\n"
           + "Automatically unlocks a paired attack symbol each turn:\n"
           + "  • Hearts ↔ Diamonds\n"
           + "  • Spades ↔ Clubs\n"
           + "\n"
           + "Both symbols in the pair can be used together in a single attack round.\n"
           + "\n"
           + "ABILITY 2 – Defense-to-Attack\n"
           + "\n"
           + "Your own defense cards can also be used as attack cards.\n"
           + "Select a mix of hand cards and own defense cards, then attack as usual.\n"
           + "Used defense cards are discarded to the cemetery after the attack.\n"
           + "\n"
           + "No activation needed for either ability.";
    } else if ("Reservists".equals(name)) {
      return "DEFENSE BONUS\n"
           + "(passive, always active)\n"
           + "\n"
           + "Each ready reservist automatically adds +1 to your king card’s defense strength.\n"
           + "No action required.\n"
           + "\n"
           + "ATTACK BOOST\n"
           + "\n"
           + "During an attack, after the attack preview appears, tap Reservists to spend\n"
           + "one unit for +1 attack strength. Tap multiple times for more boost.\n"
           + "\n"
           + "To activate (attack): tap Reservists during the attack preview\n"
           + "\n"
           + "You have up to 4 reservists. Starts with 2 ready. 2 recover each new turn.";
    }
    return "No information available.";
  }
}
