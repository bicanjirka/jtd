package td.enemy;

import td.damage.DamageType;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import java.util.Optional;

/**
 * Plating: every hit loses a flat amount, never below zero. {@code restrictedTo}, when present,
 * limits it to one damage type; otherwise it plates both.
 */
public record FlatResistTrait(int flatReduction, Optional<DamageType> restrictedTo) implements Trait {

    public FlatResistTrait(int flatReduction) {
        this(flatReduction, Optional.empty());
    }

    public static FlatResistTrait physicalOnly(int flatReduction) {
        return new FlatResistTrait(flatReduction, Optional.of(DamageType.PHYSICAL));
    }

    public static FlatResistTrait magicOnly(int flatReduction) {
        return new FlatResistTrait(flatReduction, Optional.of(DamageType.MAGIC));
    }

    @Override
    public StatModifiers modifiers(TraitContext context) {
        StatModifier plating = StatModifier.flat(this.flatReduction);
        return this.restrictedTo
                .map(type -> StatModifiers.of(EnemyStat.platingFor(type), plating))
                .orElseGet(() -> StatModifiers.of(EnemyStat.PHYSICAL_PLATING, plating)
                        .and(EnemyStat.MAGIC_PLATING, plating));
    }

    @Override
    public String describe() {
        String target = this.restrictedTo.map(PercentResistTrait::damageName).orElse("every");
        return "Plating: shrugs off " + TraitText.points(this.flatReduction) + " of " + target + " hit";
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.FLAT_RESIST;
    }
}
