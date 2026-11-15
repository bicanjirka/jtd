package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** Burden: the steady weight, a drag that may go deeper. */
public final class BurdenMechanic implements PulseMechanic {

    private static final float DRAG_CAP = 0.75f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withDragCap(DRAG_CAP).withLook(StoneLook.BURDEN);
    }

    @Override
    public TermText describe() {
        return TermText.of("the field's ").plus(EffectKind.DRAGGED).plus(" may reach " + TermNumbers.percent(DRAG_CAP)
                + ", from " + TermNumbers.percent(PulseSpec.DRAG_CAP));
    }
}
