package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/** Gravity: the hard stop, a Daze for an enemy a beat inside, once a visit. */
public final class GravityMechanic implements PulseMechanic {

    private static final int AFTER_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final int DAZE_TICKS = Math.round(0.75f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withGravity(new GravitySpec(AFTER_TICKS, DAZE_TICKS)).withLook(StoneLook.GRAVITY);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy " + TermNumbers.seconds(AFTER_TICKS / TickRate.TICKS_PER_SECOND)
                + " inside the field gains ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(DAZE_TICKS / TickRate.TICKS_PER_SECOND) + ", once a visit");
    }
}
