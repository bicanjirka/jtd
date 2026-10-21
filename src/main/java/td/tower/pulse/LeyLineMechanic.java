package td.tower.pulse;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Ley Line: the field casts a beam to another field, and an enemy on it gets both fields' rules. */
public final class LeyLineMechanic implements PulseMechanic {

    private static final float REACH_CELLS = 5f;
    private static final float BAND_CELLS = 0.3f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withLine(new LineSpec(REACH_CELLS, BAND_CELLS));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(TowerTerm.LEY_LINE);
    }

    @Override
    public TermText describe() {
        return TermText.of("casts a ").plus(TowerTerm.LEY_LINE)
                .plus(" to another Obelisk within " + TermNumbers.cells(REACH_CELLS)
                        + ", the one whose line crosses the path most; an enemy on it gets both fields' rules at the ")
                .plus(EffectKind.TOLL).plus(" it carries, but no damage and no ").plus(EffectKind.TOLL);
    }
}
