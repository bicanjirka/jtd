package td.tower.pulse;

import td.util.TickRate;

/** Phase Field II: an enemy is revealed for two seconds when it gains its first Toll stack of a visit. */
public final class RevealOnEntryPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withVisit(spec.visit().revealing(REVEAL_TICKS));
    }
}
