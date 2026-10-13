package td.tower.cinder;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.tower.upgrade.PerkText;

/** Cauterize: burning enemies receive half the healing and shielding. */
public final class CauterizePerk implements CinderPerk {

    private static final float HEALING_RECEIVED = -0.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.HEALING_RECEIVED, StatModifier.flat(HEALING_RECEIVED))));
    }

    @Override
    public String describe() {
        return "burning enemies receive " + PerkText.percent(-HEALING_RECEIVED) + " less healing and shielding";
    }
}
