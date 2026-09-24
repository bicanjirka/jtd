package td.enemy;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Full resilience: no critical hit ever lands on this enemy. */
public record CriticalImmunityTrait() implements Trait {

    private static final StatModifiers FULL_RESILIENCE = StatModifiers.of(EnemyStat.RESILIENCE, StatModifier.flat(100f));

    @Override
    public StatModifiers modifiers(TraitContext context) {
        return FULL_RESILIENCE;
    }

    @Override
    public TraitLine describe() {
        return TraitLine.of(this.marker(), "Crit immune");
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.CRITICAL_IMMUNE;
    }
}
