package td.tower.mortar;

import td.term.TermText;
import td.tower.upgrade.PerkText;

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
        return TermText.of("Bracketing: a shell landing within " + PerkText.cells(RADIUS_CELLS) + " cells of the last gets more damage and +"
                + PerkText.percent(STEP) + " radius, up to " + MAX_STEPS + " steps; a shell elsewhere resets it");
    }
}
