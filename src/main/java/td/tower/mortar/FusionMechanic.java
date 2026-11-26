package td.tower.mortar;

import td.damage.DamageUnits;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerStats;
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
        return this.fuses("");
    }

    @Override
    public TermText describe(TowerStats stats) {
        int nuke = ShellKind.NUKE.damage(stats.damage(), stats.value(TowerStat.NORMAL_DAMAGE),
                stats.value(TowerStat.HEAVY_DAMAGE));
        return this.fuses(" that hits for " + TermNumbers.number(DamageUnits.inPoints(nuke)) + " damage");
    }

    /** The sentence, with {@code hit} said right after the Nuke it makes. */
    private TermText fuses(String hit) {
        return TermText.of("a ").plus(TowerTerm.HEAVY_SHELL).plus(" shell drawn right behind a ")
                .plus(TowerTerm.HEAVY_SHELL).plus(" fuses with it into a ").plus(TowerTerm.NUKE_SHELL)
                .plus(hit + ", which ").plus(ShellKind.NUKE.groundText())
                .plus(" and whose flash gives every enemy on the board ").plus(ShellKind.NUKE.flashText())
                .plus("; Normals come up less often");
    }
}
