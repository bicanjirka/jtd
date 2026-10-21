package td.tower.splash;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Arc: the blast carries past its edge as lightning. */
public final class ArcPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(ArcSpec.arcs());
    }

    @Override
    public TermText describe() {
        return TermText.of("the blast carries past its edge: " + ArcSpec.BASE_JUMPS + " jumps at " + PerkText.percent(ArcSpec.BASE_SHARE)
                + " as magic, further from Saturated enemies");
    }
}
