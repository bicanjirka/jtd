package td.tower.cinder;

/** Lingering Flames: each wave leaves a patch of burning ground where its target stands, for two seconds. */
public final class LingeringFlamesPerk implements CinderPerk {

    private static final float RADIUS_CELLS = 0.7f;
    private static final int TICKS = 40;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withLinger(LingerSpec.of(RADIUS_CELLS, TICKS));
    }
}
