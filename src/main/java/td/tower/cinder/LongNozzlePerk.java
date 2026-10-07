package td.tower.cinder;

/** Range III's Long Nozzle: the wave travels twice as fast. */
public final class LongNozzlePerk implements CinderPerk {

    private static final float FACTOR = 2f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withWaveSpeedScaledBy(FACTOR);
    }
}
