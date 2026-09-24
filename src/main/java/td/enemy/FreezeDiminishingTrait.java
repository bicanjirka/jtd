package td.enemy;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Successive freezes on this enemy wear off faster, down to immunity for a while. */
public record FreezeDiminishingTrait() implements Trait {

    private static final StatModifiers DIMINISHING = StatModifiers.of(EnemyStat.FREEZE_DR, StatModifier.flat(1f));

    @Override
    public StatModifiers modifiers(TraitContext context) {
        return DIMINISHING;
    }

    @Override
    public String describe() {
        return "Repeated freezes wear off faster";
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.FREEZE_DIMINISHING;
    }
}
