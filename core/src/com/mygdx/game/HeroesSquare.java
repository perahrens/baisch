package com.mygdx.game;

import com.mygdx.heroes.BatteryTower;
import com.mygdx.heroes.FortifiedTower;
import com.mygdx.heroes.Hero;
import com.mygdx.heroes.King;
import com.mygdx.heroes.Lieutenant;
import com.mygdx.heroes.Magician;
import com.mygdx.heroes.Major;
import com.mygdx.heroes.Mercenaries;
import com.mygdx.heroes.Merchant;
import com.mygdx.heroes.Priest;
import com.mygdx.heroes.Reservists;
import com.mygdx.heroes.Saboteurs;
import com.mygdx.heroes.Spy;

public class HeroesSquare {
	
	//white heroes
	private Mercenaries mercenaries;
	private Major major;
	private Spy spy;
	private BatteryTower batteryTower;
	private Merchant merchant;
	private Priest priest;

	//black heroes
	private Reservists reservists;
	private Lieutenant lieutenant;
	private Saboteurs saboteurs;
	private FortifiedTower fortifiedTower;
	private Magician magician;
	private King king;
	
	public HeroesSquare() {
		//white heroes
		mercenaries = new Mercenaries();
		major = new Major();
		spy = new Spy();
		batteryTower = new BatteryTower();
		merchant = new Merchant();
		priest = new Priest();
		
		//black heroes
		reservists = new Reservists();
		lieutenant = new Lieutenant();
		saboteurs = new Saboteurs();
		fortifiedTower = new FortifiedTower();
		magician = new Magician();
		king = new King();
	}
	
	public Hero getHero(int index) {
		Hero hero = null;
		switch (index) {
		case 2:
			hero = mercenaries;
			mercenaries = null;
			break;
		case 3:
			hero = major;
			major = null;
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
			hero = lieutenant;
			lieutenant = null;
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
			hero = king;
			king = null;
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
			major = new Major();
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
			lieutenant = new Lieutenant();
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
			king = new King();
			break;
		default:
			break;
		}
	}

}
