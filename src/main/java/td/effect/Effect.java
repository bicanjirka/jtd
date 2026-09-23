package td.effect;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.Optional;

/**
 * A timed, stacking-or-not modifier attached to a live enemy - the runtime result of a tower's
 * on-hit effect, and (per this project's shared-primitive decision) later of an enemy's own
 * ability or an aura tower's continuous buff too.
 * <p>
 * Every field has a meaningful identity value for every {@link EffectKind}, so resolving an
 * enemy's active effects needs no per-kind branching: effective speed is the product of every
 * active effect's {@link #speedMultiplier}, damage-over-time is each effect's
 * {@link #damagePerTick} applied through its own {@link #sink}, and damage absorption is each
 * effect's {@link #shieldPercent}, and restoring health is each effect's {@link #healPerTick},
 * read directly rather than through {@link #sink} (see {@link #heal}). A slow carries
 * {@code Damage.none()}; a burn carries a {@code speedMultiplier} of {@code 1f}. Freeze is
 * modelled as a slow with a {@code speedMultiplier} of {@code 0f} - deliberately one
 * speed-to-zero effect rather than a separate "stun" mechanic, since nothing in this game
 * distinguishes the two until an ability exists for stun to suppress that freeze wouldn't
 * already cover.
 * <p>
 * {@link #kind} exists only for the UI marker and for matching against an already-active
 * effect of the same kind when re-applying one - see {@code ActiveEffects}.
 * <p>
 * {@link #shieldRestrictedTo} is meaningful only for {@link EffectKind#SHIELD} - empty (every
 * factory below) means "absorbs both damage kinds", the same identity-value convention
 * {@code shieldPercent} being {@code 0f} for a non-shield kind already follows. It is reached
 * through {@link #withShieldRestrictedTo}, not a widened {@link #shield} argument list - see
 * {@code ShieldTemplate}, the authored counterpart that actually sets it.
 * <p>
 * {@link #authoredDurationTicks} is the duration as first authored, never decremented (unlike
 * {@link #remainingTicks}, which counts down) - it's what lets {@code ActiveEffects} compute how
 * far through its life an effect currently is at any tick, for {@code SLOW}'s recovery curve and
 * {@code BURN}'s decay rate. Set equal to {@code durationTicks} by every factory below, including
 * ones that never read it back - the same harmless-identity-value convention every other field
 * here already follows for the kinds that don't use it.
 * <p>
 * {@link #fuelLevel} and {@link #peakBurnL0} exist only for {@link EffectKind#BURN}'s decaying
 * fuel-pool model - see {@code ActiveEffects}' burn handling and {@code td/effect/CLAUDE.md}.
 * {@code fuelLevel} is the pool's current level {@code L}; {@code peakBurnL0} is the strongest
 * single application's own {@code damagePerTick} this mob has ever taken, which bounds how much
 * a reapplication can still add. Both are {@code 0f} - a harmless identity value - for every
 * other kind.
 */
public record Effect(EffectKind kind, float speedMultiplier, Damage damagePerTick, float shieldPercent,
                     int remainingTicks, DamageSink sink, int healPerTick,
                     Optional<DamageType> shieldRestrictedTo, int authoredDurationTicks,
                     float fuelLevel, float peakBurnL0) {

    public static Effect slow(float speedMultiplier, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SLOW, speedMultiplier, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, 0f, 0f);
    }

    public static Effect freeze(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.FREEZE, 0f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, 0f, 0f);
    }

    public static Effect burn(Damage damagePerTick, int durationTicks, DamageSink sink) {
        float l0 = damagePerTick.amount();
        return new Effect(EffectKind.BURN, 1f, damagePerTick, 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, l0, l0);
    }

    /**
     * Reduces a percentage of every incoming hit while active - see {@code ActiveEffects.applyShield}.
     */
    public static Effect shield(float shieldPercent, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SHIELD, 1f, Damage.none(), shieldPercent, durationTicks, sink, 0,
                Optional.empty(), durationTicks, 0f, 0f);
    }

    /**
     * Not a valid target while active - see {@code ActiveEffects.isInvisible}.
     */
    public static Effect invisible(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.INVISIBLE, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, 0f, 0f);
    }

    /**
     * Restores {@code healPerTick} health, every tick, for {@code durationTicks} - see
     * {@code ActiveEffects.healPerTick}. Deliberately not modelled as a negative
     * {@code damagePerTick} through the {@code sink} a burn uses - {@code Damage}'s compact
     * constructor clamps at zero specifically so a healing hit can never exist, and a heal
     * credits nobody the way a damage-over-time tick credits the tower that applied it. This is
     * a plain per-tick amount a mob reads and applies to its own health directly.
     */
    public static Effect heal(int healPerTick, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.HEAL, 1f, Damage.none(), 0f, durationTicks, sink, healPerTick,
                Optional.empty(), durationTicks, 0f, 0f);
    }

    /**
     * Narrows a {@link EffectKind#SHIELD} effect to absorb one {@link DamageType} only - the
     * other passes through untouched. The narrow entry point this record's width (11 components)
     * asks for, rather than widening {@link #shield}'s own argument list.
     */
    public Effect withShieldRestrictedTo(DamageType type) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, Optional.of(type),
                this.authoredDurationTicks, this.fuelLevel, this.peakBurnL0);
    }

    Effect withRemainingTicks(int remainingTicks) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent, remainingTicks,
                this.sink, this.healPerTick, this.shieldRestrictedTo, this.authoredDurationTicks,
                this.fuelLevel, this.peakBurnL0);
    }

    /**
     * A copy carrying {@code BURN}'s fuel pool forward - after a tick's decay, or after a
     * reapplication tops it up. {@code peakBurnL0} only ever grows, since it bounds how much any
     * future reapplication can still add regardless of which application is currently active.
     */
    Effect withFuelLevel(float fuelLevel, float peakBurnL0) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, fuelLevel, peakBurnL0);
    }
}
