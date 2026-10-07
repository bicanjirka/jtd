package td.tower.cinder;

/** Wide Nozzle II: the cone is 20% wider. */
public final class NozzleWidthPerk implements CinderPerk {

    private static final float FACTOR = 1.2f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withConeScaledBy(FACTOR);
    }
}
