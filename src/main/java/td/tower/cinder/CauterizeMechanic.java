package td.tower.cinder;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

/** Cauterize: burning enemies lose spirit, so they get less from healing and shielding. */
public final class CauterizeMechanic implements CinderMechanic {

    private static final float SPIRIT = -50f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.SPIRIT, StatModifier.flat(SPIRIT))));
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy with ").plus(EffectKind.BURN)
                .plus(" has " + TermNumbers.number(-SPIRIT) + " less ").plus(EnemyStat.SPIRIT)
                .plus(", so it gets less from ").plus(EffectKind.HEAL).plus(" and ").plus(EffectKind.SHIELD);
    }
}
