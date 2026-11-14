package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** High Voltage: the Arc's jumps hit harder. */
public final class HighVoltageMechanic implements SplashMechanic {

    private static final float SHARE = 0.6f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().withShareAtLeast(SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus("'s jumps deal " + TermNumbers.percent(SHARE) + " of base damage, up from "
                + TermNumbers.percent(ArcSpec.SHARE));
    }
}
