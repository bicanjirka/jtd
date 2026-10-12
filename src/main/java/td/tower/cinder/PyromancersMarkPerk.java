package td.tower.cinder;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Pyromancer's Mark: burning enemies take 15% more magic damage from every source. */
public final class PyromancersMarkPerk implements CinderPerk {

    private static final float MAGIC_DAMAGE_TAKEN = 0.15f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.MAGIC_DAMAGE_TAKEN, StatModifier.flat(MAGIC_DAMAGE_TAKEN))));
    }
}
