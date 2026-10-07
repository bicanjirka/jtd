package td.tower.seeker;

/** Attune's Nest: the cooldown loads a missile, up to three, and stored missiles launch as a salvo. */
public final class NestPerk implements SeekerPerk {

    private static final int CAPACITY = 3;
    private static final int LAUNCH_GAP_TICKS = 4;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withNest(NestSpec.of(CAPACITY, LAUNCH_GAP_TICKS));
    }
}
