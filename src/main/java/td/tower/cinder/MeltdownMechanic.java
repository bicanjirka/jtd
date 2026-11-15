package td.tower.cinder;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.term.TermNumbers;
import td.term.TermText;

/** Meltdown: a burning enemy's armor and plating burn it, each point adding to the fuel it takes. */
public final class MeltdownMechanic implements CinderMechanic {

    private static final float PER_POINT = 0.006f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withMeltdown(PER_POINT);
    }

    @Override
    public TermText describe() {
        return TermText.of("each point of an enemy's ").plus(EnemyStat.ARMOR).plus(" and ").plus(EnemyStat.PLATING)
                .plus(" adds " + TermNumbers.percent(PER_POINT) + " to the ").plus(EffectKind.BURN).plus(" it pours");
    }
}
