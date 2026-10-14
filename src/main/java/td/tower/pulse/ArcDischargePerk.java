package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Arc Discharge: once a second a zap marks the healthiest visible enemy in the field for every tower. */
public final class ArcDischargePerk implements PulsePerk {

    private static final int MARK_TICKS = Math.round(3f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withZap(ZapSpec.marking(MARK_TICKS));
    }

    @Override
    public String describe() {
        return "once a second a zap marks the healthiest visible enemy inside: every tower turns on it, and it is "
                + "Exposed, for " + PerkText.seconds(MARK_TICKS);
    }
}
