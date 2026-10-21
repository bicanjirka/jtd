package td.tower.pulse;

import td.term.Term;

import java.util.Set;

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

    @Override
    public Set<Term> applies() {
        return Set.copyOf(this.mode.applies());
    }

    @Override
    public String describe() {
        return this.mode.describe();
    }
}
