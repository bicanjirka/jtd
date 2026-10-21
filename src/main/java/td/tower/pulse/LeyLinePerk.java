package td.tower.pulse;

import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** Ley Line: the field casts a beam to another field, and an enemy on it gets both fields' rules. */
public final class LeyLinePerk implements PulsePerk {

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
        return TermText.of("casts a beam to another Obelisk within " + PerkText.cells(REACH_CELLS) + " cells, the one whose beam "
                + "crosses the path most; an enemy on it gets both fields' rules at the Toll it carries, but no damage "
                + "and no Toll");
    }
}
