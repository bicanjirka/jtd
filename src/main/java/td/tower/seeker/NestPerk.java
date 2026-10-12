package td.tower.seeker;

/** Attune's Nest: the cooldown loads a missile, and stored missiles launch as a salvo. */
public final class NestPerk implements SeekerPerk {

    private static final int LAUNCH_GAP_TICKS = 4;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withLaunchGapTicks(LAUNCH_GAP_TICKS);
    }
}
