package td.tower.splash;

import td.damage.Damage;
import td.damage.DamageUnits;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Stormcaller: its stones and its Arc tag what they damage with Static, which discharges once the enemy is badly hurt. */
public final class StaticMechanic implements SplashMechanic {

    /** A tagged enemy discharges at this share of its health or less. */
    public static final float DISCHARGE_AT = 0.1f;
    /** What one stack discharges, in points of magic damage. */
    public static final float DISCHARGE_POINTS = 15f;

    /** What one stack discharges into its enemy. */
    public static Damage discharge() {
        return Damage.magic(DamageUnits.ofPoints(DISCHARGE_POINTS));
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withPassive(Passive.STATIC);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(TowerTerm.STATIC);
    }

    @Override
    public TermText describe() {
        return TermText.of("passive ").plus(TowerTerm.STATIC).plus(": every enemy its stones or its ")
                .plus(TowerTerm.ARC).plus(" damage carries a stack; at " + TermNumbers.percent(DISCHARGE_AT)
                        + " health or less it discharges " + TermNumbers.whole(DISCHARGE_POINTS)
                        + " magic damage per stack into itself and gives ")
                .plus(PassivePayout.describe(TowerTerm.SPARK));
    }
}
