package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Absolute Zero: a missile freezes everything within a cell of its impact, shatters hit twice as
 * hard, and an enemy the Seeker froze shatters when its freeze ends.
 */
public final class AbsoluteZeroMechanic implements SeekerMechanic {

    private static final float AREA_CELLS = 1f;
    private static final float SHATTER_FACTOR = 2f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().withArea(AREA_CELLS))
                .withShatter(spec.shatter().harderBy(SHATTER_FACTOR).alsoOnThaw());
    }

    @Override
    public TermText describe() {
        return TermText.of("the missile applies ").plus(EffectKind.FREEZE)
                .plus(" to everything within " + TermNumbers.cells(AREA_CELLS) + " of the impact; a ")
                .plus(TowerTerm.SHATTER).plus(" deals " + TermNumbers.times(SHATTER_FACTOR) + "; an enemy with this tower's ")
                .plus(EffectKind.FREEZE).plus(" breaks in a ").plus(TowerTerm.SHATTER).plus(" when it ends");
    }
}
