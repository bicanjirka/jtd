package td.tower.pulse;

/** A node that adds a rule to the field: Null Field, Dead Zone, Corrosion and the rest. */
public final class ModePerk implements PulsePerk {

    private final FieldMode mode;

    public ModePerk(FieldMode mode) {
        this.mode = mode;
    }

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withModes(spec.modes().with(this.mode));
    }
}
