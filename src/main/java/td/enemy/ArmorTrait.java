package td.enemy;

import td.damage.DamageType;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import java.util.Optional;

/**
 * Armor or magic resist in points: a hit is multiplied by {@code 100 / (100 + armor)}, so 25 keeps
 * four fifths of it. {@code restrictedTo}, when present, limits it to one damage type; otherwise it
 * gives both.
 */
public record ArmorTrait(float armor, Optional<DamageType> restrictedTo) implements Trait {

    public static ArmorTrait both(float armor) {
        return new ArmorTrait(armor, Optional.empty());
    }

    public static ArmorTrait physicalOnly(float armor) {
        return new ArmorTrait(armor, Optional.of(DamageType.PHYSICAL));
    }

    public static ArmorTrait magicOnly(float armor) {
        return new ArmorTrait(armor, Optional.of(DamageType.MAGIC));
    }

    @Override
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        StatModifier points = StatModifier.flat(this.armor);
        return this.restrictedTo
                .map(type -> StatModifiers.of(EnemyStat.mitigationFor(type), points))
                .orElseGet(() -> StatModifiers.of(EnemyStat.ARMOR, points).and(EnemyStat.MAGIC_RESIST, points));
    }

    @Override
    public TraitLine describe() {
        return new TraitLine(this.marker(), label(this.restrictedTo), "+" + Math.round(this.armor));
    }

    /** "Armor", "Magic resist", or both for an unrestricted trait. */
    static String label(Optional<DamageType> restrictedTo) {
        return restrictedTo.map(type -> switch (type) {
            case PHYSICAL -> "Armor";
            case MAGIC -> "Magic resist";
        }).orElse("Armor, magic resist");
    }

    static String damageName(DamageType type) {
        return switch (type) {
            case PHYSICAL -> "physical";
            case MAGIC -> "magic";
        };
    }

    @Override
    public TraitMarker marker() {
        return this.restrictedTo.map(type -> switch (type) {
            case PHYSICAL -> TraitMarker.PHYSICAL_RESIST;
            case MAGIC -> TraitMarker.MAGIC_RESIST;
        }).orElse(TraitMarker.PERCENT_RESIST);
    }
}
