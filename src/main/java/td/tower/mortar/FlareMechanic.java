package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Flare: adds Flare shells to the magazine, Normals that leave a flare; a Mortar keeps only its newest few burning. */
public final class FlareMechanic implements MortarMechanic {

    /** Flares one Mortar keeps burning; a new one puts out the oldest. */
    public static final int FLARES_KEPT = 3;

    private static final int WEIGHT = 1;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withMagazine(spec.magazine().withWeight(ShellKind.FLARE, WEIGHT));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.REVEALED);
    }

    @Override
    public TermText describe() {
        return TermText.of("adds ").plus(TowerTerm.FLARE).plus(" shells to what it fires; each blasts as a Normal shell and ").plus(ShellKind.FLARE.groundText()).plus(", and only its newest " + FLARES_KEPT + " stay");
    }
}
