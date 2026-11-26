package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.Set;

/** Concussion: a Heavy shell Dazes everything its blast reaches, longer in its full-damage zone than at its edge. */
public final class ConcussionMechanic implements MortarMechanic {

    public static final int CORE_DAZE_TICKS = Math.round(TickRate.TICKS_PER_SECOND * 1f);
    public static final int EDGE_DAZE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withHeavy(HeavyRider.CONCUSSION);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED);
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(TowerTerm.HEAVY_SHELL).plus(" shell applies ").plus(EffectKind.DAZED)
                .plus(" to every enemy its blast reaches: " + seconds(CORE_DAZE_TICKS) + " at full damage, "
                        + seconds(EDGE_DAZE_TICKS) + " at its edge");
    }

    private static String seconds(int ticks) {
        return TermNumbers.seconds(ticks / TickRate.TICKS_PER_SECOND);
    }
}
