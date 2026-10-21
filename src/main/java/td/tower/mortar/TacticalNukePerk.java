package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Tactical Nuke: every fourth shell is a nuke: four times the damage over one and a half times the blast. */
public final class TacticalNukePerk implements MortarPerk {

    private static final int EVERY = 4;
    private static final float DAMAGE_FACTOR = 4f;
    private static final float RADIUS_FACTOR = 1.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withNuke(NukeSpec.of(EVERY, DAMAGE_FACTOR, RADIUS_FACTOR))
                .withShells(spec.shells().withNukeEvery(EVERY));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.FALLOUT);
    }

    @Override
    public TermText describe() {
        return TermText.of("every " + TermNumbers.ordinal(EVERY) + " shell is a ")
                .plus(TermText.of(TowerTerm.NUKE_SHELL, ShellType.NUKE.describe()))
                .plus(": " + TermNumbers.times(DAMAGE_FACTOR) + " damage over " + TermNumbers.times(RADIUS_FACTOR)
                        + " the blast; it is never a special shell");
    }
}
