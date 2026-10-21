package td.tower.cinder;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

/** Heat: burning enemies take 20% more damage over time from every source. */
public final class HeatMechanic implements CinderMechanic {

    private static final float PERIODIC_DAMAGE_TAKEN = 0.2f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.PERIODIC_DAMAGE_TAKEN, StatModifier.flat(PERIODIC_DAMAGE_TAKEN))));
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy with ").plus(EffectKind.BURN)
                .plus(" takes " + TermNumbers.signedPercent(PERIODIC_DAMAGE_TAKEN) + " damage over time from every source");
    }
}
