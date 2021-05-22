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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbyssalSprite extends MobSprite {

	private Emitter cloud;

	public AbyssalSprite() {
		super();
		
		texture( Assets.Sprites.WRAITH );
		
		TextureFilm frames = new TextureFilm( texture, 14, 15 );
		
		idle = new Animation( 10, true );
		idle.frames( frames, 0, 1 );
		
		run = new Animation( 15, true );
		run.frames( frames, 0, 1 );
		
		attack = new Animation( 15, false );
		attack.frames( frames, 0, 2, 3 );
		
		die = new Animation( 32, false );
		die.frames( frames, 0, 4, 5, 6, 7 );
		
		play( idle );
	}
	
	@Override
	public int blood() {
		return 0x88000000;
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );

		if (cloud == null) {
			cloud = emitter();
			cloud.pour( Speck.factory( Speck.SMOKE ), 0.03f );
		}
	}

	@Override
	public void update() {

		super.update();

		if (cloud != null) {
			cloud.visible = visible;
		}
		if (visible){
			Random.pushGenerator();
			if (Random.Int(28) == 0)
				Sample.INSTANCE.play(Random.element(
					Arrays.asList(Assets.Sounds.ABYSSAL_1, Assets.Sounds.ABYSSAL_2, Assets.Sounds.ABYSSAL_3,
							Assets.Sounds.ABYSSAL_4, Assets.Sounds.ABYSSAL_5, Assets.Sounds.ABYSSAL_6)), 0.5f);
		}
	}

	@Override
	public void kill() {
		super.kill();

		if (cloud != null) {
			cloud.on = false;
		}
	}
}
