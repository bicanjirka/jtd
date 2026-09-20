package td.enemy;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.Optional;

/**
 * Reduces every incoming hit by a flat amount, clamped at zero rather than healing - the
 * Warden's armor, distinct in kind from {@link PercentResistTrait}'s percentage so the
 * feature demonstrates more than one shield flavor. Deliberately a per-hit flat reduction, not
 * a depleting absorption pool: {@link Trait} instances are shared across every mob built from
 * the same {@link EnemyDefinition} (see {@link TraitContext}), so a pool that's "used up" over
 * one mob's lifetime doesn't fit this model without per-mob mutable trait state, which nothing
 * else in v1 needs either.
 * <p>
 * {@code restrictedTo}, when present, narrows this trait to one {@link DamageType} - a hit of the
 * other type passes through untouched. Empty (the 1-arg constructor, every pre-existing caller's
 * shape) resists both, unchanged from before this field existed - see {@link #physicalOnly}/
 * {@link #magicOnly}.
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
