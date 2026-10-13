package td.tower.cinder;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.tower.upgrade.PerkText;

/** Heat: burning enemies take 20% more damage over time from every source. */
public final class HeatPerk implements CinderPerk {

    private static final float PERIODIC_DAMAGE_TAKEN = 0.2f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.PERIODIC_DAMAGE_TAKEN, StatModifier.flat(PERIODIC_DAMAGE_TAKEN))));
    }

    @Override
    public String describe() {
        return "burning enemies take +" + PerkText.percent(PERIODIC_DAMAGE_TAKEN) + " damage over time from every source";
    }
}
