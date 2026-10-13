package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Phase Field II: an enemy is revealed for two seconds when it gains its first Toll stack of a visit. */
public final class RevealOnEntryPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withVisit(spec.visit().revealing(REVEAL_TICKS));
    }

    @Override
    public String describe() {
        return "an enemy is revealed to every tower for " + PerkText.seconds(REVEAL_TICKS)
                + " when it gains its first Toll stack, once per visit";
    }
}
