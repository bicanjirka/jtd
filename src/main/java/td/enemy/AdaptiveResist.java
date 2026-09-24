package td.enemy;

import td.damage.DamageMix;

import java.util.Optional;

/**
 * Armor that resists whichever damage type has dominated the level so far, chosen once per mob at
 * spawn. It punishes a one-type defence and does nothing against an even one.
 *
 * @param fullFraction the percentage-resist fraction when every landed point was one type
 */
public record AdaptiveResist(float fullFraction) implements TraitTemplate {

    /**
     * The resist this mob spawns with: restricted to the dominant type and scaled linearly from no
     * effect at an even split to {@code fullFraction} at a one-sided mix; empty when nothing
     * dominates.
     */
    @Override
    public Optional<Trait> resolvedFor(DamageMix mix) {
        float dominance = mix.dominance();
        if (dominance == 0f) {
            return Optional.empty();
        }
        float fraction = 1f - dominance * (1f - this.fullFraction);
        return Optional.of(new PercentResistTrait(fraction, Optional.of(mix.dominant())));
    }
}
