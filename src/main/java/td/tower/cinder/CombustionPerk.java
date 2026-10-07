package td.tower.cinder;

/** Combustion: a pool holds twice as much, and one that reaches its cap bursts for half of it within a cell. */
public final class CombustionPerk implements CinderPerk {

    private static final float CAP_FACTOR = 4f;
    private static final float SHARE = 0.5f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCapFactor(CAP_FACTOR)).withCombustion(CombustionSpec.of(SHARE, RADIUS_CELLS));
    }
}
