package td.effect;

import td.damage.Damage;

/**
 * A timed, stacking-or-not modifier attached to a live enemy - the runtime result of a tower's
 * on-hit effect, and (per this project's shared-primitive decision) later of an enemy's own
 * ability or an aura tower's continuous buff too.
 * <p>
 * Every field has a meaningful identity value for every {@link EffectKind}, so resolving an
 * enemy's active effects needs no per-kind branching: effective speed is the product of every
 * active effect's {@link #speedMultiplier}, damage-over-time is each effect's
 * {@link #damagePerTick} applied through its own {@link #sink}, and damage absorption is each
 * effect's {@link #shieldPercent}. A slow carries {@code Damage.none()}; a burn carries a
 * {@code speedMultiplier} of {@code 1f}. Freeze is modelled as a slow with a
 * {@code speedMultiplier} of {@code 0f} - deliberately one speed-to-zero effect rather than a
 * separate "stun" mechanic, since nothing in this game distinguishes the two until an ability
 * exists for stun to suppress that freeze wouldn't already cover.
 * <p>
 * {@link #kind} exists only for the UI marker and for matching against an already-active
 * effect of the same kind when re-applying one - see {@code ActiveEffects}.
 */
public record Effect(EffectKind kind, float speedMultiplier, Damage damagePerTick, float shieldPercent,
                     int remainingTicks, DamageSink sink) {

    public static Effect slow(float speedMultiplier, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SLOW, speedMultiplier, Damage.none(), 0f, durationTicks, sink);
    }

    public static Effect freeze(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.FREEZE, 0f, Damage.none(), 0f, durationTicks, sink);
    }

    public static Effect burn(Damage damagePerTick, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.BURN, 1f, damagePerTick, 0f, durationTicks, sink);
    }

    /**
     * Reduces a percentage of every incoming hit while active - see {@code ActiveEffects.applyShield}.
     */
    public static Effect shield(float shieldPercent, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SHIELD, 1f, Damage.none(), shieldPercent, durationTicks, sink);
    }

    /**
     * Not a valid target while active - see {@code ActiveEffects.isInvisible}.
     */
    public static Effect invisible(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.INVISIBLE, 1f, Damage.none(), 0f, durationTicks, sink);
    }

    Effect withRemainingTicks(int remainingTicks) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent, remainingTicks, this.sink);
    }
}
