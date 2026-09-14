package td.enemy;

import td.damage.Damage;

/**
 * Absorbs a level-scaled fraction of every incoming hit - the migrated Square's resistance.
 * The surviving fraction is {@code baseFraction - perLevelReduction * level}, which {@link Damage}'s
 * own zero-clamp turns into full immunity, never a healing hit, once it reaches zero or below.
 */
public record PercentResistTrait(float baseFraction, float perLevelReduction) implements Trait {

    @Override
    public Damage onHit(Damage incoming, TraitContext context) {
        return incoming.scaledBy(this.baseFraction - this.perLevelReduction * context.level());
    }
}
