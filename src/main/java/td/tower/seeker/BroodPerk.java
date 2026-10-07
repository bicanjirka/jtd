package td.tower.seeker;

/** Brood: the nest holds six, and a salvo spreads across different targets. */
public final class BroodPerk implements SeekerPerk {

    private static final int CAPACITY = 6;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withNest(spec.nest().withCapacity(Math.max(spec.nest().capacity(), CAPACITY))).withSpreadSalvo();
    }
}
