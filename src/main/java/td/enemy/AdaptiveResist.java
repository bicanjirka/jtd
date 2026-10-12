package td.enemy;

import td.damage.DamageMix;
import td.damage.DamageType;

import java.util.Optional;

/**
 * Armor chosen once per mob at spawn from how far the level's damage mix leans toward the type it
 * resists: {@code evenArmor} at an even split or before any damage, {@code fullArmor} when every
 * landed point was that type, linear in between.
 *
 * @param resisted  the type it resists; empty to resist whichever type dominates the mix
 * @param evenArmor the armor points at no lean
 * @param fullArmor the armor points at a one-sided mix
 */
public record AdaptiveResist(Optional<DamageType> resisted, float evenArmor, float fullArmor)
        implements TraitTemplate {

    /** Punishes a one-type defence and does nothing against an even one. */
    public static AdaptiveResist againstDominant(float fullArmor) {
        return new AdaptiveResist(Optional.empty(), 0f, fullArmor);
    }

    /** Always resists {@code type}, harder the more of the landed damage was {@code type}. */
    public static AdaptiveResist against(DamageType type, float evenArmor, float fullArmor) {
        return new AdaptiveResist(Optional.of(type), evenArmor, fullArmor);
    }

    /** The range the player can expect: the preview cannot know this level's damage mix. */
    @Override
    public TraitLine describe() {
        float strongest = Math.max(this.evenArmor, this.fullArmor);
        TraitMarker marker = this.resisted.map(type -> switch (type) {
            case PHYSICAL -> TraitMarker.PHYSICAL_RESIST;
            case MAGIC -> TraitMarker.MAGIC_RESIST;
        }).orElse(TraitMarker.PERCENT_RESIST);
        String label = this.resisted.map(type -> "Adaptive " + ArmorTrait.label(Optional.of(type)).toLowerCase())
                .orElse("Adaptive resist");
        return new TraitLine(marker, label, "up to +" + Math.round(strongest));
    }

    @Override
    public Optional<Trait> resolvedFor(DamageMix mix) {
        DamageType type = this.resisted.orElseGet(mix::dominant);
        float armor = this.evenArmor + mix.lean(type) * (this.fullArmor - this.evenArmor);
        if (armor <= 0f) {
            return Optional.empty();
        }
        return Optional.of(new ArmorTrait(armor, Optional.of(type)));
    }
}
