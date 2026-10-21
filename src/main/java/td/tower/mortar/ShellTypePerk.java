package td.tower.mortar;

import td.term.Term;
import td.term.TermText;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** A special that unlocks a shell type: the Mortar fires it every third shell. */
public final class ShellTypePerk implements MortarPerk {

    private final ShellType type;

    public ShellTypePerk(ShellType type) {
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
        return TermText.of("every " + PerkText.ordinal(ShellPlan.CYCLE) + " shell is " + this.type.describe());
    }
}
