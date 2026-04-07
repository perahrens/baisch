package com.mygdx.game;

import java.util.ArrayList;
import com.mygdx.game.heroes.BatteryTower;
import com.mygdx.game.heroes.FortifiedTower;
import com.mygdx.game.heroes.Hero;
import com.mygdx.game.heroes.Banneret;
import com.mygdx.game.heroes.Magician;
import com.mygdx.game.heroes.Marshal;
import com.mygdx.game.heroes.Mercenaries;
import com.mygdx.game.heroes.Merchant;
import com.mygdx.game.heroes.Priest;
import com.mygdx.game.heroes.Reservists;
import com.mygdx.game.heroes.Saboteurs;
import com.mygdx.game.heroes.Spy;
import com.mygdx.game.heroes.Warlord;

public class HeroesSquare {
	
	//white heroes
	private Mercenaries mercenaries;
	private Marshal marshal;
	private Spy spy;
	private BatteryTower batteryTower;
	private Merchant merchant;
	private Priest priest;

	//black heroes
	private Reservists reservists;
	private Banneret banneret;
	private Saboteurs saboteurs;
	private FortifiedTower fortifiedTower;
	private Magician magician;
	private Warlord warlord;
	
	public HeroesSquare() {
		//white heroes
		mercenaries = new Mercenaries();
		marshal = new Marshal();
		spy = new Spy();
		batteryTower = new BatteryTower();
		merchant = new Merchant();
		priest = new Priest();
		
		//black heroes
		reservists = new Reservists();
		banneret = new Banneret();
		saboteurs = new Saboteurs();
		fortifiedTower = new FortifiedTower();
		magician = new Magician();
		warlord = new Warlord();
	}
	
	public Hero getHero(int index) {
		Hero hero = null;
		switch (index) {
		case 2:
			hero = mercenaries;
			mercenaries = null;
			break;
		case 3:
			hero = marshal;
			marshal = null;
			break;
		case 4:
			hero = spy;
			spy = null;
			break;
		case 5:
			hero = batteryTower;
			batteryTower = null;
			break;
		case 6:
			hero = merchant;
			merchant = null;
			break;
		case 7:
			hero = priest;
			priest = null;
			break;
		case 8:
			hero = reservists;
			reservists = null;
			break;
		case 9:
			hero = banneret;
			banneret = null;
			break;
		case 10:
			hero = saboteurs;
			saboteurs = null;
			break;
		case 11:
			hero = fortifiedTower;
			fortifiedTower = null;
			break;
		case 12:
			hero = magician;
			magician = null;
			break;
		case 13:
			hero = warlord;
			warlord = null;
			break;
		default:
			break;
		}
		
		return hero;
	}
	
	public void addHero(int index) {
		switch (index) {
		case 2:
			mercenaries = new Mercenaries();
			break;
		case 3:
			marshal = new Marshal();
			break;
		case 4:
			spy = new Spy();
			break;
		case 5:
			batteryTower = new BatteryTower();
			break;
		case 6:
			merchant = new Merchant();
			break;
		case 7:
			priest = new Priest();
			break;
		case 8:
			reservists = new Reservists();
			break;
		case 9:
			banneret = new Banneret();
			break;
		case 10:
			saboteurs = new Saboteurs();
			break;
		case 11:
			fortifiedTower = new FortifiedTower();
			break;
		case 12:
			magician = new Magician();
			break;
		case 13:
			warlord = new Warlord();
			break;
		default:
			break;
		}
	}

	/**
	 * Maps a drawn card's index (2-13) to the corresponding hero per the game rules:
	 * 2=Mercenaries, 3=Spy, 4=Marshal, 5=BatteryTower, 6=Merchant, 7=Priest,
	 * 8=Reservists, 9=Saboteurs, 10=Banneret, 11=FortifiedTower, 12=Magician, 13=King.
	 * Returns null if the hero is already taken.
	 */
	public Hero getHeroByCardIndex(int cardIndex) {
		switch (cardIndex) {
			case 2:  return getHero(2);   // Mercenaries
			case 3:  return getHero(4);   // Spy (internal index 4)
			case 4:  return getHero(3);   // Marshal (internal index 3)
			case 5:  return getHero(5);   // BatteryTower
			case 6:  return getHero(6);   // Merchant
			case 7:  return getHero(7);   // Priest
			case 8:  return getHero(8);   // Reservists
			case 9:  return getHero(10);  // Saboteurs (internal index 10)
			case 10: return getHero(9);   // Banneret (internal index 9)
			case 11: return getHero(11);  // FortifiedTower
			case 12: return getHero(12);  // Magician
			case 13: return getHero(13);  // Warlord (Black King)
			default: return null;
		}
	}

	/**
	 * Returns the hero name associated with a direct card index (2-13), regardless of
	 * whether the hero is still available. Used to identify which hero was drawn when
	 * {@link #getHeroByCardIndex} returns null (hero already owned by a player).
	 */
	public static String heroNameByCardIndex(int cardIndex) {
		switch (cardIndex) {
			case 2:  return "Mercenaries";
			case 3:  return "Spy";
			case 4:  return "Marshal";
			case 5:  return "Battery Tower";
			case 6:  return "Merchant";
			case 7:  return "Priest";
			case 8:  return "Reservists";
			case 9:  return "Saboteurs";
			case 10: return "Banneret";
			case 11: return "Fortified Tower";
			case 12: return "Magician";
			case 13: return "Warlord";
			default: return null;
		}
	}

	/** Peek at the hero at an internal index without consuming it. */
	private Hero peekHero(int index) {
		switch (index) {
			case 2:  return mercenaries;
			case 3:  return marshal;
			case 4:  return spy;
			case 5:  return batteryTower;
			case 6:  return merchant;
			case 7:  return priest;
			case 8:  return reservists;
			case 9:  return banneret;
			case 10: return saboteurs;
			case 11: return fortifiedTower;
			case 12: return magician;
			case 13: return warlord;
			default: return null;
		}
	}

	/** Consume a hero from the pool by name (used when player makes a selection). */
	public Hero consumeHeroByName(String heroName) {
		for (int i = 2; i <= 13; i++) {
			Hero h = peekHero(i);
			if (h != null && heroName.equals(h.getHeroName())) {
				return getHero(i);
			}
		}
		return null;
	}

	/** All still-available white heroes (Mercenaries, Spy, Marshal, BatteryTower, Merchant, Priest). */
	public ArrayList<Hero> getAvailableWhiteHeroes() {
		ArrayList<Hero> list = new ArrayList<Hero>();
		if (mercenaries != null)  list.add(mercenaries);
		if (spy != null)          list.add(spy);
		if (marshal != null)      list.add(marshal);
		if (batteryTower != null) list.add(batteryTower);
		if (merchant != null)     list.add(merchant);
		if (priest != null)       list.add(priest);
		return list;
	}

	/** All still-available black heroes (Reservists, Saboteurs, Banneret, FortifiedTower, Magician, Warlord). */
	public ArrayList<Hero> getAvailableBlackHeroes() {
		ArrayList<Hero> list = new ArrayList<Hero>();
		if (reservists != null)    list.add(reservists);
		if (saboteurs != null)     list.add(saboteurs);
		if (banneret != null)      list.add(banneret);
		if (fortifiedTower != null) list.add(fortifiedTower);
		if (magician != null)      list.add(magician);
		if (warlord != null)       list.add(warlord);
		return list;
	}

	/** All still-available heroes (white + black). */
	public ArrayList<Hero> getAvailableAllHeroes() {
		ArrayList<Hero> list = getAvailableWhiteHeroes();
		list.addAll(getAvailableBlackHeroes());
		return list;
	}

}
