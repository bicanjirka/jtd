package td.enemy;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.Optional;

/**
 * Absorbs a fixed fraction of every incoming hit - the migrated Square's resistance. {@link
 * Damage}'s own zero-clamp turns a {@code fraction} of zero or below into full immunity, never a
 * healing hit. A stronger or weaker resistance is a different concrete instance authored at a
 * different {@link Rank}, not a formula scaled by anything live on the mob - see
 * {@code td/enemy/CLAUDE.md}'s note on why {@link TraitContext} no longer carries a level.
 * <p>
 * {@code restrictedTo}, when present, narrows this trait to one {@link DamageType} - a hit of the
 * other type passes through untouched. Empty (the 1-arg constructor, every pre-existing caller's
 * shape) resists both, unchanged from before this field existed - see {@link #physicalOnly}/
 * {@link #magicOnly}.
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
        return TraitMarker.PERCENT_RESIST;
    }
}
