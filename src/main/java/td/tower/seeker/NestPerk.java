package td.tower.seeker;

import td.term.TermText;

/** Attune's Nest: the cooldown loads a missile, and stored missiles launch as a salvo. */
public final class NestPerk implements SeekerPerk {

    private static final int LAUNCH_GAP_TICKS = 4;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withLaunchGapTicks(LAUNCH_GAP_TICKS);
    }

    @Override
    public TermText describe() {
        return TermText.of("Nest: the cooldown loads a missile into the nest instead of firing; with a target in range, stored missiles launch "
                + LAUNCH_GAP_TICKS + " ticks apart");
    }
}
