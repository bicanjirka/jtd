package td.enemy;

import td.damage.DamageType;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import java.util.Optional;

/**
 * Armor or magic resist authored as the fraction of a hit that is kept: {@code 0.5} is the armor
 * that halves a hit. {@code restrictedTo}, when present, limits it to one damage type; otherwise it
 * gives both.
 */
public record PercentResistTrait(float fraction, Optional<DamageType> restrictedTo) implements Trait {

    public PercentResistTrait(float fraction) {
        this(fraction, Optional.empty());
    }

    public static PercentResistTrait physicalOnly(float fraction) {
        return new PercentResistTrait(fraction, Optional.of(DamageType.PHYSICAL));
    }

    public static PercentResistTrait magicOnly(float fraction) {
        return new PercentResistTrait(fraction, Optional.of(DamageType.MAGIC));
    }

    /** The armor whose {@code 100 / (100 + armor)} multiplier keeps {@code fraction} of a hit. */
    public static float armorKeeping(float fraction) {
        if (fraction <= 0f) {
            return Float.MAX_VALUE;
        }
        return 100f * (1f - fraction) / fraction;
    }

    @Override
    public StatModifiers modifiers(TraitContext context) {
        StatModifier armor = StatModifier.flat(armorKeeping(this.fraction));
        return this.restrictedTo
                .map(type -> StatModifiers.of(EnemyStat.mitigationFor(type), armor))
                .orElseGet(() -> StatModifiers.of(EnemyStat.ARMOR, armor).and(EnemyStat.MAGIC_RESIST, armor));
    }

    @Override
    public String describe() {
        String target = this.restrictedTo.map(PercentResistTrait::damageName).orElse("all");
        return "Resists " + Math.round((1f - this.fraction) * 100) + "% of " + target + " damage";
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
