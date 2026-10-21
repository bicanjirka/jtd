package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermText;

/** Gravity Well: the drag deepens toward the field's centre. */
public final class GravityWellMechanic implements PulseMechanic {

    private static final int CENTRE_DRAG = 50;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withHold(spec.hold().wellOf(CENTRE_DRAG));
    }

    @Override
    public TermText describe() {
        return TermText.of("the ").plus(EffectKind.DRAGGED)
                .plus(" deepens toward the centre, to " + CENTRE_DRAG + "% at the middle");
    }
}
