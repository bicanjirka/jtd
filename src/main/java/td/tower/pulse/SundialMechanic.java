package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

/** The Sundial: the stone's shadow turns once around it now and then, hitting every enemy it passes. */
public final class SundialMechanic implements PulseMechanic {

    private static final int PERIOD_TICKS = Math.round(3 * TickRate.TICKS_PER_SECOND);
    private static final int TURN_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final float REACH_CELLS = 4f;
    private static final float DAMAGE_POINTS = 20f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSundial(new SundialSpec(PERIOD_TICKS, TURN_TICKS, REACH_CELLS, DAMAGE_POINTS, 1f, false));
    }

    @Override
    public TermText describe() {
        return TermText.of("every " + TermNumbers.seconds(PERIOD_TICKS / TickRate.TICKS_PER_SECOND) + " the ")
                .plus(TowerTerm.SUNDIAL).plus(" turns once around the stone, out to "
                        + TermNumbers.cells(REACH_CELLS) + ": every enemy it passes takes an aimed hit of "
                        + TermNumbers.whole(DAMAGE_POINTS) + " magic damage and a stack of ").plus(EffectKind.TOLL);
    }
}
