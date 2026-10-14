package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Phase Field II: a visit's reveal lasts two seconds. */
public final class LongerRevealPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withVisit(spec.visit().revealing(REVEAL_TICKS));
    }

    @Override
    public String describe() {
        return "the visit's reveal lasts " + PerkText.seconds(REVEAL_TICKS);
    }
}
