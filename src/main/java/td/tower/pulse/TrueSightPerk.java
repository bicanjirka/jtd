package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** True Sight: a visit's reveal lasts four seconds, and the enemy is Dazed for two. */
public final class TrueSightPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(4f * TickRate.TICKS_PER_SECOND);
    private static final int DAZE_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withVisit(spec.visit().revealing(REVEAL_TICKS).dazing(DAZE_TICKS));
    }

    @Override
    public String describe() {
        return "the visit's reveal lasts " + PerkText.seconds(REVEAL_TICKS) + " and Dazes the enemy for " + PerkText.seconds(DAZE_TICKS);
    }
}
