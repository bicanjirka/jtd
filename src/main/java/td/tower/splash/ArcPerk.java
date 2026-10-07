package td.tower.splash;

/** Arc: the blast carries past its edge as lightning. */
public final class ArcPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(ArcSpec.arcs());
    }
}
