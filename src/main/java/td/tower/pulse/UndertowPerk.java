package td.tower.pulse;

import td.effect.DragStrength;
import td.effect.FieldStrength;
import td.tower.upgrade.PerkText;

/** Undertow: the drag deepens with Toll, Anchors, and counts as a chill for a freeze. */
public final class UndertowPerk implements PulsePerk {

    private static final int DRAG_AT_FULL = 30;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withHold(spec.hold().deepeningTo(DRAG_AT_FULL)).withModes(spec.modes().with(FieldMode.UNDERTOW));
    }

    @Override
    public String describe() {
        return "the drag grows with Toll to " + DragStrength.at(DRAG_AT_FULL, FieldStrength.FULL)
                + "% at full Toll; enemies inside are Anchored, and the drag counts as a chill for a freeze, for "
                + PerkText.seconds(FieldMode.LINGER_TICKS) + " after they leave too";
    }
}
