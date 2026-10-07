package td.tower.pulse;

/** Null Field: every enemy inside the field is Silenced. */
public final class NullFieldPerk implements PulsePerk {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withModes(spec.modes().silencing());
    }
}
