package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Shatter: as a freeze it landed ends, the ice bursts on every other enemy close by. */
public final class ShatterMechanic implements SeekerMechanic {

    public static final float RADIUS_CELLS = 1f;
    /** The share of the missile's damage the burst deals. */
    public static final float SHARE = 0.35f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withIce(IceRider.SHATTER);
    }

    @Override
    public TermText describe() {
        return TermText.of("when a ").plus(EffectKind.FREEZE).plus(" it landed ends, the ice bursts in a ")
                .plus(TowerTerm.SHATTER).plus(": every other enemy within " + TermNumbers.cells(RADIUS_CELLS)
                        + " takes " + TermNumbers.percent(SHARE) + " of the missile's damage");
    }
}
