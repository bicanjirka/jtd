package td.tower.pulse;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

/** Swift Shadow: the shadow turns more often. */
public final class SwiftShadowMechanic implements PulseMechanic {

    private static final int PERIOD_TICKS = Math.round(2 * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSundial(spec.sundial().withPeriodTicks(PERIOD_TICKS));
    }

    @Override
    public TermText describe() {
        return TermText.of("the ").plus(TowerTerm.SUNDIAL)
                .plus(" turns every " + TermNumbers.seconds(PERIOD_TICKS / TickRate.TICKS_PER_SECOND));
    }
}
