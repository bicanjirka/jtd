package td.tower.cinder;

/** Attune's Stoke: a wave on an enemy already burning from this Cinder raises its burn, up to three times. */
public final class StokePerk implements CinderPerk {

    private static final int MAX_STEPS = 3;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withStoke(StokeSpec.of(MAX_STEPS));
    }

    @Override
    public String describe() {
        return "Stoke: a wave on an enemy already burning from this tower raises its burn a step, up to " + MAX_STEPS
                + " times; the stacks go when the burn ends";
    }
}
