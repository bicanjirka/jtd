package td.tower.seeker;

import td.tower.upgrade.PerkText;

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

    @Override
    public String describe() {
        return "the missile freezes everything within " + PerkText.cells(AREA_CELLS) + (AREA_CELLS == 1f ? " cell" : " cells")
                + " of the impact; shatters deal " + PerkText.times(SHATTER_FACTOR)
                + "; an enemy this tower froze shatters when its freeze ends";
    }
}
