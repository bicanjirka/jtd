package td.tower.pulse;

import td.util.TickRate;

/** A node that makes Toll last longer after an enemy leaves the field. */
public final class LingeringTollPerk implements PulsePerk {

    private final int ticks;

    /** Toll lasting {@code seconds} longer. */
    public LingeringTollPerk(float seconds) {
        this.ticks = Math.round(seconds * TickRate.TICKS_PER_SECOND);
    }

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withToll(spec.toll().lingeringLonger(this.ticks));
    }
}
