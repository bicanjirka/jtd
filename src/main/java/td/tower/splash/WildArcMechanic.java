package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;

/** Wild Arc: the Arc may jump back to an enemy it already hit. */
public final class WildArcMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().revisiting());
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus(" may jump to an enemy it already hit");
    }
}
