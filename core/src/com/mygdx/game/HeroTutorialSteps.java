package com.mygdx.game;

final class HeroTutorialSteps {
  private HeroTutorialSteps() {}

  static GameScreen.TutorialStepDef[] forHero(String heroName) {
    if ("Mercenaries".equals(heroName)) return mercenaries();
    if ("Marshal".equals(heroName)) return marshal();
    if ("Spy".equals(heroName)) return spy();
    if ("Battery Tower".equals(heroName)) return batteryTower();
    if ("Merchant".equals(heroName)) return merchant();
    if ("Priest".equals(heroName)) return priest();
    if ("Reservists".equals(heroName)) return reservists();
    if ("Banneret".equals(heroName)) return banneret();
    if ("Saboteurs".equals(heroName)) return saboteurs();
    if ("Fortified Tower".equals(heroName)) return fortifiedTower();
    if ("Magician".equals(heroName)) return magician();
    if ("Warlord".equals(heroName)) return warlord();
    return null;
  }

  private static boolean isGerman() {
    return Localization.DE.equals(Localization.getLanguage());
  }

  private static String tr(String english, String german) {
    return isGerman() ? german : english;
  }

  private static GameScreen.TutorialStepDef doneStep() {
    return new GameScreen.TutorialStepDef(
      tr("Tutorial Complete!", "Tutorial abgeschlossen!"),
      tr(
        "Well done! You've practised the key abilities of this hero.\n\nYou can keep playing this game freely or return to the menu to try another tutorial.",
        "Gut gemacht! Du hast die wichtigsten Faehigkeiten dieses Helden geuebt.\n\nDu kannst diese Partie weiterspielen oder ins Menue zurueckkehren und ein anderes Tutorial ausprobieren."
      ),
      tr("Back to Menu", "Zurueck zum Menue"),
      true
    );
  }

  private static GameScreen.TutorialStepDef[] mercenaries() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Mercenaries", "Soeldner"), tr("You start with the Mercenaries hero. Mercenaries add +1 to attack or defense.\n\nYou have up to 8 units. 4 units recover at the start of each new turn.", "Du startest mit dem Helden Soeldner. Soeldner geben +1 Angriff oder +1 Verteidigung.\n\nDu hast bis zu 8 Einheiten. Zu Beginn jedes neuen Zuges erholen sich 4 Einheiten."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Defense Boost", "Verteidigungsbonus"), tr("To boost a defense card:\n  1. Tap the Mercenaries hero icon to select it.\n  2. Tap one of your own defense cards.\n\nA pawn icon and +N label appears on the boosted card.", "Um eine Verteidigungskarte zu verstaerken:\n  1. Tippe auf das Soeldner-Heldenicon.\n  2. Tippe auf eine deiner Verteidigungskarten.\n\nAuf der verstaerkten Karte erscheinen ein Bauernsymbol und ein +N-Wert."), null),
      GameScreen.TutorialStepDef.banner(tr("Try the Defense Boost", "Probiere den Verteidigungsbonus"), tr("Tap Mercenaries, then tap one of your own defense cards. Click Next when done.", "Tippe auf Soeldner und danach auf eine deiner Verteidigungskarten. Klicke auf Weiter, wenn du fertig bist."), null),
      new GameScreen.TutorialStepDef(tr("Attack Boost", "Angriffsbonus"), tr("To boost an attack:\n  1. Select hand cards for an attack.\n  2. Tap the Mercenaries hero icon - each tap adds +1 attack strength.\n\nThen loot a deck or attack an enemy as usual.", "Um einen Angriff zu verstaerken:\n  1. Waehle Handkarten fuer einen Angriff aus.\n  2. Tippe auf das Soeldner-Heldenicon - jeder Tipp gibt +1 Angriff.\n\nPluendere danach wie gewohnt einen Stapel oder greife einen Gegner an."), null),
      GameScreen.TutorialStepDef.banner(tr("Try a Loot with Mercenaries", "Probiere eine Pluenderung mit Soeldnern"), tr("Select a hand card, tap Mercenaries to boost, then tap a harvest deck.", "Waehle eine Handkarte, verstaerke sie mit Soeldnern und tippe dann auf einen Erntestapel."), "LOOT"),
      new GameScreen.TutorialStepDef(tr("Recovery", "Erholung"), tr("Mercenaries lost in a failed attack are gone forever.\n\nEach new turn you regain up to 4 units (capped at 8 total). End your turn now and watch your pool refill.", "Soeldner, die bei einem gescheiterten Angriff verloren gehen, sind dauerhaft weg.\n\nIn jedem neuen Zug erhaeltst du bis zu 4 Einheiten zurueck, maximal 8 insgesamt. Beende jetzt deinen Zug und beobachte, wie sich dein Vorrat erholt."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' to let the bot play and see your mercenaries recover.", "Klicke auf 'Zug beenden', damit der Bot spielt und sich deine Soeldner erholen."), "FINISH_TURN"),
      GameScreen.TutorialStepDef.banner(tr("Bot is Playing...", "Der Bot spielt..."), tr("Wait for the bot to finish its turn.", "Warte, bis der Bot seinen Zug beendet hat."), "MY_TURN_START"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] marshal() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef("Marshal", tr("Normally you have 1 take + 1 put action for defense cards per turn.\n\nWith the Marshal, both limits are replaced by a shared pool of 3 actions you can mix and match in any combination. It's passive - no activation needed.", "Normalerweise hast du pro Zug 1 Nehmen- und 1 Legen-Aktion fuer Verteidigungskarten.\n\nMit dem Marshal werden beide Grenzen durch einen gemeinsamen Pool von 3 Aktionen ersetzt, die du beliebig kombinieren kannst. Die Faehigkeit ist passiv - keine Aktivierung noetig."), tr("Begin", "Start")),
      GameScreen.TutorialStepDef.banner(tr("Take a Defense Card", "Nimm eine Verteidigungskarte"), tr("Tap one of your defense cards to take it back to your hand.", "Tippe auf eine deiner Verteidigungskarten, um sie zurueck auf die Hand zu nehmen."), "TAKE_DEF"),
      GameScreen.TutorialStepDef.banner(tr("Place a Defense Card", "Lege eine Verteidigungskarte"), tr("Select a hand card, then tap an empty shield slot to place it.", "Waehle eine Handkarte aus und tippe dann auf einen leeren Schildslot."), "PUT_DEF"),
      new GameScreen.TutorialStepDef(tr("Use the Pool Freely", "Nutze den Aktionspool frei"), tr("You have actions left! Try discarding all 3 defense cards, then placing 3 fresh ones - all in a single Marshal turn. Click Got it when ready to finish your turn.", "Du hast noch Aktionen uebrig! Versuche, alle 3 Verteidigungskarten abzuwerfen und danach 3 neue zu legen - alles in einem einzigen Marshal-Zug. Klicke auf Verstanden, wenn du danach beenden willst."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when you're satisfied with your defense layout.", "Klicke auf 'Zug beenden', wenn du mit deiner Verteidigung zufrieden bist."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] spy() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Spy", "Spion"), tr("The Spy lets you flip a face-down enemy defense card so everyone can see its value.\n\nYou start with 1 flip per turn.", "Mit dem Spion kannst du eine verdeckte gegnerische Verteidigungskarte aufdecken, damit alle ihren Wert sehen.\n\nDu startest mit 1 Aufdecken pro Zug."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Flip an Enemy Card", "Decke eine gegnerische Karte auf"), tr("Tap the Spy hero icon to select it, then tap one of the bot's face-down defense cards. The card flips face-up and its value becomes visible to all players.", "Tippe auf das Spion-Heldenicon und danach auf eine verdeckte gegnerische Verteidigungskarte. Die Karte wird sichtbar und ihr Wert ist fuer alle erkennbar."), null),
      GameScreen.TutorialStepDef.banner(tr("Try Flipping", "Probiere das Aufdecken"), tr("Tap Spy, then tap an enemy face-down defense card. Click Next when done.", "Tippe auf Spion und danach auf eine verdeckte gegnerische Verteidigungskarte. Klicke auf Weiter, wenn du fertig bist."), null),
      new GameScreen.TutorialStepDef(tr("Extend Mode", "Erweiterungsmodus"), tr("If you sacrifice one of your own defense cards to the Spy, you gain +2 extra flips this turn (max 3 total).\n\nTo extend: with Spy selected, tap one of your own defense cards.", "Wenn du dem Spion eine eigene Verteidigungskarte opferst, erhaeltst du in diesem Zug 2 zusaetzliche Aufdeckungen, maximal 3 insgesamt.\n\nWaehle dazu den Spion und tippe auf eine eigene Verteidigungskarte."), null),
      GameScreen.TutorialStepDef.banner(tr("Try Extending (Optional)", "Probiere das Erweitern (optional)"), tr("Tap Spy, then tap one of your own defense cards to gain +2 flips. Or click Next to skip.", "Tippe auf Spion und danach auf eine eigene Verteidigungskarte, um 2 zusaetzliche Aufdeckungen zu erhalten. Oder klicke auf Weiter."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Spy reveals reset each turn. Click 'Finish turn' when done.", "Die Aufdeckungen des Spions werden in jedem Zug zurueckgesetzt. Klicke auf 'Zug beenden', wenn du fertig bist."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] batteryTower() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Battery Tower", "Batterieturm"), tr("When one of your defense cards or your king is attacked, you can spend a charge to completely deny that attack. You start with 1 charge that auto-refills each turn.\n\nTo trigger this scenario the bot must attack you - finish your turn first.", "Wenn eine deiner Verteidigungskarten oder dein Koenig angegriffen wird, kannst du eine Ladung ausgeben und den Angriff komplett abwehren. Du startest mit 1 Ladung, die sich in jedem Zug automatisch erneuert.\n\nDamit diese Situation eintritt, muss der Bot dich angreifen - beende also zuerst deinen Zug."), tr("Begin", "Start")),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' to let the bot play. You can place defense cards first if you wish.", "Klicke auf 'Zug beenden', damit der Bot spielt. Wenn du moechtest, kannst du vorher noch Verteidigungskarten legen."), "FINISH_TURN"),
      GameScreen.TutorialStepDef.banner(tr("Defend with Battery Tower", "Verteidige mit dem Batterieturm"), tr("If the bot attacks one of your defense cards or your king, an Allow / Deny prompt appears. Tap 'Battery Tower' (Deny) to block the attack.", "Wenn der Bot eine deiner Verteidigungskarten oder deinen Koenig angreift, erscheint eine Erlauben-/Ablehnen-Abfrage. Tippe auf 'Batterieturm', um den Angriff zu blockieren."), "MY_TURN_START"),
      new GameScreen.TutorialStepDef(tr("After the Bot's Turn", "Nach dem Zug des Bots"), tr("If you denied an attack, the attacker's hand cards are locked for the rest of their turn and their attack cards are revealed to you.\n\nBattery Tower automatically refills 1 charge at the start of each new turn - so you always have one ready when defending.", "Wenn du einen Angriff abgewehrt hast, sind die Handkarten des Angreifers fuer den Rest seines Zuges gesperrt und seine Angriffskarten werden dir gezeigt.\n\nDer Batterieturm erhaelt zu Beginn jedes neuen Zuges automatisch 1 Ladung zurueck."), null),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] merchant() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Merchant", "Haendler"), tr("The Merchant trades a hand card for a new random card from the deck (once per turn).\n\nYou see the new card first and can choose to keep it or try a 2nd time.", "Der Haendler tauscht einmal pro Zug eine Handkarte gegen eine neue Zufallskarte vom Stapel.\n\nDu siehst die neue Karte zuerst und kannst entscheiden, ob du sie behaeltst oder einen zweiten Versuch startest."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Trade a Card", "Tausche eine Karte"), tr("Tap the Merchant hero icon, then tap the hand card you want to replace.\n\nAfter the new card is shown:\n  - Keep it: old card is discarded, new card stays.\n  - 2nd chance: discard the new card and draw again.", "Tippe auf das Haendler-Heldenicon und danach auf die Handkarte, die du ersetzen willst.\n\nDanach kannst du die neue Karte behalten oder fuer einen zweiten Versuch erneut tauschen."), null),
      GameScreen.TutorialStepDef.banner(tr("Try the Merchant", "Probiere den Haendler"), tr("Tap Merchant, then tap a hand card to trade it. Click Next when done.", "Tippe auf Haendler und danach auf eine Handkarte, um sie zu tauschen. Klicke auf Weiter, wenn du fertig bist."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when done. You get 1 trade per new turn.", "Klicke auf 'Zug beenden', wenn du fertig bist. In jedem neuen Zug bekommst du wieder 1 Tausch."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] priest() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Priest", "Priester"), tr("The Priest lets you steal a card from the enemy's hand - but only cards matching your attack symbol.", "Der Priester erlaubt dir, eine Karte aus der gegnerischen Hand zu stehlen - aber nur Karten, die zu deinem Angriffssymbol passen."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("How to Use the Priest", "So benutzt du den Priester"), tr("To steal an enemy hand card:\n  1. Tap the Priest hero icon to select it.\n  2. Tap the enemy's hand card deck.\n  3. Pick a card matching your attack symbol to take it.\n\nNote: you must make at least one attack or loot first this turn to set your attack symbol.", "Um eine gegnerische Handkarte zu stehlen:\n  1. Tippe auf das Priester-Heldenicon.\n  2. Tippe auf den gegnerischen Handkartenstapel.\n  3. Waehle eine Karte mit deinem Angriffssymbol.\n\nWichtig: Du musst in diesem Zug vorher mindestens einmal angegriffen oder gepluendert haben, damit dein Angriffssymbol feststeht."), null),
      GameScreen.TutorialStepDef.banner(tr("Try the Priest", "Probiere den Priester"), tr("First loot or attack to set your symbol, then tap Priest and tap the bot's hand deck. Click Next when done.", "Greife zuerst an oder pluendere, damit dein Symbol festgelegt wird. Tippe dann auf Priester und auf den Handstapel des Bots. Klicke auf Weiter, wenn du fertig bist."), "LOOT"),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when done. Conversion attempts reset each new turn.", "Klicke auf 'Zug beenden', wenn du fertig bist. Die Umwandlungsversuche werden in jedem Zug zurueckgesetzt."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] reservists() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Reservists", "Reservisten"), tr("Reservists provide both a passive defense bonus and an on-demand attack boost.\n\nYou start with 2 ready units (max 4). 2 recover each new turn.", "Reservisten geben dir sowohl einen passiven Verteidigungsbonus als auch einen aktivierbaren Angriffsbonus.\n\nDu startest mit 2 bereiten Einheiten, maximal 4. In jedem neuen Zug erholen sich 2 Einheiten."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Defense Bonus (Passive)", "Verteidigungsbonus (passiv)"), tr("Each ready reservist automatically adds +1 to your king card's defense strength. Look at your king - the bonus is reflected in the displayed strength.", "Jeder bereite Reservist gibt deiner Koenigskarte automatisch +1 Verteidigung. Schau auf deinen Koenig - der Bonus ist im angezeigten Wert bereits enthalten."), null),
      new GameScreen.TutorialStepDef(tr("Attack Boost", "Angriffsbonus"), tr("During an attack, when the attack preview overlay shows, a 'Reservists +1' button appears if spending a reservist would flip a failing attack to a success.\n\nTap it to spend one unit and add +1 to the attack.", "Waehren eines Angriffs kann ein Button 'Reservisten +1' erscheinen, wenn ein zusaetzlicher Punkt den Angriff von Misserfolg auf Erfolg drehen wuerde.\n\nTippe darauf, um eine Einheit auszugeben und +1 Angriff zu erhalten."), null),
      GameScreen.TutorialStepDef.banner(tr("Try an Attack", "Probiere einen Angriff"), tr("Loot a harvest deck or attack an enemy defense card. If the attack is failing and you have ready reservists, the boost button will appear.", "Pluendere einen Erntestapel oder greife eine gegnerische Verteidigung an. Wenn der Angriff knapp scheitert und du bereite Reservisten hast, erscheint der Bonus-Button."), "LOOT"),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn'. 2 reservists will recover for next turn.", "Klicke auf 'Zug beenden'. 2 Reservisten erholen sich fuer den naechsten Zug."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] banneret() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Banneret", "Bannertraeger"), tr("The Banneret unlocks one passive ability: Dual Symbol.", "Der Bannertraeger schaltet eine passive Faehigkeit frei: Doppelsymbol."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Dual Symbol (Passive)", "Doppelsymbol (passiv)"), tr("When you start an attack, a paired symbol unlocks automatically:\n  - Hearts <-> Diamonds\n  - Spades <-> Clubs\n\nBoth symbols can be combined in the same attack round.", "Wenn du einen Angriff beginnst, wird automatisch ein passendes Zweitsymbol freigeschaltet:\n  - Herzen <-> Karo\n  - Pik <-> Kreuz\n\nBeide Symbole koennen dann im selben Angriff kombiniert werden."), null),
      GameScreen.TutorialStepDef.banner(tr("Try a Loot", "Probiere eine Pluenderung"), tr("Select hand cards and loot a harvest deck. Notice the dual symbol unlocking.", "Waehle Handkarten aus und pluendere einen Erntestapel. Achte darauf, wie das zweite Symbol freigeschaltet wird."), "LOOT"),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when done.", "Klicke auf 'Zug beenden', wenn du fertig bist."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] saboteurs() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Saboteurs", "Saboteure"), tr("You have 2 saboteur units. Place them on empty enemy defense slots to block them - the enemy cannot place a defense card there.\n\nIf the enemy attacks through a sabotaged slot and wins, the saboteur is destroyed and takes 2 turns to recover.", "Du hast 2 Saboteur-Einheiten. Platziere sie auf leeren gegnerischen Verteidigungsslots, um diese zu blockieren - dort kann der Gegner keine Verteidigungskarte legen.\n\nGreift der Gegner erfolgreich durch einen sabotierten Slot an, wird der Saboteur zerstoert und braucht 2 Zuege zur Erholung."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Sabotage an Empty Slot", "Sabotiere einen leeren Slot"), tr("Tap the Saboteurs hero icon, then tap an empty defense slot of the bot.\n\n(If the bot has no empty slots, attack one of their defense cards first to clear a slot.)", "Tippe auf das Saboteure-Heldenicon und danach auf einen leeren Verteidigungsslot des Bots.\n\nFalls der Bot keinen leeren Slot hat, greife zuerst eine seiner Verteidigungskarten an, um einen Slot freizumachen."), null),
      GameScreen.TutorialStepDef.banner(tr("Try Sabotaging", "Probiere das Sabotieren"), tr("Tap Saboteurs, then tap an empty enemy defense slot. Click Next when done.", "Tippe auf Saboteure und danach auf einen leeren gegnerischen Verteidigungsslot. Klicke auf Weiter, wenn du fertig bist."), null),
      new GameScreen.TutorialStepDef(tr("Removal & Recovery", "Entfernung und Erholung"), tr("The enemy can remove a saboteur on their turn. If they attack through it and win, the saboteur dies - recovery takes 2 turns.", "Der Gegner kann in seinem Zug einen Saboteur entfernen. Greift er erfolgreich durch einen sabotierten Slot an, stirbt der Saboteur - die Erholung dauert 2 Zuege."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' to let the bot play.", "Klicke auf 'Zug beenden', damit der Bot spielt."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] fortifiedTower() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Fortified Tower", "Festungsturm"), tr("The Fortified Tower lets you stack a second card on top of a defense slot. When the slot is attacked, the values of both cards are added together - making it much harder to break through.", "Der Festungsturm erlaubt dir, eine zweite Karte auf einen Verteidigungsslot zu stapeln. Wird dieser Slot angegriffen, werden die Werte beider Karten addiert - dadurch wird er deutlich schwerer zu durchbrechen."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Stack a Defense Card", "Stapele eine Verteidigungskarte"), tr("Stacking is automatic - no need to select the Fortified Tower hero first:\n  1. Tap a hand card to select it.\n  2. Matching defense slots are highlighted in purple.\n  3. Tap one of the highlighted slots - the hand card stacks on top.\n\nOnly matching symbols can be stacked, and the slot must not already be stacked.", "Das Stapeln geschieht automatisch - du musst den Festungsturm nicht zuerst auswaehlen:\n  1. Waehle eine Handkarte.\n  2. Passende Verteidigungsslots werden violett markiert.\n  3. Tippe auf einen markierten Slot, um die Karte dort zu stapeln.\n\nNur passende Symbole duerfen gestapelt werden und der Slot darf noch nicht belegt sein."), null),
      GameScreen.TutorialStepDef.banner(tr("Try Stacking", "Probiere das Stapeln"), tr("Select a hand card, then tap one of the highlighted matching defense slots. Click Next when done.", "Waehle eine Handkarte und tippe dann auf einen passend markierten Verteidigungsslot. Klicke auf Weiter, wenn du fertig bist."), null),
      new GameScreen.TutorialStepDef(tr("Take the Stack Back", "Nimm den Stapel zurueck"), tr("You can take both cards back in one defense-take action by tapping the stacked slot. The Marshal hero pairs nicely here.", "Du kannst beide Karten mit einer einzigen Nehmen-Aktion zurueck auf die Hand holen, indem du auf den gestapelten Slot tippst. Der Marshal passt gut zu dieser Faehigkeit."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when done. The Tower's stack action refreshes each new turn.", "Klicke auf 'Zug beenden', wenn du fertig bist. Die Stapelaktion des Turms wird in jedem neuen Zug erneuert."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] magician() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Magician", "Magier"), tr("Once per turn, the Magician replaces all cards in an enemy defense slot with random cards drawn from the deck. The replaced cards go to the cemetery.\n\nThe new cards' face state is inverted: face-down becomes face-up and vice versa.", "Einmal pro Zug ersetzt der Magier alle Karten in einem gegnerischen Verteidigungsslot durch zufaellige Karten vom Stapel. Die alten Karten gehen auf den Friedhof.\n\nDer Sichtbarkeitszustand der neuen Karten wird umgedreht: verdeckt wird offen und offen wird verdeckt."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Cast Card Replacement", "Nutze den Kartentausch"), tr("Tap the Magician hero icon, then tap any enemy defense card. The slot is rebuilt with new cards from the deck.\n\nIf the enemy has no defense cards left, you may instead target their king card to replace it.", "Tippe auf das Magier-Heldenicon und danach auf eine gegnerische Verteidigungskarte. Der Slot wird mit neuen Karten vom Stapel neu aufgebaut.\n\nHat der Gegner keine Verteidigung mehr, darfst du stattdessen seine Koenigskarte anvisieren."), null),
      GameScreen.TutorialStepDef.banner(tr("Try the Magician", "Probiere den Magier"), tr("Tap Magician, then tap any enemy defense card (or king card if the enemy has no defenses). Click Next when done.", "Tippe auf Magier und danach auf eine gegnerische Verteidigungskarte, oder auf die Koenigskarte, falls keine Verteidigung mehr liegt. Klicke auf Weiter, wenn du fertig bist."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when done. The spell recharges next turn.", "Klicke auf 'Zug beenden', wenn du fertig bist. Der Zauber wird im naechsten Zug wieder aufgeladen."), "FINISH_TURN"),
      doneStep(),
    };
  }

  private static GameScreen.TutorialStepDef[] warlord() {
    return new GameScreen.TutorialStepDef[] {
      new GameScreen.TutorialStepDef(tr("Warlord", "Kriegsherr"), tr("The Warlord grants two abilities: a Direct King Attack (once per turn) and a King Swap (swap your king with a hand card without spending the king attack action).", "Der Kriegsherr verleiht zwei Faehigkeiten: einen direkten Koenigsangriff einmal pro Zug und einen Koenigstausch, bei dem du deinen Koenig mit einer Handkarte tauschst, ohne den Koenigsangriff zu verbrauchen."), tr("Begin", "Start")),
      new GameScreen.TutorialStepDef(tr("Direct King Attack", "Direkter Koenigsangriff"), tr("Your king can attack an enemy defense card directly - no need to clear your own defense cards first.\n\n  1. Tap the Warlord hero icon to select it.\n  2. Tap an enemy defense card.\n\nCannot be combined with hand cards in the same attack.", "Dein Koenig kann eine gegnerische Verteidigungskarte direkt angreifen - du musst deine eigenen Verteidigungskarten vorher nicht entfernen.\n\n  1. Tippe auf das Kriegsherr-Heldenicon.\n  2. Tippe auf eine gegnerische Verteidigungskarte.\n\nDiese Aktion kann nicht mit Handkarten kombiniert werden."), null),
      GameScreen.TutorialStepDef.banner(tr("Try a Direct King Attack", "Probiere einen direkten Koenigsangriff"), tr("Tap Warlord, then tap an enemy defense card. Click Next when done (or to skip).", "Tippe auf Kriegsherr und danach auf eine gegnerische Verteidigungskarte. Klicke auf Weiter, wenn du fertig bist oder ueberspringen willst."), null),
      new GameScreen.TutorialStepDef(tr("King Swap", "Koenigstausch"), tr("Swap your king with a hand card any time:\n  1. Tap your own king card to select it.\n  2. Tap the hand card you want as the new king.\n\nThe old king moves to your hand. This does NOT consume your normal once-per-turn king attack.", "Du kannst deinen Koenig jederzeit mit einer Handkarte tauschen:\n  1. Tippe auf deine Koenigskarte.\n  2. Tippe auf die Handkarte, die neuer Koenig werden soll.\n\nDer alte Koenig wandert auf deine Hand. Diese Aktion verbraucht nicht deinen normalen Koenigsangriff."), null),
      GameScreen.TutorialStepDef.banner(tr("Try a King Swap (Optional)", "Probiere einen Koenigstausch (optional)"), tr("Tap your own king, then tap a hand card to swap. Or click Next to skip.", "Tippe auf deinen Koenig und danach auf eine Handkarte, um zu tauschen. Oder klicke auf Weiter, um zu ueberspringen."), null),
      GameScreen.TutorialStepDef.banner(tr("Finish Your Turn", "Beende deinen Zug"), tr("Click 'Finish turn' when done.", "Klicke auf 'Zug beenden', wenn du fertig bist."), "FINISH_TURN"),
      doneStep(),
    };
  }
}
