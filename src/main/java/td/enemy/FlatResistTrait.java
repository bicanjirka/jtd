package td.enemy;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.Optional;

/**
 * Reduces every hit by a flat amount, clamped at zero. Per hit rather than a depleting pool,
 * because traits are shared by every mob of a definition and hold no per-mob state.
 * {@code restrictedTo}, when present, limits it to one damage type.
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
    public Damage onHit(Damage incoming, TraitContext context) {
        if (this.restrictedTo.isPresent() && this.restrictedTo.get() != incoming.type()) {
            return incoming;
        }
        return new Damage(incoming.amount() - this.flatReduction, incoming.type(), incoming.critical());
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.FLAT_RESIST;
    }
}
