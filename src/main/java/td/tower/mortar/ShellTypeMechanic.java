package td.tower.mortar;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Set;

/** A special that unlocks a shell type: the Mortar fires it every third shell. */
public final class ShellTypeMechanic implements MortarMechanic {

    private final ShellType type;

    public ShellTypeMechanic(ShellType type) {
        this.type = type;
    }

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShells(spec.shells().withSpecial(this.type));
    }

    @Override
    public Set<Term> applies() {
        return Set.copyOf(this.type.applies());
    }

    @Override
    public TermText describe() {
        return TermText.of("every " + TermNumbers.ordinal(ShellPlan.CYCLE) + " shell is a ")
                .plus(TermText.of(this.type.term(), this.type.describe()));
    }
}
