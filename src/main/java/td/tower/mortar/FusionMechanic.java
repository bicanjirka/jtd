package td.tower.mortar;

import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Fusion: a Heavy drawn right behind a Heavy fuses with it into a Nuke. */
public final class FusionMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withMagazine(spec.magazine().fusing().withWeight(ShellKind.NORMAL, -1));
    }

    @Override
    public Set<Term> applies() {
        return Set.copyOf(ShellKind.NUKE.applies());
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(TowerTerm.HEAVY_SHELL).plus(" shell drawn right behind a ")
                .plus(TowerTerm.HEAVY_SHELL).plus(" fuses with it into a ").plus(TowerTerm.NUKE_SHELL)
                .plus(", which ").plus(ShellKind.NUKE.groundText())
                .plus("; Normals come up less often");
    }
}
