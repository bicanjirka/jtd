package td.tower.cinder;

/** Kindling: burning enemies earn Scorched twice as fast. */
public final class KindlingPerk implements CinderPerk {

    private static final int RATE = 2;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withStackRate(RATE));
    }
}
