package td.enemy;

/**
 * Accelerates as the mob takes damage, from its intrinsic base speed at full health up to a
 * fixed maximum as it nears death - the migrated Triangle's hurt curve. {@link #speedFactor}
 * returns the multiplier on intrinsic speed for the current health fraction, recomputed fresh
 * on every hit rather than accumulated, so it can never drift. A higher top speed is a different
 * concrete instance authored at a different {@link Rank}, not a formula scaled by anything live
 * on the mob.
 */
public record HurtSpeedTrait(float maxMultiplier) implements Trait {

    @Override
    public float speedFactor(TraitContext context) {
        return 1f + (this.maxMultiplier - 1f) * (1f - context.healthFraction());
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.HURT_SPEED;
    }
}
