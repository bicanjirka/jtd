package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.Set;

/** Shrapnel: a Heavy shell makes everything its blast reaches Bleed, a share of the shell's damage a cell. */
public final class ShrapnelMechanic implements MortarMechanic {

    /** What a Bleed costs a cell walked, a share of the Heavy shell's damage */
    public static final float SHARE_PER_CELL = 0.18f;
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
                .plus(" to every enemy its blast reaches: " + TermNumbers.percent(SHARE_PER_CELL)
                        + " of its damage a cell for "
                        + TermNumbers.seconds(BLEED_TICKS / TickRate.TICKS_PER_SECOND));
    }
}
