package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Phase Field: an enemy is revealed and Dazed for half a second when it gains its first Toll stack of a visit. */
public final class RevealOnEntryPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(0.5f * TickRate.TICKS_PER_SECOND);
    private static final int DAZE_TICKS = Math.round(0.5f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withVisit(spec.visit().revealing(REVEAL_TICKS).dazing(DAZE_TICKS));
    }

    @Override
    public String describe() {
        return "an enemy is revealed to every tower for " + PerkText.seconds(REVEAL_TICKS) + " and Dazed for "
                + PerkText.seconds(DAZE_TICKS) + " when it gains its first Toll stack, once per visit";
    }
}
