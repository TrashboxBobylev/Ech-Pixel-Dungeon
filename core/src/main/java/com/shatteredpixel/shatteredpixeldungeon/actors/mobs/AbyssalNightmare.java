/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 *  Shattered Pixel Dungeon
 *  Copyright (C) 2014-2022 Evan Debenham
 *
 * Summoning Pixel Dungeon
 * Copyright (C) 2019-2022 TrashboxBobylev
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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.DisintegrationTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrimTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AbyssalSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.BArray;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class AbyssalNightmare extends Wraith {

	{
		spriteClass = AbyssalSprite.class;

		flying = true;
		baseSpeed = 1.25f;

		properties.add(Property.INORGANIC);
		properties.add(Property.UNDEAD);
		properties.add(Property.DEMONIC);
		properties.add(Property.BOSS);
		properties.add(Property.LARGE);
	}

	private static final float SPLIT_DELAY	= 1f;
	
	int generation	= 0;
	int regenTurns = 0;
	
	private static final String GENERATION	= "generation";
	private static final String REGEN       = "regenTurns";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( GENERATION, generation );
		bundle.put( REGEN, regenTurns);
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		generation = bundle.getInt( GENERATION );
		regenTurns = bundle.getInt( REGEN);
		if (generation > 0) EXP = 0;
	}

	@Override
	public boolean canSee(int pos) {
		return true;
	}

	@Override
	public int damageRoll() {
		return (int) (super.damageRoll() * Random.NormalFloat( 1.5f, 2f ));
	}

	@Override
	public int attackSkill( Char target ) {
		return 18 + level;
	}

	public void adjustStats( int level ) {
		this.level = level;
		defenseSkill = attackSkill( null ) + 4;
		enemySeen = true;
		HP = HT = level*8;
	}

	@Override
	public float attackDelay() {
		return super.attackDelay()*1.5f;
	}

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );

		if (++regenTurns == 3) {
			HP = HT;
			CellEmitter.bottom(pos).burst(SmokeParticle.FACTORY, 20);
		}

		boolean justAlerted = alerted;
		alerted = false;

		if (justAlerted){
			sprite.showAlert();
		} else {
			sprite.hideAlert();
			sprite.hideLost();
		}

		if (paralysed > 0) {
			enemySeen = false;
			spend( TICK );
			return true;
		}

		enemy = chooseEnemy();

		boolean enemyInFOV = enemy != null && enemy.isAlive() && enemy.invisible == 0;

		return state.act( enemyInFOV, justAlerted );
	}

	@Override
	public boolean isImmune(Class effect) {
		return !effect.isAssignableFrom(Corruption.class) && super.isImmune(effect);
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		if (Random.Int(3) == 0){
			ArrayList<Integer> candidates = new ArrayList<>();
			boolean[] solid = Dungeon.level.solid;

			int[] neighbours = {pos + 1, pos - 1, pos + Dungeon.level.width(), pos - Dungeon.level.width()};
			for (int n : neighbours) {
				if (!solid[n] && Actor.findChar( n ) == null) {
					candidates.add( n );
				}
			}

			if (candidates.size() > 0) {

				AbyssalNightmare clone = split();
				clone.HP = HP;
				clone.pos = Random.element( candidates );
				clone.state = clone.HUNTING;

				Dungeon.level.occupyCell(clone);

				GameScene.add( clone, SPLIT_DELAY );
				Actor.addDelayed( new Pushing( clone, pos, clone.pos ), -1 );
			}
		}
		return super.attackProc(enemy, damage);
	}

	private AbyssalNightmare split() {
		AbyssalNightmare clone = new AbyssalNightmare();
		clone.EXP = 0;
		clone.adjustStats(level);
		clone.state = clone.HUNTING;
		ChampionEnemy.rollForChampion(clone);
		if (buff(Corruption.class ) != null) {
			Buff.affect( clone, Corruption.class);
		}
		return clone;
	}

	@Override
	protected boolean getCloser(int target) {
		if (super.getCloser(target)){
			return true;
		} else {

			if (target == pos || Dungeon.level.adjacent(pos, target)) {
				return false;
			}

			int bestpos = pos;
			for (int i : PathFinder.NEIGHBOURS8){
				PathFinder.buildDistanceMap(pos+i, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
				if (PathFinder.distance[pos+i] == Integer.MAX_VALUE){
					continue;
				}
				if (Actor.findChar(pos+i) == null &&
						Dungeon.level.trueDistance(bestpos, target) > Dungeon.level.trueDistance(pos+i, target)){
					bestpos = pos+i;
				}
			}
			if (bestpos != pos){

				for (int i : PathFinder.CIRCLE8){
					if ((Dungeon.level.map[pos+i] == Terrain.WALL || Dungeon.level.map[pos+i] == Terrain.WALL_DECO ||
							Dungeon.level.map[pos+i] == Terrain.DOOR || Dungeon.level.map[pos+i] == Terrain.SECRET_DOOR)){
						Level.set(pos+i, Terrain.EMPTY);
						if (Dungeon.hero.fieldOfView[pos+i]){
							CellEmitter.bottom(pos+i).burst(SmokeParticle.FACTORY, 12);
						}
						GameScene.updateMap(pos+i);
					}
				}
				Dungeon.level.cleanWalls();
				Dungeon.observe();

				bestpos = pos;
				for (int i : PathFinder.NEIGHBOURS8){
					if (Actor.findChar(pos+i) == null && Dungeon.level.openSpace[pos+i] &&
							Dungeon.level.trueDistance(bestpos, target) > Dungeon.level.trueDistance(pos+i, target)){
						bestpos = pos+i;
					}
				}

				if (bestpos != pos) {
					move(bestpos);
				}

				return true;
			}

			return false;
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		regenTurns = -1;
	}

	public static AbyssalNightmare spawnAt(int pos ) {
			AbyssalNightmare w = new AbyssalNightmare();
			w.adjustStats( Dungeon.depth );
			w.pos = pos;
			w.state = w.HUNTING;
			ChampionEnemy.rollForChampion(w);
			GameScene.add( w, 1f );

			w.sprite.alpha( 0 );
			w.sprite.parent.add( new AlphaTweener( w.sprite, 1, 0.5f ) );
			CellEmitter.bottom(pos).burst(SmokeParticle.FACTORY, 20);

			w.sprite.emitter().burst( ShadowParticle.CURSE, 20 );

			return w;
	}

	{
		immunities.add( Blizzard.class );
		immunities.add( ConfusionGas.class );
		immunities.add( CorrosiveGas.class );
		immunities.add( Electricity.class );
		immunities.add( Fire.class );
		immunities.add( Freezing.class );
		immunities.add( Inferno.class );
		immunities.add( ParalyticGas.class );
		immunities.add( Regrowth.class );
		immunities.add( SmokeScreen.class );
		immunities.add( StenchGas.class );
		immunities.add( StormCloud.class );
		immunities.add( ToxicGas.class );
		immunities.add( Web.class );

		immunities.add( Burning.class );
		immunities.add( Charm.class );
		immunities.add( Chill.class );
		immunities.add( Frost.class );
		immunities.add( Ooze.class );
		immunities.add( Paralysis.class );
		immunities.add( Poison.class );
		immunities.add( Corrosion.class );
		immunities.add( Weakness.class );
		immunities.add( MagicalSleep.class);
		immunities.add( Vertigo.class);
		immunities.add( Terror.class);
		immunities.add( Vulnerable.class);
		immunities.add( Slow.class);
		immunities.add( Blindness.class);
		immunities.add( Cripple.class);
		immunities.add( Drowsy.class);
		immunities.add( Hex.class);
		immunities.add( Sleep.class);

		immunities.add( DisintegrationTrap.class );
		immunities.add( GrimTrap.class );

		immunities.add( WandOfBlastWave.class );
		immunities.add( WandOfDisintegration.class );
		immunities.add( WandOfFireblast.class );
		immunities.add( WandOfFrost.class );
		immunities.add( WandOfLightning.class );
		immunities.add( WandOfLivingEarth.class );
		immunities.add( WandOfMagicMissile.class );
		immunities.add( WandOfPrismaticLight.class );
		immunities.add( WandOfTransfusion.class );
		immunities.add( WandOfWarding.Ward.class );

		immunities.add( Shaman.EarthenBolt.class );
		immunities.add( Warlock.DarkBolt.class );
		immunities.add( Eye.DeathGaze.class );
		immunities.add( YogFist.BrightFist.LightBeam.class );

		immunities.add(Tengu.FireAbility.FireBlob.class);

		immunities.add(Grim.class);
		immunities.add(Kinetic.class);
		immunities.add(Blazing.class);
		immunities.add(Shocking.class);
		immunities.add(Vampiric.class);
	}
}
