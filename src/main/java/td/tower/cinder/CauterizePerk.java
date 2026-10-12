package td.tower.cinder;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Cauterize: burning enemies receive half the healing and shielding. */
public final class CauterizePerk implements CinderPerk {

    private static final float HEALING_RECEIVED = -0.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.HEALING_RECEIVED, StatModifier.flat(HEALING_RECEIVED))));
    }
}
