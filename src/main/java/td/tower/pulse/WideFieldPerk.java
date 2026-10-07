package td.tower.pulse;

import td.util.TickRate;

/** Wide Field: Toll lasts a second longer after an enemy leaves. */
public final class WideFieldPerk implements PulsePerk {

    private static final int LINGER_TICKS = Math.round(1f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withToll(spec.toll().lingeringLonger(LINGER_TICKS));
    }
}
