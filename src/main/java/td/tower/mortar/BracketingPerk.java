package td.tower.mortar;

/** Attune's Bracketing: a shell landing near the last one hits 10% harder and wider, up to three steps. */
public final class BracketingPerk implements MortarPerk {

    private static final float RADIUS_CELLS = 1.5f;
    private static final int MAX_STEPS = 3;
    private static final float STEP = 0.1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBracket(BracketSpec.of(RADIUS_CELLS, MAX_STEPS, STEP, STEP));
    }
}
