package td.tower.splash;

import td.stat.EnemyStat;
import td.term.TermText;
import td.tower.TowerTerm;

/** Short Circuit: the enemy's armor makes the Arc hit harder. */
public final class ShortCircuitMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().shortCircuiting());
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus(" turns ").plus(EnemyStat.ARMOR)
                .plus(" against its enemy: a jump hits harder by the share the armor would cut from a physical hit");
    }
}
