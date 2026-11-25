package td.tower.mortar;

import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** A Payload: adds its kind of shell to the magazine, which leaves its ground where it lands. */
public final class PayloadMechanic implements MortarMechanic {

    public static final int WEIGHT = 3;

    private final ShellKind kind;

    public PayloadMechanic(ShellKind kind) {
        if (!kind.isPayload()) {
            throw new IllegalArgumentException(kind + " is no payload");
        }
        this.kind = kind;
    }

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withMagazine(spec.magazine().withWeight(this.kind, WEIGHT));
    }

    @Override
    public Set<Term> applies() {
        return Set.copyOf(this.kind.applies());
    }

    @Override
    public TermText describe() {
        return TermText.of("adds ").plus(this.kind.mention()).plus(" shells to what it fires; each deals ")
                .plus(this.kind.damageText() + " and ")
                .plus(this.kind.groundText());
    }
}
