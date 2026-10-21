package td.tower.seeker;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

/** Attune's Nest: the cooldown loads a missile, and stored missiles launch as a salvo. */
public final class NestPerk implements SeekerPerk {

    private static final int LAUNCH_GAP_TICKS = 4;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withLaunchGapTicks(LAUNCH_GAP_TICKS);
    }

    @Override
    public TermText describe() {
        return TermText.of("the cooldown loads a missile into the ").plus(TowerTerm.NEST)
                .plus(" instead of firing; with a target in range, stored missiles launch "
                        + TermNumbers.seconds(LAUNCH_GAP_TICKS / (float) TickRate.TICKS_PER_SECOND) + " apart");
    }
}
