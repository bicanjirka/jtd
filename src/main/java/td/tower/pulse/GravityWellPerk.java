package td.tower.pulse;

/** Gravity Well: the drag deepens toward the field's centre. */
public final class GravityWellPerk implements PulsePerk {

    private static final int CENTRE_DRAG = 50;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withHold(spec.hold().wellOf(CENTRE_DRAG));
    }

    @Override
    public String describe() {
        return "the drag deepens toward the centre, to " + CENTRE_DRAG + "% at the middle";
    }
}
