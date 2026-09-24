package td.enemy;

import td.damage.DamageMix;
import td.damage.DamageType;

import java.util.Optional;

/**
 * Armor chosen once per mob at spawn from how far the level's damage mix leans toward the type it
 * resists: {@code evenFraction} at an even split or before any damage, {@code fullFraction} when
 * every landed point was that type, linear in between.
 *
 * @param resisted     the type it resists; empty to resist whichever type dominates the mix
 * @param evenFraction the percentage-resist fraction at no lean; {@code 1} is no resist at all
 * @param fullFraction the percentage-resist fraction at a one-sided mix
 */
public record AdaptiveResist(Optional<DamageType> resisted, float evenFraction, float fullFraction)
        implements TraitTemplate {

    /** Punishes a one-type defence and does nothing against an even one. */
    public static AdaptiveResist againstDominant(float fullFraction) {
        return new AdaptiveResist(Optional.empty(), 1f, fullFraction);
    }

    /** Always resists {@code type}, harder the more of the landed damage was {@code type}. */
    public static AdaptiveResist against(DamageType type, float evenFraction, float fullFraction) {
        return new AdaptiveResist(Optional.of(type), evenFraction, fullFraction);
    }

    @Override
    public Optional<Trait> resolvedFor(DamageMix mix) {
        DamageType type = this.resisted.orElseGet(mix::dominant);
        float fraction = this.evenFraction + mix.lean(type) * (this.fullFraction - this.evenFraction);
        if (fraction >= 1f) {
            return Optional.empty();
        }
        return Optional.of(new PercentResistTrait(fraction, Optional.of(type)));
    }
}
