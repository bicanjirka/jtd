package td.tower.cinder;

/** Wide Nozzle: the cone is 30% wider, and an enemy that leaves it keeps its Stoke for two seconds. */
public final class WideNozzlePerk implements CinderPerk {

    private static final float WIDTH = 1.3f;
    private static final int GRACE_TICKS = 40;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withConeScaledBy(WIDTH).withStoke(spec.stoke().withGraceTicks(GRACE_TICKS));
    }
}
