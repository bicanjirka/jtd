package td.tower.splash;

/** Potency: strengthens the chain's level I. On Arc, one more jump and arcs at 65% of the blast. */
public final class PotencyPerk implements SplashPerk {

    private static final float ARC_SHARE = 0.65f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        if (!spec.arcs().active()) {
            return spec;
        }
        return spec.withArcs(spec.arcs().withMoreJumps(1).withShareAtLeast(ARC_SHARE));
    }
}
