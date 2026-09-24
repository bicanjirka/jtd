package td.enemy;

/**
 * Speeds up as health drops, from base speed at full health to {@code maxMultiplier} near death.
 * Recomputed from the health fraction on each hit, so it never drifts.
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
