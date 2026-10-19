package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Sight: every enemy inside is revealed, and a moment after it leaves. */
public final class SightPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(0.5f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSight(spec.sight().revealing(REVEAL_TICKS));
    }

    @Override
    public String describe() {
        return "enemies inside are revealed to every tower, and for " + PerkText.seconds(REVEAL_TICKS) + " after they leave";
    }
}
