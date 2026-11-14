package td.tower.splash;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Burst may aim at, what its passive is, how its Arc runs, how its hexes bite and how fast its stones earn its
 * currency: the base rules reshaped by each mechanic it owns. Each {@code withX} is a copy.
 *
 * @param reach         the enemies it may aim at
 * @param passive       what its stones leave on the enemies they damage
 * @param arc           how a charged stone's Arc runs
 * @param hexes         how its hexes bite
 * @param damagePerUnit the damage its stones deal for one unit of its currency, in points
 */
public record SplashSpec(Reach reach, Passive passive, ArcSpec arc, HexSpec hexes, float damagePerUnit) {

    /** The damage a stone deals for one Spark or Soul, in points. */
    public static final float DAMAGE_PER_UNIT = 200f;

    /** The visible enemies in range, no passive, and the bare Arc and hexes. */
    public static SplashSpec from(Viewpoint view) {
        return new SplashSpec(Reach.visible(view), Passive.NONE, ArcSpec.bare(), HexSpec.bare(), DAMAGE_PER_UNIT);
    }

    public SplashSpec withPassive(Passive passive) {
        return new SplashSpec(this.reach, passive, this.arc, this.hexes, this.damagePerUnit);
    }

    public SplashSpec withArc(ArcSpec arc) {
        return new SplashSpec(this.reach, this.passive, arc, this.hexes, this.damagePerUnit);
    }

    public SplashSpec withHexes(HexSpec hexes) {
        return new SplashSpec(this.reach, this.passive, this.arc, hexes, this.damagePerUnit);
    }

    /** Earns a unit for {@code points} of damage, if that is faster than it earns now. */
    public SplashSpec withDamagePerUnitAtMost(float points) {
        return new SplashSpec(this.reach, this.passive, this.arc, this.hexes, Math.min(this.damagePerUnit, points));
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
