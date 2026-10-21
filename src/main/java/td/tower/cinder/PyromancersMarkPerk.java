package td.tower.cinder;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Pyromancer's Mark: burning enemies take 40% more magic damage from every source. */
public final class PyromancersMarkPerk implements CinderPerk {

    private static final float MAGIC_DAMAGE_TAKEN = 0.4f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().withCarried(StatModifiers.of(EnemyStat.MAGIC_DAMAGE_TAKEN, StatModifier.flat(MAGIC_DAMAGE_TAKEN))));
    }

    @Override
    public TermText describe() {
        return TermText.of("burning enemies take +" + PerkText.percent(MAGIC_DAMAGE_TAKEN) + " magic damage from every source");
    }
}
