package td.effect;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.List;
import java.util.Optional;

/**
 * A timed modifier on a live enemy, from a tower hit or an enemy ability.
 * <p>
 * Every field has an identity value for the kinds that don't use it, so resolving effects needs no
 * per-kind branching: speed multiplies, damage-over-time goes through {@link #sink}, shields absorb
 * and heals restore. Freeze is a slow to {@code 0f}, not a separate stun.
 * <p>
 * {@link #kind} is only for UI markers and for matching a reapplication.
 * {@link #shieldRestrictedTo} applies to shields only; empty absorbs both damage types.
 * {@link #authoredDurationTicks} never counts down, so progress through the effect's life can be
 * computed. {@link #burnFuel} and {@link #peakBurnL0} are the burn pool: one contribution per
 * tower, and the strongest application ever taken, which bounds what a reapplication can add.
 */
public record Effect(EffectKind kind, float speedMultiplier, Damage damagePerTick, float shieldPercent,
                     int remainingTicks, DamageSink sink, int healPerTick,
                     Optional<DamageType> shieldRestrictedTo, int authoredDurationTicks,
                     List<BurnContribution> burnFuel, float peakBurnL0) {

    public static Effect slow(float speedMultiplier, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SLOW, speedMultiplier, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f);
    }

    public static Effect freeze(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.FREEZE, 0f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f);
    }

    public static Effect burn(Damage damagePerTick, int durationTicks, DamageSink sink) {
        float l0 = damagePerTick.amount();
        return new Effect(EffectKind.BURN, 1f, damagePerTick, 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(new BurnContribution(sink, l0)), l0);
    }

    /** Absorbs a percentage of every hit while active. */
    public static Effect shield(float shieldPercent, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SHIELD, 1f, Damage.none(), shieldPercent, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f);
    }

    /** Untargetable while active. */
    public static Effect invisible(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.INVISIBLE, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f);
    }

    /**
     * Restores {@code healPerTick} every tick. Not a negative {@code damagePerTick}: damage clamps
     * at zero, and a heal credits nobody.
     */
    public static Effect heal(int healPerTick, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.HEAL, 1f, Damage.none(), 0f, durationTicks, sink, healPerTick,
                Optional.empty(), durationTicks, List.of(), 0f);
    }

    /** The burn pool's current total; {@code 0f} for other kinds. */
    public float fuelLevel() {
        float total = 0f;
        for (BurnContribution contribution : this.burnFuel) {
            total += contribution.amount();
        }
        return total;
    }

    /** Narrows a shield to one damage type; the other passes through. */
    public Effect withShieldRestrictedTo(DamageType type) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, Optional.of(type),
                this.authoredDurationTicks, this.burnFuel, this.peakBurnL0);
    }

    Effect withRemainingTicks(int remainingTicks) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent, remainingTicks,
                this.sink, this.healPerTick, this.shieldRestrictedTo, this.authoredDurationTicks,
                this.burnFuel, this.peakBurnL0);
    }

    /** Carries the burn pool forward after decay or a top-up. {@code peakBurnL0} only grows. */
    Effect withBurnFuel(List<BurnContribution> burnFuel, float peakBurnL0) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, burnFuel, peakBurnL0);
    }
}
