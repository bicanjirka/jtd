package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.Set;

/** Shrapnel: a Heavy shell makes everything its blast reaches Bleed, so many points a cell. */
public final class ShrapnelMechanic implements MortarMechanic {

    /** What a Bleed costs a cell walked, in points. */
    public static final float POINTS_PER_CELL = 2.8f;
    public static final int BLEED_TICKS = Math.round(4 * TickRate.TICKS_PER_SECOND);

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withHeavy(HeavyRider.SHRAPNEL);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BLEEDING);
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(TowerTerm.HEAVY_SHELL).plus(" shell applies ").plus(EffectKind.BLEEDING)
                .plus(" to every enemy its blast reaches: " + TermNumbers.number(POINTS_PER_CELL)
                        + " damage a cell for "
                        + TermNumbers.seconds(BLEED_TICKS / TickRate.TICKS_PER_SECOND));
    }
}
