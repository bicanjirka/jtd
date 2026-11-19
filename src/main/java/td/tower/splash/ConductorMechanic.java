package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerTerm;

/** Conductor: the Arc's jumps can crit, each rolled on its own. */
public final class ConductorMechanic implements SplashMechanic {

    private static final float CRIT_CHANCE = 0.15f;
    /** What a crit of a jump multiplies its damage by. */
    private static final float CRIT_MULTIPLIER = 4.5f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().withCritChanceAtLeast(CRIT_CHANCE).withCritMultiplierAtLeast(CRIT_MULTIPLIER));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus("'s jumps can crit: ").plus(TowerStat.CRIT_CHANCE)
                .plus(" " + TermNumbers.percent(CRIT_CHANCE) + ", rolled per jump, and a crit of a jump deals "
                + TermNumbers.percent(CRIT_MULTIPLIER));
    }
}
