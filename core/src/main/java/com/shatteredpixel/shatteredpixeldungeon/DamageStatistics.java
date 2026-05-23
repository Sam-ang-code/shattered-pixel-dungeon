/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.watabou.utils.Bundle;

public class DamageStatistics {

	public static long heroDamageDealt;
	public static long heroDamageTaken;
	public static int enemiesDefeated;
	public static int heroAttacksMade;

	private static final String HERO_DAMAGE_DEALT = "hero_damage_dealt";
	private static final String HERO_DAMAGE_TAKEN = "hero_damage_taken";
	private static final String ENEMIES_DEFEATED = "enemies_defeated";
	private static final String HERO_ATTACKS_MADE = "hero_attacks_made";

	private DamageStatistics() {
	}

	public static void reset() {
		heroDamageDealt = 0;
		heroDamageTaken = 0;
		enemiesDefeated = 0;
		heroAttacksMade = 0;
	}

	public static void storeInBundle( Bundle bundle ) {
		bundle.put( HERO_DAMAGE_DEALT, heroDamageDealt );
		bundle.put( HERO_DAMAGE_TAKEN, heroDamageTaken );
		bundle.put( ENEMIES_DEFEATED, enemiesDefeated );
		bundle.put( HERO_ATTACKS_MADE, heroAttacksMade );
	}

	public static void restoreFromBundle( Bundle bundle ) {
		heroDamageDealt = bundle.getLong( HERO_DAMAGE_DEALT );
		heroDamageTaken = bundle.getLong( HERO_DAMAGE_TAKEN );
		enemiesDefeated = bundle.getInt( ENEMIES_DEFEATED );
		heroAttacksMade = bundle.getInt( HERO_ATTACKS_MADE );
	}

	public static void recordHeroAttack() {
		heroAttacksMade++;
	}

	public static void recordHeroDamageTaken( int damage ) {
		if (damage > 0) {
			heroDamageTaken += damage;
		}
	}

	public static void recordEnemyDefeated() {
		enemiesDefeated++;
	}

	public static void recordDamageDealt( Char target, Object source, int damage ) {
		if (damage > 0 && isHeroDamageSource(source) && target != Dungeon.hero && target.alignment == Char.Alignment.ENEMY) {
			heroDamageDealt += damage;
		}
	}

	private static boolean isHeroDamageSource( Object source ) {
		if (source == null) {
			return false;
		}
		if (source == Dungeon.hero || source instanceof Hero) {
			return true;
		}
		if (source instanceof Wand || source instanceof ClericSpell || source instanceof ArmorAbility) {
			return true;
		}
		if (source instanceof Weapon || source instanceof Weapon.Enchantment) {
			return true;
		}
		return false;
	}
}
