package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Attune's Bracketing: a shell landing near the last one hits harder and 10% wider, up to three steps. */
public final class BracketingPerk implements MortarPerk {

    private static final float RADIUS_CELLS = 1.5f;
    private static final int MAX_STEPS = 3;
    private static final float STEP = 0.1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBracket(BracketSpec.of(RADIUS_CELLS, MAX_STEPS, STEP));
    }

    @Override
    public TermText describe() {
        return TermText.of("a shell landing within " + TermNumbers.cells(RADIUS_CELLS) + " of the last raises ")
                .plus(TermText.of(TowerTerm.BRACKETING, TermText.of("each step: more damage and "
                        + TermNumbers.signedPercent(STEP) + " blast radius")))
                .plus(" a step, up to " + MAX_STEPS + "; a shell elsewhere clears it");
    }
}
