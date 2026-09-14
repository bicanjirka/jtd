package td.enemy;

/**
 * Accelerates as the mob takes damage, from its intrinsic base speed at full health up to a
 * level-scaled maximum as it nears death - the migrated Triangle's hurt curve. The maximum is
 * {@code speedBase * (multiplierBase + multiplierPerLevel * level)}; {@link #speedFactor}
 * returns the multiplier on intrinsic speed for the current health fraction, recomputed fresh
 * on every hit rather than accumulated, so it can never drift.
 */
public record HurtSpeedTrait(float multiplierBase, float multiplierPerLevel) implements Trait {

    @Override
    public float speedFactor(TraitContext context) {
        float maxMultiplier = this.multiplierBase + this.multiplierPerLevel * context.level();
        return 1f + (maxMultiplier - 1f) * (1f - context.healthFraction());
    }
}
