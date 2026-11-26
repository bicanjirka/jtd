package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerTerm;

/** Shatter: as a freeze it landed ends, the ice bursts on every other enemy close by. */
public final class ShatterMechanic implements SeekerMechanic {

    public static final float RADIUS_CELLS = 1f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withIce(IceRider.SHATTER);
    }

    @Override
    public TermText describe() {
        return TermText.of("when a ").plus(EffectKind.FREEZE).plus(" it landed ends, the ice bursts in a ")
                .plus(TowerTerm.SHATTER).plus(": every other enemy within " + TermNumbers.cells(RADIUS_CELLS)
                        + " takes its ").plus(TowerStat.SHATTER_DAMAGE).plus(" as magic damage, however hard the missile hit");
    }
}
