package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

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
        return TermText.of("enemies within " + TermNumbers.cells(DAZE_RADIUS_CELLS) + " of the impact gain ")
                .plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(DAZE_TICKS / (float) TickRate.TICKS_PER_SECOND));
    }
}
