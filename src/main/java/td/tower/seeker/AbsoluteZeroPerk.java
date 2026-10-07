package td.tower.seeker;

/**
 * Absolute Zero: a missile freezes everything within a cell of its impact, shatters hit twice as
 * hard, and an enemy the Seeker froze shatters when its freeze ends.
 */
public final class AbsoluteZeroPerk implements SeekerPerk {

    private static final float AREA_CELLS = 1f;
    private static final float SHATTER_FACTOR = 2f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().withArea(AREA_CELLS))
                .withShatter(spec.shatter().harderBy(SHATTER_FACTOR).alsoOnThaw());
    }
}
