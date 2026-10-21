package td.tower.pulse;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Event Horizon: each death inside widens the field by a tenth of its range until the wave ends. */
public final class EventHorizonMechanic implements PulseMechanic {

    private static final float PER_DEATH = 0.1f;
    private static final float MAX = 1f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withField(spec.field().withPerDeathGrowth(PER_DEATH, MAX));
    }

    @Override
    public TermText describe() {
        return TermText.of("each death inside adds a stack of ").plus(TowerTerm.EVENT_HORIZON)
                .plus(": the field widens by " + TermNumbers.percent(PER_DEATH)
                        + " of its range until the wave ends, up to " + TermNumbers.times(1f + MAX) + " its range");
    }
}
