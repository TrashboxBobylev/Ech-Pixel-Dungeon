package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.AbyssalNightmare;
import com.watabou.noosa.audio.Sample;

public class Nightmare extends Corruption {

    @Override
    public boolean attachTo(Char target) {
        if (super.attachTo(target)){
            target.alignment = Char.Alignment.ENEMY;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean act() {
        buildToDamage += target.HT/30f;

        int damage = (int)buildToDamage;
        buildToDamage -= damage;

        if (damage > 0) {
            target.damage(damage, this);
        }

        spend(TICK);

        return true;
    }
}
