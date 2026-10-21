package td.tower.pulse;

import td.effect.DragStrength;
import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.term.Term;
import td.term.TermText;
import td.tower.upgrade.PerkText;

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
        return TermText.of("the drag grows with Toll to " + DragStrength.at(DRAG_AT_FULL, FieldStrength.FULL)
                + "% at full Toll; enemies inside are Anchored, and the drag counts as a chill for a freeze, for "
                + PerkText.seconds(FieldMode.LINGER_TICKS) + " after they leave too");
    }
}
