package td.effect;

import td.damage.AttackOrigin;
import td.damage.DamageType;

import java.util.Optional;

/**
 * One source's shield: what it has left to absorb, the pool it was cast with, the one damage type it takes if it
 * is limited to one, and its time.
 *
 * @param source         who cast it: a recast from the same source tops it up
 * @param remaining      what it has left, in damage units
 * @param max            the pool it was cast with, in damage units
 * @param restrictedTo   the one damage type it takes; empty takes both
 * @param remainingTicks the ticks it has left; {@link Effect#LASTING_TICKS} until broken
 */
public record ShieldShare(AttackOrigin source, float remaining, float max, Optional<DamageType> restrictedTo,
                          int remainingTicks) implements SourceShare<ShieldShare> {

    /** A shield of {@code units} for {@code durationTicks}, from no source in particular, that takes every type. */
    static ShieldShare cast(float units, int durationTicks) {
        return new ShieldShare(AttackOrigin.none(), units, units, Optional.empty(), durationTicks);
    }

    /** Whether a hit of {@code type} meets this shield. */
    public boolean takes(DamageType type) {
        return this.restrictedTo.map(only -> only == type).orElse(true);
    }

    @Override
    public ShieldShare withSource(AttackOrigin source) {
        return new ShieldShare(source, this.remaining, this.max, this.restrictedTo, this.remainingTicks);
    }

    @Override
    public ShieldShare withRemainingTicks(int ticks) {
        return new ShieldShare(this.source, this.remaining, this.max, this.restrictedTo, ticks);
    }

    /** Topped up to the larger pool, with the later end. */
    @Override
    public ShieldShare refreshedBy(ShieldShare incoming) {
        return new ShieldShare(this.source, Math.max(this.remaining, incoming.remaining), Math.max(this.max, incoming.max),
                incoming.restrictedTo, Math.max(this.remainingTicks, incoming.remainingTicks));
    }

    /** Out of time, or with less than a unit left. */
    @Override
    public boolean isGone() {
        return this.remainingTicks <= 0 || this.remaining < 1f;
    }

    ShieldShare withRemaining(float units) {
        return new ShieldShare(this.source, units, this.max, this.restrictedTo, this.remainingTicks);
    }

    ShieldShare withRestrictedTo(DamageType type) {
        return new ShieldShare(this.source, this.remaining, this.max, Optional.of(type), this.remainingTicks);
    }
}
