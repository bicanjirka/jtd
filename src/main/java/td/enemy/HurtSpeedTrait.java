package td.enemy;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/**
 * Speeds up as health drops, from base speed at full health to {@code maxMultiplier} near death.
 * Derived from the health fraction whenever the stats resolve, so it never drifts.
 */
public record HurtSpeedTrait(float maxMultiplier) implements Trait {

    @Override
    public StatModifiers modifiers(TraitContext context) {
        float factor = 1f + (this.maxMultiplier - 1f) * (1f - context.healthFraction());
        return StatModifiers.of(EnemyStat.MOVE_SPEED, StatModifier.times(factor));
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.HURT_SPEED;
    }
}
