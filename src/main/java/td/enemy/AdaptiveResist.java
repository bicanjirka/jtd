package td.enemy;

import td.damage.DamageMix;
import td.damage.DamageType;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Locale;
import java.util.Optional;

/**
 * Armor chosen once per mob at spawn from how far the level's damage mix leans toward the type it
 * resists: {@code evenArmor} at an even split or before any damage, {@code fullArmor} when every
 * landed point was that type, linear in between. A trait, not a base stat, because what it gives
 * depends on how the level has been played.
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
        String label = this.resisted.map(type -> "Adaptive " + Adapted.labelFor(type).toLowerCase(Locale.ROOT))
                .orElse("Adaptive resist");
        return new TraitLine(TraitMarker.PERCENT_RESIST, label, "up to +" + Math.round(strongest));
    }

    @Override
    public TraitEntry entry() {
        String most = TermNumbers.signed(Math.round(Math.max(this.evenArmor, this.fullArmor)));
        TermText text = this.resisted
                .map(type -> TermText.of("settles on up to " + most + " "
                        + Adapted.labelFor(type).toLowerCase(Locale.ROOT) + ", more the more of this level's damage was "
                        + type.name().toLowerCase(Locale.ROOT)))
                .orElseGet(() -> TermText.of("settles on up to " + most + " armor or magic resist against whichever "
                        + "damage type this level has dealt most"));
        return TraitEntry.of(TraitTerm.ADAPTATION, text);
    }

    @Override
    public Optional<Trait> resolvedFor(DamageMix mix) {
        DamageType type = this.resisted.orElseGet(mix::dominant);
        float armor = this.evenArmor + mix.lean(type) * (this.fullArmor - this.evenArmor);
        if (armor <= 0f) {
            return Optional.empty();
        }
        return Optional.of(new Adapted(armor, type));
    }

    /** What one mob's adaptive resist settled on: {@code armor} points against {@code type}. */
    public record Adapted(float armor, DamageType type) implements Trait {

        @Override
        public StatModifiers<EnemyStat> modifiers(TraitContext context) {
            return StatModifiers.of(EnemyStat.mitigationFor(this.type), StatModifier.flat(this.armor));
        }

        @Override
        public TraitLine describe() {
            return new TraitLine(this.marker(), "Adapted " + labelFor(this.type).toLowerCase(Locale.ROOT),
                    "+" + Math.round(this.armor));
        }

        @Override
        public TraitEntry entry() {
            return TraitEntry.of(TraitTerm.ADAPTATION, TermText.of("settled on "
                    + TermNumbers.signed(Math.round(this.armor)) + " " + labelFor(this.type).toLowerCase(Locale.ROOT)
                    + " against this level's damage"));
        }

        @Override
        public TraitMarker marker() {
            return markerFor(this.type);
        }

        static TraitMarker markerFor(DamageType type) {
            return switch (type) {
                case PHYSICAL -> TraitMarker.PHYSICAL_RESIST;
                case MAGIC -> TraitMarker.MAGIC_RESIST;
            };
        }

        static String labelFor(DamageType type) {
            return switch (type) {
                case PHYSICAL -> "Armor";
                case MAGIC -> "Magic resist";
            };
        }
    }
}
