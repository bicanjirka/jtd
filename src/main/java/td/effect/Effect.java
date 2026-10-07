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
 * and heals restore. Freeze is a speed of {@code 0f}, not a separate stun.
 * <p>
 * {@link #kind} is only for UI markers and for matching a reapplication.
 * {@link #shieldRestrictedTo} applies to shields only; empty absorbs both damage types.
 * {@link #authoredDurationTicks} never counts down, so progress through the effect's life can be
 * computed. {@link #fuel} is a decaying level (chill, burn or poison): one contribution per tower.
 * {@link #peakL0} is a pool's strongest application ever taken, which bounds what a reapplication
 * can add. {@link #stacks} is a vulnerability's stack count, or a scorched or sickened debuff's. A
 * burn or poison counts down {@link #stackClock} to the next stack it earns, one every
 * {@link #STACK_INTERVAL_TICKS}; the stacks themselves are a separate debuff that outlasts the pool.
 * {@link #faultLine} marks a Fractured that recovers nothing while the enemy is exposed.
 * {@link #stackCap} is the most stacks a stacking kind may reach, {@code 0} for its kind's own cap.
 */
public record Effect(EffectKind kind, float speedMultiplier, Damage damagePerTick, float shieldPercent,
                     int remainingTicks, DamageSink sink, int healPerTick,
                     Optional<DamageType> shieldRestrictedTo, int authoredDurationTicks,
                     List<FuelContribution> fuel, float peakL0, int stacks, int stackClock,
                     boolean faultLine, int stackCap) {

    /** A burn or poison earns a stack this often while it lasts: one every half second. */
    static final int STACK_INTERVAL_TICKS = 10;
    /** How deep a Fractured that Fault Line left falls: resilience -100. */
    private static final int FAULT_LINE_STACK_CAP = 10;

    /**
     * Slows in proportion to its level: {@code amount} at first (0.5 is half speed), decaying
     * linearly to nothing over {@code durationTicks}.
     */
    public static Effect chill(float amount, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.CHILL, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(new FuelContribution(sink, amount, amount / durationTicks)),
                0f, 0, 0, false, 0);
    }

    public static Effect freeze(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.FREEZE, 0f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /** Stops the enemy for {@code durationTicks}; unlike a freeze, it puts out nothing. */
    public static Effect dazed(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.DAZED, 0f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /** Damage that decays exponentially; each stack it earns lowers resilience by one for good. */
    public static Effect burn(Damage damagePerTick, int durationTicks, DamageSink sink) {
        return pool(EffectKind.BURN, damagePerTick, durationTicks, sink);
    }

    /**
     * A second pool of its own, so it stacks with a burn: damage that also slows in proportion to the
     * pool, and each stack it earns lowers spirit by one for good.
     */
    public static Effect poison(Damage damagePerTick, int durationTicks, DamageSink sink) {
        return pool(EffectKind.POISON, damagePerTick, durationTicks, sink);
    }

    private static Effect pool(EffectKind kind, Damage damagePerTick, int durationTicks, DamageSink sink) {
        float l0 = damagePerTick.amount();
        return new Effect(kind, 1f, damagePerTick, 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(new FuelContribution(sink, l0)), l0, 0, STACK_INTERVAL_TICKS, false, 0);
    }

    /** Absorbs a percentage of every hit while active. */
    public static Effect shield(float shieldPercent, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.SHIELD, 1f, Damage.none(), shieldPercent, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /** Untargetable while active. */
    public static Effect invisible(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.INVISIBLE, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /** Targetable again while active, even through invisibility. */
    public static Effect revealed(int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.REVEALED, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /**
     * A burn's or poison's lasting mark ({@code SCORCHED} or {@code SICKENED}) with {@code stacks}
     * stacks. Made by {@code ActiveEffects} as a pool earns them, never by a tower.
     */
    static Effect stackDebuff(EffectKind kind, int stacks) {
        return new Effect(kind, 1f, Damage.none(), 0f, 0, d -> {
        }, 0, Optional.empty(), 0, List.of(), 0f, stacks, 0, false, 0);
    }

    /** {@code stacks} more hits' worth of extra damage taken, on one shared clock. */
    public static Effect vulnerable(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.VULNERABLE, stacks, durationTicks, sink);
    }

    /** {@code stacks} more points of lost armor, five each, on one shared clock. */
    public static Effect sundered(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.SUNDERED, stacks, durationTicks, sink);
    }

    /** {@code stacks} more steps of magic damage taken, on one shared clock. */
    public static Effect resonating(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.RESONATING, stacks, durationTicks, sink);
    }

    /** {@code stacks} more steps of lost resilience; each wears off on its own, so there is no clock. */
    public static Effect fractured(int stacks, DamageSink sink) {
        return stacking(EffectKind.FRACTURED, stacks, 0, sink);
    }

    /** This Fractured as Fault Line leaves it: it falls twice as far, and holds while the enemy is exposed. */
    public Effect withFaultLine() {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, this.fuel, this.peakL0, this.stacks, this.stackClock, true,
                FAULT_LINE_STACK_CAP);
    }

    /** This stacking effect allowed up to {@code stackCap} stacks instead of its kind's cap. */
    public Effect withStackCap(int stackCap) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, this.fuel, this.peakL0, this.stacks, this.stackClock, this.faultLine,
                stackCap);
    }

    /** The most stacks it may reach: its own cap, else its kind's. */
    public int effectiveStackCap() {
        return this.stackCap > 0 ? this.stackCap : this.kind.maxStacks();
    }

    /**
     * {@code stacks} more Saturation, on one shared clock any blast that catches the enemy refreshes;
     * the stacks all go when it runs out.
     */
    public static Effect saturated(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.SATURATED, stacks, durationTicks, sink);
    }

    /** Crit chance taken doubles while it lasts. */
    public static Effect exposed(int durationTicks, DamageSink sink) {
        return timed(EffectKind.EXPOSED, durationTicks, sink);
    }

    /** The next hit lands as a guaranteed crit and ends the mark; it ends by itself after {@code durationTicks}. */
    public static Effect marked(int durationTicks, DamageSink sink) {
        return timed(EffectKind.MARKED, durationTicks, sink);
    }

    /** Extra damage from every tower, and the preferred target of towers that pick one. */
    public static Effect priority(int durationTicks, DamageSink sink) {
        return timed(EffectKind.PRIORITY, durationTicks, sink);
    }

    private static Effect stacking(EffectKind kind, int stacks, int durationTicks, DamageSink sink) {
        return new Effect(kind, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, stacks, 0, false, 0);
    }

    private static Effect timed(EffectKind kind, int durationTicks, DamageSink sink) {
        return new Effect(kind, 1f, Damage.none(), 0f, durationTicks, sink, 0,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /**
     * Restores {@code healPerTick} every tick. Not a negative {@code damagePerTick}: damage clamps
     * at zero, and a heal credits nobody.
     */
    public static Effect heal(int healPerTick, int durationTicks, DamageSink sink) {
        return new Effect(EffectKind.HEAL, 1f, Damage.none(), 0f, durationTicks, sink, healPerTick,
                Optional.empty(), durationTicks, List.of(), 0f, 0, 0, false, 0);
    }

    /** The level's current total; {@code 0f} for kinds without one. */
    public float fuelLevel() {
        float total = 0f;
        for (FuelContribution contribution : this.fuel) {
            total += contribution.amount();
        }
        return total;
    }

    public Effect withShieldPercent(float shieldPercent) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, this.fuel, this.peakL0, this.stacks, this.stackClock, this.faultLine, this.stackCap);
    }

    /** Narrows a shield to one damage type; the other passes through. */
    public Effect withShieldRestrictedTo(DamageType type) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, Optional.of(type),
                this.authoredDurationTicks, this.fuel, this.peakL0, this.stacks, this.stackClock, this.faultLine, this.stackCap);
    }

    /**
     * Both the remaining and the authored duration times {@code factor}, rounded. A shorter authored
     * duration also makes a pool decay faster, so its total damage shrinks with it, and a chill
     * lasts as many times longer as its decay is slower.
     */
    public Effect withDurationScaledBy(float factor) {
        List<FuelContribution> scaled = this.kind == EffectKind.CHILL
                ? this.fuel.stream().map(c -> new FuelContribution(c.sink(), c.amount(), c.decayPerTick() / factor)).toList()
                : this.fuel;
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                Math.round(this.remainingTicks * factor), this.sink, this.healPerTick, this.shieldRestrictedTo,
                Math.round(this.authoredDurationTicks * factor), scaled, this.peakL0, this.stacks, this.stackClock, this.faultLine, this.stackCap);
    }

    Effect withRemainingTicks(int remainingTicks) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent, remainingTicks,
                this.sink, this.healPerTick, this.shieldRestrictedTo, this.authoredDurationTicks,
                this.fuel, this.peakL0, this.stacks, this.stackClock, this.faultLine, this.stackCap);
    }

    /** Carries the level forward after decay or a top-up. {@code peakL0} only grows. */
    Effect withFuel(List<FuelContribution> fuel, float peakL0) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, fuel, peakL0, this.stacks, this.stackClock, this.faultLine, this.stackCap);
    }

    /** The same effect with a new stack count and a fresh clock of {@code remainingTicks}. */
    Effect withStacks(int stacks, int remainingTicks) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, this.fuel, this.peakL0, stacks, this.stackClock, this.faultLine, this.stackCap);
    }

    /** The same pool with its clock to the next stack set to {@code stackClock}. */
    Effect withStackClock(int stackClock) {
        return new Effect(this.kind, this.speedMultiplier, this.damagePerTick, this.shieldPercent,
                this.remainingTicks, this.sink, this.healPerTick, this.shieldRestrictedTo,
                this.authoredDurationTicks, this.fuel, this.peakL0, this.stacks, stackClock, this.faultLine, this.stackCap);
    }
}
