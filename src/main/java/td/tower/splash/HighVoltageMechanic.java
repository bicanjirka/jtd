package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** High Voltage: the Arc's jumps hit harder. */
public final class HighVoltageMechanic implements SplashMechanic {

    private static final float POINTS = 7f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().withPointsAtLeast(POINTS));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus("'s jumps deal " + TermNumbers.number(POINTS) + " magic damage, up from "
                + TermNumbers.number(ArcSpec.POINTS));
    }
}
