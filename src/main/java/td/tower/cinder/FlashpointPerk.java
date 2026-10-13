package td.tower.cinder;

/** Flashpoint: a critical ignition also adds three Scorched. */
public final class FlashpointPerk implements CinderPerk {

    private static final int STACKS = 3;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withCritScorch(STACKS);
    }

    @Override
    public String describe() {
        return "a critical ignition also adds " + STACKS + " Scorched";
    }
}
