package td.enemy;

import td.damage.DamageType;
import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import java.util.Optional;

/**
 * Plating: every hit loses a flat number of points, never below zero. {@code restrictedTo}, when present,
 * limits it to one damage type; otherwise it plates both.
 */
public record FlatResistTrait(float points, Optional<DamageType> restrictedTo) implements Trait {

    public FlatResistTrait(float points) {
        this(points, Optional.empty());
    }

    public static FlatResistTrait physicalOnly(float points) {
        return new FlatResistTrait(points, Optional.of(DamageType.PHYSICAL));
    }

    public static FlatResistTrait magicOnly(float points) {
        return new FlatResistTrait(points, Optional.of(DamageType.MAGIC));
    }

    @Override
    public StatModifiers modifiers(TraitContext context) {
        StatModifier plating = StatModifier.flat(DamageUnits.ofPoints(this.points));
        return this.restrictedTo
                .map(type -> StatModifiers.of(EnemyStat.platingFor(type), plating))
                .orElseGet(() -> StatModifiers.of(EnemyStat.PHYSICAL_PLATING, plating)
                        .and(EnemyStat.MAGIC_PLATING, plating));
    }

    @Override
    public TraitLine describe() {
        String label = this.restrictedTo.map(type -> "Plating, " + PercentResistTrait.damageName(type)).orElse("Plating");
        return new TraitLine(this.marker(), label, "-" + TraitText.decimal(this.points) + "/hit");
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.FLAT_RESIST;
    }
}
