package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** Heavy Shell: what the shell lands within half a cell of is Dazed. */
public final class HeavyShellPerk implements MortarPerk {

    private static final int DAZE_TICKS = 10;
    private static final float DAZE_RADIUS_CELLS = 0.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withDaze(DazeSpec.of(DAZE_TICKS, DAZE_RADIUS_CELLS));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED);
    }

    @Override
    public TermText describe() {
        return TermText.of("enemies within " + PerkText.cells(DAZE_RADIUS_CELLS) + " cells of the impact are Dazed " + PerkText.seconds(DAZE_TICKS));
    }
}
