package td.tower.cinder;

/** Combustion: a pool that reaches its cap bursts for half of it within a cell. */
public final class CombustionPerk implements CinderPerk {

    private static final float SHARE = 0.5f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withCombustion(CombustionSpec.of(SHARE, RADIUS_CELLS));
    }
}
