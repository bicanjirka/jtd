package td.tower.cinder;

/** Attune's Stoke: a wave on an enemy already burning from this Cinder raises its burn 10%, up to three times. */
public final class StokePerk implements CinderPerk {

    private static final int MAX_STEPS = 3;
    private static final float STEP = 0.1f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withStoke(StokeSpec.of(MAX_STEPS, STEP));
    }
}
