package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerTerm;

/** Conductor: the Arc's jumps can crit, each rolled on its own. */
public final class ConductorMechanic implements SplashMechanic {

    private static final float CRIT_CHANCE = 0.15f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().withCritChanceAtLeast(CRIT_CHANCE));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus("'s jumps can crit: ").plus(TowerStat.CRIT_CHANCE)
                .plus(" " + TermNumbers.percent(CRIT_CHANCE) + ", rolled per jump");
    }
}
