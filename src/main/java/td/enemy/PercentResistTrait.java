package td.enemy;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.Optional;

/**
 * Absorbs a fixed fraction of every hit; damage's zero clamp keeps it from healing.
 * {@code restrictedTo}, when present, limits it to one damage type.
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

    @Override
    public Damage onHit(Damage incoming, TraitContext context) {
        if (this.restrictedTo.isPresent() && this.restrictedTo.get() != incoming.type()) {
            return incoming;
        }
        return incoming.scaledBy(this.fraction);
    }

    @Override
    public TraitMarker marker() {
        return this.restrictedTo.map(type -> switch (type) {
            case PHYSICAL -> TraitMarker.PHYSICAL_RESIST;
            case MAGIC -> TraitMarker.MAGIC_RESIST;
        }).orElse(TraitMarker.PERCENT_RESIST);
    }
}
