package td.tower.pulse;

import td.effect.DragStrength;
import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** Undertow: the drag deepens with Toll, Anchors, and counts as a chill for a freeze. */
public final class UndertowPerk implements PulsePerk {

    private static final int DRAG_AT_FULL = 30;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withHold(spec.hold().deepeningTo(DRAG_AT_FULL)).withModes(spec.modes().with(FieldMode.UNDERTOW));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.ANCHORED, EffectKind.UNDERTOW);
    }

    @Override
    public TermText describe() {
        return TermText.of("the ").plus(EffectKind.DRAGGED).plus(" grows with ").plus(EffectKind.TOLL)
                .plus(" to " + DragStrength.at(DRAG_AT_FULL, FieldStrength.FULL) + "% at full ").plus(EffectKind.TOLL)
                .plus("; enemies inside gain ").plus(EffectKind.ANCHORED).plus(" and ").plus(EffectKind.UNDERTOW)
                .plus(", and keep them " + TermNumbers.seconds(FieldMode.LINGER_TICKS / (float) TickRate.TICKS_PER_SECOND) + " after they leave");
    }
}
